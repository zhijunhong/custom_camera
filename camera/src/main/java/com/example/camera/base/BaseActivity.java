package com.example.camera.base;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private static Toast mToast;
    private static Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 显示通知
     * @param msg
     */
    public static void showToast(final Activity activity, final String msg) {
        if (!isMainThread()) {
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mToast == null) {
                        mToast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
                    } else {
                        mToast.setText(msg);
                    }
                    mToast.show();
                }
            });
        } else {
            if (mToast == null) {
                mToast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
            } else {
                mToast.setText(msg);
            }
            mToast.show();
        }
    }

    /**
     * 判断是否是主线程
     *
     * @return
     */
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

}
