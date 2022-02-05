/*
 * Copyright (C) 2010 Google Inc.
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
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Access Camera functions.
 * 
 */
public class CameraFacade extends RpcReceiver {

  private final Service mService;
 // private final Parameters mParameters;

  private final String sdcard;
  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private final String qpyProvider;

  /*private class BooleanResult {
    boolean mmResult = false;
  }*/

  public CameraFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    /*
    Camera camera = Camera.open();
    try {
      mParameters = camera.getParameters();
    } finally {
      camera.release();
    }*/
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    sdcard = Environment.getExternalStorageDirectory().toString();
    context = mAndroidFacade.context;
    qpyProvider = mAndroidFacade.qpyProvider;
  }

  /* 乘着船：过时淘汰
  @Rpc(description = "Take a picture and save it to the specified path.", returns = "A map of Booleans autoFocus and takePicture where True indicates success.")
  public Bundle cameraCapturePicture(@RpcParameter(name = "targetPath") final String targetPath,
      @RpcParameter(name = "useAutoFocus") @RpcDefault("true") Boolean useAutoFocus)
      throws InterruptedException {
    final BooleanResult autoFocusResult = new BooleanResult();
    final BooleanResult takePictureResult = new BooleanResult();

    Camera camera = Camera.open();
    camera.setParameters(mParameters);

    try {
      Method method = camera.getClass().getMethod("setDisplayOrientation", int.class);
      method.invoke(camera, 90);
    } catch (Exception e) {
      LogUtil.e(e);
    }

    try {
      FutureActivityTask<SurfaceHolder> previewTask = setPreviewDisplay(camera);
      camera.startPreview();
      if (useAutoFocus) {
        autoFocus(autoFocusResult, camera);
      }
      takePicture(new File(targetPath), takePictureResult, camera);
      previewTask.finish();
    } catch (Exception e) {
      LogUtil.e(e);
    } finally {
      camera.release();
    }

    Bundle result = new Bundle();
    result.putBoolean("autoFocus", autoFocusResult.mmResult);
    result.putBoolean("takePicture", takePictureResult.mmResult);
    return result;
  }

  private FutureActivityTask<SurfaceHolder> setPreviewDisplay(Camera camera) throws IOException,
      InterruptedException {
    FutureActivityTask<SurfaceHolder> task = new FutureActivityTask<SurfaceHolder>() {
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
            setResult(view.getHolder());
          }

          @Override
          public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
          }
        });
      }
    };
    FutureActivityTaskExecutor taskQueue =
        ((QSL4APP) mService.getApplication()).getTaskExecutor();
    taskQueue.execute(task);
    camera.setPreviewDisplay(task.getResult());
    return task;
  }

  private void takePicture(final File file, final BooleanResult takePictureResult,
      final Camera camera) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    camera.takePicture(null, null, new PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, Camera camera) {
        if (!FileUtils.makeDirectories(file.getParentFile(), 0755)) {
          takePictureResult.mmResult = false;
          return;
        }
        try {
          FileOutputStream output = new FileOutputStream(file);
          output.write(data);
          output.close();
          takePictureResult.mmResult = true;
        } catch (FileNotFoundException e) {
          LogUtil.e("Failed to save picture.", e);
          takePictureResult.mmResult = false;
          return;
        } catch (IOException e) {
          LogUtil.e("Failed to save picture.", e);
          takePictureResult.mmResult = false;
          return;
        } finally {
          latch.countDown();
        }
      }
    });
    latch.await();
  }

  private void autoFocus(final BooleanResult result, final Camera camera)
      throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    {
      camera.autoFocus(new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
          result.mmResult = success;
          latch.countDown();
        }
      });
      latch.await();
    }
  }

  @Rpc(description = "Starts the image capture application to take a picture and saves it to the specified path.")
  public void cameraInteractiveCapturePicture(
      @RpcParameter(name = "targetPath") final String targetPath) {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    File file = new File(targetPath);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));

    AndroidFacade facade = mManager.getReceiver(AndroidFacade.class);
    if (intent.resolveActivity(mService.getPackageManager())!=null) {
      facade.startActivityForResult(intent);
    } else {
      LogUtil.e("No camera found");
    }
  }

   */

  @Override
  public void shutdown() {
    // Nothing to clean up.
  }

  //乘着船 添加
  @Rpc(description = "Take Picture with system camera .")
  public String takePicture(
          @RpcParameter(name = "path") @RpcOptional String path
  ) throws Exception {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    String fn;
    File out;
    if (path == null) {
      String imgPath = sdcard + "/DCIM/";//存放照片的文件夹
      fn = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".jpg";//照片命名
      out = new File(imgPath);
      if (!out.exists()) {
        out.mkdirs();
      }
      out = new File(imgPath, fn);
      fn = imgPath + fn;//该照片的绝对路径
    } else {
      fn = path;
      out = new File(fn);
    }
    Uri uri;
   // if (Build.VERSION.SDK_INT>=24){
      uri = FileProvider.getUriForFile(context,qpyProvider,out);
    //} else {
    //  uri = Uri.fromFile(out);
    //}
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    Intent intentR = mAndroidFacade.startActivityForResultCode(intent);
    switch (intentR.getIntExtra("RESULT_CODE",-1025)){
      case -1025:
        throw new Exception(intentR.getStringExtra("EXCEPTION"));
      case Activity.RESULT_OK:
        return fn;
      default:
        return null;
    }
  }

  @Rpc(description = "Take Video with system camera .")
  public String takeVideo(
          @RpcParameter(name = "path") @RpcOptional String path,
          @RpcParameter(name = "quality") @RpcDefault("1") Integer quality
  ) throws Exception {
    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    String fn;
    File out;
    if (path == null) {
      String vidPath = sdcard + "/DCIM/";//存放照片的文件夹
      fn = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".mp4";//视频命名
      out = new File(vidPath);
      if (!out.exists()) {
        out.mkdirs();
      }
      out = new File(vidPath, fn);
      fn = vidPath + fn;//该照片的绝对路径
    } else {
      fn = path;
      out = new File(fn);
    }
    Uri uri;
    //if (Build.VERSION.SDK_INT>=24){
      uri = FileProvider.getUriForFile(context,qpyProvider,out);
    //} else {
    //  uri = Uri.fromFile(out);
    //}
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,quality);
    Intent intentR = mAndroidFacade.startActivityForResultCode(intent);
    switch (intentR.getIntExtra("RESULT_CODE",-1025)){
      case -1025:
        throw new Exception(intentR.getStringExtra("EXCEPTION"));
      case Activity.RESULT_OK:
        return fn;
      default:
        return null;
    }
  }

}
