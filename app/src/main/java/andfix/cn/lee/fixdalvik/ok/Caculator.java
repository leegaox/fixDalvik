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
