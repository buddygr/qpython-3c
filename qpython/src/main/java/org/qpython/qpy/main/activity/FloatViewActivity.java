package org.qpython.qpy.main.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qsl4a.qsl4a.facade.FloatViewFacade;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.util.ArrayList;
import java.util.Date;

public class FloatViewActivity extends Activity
    {
        //按钮数组
        static final ArrayList<Button> buttons = FloatViewFacade.buttons;
        //参数数组
        static final ArrayList<WindowManager.LayoutParams> params = FloatViewFacade.params;
        //时间数组
        static final ArrayList<Long> times = FloatViewFacade.times;
        //操作类型数组
        static final ArrayList<String> operations = FloatViewFacade.operations;
        static WindowManager windowManager;
        static DisplayMetrics displayMetrics;
        //public static Handler handler;

        @SuppressLint({"SimpleDateFormat", "HandlerLeak"})
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT < 26) {
                Toast.makeText(this,
                        getString(R.string.float_view_android),
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            windowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
            FloatViewFacade.windowManager = windowManager;
            displayMetrics = this.getResources().getDisplayMetrics();
            FloatViewFacade.handler = new Handler(){
                @Override
                public void handleMessage(Message msg){
                    super.handleMessage(msg);
                    floatView((Intent) msg.obj);
                }
            };
            floatView(getIntent());
        }

    @SuppressLint("ClickableViewAccessibility")
    public void floatView(Intent intent){
        //Intent intent = getIntent();
        if (intent==null) intent = new Intent();
        WindowManager.LayoutParams layoutParams = null;
        int index;
        //悬浮按钮
        Button floatButton = null;
        //悬浮窗文本
        String text=intent.getStringExtra("text");
        //是否彩色文本
        boolean isHtml = false;
        if (text == null) {
            text = intent.getStringExtra("html");
            if (text == null)
                text = "drag move\nlong click close";
            else isHtml = true;
        }
        //悬浮窗背景色 格式:aarrggbb或rrggbb
        int backColor=colorToInt(intent.getStringExtra("backColor"),"7f7f7f7f");
        //悬浮窗文字颜色 格式:aarrggbb或rrggbb
        int textColor=colorToInt(intent.getStringExtra("textColor"),"ff000000");
        //字体大小
        int textSize=intent.getIntExtra("textSize",10);
        //脚本路径
        final String script=intent.getStringExtra("script");
        //脚本参数
        final String arg=intent.getStringExtra("arg");
        final boolean clickRemove = intent.getBooleanExtra("clickRemove",true);
        //moveTaskToBack(true);
        //索引参数
        index = intent.getIntExtra("index",-1);

        boolean reinit = false;
        if (index>=0) {
            try {
                floatButton = buttons.get(index);
                if(floatButton==null){
                    floatButton = new Button(this);
                    layoutParams = new WindowManager.LayoutParams();
                    reinit = true;
                } else layoutParams = params.get(index);
            } catch (Exception e){
                index=-1;
            }
        }
        if(index<0){
            floatButton = new Button(this);
            layoutParams = new WindowManager.LayoutParams();
            index = buttons.size();
        }
            WindowManager.LayoutParams finalLayoutParams = layoutParams;
            floatButton.setOnTouchListener(new View.OnTouchListener() {
            private int x;
            private int y;

            @SuppressLint("ClickableViewAccessibility")
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int index = (int) view.getTag();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = (int) event.getRawX();
                        y = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) event.getRawX();
                        int nowY = (int) event.getRawY();
                        boolean moved = nowX != x || nowY != y;
                        x = nowX;
                        y = nowY;
                        finalLayoutParams.x = nowX - displayMetrics.widthPixels/2;
                        finalLayoutParams.y = nowY - displayMetrics.heightPixels/2;
                        // 更新悬浮窗控件布局
                        if (moved) {
                            windowManager.updateViewLayout(view, finalLayoutParams);
                            operations.set(index, "move");
                        } else {
                            operations.set(index, "click");
                        }
                        times.set(index, System.currentTimeMillis());
                        break;
                    case MotionEvent.ACTION_UP:
                    if (operations.get(index).equals("click")) {
                            if(script!=null)
                                ScriptExec.getInstance().playScript(FloatViewActivity.this,
                                        script, arg,false);
                            if (clickRemove) FloatViewFacade.removeButton(index);
                            FloatViewActivity.this.finish();
                        }
                    default:
                        break;
                }
                return false;
            }
        });
        if(isHtml)
            floatButton.setText(HtmlUtil.textToHtml(text));
        else
            floatButton.setText(text);
        floatButton.setBackgroundColor(backColor);
        floatButton.setTextColor(textColor);
        floatButton.setTextSize(textSize);
        layoutParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;//下拉通知栏不可见
        // 设置Window flag,锁定悬浮窗 ,若不设置，悬浮窗会占用整个屏幕的点击事件，FLAG_NOT_FOCUSABLE不设置会导致菜单键和返回键失效
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        //悬浮窗宽度
        int width = intent.getIntExtra("width",300);
        if(width!=Integer.MIN_VALUE)
            layoutParams.width = width;
        //悬浮窗高度
        int height = intent.getIntExtra("height",150);
        if (height!=Integer.MIN_VALUE)
            layoutParams.height = height;
        //起始横坐标，原点为屏幕中心
        int x = intent.getIntExtra("x",0);
        if (x!=Integer.MIN_VALUE)
            layoutParams.x=x;
        //起始纵坐标，原点为屏幕中心
        int y = intent.getIntExtra("y",0);
        if (y!=Integer.MIN_VALUE)
            layoutParams.y=y;
        //记录结果
        try {
            if(index>=buttons.size()){
                windowManager.addView(floatButton, layoutParams);
                floatButton.setTag(buttons.size());
                buttons.add(floatButton);
                params.add(layoutParams);
                times.add(System.currentTimeMillis());
                operations.add("initial");
            } else {
                if(reinit) {
                    windowManager.addView(floatButton, layoutParams);
                    buttons.set(index,floatButton);
                    operations.set(index,"reinitial");
                    floatButton.setTag(index);
                } else {
                    windowManager.updateViewLayout(floatButton, layoutParams);
                    operations.set(index,"modify");
                }
                params.set(index,layoutParams);
                times.set(index,System.currentTimeMillis());
            }
        } catch (Exception e) {
            Toast.makeText(this,getString(R.string.float_view_permission)+"\n"+ e,Toast.LENGTH_LONG).show();
            return;
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
