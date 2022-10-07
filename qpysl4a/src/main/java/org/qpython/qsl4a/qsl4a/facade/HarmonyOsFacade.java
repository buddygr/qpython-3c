package org.qpython.qsl4a.qsl4a.facade;

import android.text.TextUtils;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A selection of commonly used intents. <br>
 * <br>
 * These can be used to trigger some common tasks.
 * 
 */
@SuppressWarnings("deprecation")
public class HarmonyOsFacade extends RpcReceiver {

    private static final String HARMONY = "Harmony";
    private static final String EMUI = "Emui";

    public HarmonyOsFacade(FacadeManager manager) {
    super(manager);
  }

    /**
     * 齐行超
     * 2020-02-16
     *
         * 是否为鸿蒙系统
         *
         * @return true为鸿蒙系统
         */
        private static boolean isHarmonyOs() {
            try {
                Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
                Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
                assert osBrand != null;
                return HARMONY.equalsIgnoreCase(osBrand.toString());
            } catch (Throwable x) {
                return false;
            }
        }

        /**
         * 获取鸿蒙系统版本号
         * edit by 乘着船
         * @return 版本号等
         */
        private static void getHarmonyVersion(Map<String,String> map) throws Exception {
            putHwScMap(map,"Version","platform.version");
            putHwScMap(map,"ApiVersion","os.apiversion");
            putHwScMap(map,"ReleaseType","os.releasetype");
            map.put("Open"+HARMONY+"Version",getSystemProperty("hw_sc.build.os.version", ""));
            putRoEmuiMap(map,"Version","version.emui");
            putRoEmuiMap(map,"ApiLevel","hw_emui_api_level");
        }

        private static void putHwScMap(Map<String,String> map,String str1,String str2) throws Exception {
            map.put(HARMONY+str1,getSystemProperty("hw_sc.build."+str2, ""));
        }

    private static void putRoEmuiMap(Map<String,String> map,String str1,String str2) throws Exception {
        map.put(EMUI+str1,getSystemProperty("ro.build."+str2, ""));
    }

        /**
         * 获取属性
         * @param property
         * @param defaultValue
         * @return
         */
        @Rpc(description = "get System Property")
        public static String getSystemProperty(
                @RpcParameter(name = "property") String property,
                @RpcParameter(name = "defaultValue") @RpcDefault("") String defaultValue
        ) throws Exception {
            try {
                Class spClz = Class.forName("android.os.SystemProperties");
                Method method = spClz.getDeclaredMethod("get", String.class);
                String value = (String) method.invoke(spClz, property);
                if (TextUtils.isEmpty(value)) return defaultValue;
                return value;
            } catch (Throwable e) {
                throw new Exception(e);
            }
        }

        /**
         * 获得鸿蒙系统版本号（含小版本号，实际上同Android的android.os.Build.DISPLAY）
         * @return 版本号
         */
        private static String getHarmonyDisplayVersion() {
            return android.os.Build.DISPLAY;
        }

        @Rpc(description = "get Harmony OS Information .")
        public static Map<String,String> getHarmonyOsInformation() throws Exception {
            Map<String, String> map = new HashMap<>();
            boolean isHarmony = isHarmonyOs();
            map.put("isHarmonyOs",String.valueOf(isHarmony));
            if (isHarmony) {
                getHarmonyVersion(map);
                map.put("HarmonyDisplayVersion",getHarmonyDisplayVersion());
            }
            return map;
        }

    @Override
    public void shutdown() {
    }
}
