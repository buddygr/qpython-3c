package org.qpython.qpy.main.model;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.quseit.util.FileHelper;

import org.qpython.qpy.R;
import org.qpython.qpy.main.app.App;
import org.qpython.qpy.main.app.CONF;

import java.io.File;
import java.io.IOException;

import util.FileUtil;

/**
 * QPython script list item model
 * Created by Hmei on 2017-05-25.
 * Edit by 乘着船 at 2023
 */

public class QPyScriptModel extends AppModel {
    private final File file;
    private byte fileType;
    private int res = -1;
    private byte pathType = 0;
    public static final byte SCRIPT = 0;
    public static final byte NOTEBOOK = -1;
    public static final byte PROJECT = 1;

    private static final int[] COLOR = {
        Color.argb(0,0,0,0),
        Color.argb(32,192,255,192),
        Color.argb(48, 224,224,191)
    };

    public QPyScriptModel(File file) {
        this.file = file;
        if(file==null)
            return;
        String path = file.getAbsolutePath();
        if(path.startsWith(CONF.SCOPE_STORAGE_PATH))
            pathType = 0;
        else if (path.startsWith(CONF.LEGACY_PATH))
            pathType = 1;
        else
            pathType = 2;
    }


    private int initRes() {
        String path = getPath();
        if(path.contains("/projects"))
            fileType = PROJECT;
        String content;
        if (fileType == PROJECT) {
            File mainPy = FileHelper.getMainFileByType(getFile());
            if (mainPy == null) {
                res = R.drawable.ic_project_qapp;
                return res;
            }
            content = FileUtil.getFileContents(mainPy.getAbsolutePath());
        } else {
            if(path.endsWith(".ipynb")) {
                fileType = NOTEBOOK;
                content = "";
            } else content = FileUtil.getFileContents(path);
        }
        if(fileType == NOTEBOOK){
            res = R.drawable.ic_pyfile_notebookapp;
        }else{
        boolean isWeb = content.contains("#qpy:webapp");
        if (isWeb) {
                //res = (isProj ? R.drawable.ic_project_webapp3 : R.drawable.ic_pyfile_webapp3);
                res = (fileType == PROJECT ? R.drawable.ic_project_webapp : R.drawable.ic_pyfile_webapp);
        } else {
                res = (fileType == PROJECT ? R.drawable.ic_project_qapp3 : R.drawable.ic_pyfile_qapp3);
        }}
        return res;
    }

    @Override
    public Drawable getIcon() {
        return App.getContext().getResources().getDrawable(res == -1 ? initRes() : res);
    }

    @Override
    public String getLabel() {
        return file.getName();
    }

    @Override
    public int getIconRes() {
        return res == -1 ? initRes() : res;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public byte getPathType(){
        return pathType;
    }

    public int getPathColor(){
        return COLOR[pathType];
    }

    public boolean isProj() {
        return fileType == PROJECT;
    }

    public boolean isNotebook() {
        return fileType == NOTEBOOK;
    }

    public byte getFileType(){
        return fileType;
    }
}
