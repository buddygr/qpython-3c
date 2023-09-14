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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.IconCompat;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.swiftp.Defaults;
import org.swiftp.FTPServerService;
import org.swiftp.Globals;

import java.io.File;

public class FtpFacade extends RpcReceiver {

  private final AndroidFacade mAndroidFacade;
  private static Context context;
  private static SharedPreferences preference;
  private static SharedPreferences.Editor editor;
  private final String SettingActivity = "org.qpython.qpy.main.activity.SettingActivity";

  public FtpFacade(FacadeManager manager) {
    super(manager);
    mAndroidFacade = manager.getReceiver(AndroidFacade.class);
    context = mAndroidFacade.context;
    preference = PreferenceManager.getDefaultSharedPreferences(context);
    editor = preference.edit();
    Globals.setContext(context);
    Defaults.setPortNumber(Integer.parseInt(context.getString(org.swiftp.R.string.portnumber_default)));
  }

  @Rpc(description = "FTP Start Service .")
  public String[] ftpStart() throws Exception {
    sendMessage(FTPServerService.ACTION_STARTED);
    return ftpGet();
  }

  @Rpc(description = "FTP Stop Service .")
  public void ftpStop() throws Exception {
    sendMessage(FTPServerService.ACTION_STOPPED);
  }

  private void sendMessage(String action) throws Exception {
    Intent intent = new Intent();
    intent.setClassName(context.getPackageName(),SettingActivity);
    intent.setAction(action);
    mAndroidFacade.startActivityForResult(intent);
  }

  @Rpc(description = "FTP Server is Running .")
  public boolean ftpIsRunning() {
    return FTPServerService.isRunning();
  }

  @Rpc(description = "FTP Server get IP address .")
  public static String[] ftpGet() {
    int port = FTPServerService.getPort();
    if(port == 0)
      FTPServerService.loadPort(context);
    String[] addr = FTPServerService.getIpPortString();
    if(addr == null)
      addr = new String[]{"ftp://0.0.0.0:"+port+"/"};
    return addr;
  }

  @Rpc(description = "FTP Server set port,username,password .")
  public void ftpSet(
          @RpcParameter(name = "port") @RpcOptional Integer port,
          @RpcParameter(name = "chrootDir") @RpcOptional String chrootDir,
          @RpcParameter(name = "username") @RpcDefault("ftp") String username,
          @RpcParameter(name = "password") @RpcDefault("ftp") String password
  ) throws Exception {
    if(port==null)
      port = Defaults.getPortNumber();
    if(chrootDir==null)
      chrootDir = Environment.getExternalStorageDirectory().getCanonicalPath();
    else {
      File chrootFile = new File(chrootDir);
      if (!(chrootFile.isDirectory() && chrootFile.canRead()))
        throw new Exception("Invalid Path");
      chrootDir = chrootFile.getCanonicalPath();
    }
    editor.putString(context.getString(org.swiftp.R.string.key_port_num),port.toString());
    editor.putString(context.getString(org.swiftp.R.string.key_root_dir),chrootDir);
    editor.putString(context.getString(org.swiftp.R.string.key_username),username);
    editor.putString(context.getString(org.swiftp.R.string.key_ftp_pwd),password);
    editor.apply();
  }

  @Override
  public void shutdown() {
    // Nothing to do yet.
  }
}
