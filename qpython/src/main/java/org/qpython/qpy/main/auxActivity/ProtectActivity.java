package org.qpython.qpy.main.auxActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.qpython.qpy.R;
import org.qpython.qpy.main.utils.Utils;

public class ProtectActivity extends Activity
    {   /* state :
             0 = 未知状态
             1 = 无需保护
             2 = 已经保护
             3 = 解除保护
        */
        private static byte state = 0;//未知状态
        public static void DoProtect(Context context){
            context.startActivity(new Intent(context, ProtectActivity.class)); //启动保护
        }
        private static void DoneProtect(){
            state = 2;//已经保护
        }
        public static void UndoProtect() {
            if (state % 2 == 0) state++;
            // 0 未知状态 -> 1 无需保护
            // 2 已经保护 -> 3 解除保护
        }
        public static boolean IsProtected(){
            return state>=2;
            // 2 已经保护
            // 3 解除保护
        }
        public static boolean UnknownState() {
            return state==0;//未知状态
        }

        public static void CheckProtect(Context context){
            if (UnknownState()) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("qpython_protect",false))
                    DoProtect(context);
                else UndoProtect();
            }
        }

        private void NotifyProtect(){
            if (getIntent().getAction()==null)
                Toast.makeText(this,getString(R.string.qpython_protect_run),Toast.LENGTH_SHORT).show();
            Utils.showNotification(this,'3',R.string.qpython_protect,R.string.qpython_protect_run);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (IsProtected()) {
                NotifyProtect();
                finish();
                return;
            }
            if (Build.VERSION.SDK_INT<26){
                Toast.makeText(this,
                        getString(R.string.float_view_android),
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        final Button floatButton=new Button(this);//悬浮按钮
        final WindowManager windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        //隐藏悬浮窗：屏幕左下角有一块5*5像素的几乎全透明的肉眼几乎不可见的悬浮窗，悬浮窗可以保护后台应用
        long color;
        color = Long.valueOf("05ffffff",16);
        floatButton.setBackgroundColor((int) color);
        layoutParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;//下拉通知栏不可见
        // 设置Window flag,锁定悬浮窗 ,若不设置，悬浮窗会占用整个屏幕的点击事件
            // FLAG_NOT_FOCUSABLE不设置会导致菜单键和返回键失效
            // FLAG_NOT_TOUCHABLE该悬浮窗口没有点击事件
    layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置悬浮窗的长得宽
        layoutParams.width = 5;
        layoutParams.height = 5;
        //起始横坐标，原点为屏幕中心
        layoutParams.x = -3000;
        //起始纵坐标，原点为屏幕中心
        layoutParams.y =  3000;
        try {
            windowManager.addView(floatButton, layoutParams);
            DoneProtect();
            NotifyProtect();
        } catch (Exception e) {
            Toast.makeText(this,getString(R.string.float_view_permission)+"\n"+e.toString(),Toast.LENGTH_LONG).show();
        }
        finish();
        }

}
