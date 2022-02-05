/*
 * Copyright (C) 2016 Google Inc.
 * seekbar Copyright (C) 2014 shimoda
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.facade.ui;

import android.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.json.JSONArray;
import org.qpython.qsl4a.qsl4a.facade.EventFacade;
import org.qpython.qsl4a.qsl4a.future.FutureActivityTask;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FullScreenTask extends FutureActivityTask<Object> implements OnClickListener,
        OnItemClickListener, OnSeekBarChangeListener {
  private EventFacade mEventFacade;
  private UiFacade mUiFacade;
  public View mView = null;
  protected ViewInflater mInflater = new ViewInflater();
  protected String mLayout;
  protected final CountDownLatch mShowLatch = new CountDownLatch(1);
  protected Handler mHandler = null;
  private List<Integer> mOverrideKeys;
  protected String mTitle;
  protected int mTheme;

  public FullScreenTask(String layout, String title, Integer theme) {
    super();
    mLayout = layout;
    if (title != null) {
      mTitle = title;
    } else {
      mTitle = "";
    }
    if (theme != null) {
      mTheme = theme;
    } else {
      mTheme = 0;
    }
  }

  @Override
  public void onCreate() {
    // super.onCreate();
    final Activity activity = getActivity();
    if (mHandler == null) {
      mHandler = new Handler();
    }
    mInflater.getErrors().clear();
    try {
      if (mView == null) {
        StringReader sr = new StringReader(mLayout);
        XmlPullParser xml = ViewInflater.getXml(sr);
        mView = mInflater.inflate(activity, xml);
      }
    } catch (Exception e) {
      String E = e.toString();
      mInflater.getErrors().add(E);
      mView = defaultView(E);
      mInflater.setIdList(R.id.class);
    }
    //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    activity.setTheme(mTheme);
    activity.setContentView(mView);
    activity.setTitle(mTitle);
    if (!mTitle.equals(""))
      activity.setTaskDescription(new ActivityManager.TaskDescription(mTitle));
    mInflater.setClickListener(mView, this, this, this);
    mShowLatch.countDown();
  }

  @Override
  public void onDestroy() {
    mEventFacade.postEvent("screen", "destroy");
    super.onDestroy();
  }

  /** default view in case of errors */
  protected View defaultView(String Error) {
    final Activity activity = getActivity();
    LinearLayout result = new LinearLayout(activity);
    result.setOrientation(LinearLayout.VERTICAL);
    EditText text = new EditText(activity);
    text.setText("Error Message :\n" + Error);
    result.addView(text, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    Button OK = new Button(activity);
    OK.setText("OK");
    result.addView(OK, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    return result;
  }

  public EventFacade getEventFacade() {
    return mEventFacade;
  }

  public void setEventFacade(EventFacade eventFacade) {
    mEventFacade = eventFacade;
  }

  public void setUiFacade(UiFacade uiFacade) {
    mUiFacade = uiFacade;
  }

  public CountDownLatch getShowLatch() {
    return mShowLatch;
  }

  public Map<String, Map<String, String>> getViewAsMap() {
    return mInflater.getViewAsMap(mView);
  }

  private View getViewByName(String idName) {
    View result = null;
    int id = mInflater.getId(idName);
    if (id != 0) {
      result = mView.findViewById(id);
    }
    return result;
  }

  public Map<String, String> getViewDetail(String idName) {
    Map<String, String> result = new HashMap<String, String>();
    result.put("error", "id not found (" + idName + ")");
    View v = getViewByName(idName);
    if (v != null) {
      result = mInflater.getViewInfo(v);
    }
    return result;
  }

  public String getViewPropery(String idName,String attr) {
    View v = getViewByName(idName);
    switch (attr) {
      case "id":
        return idName;
      case "type":
        return v.getClass().getSimpleName();
      default:
        return mInflater.getProperty(v,attr);
    }
  }

  public String setViewProperty(String idName, String property, String value) {
    View v = getViewByName(idName);
    mInflater.getErrors().clear();
    if (v != null) {
      SetProperty p = new SetProperty(v, property, value);
      mHandler.post(p);
      try {
        p.mLatch.await();
      } catch (InterruptedException e) {
        mInflater.getErrors().add(e.toString());
      }
    } else {
      return "View " + idName + " not found.";
    }
    if (mInflater.getErrors().size() == 0) {
      return "OK";
    }
    return mInflater.getErrors().get(0);
  }

  public String setList(String id, JSONArray items) {
    View v = getViewByName(id);
    mInflater.getErrors().clear();
    if (v != null) {
      SetList p = new SetList(v, items);
      mHandler.post(p);
      try {
        p.mLatch.await();
      } catch (InterruptedException e) {
        mInflater.getErrors().add(e.toString());
      }
    } else {
      return "View " + id + " not found.";
    }
    if (mInflater.getErrors().size() == 0) {
      return "OK";
    }
    return mInflater.getErrors().get(0);
  }

  public String setList2(String id, JSONArray items, JSONArray intRes) {
    View v = getViewByName(id);
    mInflater.getErrors().clear();
    if (v != null) {
      SetList2 p = new SetList2(v, items, intRes);
      mHandler.post(p);
      try {
        p.mLatch.await();
      } catch (InterruptedException e) {
        mInflater.getErrors().add(e.toString());
      }
    } else {
      return "View " + id + " not found.";
    }
    if (mInflater.getErrors().size() == 0) {
      return "OK";
    }
    return mInflater.getErrors().get(0);
  }

  @Override
  public void onClick(View view) {
    mEventFacade.postEvent("click", mInflater.getViewInfo(view));
  }

  public void loadLayout(String layout) {
    ViewInflater inflater = new ViewInflater();
    View view;
    StringReader sr = new StringReader(layout);
    try {
      XmlPullParser xml = ViewInflater.getXml(sr);
      view = inflater.inflate(getActivity(), xml);
      mView = view;
      mInflater = inflater;
      getActivity().setContentView(mView);
      mInflater.setClickListener(mView, this, this, this);
      mLayout = layout;
      mView.invalidate();
    } catch (Exception e) {
      mInflater.getErrors().add(e.toString());
    }
  }

  private class SetProperty implements Runnable {
    View mView;
    String mProperty;
    String mValue;
    CountDownLatch mLatch = new CountDownLatch(1);

    SetProperty(View view, String property, String value) {
      mView = view;
      mProperty = property;
      mValue = value;
    }

    @Override
    public void run() {
      // TODO Auto-generated method stub
      mInflater.setProperty(mView, mProperty, mValue);
      mView.invalidate();
      mLatch.countDown();
    }
  }

  private class SetList implements Runnable {
    View mView;
    JSONArray mItems;
    CountDownLatch mLatch = new CountDownLatch(1);

    SetList(View view, JSONArray items) {
      mView = view;
      mItems = items;
      mLatch.countDown();
    }

    @Override
    public void run() {
      mInflater.setListAdapter(mView, mItems);
      mView.invalidate();
    }
  }

  private class SetList2 implements Runnable {
    View mView;
    JSONArray mItems;
    JSONArray mIntRes;

    CountDownLatch mLatch = new CountDownLatch(1);

    SetList2(View view, JSONArray intRes, JSONArray items) {
      mView = view;
      mItems = items;
      mIntRes = intRes;
      mLatch.countDown();
    }

    @Override
    public void run() {
      mInflater.setListAdapter2(mView, mIntRes, mItems);
      mView.invalidate();
    }
  }

  private class SetLayout implements Runnable {
    String mLayout;
    CountDownLatch mLatch = new CountDownLatch(1);

    SetLayout(String layout) {
      mLayout = layout;
    }

    @Override
    public void run() {
      loadLayout(mLayout);
      mLatch.countDown();
    }
  }

  private class SetTitle implements Runnable {
    String mSetTitle;
    CountDownLatch mLatch = new CountDownLatch(1);

    SetTitle(String title) {
      mSetTitle = title;
    }

    @Override
    public void run() {
      mTitle = mSetTitle;
      getActivity().setTitle(mSetTitle);
      mLatch.countDown();
    }
  }

  private class SetTheme implements Runnable {
    int mSetTheme;
    CountDownLatch mLatch = new CountDownLatch(1);

    SetTheme(int theme) { mSetTheme = theme;}

    @Override
    public void run() {
      mTheme = mSetTheme;
      getActivity().setTheme(mSetTheme);
      mLatch.countDown();
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    Map<String, String> data = new HashMap<String, String>();
    data.put("key", String.valueOf(keyCode));
    data.put("action", String.valueOf(event.getAction()));
    mEventFacade.postEvent("key", data);
    boolean overrideKey =
            (keyCode == KeyEvent.KEYCODE_BACK)
                    || (mOverrideKeys == null ? false : mOverrideKeys.contains(keyCode));
    return overrideKey;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    return mUiFacade.onPrepareOptionsMenu(menu);
  }

  @Override
  public void onItemClick(AdapterView<?> aview, View aitem, int position, long id) {
    Map<String, String> data = mInflater.getViewInfo(aview);
    data.put("position", String.valueOf(position));
    mEventFacade.postEvent("itemclick", data);
  }

  public void setOverrideKeys(List<Integer> overrideKeys) {
    mOverrideKeys = overrideKeys;
  }

  // Used to hot-switch screens.
  public void setLayout(String layout) {
    SetLayout p = new SetLayout(layout);
    mHandler.post(p);
    try {
      p.mLatch.await();
    } catch (InterruptedException e) {
      mInflater.getErrors().add(e.toString());
    }
  }

  public void setTitle(String title) {
    SetTitle p = new SetTitle(title);
    mHandler.post(p);
    try {
      p.mLatch.await();
    } catch (InterruptedException e) {
      mInflater.getErrors().add(e.toString());
    }
  }

  public void setTheme(int theme) {
    SetTheme p = new SetTheme(theme);
    mHandler.post(p);
    try {
      p.mLatch.await();
    } catch (InterruptedException e) {
      mInflater.getErrors().add(e.toString());
    }
  }

  @Override
  public void onProgressChanged(SeekBar aview, int progress, boolean fromUser) {
    Map<String, String> data = mInflater.getViewInfo(aview);
    data.put("position", String.valueOf(progress));
    data.put("progress", String.valueOf(progress));
    mEventFacade.postEvent("itemclick", data);
  }

  @Override
  public void onStartTrackingTouch(SeekBar arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStopTrackingTouch(SeekBar arg0) {
    // TODO Auto-generated method stub

  }

  public Bitmap getScreenShot() {
    View v = getActivity().getWindow().getDecorView();
    v.setDrawingCacheEnabled(true);
    return v.getDrawingCache();
  }

}
