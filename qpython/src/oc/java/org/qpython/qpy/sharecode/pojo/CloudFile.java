package org.qpython.qpy.codeshare.pojo;


import org.qpython.qpy.main.app.CONF;

import java.io.Serializable;

public class CloudFile implements Serializable {
    private String projectName;
    private String path;
    private String uploadTime;
    private String name;
    private String content;
    private String key;
    private boolean uploading;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getContentSize() {
        return content.length();
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAbsolutePath() {
        return CONF.SCOPE_STORAGE_PATH + getPath();
    }
}
