package com.quseit.view.item;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.quseit.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;


public class VideoItem {
	private static final String TAG = "VideoItem";

    public String headerText;
    public String desc;
    public String headUrl;
    public String other;
    public int statImage;
    public int thumbImage;
    public Bitmap coverBitmap;
    public int tpl;
    
    public VideoItem(String text, String desc) {
        this.desc = desc;
        this.headUrl = "";
        this.other = "";
        this.statImage = 0;
        this.thumbImage = 0;
        this.coverBitmap = null;
        this.tpl = 0;
    }
    
    public VideoItem(String text, String desc, String headUrl, String other, int stat) {
        this.desc = desc;
        this.headUrl = headUrl;
        this.other = other;
        this.statImage = stat;
        this.thumbImage = 0;
        this.coverBitmap = null;
        this.tpl = 0;


    }
    public VideoItem(String text, String desc, String headUrl, String other, int stat, int thumb) {
        this.desc = desc;
        this.headUrl = headUrl;
        this.other = other;
        this.statImage = stat;
        this.thumbImage = thumb;
        this.coverBitmap = null;
        this.tpl = 0;


    }
    
    public VideoItem(String text, String desc, Bitmap coverBitmap, String other, int stat, int thumb) {
        this.desc = desc;
        this.coverBitmap = coverBitmap;
        this.other = other;
        this.statImage = stat;
        this.thumbImage = thumb;
        this.tpl = 0;

    }

    public VideoItem(String text, String desc, String headUrl, String other, int stat, int thumb, int tpl) {
        this.desc = desc;
        this.headUrl = headUrl;
        this.other = other;
        this.statImage = stat;
        this.thumbImage = thumb;
        this.coverBitmap = null;
        this.tpl = tpl;


    }
    

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        TypedArray a = r.obtainAttributes(attrs, R.styleable.VideoItem);
        
        headerText = a.getString(R.styleable.VideoItem_headerText);
        desc = a.getString(R.styleable.VideoItem_desc);
        headUrl = a.getString(R.styleable.VideoItem_headUrl);
        other = a.getString(R.styleable.VideoItem_other);
        statImage = a.getInteger(R.styleable.VideoItem_statImage, statImage);
        thumbImage = a.getInteger(R.styleable.VideoItem_thumbImage, thumbImage);
        tpl = a.getInteger(R.styleable.VideoItem_tpl, tpl);

        a.recycle();
        Log.d(TAG, "inflate");
    }
    
}
