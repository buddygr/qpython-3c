package org.qpython.qpy.main.adapter;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import com.quseit.util.FileHelper;

import org.qpython.qpy.R;
import org.qpython.qpy.console.ScriptExec;
import org.qpython.qpy.databinding.ItemAppListBinding;
import org.qpython.qpy.main.activity.AppListActivity;
import org.qpython.qpy.main.activity.HomeMainActivity;
import org.qpython.qpy.main.model.AppModel;
import org.qpython.qpy.main.model.LocalAppModel;
import org.qpython.qpy.main.model.QPyScriptModel;
import org.qpython.qpy.texteditor.EditorActivity;
import org.qpython.qpy.texteditor.TedLocalActivity;
import org.qpython.qpy.texteditor.ui.view.EnterDialog;

import java.io.File;
import java.util.List;

/**
 * App list adapter
 * Created by Hmei on 2017-05-25.
 * 乘着船 修改 2021
 */

public class AppListAdapter extends RecyclerView.Adapter<MyViewHolder<ItemAppListBinding>> {
    private static final String TYPE_SCRIPT = "script";
    public static final String TAG = "AppListAdapter";

    private List<AppModel> dataList;
    private String         type;
    private final Context  context;
    private Callback       callback;

    public AppListAdapter(List<AppModel> dataList, String type, Context context) {
        this.dataList = dataList;
        this.type = type;
        this.context = context;
    }

    @Override
    public MyViewHolder<ItemAppListBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemAppListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_app_list, parent, false);
        MyViewHolder<ItemAppListBinding> holder = new MyViewHolder<>(binding.getRoot());
        holder.setBinding(binding);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder<ItemAppListBinding> holder, int position) {
        ItemAppListBinding binding = holder.getBinding();
        if (type.equals(TYPE_SCRIPT) & position == dataList.size()) {
            binding.ciAppIcon.setImageResource(R.drawable.ic_home_qpy_add);
            binding.tvAppName.setText(R.string.add);
        } else {
            AppModel pos = dataList.get(position);
            binding.ciAppIcon.setImageDrawable(pos.getIcon());
            binding.tvAppName.setText(pos.getLabel());
            if(pos instanceof QPyScriptModel){
                binding.ciAppIcon.setColorFilter(((QPyScriptModel) pos).getPathColor());
        }}
        binding.getRoot().setOnClickListener(v -> {
            if (position == dataList.size()) {
                EditorActivity.start(context, "");
            } else if (dataList.get(position) instanceof QPyScriptModel) {
                QPyScriptModel qPyScriptItem = (QPyScriptModel) dataList.get(position);
                if (qPyScriptItem.isProj()) {
                    callback.runProject(qPyScriptItem);
                } else if (qPyScriptItem.isNotebook()) {
                    callback.runNotebook(qPyScriptItem);
                } else {
                    callback.runScript(qPyScriptItem);
                }
                AppListActivity.setPathsValue(qPyScriptItem);
            } else if (dataList.get(position) instanceof LocalAppModel) {
                LocalAppModel localAppItem = (LocalAppModel) dataList.get(position);
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(localAppItem.getApplicationPackageName());
                if (intent != null) {
                    context.startActivity(intent);
                }
            } else {

                Log.d(TAG, "ONCLICK");
                callback.exit();
            }
        });

        binding.getRoot().setOnLongClickListener(v -> {
            if (position != dataList.size()) {
                CharSequence[] chars = new CharSequence[]{context.getString(R.string.create_shortcut), context.getString(R.string.run_with_params), context.getString(R.string.open_with_editor)};

                final boolean[] isConsumed = new boolean[]{true};
                new AlertDialog.Builder(context, R.style.MyDialog)
                        .setTitle(R.string.choose_action)
                        .setItems(chars, (dialog, which) -> {
                            switch (which) {
                                case 0: // Create Shortcut
                                    isConsumed[0] = createShortCut(position);
                                    dialog.dismiss();
                                    break;
                                case 1: // Run with params
                                    runWithParams(position);
                                    dialog.dismiss();
                                    break;
                                case 2:
                                    openToEdit(position);
                                    dialog.dismiss();

                            }
                        }).setNegativeButton(context.getString(R.string.close), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                        .show();
                return isConsumed[0];
            } else {
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : type.equals(TYPE_SCRIPT) ? dataList.size() + 1 : dataList.size();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void runScript(QPyScriptModel item);
        void runProject(QPyScriptModel item);
        void runNotebook(QPyScriptModel item);
        void exit();
    }

    @SuppressLint("StringFormatInvalid")
    private boolean createShortCut(int position) {
        //最后一个为添加script，不用创建快键图标
        if (position == dataList.size()) {
            return false;
        }
        // Create shortcut
        QPyScriptModel qPyScriptModel = (QPyScriptModel) dataList.get(position);
        Intent intent = new Intent();
        intent.setClass(context, HomeMainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("type", "script");
        intent.putExtra("path", qPyScriptModel.getPath());
        intent.putExtra("isProj", qPyScriptModel.isProj());
        String label = dataList.get(position).getLabel();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);
            if (mShortcutManager.isRequestPinShortcutSupported()) {
                int dot = label.lastIndexOf('.');
                if(dot>0)
                    label = label.substring(0,dot);
                intent.putExtra("label",label);
                new EnterDialog(context)
                        .setTitle(context.getString(R.string.addshortcut_shortcut_label))
                        .setMessage(context.getString(R.string.shortcut_create_unsuc))
                        .setText(label)
                        .setConfirmListener(enterLabel -> {
                            if(enterLabel.isEmpty())
                                enterLabel=intent.getStringExtra("label");
                            intent.removeExtra("label");
                            ShortcutInfo pinShortcutInfo =
                                    new ShortcutInfo.Builder(context, enterLabel)
                                            .setShortLabel(enterLabel)
                                            .setLongLabel(enterLabel)
                                            .setIcon(Icon.createWithResource(context, dataList.get(position).getIconRes()))
                                            .setIntent(intent)
                                            .build();
                            Intent pinnedShortcutCallbackIntent =
                                    mShortcutManager.createShortcutResultIntent(pinShortcutInfo);
                            PendingIntent successCallback = PendingIntent.getBroadcast(context, 0,
                                    pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);
                            mShortcutManager.requestPinShortcut(pinShortcutInfo,
                                    successCallback.getIntentSender());
                            return true;
                        }).show();
            //Toast.makeText(context, context.getString(R.string.shortcut_create_unsuc, dataList.get(position).getLabel()), Toast.LENGTH_LONG).show();
            }
        } else {
            //Adding shortcut for MainActivity
            //on Home screen
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context.getApplicationContext(),
                            dataList.get(position).getIconRes()));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.getApplicationContext().sendBroadcast(addIntent);
            Toast.makeText(context, context.getString(R.string.shortcut_create_suc, dataList.get(position).getLabel()), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void openToEdit(int position) {
        QPyScriptModel model = (QPyScriptModel) dataList.get(position);
        if (model.isProj()) {
            TedLocalActivity.start(context,model.getPath());
        } else {
            EditorActivity.start(context, Uri.fromFile(new File(model.getPath())));
        }
    }

    private void runWithParams(int position) {
        new EnterDialog(context)
                .setTitle(context.getString(R.string.enter_u_params))
                .setHint(context.getString(R.string.params))
                .setConfirmListener(args -> {
                    if (TextUtils.isEmpty(args)) {
                        Toast.makeText(context, R.string.params_emp, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    QPyScriptModel model = (QPyScriptModel) dataList.get(position);
                    if (model.isProj()) {
                        ScriptExec.getInstance().playProject(context, model.getPath(), args);
                    } else {
                        ScriptExec.getInstance().playScript(context, model.getPath(),
                                args);
                    }
                    return true;
                })
                .show();
    }
}
