package org.qpython.qpysdk.utils;

import java.io.File;

/**
 * 对SD卡文件的管理
 * 
 * @author ch.linghu
 * 
 */
public class FileHelper {
	@SuppressWarnings("unused")
	private static final String TAG = "FileHelper";


	public static String getFileName(String filename) {
		File f = new File(filename);
		return f.getName();
	}
	
    public static String getExt(String filename, String def) {
    	String[] yy = filename.split("\\?");
        String[] xx = yy[0].split("\\.");
        //LogUtil.d(TAG, "filename:"+filename+"-size:"+xx.length);
    
        if (xx.length<2) {
            return def; 
        } else {
            String ext = xx[xx.length-1];
            //LogUtil.d(TAG, "ext:"+ext);
            return ext;
        }   
    }

}
