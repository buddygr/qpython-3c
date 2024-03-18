package org.qpython.qpy.main.activity;

//Edit by 乘着船 2022 - 2023

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.Spanned;
import android.widget.Toast;

import com.quseit.util.FileHelper;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.console.TermActivity;
import org.qpython.qpy.databinding.ActivityMainBinding;
import org.qpython.qpy.main.app.CONF;
import org.qpython.qpy.main.auxActivity.CourseActivity;
import org.qpython.qpy.main.auxActivity.ProtectActivity;
import org.qpython.qpy.main.auxActivity.ScreenRecordActivity;
import org.qpython.qpy.main.fragment.QPyExtFragment;
import org.qpython.qpy.main.utils.Bus;
import org.qpython.qpy.texteditor.EditorActivity;
import org.qpython.qpy.texteditor.TedLocalActivity;
import org.qpython.qsl4a.qsl4a.facade.AndroidFacade;
import org.qpython.qsl4a.qsl4a.facade.QPyInterfaceFacade;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeMainActivity extends BaseActivity {

    private ActivityMainBinding binding;

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
        setHandler();
        setConsoleMenu();
        startMain();
        handleNotification(savedInstanceState);
        runShortcut();
    }

    private void startMain() {
        initListener();
        Bus.getDefault().register(this);
        QPyExtFragment.openQpySDK(this);
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.aux_window)));
        ProtectActivity.CheckProtect(this);
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

    public void startShell(String name){
        TermActivity.startShell(this,name);
    }

    private void playPy(String name){
        ScriptExec.play(HomeMainActivity.this,
                CONF.binDir+name+".py");
    }

    private void initListener() {

        binding.ivScan.setOnClickListener(v -> Bus.getDefault().post(new StartQrCodeActivityEvent()));

        binding.llTerminal.setOnClickListener(v -> {
            //JsonRpcServer.startService(this);
            TermActivity.startActivity(HomeMainActivity.this);
            sendEvent(getString(R.string.event_term));
        });

        binding.llTerminal.setOnLongClickListener(v -> {
            //JsonRpcServer.startService(this);

            List<Spanned> list = new ArrayList<>();
            for(byte i = 0;i < consoleItem.length;i++)
                list.add(consoleItem[consoleMenu.get(i)]);
            Spanned[] chars = list.toArray(new Spanned[consoleItem.length]);
            boolean k = new File(CONF.binDir,"NoteBook.py").exists();
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
                                if (k) startShell("ipython.py");
                                else liteVersionNotSupport();
                                break;
                            case 2:
                                playPy("SL4A_GUI_Console");
                                break;
                            case 3:
                                playPy("browserConsole");
                                break;
                            case 4:
                                startShell("shell.py");
                                break;
                            case 5:
                                startShell("shell");
                                break;
                            case 6:
                                if (k) playPy("NoteBook");
                                else liteVersionNotSupport();
                                break;
                        }
                        if(which>0){
                            i = consoleMenu.get(which - 1);
                            consoleMenu.set(which - 1,consoleMenu.get(which));
                            consoleMenu.set(which,i);
                            StringBuilder sb = new StringBuilder();
                            for(i = 0; i < consoleItem.length; i++)
                                sb.append(consoleMenu.get(i));
                            FileHelper.putFileContents(CONSOLE_SETTING,sb.toString());
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
        binding.llCourse.setOnClickListener(v -> {
            CourseActivity.startActivity(this);
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
            //JsonRpcServer.startService(this);
            AppListActivity.start(this, AppListActivity.TYPE_SCRIPT);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            sendEvent(getString(R.string.event_top));
        });
        binding.llRecord.setOnClickListener(v -> {
            startActivity(new Intent(this, ScreenRecordActivity.class));
        });

    }

    private void liteVersionNotSupport() {
        Toast.makeText(this,R.string.lite_version_not_support,Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
    }

    private void handleNotification(Bundle bundle) {
        if (bundle == null) return;
    if (!bundle.getBoolean("force") && !CONF.PREF.getBoolean(getString(R.string.key_hide_push), true)) {
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
        if (!CONF.PREF.getBoolean(getString(R.string.key_hide_push), true)) {
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

    public void runShortcut(Intent intent) {
        String action = intent.getAction();
        if (action!=null && action.equals(Intent.ACTION_VIEW)){
        String path = intent.getStringExtra("path");
        String arg = intent.getStringExtra("arg");
        boolean isProj = intent.getBooleanExtra("isProj", false);
        if (isProj)
            ScriptExec.playPro(this, path, arg);
        else {
            if(path.endsWith(".ipynb"))
                AppListActivity.openNotebook(this,path);
            else
                ScriptExec.play(this, path, arg);
    }}}

    private void runShortcut(){
        if(SplashActivity.delay > 100){
            if(CONF.pyVer.isEmpty())
                return;
            runShortcut(getIntent());
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
                ScriptExec.play(
                        HomeMainActivity.this,
                        string[0],string[1]
                );
            }
        };
        AndroidFacade.handler = QPyInterfaceFacade.handler;
    }

    private void setConsoleMenu(){
        CONSOLE_SETTING = getFilesDir() + "/text/ver/" + CONSOLE_SETTING;
            consoleItem = new Spanned[]{
                strIdToHtm(R.string.python_interpreter,"bfdfdf"),
                //strIdToHtm(R.string.color_python_interpreter,"dfdfbf"),
                strIdToHtm(R.string.ipython_interactive,"dfbfdf"),
                strIdToHtm(R.string.sl4a_gui_console,"dfdfbf"),//"ffffff"),
                strIdToHtm(R.string.browser_console,"ffbfbf"),
                strIdToHtm(R.string.python_shell_terminal,"bfbfff"),
                strIdToHtm(R.string.shell_terminal,"bfffbf"),
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
