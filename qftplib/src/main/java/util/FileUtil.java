/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.

文件管理类 by 乘着船 at 2023
 */

package util;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.system.ErrnoException;
import android.system.Os;

import org.swiftp.Globals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import util.DocumentUtil;
import util.IoUtil;

/**
 * Utility functions for handling files.
 *
 * @author Damon Kohler (damonkohler@gmail.com)
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    public static Context activity;

    private FileUtil() {
        // Utility class.
    }

    static public boolean externalStorageMounted() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static int chmod(File path, int mode) throws Exception {
        Class<?> fileUtils = Class.forName("android.os.FileUtils");
        Method setPermissions =
                fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
        return (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setPermission(File file) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);

        Files.setPosixFilePermissions(file.toPath(), perms);
    }

    public static boolean recursiveChmod(File root, int mode) throws Exception {
        boolean success = chmod(root, mode) == 0;
        for (File path : root.listFiles()) {
            if (path.isDirectory()) {
                success = recursiveChmod(path, mode);
            }
            success &= (chmod(path, mode) == 0);
        }
        return success;
    }

    public static boolean delete(String path) {
        return delete(new File(path));
    }

    public static boolean delete(File path) {
        boolean result = true;
        if (path.canWrite()) {
            if (path.isDirectory()) {
                for (File child : path.listFiles()) {
                    result &= delete(child);
                }
                result &= path.delete(); // Delete empty directory.
            } else if (path.isFile()) {
                result = path.delete();
            }
            //if (!result) {
                //Log.e(TAG, "Delete failed;");
            //}
            return result;
        } else {
            if (path.exists()) {
                DocumentFile documentFile = DocumentUtil.getDocumentFile(path,null,activity);
            if(documentFile!=null)
                return documentFile.delete();
            else return false;
            } else {
                //Log.e(TAG, "File does not exist.");
            return false;
           }
        }
    }

    public static File copyFromStream(String name, InputStream input) {
        if (name == null || name.length() == 0) {
            //Log.e(TAG, "No script name specified.");
            return null;
        }
        File file = new File(name);
        if (!makeDirectories(file.getParentFile(), 0755)) {
            return null;
        }
        try {
            OutputStream output = new FileOutputStream(file);
            IoUtil.copy(input, output);
        } catch (Exception e) {
            //Log.e(TAG, e);
            return null;
        }
        return file;
    }

    public static boolean makeDirectories(File directory, int mode) {
        File parent = directory;
        while (parent.getParentFile() != null && !parent.exists()) {
            parent = parent.getParentFile();
        }
        if (!directory.exists()) {
            //Log.d(TAG, "Creating directory: " + directory.getName());
            if (!directory.mkdirs()) {
                //Log.e(TAG, "Failed to create directory.");
                return false;
            }
        }
        try {
            recursiveChmod(parent, mode);
        } catch (Exception e) {
            //Log.e(TAG, e);
            return false;
        }
        return true;
    }

    public static boolean mkdir(File directory) {
        boolean result;
        if (directory.exists())
          return false;
        result = directory.mkdirs();
        if(!result) {
            DocumentFile documentFile = DocumentUtil.getDocumentFile(directory, true, activity);
            result = documentFile != null && documentFile.exists();
        }
        return result;
    }

    public static File getExternalDownload() {
        try {
            Class<?> c = Class.forName("android.os.Environment");
            Method m = c.getDeclaredMethod("getExternalStoragePublicDirectory", String.class);
            String download = c.getDeclaredField("DIRECTORY_DOWNLOADS").get(null).toString();
            return (File) m.invoke(null, download);
        } catch (Exception e) {
            return new File(Environment.getExternalStorageDirectory(), "Download");
        }
    }

    public static boolean rename(String oldPath, String newPath) {
        return rename(new File(oldPath),new File(newPath));
    }

    public static boolean rename(File file, String name) {
        return rename(file,new File(file.getParent(), name));
    }
    public static boolean rename(File oldFile,File newFile) {
        boolean result =oldFile.renameTo(newFile);
        if(!result && oldFile.exists()) {
            try {
                DocumentFile documentFile = DocumentUtil.getDocumentFile(oldFile,null,activity);
                if (documentFile != null)
                    result = documentFile.renameTo(newFile.getName());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return result;
    }

    public static String readToString(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        FileReader reader = new FileReader(file);
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[1024 * 4];
        int numRead = 0;
        while ((numRead = reader.read(buffer)) > -1) {
            out.append(String.valueOf(buffer, 0, numRead));
        }
        reader.close();
        return out.toString();
    }

    public static String readFromAssetsFile(Context context, String name) throws IOException {
        AssetManager am = context.getAssets();
        BufferedReader reader = new BufferedReader(new InputStreamReader(am.open(name)));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public static void lnOrcopy(File src, File dst, int sdk) throws IOException, ErrnoException {

        if (sdk>=21) {
            Os.symlink(src.getAbsolutePath(), dst.getAbsolutePath());
        } else {
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
        }
    }

    public static String getFileContents(String filename) {

        File scriptFile = new File( filename );
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                while ((line = in.readLine())!=null) {
                    tContent.append(line).append("\n");
                }
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return tContent.toString();
    }

    public static String getFileContents(String filename, int pos) {

        File scriptFile = new File( filename );
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                while ((line = in.readLine())!=null) {
                    tContent.append(line).append("\n");
                    if (tContent.length()>=pos) {
                        in.close();
                        return tContent.toString();
                    }
                }
                in.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return tContent.toString();
    }

    public static boolean canWrite(File file){
    boolean canWrite = file.canWrite();
    if(!canWrite){
        DocumentFile documentFile = DocumentUtil.getDocumentFile(file,false, activity);
        if(documentFile!=null)
            canWrite = documentFile.canWrite();
    }
      return canWrite;
    }

    public static FileOutputStream getFileOutputStream(File file,boolean append){
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file, append);
        } catch (IOException e){
            fileOutputStream = (FileOutputStream) DocumentUtil.getOutputStream(activity,file,append);
        }
        return fileOutputStream;
    }

    public static FileOutputStream getFileOutputStream(File file){
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e){
            fileOutputStream = (FileOutputStream) DocumentUtil.getOutputStream(activity,file);
        }
        return fileOutputStream;
    }

    public static void writeToFile(String filePath, String text, boolean append) {
        FileOutputStream fOut;
        try{
        try {
            fOut = new FileOutputStream(filePath,append);
            fOut.write(text.getBytes());
        } catch (IOException e) {
            DocumentFile file = DocumentUtil.getDocumentFile(new File(filePath), false, activity);
            String mode;
            if(append)
                mode = "wa";
            else mode = "wt";
            fOut = (FileOutputStream) activity.getContentResolver().openOutputStream(file.getUri(),mode);
            fOut.write(text.getBytes());
        }
            fOut.flush();
            fOut.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    public static void writeToFile(String filePath, String text) {
        FileOutputStream fOut;
        try{
            try {
                fOut = new FileOutputStream(filePath);
                fOut.write(text.getBytes());
            } catch (IOException e) {
                DocumentFile file = DocumentUtil.getDocumentFile(new File(filePath), false, activity);
                fOut = (FileOutputStream) activity.getContentResolver().openOutputStream(file.getUri(),"wt");
                fOut.write(text.getBytes());
            }
            fOut.flush();
            fOut.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }
}
