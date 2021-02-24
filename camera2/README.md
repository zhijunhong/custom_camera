# Android自定义Camera2相机

## 写在前面

Google从*Android 5.0 L(API 21)* 版本，开始引入`Camera2(android.hardware.camera2)`以取代`Camera1(android.hardware.Camera)`相机框架。

Camera2相比于之前的Camera1架构完全不同，使用起来比较复杂，与此同时功能也变得非常强大。

此篇博客，能够帮助你快速构建并理解自定义Camera2相机的关键步骤。



**完整代码，请移步：https://github.com/zhijunhong/Camera2Sample**



## 使用Camera2的优点

通过设计框架的改造和优化，Camera2具备了以下优点:

1. 改进了新硬件的性能，使用更先进的API架构;
2. 可以获取更多的帧(预览/拍照)信息以及手动控制每一帧的参数;
3. 对Camera的控制更加完全(比如支持调整focus distance, 剪裁预览/拍照图片);
4. 支持更多图片格式(yuv/raw)以及高速连拍;
5. ......

## 自定义Camera2相机

### 一些概念

#### 1. Pipeline

Camera2的API模型被设计成一个 Pipeline（管道），它按顺序处理每一帧的请求并返回请求结果给客户端。下面这张来自官方的图展示了Pipeline的工作流程，我们会通过一个简单的例子详细解释这张图。

![img](https://upload-images.jianshu.io/upload_images/1519399-5a506d2c183d815e.png?imageMogr2/auto-orient/strip|imageView2/2/w/548/format/webp)

Pipeline示意图

为了解释上面的示意图，假设我们想要同时拍摄两张不同尺寸的图片，并且在拍摄的过程中闪光灯必须亮起来。整个拍摄流程如下：

1. 创建一个用于从Pipeline获取图片的CaptureRequest；
2. 修改CaptureRequest的闪光灯配置，让闪光灯在拍照过程中亮起来;
3. 创建两个不同尺寸的Surface用于接收图片数据，并且将它们添加到CaptureRequest中;
4. 发送配置好的CaptureRequest到Pipeline中等待它返回拍照结果。

一个新的CaptureRequest会被放入一个被称作Pending Request Queue的队列中等待被执行，当In-Flight Capture Queue队列空闲的时候就会从Pending Request Queue获取若干个待处理的CaptureRequest，并且根据每一个CaptureRequest 的配置进行Capture操作。最后我们从不同尺寸的Surface中获取图片数据并且还会得到一个包含了很多与本次拍照相关的信息的CaptureResult，流程结束。

#### 2. Supported Hardware Level

相机功能的强大与否和硬件息息相关，不同厂商对 Camera2 的支持程度也不同，所以Camera2定义了一个叫做Supported Hardware Level的重要概念，其作用是将不同设备上的Camera2根据功能的支持情况划分成多个不同级别以便开发者能够大概了解当前设备上Camera2的支持情况。截止到Android P为止，从低到高一共有LEGACY、LIMITED、FULL 和 LEVEL_3四个级别：

1. **LEGACY**：向后兼容的级别，处于该级别的设备意味着它只支持Camera1的功能，不具备任何Camera2高级特性;
2. **LIMITED**：除了支持Camera1的基础功能之外，还支持部分Camera2高级特性的级别;
3. **FULL**：支持所有Camera2的高级特性;
4. **LEVEL_3**：新增更多Camera2高级特性，例如YUV数据的后处理等。

#### 3. Capture

相机的所有操作和参数配置最终都是服务于图像捕获，例如对焦是为了让某一个区域的图像更加清晰，调节曝光补偿是为了调节图像的亮度。因此，在Camera2 里面所有的相机操作和参数配置都被抽象成Capture（捕获），所以不要简单的把Capture直接理解成是拍照，因为Capture操作可能仅仅是为了让预览画面更清晰而进行对焦而已。如果你熟悉Camera，那你可能会问 `setFlashMode()` 在哪？`setFocusMode()` 在哪？`takePicture()` 在哪？告诉你，它们都是通过Capture 来实现的。

Capture从执行方式上又被细分为【单次模式】、【多次模式】和【重复模式】三种，我们来一一解释下：

- **单次模式（One-shot）**：指的是只执行一次的Capture操作，例如设置闪光灯模式、对焦模式和拍一张照片等。多个一次性模式的Capture会进入队列按顺序执行。
- **多次模式（Burst）**：指的是连续多次执行指定的Capture操作，该模式和多次执行单次模式的最大区别是连续多次Capture期间不允许插入其他任何Capture 操作，例如连续拍摄100张照片，在拍摄这100张照片期间任何新的Capture请求都会排队等待，直到拍完100张照片。多组多次模式的Capture会进入队列按顺序执行。
- **重复模式（Repeating）**：指的是不断重复执行指定的Capture操作，当有其他模式的Capture提交时会暂停该模式，转而执行其他被模式的Capture，当其他模式的 Capture 执行完毕后又会自动恢复继续执行该模式的Capture，例如显示预览画面就是不断 Capture 获取每一帧画面。该模式的 Capture 是全局唯一的，也就是新提交的重复模式Capture会覆盖旧的重复模式Capture。

### 关键API

| CameraManager            | CameraManager是一个负责查询和建立相机连接的系统服务，它的功能不多，这里列出几个CameraManager的关键功能：<br/>1.将相机信息封装到CameraCharacteristics中，并提获取CameraCharacteristics实例的方式;<br />2.根据指定的相机ID连接相机设备；<br />3.提供将闪光灯设置成手电筒模式的快捷方式。 |
| ------------------------ | ------------------------------------------------------------ |
| CameraCharacteristics    | CameraCharacteristics 是一个只读的相机信息提供者，其内部携带大量的相机信息，包括代表相机朝向的 `LENS_FACING`；判断闪光灯是否可用的 `FLASH_INFO_AVAILABLE`；获取所有可用 AE 模式的 `CONTROL_AE_AVAILABLE_MODES` 等。如果你对Camera1比较熟悉，那么CameraCharacteristics有点像Camera1的 `Camera.CameraInfo` 或者 `Camera.Parameters`。 |
| **CameraDevice**         | **CameraDevice 代表当前连接的相机设备，它的职责有以下四个：<br />1.根据指定的参数创建 CameraCaptureSession；<br />2.根据指定的模板创建 CaptureRequest；<br />3.关闭相机设备；<br />4.监听相机设备的状态，例如断开连接、开启成功和开启失败等。<br />熟悉Camera1的人可能会说CameraDevice就是Camera1的 Camera 类，实则不是，Camera 类几乎负责了所有相机的操作，而 CameraDevice 的功能则十分的单一，就是只负责建立相机连接的事务，而更加细化的相机操作则交给了稍后会介绍的CameraCaptureSession。** |
| Surface                  | Surface 是一块用于填充图像数据的内存空间，例如你可以使用 SurfaceView 的 Surface 接收每一帧预览数据用于显示预览画面，也可以使用 ImageReader 的 Surface 接收 JPEG 或 YUV 数据。每一个 Surface 都可以有自己的尺寸和数据格式，你可以从 CameraCharacteristics 获取某一个数据格式支持的尺寸列表。 |
| **CameraCaptureSession** | **CameraCaptureSession 实际上就是配置了目标 Surface 的 Pipeline 实例，我们在使用相机功能之前必须先创建 CameraCaptureSession 实例。一个 CameraDevice 一次只能开启一个 CameraCaptureSession，绝大部分的相机操作都是通过向 CameraCaptureSession 提交一个 Capture 请求实现的，例如拍照、连拍、设置闪光灯模式、触摸对焦、显示预览画面等。** |
| CaptureRequest           | CaptureRequest 是向 CameraCaptureSession 提交 Capture 请求时的信息载体，其内部包括了本次 Capture 的参数配置和接收图像数据的 Surface。CaptureRequest 可以配置的信息非常多，包括图像格式、图像分辨率、传感器控制、闪光灯控制、3A 控制等等，可以说绝大部分的相机参数都是通过 CaptureRequest 配置的。值得注意的是每一个 CaptureRequest 表示一帧画面的操作，这意味着你可以精确控制每一帧的 Capture 操作。 |
| **CaptureResult**        | **CaptureResult 是每一次 Capture 操作的结果，里面包括了很多状态信息，包括闪光灯状态、对焦状态、时间戳等等。例如你可以在拍照完成的时候，通过 CaptureResult 获取本次拍照时的对焦状态和时间戳。需要注意的是，CaptureResult 并不包含任何图像数据，前面我们在介绍 Surface 的时候说了，图像数据都是从 Surface 获取的。** |
| ImageReader              | 用于从相机打开的通道中读取需要的格式的原始图像数据，可以设置多个ImageReader。 |

### 开发流程

![截屏2021-02-23下午5.32.48](/Users/zhijunhong/Desktop/截屏2021-02-23下午5.32.48.png)

#### 1.获取CameraManager

```kotlin
private val cameraManager: CameraManager by lazy { getSystemService(CameraManager::class.java) }
```

#### 2.获取相机信息

```kotlin
val cameraIdList = cameraManager.cameraIdList
cameraIdList.forEach { cameraId ->
    val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
    if (cameraCharacteristics.isHardwareLevelSupported(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
        if (cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT) {
            frontCameraId = cameraId
            frontCameraCharacteristics = cameraCharacteristics
        } else if (cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK) {
            backCameraId = cameraId
            backCameraCharacteristics = cameraCharacteristics
        }
    }
}
```

通过CameraManager获取到所有摄像头cameraId,通过循环判断是前摄像头（`CameraCharacteristics.LENS_FACING_FRONT`）还是后摄像头（`CameraCharacteristics.LENS_FACING_BACK`）

#### 3.初始化ImageReader

```kotlin
private var jpegImageReader: ImageReader? = null
jpegImageReader = ImageReader.newInstance(imageSize.width, imageSize.height, ImageFormat.JPEG, 5)
jpegImageReader?.setOnImageAvailableListener(OnJpegImageAvailableListener(), cameraHandler)

......

private inner class OnJpegImageAvailableListener : ImageReader.OnImageAvailableListener {

  private val dateFormat: DateFormat = SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
  private val cameraDir: String = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"

  @WorkerThread
  override fun onImageAvailable(imageReader: ImageReader) {
    val image = imageReader.acquireNextImage()
    val captureResult = captureResults.take()
    if (image != null && captureResult != null) {
      image.use {
        val jpegByteBuffer = it.planes[0].buffer// Jpeg image data only occupy the planes[0].
        val jpegByteArray = ByteArray(jpegByteBuffer.remaining())
        jpegByteBuffer.get(jpegByteArray)
        val width = it.width
        val height = it.height
        saveImageExecutor.execute {
          val date = System.currentTimeMillis()
          val title = "IMG_${dateFormat.format(date)}"// e.g. IMG_20190211100833786
          val displayName = "$title.jpeg"// e.g. IMG_20190211100833786.jpeg
          val path = "$cameraDir/$displayName"// e.g. /sdcard/DCIM/Camera/IMG_20190211100833786.jpeg
          val orientation = captureResult[CaptureResult.JPEG_ORIENTATION]
          val location = captureResult[CaptureResult.JPEG_GPS_LOCATION]
          val longitude = location?.longitude ?: 0.0
          val latitude = location?.latitude ?: 0.0

          // Write the jpeg data into the specified file.
          File(path).writeBytes(jpegByteArray)

          // Insert the image information into the media store.
          val values = ContentValues()
          values.put(MediaStore.Images.ImageColumns.TITLE, title)
          values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
          values.put(MediaStore.Images.ImageColumns.DATA, path)
          values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
          values.put(MediaStore.Images.ImageColumns.WIDTH, width)
          values.put(MediaStore.Images.ImageColumns.HEIGHT, height)
          values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation)
          values.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude)
          values.put(MediaStore.Images.ImageColumns.LATITUDE, latitude)
          contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

          // Refresh the thumbnail of image.
          val thumbnail = getThumbnail(path)
          if (thumbnail != null) {
            runOnUiThread {
              thumbnailView.setImageBitmap(thumbnail)
              thumbnailView.scaleX = 0.8F
              thumbnailView.scaleY = 0.8F
              thumbnailView.animate().setDuration(50).scaleX(1.0F).scaleY(1.0F).start()
            }
          }
        }
      }
    }
  }
}
```

`ImageReader`是获取图像数据的重要途径，通过它可以获取到不同格式的图像数据，例如JPEG、YUV、RAW等。通过`ImageReader.newInstance(int width, int height, int format, int maxImages)`创建`ImageReader`对象，有4个参数：

- width：图像数据的宽度
- height：图像数据的高度
- format：图像数据的格式，例如`ImageFormat.JPEG`，`ImageFormat.YUV_420_888`等
- maxImages：最大Image个数，Image对象池的大小，指定了能从ImageReader获取Image对象的最大值，过多获取缓冲区可能导致OOM，所以最好按照最少的需要去设置这个值

ImageReader其他相关的方法和回调：

- `ImageReader.OnImageAvailableListener`：有新图像数据的回调
- `acquireLatestImage()`：从ImageReader的队列里面，获取最新的Image，删除旧的，如果没有可用的Image，返回null
- `acquireNextImage()`：获取下一个最新的可用Image，没有则返回null
- `close()`：释放与此ImageReader关联的所有资源
- `getSurface()`：获取为当前ImageReader生成Image的Surface

#### 4.打开相机设备

```kotlin
val cameraStateCallback = CameraStateCallback()
cameraManager.openCamera(cameraId, cameraStateCallback, mainHandler)

......

 private inner class CameraStateCallback : CameraDevice.StateCallback() {
        @MainThread
        override fun onOpened(camera: CameraDevice) {
            cameraDeviceFuture!!.set(camera)
            cameraCharacteristicsFuture!!.set(getCameraCharacteristics(camera.id))
        }

        @MainThread
        override fun onClosed(camera: CameraDevice) {

        }

        @MainThread
        override fun onDisconnected(camera: CameraDevice) {
            cameraDeviceFuture!!.set(camera)
            closeCamera()
        }

        @MainThread
        override fun onError(camera: CameraDevice, error: Int) {
            cameraDeviceFuture!!.set(camera)
            closeCamera()
        }
    }
```

`cameraManager.openCamera(@NonNull String cameraId,@NonNull final CameraDevice.StateCallback callback, @Nullable Handler handler)`的三个参数:

- cameraId：摄像头的唯一标识
- callback：设备连接状态变化的回调
- handler：回调执行的Handler对象，传入null则使用当前的主线程Handler

其中CameraStateCallback回调:

- onOpened：表示相机打开成功，可以真正开始使用相机，创建Capture会话
- onDisconnected：当相机断开连接时回调该方法，需要进行释放相机的操作
- onError：当相机打开失败时，需要进行释放相机的操作
- onClosed：调用Camera.close()后的回调方法

#### 5.创建Capture会话

```kotlin
val sessionStateCallback = SessionStateCallback()
......
val cameraDevice = cameraDeviceFuture?.get()
cameraDevice?.createCaptureSession(outputs, sessionStateCallback, mainHandler)

......

 private inner class SessionStateCallback : CameraCaptureSession.StateCallback() {
        @MainThread
        override fun onConfigureFailed(session: CameraCaptureSession) {
            captureSessionFuture!!.set(session)
        }

        @MainThread
        override fun onConfigured(session: CameraCaptureSession) {
            captureSessionFuture!!.set(session)
        }

        @MainThread
        override fun onClosed(session: CameraCaptureSession) {

        }
    }
```

这段的代码核心方法是`mCameraDevice.createCaptureSession()`创建Capture会话，它接受了三个参数：

- outputs：用于接受图像数据的surface集合，这里传入的是一个preview的surface
- callback：用于监听 Session 状态的CameraCaptureSession.StateCallback对象
- handler：用于执行CameraCaptureSession.StateCallback的Handler对象，传入null则使用当前的主线程Handler

#### 6.创建CaptureRequest

CaptureRequest是向CameraCaptureSession提交Capture请求时的信息载体，其内部包括了本次Capture的参数配置和接收图像数据的Surface

```kotlin
if (cameraDevice != null) {
  previewImageRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
  captureImageRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
}

......

val cameraDevice = cameraDeviceFuture?.get()
val captureSession = captureSessionFuture?.get()
val previewImageRequestBuilder = previewImageRequestBuilder!!
val captureImageRequestBuilder = captureImageRequestBuilder!!
if (cameraDevice != null && captureSession != null) {
  val previewSurface = previewSurface!!
  val previewDataSurface = previewDataSurface
  previewImageRequestBuilder.addTarget(previewSurface)
  // Avoid missing preview frame while capturing image.
  captureImageRequestBuilder.addTarget(previewSurface)
  if (previewDataSurface != null) {
    previewImageRequestBuilder.addTarget(previewDataSurface)
    // Avoid missing preview data while capturing image.
    captureImageRequestBuilder.addTarget(previewDataSurface)
  }
  val previewRequest = previewImageRequestBuilder.build()
  captureSession.setRepeatingRequest(previewRequest, RepeatingCaptureStateCallback(), mainHandler)
}

......

private inner class RepeatingCaptureStateCallback : CameraCaptureSession.CaptureCallback() {
  @MainThread
  override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
    super.onCaptureStarted(session, request, timestamp, frameNumber)
  }

  @MainThread
  override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
    super.onCaptureCompleted(session, request, result)
  }
}
```

除了模式的配置，CaptureRequest还可以配置很多其他信息，例如图像格式、图像分辨率、传感器控制、闪光灯控制、3A(自动对焦-AF、自动曝光-AE和自动白平衡-AWB)控制等。在createCaptureSession的回调中可以进行设置，最后通过`build()`方法生成CaptureRequest对象。

#### 7.预览

Camera2中，通过连续重复的Capture实现预览功能，每次Capture会把预览画面显示到对应的Surface上。连续重复的Capture操作通过

`captureSession.setRepeatingRequest(previewRequest, RepeatingCaptureStateCallback(), mainHandler)`

实现，该方法有三个参数：

- request：CaptureRequest对象
- listener：监听Capture 状态的回调
- handler：用于执行CameraCaptureSession.CaptureCallback的Handler对象，传入null则使用当前的主线程Handler

停止预览使用`mCaptureSession.stopRepeating()`方法。

#### 8.拍照

设置上面的request，session后，就可以真正的开始拍照操作

```kotlin
val captureImageRequest = captureImageRequestBuilder.build()
captureSession.capture(captureImageRequest, CaptureImageStateCallback(), mainHandler)

......

private inner class CaptureImageStateCallback : CameraCaptureSession.CaptureCallback() {

  @MainThread
  override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
    super.onCaptureStarted(session, request, timestamp, frameNumber)
    // Play the shutter click sound.
    cameraHandler?.post { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) }
  }

  @MainThread
  override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
    super.onCaptureCompleted(session, request, result)
    captureResults.put(result)
  }
}
```

`captureSession.capture()`方法也有三个参数，和`mCaptureSession.setRepeatingRequest`一样：

- request：CaptureRequest对象
- listener：监听Capture 状态的回调
- handler：用于执行CameraCaptureSession.CaptureCallback的Handler对象，传入null则使用当前的主线程Handler

#### 9.关闭相机

和其他硬件资源的使用一样，当我们不再需要使用相机时记得调用 CameraDevice.close() 方法及时关闭相机回收资源。关闭相机的操作至关重要，因为如果你一直占用相机资源，其他基于相机开发的功能都会无法正常使用，严重情况下直接导致其他相机相关的 APP 无法正常使用，当相机被完全关闭的时候会通过 CameraStateCallback.onCllosed() 方法通知你相机已经被关闭。那么在什么时候关闭相机最合适呢？个人的建议是在 onPause() 的时候就一定要关闭相机，因为在这个时候相机页面已经不是用户关注的焦点，大部分情况下已经可以关闭相机了。

```kotlin
cameraDevice?.close()
previewDataImageReader?.close()
jpegImageReader?.close()
```

先后对CaptureSession，CameraDevice，ImageReader进行close操作，释放资源。

# 从 Camera1迁移到Camera2的建议

如果你的项目正在使用Camera1，并且打算从Camera1迁移到Camera2的话，希望以下几个建议可以对你有所帮助：

1. Camera1严格区分了预览和拍照两个流程，而Camera2则把这两个流程都抽象成了Capture行为，所以建议你不要带着过多的Camera1思维使用Camera2，避免因为思维上的束缚而无法充分利用Camera2灵活的 API；
2. 如同Camera1一样，Camera2的一些 API调用也会耗时，所以建议你使用独立的线程执行所有的相机操作，尽量避免直接在主线程调用Camera2的API，HandlerThread 是一个不错的选择；
3. 可以认为Camera1是Camera2的一个子集，也就是说Camera1能做的事情Camera2一定能做，反过来则不一定行得通；
4. 如果你的应用程序需要同时兼容Camera1 和Camera2，个人建议分开维护，因为Camera1蹩脚的API设计很可能让Camera2灵活的API无法得到充分的发挥，另外将两个设计上完全不兼容的东西搅和在一起带来的痛苦可能远大于其带来便利性，多写一些冗余的代码也许还更开心；
5. 官方说Camera2的性能会更好，但在较早期的一些机器上运行Camera2的性能并没有比Camera1好多少；
6. 当设备的 Supported Hardware Level 低于FULL的时候，建议还是使用Camera1，因为FULL级别以下的 Camera2 能提供的功能几乎和Camera1一样，所以倒不如选择更加稳定的Camera1。



完整代码：https://github.com/zhijunhong/Camera2Sample

**最后，别忘了start哟~**



## 参考：

[Android Camera-Camera2使用](http://yeungeek.github.io/2020/01/19/AndroidCamera-UsingCamera2/)

[Android Camera2 教程](https://www.jianshu.com/p/9a2e66916fcb)













