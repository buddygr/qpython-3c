package org.qpython.qpy.main.activity;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import com.quseit.util.FileHelper;
import com.quseit.util.FolderUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.main.adapter.AppListAdapter;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.event.AppsLoader;
import org.qpython.qpy.main.model.AppModel;
import org.qpython.qpy.main.model.QPyScriptModel;
import org.qpython.qpy.texteditor.EditorActivity;
import org.qpython.qpysdk.QPyConstants;
import org.qpython.qpysdk.utils.Utils;
import org.qpython.qsl4a.QPyScriptService;
import org.qpython.qsl4a.qsl4a.jsonrpc.JsonRpcServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import util.FileUtil;

/**
 * Local App list
 * Created by Hmei on 2017-05-22.
 * Edit by 乘着船 2021-2023
 */

public class AppListActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ArrayList<AppModel>> {
    public static final String TYPE_SCRIPT = "script";

    private List<AppModel> dataList;
    private AppListAdapter adapter;
    public static String APP_LIST_SETTING;
    protected static byte[] frequencyAppList = new byte[0];

    public static void start(Context context, String type) {
        Intent starter = new Intent(context, AppListActivity.class);
        starter.putExtra("type", type);
        context.startActivity(starter);
        APP_LIST_SETTING = context.getFilesDir() + "/text/ver/" + AppListAdapter.TAG;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_app);
        initView();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needLog && event != null) {
            showLogDialog(event);
            needLog = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        dataList = new ArrayList<>();
        adapter = new AppListAdapter(dataList, getIntent().getStringExtra("type"), this);
        adapter.setCallback(new AppListAdapter.Callback() {
            @Override
            public void runScript(QPyScriptModel item) {
                ScriptExec.getInstance().playScript(AppListActivity.this, item.getPath(), null);
            }

            @Override
            public void runProject(QPyScriptModel item) {
                ScriptExec.getInstance().playProject(AppListActivity.this, item.getPath());
            }

            @Override
            public void runNotebook(QPyScriptModel item) {
                openNotebook(AppListActivity.this,item.getFile());
            }

            @Override
            public void exit() {
                AppListActivity.this.finish();
            }
        });

        RecyclerView appsView = findViewById(R.id.rv_app);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, 3, LinearLayoutManager.VERTICAL, false);
        appsView.setLayoutManager(mLayoutManager);
        appsView.setAdapter(adapter);

        ((TextView) findViewById(R.id.tv_folder_name)).setText(R.string.qpy_app);
        ((TextView) findViewById(R.id.tv_ar_back)).setOnClickListener(view -> finish());
        getProjectScriptList();
    }

    private void getProjectList(File projectFile) {
        File[] projectFiles = projectFile.listFiles();
        if (projectFiles != null) {
            Arrays.sort(projectFiles, FolderUtils.sortByName);
            for (File file : projectFiles) {
                if (file.isDirectory()) {
                    if ((new File(file, "main.py")).exists())
                        dataList.add(new QPyScriptModel(file));
                    else {
                        getProjectList(file);
                    }
                }
            }
        }
    }

    private void getProjectScriptList() {
        for (String path : getPathsOrder()){
            File projectFile = new File(path,QPyConstants.DFROM_PRJ3);
            getProjectList(projectFile);
            getScriptList(path);
            getNotebookList(path);
        }
    }

    private void getScriptList(String path) {
        try {
            File[] files = FileHelper.getPyFiles(new File(path + "/" + QPyConstants.DFROM_QPY3));
            if (files!=null && files.length > 0) {
                Arrays.sort(files, FolderUtils.sortByName);
                for (File file : files) {
                    dataList.add(new QPyScriptModel(file));
                }
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getNotebookList(String path) {
        try {
            File[] files = FileHelper.getPyFiles(new File(path + "/notebooks"));
            if (files!=null && files.length > 0) {
                Arrays.sort(files, FolderUtils.sortByName);
                for (File file : files) {
                    dataList.add(new QPyScriptModel(file));
                }
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getPathsOrder(){
        String[] paths = CONF.PATHS();
        getPathsValueFromFile(paths.length);
        String s;    byte k;
        byte[] freq = frequencyAppList.clone();
        for(byte i = 0;i < paths.length - 1; i++)
            for(byte j = 1;j < paths.length; j++){
                if(freq[i] < freq[j]){
                    k = freq[i];
                    freq[i] = freq[j];
                    freq[j] = k;
                    s = paths[i];
                    paths[i] = paths[j];
                    paths[j] = s;
                }
            }
        return paths;
    }

    public static void getPathsValue(byte length){
        String[] paths = CONF.PATHS();
        if(length < paths.length)
            length = (byte) paths.length;
        getPathsValueFromFile(length);
    }

    private static void getPathsValueFromFile(int length){
        if(frequencyAppList.length == 0){
          frequencyAppList = new byte[length];
        try {
            int num = Integer.parseInt(FileHelper.getFileContent(APP_LIST_SETTING));
            for (byte i = 0;i < length; i++){
                frequencyAppList[i] = (byte) (num % 10);
                num /= 10;
            }
        } catch (Exception ignored) {}
    } else if(length > frequencyAppList.length){
        byte[] freq = frequencyAppList.clone();
        frequencyAppList = new byte[length];
        System.arraycopy(freq, 0, frequencyAppList, 0, freq.length);
        }
    }

    public static void setPathsValue(QPyScriptModel qPyScriptModel) {
        byte length = qPyScriptModel.getPathType();
        getPathsValue(length);
        frequencyAppList[length] += 1;
        byte k = 10;
        for (byte b : frequencyAppList) {
            if (b < k)
                k = b;
            else if (b > 9)
                k += 1;
        }
        int num = 0,t = 1;
        for(byte i = 0; i < frequencyAppList.length; i++){
            frequencyAppList[i] -= k;
            if(frequencyAppList[i] < 0)
                frequencyAppList[i] = 0;
            num += frequencyAppList[i] * t;
            t *= 10;
        }
        FileUtil.writeToFile(APP_LIST_SETTING,String.valueOf(num));
    }

    @Override
    public Loader<ArrayList<AppModel>> onCreateLoader(int id, Bundle args) {
        return new AppsLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<AppModel>> loader, ArrayList<AppModel> data) {
        dataList.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<AppModel>> loader) {
        dataList.clear();
        adapter.notifyDataSetChanged();
    }

    boolean              needLog;
    ScriptExec.LogDialog event;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ScriptExec.LogDialog event) {
        this.event = event;
        needLog = true;
    }

    public void showLogDialog(ScriptExec.LogDialog event) {
        AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.MyDialog)
                .setTitle(R.string.log_title)
                .setMessage(com.quseit.qpyengine.R.string.open_log)
                .setNegativeButton(R.string.no, ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                    Utils.checkRunTimeLog(this, event.title, event.path);

                    dialog.dismiss();
                })
                .create();
        alertDialog.show();
    }

    public static void openNotebook(Context context,String file){
        openNotebook(context,new File(file));
    }

    public static void openNotebook(Context context,File file){
        if(JsonRpcServer.isServiceRunning()) {
        File qcnb = new File(CONF.filesDir,"bin/quick_notebook.py");
        boolean notebookInstall = qcnb.exists();
        if(notebookInstall)
            ScriptExec.getInstance().playQScript(context,qcnb.getAbsolutePath(),file.getAbsolutePath());
        else
            EditorActivity.start(context, Uri.fromFile(file));
    } else {
            context.startService(new Intent(context, QPyScriptService.class));
            Toast.makeText(context, R.string.sl4a_start, Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    openNotebook(context,file);
                }
            },512);
        }}

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.fade_out);
    }
}