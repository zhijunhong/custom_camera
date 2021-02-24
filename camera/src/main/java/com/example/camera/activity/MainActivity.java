package com.example.camera.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.camera.R;

/**
 * 分发跳转
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_system_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //系统camera
                Intent intent = new Intent(MainActivity.this, SystemCameraActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_custom_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //自定义camera
                //系统camera
                Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
                startActivity(intent);
            }
        });
    }
}
