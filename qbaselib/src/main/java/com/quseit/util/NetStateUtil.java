package com.quseit.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Hmei
 * 11/27/17
 */
public class NetStateUtil {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WIFI, DATA, EMPTY})
    public @interface State {
    }

    public static final int WIFI  = 0;
    public static final int DATA  = 1;
    public static final int EMPTY = 2;

    private static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    public static @State
    int getNetState(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        if (info != null && info.isConnected()) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                    return DATA;
                default:
                    return EMPTY;
            }
        } else {
            return EMPTY;
        }
    }

    public static boolean isConnected(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return info != null && info.isConnected();
    }
}
