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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qsl4a.QSL4APP;
import org.qpython.qsl4a.R;
import org.qpython.qsl4a.qsl4a.LogUtil;
import org.qpython.qsl4a.qsl4a.NotificationIdFactory;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTaskExecutor;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.util.FileUtils;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;
import org.qpython.qsl4a.qsl4a.util.SPFUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Some general purpose Android routines.<br>
 * <h2>Intents</h2> Intents are returned as a map, in the following form:<br>
 * <ul>
 * <li><b>action</b> - action.
 * <li><b>data</b> - url
 * <li><b>type</b> - mime type
 * <li><b>packagename</b> - name of package. If used, requires classname to be useful (optional)
 * <li><b>classname</b> - name of class. If used, requires packagename to be useful (optional)
 * <li><b>categories</b> - list of categories
 * <li><b>extras</b> - map of extras
 * <li><b>flags</b> - integer flags.
 * </ul>
 * <br>
 * An intent can be built using the {@see #makeIntent} call, but can also be constructed exterally.
 *
 */
@SuppressWarnings("deprecation")
public class AndroidFacade extends RpcReceiver {
  /**
   * An instance of this interface is passed to the facade. From this object, the resource IDs can
   * be obtained.
   */
  public interface Resources {
    //int getLogo48();
  }

  public final Service mService;
  public final Handler mHandler;
  private final Intent mIntent;
  public final FutureActivityTaskExecutor mTaskQueue;

  private final Vibrator mVibrator;
  private final NotificationManager mNotificationManager;

  private final Resources mResources;
  private ClipboardManager mClipboard = null;
  private static int deamonCount = 0;
  public final Context context;
  public final String qpyProvider;
  public static Handler handler;

  public final int intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION  | Intent.FLAG_GRANT_WRITE_URI_PERMISSION ;

  @Override
  public void shutdown() {
  }

  public Service getmService() {
    return this.mService;
  }

  public AndroidFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mIntent = manager.getIntent();
    // River Modify
    QSL4APP application = ((QSL4APP) mService.getApplication());
    mTaskQueue = application.getTaskExecutor();
    mHandler = new Handler(mService.getMainLooper());
    mVibrator = (Vibrator) mService.getSystemService(Context.VIBRATOR_SERVICE);
    mNotificationManager =
            (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
    mResources = manager.getAndroidFacadeResources();
    //乘着船 修改
    context = mService.getApplicationContext();
    qpyProvider = context.getPackageName() + ".provider";
  }

  ClipboardManager getClipboardManager() {
    Object clipboard;
    if (mClipboard == null) {
      try {
        clipboard = mService.getSystemService(Context.CLIPBOARD_SERVICE);
      } catch (Exception e) {
        Looper.prepare(); // Clipboard manager won't work without this on higher SDK levels...
        clipboard = mService.getSystemService(Context.CLIPBOARD_SERVICE);
      }
      mClipboard = (ClipboardManager) clipboard;
      if (mClipboard == null) {
        LogUtil.w("Clipboard managed not accessible.");
      }
    }
    return mClipboard;
  }

  /**
   * Creates a new AndroidFacade that simplifies the interface to various Android APIs.
   */

  @Rpc(description = "Put text in the clipboard.")
  public void setClipboard(@RpcParameter(name = "text") String text) {
    getClipboardManager().setText(text);
  }

  @Rpc(description = "Read text from the clipboard.",
          returns = "The text in the clipboard.")
  public String getClipboard() {
    CharSequence text = getClipboardManager().getText();
    return text == null ? null : text.toString();
  }

  Intent startActivityForResult(final Intent intent) {
    FutureActivityTask<Intent> task = new FutureActivityTask<Intent>() {
      @Override
      public void onCreate() {
        super.onCreate();
        try {
          startActivityForResult(intent, 1024);
        } catch (Exception e) {
          intent.putExtra("EXCEPTION", e.toString());
          setResult(intent);
        }
      }

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(data);
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      task.finish();
    }
  }

  Intent startActivityForResultCode(final Intent intent) {
    FutureActivityTask<Intent> task = new FutureActivityTask<Intent>() {
      @Override
      public void onCreate() {
        super.onCreate();
        try {
          int requestCode;
          if (intent!=null){
            requestCode=intent.getIntExtra("REQUEST_CODE",1024);
          } else {
            requestCode=2048;
          }
          startActivityForResult(intent, requestCode);
        } catch (Exception e) {
          if(intent!=null) {
            intent.putExtra("EXCEPTION", e.toString());
            setResult(intent);
          }
        }
      }

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data==null)
          data=new Intent();
        data.putExtra("RESULT_CODE",resultCode);
        setResult(data);
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      task.finish();
    }
  }

  // TODO(damonkohler): Pull this out into proper argument deserialization and support
  // complex/nested types being passed in.
  public static void putExtrasFromJsonObject(JSONObject extras, Intent intent) throws Exception {
    JSONArray names = extras.names();
    if(names==null)
      return;
    for (int i = 0; i < names.length(); i++) {
      String name = names.getString(i);
      Object data = extras.get(name);
      //if (data == null) {
      //  continue;
      //}
      if (data instanceof Integer) {
        intent.putExtra(name, (Integer) data);
      }
     /* if (data instanceof Float) {
        intent.putExtra(name, (Float) data);
      }*/
      if (data instanceof Double) {
        intent.putExtra(name, (Double) data);
      }
      if (data instanceof Long) {
        intent.putExtra(name, (Long) data);
      }
      if (data instanceof String) {
        putExtrasFromString(intent, name, data);
      }
      if (data instanceof Boolean) {
        intent.putExtra(name, (Boolean) data);
      }
      // Nested JSONObject
      if (data instanceof JSONObject) {
        Bundle nestedBundle = new Bundle();
        intent.putExtra(name, nestedBundle);
        putNestedJSONObject((JSONObject) data, nestedBundle);
      }
      // Nested JSONArray. Doesn't support mixed types in single array
      if (data instanceof JSONArray) {
        JSONArray jdata = (JSONArray) data;
        // Empty array. No way to tell what type of data to pass on, so skipping
        if (jdata.length() == 0) {
          LogUtil.e("Empty array not supported in JSONObject, skipping");
          continue;
        }
        Object fdata = jdata.get(0);
        // Integer
        if (fdata instanceof Integer) {
          Integer[] integerArrayData = new Integer[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            integerArrayData[j] = jdata.getInt(j);
          }
          intent.putExtra(name, integerArrayData);
        }
        // Double
        if (fdata instanceof Double) {
          Double[] doubleArrayData = new Double[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            doubleArrayData[j] = jdata.getDouble(j);
          }
          intent.putExtra(name, doubleArrayData);
        }
        // Long
        if (fdata instanceof Long) {
          Long[] longArrayData = new Long[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            longArrayData[j] = jdata.getLong(j);
          }
          intent.putExtra(name, longArrayData);
        }
        // String
        if (fdata instanceof String) {
          String[] stringArrayData = new String[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            stringArrayData[j] = jdata.getString(j);
          }
          intent.putExtra(name, stringArrayData);
        }
        // Boolean
        if (fdata instanceof Boolean) {
          Boolean[] booleanArrayData = new Boolean[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            booleanArrayData[j] = jdata.getBoolean(j);
          }
          intent.putExtra(name, booleanArrayData);
        }
      }
    }
  }

  private static void putExtrasFromString(Intent intent, String name, Object data) throws Exception {
    String str = (String) data;
    if(str.length()>5 && str.charAt(0) == 0 ){
      int p = str.indexOf(0,1);
      if(p<0)
        throw new Exception("No type of special String : " + str);
      String type = str.substring(1,p);
      str = str.substring(p+1);
      if(type.equals("uri"))
        intent.putExtra(name, Uri.parse(str));
      else if(type.equals("byte"))
        intent.putExtra(name, Base64.decode(str, Base64.DEFAULT));
      else intent.putExtra(name, str);
    } else intent.putExtra(name, str);
  }

  private static void putStringToBundle(Bundle bundle, String name, Object data) throws Exception {
    String str = (String) data;
    if(str.length()>5 && str.charAt(0) == 0 ){
      int p = str.indexOf(0,1);
      if(p<0)
        throw new Exception("No type of special String : " + str);
      String type = str.substring(1,p);
      str = str.substring(p+1);
      if(type.equals("byte"))
        bundle.putByteArray(name, Base64.decode(str, Base64.DEFAULT));
      else bundle.putString(name, str);
    } else bundle.putString(name, str);
  }

  // Contributed by Emmanuel T
  // Nested Array handling contributed by Sergey Zelenev
  private static void putNestedJSONObject(JSONObject jsonObject, Bundle bundle)
          throws Exception {
    JSONArray names = jsonObject.names();
    if(names == null)
      return;
    for (int i = 0; i < names.length(); i++) {
      String name = names.getString(i);
      Object data = jsonObject.get(name);
      /*if (data == null) {
        continue;
      }*/
      if (data instanceof Integer) {
        bundle.putInt(name, (Integer) data);
      }
      /*if (data instanceof Float) {
        bundle.putFloat(name, ((Float) data).floatValue());
      }*/
      if (data instanceof Double) {
        bundle.putDouble(name, (Double) data);
      }
      if (data instanceof Long) {
        bundle.putLong(name, (Long) data);
      }
      if (data instanceof String) {
        putStringToBundle(bundle,name,data);
      }
      if (data instanceof Boolean) {
        bundle.putBoolean(name, (Boolean) data);
      }
      // Nested JSONObject
      if (data instanceof JSONObject) {
        Bundle nestedBundle = new Bundle();
        bundle.putBundle(name, nestedBundle);
        putNestedJSONObject((JSONObject) data, nestedBundle);
      }
      // Nested JSONArray. Doesn't support mixed types in single array
      if (data instanceof JSONArray) {
        // Empty array. No way to tell what type of data to pass on, so skipping
        JSONArray jdata = (JSONArray) data;
        if (jdata.length() == 0) {
          LogUtil.e("Empty array not supported in nested JSONObject, skipping");
          continue;
        }
        Object fdata = jdata.get(0);
        // Integer
        if (fdata instanceof Integer) {
          int[] integerArrayData = new int[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            integerArrayData[j] = jdata.getInt(j);
          }
          bundle.putIntArray(name, integerArrayData);
        }
        // Double
        if (fdata instanceof Double) {
          double[] doubleArrayData = new double[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            doubleArrayData[j] = jdata.getDouble(j);
          }
          bundle.putDoubleArray(name, doubleArrayData);
        }
        // Long
        if (fdata instanceof Long) {
          long[] longArrayData = new long[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            longArrayData[j] = jdata.getLong(j);
          }
          bundle.putLongArray(name, longArrayData);
        }
        // String
        if (fdata instanceof String) {
          String[] stringArrayData = new String[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            stringArrayData[j] = jdata.getString(j);
          }
          bundle.putStringArray(name, stringArrayData);
        }
        // Boolean
        if (fdata instanceof Boolean) {
          boolean[] booleanArrayData = new boolean[jdata.length()];
          for (int j = 0; j < jdata.length(); ++j) {
            booleanArrayData[j] = jdata.getBoolean(j);
          }
          bundle.putBooleanArray(name, booleanArrayData);
        }
      }
    }
  }

  void startActivity(final Intent intent) throws Exception {
    try {
      intent.setFlags(intent.getFlags() | intentFlags);
      context.startActivity(intent);
    } catch (Exception e) {
      throw new Exception("Failed to launch intent : "+ e);
    }
  }

  private Intent buildIntent(String action, String uri, String type, JSONObject extras,
                             String packagename, String classname, JSONArray categories) throws Exception {
    Intent intent = new Intent(action);
    intent.setDataAndType(uri != null ? Uri.parse(uri) : null, type);
    if (packagename != null && classname != null) {
      intent.setComponent(new ComponentName(packagename, classname));
    }
    if (extras != null) {
      putExtrasFromJsonObject(extras, intent);
    }
    if (categories != null) {
      for (int i = 0; i < categories.length(); i++) {
        intent.addCategory(categories.getString(i));
      }
    }
    return intent;
  }

  // TODO(damonkohler): It's unnecessary to add the complication of choosing between startActivity
  // and startActivityForResult. It's probably better to just always use the ForResult version.
  // However, this makes the call always blocking. We'd need to add an extra boolean parameter to
  // indicate if we should wait for a result.
  @Rpc(description = "Starts an activity and returns the result.",
          returns = "A Map representation of the result Intent.")
  public Intent startActivityForResult(
          @RpcParameter(name = "action") String action,
          @RpcParameter(name = "uri") @RpcOptional String uri,
          @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
          @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras,
          @RpcParameter(name = "packagename", description = "name of package. If used, requires classname to be useful") @RpcOptional String packagename,
          @RpcParameter(name = "classname", description = "name of class. If used, requires packagename to be useful") @RpcOptional String classname)
          throws Exception {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    return startActivityForResult(intent);
  }

  @Rpc(description = "Starts an activity and returns the result.", returns = "A Map representation of the result Intent.")
  public Intent startActivityForResultIntent(
          @RpcParameter(name = "intent", description = "Intent in the format as returned from makeIntent") Intent intent) {
    return startActivityForResult(intent);
  }

  public void doStartActivity(final Intent intent, Boolean wait,int description) throws Exception {
    Exception[] except = new Exception[1];
    if (wait == null || !wait) {
      startActivity(intent);
    } else {
      FutureActivityTask<Intent> task = new FutureActivityTask<Intent>() {
        private boolean mSecondResume = false;

        @Override
        public void onCreate() {
          super.onCreate();
          if(deamonCount==0)
            Toast.makeText(context,R.string.click_daemon,Toast.LENGTH_SHORT).show();
          setTaskDescription(context.getString(description)+context.getString(R.string.daemon)+(++deamonCount));
          intent.setFlags(intent.getFlags() | intentFlags);
          try {
            startActivity(intent);
          } catch (Exception exception) {
            except[0] = exception;
          }
        }

        @Override
        public void onResume() {
          if (mSecondResume) {
            finish();
          }
          mSecondResume = true;
        }

        @Override
        public void onDestroy() {
          setResult(null);
        }

      };
      mTaskQueue.execute(task);

      try {
        task.getResult();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if(except[0]!=null) {
        throw except[0];
      }
    }
  }

  /**
   * packagename and classname, if provided, are used in a 'setComponent' call.
   */
  @Rpc(description = "Starts an activity.")
  public void startActivity(
          @RpcParameter(name = "action") String action,
          @RpcParameter(name = "uri") @RpcOptional String uri,
          @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
          @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras,
          @RpcParameter(name = "wait", description = "block until the user exits the started activity") @RpcOptional Boolean wait,
          @RpcParameter(name = "packagename", description = "name of package. If used, requires classname to be useful") @RpcOptional String packagename,
          @RpcParameter(name = "classname", description = "name of class. If used, requires packagename to be useful") @RpcOptional String classname)
          throws Exception {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    doStartActivity(intent, wait, R.string.normal);
  }

  @Rpc(description = "Send a broadcast.")
  public void sendBroadcast(
          @RpcParameter(name = "action") String action,
          @RpcParameter(name = "uri") @RpcOptional String uri,
          @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
          @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras,
          @RpcParameter(name = "packagename", description = "name of package. If used, requires classname to be useful") @RpcOptional String packagename,
          @RpcParameter(name = "classname", description = "name of class. If used, requires packagename to be useful") @RpcOptional String classname)
          throws Exception {
    final Intent intent = buildIntent(action, uri, type, extras, packagename, classname, null);
    try {
      mService.sendBroadcast(intent);
    } catch (Exception e) {
      LogUtil.e("Failed to broadcast intent.", e);
    }
  }

  @Rpc(description = "Create an Intent.", returns = "An object representing an Intent")
  public Intent makeIntent(
          @RpcParameter(name = "action") String action,
          @RpcParameter(name = "uri") @RpcOptional String uri,
          @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
          @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras,
          @RpcParameter(name = "categories", description = "a List of categories to add to the Intent") @RpcOptional JSONArray categories,
          @RpcParameter(name = "packagename", description = "name of package. If used, requires classname to be useful") @RpcOptional String packagename,
          @RpcParameter(name = "classname", description = "name of class. If used, requires packagename to be useful") @RpcOptional String classname,
          @RpcParameter(name = "flags", description = "Intent flags") @RpcOptional Integer flags)
          throws Exception {
    Intent intent = buildIntent(action, uri, type, extras, packagename, classname, categories);
    if (flags == null) {
      flags = intentFlags;
    }
    intent.setFlags(flags);
    return intent;
  }

  @Rpc(description = "Start Activity using Intent")
  public void startActivityIntent(
          @RpcParameter(name = "intent", description = "Intent in the format as returned from makeIntent") Intent intent,
          @RpcParameter(name = "wait", description = "block until the user exits the started activity") @RpcOptional Boolean wait)
          throws Exception {
    doStartActivity(intent, wait, R.string.normal);
  }

  @Rpc(description = "Send Broadcast Intent")
  public void sendBroadcastIntent(
          @RpcParameter(name = "intent", description = "Intent in the format as returned from makeIntent") Intent intent)
          throws Exception {
    mService.sendBroadcast(intent);
  }

  @Rpc(description = "Vibrates the phone or a specified duration in milliseconds.")
  public void vibrate(
          @RpcParameter(name = "duration", description = "duration in milliseconds") @RpcDefault("300") Integer duration) {
    mVibrator.vibrate(duration);
  }

  @Rpc(description = "Displays a Toast notification.")
  public void makeToast(@RpcParameter(name = "message") final String message,
                        @RpcParameter(name = "length") @RpcDefault("0") final Integer length,
                        @RpcParameter(name = "isHtml") @RpcDefault("false") final Boolean isHtml) {
    mHandler.post(() -> {
      if(isHtml)
          Toast.makeText(mService, HtmlUtil.textToHtml(message), length).show();
      else
          Toast.makeText(mService, message, length).show();
    });
  }

  public void makeToast(final String message,final Integer length) {
    makeToast(message,length,false);
  }

  public void makeToast(final String message) {
    makeToast(message,0,false);
  }

  /* 乘着船：过时删除
  private String getInputFromAlertDialog(final String title, final String message,
                                         final boolean password) {
    final FutureActivityTask<String> task = new FutureActivityTask<String>() {
      @Override
      public void onCreate() {
        super.onCreate();
        final EditText input = new EditText(getActivity());
        if (password) {
          input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
          input.setTransformationMethod(new PasswordTransformationMethod());
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();
            setResult(input.getText().toString());
            finish();
          }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            dialog.dismiss();
            setResult(null);
            finish();
          }
        });
        alert.show();
      }
    };
    mTaskQueue.execute(task);

    try {
      return task.getResult();
    } catch (Exception e) {
      LogUtil.e("Failed to display dialog.", e);
      throw new RuntimeException(e);
    }
  }

  @Rpc(description = "Queries the user for a text input.")
  @RpcDeprecated(value = "dialogGetInput", release = "r3")
  public String getInput(
          @RpcParameter(name = "title", description = "title of the input box") @RpcDefault("SL4A Input") final String title,
          @RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter value:") final String message) {
    return getInputFromAlertDialog(title, message, false);
  }

  @Rpc(description = "Queries the user for a password.")
  @RpcDeprecated(value = "dialogGetPassword", release = "r3")
  public String getPassword(
          @RpcParameter(name = "title", description = "title of the input box") @RpcDefault("SL4A Password Input") final String title,
          @RpcParameter(name = "message", description = "message to display above the input box") @RpcDefault("Please enter password:") final String message) {
    return getInputFromAlertDialog(title, message, true);
  }*/

  private static int NOTIFICATION_ID = 0x20002;//通知栏消息id

  @SuppressLint("UnspecifiedImmutableFlag")
  @Rpc(description = "Displays a notification that will be canceled when the user clicks on it.")
  public void notify(
          @RpcParameter(name = "title", description = "title") final String title,
          @RpcParameter(name = "message") final String message,
          @RpcParameter(name = "uri") @RpcOptional final String uri,
          @RpcParameter(name = "arg") @RpcOptional final String arg) {
    // This contentIntent is a noop.
    Intent intent;
    PendingIntent contentIntent = null;
    if (uri!=null) {
      //android.util.Log.d("AndroidFacade", "attachmentUri:"+attachmentUri);
      if (uri.startsWith("http:") || uri.startsWith("https:")) {
        intent = SPFUtils.getLinkAsIntent(context, uri);
      } else {
        if (uri.endsWith(".py")) {
          try{
            intent = new Intent (context,NotificationClickReceiver.class);
            intent.putExtra("path",uri);
            intent.putExtra("arg",arg);
            contentIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          } catch (Exception e) {
            intent = new Intent();
            intent.setClassName(mService.getPackageName(), "org.qpython.qpylib.MPyApi");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("org.qpython.qpylib.action.MPyApi");
            Bundle mBundle = new Bundle();
            mBundle.putString("app", SPFUtils.getCode(mService.getApplicationContext()));
            mBundle.putString("act", "onPyApi");
            mBundle.putString("flag", "api");
            mBundle.putString("param", "fileapi");
            mBundle.putString("pyfile", uri);
            mBundle.putString("pyarg", arg);
            intent.putExtras(mBundle);
          }
        } else {
        intent = new Intent();
        intent.setClassName(context.getPackageName(), uri);
      }}
    } else {
      intent = new Intent();
    }
    if(contentIntent == null)
        contentIntent = PendingIntent.getActivity(mService, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification notification = SPFUtils.getNotification(context, title, message, contentIntent,
            SPFUtils.getDrawableId(mService, "img_home_logo"), null);

    // Get a unique notification id from the application.
    mNotificationManager.notify(NotificationIdFactory.create(), notification);
  }

  @Rpc(description = "Returns the status of network connection.")
  public boolean getNetworkStatus() {
    return SPFUtils.netCheckin(context);
  }


  @Rpc(description = "Returns the intent that launched the script.")
  public Object getIntent() {
    return mIntent;
  }

  @Rpc(description = "Launches an activity that sends an e-mail message to a given recipient.")
  public void sendEmail(
          @RpcParameter(name = "to", description = "A comma separated list of recipients.") final String to,
          @RpcParameter(name = "subject") final String subject,
          @RpcParameter(name = "body") final String body,
          @RpcParameter(name = "attachmentUri") @RpcOptional final String attachmentUri) throws Exception {
    final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
    intent.setType("plain/text");
    intent.putExtra(android.content.Intent.EXTRA_EMAIL, to.split(","));
    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
    intent.putExtra(android.content.Intent.EXTRA_TEXT, body);
    if (attachmentUri != null) {
      intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(attachmentUri));
    }
    startActivity(intent);
  }

  @Rpc(description = "Returns package version code.")
  public int getPackageVersionCode(@RpcParameter(name = "packageName") final String packageName) {
    int result = -1;
    PackageInfo pInfo = null;
    try {
      pInfo =
              mService.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      pInfo = null;
    }
    if (pInfo != null) {
      result = pInfo.versionCode;
    }
    return result;
  }

  @Rpc(description = "Returns package version name.")
  public String getPackageVersion(@RpcParameter(name = "packageName") final String packageName) {
    PackageInfo packageInfo = null;
    try {
      packageInfo =
              mService.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      return null;
    }
    if (packageInfo != null) {
      return packageInfo.versionName;
    }
    return null;
  }

  /*@Rpc(description = "Checks if version of QPython SL4A is greater than or equal to the specified version.")
  public boolean requiredVersion(@RpcParameter(name = "requiredVersion") final Integer version) {
    boolean result = false;
    int packageVersion = getPackageVersionCode("com.googlecode.android_scripting");
    if (version > -1) {
      result = (packageVersion >= version);
    }
    return result;
  }*/

  @Rpc(description = "Writes message to logcat.")
  public void log(@RpcParameter(name = "message") String message) {
    android.util.Log.v("QSL4A", message);
  }

  /**
   *
   * Map returned:
   *
   * <pre>
   *   TZ = Timezone
   *     id = Timezone ID
   *     display = Timezone display name
   *     offset = Offset from UTC (in ms)
   *   SDK = SDK Version
   *   download = default download path
   *   appcache = Location of application cache
   *   sdcard = Space on sdcard
   *     availblocks = Available blocks
   *     blockcount = Total Blocks
   *     blocksize = size of block.
   * </pre>
   */
  @SuppressLint("SdCardPath")
  @Rpc(description = "A map of various useful environment details")
  public Map<String, Object> environment() {
    Map<String, Object> result = new HashMap<String, Object>();
    Map<String, Object> zone = new HashMap<String, Object>();
    Map<String, Object> space = new HashMap<String, Object>();
    TimeZone tz = TimeZone.getDefault();
    zone.put("id", tz.getID());
    zone.put("display", tz.getDisplayName());
    zone.put("offset", tz.getOffset((new Date()).getTime()));
    result.put("TZ", zone);
    result.put("SDK", android.os.Build.VERSION.SDK);
    result.put("download", FileUtils.getExternalDownload().getAbsolutePath());
    result.put("appcache", mService.getCacheDir().getAbsolutePath());
    try {
      StatFs fs = new StatFs("/sdcard");
      space.put("availblocks", fs.getAvailableBlocks());
      space.put("blocksize", fs.getBlockSize());
      space.put("blockcount", fs.getBlockCount());
    } catch (Exception e) {
      space.put("exception", e.toString());
    }
    result.put("sdcard", space);
    return result;
  }

  @Rpc(description = "Get list of constants (static final fields) for a class")
  public Bundle getConstants(
          @RpcParameter(name = "classname", description = "Class to get constants from") String classname)
          throws Exception {
    Bundle result = new Bundle();
    int flags = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;
    Class<?> clazz = Class.forName(classname);
    for (Field field : clazz.getFields()) {
      if ((field.getModifiers() & flags) == flags) {
        Class<?> type = field.getType();
        String name = field.getName();
        if (type == int.class) {
          result.putInt(name, field.getInt(null));
        } else if (type == long.class) {
          result.putLong(name, field.getLong(null));
        } else if (type == double.class) {
          result.putDouble(name, field.getDouble(null));
        } else if (type == char.class) {
          result.putChar(name, field.getChar(null));
        } else if (type instanceof Object) {
          result.putString(name, field.get(null).toString());
        }
      }
    }
    return result;
  }

  public static class NotificationClickReceiver extends BroadcastReceiver {
    public NotificationClickReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
      Message msg = new Message();
      msg.obj = new String[]{
              intent.getStringExtra("path"),
              intent.getStringExtra("arg")
      };
      handler.sendMessage(msg);
    }
  }
}
