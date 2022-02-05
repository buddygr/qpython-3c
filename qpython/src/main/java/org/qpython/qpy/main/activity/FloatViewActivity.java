package org.qpython.qpy.main.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;

import java.util.Date;

public class FloatViewActivity extends Activity
    {
        //状态:坐标x,坐标y
        static final int[] state = {0,0};
        //状态:时间,操作类型
        static final String[] State = {"",""};

        @SuppressLint({"ClickableViewAccessibility", "SimpleDateFormat"})
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT<26){
                Toast.makeText(this,
                        getString(R.string.float_view_android),
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        Intent intent = getIntent();
        if (intent==null) intent = new Intent();
        //返回结果
        boolean result=intent.getBooleanExtra("result",false);
        if (result){
            Intent intentR = new Intent();
            //返回横坐标，原点为屏幕中心
            intentR.putExtra("x",state[0]);
            //返回纵坐标，原点为屏幕中心
            intentR.putExtra("y",state[1]);
            intentR.putExtra("time",State[0]);
            intentR.putExtra("operation",State[1]);
            setResult(RESULT_OK,intentR);
            finish();
            return;
        }
        //悬浮窗文本
        String text=intent.getStringExtra("text");
        if (text == null) text = "drag move\nlong click close";
        //悬浮窗宽度
        int width=intent.getIntExtra("width",300);
        //悬浮窗高度
        int height=intent.getIntExtra("height",150);
        //悬浮窗背景色 格式:aarrggbb或rrggbb
        int backColor=colorToInt(intent.getStringExtra("backColor"),"7f7f7f7f");
        //悬浮窗文字颜色 格式:aarrggbb或rrggbb
        int textColor=colorToInt(intent.getStringExtra("textColor"),"ff000000");
        //字体大小
        int textSize=intent.getIntExtra("textSize",10);
        final String script=intent.getStringExtra("script");
        final String arg=intent.getStringExtra("arg");
        //moveTaskToBack(true);
        final Button floatButton=new Button(this);//悬浮按钮
        final WindowManager windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        floatButton.setOnTouchListener(new View.OnTouchListener() {
            private int x;
            private int y;
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = (int) event.getRawX();
                        y = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) event.getRawX();
                        int nowY = (int) event.getRawY();
                        int movedX = nowX - x;
                        int movedY = nowY - y;
                        x = nowX;
                        y = nowY;
                        layoutParams.x = layoutParams.x + movedX;
                        layoutParams.y = layoutParams.y + movedY;
                        // 更新悬浮窗控件布局
                        if (movedX!=0 || movedY!=0){
                            windowManager.updateViewLayout(view, layoutParams);
                            State[1] = "move";
                        } else {
                            State[1] = "click";
                        }
                        //记录结果
                        state[0]=layoutParams.x;
                        state[1]=layoutParams.y;
                        State[0] = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date(System.currentTimeMillis()));
                        break;
                    case MotionEvent.ACTION_UP:
                        if (State[1] == "click") {
                            if(script!=null)
                                ScriptExec.getInstance().playScript(FloatViewActivity.this,
                                        script, arg,false);
                            windowManager.removeView(floatButton);
                            FloatViewActivity.this.finish();
                        }
                    default:
                        break;
                }
                return false;
            }
        });
        floatButton.setText(text);
        floatButton.setBackgroundColor(backColor);
        floatButton.setTextColor(textColor);
        floatButton.setTextSize(textSize);
        layoutParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;//下拉通知栏不可见
        // 设置Window flag,锁定悬浮窗 ,若不设置，悬浮窗会占用整个屏幕的点击事件，FLAG_NOT_FOCUSABLE不设置会导致菜单键和返回键失效
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        // 设置悬浮窗的长得宽
        layoutParams.width = width;
        layoutParams.height = height;
        //起始横坐标，原点为屏幕中心
        layoutParams.x=intent.getIntExtra("x",0);
        //起始纵坐标，原点为屏幕中心
        layoutParams.y=intent.getIntExtra("y",0);
        //记录结果

        state[0]=layoutParams.x;

        state[1]=layoutParams.y;
        State[0] = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date(System.currentTimeMillis()));
        State[1] = "initial";
        try {
            windowManager.addView(floatButton, layoutParams);
        } catch (Exception e) {
            Toast.makeText(this,getString(R.string.float_view_permission)+"\n"+e.toString(),Toast.LENGTH_LONG).show();
        }
        finish();
        }

        private int colorToInt(String color,String defaultColor){
        if (color == null) {
            color = defaultColor;
        } else {
            int len = color.length();
            if (len <= 6) {
                color = defaultColor.substring(0,2) + "000000".substring(len) + color;
            }
        }
        long l;
        try {
            l = Long.valueOf(color,16);
            return (int) l;
        }
        catch (Exception e) {
            l = Long.valueOf(defaultColor,16);
            return (int) l;
        }
    }
}
