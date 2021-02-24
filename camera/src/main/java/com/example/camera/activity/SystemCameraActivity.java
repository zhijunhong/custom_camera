package com.example.camera.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.camera.R;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.RequestCallback;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * 系统相机
 * 拍摄视频见注释部分
 */
public class SystemCameraActivity extends AppCompatActivity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
//    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 101;                           //打开拍摄视频
    private Uri fileUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        solveFileProvider();

        setContentView(R.layout.activity_system_camera);

        findViewById(R.id.btn_open_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开系统相机
                openCapturePicOrVideo();
            }
        });
    }

    /**
     * 解决file provider问题
     */
    private void solveFileProvider() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    /**
     * 启动系统相机
     */
    private void openCapturePicOrVideo() {
        PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            Toast.makeText(SystemCameraActivity.this, "All permissions are granted", Toast.LENGTH_LONG).show();

                            //拍照
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);                      // create a file to save the image
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

                            //拍摄视频
//                            Intent intent1 = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                            fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);                    // create a file to save the video
//                            intent1.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);                   // set the image file name
//                            intent1.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);                  // set the video image quality to high
//                            // start the Video Capture Intent
//                            startActivityForResult(intent1, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

                        } else {
                            Toast.makeText(SystemCameraActivity.this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String tip;
                if (data != null) {
                    tip = "Image saved to:" + data.getParcelableExtra("data");               //系统目录
                } else {
                    tip = "Image saved to:" + fileUri;                                              //指定目录
                }
                Toast.makeText(this, tip, Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
            } else {
            }
        }

//        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                Toast.makeText(this, "Video saved to:\n" +
//                        data.getData(), Toast.LENGTH_LONG).show();
//            } else if (resultCode == RESULT_CANCELED) {
//            } else {
//            }
//        }
    }

    /**
     * 指定文件存储路径
     * @param type
     * @return
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "camera_app");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.i("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
        return mediaFile;
    }
}
