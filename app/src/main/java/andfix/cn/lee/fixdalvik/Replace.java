package andfix.cn.lee.fixdalvik;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yanfa
 * @Title: {标题}
 * @Description:{使用Replace注解标记一个出Bug的方法}
 * @date 2018/2/1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Replace {
    //需要加载的类名
    String clazz();

    //方法名
    String method();
}
