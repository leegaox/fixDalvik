package andfix.cn.lee.fixdalvik;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * @author yanfa
 * @Title: {标题}
 * @Description:{Dex文件的加载}
 * @date 2018/2/1
 */
public class DxManager {

    private String TAG = "DxManager";

    private Context context;

    public DxManager(Context context) {
        this.context = context;
    }

    /**
     * 加载Dex文件
     *
     * @param dexFilePath
     */
    public void loadDex(File dexFilePath) {
        //方式1. 初始的实现方式：使用DexFile加载dex文件遍历类和类方法找到要修复的方法进如native修复方法。
        dexFileFix(dexFilePath);

        //方式2.修复方式：使用DexClassLoader加载指定的类名的类。
        singleFix(dexFilePath);

        //方式3.反射DexClassLoader->pathList(DexPathList)->dexElement(DexPathList内部静态类Element)->dexFile(DexFile)
        dexClassLoadFix(dexFilePath);
    }

    /**
     * 通过看BaseDexClassLoader源码找到单一方式加载的解决方案：使用反射遍历DexFile。
     * 解决DexClassLoad单一方式加载类，但该方式仍然是通过DexFile（废弃的类）获得dex里的类，仍然存在以下问题：
     * //TODO...7.0 修复后计算结果不对，8.0修复后的方法奔溃。
     *
     * @param dexFilePath
     */
    private void dexClassLoadFix(File dexFilePath) {
        //指定dexoutputpath为APP自己的缓存目录
        File dexOutputDir = context.getDir("dex", 0);
        //下面开始加载dex class
        DexClassLoader dexClassLoader = new DexClassLoader(dexFilePath.getAbsolutePath(), dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        try {
            Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
//            Constructor<?>[] constructors = cl.getConstructors();
//            Object instance = constructors[0].newInstance(dexFilePath.getAbsolutePath(), dexOutputDir, null, context.getClassLoader());
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

    /**
     * 使用DexClassLoader单一方式加载类修复，无法动态修复dex里所有的class类。
     * //TODO...该方式修复的结果 : 不存在7.0的问题，8.0仍然奔溃。
     *
     * @param dexFilePath
     */
    private void singleFix(File dexFilePath) {
        //指定dexoutputpath为APP自己的缓存目录
        File dexOutputDir = context.getDir("dex", 0);
        DexClassLoader dexClassLoader = new DexClassLoader(dexFilePath.getAbsolutePath(), dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        //DexclassLoader只能指定类名加载类，无法遍历dex里的Class，因此通过反射的方式获取BaseDexClassLoader->pathList->dexElements->dexFile 的方式。
        try {
            Class realClazz = dexClassLoader.loadClass("andfix.cn.lee.fixdalvik.ok.Caculator");
            Log.i(TAG, "找到类：" + realClazz.getName());
            //修复
            fix(realClazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 使用DexFile类获得dex文件里的Class，加载dex文件进行修复。
     * //TODO...使用DexFile存在的问题：7.0 修复后计算结果不对，8.0修复后的方法奔溃。
     *
     * @param dexFilePath
     */
    private void dexFileFix(File dexFilePath) {
        File optFile = new File(context.getCacheDir(), dexFilePath.getName());
        if (optFile.exists()) {
            optFile.delete();
        }
        try {
            //加载dex
            DexFile dexFile = DexFile.loadDex(dexFilePath.getAbsolutePath(), optFile.getAbsolutePath(), Context.MODE_PRIVATE);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用native修复类方法
     *
     * @param realClazz
     */
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
                Method wrongMethod = wrongClass.getMethod(wrongMethodName, method.getParameterTypes());
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

    /**
     * @param cl            Class.forName获取的Class对象
     * @param fieldName     变量名字
     * @param classInstance 类实例对象
     * @return 变量名的值（变量名表示的类的实例）
     */
    protected Object getFieldValue(Class cl, String fieldName, Object classInstance) {
        Object object = null;
        try {
            Field field = cl.getDeclaredField(fieldName);
            if (field.isAccessible()) {
                object = field.get(classInstance);
            } else {
                field.setAccessible(true);
                object = field.get(classInstance);
                field.setAccessible(false);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return object;
    }

    private native void replace(Method wrongMethod, Method rightMethod);

    static {
        System.loadLibrary("native-lib");
    }

}
