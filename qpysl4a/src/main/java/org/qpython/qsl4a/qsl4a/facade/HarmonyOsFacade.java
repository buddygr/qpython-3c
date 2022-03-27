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
                return "Harmony".equalsIgnoreCase(osBrand.toString());
            } catch (Throwable x) {
                return false;
            }
        }

        /**
         * 获取鸿蒙系统版本号
         *
         * @return 版本号
         */
        private static String getHarmonyVersion() throws Exception {
            return getSystemProperty("hw_sc.build.platform.version", "");
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
                map.put("HarmonyVersion",getHarmonyVersion());
                map.put("HarmonyDisplayVersion",getHarmonyDisplayVersion());
            }
            return map;
        }

    @Override
    public void shutdown() {
    }
}
