package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Button;

import org.json.JSONObject;
import org.qpython.qsl4a.R;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.util.ArrayList;

public class FloatViewFacade extends RpcReceiver {

  private final Service mService;
  private final PackageManager mPackageManager;
  private final AndroidFacade mAndroidFacade;
  private final String floatViewActivity = "org.qpython.qpy.main.activity.FloatViewActivity";
  private final String protectActivity = "org.qpython.qpy.main.auxActivity.ProtectActivity";
  private final Context context;

  private final String[] argName = new String[] {
          "x","y","width","height","textSize", //Integer型(可以设为上次)
          "index", //Integer型(索引，不可设为上次)
          "text","html", //字符型(二选一)
          "backColor","textColor", //字符型(二选一)
          "clickRemove", //布尔型
          "flag", //Integer型(索引，不可设为上次)
          "script","arg" //字符型(无前则无后)
  };

  //按钮数组
  public static final ArrayList<Button> buttons = new ArrayList<>();
  //参数数组
  public static final ArrayList<WindowManager.LayoutParams> params = new ArrayList<>();
  //时间数组
  public static final ArrayList<Long> times = new ArrayList<>();
  //操作类型数组
  public static final ArrayList<String> operations = new ArrayList<>();
  //窗口管理器
  public static WindowManager windowManager;
  public static Handler handler;

  public FloatViewFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mPackageManager = mService.getPackageManager();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
  }

  @Rpc(description = "Show Float View .")
    public int floatView(
    @RpcParameter(name = "args") @RpcOptional JSONObject args)
          throws Exception  {
    if (args == null) {
      args = new JSONObject();
    }
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),floatViewActivity);
    String ArgName;
    int index = -1;
    for(byte i=0;i<5;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getInt(ArgName));
        continue;
      } catch (Exception ignored) {}
      try {
        if(args.getString(ArgName).equalsIgnoreCase("last")){
          intent.putExtra(ArgName,Integer.MIN_VALUE);
        }
      } catch (Exception ignored) {}
    }
    ArgName = argName[5];
    try {
      index = args.getInt(ArgName);
      intent.putExtra(ArgName, index);
    } catch (Exception ignored) {}
    for(byte i=6;i<8;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getString(ArgName));
        break;
      } catch (Exception ignored) {}
    }
    for(byte i=8;i<10;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getString(ArgName));
      } catch (Exception ignored) {}
    }
    ArgName = argName[10];
    try {
      intent.putExtra(ArgName, args.getBoolean(ArgName));
    } catch (Exception ignored) {}
    ArgName = argName[11];
    try {
      intent.putExtra(ArgName, args.getInt(ArgName));
    } catch (Exception ignored) {}
    for(byte i=12;i<14;i++) {
      ArgName = argName[i];
      try {
        intent.putExtra(ArgName, args.getString(ArgName));
      } catch (Exception e) {
        break;
      }
    }
    //intent.setFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    intent.setAction(Intent.ACTION_VIEW);
    if(handler == null) {
      mAndroidFacade.startActivity(intent);
    } else {
      Message msg = new Message();
      msg.obj = intent;
      handler.sendMessage(msg);
    }
    if(index >= 0 && index < buttons.size())
      return buttons.size();
    else return buttons.size() + 1;
    }

  @Rpc(description = "Return Float View Result.")
  public JSONObject floatViewResult(
          @RpcParameter(name="index") @RpcDefault("-1") Integer index
  ) throws Exception  {
    JSONObject result = new JSONObject();
      if(index<0) index=buttons.size()-1;
      if(index<0 || index>=buttons.size()){
        throw new Exception(context.getString(R.string.float_view_out_range));
      }
      WindowManager.LayoutParams layoutParams = params.get(index);
      if(buttons.get(index) == null){
        result.put("removed",true);
      }
      //返回横坐标，原点为屏幕中心
      result.put("x", layoutParams.x);
      //返回纵坐标，原点为屏幕中心
      result.put("y", layoutParams.y);
      //返回操作时间
      result.put("time", times.get(index));
      //返回操作类型
      result.put("operation", operations.get(index));
      //返回索引位置
      result.put("index",index);
      return result;
    }

  @Rpc(description = "Remove Float View .")
  public int floatViewRemove(
          @RpcParameter(name = "index") @RpcDefault("-1") Integer index
  )
          throws Exception {
    //删除悬浮窗
    //返回删除的悬浮窗个数
    if (index>-2){
      if(index==-1){//删除所有悬浮窗
        index = 0;
        for(Button button : buttons)
          index += removeButton(button);
        buttons.clear();
        params.clear();
        times.clear();
        operations.clear();
      } else {//删除一个悬浮窗
        try {
          if(index>=buttons.size()){
            throw new Exception(context.getString(R.string.float_view_out_range));
          }
          index = removeButton(index);
        } catch (Exception e){
          throw new Exception(e.toString());
        }
      }
    } else index = 0;
    if(buttons.size() == 0)
      handler = null;
    return index;
  }

  public static int removeButton(int index){
    int i = removeButton(buttons.get(index));
    buttons.set(index,null);
    return i;
  }

  private static int removeButton(Button button){
    if(button!=null) {
      windowManager.removeView(button);
      return 1;
    } else return 0;
  }

  @Rpc(description = "Float View Count .")
  public int floatViewCount(){
    int cnt = 0;
    for(int i=0;i<buttons.size();i++)
      if(buttons.get(i)!=null)
        cnt+=1;
      return cnt;
  }

  @Rpc(description = "QPython Background Protect .")
  public void backgroundProtect(
          @RpcParameter(name = "enabled") @RpcDefault("true") Boolean enabled
  ) throws Exception {
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),protectActivity);
    String action;
    if(enabled)
      action = Intent.ACTION_RUN;
    else
      action = Intent.ACTION_DELETE;
    intent.setAction(action);
    mAndroidFacade.startActivity(intent);
  }

  @Override
  public void shutdown() {
  }
}
