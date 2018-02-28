package andfix.cn.lee.fixdalvik;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;

import dalvik.system.DexFile;

public class MainActivity extends AppCompatActivity {
    private TextView tv;
    private boolean fixed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.sample_text);

    }

    public void caculate(View view) {
        Caculator caculator = new Caculator();
        tv.setText("计算结果：" + caculator.caculate());
    }

    public void fix(View view) {
        if (!fixed) {
            fixed = true;
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "out.dex");
            DxManager dxManager = new DxManager(this);
            dxManager.loadDex(file);
        }


    }


}
