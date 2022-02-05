package org.qpython.qpy.main.activity;

//Edit by 乘着船 2022

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
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

import java.io.File;

public class HomeMainActivity extends BaseActivity {

    private QPySDK qpysdk;

    private ActivityMainBinding binding;

    public static void start(Context context) {
        Intent starter = new Intent(context, HomeMainActivity.class);
        context.startActivity(starter);
    }

    /*public static void start(Context context, String userName) {
        Intent starter = new Intent(context, HomeMainActivity.class);
        starter.putExtra(USER_NAME, userName);
        context.startActivity(starter);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //App.setActivity(this);
        startMain();
        //handlePython3(getIntent());
        handleNotification(savedInstanceState);
        runShortcut(getIntent());
    }

    /*private void initIcon() {
        binding.icon.setImageResource(R.drawable.img_home_logo_3);
        switch (NAction.getQPyInterpreter(this)) {
            case "3.x":
                binding.icon.setImageResource(R.drawable.img_home_logo_3);
                break;
            case "2.x":
                binding.icon.setImageResource(R.drawable.img_home_logo);
                break;
        }
    }

    private void initUser() {
        if (App.getUser() == null) {
            binding.login.setVisibility(View.GONE);
        } else {
            binding.login.setText(Html.fromHtml(getString(R.string.welcome_s, App.getUser().getNick())));
        }
    }*/

    private void startMain() {
        initListener();
        startPyService();
        Bus.getDefault().register(this);
        openQpySDK();
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.aux_window)));
        ProtectActivity.CheckProtect(this);
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            UpdateHelper.checkConfUpdate(this, QPyConstants.BASE_PATH);
        }*/
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
        //initUser();
        //initIcon();
        handleNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //handlePython3(intent);
        runShortcut(intent);
    }

    private void startShell(String name){
        TermActivity.startShell(this,name);
    }

    private void playPy(String name){
        ScriptExec.getInstance().playScript(HomeMainActivity.this,
                CONF.binDir+name+".py", null, false);
    }

    private void initListener() {

        binding.ivScan.setOnClickListener(v -> Bus.getDefault().post(new StartQrCodeActivityEvent()));
        /*binding.login.setOnClickListener(v -> {
            if (App.getUser() == null) {
                sendEvent(getString(R.string.event_login));
                //startActivityForResult(new Intent(this, SignInActivity.class), LOGIN_REQUEST_CODE);
            } else {
                sendEvent(getString(R.string.event_me));
                //UserActivity.start(this);
            }
        });*/

        binding.llTerminal.setOnClickListener(v -> {
            //TermActivity.startActivity(HomeMainActivity.this);
            //默认启动彩色Python解释器
            TermActivity.startActivity(HomeMainActivity.this);
            sendEvent(getString(R.string.event_term));
        });

        binding.llTerminal.setOnLongClickListener(v -> {
            startPyService();

            CharSequence[] chars = new CharSequence[]{
                    this.getString(R.string.color_python_interpreter),
                    //this.getString(R.string.action_notebook),
                    this.getString(R.string.ipython_interactive),
                    this.getString(R.string.sl4a_gui_console),
                    this.getString(R.string.browser_console),
                    this.getString(R.string.shell_terminal),
                    this.getString(R.string.python_interpreter),
                    this.getString(R.string.python_shell_terminal),
            };
            new AlertDialog.Builder(this, R.style.MyDialog)
                    .setTitle(R.string.choose_action)
                    .setItems(chars, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                startShell("1");
                                break;
                            case 1:
                                startShell("ipython.py");
                                break;
                            case 2:
                                playPy("SL4A_GUI_Console");
                                break;
                            case 3:
                                playPy("browserConsole");
                                break;
                            case 4:
                                startShell("shell.sh");
                                break;
                            case 5:
                                startShell("blackConsole.py");
                                break;
                            case 6:
                                startShell("shell.py");
                                break;
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
//        binding.llCommunity.setOnClickListener(v -> {
//            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_COMMUNITY)));
//            sendEvent(getString(R.string.event_commu));
//        });
//        binding.llGist.setOnClickListener(view -> GistActivity.startCommunity(HomeMainActivity.this)
//        );
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

    /*private void handlePython3(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(getString(R.string.action_from_python_three))
                && NAction.getQPyInterpreter(this).equals(PYTHON_2)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.py2_now)
                    .setMessage(R.string.switch_py3_hint)
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.goto_setting, (dialog, which) -> SettingActivity.startActivity(this))
                    .create()
                    .show();
        }
    }*/

    private void handleNotification(Bundle bundle) {
        if (bundle == null) return;
        if (!bundle.getBoolean("force") && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_hide_push), true)) {
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
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_hide_push), true)) {
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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("security_agree",false))
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
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("security_agree",true).apply();
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

                if ( NAction.isQPyInterpreterSet(HomeMainActivity.this) ) {
                    getPyVer(true);
                } else {
                    /*new AlertDialog.Builder(HomeMainActivity.this, R.style.MyDialog)
                            .setTitle(R.string.notice)
                            .setMessage(R.string.py2_or_3)
                            .setPositiveButton(R.string.use_py3, (dialog1, which) -> initQpySDK3())
                            .setNegativeButton(R.string.use_py2, (dialog1, which) -> initQpySDK())
                            .create()
                            .show();*/
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

    /**
     * 在工作线程中作初始化
     *
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initQpySDK3() {
        Log.d(TAG, "initQpySDK3");
        NAction.setQPyInterpreter(HomeMainActivity.this, "3.x");
        initQPy();
        initIcon();
    }
    private void initQpySDK() {
        Log.d(TAG, "initQpySDK");
        initQPy(false);
        NAction.setQPyInterpreter(HomeMainActivity.this, "2.x");
        initIcon();
    }*/

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

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case LOGIN_REQUEST_CODE:
                    //binding.login.setText(Html.fromHtml(getString(R.string.welcome_s, App.getUser().getNick())));
                    break;
            }
        }
    }*/

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

    public static class StartQrCodeActivityEvent {

    }

    private void sendEvent(String evenName) {

    }
}
