/*
 * Copyright (C) 2010 Google Inc.
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

package org.qpython.qsl4a.qsl4a.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionUtil {
  @SuppressLint("StaticFieldLeak")
  public static Activity activity;

  private PermissionUtil() {
    // Utility class.
  }

  public static void checkPermission(String permission, int minVersion) throws Exception {
    if (Build.VERSION.SDK_INT >= minVersion)
      checkPermission(permission);
  }

  public static void checkPermission(String[] permissions, int minVersion) throws Exception {
    if (Build.VERSION.SDK_INT >= minVersion)
      checkPermission(permissions);
  }

  public static void checkPermission(String permission) throws Exception {
      checkPermission(new String[]{permission});
  }

  public static void checkPermission(String[] permissions) throws Exception {
    StringBuilder sb = new StringBuilder();
    for (String permission : permissions) {
      if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
        sb.append(" ").append(permission);
        break;
      }
    }
    if (sb.length() > 0) {
      ActivityCompat.requestPermissions(activity, permissions, 100);
      throw new Exception("Need Permission of"+sb);
    }
  }
}
