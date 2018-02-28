## 热修复 - 阿里云andfix原理

1. 服务器生成修复包out.dex（class-->dex） 供用户下载到系统根目录。

2. app客户端加载dex包，通过注解和反射找到修复好的class里需要替换的方法。

3. 通过C++修改ArgMethod在方法表中的指针：找到需要修复的MethodA（修复好的）替换MethodB（bug）

#### 如何生成dex文件：
    ex:

      - as 运行apk 生成修复后的Caculator.class文件，然后找到该文件,文件目录为build/intermediates/classes/cn.lee.fixdalvik.ok.Caculator.class

      - 使用sdk/build-tools/23.0.2/dx.bat 生成dex文件:out.dex   ,  cmd命令：dx --dex --output sourcePath(dex生成路径) path（.class路径）

#### JVM,Dalvik and ART

JVM加载的是class格式的类文件，Java虚拟机使用的指令集是基于堆栈
Dalvik加载的是dex格式的类文件，Dalvik虚拟机使用的指令是基于寄存器 JIT在运行时将解释语言编译成机器语言。
ART加载的oat文件，直接运行oat文件里类、方法所映射的机器指令。ART虚拟机使用的指令是基于寄存器。AOT 在程序运行前将解释语言编译成机器语言，发生在apk安装时。


#### TODO
   1. md详细，原理展开；
