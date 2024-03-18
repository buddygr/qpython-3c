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
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.auxActivity.ProtectActivity;
import org.qpython.qpy.main.auxActivity.ScreenRecordActivity;
import org.qpython.qpy.main.service.FTPServerService;
import org.qpython.qpy.texteditor.ui.view.EnterDialog;
import org.qpython.qpysdk.utils.Utils;
import org.qpython.qsl4a.QPyScriptService;
import org.qpython.qsl4a.qsl4a.jsonrpc.JsonRpcServer;
import org.qpython.qsl4a.qsl4a.util.PermissionUtil;
import org.swiftp.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SettingFragment extends PreferenceFragment {
    private static final String TAG = "SettingFragment";

    private SharedPreferences settings;
    private Resources         resources;
    private Preference        mPassWordPref, username_pref, portnum_pref, chroot_pref, lastlog, ipaddress, qpyCustom, autoBackup;
    private CheckBoxPreference sl4a, running_state, root, display_pwd, qpy_protect, screen_on;//, notebook_run;
    private PowerManager.WakeLock wakeLock;

    private void viewWebSite(int resId) {
        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(getString(resId))));
    }

    private SwitchPreference log, app;
    private final BroadcastReceiver ftpServerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "FTPServerService action received: " + intent.getAction());
            switch (intent.getAction()) {
                case FTPServerService.ACTION_STARTED:
                    running_state.setChecked(true);
                    // Fill in the FTP server address
                    setFtpAddress();
                    break;
                case FTPServerService.ACTION_STOPPED:
                    running_state.setChecked(false);
                    running_state.setSummary(org.swiftp.R.string.running_summary_stopped);
                    break;
                case FTPServerService.ACTION_FAILEDTOSTART:
                    running_state.setChecked(false);
                    running_state.setSummary(org.swiftp.R.string.running_summary_failed);
                    Toast.makeText(getActivity(), R.string.ip_address_need_wifi_or_ap, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    public SettingFragment() {
    }

    static private String transformPassword(String password) {
        StringBuilder sb = new StringBuilder(password.length());
        for (int i = 0; i < password.length(); ++i)
            sb.append('*');
        return sb.toString();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.qpython_setting);
        settings = CONF.PREF;
        resources = getResources();
        try {
            initSettings();
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

    @Override
    public void onResume() {
        super.onResume();
    }

    private boolean showIpAddress(){
        if(Globals.getContext()==null) {
            Globals.setContext(getActivity());
            FTPServerService.loadPort(getActivity());
        }
        ArrayList<String> ip;
        try {
            ip = FTPServerService.getWifiAndApIp();
        } catch (NullPointerException e) {
            ip = null;
        }
        if (ip!=null) {
            StringBuilder iptext = new StringBuilder();
            for(String addr:ip){
                if(iptext.length()>0)
                    iptext.append(", ");
                iptext.append(addr);
            }
            ipaddress.setSummary(ip.toString());
            return true;
        } else {
            ipaddress.setSummary(R.string.ip_address_need_wifi_or_ap);
            return false;
        }
    }

    private void initSettings() throws Exception{
        ipaddress = findPreference("ipaddress");
        showIpAddress();

        lastlog = findPreference("lastlog");
        //py_inter = (PreferenceScreen) findPreference(getString(R.string.key_py_inter));
        /*notebook_page = (PreferenceScreen) findPreference(getString(R.string.key_notebook_page));
        notebook_page.setTitle(MessageFormat.format(getString(R.string.notebook_for_py), NAction.getPyVer(getActivity())));
        if (NAction.isQPy3(getActivity())) {
            notebook_page.setSummary(NotebookUtil.isNotebookLibInstall(getActivity()) ? R.string.notebook_installed : R.string.notebook_not_started);

        } else {
            notebook_page.setSummary( R.string.notebook_py3_support);

        }*/

        //notebook_res = (Preference) findPreference(getString(R.string.key_notebook));
        //notebook_res.setSummary((NotebookUtil.isNotebookLibInstall(getActivity())||NotebookUtil.isNotebookInstall(getActivity()))?R.string.choose_notebook_inter:R.string.install_notebook_first);
        //notebook_run = (CheckBoxPreference) findPreference(getString(R.string.key_notebook_run));

        //update_qpy3 = (Preference)findPreference(getString(R.string.key_update_qpy3));
        //update_qpy2compatible = (Preference)findPreference(getString(R.string.key_update_qpy2compatible));
        //py2 = (Preference) findPreference(getString(R.string.key_py2));
        //py2compatible = (Preference) findPreference(getString(R.string.key_py2compatible));
        //py3 = (Preference) findPreference(getString(R.string.key_py3));


        root = (CheckBoxPreference) findPreference(resources.getString(R.string.key_root));
        qpyCustom = findPreference(resources.getString(R.string.qpy_custom_dir_key));
        //pyOptimize = findPreference(resources.getString(R.string.key_python_optimize));
        sl4a = (CheckBoxPreference) findPreference(resources.getString(R.string.key_sl4a));
        qpy_protect = (CheckBoxPreference) findPreference(getString(R.string.key_qpython_protect));
        screen_on = (CheckBoxPreference) findPreference(getString(R.string.key_screen_on));
        app = (SwitchPreference) findPreference(getString(R.string.key_hide_push));
        log = (SwitchPreference) findPreference(resources.getString(R.string.key_hide_noti));
        username_pref = findPreference(resources.getString(R.string.key_username));
        running_state = (CheckBoxPreference) findPreference(resources.getString(R.string.key_ftp_state));
        display_pwd = (CheckBoxPreference) findPreference(resources.getString(R.string.key_show_pwd));
        mPassWordPref = findPreference(resources.getString(R.string.key_ftp_pwd));
        portnum_pref = findPreference(resources.getString(R.string.key_port_num));
        chroot_pref = findPreference(resources.getString(R.string.key_root_dir));
        autoBackup = findPreference(resources.getString(R.string.key_auto_backup));

        boolean isRoot, isRunning;
        isRoot = settings.getBoolean(getString(R.string.key_root), false);
        root.setChecked(isRoot);
        root.setSummary(isRoot ? R.string.enable_root : R.string.disable_root);

        isRunning = JsonRpcServer.isServiceRunning();
        sl4a.setChecked(isRunning);
        sl4a.setSummary(isRunning ? R.string.sl4a_running : R.string.sl4a_un_running);

        qpy_protect.setChecked(settings.getBoolean(getString(R.string.key_qpython_protect),false));
        screen_on.setChecked(settings.getBoolean(getString(R.string.key_screen_on),false));

        app.setChecked(settings.getBoolean(getString(R.string.key_hide_push), true));
        log.setChecked(settings.getBoolean(getString(R.string.key_hide_noti), true));

        //qpypi.setSummary(settings.getString(getString(R.string.key_qpypi), org.qpython.qpy.main.app.CONF.QPYPI_URL));
        username_pref.setSummary(settings.getString(resources.getString(R.string.key_username), resources.getString(org.swiftp.R.string.username_default)));
        boolean isFTPRunning = FTPServerService.isRunning();
        running_state.setChecked(isFTPRunning);
        if (isFTPRunning) {
            setFtpAddress();
        } else {
            running_state.setSummary(R.string.running_summary_stopped);
        }
        portnum_pref.setSummary(settings.getString(resources.getString(R.string.key_port_num),
                resources.getString(org.swiftp.R.string.portnumber_default)));
        chroot_pref.setSummary(settings.getString(resources.getString(R.string.key_root_dir),
                Environment.getExternalStorageDirectory().getAbsolutePath()));
        qpyCustom.setSummary(CONF.CUSTOM_PATH);
        autoBackup.setSummary(settings.getString(resources.getString(R.string.key_auto_backup),"0"));
        autoBackup.setTitle(resources.getString(R.string.auto_backup)+" ("+resources.getString(R.string.day)+")");

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getString(R.string.key_root), root.isChecked());
        //editor.putString(getString(R.string.key_qpypi), qpypi.getSummary().toString());
        editor.putString(getString(R.string.key_username), username_pref.getSummary().toString());
        editor.putString(getString(R.string.key_ftp_pwd), settings.getString(mPassWordPref.getKey(), "ftp"));
        editor.putString(getString(R.string.key_port_num), portnum_pref.getSummary().toString());
        editor.putString(getString(R.string.key_root_dir), chroot_pref.getSummary().toString());
        editor.putString(getString(R.string.key_auto_backup),autoBackup.getSummary().toString());
        editor.putBoolean(getString(R.string.key_hide_noti), log.isChecked());
        editor.apply();
    }

    private void setFtpAddress() {
        String[] address = FTPServerService.getIpPortString();
        if (address == null) {
            Log.v(TAG, "Unable to retreive wifi ip address");
            running_state.setSummary(org.swiftp.R.string.cant_get_url);
            Toast.makeText(getActivity(),"FTP: "+getString(R.string.ip_address_need_wifi_or_ap),Toast.LENGTH_LONG).show();
        } else {
            StringBuilder iptext = new StringBuilder();
            for(String ip:address){
                if(iptext.length()>0)
                    iptext.append(", ");
                iptext.append(ip);
            }
            String summary = getString(R.string.running_summary_started,
                    Arrays.toString(address));
            running_state.setSummary(summary);
        }
    }

    /*private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }*/

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    private void initListener() throws Exception {

        lastlog.setOnPreferenceClickListener(preference -> {
            File logFolder = new File(CONF.SCOPE_STORAGE_PATH,"log");
            String[] logFiles = logFolder.list();
            if (logFiles == null || logFiles.length==0){return false;}
            android.app.AlertDialog.Builder alert;
            alert = new android.app.AlertDialog.Builder(getActivity(),R.style.MyDialog);
            alert.setTitle(R.string.choose_file);
            alert.setItems(logFiles, (dialogInterface, i) -> {
                Utils.checkRunTimeLog(getActivity(), getString(R.string.log_title),
                        logFolder +"/"+logFiles[i]);
                    });
            alert.setNegativeButton(getString(R.string.close),
                    (dialogInterface, i) -> dialogInterface.dismiss());
            alert.create().show();
            return false;
        });

        ipaddress.setOnPreferenceClickListener(preference -> showIpAddress());

        /*if (!NAction.isQPy3(getActivity())) {
            notebook_run.setSummary(getString(R.string.notebook_py3_support));
            notebook_run.setEnabled(false);

        } else {
            notebook_run.setChecked(NotebookUtil.isNBSrvSet(getActivity()));

            notebook_run.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((boolean)newValue) {
                    NotebookUtil.startNotebookService2(getActivity());

                } else {
                    NotebookUtil.killNBSrv(getActivity());
                }
                notebook_page.setSummary(NotebookUtil.isNotebookEnable(getActivity())?R.string.notebook_installed : R.string.notebook_not_started);

                return true;
            });
        }*/

        /*py2.setOnPreferenceClickListener(preference -> {
            Log.d(TAG, "py2.setOnPreferenceClickListener");
            NotebookUtil.killNBSrv(getActivity());

            releasePython2Standard();
            return false;
        });

        py3.setOnPreferenceClickListener(preference -> {
            NotebookUtil.killNBSrv(getActivity());
            releasePython3();

            return false;
        });*/

        root.setOnPreferenceChangeListener((preference, newValue) ->
        {
            if ((boolean) newValue) {
                if (NAction.isRootSystem()) {
                    NStorage.setSP(getActivity(), "app.root", "1");
                    return true;
                } else {
                    Toast.makeText(getActivity(), R.string.not_root_yet, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                NStorage.setSP(getActivity(), "app.root", "0");
                return true;
            }
        });

        sl4a.setOnPreferenceChangeListener((preference, newValue) ->

        {
            boolean isCheck = (boolean) newValue;
            Context context = getActivity();
            if (isCheck) {
                QPyScriptService.start(context);
            } else {
                QPyScriptService.stop(context);
            }
            return true;
        });

        app.setOnPreferenceChangeListener((preference, newValue) ->

        {
            boolean isCheck = (boolean) newValue;
            settings.edit().putBoolean(getString(R.string.key_hide_push), isCheck).apply();
            return true;
        });
        log.setOnPreferenceChangeListener((preference, newValue) ->

        {
            boolean isCheck = (boolean) newValue;
            settings.edit().putBoolean(getString(R.string.key_hide_noti), isCheck).apply();
            return true;
        });

        findPreference(resources.getString(R.string.key_reset)).
            setOnPreferenceClickListener(preference ->
            {
                NAction.startInstalledAppDetailsActivity(getActivity());
                return false;
            });

        findPreference("community").
            setOnPreferenceClickListener(preference ->
            {
                viewWebSite(R.string.community_website);
                return true;
            });

        findPreference("screen_record").
                setOnPreferenceClickListener(preference ->
                {
                    startActivity(new Intent(getActivity(), ScreenRecordActivity.class));
                    return true;
                });

        screen_on.setOnPreferenceChangeListener((preference,newValue) -> {
            boolean result = (boolean) newValue;
            PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            if (wakeLock != null){
                wakeLock.release();
                wakeLock = null;
            }
            if (result) {
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
                wakeLock.acquire();
            }
            settings.edit().putBoolean(getString(R.string.key_screen_on),result).apply();
            return true;
        });

        qpy_protect.setOnPreferenceChangeListener((preference,newValue) -> {
            Context context = getActivity();
            boolean result = (boolean) newValue;
            settings.edit().putBoolean(getString(R.string.key_qpython_protect),result).apply();
            if (result) {
                ProtectActivity.DoProtect(context);
            } else {
                Toast.makeText(context,getString(R.string.qpython_protect_close),Toast.LENGTH_SHORT).show();
                ProtectActivity.UndoProtect();
            }
            return true;
        });

        /*  ====================FTP====================   */
        running_state.setOnPreferenceChangeListener((preference, newValue) ->
        {
            if ((Boolean) newValue) {
                startServer();
            } else {
                stopServer();
            }
            return true;
        });

        username_pref.setOnPreferenceClickListener(preference ->

        {
            new EnterDialog(getActivity())
                    .setTitle(getString(R.string.username_label))
                    .setText(preference.getSummary().toString())
                    .setConfirmListener(name -> {
                        preference.setSummary(name);
                        updatePreference(preference);
                        stopServer();
                        return true;
                    })
                    .show();
            return true;
        });

        display_pwd.setOnPreferenceChangeListener(((preference, newValue) ->

        {
            boolean check = (boolean) newValue;
            String pwd = settings.getString(getString(R.string.key_ftp_pwd), "ftp");
            if (check) {
                mPassWordPref.setSummary(pwd);
            } else {
                mPassWordPref.setSummary(transformPassword(pwd));
            }
            return true;
        }));

        String password = settings.getString(resources.getString(R.string.key_ftp_pwd), "ftp");
        mPassWordPref.setSummary(display_pwd.isChecked() ? password :

                transformPassword(password));
        mPassWordPref.setOnPreferenceClickListener(preference ->

        {
            new EnterDialog(getActivity())
                    .setTitle(getString(R.string.password_label))
                    .setText(password)
                    .setEnterType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .setConfirmListener(name -> {
                        settings.edit().putString(mPassWordPref.getKey(), name).apply();
                        mPassWordPref.setSummary(display_pwd.isChecked() ? name : transformPassword(name));
                        stopServer();
                        return true;
                    })
                    .show();
            return true;
        });

        portnum_pref.setOnPreferenceClickListener(preference ->

        {
            new EnterDialog(getActivity())
                    .setTitle(getString(R.string.portnumber_label))
                    .setText(preference.getSummary().toString())
                    .setEnterType(InputType.TYPE_CLASS_NUMBER)
                    .setConfirmListener(name -> {
                        int portNum = Integer.parseInt(name);
                        if (portNum <= 0 || portNum > 65535) {
                            Toast.makeText(getActivity(), org.swiftp.R.string.port_validation_error, Toast.LENGTH_LONG).show();
                            return false;
                        }
                        preference.setSummary(name);
                        updatePreference(preference);
                        stopServer();
                        return true;
                    })
                    .show();
            return true;
        });

        autoBackup.setOnPreferenceClickListener(preference ->

        {
            new EnterDialog(getActivity())
                    .setTitle(getString(R.string.auto_backup))
                    .setText(preference.getSummary().toString())
                    .setMessage(getString(R.string.auto_backup_days)+
                            Environment.getExternalStorageDirectory()+
                            "/Download/QPythonBackup")
                    .setEnterType(InputType.TYPE_CLASS_TEXT)
                    .setConfirmListener(name -> {
                        preference.setSummary(name);
                        updatePreference(preference);
                        PermissionUtil.requestAllFilesPermission();
                        return true;
                    })
                    .show();
            return true;
        });

        chroot_pref.setOnPreferenceClickListener(preference ->

        {
            new EnterDialog(getActivity())
                    .setTitle(preference.getKey())
                    .setText(preference.getSummary().toString())
                    .setConfirmListener(name -> {
                        if (preference.getSummary().equals(name)) {
                            return true;
                        }
                        File chrootTest = new File(name);
                        try {
                            if (!(chrootTest.isDirectory() && chrootTest.canRead()))
                                throw new Exception("Invalid Path");
                            name = chrootTest.getCanonicalPath();
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), R.string.dir_not_valid, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        preference.setSummary(name);
                        updatePreference(preference);
                        stopServer();
                        return true;
                    })
                    .show();
            return true;

        });

        qpyCustom.setOnPreferenceClickListener(preference ->

        {
            String path = preference.getSummary().toString();
            String msg = "\n"+getString(R.string.project)+": "+path+"/projects3\n"+
                    getString(R.string.script)+": "+path+"/scripts3\n"+
                    "Notebook: "+path+"/notebooks\n"+
                    getString(R.string.library)+": "+path+"/lib/"+pyVer+"/site-packages/\n\n"+
                    getString(R.string.edit_to_new_path);
            new EnterDialog(getActivity())
                    .setTitle(getString(R.string.qpy_custom_dir))
                    .setText(path)
                    .setMessage(msg)
                    .setConfirmListener(name -> {
                        if (preference.getSummary().equals(name)) {
                            return true;
                        }
                        File customTest = new File(name);
                        try {
                            if (!(customTest.isDirectory() && customTest.canRead()))
                                throw new Exception("Invalid Path");
                            name = customTest.getCanonicalPath();
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), R.string.dir_not_valid, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        CONF.CUSTOM_PATH = name;
                        preference.setSummary(CONF.CUSTOM_PATH);
                        updatePreference(preference);
                        return true;
                    })
                    .show();
            return true;
        });

        /*pyOptimize.setOnPreferenceClickListener(preference ->
        {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.optimize_grade))
                    .setItems(PY_OPTIMIZE(), (dialogInterface, i) -> {
                        settings.edit().putInt(getString(R.string.key_python_optimize),i).apply();
                        pyOptimize.setSummary(PY_OPTIMIZE_LIST[i]);
                    })
                    .show();
            return true;
        });*/

        final CheckBoxPreference wakelock_pref = (CheckBoxPreference) findPreference(resources.getString(R.string.key_stay_awake));
        wakelock_pref.setOnPreferenceChangeListener((preference, newValue) ->
        {
            stopServer();
            return true;
        });
    }

    /*private void releaseNotebook(Preference preference) {
        Observable.create((Observable.OnSubscribe<Boolean>) subscriber -> {
            try {
                String nbfile = NStorage.getSP(App.getContext(), NotebookUtil.getNbResFk(getActivity()));
                if (!nbfile.equals("") && new File(nbfile).exists()) {    //
                    extractNotebookRes(nbfile);
                }
                if (!NotebookUtil.isNotebookLibInstall(getActivity())) {
                    NotebookUtil.extraData(getActivity());
                }

                subscriber.onNext(true);
                subscriber.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
            }

        })
        .subscribeOn(Schedulers.io())
        .doOnSubscribe(() -> mLoadingDialog.show())
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnTerminate(() -> mLoadingDialog.dismiss())
        .subscribe(new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean aBoolean) {
                Log.d(TAG, "onNext");

                NotebookUtil.startNotebookService2(getActivity());
                notebook_page.setSummary(NotebookUtil.isNotebookLibInstall(getActivity())?R.string.notebook_installed : R.string.notebook_not_started);


            }
        });
    }

    private void installNotebook() {
        new Thread(() -> {
            QPySDK qpySDK = new QPySDK(App.getContext(), getActivity());
            qpySDK.extractRes("notebook" + (NAction.isQPy3(getActivity()) ? "3" : "2"), new File(NotebookUtil.RELEASE_PATH));
        }).run();
//        new AlertDialog.Builder(getActivity(), R.style.MyDialog)
//            .setTitle(R.string.notice)
//            .setMessage(R.string.install_notebook_hint)
//            .setPositiveButton(R.string.download_it, (dialog1, which)->getNotebook())
//            .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
//            .create()
//            .show();
    }


    private void extractNotebookRes(String path) {
        final String extarget = NotebookUtil.RELEASE_PATH;

        if (path!=null && !path.equals("")) {
            File resf = new File(path);
            if (resf.exists()) {
                QPySDK qpySDK = new QPySDK(App.getContext(), getActivity());
                qpySDK.extractRes(resf, new File(extarget), false);
            }
        }
    }*/


    /*private void releaseQPycRes(String path) {
        final String extarget = CONF.PY_CACHE_PATH;

        if (path!=null && !path.equals("")) {
            File res = new File(path);

            if (res.exists()) {
                QPySDK qpySDK = new QPySDK(App.getContext(), getActivity());
                qpySDK.extractRes(res, new File(extarget), false);
            }
        }
    }*/

    private void updatePreference(Preference preference) {
        SharedPreferences.Editor editor = settings.edit();
        if (preference instanceof CheckBoxPreference) {
            editor.putBoolean(preference.getKey(), ((CheckBoxPreference) preference).isChecked());
        } else {
            editor.putString(preference.getKey(), preference.getSummary().toString());
        }
        editor.apply();
    }

    private void restartApp() {
        Intent mStartActivity = new Intent(getActivity(), HomeMainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getActivity(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager mgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public void startServer() {
        Context context = getActivity();
        Intent serverService = new Intent(context, FTPServerService.class);
        if (!FTPServerService.isRunning()) {
            warnIfNoExternalStorage();
            context.startService(serverService);
        }
    }

    public void stopServer() {
        Context context = getActivity();
        Intent serverService = new Intent(context, FTPServerService.class);
        context.stopService(serverService);
    }

    /**
     * Will check if the device contains external storage (sdcard) and display a warning
     * for the user if there is no external storage. Nothing more.
     */
    private void warnIfNoExternalStorage() {
        String storageState = Environment.getExternalStorageState();
        if (!storageState.equals(Environment.MEDIA_MOUNTED)) {
            Log.v(TAG, "Warning due to storage state " + storageState);
            Toast toast = Toast.makeText(getActivity(), org.swiftp.R.string.storage_warning,
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    public BroadcastReceiver getFtpServerReceiver() {
        return ftpServerReceiver;
    }

}