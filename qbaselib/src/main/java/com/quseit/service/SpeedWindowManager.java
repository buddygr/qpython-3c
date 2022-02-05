package com.quseit.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;


import com.quseit.android.R;
import com.quseit.config.BASE_CONF;
import com.quseit.util.PreferenceUtil;
import com.quseit.view.SmallWindowView;
import com.quseit.view.WindowView;

import java.text.DecimalFormat;

public class SpeedWindowManager  {

    private static SpeedWindowManager instance;
    private WindowManager mWindowManager;
    private WindowView mBigWindowView;
    private WindowView mSmallWindowView;
    private LayoutParams windowParams;
    private TextView tvMobileTx;
    private TextView tvMobileRx;
    private TextView tvWlanTx;
    private TextView tvWlanRx;
    private TextView tvSum;
    private long rxtxTotal = 0;
    private long mobileRecvSum = 0;
    private long mobileSendSum = 0;
    private long wlanRecvSum = 0;
    private long wlanSendSum = 0;
    private long exitTime = 0;
    private DecimalFormat showFloatFormat = new DecimalFormat("0.00");

    public static SpeedWindowManager getInstance() {
        if (instance == null) {
            instance = new SpeedWindowManager();
        }
        return instance;
    }

    public void createWindow(final Context context) {
        createWindow(context, BASE_CONF.SMALL_WINDOW_TYPE);
    }

    private void createWindow(final Context context, int type) {
        final WindowManager windowManager = getWindowManager(context);
        if (windowParams == null) {
            windowParams = getWindowParams(context);
        }

        if (mSmallWindowView == null) {
            mSmallWindowView = new SmallWindowView(context);
            Drawable background = getCurrentBgDrawable(context);
            setViewBg(background);
            if (PreferenceUtil.getSingleton(context).getBoolean(BASE_CONF.SP_LOC)) {
                setOnTouchListener(context, mSmallWindowView, BASE_CONF.BIG_WINDOW_TYPE);
            } else {
                setOnTouchListener(windowManager, context, mSmallWindowView, BASE_CONF.BIG_WINDOW_TYPE);
            }
            windowManager.addView(mSmallWindowView, windowParams);
        }
        tvSum = (TextView) mSmallWindowView.findViewById(R.id.tvSum);


        }


    private Drawable getCurrentBgDrawable(Context context) {
        Drawable background;
        int bgId;
        if (PreferenceUtil.getSingleton(context).getBoolean(BASE_CONF.SP_BG, false)) {
            bgId = R.drawable.trans_bg;
        } else {
            bgId = R.drawable.float_bg;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            background = context.getDrawable(bgId);
        } else {
            background = context.getResources().getDrawable(bgId);
        }
        return background;
    }

    public void initData() {
        mobileRecvSum = TrafficStats.getMobileRxBytes();
        mobileSendSum = TrafficStats.getMobileTxBytes();
        wlanRecvSum = TrafficStats.getTotalRxBytes() - mobileRecvSum;
        wlanSendSum = TrafficStats.getTotalTxBytes() - mobileSendSum;
        rxtxTotal = TrafficStats.getTotalRxBytes()
                + TrafficStats.getTotalTxBytes();
    }

    private LayoutParams getWindowParams(Context context) {
        final WindowManager windowManager = getWindowManager(context);
        Point sizePoint = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            windowManager.getDefaultDisplay().getSize(sizePoint);
        }
        int screenWidth = sizePoint.x;
        int screenHeight = sizePoint.y;
        LayoutParams windowParams = new LayoutParams();
        windowParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL;
        windowParams.gravity = Gravity.START | Gravity.TOP;
        windowParams.width = LayoutParams.WRAP_CONTENT;
        windowParams.height = LayoutParams.WRAP_CONTENT;
        int x = PreferenceUtil.getSingleton(context).getInt(BASE_CONF.SP_X, -1);
        int y = PreferenceUtil.getSingleton(context).getInt(BASE_CONF.SP_Y, -1);
        if (x == -1 || y == -1) {
            x = screenWidth/2;
            y = 0;
        }
        windowParams.x = x;
        windowParams.y = y;
        return windowParams;
    }

    private void setOnTouchListener(final WindowManager windowManager, final Context context, final WindowView windowView, final int type) {
        windowView.setOnTouchListener(new OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = windowParams.x;
                        paramY = windowParams.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        windowParams.x = paramX + dx;
                        windowParams.y = paramY + dy;
                        // 更新悬浮窗位置
                        windowManager.updateViewLayout(windowView, windowParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if ((System.currentTimeMillis() - exitTime) < BASE_CONF.CHANGE_DELAY) {
                            createWindow(context, type);
                            return true;
                        } else {
                            exitTime = System.currentTimeMillis();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void setOnTouchListener(final Context context, final WindowView windowView, final int type) {
        windowView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if ((System.currentTimeMillis() - exitTime) < BASE_CONF.CHANGE_DELAY) {
                            createWindow(context, type);
                            return true;
                        } else {
                            exitTime = System.currentTimeMillis();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    public void setViewBg(Drawable background) {

        if (mSmallWindowView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mSmallWindowView.setBackground(background);
            } else {
                mSmallWindowView.setBackgroundDrawable(background);
            }
        }
    }

    private void removeWindow(Context context, WindowView windowView) {
        if (windowView != null) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(windowView);
        }
    }

    public void removeAllWindow(Context context) {
        removeWindow(context, mBigWindowView);
        removeWindow(context, mSmallWindowView);
        mBigWindowView = null;
        mSmallWindowView = null;
    }

    public void updateViewData(Context context) {

        long tempSum = TrafficStats.getTotalRxBytes()
                + TrafficStats.getTotalTxBytes();
        long rxtxLast = tempSum - rxtxTotal;
        double totalSpeed = rxtxLast * 1000 / BASE_CONF.TIME_SPAN;
        rxtxTotal = tempSum;
        long tempMobileRx = TrafficStats.getMobileRxBytes();
        long tempMobileTx = TrafficStats.getMobileTxBytes();
        long tempWlanRx = TrafficStats.getTotalRxBytes() - tempMobileRx;
        long tempWlanTx = TrafficStats.getTotalTxBytes() - tempMobileTx;
        long mobileLastRecv = tempMobileRx - mobileRecvSum;
        long mobileLastSend = tempMobileTx - mobileSendSum;
        long wlanLastRecv = tempWlanRx - wlanRecvSum;
        long wlanLastSend = tempWlanTx - wlanSendSum;
        double mobileRecvSpeed = mobileLastRecv * 1000 / BASE_CONF.TIME_SPAN;
        double mobileSendSpeed = mobileLastSend * 1000 / BASE_CONF.TIME_SPAN;
        double wlanRecvSpeed = wlanLastRecv * 1000 / BASE_CONF.TIME_SPAN;
        double wlanSendSpeed = wlanLastSend * 1000 / BASE_CONF.TIME_SPAN;
        mobileRecvSum = tempMobileRx;
        mobileSendSum = tempMobileTx;
        wlanRecvSum = tempWlanRx;
        wlanSendSum = tempWlanTx;
        if (mBigWindowView != null) {
            if (mobileRecvSpeed >= 0d) {
                tvMobileRx.setText(showSpeed(mobileRecvSpeed));
            }
            if (mobileSendSpeed >= 0d) {
                tvMobileTx.setText(showSpeed(mobileSendSpeed));
            }
            if (wlanRecvSpeed >= 0d) {
                tvWlanRx.setText(showSpeed(wlanRecvSpeed));
            }
            if (wlanSendSpeed >= 0d) {
                tvWlanTx.setText(showSpeed(wlanSendSpeed));
            }
        }
        if (mSmallWindowView != null && totalSpeed >= 0d) {
            tvSum.setText(showSpeed(totalSpeed));
        }

    }

    private String showSpeed(double speed) {
        String speedString;
        if (speed >= 1048576d) {
            speedString = showFloatFormat.format(speed / 1048576d) + "MB/s";
        } else {
            speedString = showFloatFormat.format(speed / 1024d) + "KB/s";
        }
        return speedString;
    }

    public boolean isWindowShowing() {
        return mBigWindowView != null || mSmallWindowView != null;
    }

    private WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    public int getWindowX() {
        return windowParams.x;
    }

    public int getWindowY() {
        return windowParams.y;
    }

    public void fixWindow(Context context, boolean yes) {
        if (yes) {
            setOnTouchListener(context, mSmallWindowView == null ? mBigWindowView : mSmallWindowView, mSmallWindowView == null ? BASE_CONF.SMALL_WINDOW_TYPE : BASE_CONF.BIG_WINDOW_TYPE);
        } else {
            setOnTouchListener(getWindowManager(context), context, mSmallWindowView == null ? mBigWindowView : mSmallWindowView, mSmallWindowView == null ? BASE_CONF.SMALL_WINDOW_TYPE : BASE_CONF.BIG_WINDOW_TYPE);
        }
    }

}
