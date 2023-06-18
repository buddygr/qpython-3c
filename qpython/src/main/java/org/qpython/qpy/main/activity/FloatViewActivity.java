package org.qpython.qpy.main.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PixelFormat;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qsl4a.qsl4a.facade.FloatViewFacade;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.util.ArrayList;

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

        private static final int DEFAULT_FLAG =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE ;

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
                    floatView((JSONObject) msg.obj);
                }
            };
            try {
                floatView(new JSONObject(getIntent().getStringExtra("args")));
            } catch (JSONException ignored) {}
        }

    @SuppressLint("ClickableViewAccessibility")
    public void floatView(final JSONObject args){
        WindowManager.LayoutParams layoutParams = null;
        int index;
        //悬浮按钮
        Button floatButton = null;
        //悬浮窗文本
        String text = getArg(args,"text");
        //是否彩色文本
        boolean isHtml = false;
        if (text == null) {
            text = getArg(args,"html");
            if (text == null)
                text = "drag move\nlong click close";
            else isHtml = true;
        }
        //悬浮窗背景色 格式:aarrggbb或rrggbb
        final Integer backColor = colorToInt(args,"backColor","7f7f7f7f");
        //悬浮窗文字颜色 格式:aarrggbb或rrggbb
        final Integer textColor = colorToInt(args,"textColor","ff000000");
        //字体大小
        final int textSize = getArgLast(args,"textSize",10);
        //字体对齐
        final int textAlign = getArgLast(args,"textAlign",View.TEXT_ALIGNMENT_INHERIT);
        //单击移除
        final boolean clickRemove = getArg(args,"clickRemove",true);
        //脚本路径
        final String script = getArg(args,"script");
        //索引参数
        index = getArg(args,"index",buttons.size());

        byte reinit = 0;
        if (index<buttons.size()) {
            floatButton = buttons.get(index);
            if(floatButton==null){
                floatButton = new Button(this);
                layoutParams = new WindowManager.LayoutParams();
                reinit = 1;
            } else layoutParams = params.get(index);
        } else /* index>=buttons.size() */ {
            floatButton = new Button(this);
            layoutParams = new WindowManager.LayoutParams();
            floatButton.setTag(index);
            buttons.add(floatButton);
            params.add(layoutParams);
            times.add(System.currentTimeMillis());
            operations.add("initial");
            reinit = -1;
        }
        floatButton.setOnTouchListener(new View.OnTouchListener() {
            private int x;
            private int y;

            @SuppressLint("ClickableViewAccessibility")
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final int index = (int) view.getTag();
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
                        // 更新悬浮窗控件布局
                        if (moved) {
                            final WindowManager.LayoutParams layoutParams = params.get(index);
                            layoutParams.x = nowX - displayMetrics.widthPixels/2;
                            layoutParams.y = nowY - displayMetrics.heightPixels/2;
                            windowManager.updateViewLayout(view, layoutParams);
                            operations.set(index, "move");
                        } else {
                            operations.set(index, "click");
                        }
                        times.set(index, System.currentTimeMillis());
                        break;
                    case MotionEvent.ACTION_UP:
                    if (operations.get(index).equals("click")) {
                            if(script!=null){
                                //脚本参数
                                //final String arg = getArg(args,"arg");
                                ScriptExec.getInstance().playScript(FloatViewActivity.this,
                                        script, getArg(args,"arg"));
                            }
                            if (clickRemove)
                                FloatViewFacade.removeButton(index);
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
        if(backColor!=null)
          floatButton.setBackgroundColor(backColor);
        if(textColor!=null)
          floatButton.setTextColor(textColor);
        floatButton.setTextSize(textSize);
        if(textAlign != Integer.MIN_VALUE)
          floatButton.setTextAlignment(textAlign);
        layoutParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;//下拉通知栏不可见
        // 设置Window flag,锁定悬浮窗 ,若不设置，悬浮窗会占用整个屏幕的点击事件，FLAG_NOT_FOCUSABLE不设置会导致菜单键和返回键失效
        layoutParams.flags = getArg(args,"flag",DEFAULT_FLAG);
        layoutParams.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明
        //悬浮窗宽度
        int width = getArgLast(args,"width",300);
        if(width!=Integer.MIN_VALUE)
            layoutParams.width = width;
        //悬浮窗高度
        int height = getArgLast(args,"height",150);
        if (height!=Integer.MIN_VALUE)
            layoutParams.height = height;
        //起始横坐标，原点为屏幕中心
        int x = getArgLast(args,"x",0);
        if (x!=Integer.MIN_VALUE)
            layoutParams.x=x;
        //起始纵坐标，原点为屏幕中心
        int y = getArgLast(args,"y",0);
        if (y!=Integer.MIN_VALUE)
            layoutParams.y=y;
        //记录结果
        try {
            if(reinit<0){
                windowManager.addView(floatButton, layoutParams);
            } else {
                if(reinit==1) {
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

        private String getArg(JSONObject args,String flag){
            try {
                return args.getString(flag);
            } catch (JSONException e) {
                return null;
            }
        }

        private int getArgLast(JSONObject args,String flag,int defaultValue){
            try {
                return args.getInt(flag);
            } catch (JSONException e) {
                try {
                    if(args.getString(flag).equalsIgnoreCase("last"))
                        return Integer.MIN_VALUE;
                } catch (Exception ignored) {}
            }
                return defaultValue;
            }

        private int getArg(JSONObject args,String flag,int defaultValue){
            try {
                return args.getInt(flag);
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        private boolean getArg(JSONObject args,String flag,boolean defaultValue){
            try {
                return args.getBoolean(flag);
            } catch (JSONException e) {
                return defaultValue;
            }
        }

        private Integer colorToInt(JSONObject args,String colorFlag,String defaultColor) {
            String color = null;
            try {
                color = args.getString(colorFlag);
            } catch (JSONException ignored) {}
            if (color == null){
                color = defaultColor;
            } else if (color.equalsIgnoreCase("last")){
                return null;
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
