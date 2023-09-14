package org.qpython.qpy.main.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.qpython.qpy.R;
import org.qpython.qsl4a.QPyScriptService;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    public static int delay = 1000;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this,HomeMainActivity.class);
                intent.setAction(getIntent().getAction());
                startActivity(intent);
                finish();
            }
        },delay);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.main_window)));
        startService( new Intent(this, QPyScriptService.class) );
        delay = 100;
    }

}