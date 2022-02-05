package com.quseit.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.quseit.android.R;
import com.quseit.config.BASE_CONF;
import com.quseit.util.PreferenceUtil;


public class SmallWindowView extends WindowView  {

    public SmallWindowView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.float_window_small, this);
        View view = findViewById(R.id.linLayoutSmall);
        int statusBarHeight = PreferenceUtil.getSingleton(context).getInt(BASE_CONF.SP_STATUSBAR_HEIGHT, 0);
        if (statusBarHeight != 0) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.height = statusBarHeight;
            view.setLayoutParams(layoutParams);
        }
        viewWidth = view.getLayoutParams().width;
        viewHeight = view.getLayoutParams().height;
    }
}
