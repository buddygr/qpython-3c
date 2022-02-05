package com.quseit.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.HashMap;

/**
 * Based on the following Stackoverflow answer:
 * http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
 *
 * Only works when android:windowSoftInputMode="adjustPan"
 */
public class KeyboardUtils implements ViewTreeObserver.OnGlobalLayoutListener
{

    Context context;
    @Override
    public void onGlobalLayout()
    {
//        Rect r = new Rect();
//        //r will be populated with the coordinates of your view that area still visible.
//        mRootView.getWindowVisibleDisplayFrame(r);
//
//        int heightDiff = mRootView.getRootView().getHeight() - (r.bottom - r.top);
//        float dp = heightDiff/ mScreenDensity;
//
//        if(mCallback != null)
//            mCallback.onToggleSoftKeyboard(dp > 200);
        // navigation bar height
        int navigationBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        // status bar height
        int statusBarHeight = 0;
        resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        // display window size for the app layout
        Rect rect = new Rect();
        mRootView.getWindowVisibleDisplayFrame(rect);

        // screen height - (user app height + status + nav) ..... if non-zero, then there is a soft keyboard
        int keyboardHeight = mRootView.getHeight() - (statusBarHeight + navigationBarHeight + rect.height());

        if (keyboardHeight <= 0) {
            mCallback.onToggleSoftKeyboard(false);
        } else {
            mCallback.onToggleSoftKeyboard(true);
        }
    }

    public interface SoftKeyboardToggleListener
    {
        void onToggleSoftKeyboard(boolean isVisible);
    }

    private SoftKeyboardToggleListener mCallback;
    private View mRootView;
    private float mScreenDensity = 1;
    private static HashMap<SoftKeyboardToggleListener, KeyboardUtils> sListenerMap = new HashMap<>();



    public static void addKeyboardToggleListener(Activity act, SoftKeyboardToggleListener listener)
    {
        removeKeyboardToggleListener(listener);

        sListenerMap.put(listener, new KeyboardUtils(act, listener));
    }

    public static void removeKeyboardToggleListener(SoftKeyboardToggleListener listener)
    {
        if(sListenerMap.containsKey(listener))
        {
            KeyboardUtils k = sListenerMap.get(listener);
            k.removeListener();

            sListenerMap.remove(listener);
        }
    }

    public static void removeAllKeyboardToggleListeners()
    {
        for(SoftKeyboardToggleListener l : sListenerMap.keySet())
            sListenerMap.get(l).removeListener();

        sListenerMap.clear();
    }

    private void removeListener()
    {
        mCallback = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    }

    private KeyboardUtils(Activity act, SoftKeyboardToggleListener listener)
    {
        mCallback = listener;
        context = act;
        mRootView = ((ViewGroup) act.findViewById(android.R.id.content)).getChildAt(0);
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mScreenDensity = act.getResources().getDisplayMetrics().density;
    }



}
