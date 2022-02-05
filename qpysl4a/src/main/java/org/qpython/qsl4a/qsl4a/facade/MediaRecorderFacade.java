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

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONException;
import org.qpython.qsl4a.QSL4APP;
import org.qpython.qsl4a.qsl4a.FutureActivityTaskExecutor;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

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

  private final Service mService;
  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private final String sdcard;
  private Handler mHandler;
  static Intent intentMP;
  static int resultCodeMP;
  static MediaProjection mediaProjection;
  static MediaRecorder mMediaRecorder;

  public MediaRecorderFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    sdcard = Environment.getExternalStorageDirectory().toString();
    mHandler = mAndroidFacade.mHandler;
  }

  @Rpc(description = "Records audio from the microphone and saves it to the given location.")
  public String recorderStartMicrophone(
          @RpcParameter(name = "targetPath") @RpcOptional String path)
          throws IOException {
    if (path == null) {
      path = sdcard + "/Sounds/Recorder/"; /*存放录音的文件夹*/
      File _path = new File(path);
      if (!_path.exists()) {
        _path.mkdirs();
      }
      path += new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".amr";//音频命名
    }
    startAudioRecording(path, MediaRecorder.AudioSource.MIC);
    return path;
  }

  private void startAudioRecording(String targetPath, int source) throws IOException {
    mMediaRecorder = new MediaRecorder();
    mMediaRecorder.setAudioSource(source);
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
    mMediaRecorder.setOutputFile(targetPath);
    mMediaRecorder.prepare();
    mMediaRecorder.start();
  }

  @Rpc(description = "Stops a previously started recording.")
  public void recorderStop() {
    mMediaRecorder.stop();
    mMediaRecorder.reset();
    mMediaRecorder.release();
    if (mediaProjection!=null){
      mediaProjection.stop();
      mediaProjection=null;
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
        return null;
    }
  }

  private MediaProjection createMediaProjection() throws Exception {
    MediaProjectionManager mpm = ((MediaProjectionManager) mService.getSystemService(Context.MEDIA_PROJECTION_SERVICE));
    Intent permissionIntent = mpm.createScreenCaptureIntent();
    if (intentMP==null){
    intentMP = mAndroidFacade.startActivityForResultCode(permissionIntent);
    resultCodeMP = intentMP.getIntExtra("RESULT_CODE", -1025);
    if (resultCodeMP != Activity.RESULT_OK) {
      return null;
    }}
    MediaProjection mp = mpm.getMediaProjection(resultCodeMP, intentMP);
    if (mp == null) {
      throw new Exception("Null MediaProjection .");
    }
    return mp;
  }

  private void createMediaRecorder(String path, boolean audio, int quality, int screenWidth, int screenHeight) throws Exception {
    mMediaRecorder = new MediaRecorder();
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    if (audio) mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
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
    mediaProjection = createMediaProjection();
    if (mediaProjection == null) {
      return null;
    }
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    if (path == null) {
      path = sdcard + "/Pictures/Screenshots/"; /*存放截屏的文件夹*/
      File _path = new File(path);
      if (!_path.exists()) {
        _path.mkdirs();
      }
      path += new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".mp4";//视频命名
    }
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

  @Rpc(description = "Starts an activity for screen record .")
  public String screenRecord() throws JSONException {
    Intent intent = mAndroidFacade.startActivityForResult(
            "android.intent.action.VIEW", null, null, null,
            context.getPackageName(), "org.qpython.qpy.main.auxActivity.ScreenRecordActivity");
    //mMediaRecorder = intent.getParcelableExtra("mediaRecorder");
    return intent.getStringExtra("path");
  }

  @Rpc(description = "Capture ScreenShot .")
  public String imageReaderGetScreenShot(
          @RpcParameter(name = "path") @RpcOptional String path,
          @RpcParameter(name = "delayMilliSec") @RpcDefault("1000") Integer delayMilliSec
        ) throws Exception {
  MediaProjection mediaProjection = createMediaProjection();
  if (mediaProjection == null) {
    return null;
  }
  DisplayMetrics dm = context.getResources().getDisplayMetrics();
  if (path == null) {
    path = sdcard + "/Pictures/Screenshots/"; /*存放截屏的文件夹*/
    File _path = new File(path);
    if (!_path.exists()) {
      _path.mkdirs();
    }
    path += new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".jpg";//图片命名
  }
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
  /*if(!errInfo[0].equals("")){
    throw new Exception(errInfo[0]);
  }*/
  return path;
  }
  }


