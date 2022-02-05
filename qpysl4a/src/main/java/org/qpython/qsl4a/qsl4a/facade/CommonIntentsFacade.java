package org.qpython.qsl4a.qsl4a.facade;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.view.Display;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A selection of commonly used intents. <br>
 * <br>
 * These can be used to trigger some common tasks.
 * 
 */
@SuppressWarnings("deprecation")
public class CommonIntentsFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private final String qpyProvider;
  private final Service mService;


    public CommonIntentsFacade(FacadeManager manager) {
    super(manager);
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    qpyProvider = mAndroidFacade.qpyProvider;
    mService = manager.getService();
  }

  @Override
  public void shutdown() {
  }

  @Rpc(description = "Display content to be picked by URI (e.g. contacts)", returns = "A map of result values.")
  public Intent pick(@RpcParameter(name = "uri") String uri) throws JSONException {
    return mAndroidFacade.startActivityForResult(Intent.ACTION_PICK, uri, null, null, null, null);
  }

  @Rpc(description = "Starts the barcode scanner.", returns = "Scan Result String .")
  public String scanBarcode(
          @RpcParameter(name = "title") @RpcOptional String title
  ) throws Exception {
      Intent intent = new Intent();
      intent.setClassName(mService.getPackageName(),"org.qpython.qpy.main.auxActivity.QrCodeActivityRstOnly");
      intent.setAction(Intent.ACTION_VIEW);
      intent.putExtra("title",title);
      intent = mAndroidFacade.startActivityForResult(intent);
      try {
          return intent.getStringExtra("result"); }
      catch (NullPointerException e) {
          return null;
      }
  }

  /*private void view(Uri uri, String type, String title, boolean wait) throws Exception {
    Intent intent = new Intent();
    intent.setClassName(this.mAndroidFacade.getmService().getApplicationContext(),"org.qpython.qpy.main.activity.QWebViewActivity");
    //intent.putExtra("com.quseit.common.extra.CONTENT_URL1", "main");
    //intent.putExtra("com.quseit.common.extra.CONTENT_URL2", "QPyWebApp");
    //intent.putExtra("com.quseit.common.extra.CONTENT_URL6", "drawer");
    intent.setDataAndType(uri, type);
    mAndroidFacade.doStartActivity(intent,wait);
  }*/

  @Rpc(description = "Start activity with view action by URI (i.e. browser, contacts, etc.).")
  public void view(
      @RpcParameter(name = "uri") String uri,
      @RpcParameter(name = "type", description = "MIME type/subtype of the URI") @RpcOptional String type,
      @RpcParameter(name = "extras", description = "a Map of extras to add to the Intent") @RpcOptional JSONObject extras,
      @RpcParameter(name = "wait") @RpcDefault ("true") @RpcOptional Boolean wait)
      throws Exception {
    mAndroidFacade.startActivity(Intent.ACTION_VIEW, uri, type, extras, wait, null, null);
  }

    @Rpc(description = "Convert normal path to content:// or file:// .")
    public String pathToUri(
            @RpcParameter(name = "path") String path) {
        File file = new File(path);
        Uri uri;
        //if (Build.VERSION.SDK_INT>=24) {
            uri = FileProvider.getUriForFile(context,qpyProvider,file);
        //} else {
        //   uri = Uri.fromFile(file);
        //}
        return uri.toString();
    }

  @Rpc(description = "Open a file with path")
  public void openFile(
          @RpcParameter(name = "path") String path,
          @RpcParameter(name = "type", description = "a MIME type of a file") @RpcOptional String type,
          @RpcParameter(name = "wait") @RpcDefault("true") Boolean wait)
          throws Exception {
          MimeTypeMap mime = MimeTypeMap.getSingleton();
          if (type == null) {
            /* 获取文件的后缀名 */
          int dotIndex = path.lastIndexOf(".");
          if (dotIndex < 0) {
            type = "*/*";  //找不到扩展名
          } else {
            try {
              type = mime.getMimeTypeFromExtension( path.substring( dotIndex + 1 ).toLowerCase() );
              if (type == null) {
                type = "*/*";  //找不到打开方式
              }
            } catch (Exception e) {
              type="*/*";  //出现错误
            }
          }}
          Intent intent = new Intent();
          intent.setAction(android.content.Intent.ACTION_VIEW);
          File file = new File(path);
          Uri uri;
          //if (Build.VERSION.SDK_INT>=24) {
              uri = FileProvider.getUriForFile(context,qpyProvider,file);
         // } else {
           //   uri = Uri.fromFile(file);
          //}
          intent.setDataAndType(uri, type);
          try {
              mAndroidFacade.doStartActivity(intent,wait);
          } catch (Exception e) {
              e.printStackTrace();
    }
  }

  @Rpc(description = "Opens a map search for query (e.g. pizza, 123 My Street).")
  public void viewMap(@RpcParameter(name = "query, e.g. pizza, 123 My Street") String query,
                      @RpcParameter(name = "wait") @RpcOptional Boolean wait)
      throws Exception {
    view("geo:0,0?q=" + query, null, null, wait);
  }

  @Rpc(description = "Opens the list of contacts.")
  public void viewContacts(
          @RpcParameter(name = "wait") @RpcOptional Boolean wait
  ) throws Exception {
    view("content://"+ContactsContract.AUTHORITY+"/contacts",null,null, wait);
  }

  @Rpc(description = "Starts a search for the given query.")
  public void search(@RpcParameter(name = "query") String query) {
    Intent intent = new Intent(Intent.ACTION_SEARCH);
    intent.putExtra(SearchManager.QUERY, query);
    mAndroidFacade.startActivity(intent);
  }

    @Rpc(description = "Opens the browser to display a local HTML/text/audio/video File or http(s) Website .")
    public void viewHtml(
            @RpcParameter(name = "path", description = "the path to the local HTML/text/audio/video File or http(s) Website") String path,
            @RpcParameter(name = "title") @RpcOptional String title,
            @RpcParameter(name = "wait") @RpcDefault("true") Boolean wait)
            throws Exception {
        Uri uri;
        Intent intent = new Intent();
        if (path.contains("://")) {
            uri=Uri.parse(path);
            intent.putExtra("src",path);
        } else {
            uri=Uri.fromFile(new File(path));
            intent.putExtra("LOG_PATH",path);
        }
        intent.setClassName(this.mAndroidFacade.getmService().getApplicationContext(),"org.qpython.qpy.main.activity.QWebViewActivity");
        intent.setDataAndType(uri, "text/html");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("title",title);
        mAndroidFacade.doStartActivity(intent,wait);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Rpc(description = "Get system language and country .")
    public String getLocale() {
       return Resources.getSystem().getConfiguration().getLocales().get(0).toString();
    }

    @Rpc(description = "get system infomation .")
    public Map<String,String> getSysInfo(){
      Map<String,String> s = new HashMap<>();
      s.put("model", Build.MODEL);
      s.put("sdk",Build.VERSION.SDK);
      s.put("release",Build.VERSION.RELEASE);
      s.put("brand",Build.BRAND);
      s.put("device",Build.DEVICE);
      s.put("display",Build.DISPLAY);
      s.put("language",Locale.getDefault().getLanguage());
      return s;
    }

    @Rpc(description = "get screen infomation .")
    public Map<String,Float> getScreenInfo(){
        Map<String,Float> s = new HashMap<>();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        s.put("widthPixels",(float)dm.widthPixels);
        s.put("heightPixels",(float)dm.heightPixels);
        s.put("densityDpi",(float)dm.densityDpi);
        s.put("density",dm.density);
        s.put("xdpi",dm.xdpi);
        s.put("ydpi",dm.ydpi);
        s.put("scaledDensity",dm.scaledDensity);
        return s;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Rpc(description = "create python script shortcut .")
    public void createScriptShortCut(
            @RpcParameter(name="scriptPath") String scriptPath,
            @RpcParameter(name="label") @RpcOptional String label,
            @RpcParameter(name="iconPath") @RpcOptional String iconPath,
            @RpcParameter(name="scriptArg") @RpcOptional String scriptArg
            ) {
        Intent intent = new Intent();
        intent.setClassName(mService.getPackageName(), "org.qpython.qpy.main.activity.HomeMainActivity");
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("type", "script");
        intent.putExtra("path", scriptPath);
        boolean isProj = (new File(scriptPath)).isDirectory();
        intent.putExtra("isProj", isProj);
        if (scriptArg!=null) intent.putExtra("arg",scriptArg);
        if (label==null){
            try {
                label = scriptPath;
                if (label.endsWith("/")) label = label.substring(0,label.length()-1);
                label = label.substring(label.lastIndexOf("/") + 1);
                if (label.endsWith(".py")) label = label.substring(0,label.length()-3);
            } catch (Exception ignored){}
        }
        Icon icon;
        if (iconPath!=null) {
            Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
            bitmap = Bitmap.createScaledBitmap(bitmap, 192, 192, true);
            icon = Icon.createWithBitmap(bitmap);
        } else {
            icon = Icon.createWithResource(context, android.R.drawable.sym_def_app_icon);
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);
            if (mShortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo =
                        new ShortcutInfo.Builder(context, label)
                                .setShortLabel(label)
                                .setLongLabel(label)
                                .setIcon(icon)
                                .setIntent(intent)
                                .build();
                Intent pinnedShortcutCallbackIntent =
                        mShortcutManager.createShortcutResultIntent(pinShortcutInfo);
                PendingIntent successCallback = PendingIntent.getBroadcast(context, 0,
                        pinnedShortcutCallbackIntent, 0);
                mShortcutManager.requestPinShortcut(pinShortcutInfo,
                        successCallback.getIntentSender());
            }
        } else {
            mAndroidFacade.makeToast("createScriptShortCut need Android >= 8.0 .",1);
        }
    }
}
