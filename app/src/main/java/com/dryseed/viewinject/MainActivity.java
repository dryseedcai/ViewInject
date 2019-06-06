package com.dryseed.viewinject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 基于编译时注解的View注入框架
 * <p>
 * 基本思想：
 * 类似ButterKnife，编译时获取到指定注解上的信息，生成代理类（ProxyInfo），
 * 并在运行时通过接口调用代理类的inject方法，将view注入到类成员上。
 */
public class MainActivity extends AppCompatActivity {
    @Bind(R.id.id_textview)
    TextView mTv;

    @Bind(R.id.id_btn)
    Button mBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        ViewInjector.injectView(this);

        mTv.setText("ViewInject");
        mBtn.setText("ViewInject ~");

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CategoryActivity.class));
            }
        });
    }
}
