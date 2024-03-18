package org.qpython.qpy.main.auxActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quseit.util.StringUtils;

import org.qpython.qpy.R;
import org.qpython.qsl4a.qsl4a.facade.MediaRecorderFacade;
import org.qpython.qsl4a.qsl4a.util.PermissionUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScreenRecordActivity extends Activity {
  //常量
  final static int START = R.string.start_record;
  final static int STOP = R.string.stop_record;
  final static int START2 = R.string.start_record_step_2;
  final static int EXIT = R.string.exit_window;
  final static int CONTINUE = 100;
  final static int WAIT = 101;

  //状态量
  static int start = START;
  static int exit = EXIT;
  static String basepath;
  static String path = "";
  static boolean rotation = false;
  static MediaRecorder mMediaRecorder;
  static MediaProjectionManager mediaProjectionManager;
  static int permissionResultCode;
  static Intent permissionIntent;
  static MediaProjection mediaProjection;
  //临时量
  private EditText Path = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    TextView Hint = new TextView(this);
    Path = new EditText(this);
    Button Start = new Button(this);
    Start.setAllCaps(false);
    Button Exit = new Button(this);
    Exit.setAllCaps(false);
    Button Play = new Button(this);
    Play.setAllCaps(false);
    Hint.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);
    Hint.setText(getString(R.string.screen_record_video_path));
    root.addView(Hint, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    Path.setText(path);
    if (path.equals("") || start!=START) buttonEnable(Play,false);
    Path.setHint(R.string.auto_generate);
    Path.setOnClickListener(v -> {
              if (!Path.getText().toString().equals(path)){
                Path.setText(path);
              }});
    root.addView(Path, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    CheckBox Rotation = new CheckBox(this);
    Rotation.setText(getString(R.string.horizontal_screen));
    Rotation.setChecked(rotation);
    if (rotation && start!=START) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    if (start==START) Rotation.setEnabled(true);
    else Rotation.setEnabled(false);
    Rotation.setOnClickListener(v -> {
      rotation = Rotation.isChecked();
    });
    root.addView(Rotation, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    Start.setText(getString(start));
    Start.setOnClickListener(v -> {
      try {
          if (start==STOP) {
            start = START;
            Start.setText(getString(START));
            recorderStop();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Rotation.setEnabled(true);
            if (!path.equals("")) {
              buttonEnable(Play,true);
            }
          } else {
            if (rotation){
              if (start==START2) {
                start = STOP;
                Start.setText(getString(STOP));
                mMediaRecorder.start();
              }
              else {
                recorderStartScreenRecord();
                start = START2;
                Start.setText(getString(START2));
                Path.setText(path);
            }
              setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            else {
            start = STOP;
            Start.setText(getString(STOP));
            recorderStartScreenRecord();
            Path.setText(path);
          }
            Rotation.setEnabled(false);
            buttonEnable(Play,false);
          }
      } catch (Exception e) {
        showErr(e);
      }
    });
    root.addView(Start, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    Exit.setText(getString(exit));
    Exit.setOnClickListener(v -> {
      Finish();
      });
    root.addView(Exit, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    Play.setText(getString(R.string.play_video));
    Play.setOnClickListener(v -> {
      if (path != null && start==START && (new File(path)).exists()) {
        Intent intent = new Intent(this,org.qpython.qpy.main.activity.VideoActivity.class);
        intent.putExtra("path",path);
        startActivity(intent);
      }
    });
    root.addView(Play, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    recorderStartScreenRecord(WAIT);
    setContentView(root);
    if( Environment.getExternalStorageDirectory().canWrite())
      basepath = Environment.getExternalStorageDirectory().getAbsolutePath();
    else
      basepath = this.getExternalFilesDir("").getAbsolutePath();
  }

  private void recorderStartScreenRecord() {
    recorderStartScreenRecord(CONTINUE);
  }
  private void recorderStartScreenRecord(int requestCode) {
    if (permissionIntent!=null) {
      recorderContinueScreenRecord(requestCode);
      return;
    }
    mediaProjectionManager = ((MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE));
    MediaRecorderFacade.mediaProjectionManager = mediaProjectionManager;
    Intent permissionIntent = mediaProjectionManager.createScreenCaptureIntent();
    startActivityForResult(permissionIntent, requestCode);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent PermissionIntent) {
    if(PermissionIntent == null) {
      finish();
      return;
    }
    permissionResultCode = resultCode;
    permissionIntent = PermissionIntent;
    permissionIntent.putExtra("RESULT_CODE",resultCode);
    if (resultCode == RESULT_OK)
        recorderContinueScreenRecord(requestCode);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        createMediaProjection();
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }

  private void recorderContinueScreenRecord(int requestCode){
    if (requestCode == WAIT) return;
    try {
      createMediaProjection();
    DisplayMetrics dm = this.getResources().getDisplayMetrics();
    path = basepath + "/Pictures/Screenshots/"; /*存放截屏的文件夹*/
    File _path = new File(path);
    if (!_path.exists()) {
      _path.mkdirs();
    }
    path += StringUtils.getDateStr() + ".mp4";//视频命名
    int screenWidth, screenHeight;
    if (rotation) {
      screenHeight = dm.widthPixels;
      screenWidth = dm.heightPixels;
    } else {
      screenWidth = dm.widthPixels;
      screenHeight = dm.heightPixels;
    }
    createMediaRecorder(path, screenWidth, screenHeight);
    mediaProjection.createVirtualDisplay("QPyMain", screenWidth, screenHeight, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    if (!rotation) mMediaRecorder.start();
    Path.setText(path);
  } catch (Exception e){
      showErr(e);
    }}

  @SuppressLint("InlinedApi")
  private void createMediaProjection() throws Exception {
    PermissionUtil.checkPermission(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,34);
    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
      MediaRecorderFacade.mediaProjectionManager = mediaProjectionManager;
      //启动前台服务
      Intent service = new Intent(this, MediaRecorderFacade.CaptureScreenService.class);
      service.putExtras(permissionIntent);
      MediaRecorderFacade.countDownLatch = new CountDownLatch(1);
      this.startForegroundService(service);
      MediaRecorderFacade.countDownLatch.await(1000, TimeUnit.MILLISECONDS);
      mediaProjection = MediaRecorderFacade.mediaProjection;
    } else {
      mediaProjection = mediaProjectionManager.getMediaProjection(permissionResultCode, permissionIntent);
    }
    if (mediaProjection == null) {
      throw new Exception("Null MediaProjection .");
    }
  }

  private void createMediaRecorder(String path, int screenWidth, int screenHeight) {
    mMediaRecorder = new MediaRecorder();
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mMediaRecorder.setOutputFile(path);
    mMediaRecorder.setVideoSize(screenWidth, screenHeight);  //after setVideoSource(), setOutFormat()
    mMediaRecorder.setVideoEncodingBitRate(5 * screenWidth * screenHeight);
    mMediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  //after setOutputFormat()
    try {
      mMediaRecorder.prepare();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      showErr(e);
    }
  }

  private void recorderStop() {
    mMediaRecorder.stop();
    mMediaRecorder.reset();
    mMediaRecorder.release();
    if (mediaProjection!=null){
      mediaProjection.stop();
      mediaProjection=null;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        createMediaProjection();
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }

private void showErr(Exception e) {
    Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
}

private void Finish(){
  Intent intent = new Intent();
  intent.putExtra("path",path);
  //intent.putExtra("mediaRecorder", (Parcelable) mMediaRecorder);
  setResult(RESULT_OK,intent);
  this.finish();
}

@Override
  public boolean onKeyDown(int KeyCode, KeyEvent event){
  if (KeyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
    Finish();
    return true;
  }
  return super.onKeyDown(KeyCode, event);
}

private void buttonEnable(Button button,boolean enable){
    button.setEnabled(enable);
    if (enable)
      button.setTextColor(Color.rgb(255,255,255));
    else
      button.setTextColor(Color.rgb(144,112,112));
}

}
