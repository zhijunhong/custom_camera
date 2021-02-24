# Android自定义Camera相机

## 写在前面

Android Framework层为各种不同的Camera和Camera的特色功能提供了支持，使得你可以很方便的在应用使用拍照和录像功能。如果希望快速实现拍照与录制视频的方法是使用Intent方式调用系统提供的相机功能；当然，如果系统提供的方式不足以满足项目的需求，你就需要自定义Camera相机。本篇博客会通过Intent方式和自定义Camera两部分介绍如何使用相机功能。

传送门：https://github.com/zhijunhong/custom_view/tree/master/camera  转载请表明出处，谢谢~

## Intent实现方式

1. ### 权限申请

- **Camera permisson** - 为了使用相机硬件，需要请求使用Camera的权限

  ```xml
  <uses-permission android:name="android.permission.CAMERA" />
  ```

- **Camera Features** - 你的应用还必须声明使用相机功能

  ```xml
  <uses-feature android:name="android.hardware.camera" />
  ```

  增加相机功能到你的mainfest文件，这样Google Play可以阻止那些没有相机硬件或者没有相机特定功能的设备安装你的应用。

- **Storage Permission** - 应用需要保存图片或者视频到设备的外置存储空间(SD card)上，则需要在manifest中指定存取权限

  ```xml
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  ```

2. ### 代码实现

   - 申请Camera和存储权限

   ```java
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
                               fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);     // create a file to save the image
                               intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                               startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
   
                               //拍摄视频
   //                            Intent intent1 = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
   //                            fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);       // create a file to save the video
   //                            intent1.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);      // set the image file name
   //                            intent1.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);     // set the video image quality to high
   //                            // start the Video Capture Intent
   //                            startActivityForResult(intent1, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
   
                           } else {
                               Toast.makeText(SystemCameraActivity.this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show();
                           }
                       }
                   });
       }
   ```

   **Android6.0**以后的系统，需要在代码中申请敏感权限。这里为了方便起见，直接使用郭神的开源框架[**PermissionX**](https://github.com/guolindev/PermissionX)申请Camera和存储权限

   <!--中间注释部分是拍摄视频的实现方式，篇幅原因这里不再展开-->

   

   - 获取**camera intent**结果回调

   ```java
   @Override
       protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
           super.onActivityResult(requestCode, resultCode, data);
           if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
               if (resultCode == RESULT_OK) {
                   String tip;
                   if (data != null) {
                       tip = "Image saved to:" + data.getParcelableExtra("data");               //系统目录
                   } else {
                       tip = "Image saved to:" + fileUri;                                       //指定目录
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
   ```

   <!--中间注释部分是拍摄视频的实现方式，篇幅原因这里不再展开-->

**Notice**:这里重点说明一下**MediaStore.EXTRA_OUTPUT**这个参数，定义了一个Uri对象来指定存放图片的路径与文件名。这个设置信息是可选的，但是强烈建议添加。如果你不指定这个值，相机程序会使用默认的文件名保存图片到默认的位置，这个值可以从Intent.getData()的字段中获取到。



## 自定义Camera方式

以下内容，重点分析一下如何通过系统提供的Camera底层API，诸如Camera的打开或者称之为获取、Camera预览和Camera停止预览等，来定制相机功能，实现我们项目中特定的需求

1. ### 权限申请

参考Intent方式中的权限申请部分，不再赘述。

2. ### 代码实现

- 检查相机是否存在并可以访问

  ```java
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
  ```

- 获取相机资源

  ```java
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
  ```

- 创建一个继承自SurfaceView的preview类，并implement SurfaceHolder的接口的interface，这个类用来预览相机的动态图片。

  ```java
  public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
      private static final String TAG = "CameraPreview";
      private SurfaceHolder mHolder;
      private Camera mCamera;
  
      public CameraPreview(Context context, Camera camera) {
          super(context);
          mCamera = camera;
          mHolder = getHolder();
          mHolder.addCallback(this);
  
          // deprecated setting, but required on Android versions prior to 3.0
          mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
      }
  
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
          // The Surface has been created, now tell the camera where to draw the preview.
          try {
              mCamera.setPreviewDisplay(holder);
              mCamera.startPreview();
          } catch (IOException e) {
              Log.i(TAG, "Error setting camera preview: " + e.getMessage());
          }
      }
  
      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
          if (holder.getSurface() == null) {
              return;
          }
  
          try {
              mCamera.stopPreview();
          } catch (Exception e) {
              e.printStackTrace();
          }
  
          try {
              mCamera.setPreviewDisplay(holder);
              mCamera.startPreview();
          } catch (IOException e) {
              Log.i(TAG, "Error starting camera preview: " + e.getMessage());
          }
      }
  
      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
          //release source
      }
  }
  ```

- 相机布局文件

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:orientation="vertical">
  
      <FrameLayout
          android:id="@+id/camera_preview"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:layout_weight="1" />
  
      <Button
          android:layout_centerHorizontal="true"
          android:layout_alignParentBottom="true"
          android:id="@+id/button_capture"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:text="Capture" />
  </RelativeLayout>
  ```

- Capturing pictures监听

```java
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
```

- 保存拍摄的图片

  ```java
  /**
   *
   * @param data
   */
  public static void saveTakePicture(byte[] data) {
      //save the picture
      File pictureFile = FileUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
      if (pictureFile == null) {
          Log.i(TAG, "Error creating media file, check storage permissions");
          return;
      }
      try {
          FileOutputStream os = new FileOutputStream(pictureFile);
          os.write(data);
          os.close();
  
          Log.i(TAG, "Save Success!!");
      } catch (FileNotFoundException e) {
          Log.i(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
          Log.i(TAG, "Error accessing file: " + e.getMessage());
      }
  }
  ```

  <!--至此就基本完成自定义Camera的拍摄过程。-->

- 释放相机资源

相机硬件是一个共享资源，它必须被小心谨慎的管理使用，因此你的程序不应该和其他可能使用相机硬件的程序有冲突。

当你的程序执行完任务后，需要使用camers.realease()方法来释放相机对象。如果你的相机没有合理的释放相机，后续包括你自己的应用在内的所有的相机应用，都将无法正常打开相机并且可能导致程序崩溃。

```java
@Override
protected void onPause() {
    super.onPause();
    CameraManger.getInstance().releaseCamera(mCamera);
}
```

<!--在程序进入到pause状态时，立即释放相机资源。-->

```java
/**
 * 释放相机资源
 */
public void releaseCamera(Camera camera) {
    if (camera != null) {
        camera.release();      //release the camera for other applications
        camera = null;
    }
}
```

- 相机功能扩展

  Android提供了控制相机特性的方法，如图片格式化，闪光灯模式，设置聚焦等等。这里只是用设置聚焦举例，更多特性，还需要读者自行阅读官方文档实现。

  ```java
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
  ```

  

  此致，希望可以对正在研究如何自定义Camera的小伙伴提供一些思路和帮助! 

  **最后，别忘了start哟~**

  

  

  

  

  

  

  

  

  

  

  

  