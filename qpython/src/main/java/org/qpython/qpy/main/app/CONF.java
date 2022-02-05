package org.qpython.qpy.main.app;

import com.quseit.util.FileHelper;
import com.quseit.util.NAction;

import org.qpython.qpysdk.QPyConstants;

public class CONF implements QPyConstants {

    public static final String LIB_DOWNLOAD_TEMP = QPyConstants.ABSOLUTE_PATH + "/cache";
        public static final String QPYPI_URL         = "https://pypi.org/simple/";

    public static final String NOTIFICATION_SP_NAME = "NOTIFICATION_EXTRA";
    public static final String NOTIFICATION_SP_OBJ  = "NOTIFICATION_OBJ";

    public static final String IAP_NUM_REQUEST_URL = "http://apu.quseit.com/conf/iaplognum/org.qpython.qpy/";
    //public static final String GOOGLE_ID_TOKEN     = "955258715976-i6t5usa0tjg8favq17lsfaj885l4lilv.apps.googleusercontent.com";

    //public static final String CLOUD_MAP_CACHE_PATH = QPyConstants.ABSOLUTE_PATH + "/lib/.cloud_cache";

    public static String pyVer = "";
    public static String pyVerComplete = "";

    public static final String filesDir = App.getContext().getFilesDir().getAbsolutePath();

    public static final String binDir = filesDir + "/bin/";
    public static final String pytho = binDir + "python";
    public static final String qpysh = binDir + "qpython.sh";
    public static final String qpyshr = binDir + "qpython-root.sh";
    public static final String qpyccs = binDir + "colorConsole.py";
    public static final String qpypiPath = filesDir + "/lib/"+pyVer+"/site-packages/";

}
