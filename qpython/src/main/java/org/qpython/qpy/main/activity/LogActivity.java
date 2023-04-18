package org.qpython.qpy.main.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.Menu;
import android.view.MenuItem;

import com.quseit.util.FileHelper;

import org.qpython.qpy.R;
import org.qpython.qpy.databinding.ActivityLogBinding;

import java.io.File;

/**
 * LogUtil Activity
 * Created by Hmei on 2017-06-22.
 */

public class LogActivity extends BaseActivity {

    public static final String LOG_TITLE = "title";
    public static final String LOG_PATH = "path";

    ActivityLogBinding binding;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_log);
        setSupportActionBar(binding.lt.toolbar);
        setTitle(R.string.log_title);
        binding.lt.toolbar.setNavigationIcon(R.drawable.ic_back);
        binding.lt.toolbar.setNavigationOnClickListener(view -> finish());

        Intent intent = getIntent();
        String logPath = intent.getStringExtra(LOG_PATH);
        File logFile = new File(logPath);
        @SuppressLint("SimpleDateFormat") String modifyTime = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss").format(logFile.lastModified());
        binding.title.setText(getString(R.string.last_modify_time));
        binding.path.setText(logPath);
        binding.time.setText(modifyTime);
        String content = FileHelper.getFileContents(intent.getStringExtra(LOG_PATH));
        binding.content.setText(content);

//        binding.ibMail.setOnClickListener(v -> {
//
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_more:
                binding.path.setText(getIntent().getStringExtra(LOG_PATH));
                String content = FileHelper.getFileContents(getIntent().getStringExtra(LOG_PATH));
                onFeedback(content);
//                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.ui_feedback_mail)));
//                String body = MessageFormat.format(getString(com.quseit.android.R.string.feedback_email_body), Build.PRODUCT,
//                        Build.VERSION.RELEASE, Build.VERSION.SDK_INT, binding.content.getContext());
//                String subject = getString(R.string.feedback_email_subject);
//                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
//                intent.putExtra(Intent.EXTRA_TEXT, body);
//                try {
//                    startActivity(Intent.createChooser(intent,
//                            getString(R.string.email_transcript_chooser_title)));
//                } catch (ActivityNotFoundException e) {
//                    Toast.makeText(this,
//                            R.string.email_transcript_no_email_activity_found,
//                            Toast.LENGTH_LONG)
//                            .show();
//                }

        }
        return true;
    }
}