/*
 * Copyright (C) 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.facade;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.quseit.util.StringUtils;

import org.qpython.qsl4a.QSL4APP;
import org.qpython.qsl4a.qsl4a.LogUtil;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTaskExecutor;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.util.PermissionUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import util.FileUtil;

/**
 * A facade for recording media.
 * 
 * Guidance notes: Use e.g. '/sdcard/file.ext' for your media destination file. A file extension of
 * mpg will use the default settings for format and codec (often h263 which won't work with common
 * PC media players). A file extension of mp4 or 3gp will use the appropriate format with the (more
 * common) h264 codec. A video player such as QQPlayer (from the android market) plays both codecs
 * and uses the composition matrix (embedded in the video file) to correct for image rotation. Many
 * PC based media players ignore this matrix. Standard video sizes may be specified.
 * 
 * @author Felix Arends (felix.arends@gmail.com)
 * @author Damon Kohler (damonkohler@gmail.com)
 * @author John Karwatzki (jokar49@gmail.com)
 */
public class MediaRecorderFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private static String basepath;
  private final Handler mHandler;
  private final Service mService;
  static Intent intentMP;
  static int resultCodeMP;
  public static MediaProjectionManager mediaProjectionManager;
  public static MediaProjection mediaProjection;
  static MediaRecorder mMediaRecorder;
  static Camera camera;
  static final int SAMPLE_RATE_IN_HZ = 8000;
  static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
          AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
  public static CountDownLatch countDownLatch;
  AudioRecord mAudioRecord;
  boolean isGetVoiceRun;
  boolean soundDbMedia;
  final Object mLock;
  final double[] volume = {-255};

  public MediaRecorderFacade(FacadeManager manager) {
    super(manager);
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    basepath = mAndroidFacade.basepath;
    mHandler = mAndroidFacade.mHandler;
    mService = mAndroidFacade.mService;
    mLock = new Object();
  }

  @Rpc(description = "Records audio from the microphone and saves it to the given location.")
  public String recorderStartMicrophone(
          @RpcParameter(name = "targetPath") @RpcOptional String path)
          throws Exception {
    if (path == null) {
      path = basepath + "/Sounds/Recorder/" /*存放录音的文件夹*/ + StringUtils.getDateStr() + ".amr";//音频命名
    }
    FileUtil.fileAutoMkParent(path);
    startAudioRecording(path);
    return path;
  }

  private void startAudioRecording(String targetPath) throws Exception {
    mMediaRecorder = new MediaRecorder();
    setAudioSource();
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
    mMediaRecorder.setOutputFile(targetPath);
    mMediaRecorder.prepare();
    mMediaRecorder.start();
  }

  private void setAudioSource() throws Exception {
    try {
      mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    } catch(Exception e){
      throw new Exception("Please check Microphone Permission .\n"+e);
    }
  }

  @Rpc(description = "Stops a previously started recording.")
  public void recorderStop() {
    mMediaRecorder.stop();
    mMediaRecorder.reset();
    mMediaRecorder.release();
    if (mediaProjection!=null){
      mediaProjection.stop();
      mediaProjection=null;
    } else if (camera!=null) {
      camera.lock();
      camera.stopPreview();
      camera.release();
      camera=null;
    }
  }

  @Rpc(description = "Pause a previously started recording.")
  public void recorderPause() throws Exception {
    //if (Build.VERSION.SDK_INT<24) {
    //  throw new Exception("recorderPause need Android >= 7.0 .");
    //} else {
    mMediaRecorder.pause();
    // }
  }

  @Rpc(description = "Resume a previously paused recording.")
  public void recorderResume() throws Exception {
    //if (Build.VERSION.SDK_INT < 24) {
    // throw new Exception("recorderResume need Android >= 7.0 .");
    //} else {
    mMediaRecorder.resume();
    // }
  }

  @Override
  public void shutdown() {
    //mMediaRecorder.release();
  }

  // TODO(damonkohler): This shares a lot of code with the CameraFacade. It's probably worth moving
  // it there.
  private FutureActivityTask<Exception> prepare() throws Exception {
    FutureActivityTask<Exception> task = new FutureActivityTask<Exception>() {
      @SuppressWarnings("deprecation")
      @Override
      public void onCreate() {
        super.onCreate();
        final SurfaceView view = new SurfaceView(getActivity());
        getActivity().setContentView(view);
        getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        view.getHolder().addCallback(new Callback() {
          @Override
          public void surfaceDestroyed(SurfaceHolder holder) {
          }

          @Override
          public void surfaceCreated(SurfaceHolder holder) {
            try {
              mMediaRecorder.setPreviewDisplay(view.getHolder().getSurface());
              //camera.startPreview();
              mMediaRecorder.prepare();
              setResult(null);
            } catch (IOException e) {
              setResult(e);
            }
          }

          @Override
          public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
          }
        });
      }
    };

    FutureActivityTaskExecutor taskExecutor =
            ((QSL4APP) mService.getApplication()).getTaskExecutor();
    taskExecutor.execute(task);

    Exception e = task.getResult();
    if (e != null) {
      throw e;
    }
    return task;
  }

  //乘着船 添加
  @SuppressLint("Range")
  @Rpc(description = "Record Audio with system soundrecorder .")
  public String recordAudio(
  ) throws Exception {
    Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
    Intent intentR = mAndroidFacade.startActivityForResultCode(intent);
    switch (intentR.getIntExtra("RESULT_CODE", -1025)) {
      case -1025:
        throw new Exception(intentR.getStringExtra("EXCEPTION"));
      case Activity.RESULT_OK:
        Uri uriRecorder = intentR.getData();
        Cursor cursor = context.getContentResolver().query(uriRecorder, null, null, null, null);
        String recPath;
        if (cursor.moveToNext()) {
          /* _data：文件的绝对路径 ，_display_name：文件名 */
          recPath = cursor.getString(cursor.getColumnIndex("_data"));
          if (recPath.startsWith("/mnt/media_rw/")) {
            recPath = "/storage/" + recPath.substring(14);
          }
        } else recPath = null;
        cursor.close();
        return recPath;
      default:
        throw new Exception("record Audio unknown Exception .");
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static class CaptureScreenService extends Service {
    private int resultCode;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
      return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

      String SCREEN_CAPTURE = "ScreenCapture";
      Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器

      /*以下是对Android 8.0的适配*/
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //普通notification适配
        builder.setChannelId(SCREEN_CAPTURE);
        //前台服务notification适配
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(SCREEN_CAPTURE, SCREEN_CAPTURE, NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
      }

      Notification notification = builder.build(); // 获取构建好的Notification
      notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
      startForeground(100, notification);

      resultCode = intent.getIntExtra("RESULT_CODE", -1);
      intent.removeExtra("RESULT_CODE");

      mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,intent);

      countDownLatch.countDown();
      return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      stopForeground(true);
    }
  }


  @SuppressLint("InlinedApi")
  private void createMediaProjection() throws Exception {
    PermissionUtil.checkPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,34);
    mediaProjectionManager = ((MediaProjectionManager) mService.getSystemService(Context.MEDIA_PROJECTION_SERVICE));
    Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
    if (intentMP==null){
    intentMP = mAndroidFacade.startActivityForResultCode(permissionIntent);
    resultCodeMP = intentMP.getIntExtra("RESULT_CODE", -1025);
    if (resultCodeMP != Activity.RESULT_OK)
      throw new Exception("MediaProjection Result Code not OK .");
    }
    if (intentMP == null)
      throw new Exception("Null MediaProjection Permission Intent .");
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
      //启动前台服务
      Intent service = new Intent(context, CaptureScreenService.class);
      service.putExtras(intentMP);
      countDownLatch = new CountDownLatch(1);
      context.startForegroundService(service);
      countDownLatch.await(1000,TimeUnit.MILLISECONDS);
    } else {
      mediaProjection = mediaProjectionManager.getMediaProjection(resultCodeMP, intentMP);
    }
    if (mediaProjection == null)
      throw new Exception("Null MediaProjection .");
  }

  private void createMediaRecorder(String path, boolean audio, int quality, int screenWidth, int screenHeight) throws Exception {
    mMediaRecorder = new MediaRecorder();
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    if (audio) setAudioSource();
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    //int screenWidth = dm.widthPixels;
    //int screenHeight = dm.heightPixels;
    mMediaRecorder.setOutputFile(path);
    mMediaRecorder.setVideoSize(screenWidth, screenHeight);  //after setVideoSource(), setOutFormat()
    if (quality == 0) {
      mMediaRecorder.setVideoEncodingBitRate(screenWidth * screenHeight);
      mMediaRecorder.setVideoFrameRate(30);
    } else {
      mMediaRecorder.setVideoEncodingBitRate(5 * screenWidth * screenHeight);
      mMediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
    }
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
    if (audio)
      mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  //after setOutputFormat()
    try {
      mMediaRecorder.prepare();
    } catch (Exception e) {
      // TODO Auto-generated catch block
    throw new Exception(e.toString());
    }
  }

  @Rpc(description = "Record screen to a file .")
  public String recorderStartScreenRecord(
          @RpcParameter(name = "path") @RpcOptional String path,
          @RpcParameter(name = "audio") @RpcDefault("true") Boolean audio,
          @RpcParameter(name = "quality") @RpcDefault("1") Integer quality,
          @RpcParameter(name = "rotation") @RpcDefault("false") Boolean rotation,
          @RpcParameter(name = "autoStart") @RpcDefault("true") Boolean autoStart
  )
          throws Exception {
    createMediaProjection();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    if (path == null) {
      path = basepath + "/Pictures/Screenshots/" /*存放截屏的文件夹*/ + StringUtils.getDateStr() + ".mp4";//视频命名
    }
    FileUtil.fileAutoMkParent(path);
    int screenWidth, screenHeight;
    if (rotation) {
      screenHeight = dm.widthPixels;
      screenWidth = dm.heightPixels;
    } else {
      screenWidth = dm.widthPixels;
      screenHeight = dm.heightPixels;
    }
    createMediaRecorder(path, audio, quality, screenWidth, screenHeight);
    mediaProjection.createVirtualDisplay("SL4A", screenWidth, screenHeight, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    if(autoStart)mMediaRecorder.start();
    return path;
  }

  @Rpc(description = "Start Media Recorder .")
  public void recorderStart() {
    mMediaRecorder.start();
  }

  @Rpc(description = "Capture ScreenShot .")
  public String imageReaderGetScreenShot(
          @RpcParameter(name = "path") @RpcOptional String path,
          @RpcParameter(name = "delayMilliSec") @RpcDefault("1000") Integer delayMilliSec
        ) throws Exception {
  createMediaProjection();
  DisplayMetrics dm = context.getResources().getDisplayMetrics();
  if (path == null) {
    path = basepath + "/Pictures/Screenshots/" /*存放截屏的文件夹*/ + StringUtils.getDateStr() + ".jpg";//图片命名
  }
  FileUtil.fileAutoMkParent(path);
  int screenWidth = dm.widthPixels;
  int screenHeight = dm.heightPixels;
  ImageReader mImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
  VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("SL4A", screenWidth, screenHeight,
          dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
          mImageReader.getSurface(), null, mHandler);

  String finalPath = path;
  String[] errInfo = {"Image not Available"};
  mHandler.postDelayed(new Runnable() {
    @Override
    public void run() {
    try {
    errInfo[0] = "";
    Image image = mImageReader.acquireLatestImage();
    if (image != null) {
      final Image.Plane[] planes = image.getPlanes();
      final ByteBuffer buffer = planes[0].getBuffer();
      int width = image.getWidth();
      int height = image.getHeight();
      int pixelStride = planes[0].getPixelStride();
      int rowStride = planes[0].getRowStride();
      int rowPadding = rowStride - pixelStride * width;
      Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
      bitmap.copyPixelsFromBuffer(buffer);
      bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
      if (bitmap != null) {
          FileOutputStream fos = new FileOutputStream(finalPath);
          bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
        fos.close();
        bitmap.recycle();
      } else {
        errInfo[0]="Bitmap Null";
      }
    } else {
      errInfo[0]="Image Null";
    }
    if (image != null) {
      image.close();
    }
  mImageReader.close();
  if (virtualDisplay != null) {
      virtualDisplay.release();
    } else {
    errInfo[0] = "VirtualDisplay Null";
  }
    //必须代码，否则出现BufferQueueProducer: [ImageReader] dequeueBuffer: BufferQueue has been abandoned
    mImageReader.setOnImageAvailableListener(null, null);
    mediaProjection.stop();
  } catch (Exception e) {
    errInfo[0]=e.toString();
}}},delayMilliSec);
  return path;
 }

  @Rpc(description = "Records video from the camera and saves it to the given location.")
  public String recorderCaptureVideo(
          @RpcParameter(name = "targetPath") @RpcOptional String targetPath,
          //default duration 10 seconds
          @RpcParameter(name = "duration") @RpcDefault("10") Integer duration,
          //cameraId: back==0, front==1
          @RpcParameter(name = "cameraId") @RpcDefault("0") Integer cameraId,
          //CamcorderProfile.QUALITY_2160P == 8
          @RpcParameter(name = "quality") @RpcDefault("8") Integer quality
  ) throws Exception {
    if(targetPath == null)
      targetPath = basepath + "/DCIM/" + StringUtils.getDateStr() + ".mp4";//视频命名
    FileUtil.fileAutoMkParent(targetPath);
    int ms = convertSecondsToMilliseconds(duration);
      startVideoRecording(new File(targetPath), ms, cameraId, quality);
    return targetPath;
  }

  private void startVideoRecording(File file, int milliseconds, int cameraId, int quality) throws Exception {
    camera = Camera.open(cameraId);

    try {
      //Method method = camera.getClass().getMethod("setDisplayOrientation", int.class);
      //method.invoke(camera, 90);
      camera.setDisplayOrientation(90);
    } catch (Exception e) {
      LogUtil.e(e);
    }

    mMediaRecorder = new MediaRecorder();
    camera.unlock();
      mMediaRecorder.setCamera(camera);
      setAudioSource();
      //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mMediaRecorder.setProfile(CamcorderProfile.get(quality));
      //mMediaRecorder.setVideoSize(width,height);
    mMediaRecorder.setOutputFile(file.getAbsolutePath());
    if (milliseconds > 0) {
      mMediaRecorder.setMaxDuration(milliseconds);
    }
    FutureActivityTask<Exception> prepTask = prepare();
    mMediaRecorder.start();
    if (milliseconds > 0) {
      new CountDownLatch(1).await(milliseconds, TimeUnit.MILLISECONDS);
    }
    prepTask.finish();
    recorderStop();
  }

  private int convertSecondsToMilliseconds(Integer seconds) {
    if (seconds == null) {
      return 0;
    }
    return (int) (seconds * 1000L);
  }

  @Rpc(description = "Recorder Sound Volumn Get Db .")
    public double recorderSoundVolumeGetDb() {
      return volume[0];
  }

  @Rpc(description = "Recorder Sound Volumn Detect .")
  public boolean recorderSoundVolumeDetect(
          //interval > 0 --> start to detect sound volume decibel according to the time interval
          //interval <= 0 --> stop to detect sound volume decibel
          @RpcParameter(name = "interval") @RpcDefault("100") Integer interval
  ) throws Exception {
      if(interval>0){
      if (isGetVoiceRun) {
        throw new Exception("Recording, please wait ……");
      }
      PermissionUtil.checkPermission(Manifest.permission.RECORD_AUDIO);
      mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
              SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
              AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        try {
          mAudioRecord.startRecording();
          soundDbMedia = false;
        } catch (Exception e){
          soundDbMedia = true;
        }
      isGetVoiceRun = true;

      new Thread(() -> {
        while (isGetVoiceRun) {
          if(soundDbMedia)
            volume[0] = getMediaDb();
          else volume[0] = getAudioDb();
          //每interval毫秒1次
          synchronized (mLock) {
            try {
              mLock.wait(interval);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
        try {
          mAudioRecord.stop();
          mAudioRecord.release();
        } catch (Exception ignored){}
        mAudioRecord = null;
        volume[0] = -255;
      }).start();
    } else {
        isGetVoiceRun = false;
      }
      return soundDbMedia;
  }

  private double getMediaDb(){
    int am;
    try {
      am = mMediaRecorder.getMaxAmplitude();
      if (am <= 0) {
        mAudioRecord.startRecording();
        return -128;
      } else return 20 * Math.log10(am);
    } catch (Exception e){
      try {
        mAudioRecord.startRecording();
      } catch (Exception ignored){}
        soundDbMedia = false;
      return -127;
    }
  }

  private double getAudioDb(){
    short[] buffer = new short[BUFFER_SIZE];
    //r是实际读取的数据长度，一般而言r会小于buffersize
    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
    long v = 0;
    // 将 buffer 内容取出，进行平方和运算
    for (short value : buffer) {
      v += value * value;
    }
    if (v <= 0) {
      soundDbMedia = true;
      return -128;
    } else {
      // 平方和除以数据总长度，得到音量大小。
      double mean = v / (double) r;
      return 10 * Math.log10(mean);
    }
  }
  }
