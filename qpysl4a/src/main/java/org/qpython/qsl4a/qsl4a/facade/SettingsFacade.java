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

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.qpython.qsl4a.QSL4APP;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTaskExecutor;
import org.qpython.qsl4a.qsl4a.LogUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

/**
 * Exposes phone settings functionality.
 * 
 * @author Frank Spychalski (frank.spychalski@gmail.com)
 */
public class SettingsFacade extends RpcReceiver {

  //public static int AIRPLANE_MODE_OFF = 0;
  //public static int AIRPLANE_MODE_ON = 1;

  private static byte TRAFFIC_SUPPORT = 0;
  private final int qpython_uid;

  private final Service mService;
  private final AudioManager mAudio;
  private final PowerManager mPower;
  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private final ContentResolver contentResolver;

  /**
   * Creates a new SettingsFacade.
   * 
   * param service
   *          is the {@link Context} the APIs will run under
   */
  public SettingsFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mAudio = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
    mPower = (PowerManager) mService.getSystemService(Context.POWER_SERVICE);
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    contentResolver = mService.getContentResolver();
    qpython_uid = context.getApplicationInfo().uid;
  }

  private void NotificationPolicyAccessGranted() throws Exception {
  NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {
    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
    mAndroidFacade.startActivityForResult(intent);
    if(!notificationManager.isNotificationPolicyAccessGranted()){
      throw new Exception("Need Permission of NOTIFICATION_POLICY_ACCESS_SETTINGS .");
    }
  }}

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Rpc(description = "Sets the screen timeout to this number of seconds.", returns = "The original screen timeout.")
  public Integer setScreenTimeout(@RpcParameter(name = "value") Integer value) throws Exception {
    checkWriteSettings();
    Integer oldValue = getScreenTimeout();
    android.provider.Settings.System.putInt(contentResolver,
        android.provider.Settings.System.SCREEN_OFF_TIMEOUT, value * 1000);
    return oldValue;
  }

  @Rpc(description = "Returns the current screen timeout in seconds.", returns = "the current screen timeout in seconds.")
  public Integer getScreenTimeout() {
    try {
      return android.provider.Settings.System.getInt(contentResolver,
          android.provider.Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
    } catch (SettingNotFoundException e) {
      return 0;
    }
  }

  @SuppressWarnings("deprecation")
@Rpc(description = "Checks the airplane mode setting.", returns = "True if airplane mode is enabled.")
  public Boolean checkAirplaneMode() {
    try {
      return android.provider.Settings.System.getInt(contentResolver,
          android.provider.Settings.System.AIRPLANE_MODE_ON) == 1;//AIRPLANE_MODE_ON == 1
    } catch (SettingNotFoundException e) {
      return false;
    }
  }

  /*@SuppressWarnings("deprecation")
@Rpc(description = "Toggles airplane mode on and off.", returns = "True if airplane mode is enabled.")
  public Boolean toggleAirplaneMode(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
    if (enabled == null) {
      enabled = !checkAirplaneMode();
    }
    android.provider.Settings.System.putInt(mService.getContentResolver(),
        android.provider.Settings.System.AIRPLANE_MODE_ON, enabled ? AIRPLANE_MODE_ON
            : AIRPLANE_MODE_OFF);
    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    intent.putExtra("state", enabled);
    mService.sendBroadcast(intent);
    return enabled;
  }*/

  @Rpc(description = "Checks the ringer silent mode setting.", returns = "True if ringer silent mode is enabled.")
  public Boolean checkRingerSilentMode() {
    return mAudio.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
  }

  @Rpc(description = "Toggles ringer silent mode on and off.", returns = "True if ringer silent mode is enabled.")
  public Boolean toggleRingerSilentMode(
          @RpcParameter(name = "enabled") @RpcOptional Boolean enabled
  ) throws Exception {
    NotificationPolicyAccessGranted();
    if (enabled == null) {
      enabled = !checkRingerSilentMode();
    }
    mAudio.setRingerMode(enabled ? AudioManager.RINGER_MODE_SILENT
        : AudioManager.RINGER_MODE_NORMAL);
    return enabled;
  }

  @SuppressWarnings("deprecation")
@Rpc(description = "Toggles vibrate mode on and off. If ringer=true then set Ringer setting, else set Notification setting", returns = "True if vibrate mode is enabled.")
  public Boolean toggleVibrateMode(
          @RpcParameter(name = "enabled") @RpcOptional Boolean enabled,
      @RpcParameter(name = "ringer") @RpcOptional Boolean ringer
  ) throws Exception {
    NotificationPolicyAccessGranted();
    int atype = ringer ? AudioManager.VIBRATE_TYPE_RINGER : AudioManager.VIBRATE_TYPE_NOTIFICATION;
    int asetting = enabled ? AudioManager.VIBRATE_SETTING_ON : AudioManager.VIBRATE_SETTING_OFF;
    mAudio.setVibrateSetting(atype, asetting);
    return enabled;
  }

  @SuppressWarnings("deprecation")
@Rpc(description = "Checks Vibration setting. If ringer=true then query Ringer setting, else query Notification setting", returns = "True if vibrate mode is enabled.")
  public Boolean getVibrateMode(@RpcParameter(name = "ringer") @RpcOptional Boolean ringer) {
    int atype = ringer ? AudioManager.VIBRATE_TYPE_RINGER : AudioManager.VIBRATE_TYPE_NOTIFICATION;
    return mAudio.shouldVibrate(atype);
  }

  @Rpc(description = "Returns the maximum ringer volume.")
  public int getMaxRingerVolume() {
    return mAudio.getStreamMaxVolume(AudioManager.STREAM_RING);
  }

  @Rpc(description = "Returns the current ringer volume.")
  public int getRingerVolume() {
    return mAudio.getStreamVolume(AudioManager.STREAM_RING);
  }

  @Rpc(description = "Sets the ringer volume.")
  public void setRingerVolume(
          @RpcParameter(name = "volume") Integer volume
  ) throws Exception {
    NotificationPolicyAccessGranted();
    mAudio.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
  }

  @Rpc(description = "Returns the maximum media volume.")
  public int getMaxMediaVolume() {
    return mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
  }

  @Rpc(description = "Returns the current media volume.")
  public int getMediaVolume() {
    return mAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
  }

  @Rpc(description = "Sets the media volume.")
  public void setMediaVolume(@RpcParameter(name = "volume") Integer volume) {
    mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
  }

  @Rpc(description = "Returns the screen backlight brightness.", returns = "the current screen brightness between 0 and 255")
  public Integer getScreenBrightness() {
    try {
      return android.provider.Settings.System.getInt(contentResolver,
          android.provider.Settings.System.SCREEN_BRIGHTNESS);
    } catch (SettingNotFoundException e) {
      return 0;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Rpc(description = "Sets the the screen backlight brightness.", returns = "the original screen brightness.")
  public Integer setScreenBrightness(
      @RpcParameter(name = "value", description = "brightness value between 0 and 255") @RpcOptional Integer value) throws Exception {

    checkWriteSettings();

    Integer oldValue = getScreenBrightness();

    if(value == null){
      Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
      return oldValue;
    }

    if (value < 0) {
      value = 0;
    } else if (value > 255) {
      value = 255;
    }
    final int brightness = value;

    if (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
      Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    android.provider.Settings.System.putInt(contentResolver,
        android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);

    FutureActivityTask<Object> task = new FutureActivityTask<Object>() {
      @Override
      public void onCreate() {
        super.onCreate();
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.screenBrightness = brightness * 1.0f / 255;
        getActivity().getWindow().setAttributes(lp);
        setResult(null);
        finish();
      }
    };

    FutureActivityTaskExecutor taskExecutor =
        ((QSL4APP) mService.getApplication()).getTaskExecutor();
    taskExecutor.execute(task);

    return oldValue;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public void checkWriteSettings() throws Exception {
    if (!Settings.System.canWrite(context)) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName()));
      mAndroidFacade.startActivityForResult(intent);
      if(!Settings.System.canWrite(context))
        throw new Exception("No permission : MANAGE_WRITE_SETTINGS .");
    }
  }

  @Rpc(description = "Checks if the screen is on or off (requires API level 7).", returns = "True if the screen is currently on.")
  public Boolean checkScreenOn() throws Exception {
    Class<?> powerManagerClass = mPower.getClass();
    Boolean result = null;
    try {
      Method isScreenOn = powerManagerClass.getMethod("isScreenOn");
      result = (Boolean) isScreenOn.invoke(mPower);
    } catch (Exception e) {
      LogUtil.e(e);
      throw new UnsupportedOperationException("This feature is only available after Eclair.");
    }
    return result;
  }

  @Rpc(description = "return isExternalStorageManager if Android >= 11 .")
  public Boolean isExternalStorageManager(){
    if (Build.VERSION.SDK_INT<30) return null;
    return Environment.isExternalStorageManager();
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  @Rpc(description = "Get system language and country .")
  public String getLocale() {
    return Resources.getSystem().getConfiguration().getLocales().get(0).toString();
  }

  @SuppressLint("HardwareIds")
  @Rpc(description = "get system infomation .")
  public Map<String,Object> getSysInfo(){
    Map<String,Object> s = new HashMap<>();
    s.put("model", Build.MODEL);
    s.put("sdk",Build.VERSION.SDK_INT);
    s.put("release",Build.VERSION.RELEASE);
    s.put("brand",Build.BRAND);
    s.put("device",Build.DEVICE);
    s.put("display",Build.DISPLAY);
    s.put("manufacturer",Build.MANUFACTURER);
    s.put("language", Locale.getDefault().getLanguage());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      s.put("serial",Build.getSerial());
    else
      s.put("serial",Build.SERIAL);
    s.put("hardware", Build.HARDWARE);
    s.put("user", Build.USER);
    s.put("abis", Build.SUPPORTED_ABIS);
    return s;
  }

  @Rpc(description = "get screen infomation .")
  public Map<String,Object> getScreenInfo(){
    Map<String,Object> s = new HashMap<>();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    s.put("widthPixels",dm.widthPixels);
    s.put("heightPixels",dm.heightPixels);
    s.put("densityDpi",dm.densityDpi);
    s.put("density",dm.density);
    s.put("xdpi",dm.xdpi);
    s.put("ydpi",dm.ydpi);
    s.put("scaledDensity",dm.scaledDensity);
    return s;
  }

  @Rpc(description = "Nanoseconds after system startup")
  public Long elapsedRealtimeNanos(){
    return SystemClock.elapsedRealtimeNanos();
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Rpc(description = "Show Screen Lock .")
  public Boolean showScreenLock() {
    KeyguardManager mKeyguardMgr = context.getSystemService(KeyguardManager.class);
    Intent intent = mKeyguardMgr.createConfirmDeviceCredentialIntent(null,null);
    if (intent != null) {
      intent = mAndroidFacade.startActivityForResultCode(intent);
      return intent.getIntExtra("RESULT_CODE", -1023) == RESULT_OK;
    } else return null;
  }

  @Rpc(description = "Get transmit/receive traffic statistics since startup .")
  public Map<String,Long> getTrafficStats(
          @RpcParameter(name = "flags") @RpcDefault("7") Integer flags
          // Total = 1 ; Mobile = 2 ; QPython = 4
          ){
    if(unTrafficSupport())
      return null;
    Map<String,Long> map = new HashMap<>();
    if(flags % 2 == 1){
      map.put("TotalRxBytes",TrafficStats.getTotalRxBytes());
      map.put("TotalTxBytes",TrafficStats.getTotalTxBytes());
    }
    if((flags >> 1) % 2 == 1){
      map.put("MobileRxBytes",TrafficStats.getMobileRxBytes());
      map.put("MobileTxBytes",TrafficStats.getMobileTxBytes());
    }
    if((flags >> 2) % 2 == 1){
      map.put("QPythonRxBytes",TrafficStats.getUidRxBytes(qpython_uid));
      map.put("QPythonTxBytes",TrafficStats.getUidTxBytes(qpython_uid));
    }
    map.put("StartupTime",SystemClock.elapsedRealtime());
    return map;
  }

  @Rpc(description = "get qpython Tx bytes")
  public Map<String,Long> getAppTxBytes(@RpcParameter(name = "packageName") String packageName){
    List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
    Integer uid = null;
    for (PackageInfo info : packageInfos) {
      if(Objects.equals(info.packageName, packageName))
        uid = info.applicationInfo.uid;
      break;
    }
    if(uid == null)
      return null;
    Map<String,Long> map = new HashMap<>();
    map.put("bytes",TrafficStats.getUidTxBytes(uid));
    map.put("time",SystemClock.elapsedRealtime());
    return map;
  }

  private boolean unTrafficSupport(){
    if(TRAFFIC_SUPPORT > 0)
      return false;
    else if(TRAFFIC_SUPPORT < 0)
      return true;
  if(TrafficStats.getUidRxBytes(qpython_uid) == TrafficStats.UNSUPPORTED || TrafficStats.getUidTxBytes(qpython_uid) == TrafficStats.UNSUPPORTED){
    TRAFFIC_SUPPORT = -1;
    return true;
  } else {
    TRAFFIC_SUPPORT = 1;
    return false;
  }}

  @Rpc(description = "get Memory Information")
  public Map<String,Long> getMemoryInfo(){
    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    activityManager.getMemoryInfo(mi);
    Map<String,Long> map = new HashMap<>();
    map.put("availMem",mi.availMem);
    map.put("totalMem",mi.totalMem);
    return map;
  }

  @Override
  public void shutdown() {
    // Nothing to do yet.
  }
}
