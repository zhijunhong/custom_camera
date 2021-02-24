package com.example.camera.activity;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.camera.R;
import com.example.camera.base.BaseActivity;
import com.example.camera.manager.CameraManger;
import com.example.camera.utils.FileUtils;
import com.example.camera.widget.CameraPreview;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.util.List;

/**
 * 自定义camera
 */
public class CustomCameraActivity extends BaseActivity {
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private Button mBtnCapture;
    private FrameLayout mFrameLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        initView();
        bindEvent();
        initData();

    }

    private void initView() {
        mBtnCapture = findViewById(R.id.button_capture);
        mFrameLayout = findViewById(R.id.camera_preview);
    }

    private void bindEvent() {
        //点击拍照按钮
        mBtnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get an image from the camera
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        FileUtils.saveTakePicture(data);
                        //关闭界面
                        finish();
                    }
                });
            }
        });
    }

    private void initData() {
        PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            showToast(CustomCameraActivity.this, "All permissions are granted");

                            //申请权限后，启动相机
                            if (CameraManger.getInstance().checkCameraHardware(CustomCameraActivity.this)) {
                                mCamera = CameraManger.getInstance().getCameraInstance();
                                mCameraPreview = new CameraPreview(CustomCameraActivity.this, mCamera);
                                mFrameLayout.addView(mCameraPreview);

                            } else {
                                showToast(CustomCameraActivity.this, "This device not support camera");
                            }
                        } else {
                            showToast(CustomCameraActivity.this, "These permissions are denied: $deniedList");
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraManger.getInstance().releaseCamera(mCamera);
    }
}
