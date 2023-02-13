package org.qpython.qpy.main.activity;

//Edit by 乘着船 2022

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.text.Spanned;
import android.widget.Toast;

import com.quseit.util.FileHelper;
import com.quseit.util.NAction;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.console.TermActivity;
import org.qpython.qpy.databinding.ActivityMainBinding;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.auxActivity.ProtectActivity;
import org.qpython.qpy.main.auxActivity.ScreenRecordActivity;
import org.qpython.qpy.main.utils.Bus;
import org.qpython.qpy.texteditor.EditorActivity;
import org.qpython.qpy.texteditor.TedLocalActivity;
import org.qpython.qpysdk.QPySDK;
import org.qpython.qsl4a.QPyScriptService;
import org.qpython.qsl4a.qsl4a.facade.AndroidFacade;
import org.qpython.qsl4a.qsl4a.facade.QPyInterfaceFacade;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeMainActivity extends BaseActivity {

    private QPySDK qpysdk;
    private ActivityMainBinding binding;
    private static SharedPreferences PREF;

    private static String CONSOLE_SETTING = "HomeMainActivity_ConsoleMenu";
    private static Spanned[] consoleItem;
    private static final List<Byte> consoleMenu = new ArrayList<>();

    public static void start(Context context) {
        Intent starter = new Intent(context, HomeMainActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        PREF = PreferenceManager.getDefaultSharedPreferences(this);
        setHandler();
        setConsoleMenu();
        startMain();
        handleNotification(savedInstanceState);
        runShortcut();
    }

    private void startMain() {
        initListener();
        startPyService();
        Bus.getDefault().register(this);
        openQpySDK();
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.aux_window)));
        ProtectActivity.CheckProtect(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPyVer(boolean once) {
        if (CONF.pyVer.startsWith("py")) return;
        if (qpysdk==null)
            qpysdk = new QPySDK(HomeMainActivity.this, HomeMainActivity.this);
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
            if (once) startShell("init.sh");
            else runShortcut(getIntent());
        }
        catch (Exception e){
            if (once) initQPy();
        }
        qpysdk = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        runShortcut(intent);
    }

    private void startShell(String name){
        TermActivity.startShell(this,name);
    }

    private void playPy(String name){
        ScriptExec.getInstance().playQScript(HomeMainActivity.this,
                CONF.binDir+name+".py", null);
    }

    private void initListener() {

        binding.ivScan.setOnClickListener(v -> Bus.getDefault().post(new StartQrCodeActivityEvent()));

        binding.llTerminal.setOnClickListener(v -> {
            startPyService();
            TermActivity.startActivity(HomeMainActivity.this);
            sendEvent(getString(R.string.event_term));
        });

        binding.llTerminal.setOnLongClickListener(v -> {
            startPyService();

            List<Spanned> list = new ArrayList<>();
            for(byte i = 0;i < consoleItem.length;i++)
                list.add(consoleItem[consoleMenu.get(i)]);
            Spanned[] chars = list.toArray(new Spanned[consoleItem.length]);
            new AlertDialog.Builder(this, R.style.MyDialog)
                    .setTitle(R.string.choose_action)
                    .setItems(chars, (dialog, which) -> {
                        byte i;
                        i = consoleMenu.get(which);
                        switch (i) {
                            case 0:
                                startShell("default");
                                break;
                            case 1:
                                startShell("colorConsole.py");
                                break;
                            case 2:
                                startShell("ipython.py");
                                break;
                            case 3:
                                playPy("SL4A_GUI_Console");
                                break;
                            case 4:
                                playPy("browserConsole");
                                break;
                            case 5:
                                startShell("shell");
                                break;
                            case 6:
                                startShell("shell.py");
                                break;
                            case 7:
                                playPy("open_notebook");
                                break;
                        }
                        if(which>0){
                            i = consoleMenu.get(which - 1);
                            consoleMenu.set(which - 1,consoleMenu.get(which));
                            consoleMenu.set(which,i);
                            StringBuilder sb = new StringBuilder();
                            for(i = 0; i < consoleItem.length; i++)
                                sb.append(consoleMenu.get(i));
                            FileHelper.putFileContents(this,CONSOLE_SETTING,sb.toString());
                        }
                    }).setNegativeButton(getString(R.string.close), (dialogInterface, i) -> dialogInterface.dismiss())
                    .show();

            return true;
        });
        binding.llEditor.setOnClickListener(v -> {
            EditorActivity.start(this);
            sendEvent(getString(R.string.event_editor));
        });
        binding.llLibrary.setOnClickListener(v -> {
            LibActivity.start(this);
            sendEvent(getString(R.string.event_qpypi));
        });
        binding.about.setOnClickListener(v -> {
            AboutActivity.start(this);
        });
        binding.llSetting.setOnClickListener(v -> {
            SettingActivity.startActivity(this);
            sendEvent(getString(R.string.event_setting));
        });
        binding.llFile.setOnClickListener(v -> {
            TedLocalActivity.start(this, TedLocalActivity.REQUEST_HOME_PAGE);
            sendEvent(getString(R.string.event_file));
        });
        binding.llQpyApp.setOnClickListener(v -> {
            startPyService();
            AppListActivity.start(this, AppListActivity.TYPE_SCRIPT);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            sendEvent(getString(R.string.event_top));
        });
        binding.llRecord.setOnClickListener(v -> {
            startActivity(new Intent(this, ScreenRecordActivity.class));
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
    }

    private void handleNotification(Bundle bundle) {
        if (bundle == null) return;
        if (!bundle.getBoolean("force") && !PREF.getBoolean(getString(R.string.key_hide_push), true)) {
            return;
        }
        String type = bundle.getString("type", "");
        if (!type.equals("")) {
            String link = bundle.getString("link", "");
            String title = bundle.getString("title", "");

            switch (type) {
                case "in":
                    QWebViewActivity.start(this, title, link);
                    break;
                case "ext":
                    Intent starter = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(starter);
                    break;
            }
        }
    }

    private void handleNotification() {
        if (!PREF.getBoolean(getString(R.string.key_hide_push), true)) {
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences(CONF.NOTIFICATION_SP_NAME, MODE_PRIVATE);
        try {
            String notifString = sharedPreferences.getString(CONF.NOTIFICATION_SP_OBJ, "");
            if ("".equals(notifString)) {
                return;
            }
            JSONObject extra = new JSONObject(notifString);
            String type = extra.getString("type");
            String link = extra.getString("link");
            String title = extra.getString("title");
            switch (type) {
                case "in":
                    QWebViewActivity.start(this, title, link);
                    break;
                case "ext":
                    Intent starter = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(starter);
                    break;
            }
            sharedPreferences.edit().clear().apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // open web

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void startPyService() {
        // confirm the SL4A Service is started
        Intent intent = new Intent(this, QPyScriptService.class);
        startService(intent);
    }

    private void openQpySDK() {
        //Log.d("HomeMainActivity", "openQpySDK");

        String[] permssions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (PREF.getString("security_tip","").equals(getString(R.string.security_version)))
        {
            QpySdkAgree(permssions);
        } else {
            if (qpysdk==null)
                qpysdk = new QPySDK(HomeMainActivity.this, HomeMainActivity.this);
            File filesDir = HomeMainActivity.this.getFilesDir();
            qpysdk.extractRes("resource", filesDir,true);
            CONF.pyVer = "-1";
            String content = FileHelper.getFileContents(filesDir+"/text/"+getString(R.string.lang_flag)+"/security_tip");
            new AlertDialog.Builder(HomeMainActivity.this, R.style.MyDialog)
                    .setTitle(R.string.notice)
                    .setMessage(content)
                    .setPositiveButton(R.string.agree, (dialog1, which) -> {
                        PREF.edit().putString("security_tip",getString(R.string.security_version)).apply();
                        QpySdkAgree(permssions);
                    })
                    .setNegativeButton(R.string.disagree, (dialog1, which) -> finish())
                    .setOnCancelListener(dialog1 -> finish())
                    .create()
                    .show();
        }
    }

    private void QpySdkAgree(String[] permssions){
        checkPermissionDo(permssions, new BaseActivity.PermissionAction() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onGrant() {
                //这里只执行一次做为初始化
                CONF.CUSTOM_PATH = PREF.getString(getString(R.string.qpy_custom_dir_key),CONF.LEGACY_PATH);

                if ( NAction.isQPyInterpreterSet(HomeMainActivity.this) ) {
                    getPyVer(true);
                } else {
                    NAction.setQPyInterpreter(HomeMainActivity.this, "3.x");
                    initQPy();
                }
            }

            @Override
            public void onDeny() {
                Toast.makeText(HomeMainActivity.this,  getString(R.string.grant_storage_hint), Toast.LENGTH_SHORT).show();
            }

        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initQPy(){
        /*if (qpysdk==null)
            qpysdk = new QPySDK(HomeMainActivity.this, HomeMainActivity.this);*/
        //new Thread(() -> {
            File filesDir = HomeMainActivity.this.getFilesDir();
        if (!CONF.pyVer.equals("-1"))
            qpysdk.extractRes("resource", filesDir,true);
        /*try {
            FileUtils.chmod(new File(this.getFilesDir()+"/bin/qpython.sh"),0777);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        //}).start();
        //Toast.makeText(this,getString(R.string.extract_resource),Toast.LENGTH_LONG).show();
        //NAction.startInstalledAppDetailsActivity(this);
        new AlertDialog.Builder(HomeMainActivity.this, R.style.MyDialog)
                .setTitle(R.string.notice)
                .setMessage(
                        getString(R.string.welcome)+"\n\n"+
                                getString(R.string.shortcut_permission))
                .setPositiveButton(R.string.setting, (dialog1, which) -> {
                    NAction.startInstalledAppDetailsActivity(this);
                    getPyVer(false);
                })
                .setNegativeButton(R.string.ignore, (dialog1, which) -> getPyVer(false))
                .setOnCancelListener(cancel -> getPyVer(false))
                .create()
                .show();
        ScriptExec.getInstance().playScript(this,
                "setup", null,false);
        try {
            checkOtherPermission();
        } catch (Exception e) {
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe
    public void startQrCodeActivity(StartQrCodeActivityEvent event) {
        String[] permissions = {Manifest.permission.CAMERA};

        checkPermissionDo(permissions, new BaseActivity.PermissionAction() {
            @Override
            public void onGrant() {
                QrCodeActivity.start(HomeMainActivity.this);
            }

            @Override
            public void onDeny() {
                Toast.makeText(HomeMainActivity.this, getString(R.string.no_camera), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runShortcut(Intent intent) {
        String action = intent.getAction();
        if (action!=null && action.equals(Intent.ACTION_VIEW)){
        String path = intent.getStringExtra("path");
        String arg = intent.getStringExtra("arg");
        boolean isProj = intent.getBooleanExtra("isProj", false);
        if (isProj) {
            ScriptExec.getInstance().playProject(this, path, arg,false);
        } else {
            ScriptExec.getInstance().playScript(this, path, arg, false);
        }
    }}

    private void runShortcut(){
        if(SplashActivity.delay > 100){
            if(CONF.pyVer.isEmpty())
                return;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runShortcut(getIntent());
                }
            },500);
            SplashActivity.delay = 100;
        } else runShortcut(getIntent());
    }

    public static class StartQrCodeActivityEvent {

    }

    private void sendEvent(String evenName) {

    }

    @SuppressLint("HandlerLeak")
    private void setHandler(){
        QPyInterfaceFacade.handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                String[] string = (String[]) msg.obj;
                ScriptExec.getInstance().playScript(
                        HomeMainActivity.this,
                        string[0],string[1],
                        false);
            }
        };
        AndroidFacade.handler = QPyInterfaceFacade.handler;
    }

    private void setConsoleMenu(){
        CONSOLE_SETTING = getFilesDir() + "/text/ver/" + CONSOLE_SETTING;
        consoleItem = new Spanned[]{
                strIdToHtm(R.string.python_interpreter,"bfdfdf"),
                strIdToHtm(R.string.color_python_interpreter,"dfdfbf"),
                strIdToHtm(R.string.ipython_interactive,"dfbfdf"),
                strIdToHtm(R.string.sl4a_gui_console,"ffffff"),
                strIdToHtm(R.string.browser_console,"ffbfbf"),
                strIdToHtm(R.string.shell_terminal,"bfffbf"),
                strIdToHtm(R.string.python_shell_terminal,"bfbfff"),
                strIdToHtm(R.string.jupyter_notebook,"ffbfff"),
        };
        byte i, k;
        int l = consoleItem.length;
        try{
            String s = FileHelper.getFileContents(CONSOLE_SETTING);
            for(i = 0; i < l; i++){
                k = Byte.parseByte(s.substring( i, i + 1 ));
                consoleMenu.add(k);
            }
            for(i = 0; i < l; i++){
                if(!consoleMenu.contains(i))
                    throw new Exception("invalid console menu .");
            }
        }
        catch (Exception e){
            consoleMenu.clear();
            for (i = 0; i < l; i++)
                consoleMenu.add(i);
        }}

    private Spanned strIdToHtm(int strId,String color){
        return HtmlUtil.textToHtml("<font color=#"+color+"><big>"+this.getString(strId)+"</big></font>");
    }
}
