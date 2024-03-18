package com.quseit.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * 对SD卡文件的管理
 *
 * @author ch.linghu
 */
public class FileHelper {
    @SuppressWarnings("unused")
    private static final String TAG = "FileHelper";
    private static List<File> typeFiles;

    public static final void createDirIfNExists(String dirname) {
        File yy = new File(dirname);
        if (!yy.exists()) {
            yy.mkdirs();
        }
    }

    public static final void createFileFromAssetsIfNExists(Context con, String filename, String dst) {
        File yy = new File(dst);
        if (!yy.exists()) {
            String content = FileHelper.LoadDataFromAssets(con, filename);
            FileHelper.writeToFile(dst, content);
        }
    }

    public static void openFile(Context context, String filePath, String fileExtension) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(filePath);
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getMimeTypeFromExtension(fileExtension);
        intent.setDataAndType(Uri.fromFile(file), type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
        }
    }

    public static String getFileNameFromUrl(String urlFile) {
        try {
            URL url = new URL(urlFile);
            File f = new File(url.getPath());
            return f.getName();

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            return "unname.dat";
        }
    }

    public static String getTypeByMimeType(String mType) {
        if (mType.equals("application/vnd.android.package-archive")) {
            return "apk";
        } else {
            String[] xx = mType.split("/");
            if (xx.length > 1) {
                return xx[0];
            }
        }
        return "other";
    }

    public static String LoadDataFromAssets(Context context, String inFile) {
        String tContents = "";

        try {
            InputStream stream = context.getAssets().open(inFile);
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
        }
        return tContents;
    }

    public static void putFileContents(String filename, String content) {
        try {
            File fileCache = new File(filename);
            byte[] data = content.getBytes();
            FileOutputStream outStream;
            outStream = new FileOutputStream(fileCache);
            outStream.write(data);
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void writeToFile(String filePath, String data) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    return;
                }
            }

            FileOutputStream fOut = new FileOutputStream(filePath);
            fOut.write(data.getBytes());
            fOut.flush();
            fOut.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }

    }

    public static String getFileContents(String filename, int pos) {

        File scriptFile = new File(filename);
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                while ((line = in.readLine()) != null) {
                    tContent.append(line).append("\n");
                    if (tContent.length() >= pos) {
                        in.close();
                        return tContent.toString();
                    }
                }
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return tContent.toString();
    }

    public static String getFileContent(String filename) {
        File scriptFile = new File(filename);
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                int off = 0;int len = 0;
                char[] buf = new char[100];
                while ((len = in.read(buf,off,100)) == 100) {
                    tContent.append(buf);
                }
                tContent.append(buf,0,len);
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return tContent.toString();
    }

    public static String getFileContents(String filename) {
        File scriptFile = new File(filename);
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                while ((line = in.readLine()) != null) {
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

    public static String getFileContents(File scriptFile) {
        StringBuilder tContent = new StringBuilder();
        if (scriptFile.exists()) {
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(scriptFile));
                String line;

                while ((line = in.readLine()) != null) {
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

    public static void clearDir(String dir, int level, boolean deleteS) {
        //Log.d(TAG, "clearDir:"+dir);
        File basePath = new File(dir);
        if (basePath.exists() && basePath.isDirectory()) {
            for (File item : basePath.listFiles()) {
                if (item.isFile()) {
                    //Log.d(TAG, "deleteItem:"+item.getAbsolutePath());
                    item.delete();

                } else if (item.isDirectory()) {
                    clearDir(item.getAbsolutePath(), level + 1, deleteS);
                }
            }
            if (level > 0 || deleteS) {
                basePath.delete();
            }
        } else if (basePath.exists()) {
            basePath.delete();
        }
    }

    public static File getBasePath(String parDir, String subdir) throws IOException {
        try {
            File basePath = new File(Environment.getExternalStorageDirectory(),
                    parDir);

            if (!basePath.exists()) {
                if (!basePath.mkdirs()) {
                    throw new IOException(String.format("%s cannot be created!",
                            basePath.toString()));
                }
            }
            File subPath = null;
            if (!subdir.equals("")) {
                subPath = new File(Environment.getExternalStorageDirectory(),
                        parDir + "/" + subdir);
                if (!subPath.exists()) {
                    if (!subPath.mkdirs()) {
                        throw new IOException(String.format("%s cannot be created!",
                                subPath.toString()));
                    }
                }
            }

            if (!basePath.isDirectory()) {
                throw new IOException(String.format("%s is not a directory!",
                        basePath.toString()));
            }
            if (subdir.equals(""))
                return basePath;
            else
                return subPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getABSPath(String subdir) throws IOException {
        File basePath = new File(subdir);

        if (!basePath.exists()) {
            if (!basePath.mkdirs()) {
                throw new IOException(String.format("%s cannot be created!",
                        basePath.toString()));
            }
        }
        File subPath = null;
        if (!subdir.equals("")) {
            subPath = new File(subdir);
            if (!subPath.exists()) {
                if (!subPath.mkdirs()) {
                    throw new IOException(String.format("%s cannot be created!",
                            subPath.toString()));
                }
            }
        }

        if (!basePath.isDirectory()) {
            throw new IOException(String.format("%s is not a directory!",
                    basePath.toString()));
        }
        if (subdir.equals(""))
            return basePath;
        else
            return subPath;
    }

    public static String getFileName(String filename) {
        File f = new File(filename);
        return f.getName();
    }

    public static String getExt(String filename, String def) {
        String[] yy = filename.split("\\?");
        String[] xx = yy[0].split("\\.");
        //Log.d(TAG, "filename:"+filename+"-size:"+xx.length);

        if (xx.length < 2) {
            return def;
        } else {
            String ext = xx[xx.length - 1];
            //Log.d(TAG, "ext:"+ext);
            return ext;
        }
    }

    public static JSONObject getUrlAsJO(String link) {
        try {
            // get URL content
            URL url = new URL(link);
            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            String inputLine;


            String ret = "";

            while ((inputLine = br.readLine()) != null) {
                ret = ret + inputLine + "\n";
            }

            br.close();

            try {
                return new JSONObject(ret.trim());
            } catch (JSONException e) {
                return null;
            }
            //System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * @param dir
     * @return The main file to be found in dir
     */
    public static File getMainFileByType(File dir) {
        File xx = new File(dir.getAbsolutePath() + "/main.py");
        return xx.exists() ? xx : null;
    }

    /**
     * Filter Files by type
     *
     * @param dir
     * @return
     */
    public static File[] getPyFiles(File dir) {
        if (dir==null) {
            return null;
        }
        typeFiles = new ArrayList<>();
        addPyFile(dir);
        return typeFiles.toArray(new File[0]);
    }

    private static void addPyFile(File dir) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles!=null) {
            for (File file : dirFiles) {
                if (file.isDirectory() && !file.getAbsolutePath().contains("/.")) {
                    addPyFile(file);
                } else {
                    String filename = file.getName();
                    if ((filename.endsWith(".py")||filename.endsWith(".ipynb")) && filename.charAt(0)!='.')
                        typeFiles.add(file);
                }
            }
        }
    }


}
