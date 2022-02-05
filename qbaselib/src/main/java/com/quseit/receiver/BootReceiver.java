package com.quseit.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quseit.config.BASE_CONF;
import com.quseit.service.FloatWindowService;
import com.quseit.util.PreferenceUtil;


public class BootReceiver extends BroadcastReceiver  {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PreferenceUtil.getSingleton(context).getBoolean(BASE_CONF.SP_BOOT, true)) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                context.startService(new Intent(context, FloatWindowService.class));
            }
        }
    }
}
