package org.qpython.qsl4a.qsl4a.facade;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;

import org.json.JSONArray;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wifi functions.
 *
 */
public class WifiFacade extends RpcReceiver {

  private final Service mService;
  private final WifiManager mWifi;
  private final AndroidFacade mAndroidFacade;
  private final Context context;
  private Handler mHandler;
  private WifiLock mLock;

  public WifiFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    mHandler = mAndroidFacade.mHandler;
    mWifi = (WifiManager) mService.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    mLock = null;
  }

  private void makeLock(int wifiMode) {
    if (mLock == null) {
      mLock = mWifi.createWifiLock(wifiMode, "sl4a");
      mLock.acquire();
    }
  }

  @Rpc(description = "Returns the list of access points found during the most recent Wifi scan.")
  public List<ScanResult> wifiGetScanResults() {
    return mWifi.getScanResults();
  }

  @Rpc(description = "Acquires a full Wifi lock.")
  public void wifiLockAcquireFull() {
    makeLock(WifiManager.WIFI_MODE_FULL);
  }

  @Rpc(description = "Acquires a scan only Wifi lock.")
  public void wifiLockAcquireScanOnly() {
    makeLock(WifiManager.WIFI_MODE_SCAN_ONLY);
  }

  @Rpc(description = "Releases a previously acquired Wifi lock.")
  public void wifiLockRelease() {
    if (mLock != null) {
      mLock.release();
      mLock = null;
    }
  }

  @Rpc(description = "Starts a scan for Wifi access points.", returns = "True if the scan was initiated successfully.")
  public Boolean wifiStartScan() {
    return mWifi.startScan();
  }

  @Rpc(description = "Checks Wifi state.", returns = "True if Wifi is enabled.")
  public Boolean checkWifiState() {
    return mWifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
  }

  @Rpc(description = "Toggle Wifi on and off.", returns = "True if Wifi is enabled.")
  public Boolean toggleWifiState(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
    if (enabled == null) {
      enabled = !checkWifiState();
    }
    mWifi.setWifiEnabled(enabled);
    return enabled;
  }

  @Rpc(description = "Disconnects from the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiDisconnect() {
    return mWifi.disconnect();
  }

  @Rpc(description = "Returns information about the currently active access point.")
  public WifiInfo wifiGetConnectionInfo() {
    return mWifi.getConnectionInfo();
  }

  @Rpc(description = "Reassociates with the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiReassociate() {
    return mWifi.reassociate();
  }

  @Rpc(description = "Reconnects to the currently active access point.", returns = "True if the operation succeeded.")
  public Boolean wifiReconnect() {
    return mWifi.reconnect();
  }

  @Rpc(description = "get wifi ap state .")
  public String wifiGetApState() {
    try {
      Method method = mWifi.getClass().getMethod("getWifiApState");
      int i = (Integer) method.invoke(mWifi);
      switch (i) {
        case 10:
          return "disabling";
        case 11:
          return "disabled";
        case 12:
          return "enabling";
        case 13:
          return "enabled";
        default:
          return "unknown";
      }
    } catch (Exception e) {
      return "failed";
    }
  }

  @Rpc(description = "get connected hot ip")
  public JSONArray getConnectedInfo() throws Exception {
    JSONArray connectedIP = new JSONArray();
    BufferedReader br = new BufferedReader(new FileReader(
            "/proc/net/arp"));
    String line, IpMac;
    String[] splitted;
    while ((line = br.readLine()) != null) {
      if (line.contains("address")) continue;
      splitted = line.split(" +");
      if (splitted != null) {
        IpMac = splitted[0] + "|" + splitted[3];
        connectedIP.put(IpMac);
      }
    }
    return connectedIP;
  }

  private String intToIp(int ipInt) {
    StringBuilder sb = new StringBuilder();
    sb.append(ipInt & 0xFF);
    sb.append(".");
    sb.append((ipInt >>> 8) & 0xFF);
    sb.append(".");
    sb.append((ipInt >>> 16) & 0xFF);
    sb.append(".");
    sb.append((ipInt >>> 24) & 0xFF);
    return sb.toString();
  }

  @Rpc(description = "get dhcp info")
  public Map getDhcpInfo(@RpcParameter(name="ipConvertToString") @RpcDefault("true") Boolean ipConvertToString) {
    DhcpInfo info = mWifi.getDhcpInfo();
    Map map = new HashMap();
    if (ipConvertToString) {
      map.put("serverAddress", intToIp(info.serverAddress));
      map.put("ipAddress", intToIp(info.ipAddress));
      map.put("gateway", intToIp(info.gateway));
      map.put("netmask", intToIp(info.netmask));
      map.put("dns1", intToIp(info.dns1));
      map.put("dns2", intToIp(info.dns2));
    } else {
      map.put("serverAddress", info.serverAddress);
      map.put("ipAddress", info.ipAddress);
      map.put("gateway", info.gateway);
      map.put("netmask", info.netmask);
      map.put("dns1", info.dns1);
      map.put("dns2", info.dns2);
    }
    map.put("leaseDuration", info.leaseDuration);
    return map;
  }

  @Rpc(description = "get Internet Interface Address")
  public Map getInternetInterfaceAddress() throws SocketException {
    Map map = new HashMap();
    for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        JSONArray array = new JSONArray();
      for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
          if (!addr.isLoopbackAddress()){
            //map.put("addresses"+n, Arrays.toString(addr.getAddress()));
            //map.put("hostAddress",addr.getHostAddress());
            //map.put("hostName"+n,addr.getHostName());
            //map.put("canonicalHostName"+n,addr.getCanonicalHostName());
            //map.put("interface"+n,intf.toString());
            //map.put("interfaceName",intf.getName());
            //map.put("interfaceUp"+n,intf.isUp());
            array.put(addr.getHostAddress());
          }
      }
        if (array.length()>0)
          map.put(intf.getName(),array);
    }
    /*WifiInfo info = mWifi.getConnectionInfo();
    int ip = info.getIpAddress();
    if (ipConvertToString) {
      return intToIp(ip);
    } else {
      return String.valueOf(ip);
    }*/
    return map;
  }

  /*@Rpc(description = "wifi set ap enabled .")
  public String[] wifiSetApEnabled(
          @RpcParameter(name = "enabled") Boolean enabled
  ) throws Exception {
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.O){
      throw new Exception("wifiSetApEnabled Need Android >= 8.0 .");
    } else {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
      throw new Exception("wifiSetApEnabled Need Location Permission .");
    }
    String[] rst=new String[2];
    if (enabled) {
      mWifi.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback(){
        @Override
        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
          WifiConfiguration wifiConfiguration = reservation.getWifiConfiguration();
          rst[0] = wifiConfiguration.SSID;
          rst[1] = wifiConfiguration.preSharedKey;
        }
      }, mHandler);
    } else {
      Method method = mWifi.getClass().getMethod("cancelLocalOnlyHotspotRequest");
      method.setAccessible(true);
      method.invoke(mWifi);
      }
    return rst;
    }}*/

  @Override
  public void shutdown() {
    wifiLockRelease();
  }
}
