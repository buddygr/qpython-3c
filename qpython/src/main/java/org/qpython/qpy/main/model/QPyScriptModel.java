package org.qpython.qpy.main.model;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.quseit.util.FileHelper;

import org.qpython.qpy.R;
import org.qpython.qpy.main.app.App;
import org.qpython.qpy.main.app.CONF;

import java.io.File;
import java.io.IOException;

/**
 * QPython script list item model
 * Created by Hmei on 2017-05-25.
 */

public class QPyScriptModel extends AppModel {
    private File    file;
    private boolean isProj;
    private int res = -1;
    private byte pathType = 0;

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
        isProj = getPath().contains("/projects");
        String content;
        if (isProj) {
            File mainPy = FileHelper.getMainFileByType(getFile());
            if (mainPy == null) {
                res = R.drawable.ic_project_qapp;
                return res;
            }
            content = org.qpython.qpysdk.utils.FileHelper.getFileContents(mainPy.getAbsolutePath());
        } else {
            content = org.qpython.qpysdk.utils.FileHelper.getFileContents(getPath());
        }
        boolean isWeb = content.contains("#qpy:webapp");

        if (isWeb) {
            //if (isPy3) {
                res = (isProj ? R.drawable.ic_project_webapp3 : R.drawable.ic_pyfile_webapp3);
            //} else {
                res = (isProj ? R.drawable.ic_project_webapp : R.drawable.ic_pyfile_webapp);
            //}
        } else {
            //if (isPy3) {
                res = (isProj ? R.drawable.ic_project_qapp3 : R.drawable.ic_pyfile_qapp3);
            //} else {
             //   res = (isProj ? R.drawable.ic_project_qapp : R.drawable.ic_pyfile_qapp);
           // }
        }
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
        return isProj;
    }
}
