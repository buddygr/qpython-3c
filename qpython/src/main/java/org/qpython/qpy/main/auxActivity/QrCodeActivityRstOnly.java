package org.qpython.qpy.main.auxActivity;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.zxing.Result;

import org.qpython.qpy.R;
import org.qpython.qpy.main.activity.QrCodeActivity;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrCodeActivityRstOnly extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    public static void start(Context context) {
        Intent starter = new Intent(context, QrCodeActivity.class);
        context.startActivity(starter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //动态申请相机权限
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        //}
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        setContentView(R.layout.activity_qrcode);
        if (title == null) {
            setTitle(R.string.read_script_from_qrcode);
        } else {
            setTitle(title);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        mScannerView = (ZXingScannerView) findViewById(R.id.scanner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    private void close() {
        this.finish();
    }
    
    @Override
    public void handleResult(Result result) {
        String scanResult = result.getText();
        Intent intentR=new Intent();
        intentR.putExtra("result",scanResult);
        this.setResult(RESULT_OK,intentR);
        finish();
    }
}
