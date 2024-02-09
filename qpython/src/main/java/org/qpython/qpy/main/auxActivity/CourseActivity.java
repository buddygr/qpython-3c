package org.qpython.qpy.main.auxActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.qpython.qpy.R;
import org.qpython.qpy.main.fragment.CourseFragment;


public class CourseActivity extends AppCompatActivity {
    CourseFragment fragment;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CourseActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = new CourseFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.setting_fragment, fragment)
                .commit();
        setContentView(R.layout.activity_setting);
        init();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.course));
        mToolbar.setNavigationIcon(R.drawable.ic_back);
        mToolbar.setNavigationOnClickListener(view -> CourseActivity.this.finish());
    }
}
