package org.qpython.qsl4a.qsl4a.facade;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;


import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for managing Applications.
 * 
 */
public class ApplicationManagerFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private final ActivityManager mActivityManager;
  private final PackageManager mPackageManager;
  private final Context context;

  public ApplicationManagerFacade(FacadeManager manager) {
    super(manager);
    Service service = manager.getService();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    mActivityManager = (ActivityManager) service.getSystemService(Context.ACTIVITY_SERVICE);
    mPackageManager = service.getPackageManager();
    context = mAndroidFacade.context;
  }

  @Rpc(description = "Returns a list of all launchable packages with class name and application name .")
  public Map<String, String> getLaunchablePackages(
          @RpcParameter(name = "need class name") @RpcDefault("false") Boolean needClassName) {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, 0);
    Map<String, String> applications = new HashMap<String, String>();
    if (needClassName){
      for (ResolveInfo info : resolveInfos) {
      applications.put(info.activityInfo.packageName, info.activityInfo.name+"|"+info.loadLabel(mPackageManager).toString());
    }}
    else {
    for (ResolveInfo info : resolveInfos) {
      applications.put(info.activityInfo.packageName, info.loadLabel(mPackageManager).toString());
    }}
    return applications;
  }

  @Rpc(description = "get all packages")
  public Map<String,String> getAllPackages() {
    Map<String, String> packages = new HashMap<>();
    List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES |
            PackageManager.GET_SERVICES);
    for (PackageInfo info : packageInfos) {
      packages.put(info.packageName,  info.applicationInfo.loadLabel(mPackageManager).toString());
  }
    return packages;
  }

  @Rpc(description = "get system packages")
  public Map<String,String> getSystemPackages() {
    Map<String, String> packages = new HashMap<>();
    List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES |
            PackageManager.GET_SERVICES);
    for (PackageInfo info : packageInfos) {
      if(((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) || ((info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1))
        packages.put(info.packageName, info.applicationInfo.loadLabel(mPackageManager).toString());
    }
    return packages;
  }

  @Rpc(description = "get user packages")
  public Map<String,String> getUserPackages() {
    Map<String, String> packages = new HashMap<>();
    List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES |
            PackageManager.GET_SERVICES);
    for (PackageInfo info : packageInfos) {
      if(((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1) && ((info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 1))
        packages.put(info.packageName, info.applicationInfo.loadLabel(mPackageManager).toString());
    }
    return packages;
  }

  @Rpc(description = "Start activity with the given classname and/or packagename .")
  public void launch(@RpcParameter(name = "classname") @RpcOptional String classname,
                     @RpcParameter(name = "packagename") @RpcOptional String packagename,
                     @RpcParameter(name = "wait") @RpcDefault("true") @RpcOptional Boolean wait)
          throws Exception {
    Intent intent;
    if (classname == null) {
      intent=context.getPackageManager().getLaunchIntentForPackage(packagename);
    } else {
      intent = new Intent(Intent.ACTION_MAIN);
      if (packagename == null) {
        packagename = classname.substring(0, classname.lastIndexOf("."));
      }
      intent.setClassName(packagename, classname);
    }
    mAndroidFacade.doStartActivity(intent,wait);
  }

  @Rpc(description = "Returns a list of packages running activities or services.", returns = "List of packages running activities.")
  public List<String> getRunningPackages() {
    Set<String> runningPackages = new HashSet<String>();
    List<ActivityManager.RunningAppProcessInfo> appProcesses =
        mActivityManager.getRunningAppProcesses();
    for (ActivityManager.RunningAppProcessInfo info : appProcesses) {
      runningPackages.addAll(Arrays.asList(info.pkgList));
    }
    List<ActivityManager.RunningServiceInfo> serviceProcesses =
        mActivityManager.getRunningServices(Integer.MAX_VALUE);
    for (ActivityManager.RunningServiceInfo info : serviceProcesses) {
      runningPackages.add(info.service.getPackageName());
    }
    return new ArrayList<String>(runningPackages);
  }

  @SuppressWarnings("deprecation")
@Rpc(description = "Force stops a package.")
  public void forceStopPackage(
      @RpcParameter(name = "packageName", description = "name of package") String packageName) {
    mActivityManager.restartPackage(packageName);
  }

  @Override
  public void shutdown() {
  }
}
