package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

public class VideoFacade extends RpcReceiver {

  private final Service mService;
  private final PackageManager mPackageManager;
  private final AndroidFacade mAndroidFacade;

  public VideoFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mPackageManager = mService.getPackageManager();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
  }

  @Rpc(description = "Play the Video via Video Path .")
    public void videoPlay(
    @RpcParameter(name = "path") final String path,
    @RpcParameter(name = "wait") @RpcDefault("true") Boolean wait)
          throws Exception  {
    Intent intent = new Intent();
    intent.setClassName(mService.getPackageName(),"org.qpython.qpy.main.activity.VideoActivity");
    intent.putExtra("path", path);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setAction(Intent.ACTION_VIEW);
    mAndroidFacade.doStartActivity(intent,wait);
    }

  @Override
  public void shutdown() {
  }
}
