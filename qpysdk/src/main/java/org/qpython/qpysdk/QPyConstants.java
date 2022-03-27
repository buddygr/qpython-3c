package org.qpython.qpysdk;

import android.content.Context;
import android.os.Environment;

import com.quseit.config.BASE_CONF;
import com.quseit.util.FileHelper;

public interface QPyConstants extends BASE_CONF {

    String WEB_PROJECT     = "web";
    String CONSOLE_PROJECT = "console";
    //final String KIVY_PROJECT    = "kivy";
    //String PYGAME_PROJECT    = "pygame";
    String QUIET_PROJECT    = "quiet";
    String QSL4A_PROJECT    = "qsl4a";

    String BASE_PATH         = "qpython";
    //String AD_URL            = "https://apu2.quseit.com/ad/";

    //String UPDATE_URL  = "https://apu2.quseit.com/conf/update/";
    //String LOG_URL     = "https://apu2.quseit.com/conf/log/";
    //String IAP_LOG_URL = "https://apu2.quseit.com/conf/iaplog/";

    String DFROM_RUN   = ".runtime";
    String PY_CACHE    = ".qpyc";

   // String DFROM_QPY2 = "scripts";
    String DFROM_QPY3 = "scripts3";
   // String DFROM_PRJ2 = "projects";
    String DFROM_PRJ3 = "projects3";

    String KEY_PY3_RES = "setting.py3resource.path";
    //String KEY_NOTEBOOK_RES = "setting.notebook3sresource.path";
    //String KEY_NOTEBOOK2_RES = "setting.notebook2resource.path";

    String LEGACY_PATH = Environment.getExternalStorageDirectory().getPath() + "/" + BASE_PATH;
    String LEGACY_LOG = LEGACY_PATH+"/log/last.log";

    String QPYC3 = "https://dl.qpy.io/py3.json";
    String QPYC2COMPATIBLE = "https://dl.qpy.io/py2compatible.json";
    String QPYC3_VER_KEY= "setting.py3resource.ver";
    String QPYC2COMPATIBLE_VER_KEY="setting.py2compatibleresource.ver";
}
