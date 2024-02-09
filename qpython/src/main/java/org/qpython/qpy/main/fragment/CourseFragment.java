package org.qpython.qpy.main.fragment;

import static org.qpython.qpy.main.app.CONF.pyVer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.quseit.util.NAction;
import com.quseit.util.NStorage;

import org.qpython.qpy.R;
import org.qpython.qpy.main.activity.HomeMainActivity;
import org.qpython.qpy.main.activity.QWebViewActivity;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.auxActivity.ProtectActivity;
import org.qpython.qpy.main.auxActivity.ScreenRecordActivity;
import org.qpython.qpy.main.service.FTPServerService;
import org.qpython.qpy.texteditor.ui.view.EnterDialog;
import org.qpython.qpysdk.utils.Utils;
import org.qpython.qsl4a.QPyScriptService;
import org.qpython.qsl4a.qsl4a.jsonrpc.JsonRpcServer;
import org.swiftp.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class CourseFragment extends PreferenceFragment {

    public CourseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.qpython_course);
        try {
            initListener();
        } catch (Exception exception) {
            Toast.makeText(getActivity(),exception.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Disable scrollbar
        ListView listView = view.findViewById(android.R.id.list);
        if (listView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                listView.setFocusedByDefault(false);
            }
        }
        //mLoadingDialog = new LoadingDialog(getActivity());
    }

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    private void initListener() throws Exception {
        findPreference("video_account").
                setOnPreferenceClickListener(preference ->
                {
                    viewWebSite(R.string.wechat_video_website);
                    return true;
                });

        findPreference("course_official").
                setOnPreferenceClickListener(preference ->
                {
                    QWebViewActivity.start(
                            getContext(),
                            getString(R.string.course),
                            getString(R.string.url_course));
                    return true;
                });

        findPreference("news").
                setOnPreferenceClickListener(preference ->
                {
                    viewWebSite(R.string.news_page);
                    return true;
                });

        findPreference("open_source_library").
                setOnPreferenceClickListener(preference ->
                {
                    viewWebSite(R.string.bilibili_website);
                    return true;
                });

}

    private void viewWebSite(int resId) {
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(getString(resId))));
    }

}