package org.qpython.qsl4a.qsl4a.facade.ui;

import android.os.Handler;

import org.qpython.qsl4a.qsl4a.facade.FacadeManager;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.util.HashMap;
import java.util.Map;


public class UiConfig extends RpcReceiver {
  // This value should not be used for menu groups outside this class.

  public UiConfig(FacadeManager manager) {
    super(manager);
  }

  @Rpc(description = "set html picture size")
  public static void htmlPictureSetSize(
      // widthFixed or heightFixed = 0 means original picture size
      @RpcParameter(name = "width fixed") @RpcOptional Integer widthFixed,
      @RpcParameter(name = "height fixed") @RpcOptional Integer heightFixed,
      @RpcParameter(name = "width ratio") @RpcOptional Double widthRatio,
      @RpcParameter(name = "height ratio") @RpcOptional Double heightRatio
  ){
    if(widthFixed == null)
      widthFixed = 0;
    HtmlUtil.widthFixed = widthFixed;
    if(heightFixed == null)
      heightFixed = 0;
    HtmlUtil.heightFixed = heightFixed;
    if(widthRatio == null)
      widthRatio = 1.0;
    HtmlUtil.widthRatio = widthRatio;
    if(heightRatio == null)
      heightRatio = 1.0;
    HtmlUtil.heightRatio = heightRatio;
  }

  @Rpc(description = "get html picture size")
  public static Map<String,Object> htmlPictureGetSize(){
    Map<String,Object> map = new HashMap<>();
    map.put("widthFixed",HtmlUtil.widthFixed);
    map.put("heightFixed",HtmlUtil.heightFixed);
    map.put("widthRatio",HtmlUtil.widthRatio);
    map.put("heightRatio",HtmlUtil.heightRatio);
    return map;
  }

  @Override
  public void shutdown() {

  }
}