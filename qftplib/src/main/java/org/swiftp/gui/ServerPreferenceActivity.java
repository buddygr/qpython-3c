package org.swiftp.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.swiftp.R;

/**
 * This is the main activity for swiftp, it enables the user to start the server service
 * and allows the users to change the settings.
 */
public class ServerPreferenceActivity extends PreferenceActivity{

    private static String TAG = ServerPreferenceActivity.class.getSimpleName();



    public static void start(Context context) {
        Intent starter = new Intent(context, ServerPreferenceActivity.class);
        context.startActivity(starter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ftp_preferences);
        setContentView(R.layout.activity_preference);


    }



    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();

    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();

        Log.v(TAG, "Unregistering the FTPServer actions");


    }



}
