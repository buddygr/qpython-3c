package org.qpython.qpy.main.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import org.qpython.qpy.R;
import org.qpython.qpy.main.fragment.SettingFragment;
import org.qpython.qpy.main.service.FTPServerService;
import org.qpython.qsl4a.qsl4a.facade.FtpFacade;
import org.swiftp.Globals;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;


public class SettingActivity extends AppCompatActivity {
    SettingFragment fragment;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SettingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = new SettingFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.setting_fragment, fragment)
                .commit();
        //Globals.setContext(getApplicationContext());
        setContentView(R.layout.activity_setting);
        checkAction();
        init();
        //Thread.setDefaultUncaughtExceptionHandler((t, e) -> SettingActivity.restartApp(SettingActivity.this));
    }

    private void checkAction(){
        String action = getIntent().getAction();
        if(action!=null) {
            //setTheme(android.R.style.Theme_NoDisplay);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    if (action.equals(FTPServerService.ACTION_STARTED)) {
                        fragment.startServer();
                    } else if (action.equals(FTPServerService.ACTION_STOPPED)) {
                        fragment.stopServer();
                    }
                    setResult(RESULT_OK);
                    SettingActivity.this.finish();
                }
            }, 500);
        };
    }

    public static void restartApp(Context context) {
        Intent mStartActivity = new Intent(context, HomeMainActivity.class); //Replace StartActivity with the name of the first activity in your app
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FTPServerService.ACTION_STARTED);
        filter.addAction(FTPServerService.ACTION_STOPPED);
        filter.addAction(FTPServerService.ACTION_FAILEDTOSTART);
        registerReceiver(fragment.getFtpServerReceiver(), filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(fragment.getFtpServerReceiver());
    }

    @Override
    public void onBackPressed() {
        /*if (NotebookUtil.isNotebookEnable(this)){
            setResult(RESULT_OK);
        }*/
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.more));
        mToolbar.setNavigationIcon(R.drawable.ic_back);
        mToolbar.setNavigationOnClickListener(view -> SettingActivity.this.finish());
    }
}
