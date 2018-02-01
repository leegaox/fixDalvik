## 阿里云andfix原理

1. 服务器生成修复包out.dex（class-->dex） 供用户下载到系统根目录。

2. app客户端加载dex包，通过注解和反射找到修复好的class里需要替换的方法。

3. 通过C++修改ArgMethod在方法表中的指针：找到需要修复的MethodA（修复好的）替换MethodB（bug）

#### 如何生成dex文件：

    as 运行apk 生成class文件，然后找到该文件：build/intermediates/classes/cn.lee.fixdalvik.ok.Caculator.class

    使用sdk/build-tools/23.0.2/dx.bat 生成dex文件:out.dex   ,  cmd命令：dx --dex --output sourcePath(dex生成路径) path（.class路径）

#### TODO
   1. md详细，原理展开；art::mirror::class 强转代码？
