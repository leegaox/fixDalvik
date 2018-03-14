## 热修复 - 阿里云andfix原理
> Andfix在已经加载了的类中直接在native层替换掉原有方法。
> Android的java运行环境，在4.4以下用的是Dalvik虚拟机，而在4.4以上用的是Art虚拟机。Java方法在Dalvik、Art虚拟机中都对应着一个底层数据结构**ArtMethod**，ArtMethod记录了这个Java方法的所有信息，包括所属类、访问权限、代码执行地址等等。而对于不同的Android版本，底层Java对象的数据结构是不同的，因此需要对不同的版本做兼容，本文以Android4.4以下环境即Dalvik运行时为例说明Andfix的原理。

一个类的加载，必经resolve(dvmResolveClass)->link(dvmLinkClass)->init(dvmInitClass)三个阶段
,给一个类的所有方法分配内存空间发生在link阶段，Dalvik运行时下native层加载方法如下：
````c++
@android4.4_r1/art/runtime/class_linker.cc

void ClassLinker::LoadClass(const DexFile& dex_file,
                            const DexFile::ClassDef& dex_class_def,
                            SirtRef<mirror::Class>& klass,
                            mirror::ClassLoader* class_loader) {
  ... ...

  // Load methods.
  if (it.NumDirectMethods() != 0) {
    // TODO: append direct methods to class object
    mirror::ObjectArray<mirror::ArtMethod>* directs =
         AllocArtMethodArray(self, it.NumDirectMethods());
    if (UNLIKELY(directs == NULL)) {
      CHECK(self->IsExceptionPending());  // OOME.
      return;
    }
    klass->SetDirectMethods(directs);
  }
  if (it.NumVirtualMethods() != 0) {
    // TODO: append direct methods to class object
    mirror::ObjectArray<mirror::ArtMethod>* virtuals =
        AllocArtMethodArray(self, it.NumVirtualMethods());
    if (UNLIKELY(virtuals == NULL)) {
      CHECK(self->IsExceptionPending());  // OOME.
      return;
   }
    klass->SetVirtualMethods(virtuals);
  }

  ... ...
}

````

其核心在于replaceMethod函数：

````java
    private native void replace(Method wrongMethod, Method rightMethod);
````
这是一个native方法，他的参数是在Java层通过反射机制得到的Method对象所对应的jobject。wrongMethod对应的是需要被替换的原有方法，而rightMethod对应的就是新方法，新方法存在于补丁包的新类中。
Dalvik运行时的native层替换方法如下：
````c++
Java_andfix_cn_lee_fixdalvik_DxManager_replace(JNIEnv *env, jobject instance, jobject wrongMethod,
                                               jobject rightMethod) {
    //拿到错误class 字节码里面的方法表里的ArtMethod
    art::mirror::ArtMethod *smeth = (art::mirror::ArtMethod *) env->FromReflectedMethod(
            wrongMethod);
    //拿到正确class 字节码里面的方法表里的ArtMethod
    art::mirror::ArtMethod *dmeth = (art::mirror::ArtMethod *) env->FromReflectedMethod(
            rightMethod);

    //替换artMethod结构体的所有成员变量的指针
    smeth->declaring_class_= dmeth->declaring_class_;
    smeth->dex_cache_resolved_types_ = dmeth->dex_cache_resolved_types_;
    smeth->access_flags_ = dmeth->access_flags_ ;
    smeth->dex_cache_resolved_methods_ = dmeth->dex_cache_resolved_methods_;
    smeth->dex_code_item_offset_ = dmeth->dex_code_item_offset_;
    smeth->method_index_ = dmeth->method_index_;
    smeth->dex_method_index_ = dmeth->dex_method_index_;
    smeth->method_dex_index_ = dmeth->method_dex_index_;

    smeth->ptr_sized_fields_.dex_cache_resolved_methods_ = dmeth->ptr_sized_fields_.dex_cache_resolved_methods_;
    smeth->ptr_sized_fields_.entry_point_from_interpreter_ = dmeth->ptr_sized_fields_.entry_point_from_interpreter_;
    smeth->ptr_sized_fields_.entry_point_from_jni_ = dmeth->ptr_sized_fields_.entry_point_from_jni_;
    smeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_ = dmeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_;

}
````
通过env->FromReflectedMethod，可以由Method对象得到这个方法对应的ArtMethod的真正的起始地址（ArtMethods*指针数组的下标），然后就可以把它强转为ArtMethod指针，从而对其索引成员进行替换。

这样全部替换完之后就完成了热修复的逻辑。以后调用这个方法时就会直接走到新方法的实现中了。

## fixDalvik Demo

### 模拟异常
在package andfix.cn.lee.fixdalvik下模拟一个除数为0的异常:
````java
package andfix.cn.lee.fixdalvik;

public class Caculator {

    public int caculate() {
        int i = 0;
        int j = 100;
        return j / i;
    }
}
````
使用**自定义注解@Replace**标识出Bug的方法：
````java

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replace {
    //需要加载的类名
    String clazz();

    //方法名
    String method();
}
````

在package andfix.cn.lee.fixdalvik.ok下模拟修复好的Caculator类:

````java
package andfix.cn.lee.fixdalvik.ok;

import andfix.cn.lee.fixdalvik.Replace;

/**
 * 模拟服务器修上复好bug的文件
 */
public class Caculator {

    //使用注解标识需要替换的方法
    @Replace(clazz = "andfix.cn.lee.fixdalvik.Caculator", method = "caculate")
    public int caculate() {
        int i = 10;
        int j = 100;
        return j / i;
    }
}
````
### 生成dex，下发补丁包
- as run app 生成修复后的Caculator.class文件，然后找到该文件,文件目录为build/intermediates/classes/cn.lee.fixdalvik.ok.Caculator.class

- 使用sdk/build-tools/23.0.2/dx.bat 生成dex文件:out.dex   ,  cmd命令：dx --dex --output sourcePath(dex生成路径) path（.class路径）

### app加载dex补丁包
app短下载好out.dex补丁包存放在跟路径下，通过**DexClassLoader**(代替废弃的DexFile)加载dex文件,通过反射找到加载的dex里的DexFile对象的dexElements（类元素数组）,循环遍历修复dex里的类文件：
````java
    private void dexClassLoadFix(File dexFilePath) {
        //指定dexoutputpath为APP自己的缓存目录
        File dexOutputDir = context.getDir("dex", 0);
        //下面开始加载dex class
        DexClassLoader dexClassLoader = new DexClassLoader(dexFilePath.getAbsolutePath(), dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        try {
            Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
            //获取私有变量pathList（DexPathList实例）
            Object pathList = getFieldValue(cl, "pathList", dexClassLoader);
            Class<?> DexPathListCl = Class.forName("dalvik.system.DexPathList");
            //获取私有变量dexElements
            Object[] dexElements = (Object[]) getFieldValue(DexPathListCl, "dexElements", pathList);
            //获取DexPathList内部类Element
            Class elementClass = Class.forName("dalvik.system.DexPathList$Element");
            //遍历dexElements里的dexFile
            for (Object elementObj : dexElements) {
                DexFile dexFile = (DexFile) getFieldValue(elementClass, "dexFile", elementObj);
                //遍历dex里面的class
                Enumeration<String> entry = dexFile.entries();
                while (entry.hasMoreElements()) {
                    String className = entry.nextElement();
                    //修复好的realClazz ,使用反射注解找到需要修复的方法
                    Class realClazz = dexFile.loadClass(className, context.getClassLoader());
                    Log.i(TAG, "找到类：" + className);
                    //修复
                    fix(realClazz);
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
````
通过**Replace**注解找到类里面需要替换的方法，然后调用nativce方法进行修复：
````java
    private void fix(Class realClazz) {
        Method[] methods = realClazz.getDeclaredMethods();
        for (Method method : methods) {
            //拿到注解
            Replace replace = method.getAnnotation(Replace.class);
            if (replace == null) {
                continue;
            }
            String wrongClazzName = replace.clazz();
            String wrongMethodName = replace.method();
            try {
                Class wrongClass = Class.forName(wrongClazzName);
                //最终拿到错误的Method对象
                Method wrongMethod = wrongClass.getDeclaredMethod(wrongMethodName, method.getParameterTypes());
                //修复
                Log.i(TAG, "修复错误方法：" + wrongMethodName);
                replace(wrongMethod, method);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    private native void replace(Method wrongMethod, Method rightMethod);
````

### 注
#### JVM,Dalvik and ART

JVM加载的是class格式的类文件，Java虚拟机使用的指令集是基于堆栈
Dalvik加载的是dex格式的类文件，Dalvik虚拟机使用的指令是基于寄存器 JIT在运行时将解释语言编译成机器语言。
ART加载的oat文件，直接运行oat文件里类、方法所映射的机器指令。ART虚拟机使用的指令是基于寄存器。AOT 在程序运行前将解释语言编译成机器语言，发生在apk安装时。
实际上，Dalvik和Art最后加载的都是优化后的odex（optimized dex），只是Art优化后的odex 比Dalvik优化的odex文件多了oat文件。
