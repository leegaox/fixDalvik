package andfix.cn.lee.fixdalvik;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;

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
                //修复号的realClazz ,使用反射注解找到需要修复的方法
                Class realClazz = dexFile.loadClass(className, context.getClassLoader());
                Log.i(TAG, "找到类：" + className);
                //修复
                fix(realClazz);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private native void replace(Method wrongMethod, Method rightMethod);

    static {
        System.loadLibrary("native-lib");
    }

}
