package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

public class FloatViewFacade extends RpcReceiver {

  private final Service mService;
  private final PackageManager mPackageManager;
  private final AndroidFacade mAndroidFacade;
  private final String floatViewActivity = "org.qpython.qpy.main.activity.FloatViewActivity";
  private final String protectActivity = "org.qpython.qpy.main.auxActivity.ProtectActivity";

  public FloatViewFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mPackageManager = mService.getPackageManager();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
  }

  @Rpc(description = "Show Float View .")
    public void floatView(
    @RpcParameter(name = "args") @RpcOptional JSONObject args)
          throws Exception  {
    if (args == null) {
      args = new JSONObject();
    }
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),floatViewActivity);
    String[] argName = new String[] {
            "x","y","textSize","width","height","index",
            "text","html",
            "backColor","textColor","script","arg"
    };
    String ArgName;
    for(byte i=0;i<6;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getInt(ArgName));
      } catch (Exception ignored) {}
    }
    for(byte i=6;i<8;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getString(ArgName));
        break;
      } catch (Exception ignored) {}
    }
    for(byte i=8;i<12;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getString(ArgName));
      } catch (Exception ignored) {}
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setAction(Intent.ACTION_VIEW);
    mAndroidFacade.startActivity(intent);
    }

  @Rpc(description = "Return Float View Result.")
  public JSONObject floatViewResult(
          @RpcParameter(name="index") @RpcDefault("-1") Integer index
  ) throws Exception  {
    /*Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),floatViewActivity);
    intent.putExtra("result", true);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setAction(Intent.ACTION_VIEW);*/
    JSONObject result = new JSONObject();
    result.put("result",index);
    Intent intentR = mAndroidFacade.startActivityForResult(
            "android.intent.action.VIEW",null,null,result,
            mService.getPackageName(), floatViewActivity);
    result = new JSONObject();
    String[] argName = new String[] {
            "x","y","index",
            "time","operation"
    };
    String ArgName;
    for(byte i=0;i<3;i++) {
      ArgName = argName[i];
      try {
        result.put(ArgName,intentR.getIntExtra(ArgName,0));
      } catch (Exception e) {
        result.put(ArgName,e.toString());
      }
    }
    for(byte i=3;i<5;i++) {
      ArgName = argName[i];
      try {
        result.put(ArgName,intentR.getStringExtra(ArgName));
      } catch (Exception e) {
        result.put(ArgName,e.toString());
      }
    }
    return result;
  }

  @Rpc(description = "Remove Float View .")
  public void floatViewRemove(
          @RpcParameter(name = "index") @RpcDefault("-1") Integer index
  )
          throws Exception {
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(), floatViewActivity);
    intent.putExtra("remove", index);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setAction(Intent.ACTION_VIEW);
    mAndroidFacade.startActivity(intent);
  }

  @Rpc(description = "QPython Background Protect .")
  public void backgroundProtect() {
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),protectActivity);
    intent.setAction(Intent.ACTION_VIEW);
    mAndroidFacade.startActivity(intent);
  }

  @Override
  public void shutdown() {
  }
}
