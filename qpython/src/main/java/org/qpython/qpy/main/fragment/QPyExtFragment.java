package org.qpython.qpy.main.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.quseit.util.FileHelper;
import com.quseit.util.NAction;

import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.main.activity.BaseActivity;
import org.qpython.qpy.main.activity.HomeMainActivity;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpysdk.QPySDK;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QPyExtFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QPyExtFragment extends Fragment {

    @SuppressLint("StaticFieldLeak")
    public static HomeMainActivity activity;
    @SuppressLint("StaticFieldLeak")
    private static QPySDK qpysdk;
    public QPyExtFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static QPyExtFragment newInstance() {
        return new QPyExtFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_qpy_ext, container, false);
    }

    private static void viewWebSite(int resId) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(resId))));
    }

    public static void viewPage(int pos){
        switch (pos) {
            case 1:
                viewWebSite(R.string.qpython_sl4a_gui_gitee);
                break;
            case 2:
                viewWebSite(R.string.qpython_3c_release_gitee);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void checkOtherPermission() throws Exception {
        List<String> unPermissionList = new ArrayList<String>();
        PackageManager pm = activity.getPackageManager();
        PackageInfo info;
        String[] packagePermissions;

        info = pm.getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
        packagePermissions = info.requestedPermissions;
        if (packagePermissions != null) {
            for (String packagePermission : packagePermissions) {
                if (ContextCompat.checkSelfPermission(activity, packagePermission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    unPermissionList.add(packagePermission);//添加还未授予的权限到unPermissionList中
                }
            }

            //有权限没有通过，需要申请
            if (unPermissionList.size() > 0) {
                ActivityCompat.requestPermissions(
                        activity,unPermissionList.toArray(new String[0]),100);
            }
        }
        if (!Settings.canDrawOverlays(activity))
            activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName())),100);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void initQPy(){
        File filesDir = activity.getFilesDir();
        if (!CONF.pyVer.equals("-1"))
            qpysdk.extractRes("resource", filesDir,true);
        new AlertDialog.Builder(activity, R.style.MyDialog)
                .setTitle(R.string.notice)
                .setMessage(
                        activity.getString(R.string.welcome)+"\n\n"+
                                activity.getString(R.string.shortcut_permission))
                .setPositiveButton(R.string.setting, (dialog1, which) -> {
                    NAction.startInstalledAppDetailsActivity(activity);
                    getPyVer(false);
                })
                .setNegativeButton(R.string.ignore, (dialog1, which) -> getPyVer(false))
                .setOnCancelListener(cancel -> getPyVer(false))
                .create()
                .show();
        ScriptExec.getInstance().playScript(activity,
                "setup", null,false);
        try {
            checkOtherPermission();
        } catch (Exception e) {
            Toast.makeText(activity,e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void getPyVer(boolean once) {
        if (CONF.pyVer.startsWith("py")) return;
        if (qpysdk==null)
            qpysdk = new QPySDK(activity, activity);
        if(once && qpysdk.needUpdateRes())
        {
            initQPy();
            qpysdk = null;
            return;
        }
        String[] pyVer;
        try {
            pyVer = qpysdk.getPyVer();
            CONF.pyVerComplete = pyVer[1];
            CONF.pyVer = pyVer[0];
            //可以消除终端中文输入的某些bug，虽然不知道为什么
            if (once) activity.startShell("init.sh");
            else activity.runShortcut(activity.getIntent());
        }
        catch (Exception e){
            if (once) initQPy();
        }
        qpysdk = null;
    }

    public static void openQpySDK(HomeMainActivity context) {
        activity = context;
        CONF.PREF = PreferenceManager.getDefaultSharedPreferences(activity);
        if (qpysdk==null)
            qpysdk = new QPySDK(activity, activity);
        String[] permssions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (CONF.PREF.getString("security_tip","").equals(activity.getString(R.string.security_version)))
        {
            QpySdkAgree(permssions);
        } else {
            File filesDir = activity.getFilesDir();
            qpysdk.extractRes("resource", filesDir,true);
            CONF.pyVer = "-1";
            String content = FileHelper.getFileContents(filesDir+"/text/"+activity.getString(R.string.lang_flag)+"/security_tip");
            new AlertDialog.Builder(activity, R.style.MyDialog)
                    .setTitle(R.string.notice)
                    .setMessage(content)
                    .setPositiveButton(R.string.agree, (dialog1, which) -> {
                        CONF.PREF.edit().putString("security_tip",activity.getString(R.string.security_version)).apply();
                        QpySdkAgree(permssions);
                    })
                    .setNegativeButton(R.string.disagree, (dialog1, which) -> activity.finish())
                    .setOnCancelListener(dialog1 -> activity.finish())
                    .create()
                    .show();
        }
    }

    private static void QpySdkAgree(String[] permssions){
        activity.checkPermissionDo(permssions, new BaseActivity.PermissionAction() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onGrant() {
                //这里只执行一次做为初始化
                CONF.CUSTOM_PATH = CONF.PREF.getString(activity.getString(R.string.qpy_custom_dir_key),CONF.LEGACY_PATH);

                if ( NAction.isQPyInterpreterSet(activity) ) {
                    getPyVer(true);
                } else {
                    NAction.setQPyInterpreter(activity, "3.x");
                    initQPy();
                }
            }

            @Override
            public void onDeny() {
                Toast.makeText(activity, activity.getString(R.string.grant_storage_hint), Toast.LENGTH_SHORT).show();
            }

        });
    }
}