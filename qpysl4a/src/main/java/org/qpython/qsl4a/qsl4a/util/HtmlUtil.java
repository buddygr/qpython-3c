package org.qpython.qsl4a.qsl4a.util;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import java.io.IOException;
import java.net.URL;

public class HtmlUtil {

    public static final Html.ImageGetter imageGetter = source -> {
        Drawable drawable = null;
        try {
            drawable = Drawable.createFromStream(new URL(source).openStream(), null);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return drawable;
    };

    public static Spanned textToHtml(String text){
        return Html.fromHtml(text,imageGetter,null);
    }
}
