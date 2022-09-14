package org.qpython.qsl4a.qsl4a.util;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import java.io.IOException;
import java.net.URL;

public class HtmlUtil {
    public static int widthFixed = 0;
    public static int heightFixed = 0;
    public static double widthRatio = 1.0;
    public static double heightRatio = 1.0;

    public static final Html.ImageGetter imageGetter = source -> {
        Drawable drawable = null;
        int width,height;
        try {
            drawable = Drawable.createFromStream(new URL(source).openStream(), null);
            if(widthFixed <= 0)
                width = drawable.getIntrinsicWidth();
            else
                width = widthFixed;
            if(heightFixed <= 0)
                height = drawable.getIntrinsicHeight();
            else
                height = heightFixed;
            if(widthRatio != 1.0)
                width *= widthRatio;
            if(heightRatio != 1.0)
                height *= heightRatio;
            drawable.setBounds(0, 0, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return drawable;
    };

    public static Spanned textToHtml(String text){
        return Html.fromHtml(text,imageGetter,null);
    }
}
