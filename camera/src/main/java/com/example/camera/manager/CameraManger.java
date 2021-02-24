package com.example.camera.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraManger {
    private static final String TAG = "CameraManger";

    private static CameraManger instance;
    private CameraManger() {

    }

    public static CameraManger getInstance() {
        if (instance == null) {
            synchronized (CameraManger.class){
                if (instance == null) {
                    instance = new CameraManger();
                }
            }
        }
        return instance;
    }

    /**
     * 获取相机实例
     *
     * @return
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            int cameraCount = Camera.getNumberOfCameras();
            c = Camera.open(cameraCount - 1);

            //设置自动对焦参数
            Camera.Parameters parameters = c.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            c.setParameters(parameters);

            // TODO: 2020-06-18 可以在用户点击的区域画矩形，增加用户体验

        } catch (Exception e) {
            Log.i(TAG, "Camera open exception: " + e.getMessage());
        }
        return c;
    }


    /**
     * 释放相机资源
     */
    public void releaseCamera(Camera camera) {
        if (camera != null) {
            camera.release();      //release the camera for other applications
            camera = null;
        }
    }


    /**
     * 检查是否有相机硬件
     *
     * @param context
     * @return
     */
    public boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            //has a camera
            return true;
        } else {
            //no camera
            return false;
        }
    }
}
