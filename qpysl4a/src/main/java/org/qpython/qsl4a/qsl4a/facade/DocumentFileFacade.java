package org.qpython.qsl4a.qsl4a.facade;
//by 乘着船 at 2021-2022

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.util.Base64;

import com.quseit.util.DocumentsUtils;

import org.json.JSONArray;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DocumentFileFacade extends RpcReceiver {

    private final AndroidFacade mAndroidFacade;
    private final Context context;

    public DocumentFileFacade(FacadeManager manager) {
        super(manager);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        context = mAndroidFacade.context;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Rpc(description = "Show Open Document Tree with RootPath .")
    public Uri documentTreeShowOpen(
            @RpcParameter(name = "rootPath") String rootPath
    ) throws Exception {
        File file = new File(rootPath);
        if(file.canWrite())
            return Uri.fromFile(file);
        String p = file.getAbsolutePath();
        if(p.startsWith(DocumentsUtils.ANDROID_PATH))
            return documentTreeAndroid(file);
        // else next grade 1
        DocumentFile documentFile;
        if (DocumentsUtils.isOnExtSdCard(file, context)) {
            documentFile = DocumentsUtils.getDocumentFile(file, true, context);
            if(documentFile != null && documentFile.canWrite())
                return documentFile.getUri();
            // else next grade 2
            } else {
                p = PreferenceManager.getDefaultSharedPreferences(context).getString(rootPath, null);
                if (p != null) {
                    documentFile = DocumentFile.fromTreeUri(context, Uri.parse(p));
                    if(documentFile != null && documentFile.canWrite())
                        return documentFile.getUri();
                    // else next grade 3
                } // else next grade 2
            }
        Intent intent = null;
        StorageManager sm = context.getSystemService(StorageManager.class);
        StorageVolume volume = sm.getStorageVolume(file);
        if (volume != null) {
            intent = volume.createAccessIntent(null);
        }
       if (intent == null) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
       }
       Intent intentR = mAndroidFacade.startActivityForResultCode(intent);
       switch (intentR.getIntExtra("RESULT_CODE", -1025)) {
           case -1025:
               throw new Exception(intentR.getStringExtra("EXCEPTION"));
           case Activity.RESULT_OK:
               Uri uri = intentR.getData();
               DocumentsUtils.saveTreeUri(context,rootPath,uri);
               return uri;
           default:
               return null;
            }
    }

    @Rpc(description = "Document File Rename .")
    public boolean documentFileRenameTo (
            @RpcParameter(name = "src") String src,
            @RpcParameter(name = "dest") String dest) throws Exception {
        return DocumentsUtils.renameTo(context,new File(src),new File(dest));
    }

    @Rpc(description = "Document File ( or Tree ) Delete .")
    public boolean documentFileDelete (
            @RpcParameter(name = "file or tree") String file) {
        return DocumentsUtils.delete(context,new File(file));
    }

    @Rpc(description = "Document File Make Directorys .")
    public boolean documentFileMkdir (
            @RpcParameter(name = "dir") String dir) {
        return DocumentsUtils.mkdirs(context,new File(dir));
    }

    @Rpc(description = "Document File Input Stream .")
    public String documentFileInputStream (
            @RpcParameter(name = "srcFile") String srcFile,
            @RpcParameter(name = "encodingFormat") @RpcDefault("") String encodingFormat)
    throws Exception{
        byte[] data;
        InputStream fis=DocumentsUtils.getInputStream(context,new File(srcFile));
        int length = fis.available();
        data = new byte[length];
        fis.read(data);
        fis.close();
        if (encodingFormat.equals("")) {
            return Base64.encodeToString( data, Base64.DEFAULT );
        } else {
            return new String(data, encodingFormat);
        }
        }

    @Rpc(description = "Document File Output Stream .")
    public void documentFileOutputStream (
            @RpcParameter(name = "destFile") String destFile,
            @RpcParameter(name = "srcString") @RpcDefault("") String srcString,
            @RpcParameter(name = "encodingFormat") @RpcDefault("") String encodingFormat)
            throws Exception{
        byte[] data;
        if (encodingFormat.equals("")) {
            data = Base64.decode( srcString, Base64.DEFAULT );
        } else {
            data = srcString.getBytes( encodingFormat );
        }
        OutputStream fos=DocumentsUtils.getOutputStream(context,new File(destFile));
        fos.write(data);
        fos.flush();
        fos.close();
    }

    @Rpc(description = "Document File Copy .")
    public void documentFileCopy (
            @RpcParameter(name = "src") String src,
            @RpcParameter(name = "dest") String dest)
            throws Exception{
        DocumentsUtils.copy(context,new File(src),new File(dest));
    }

    @Rpc(description = "Document File List Files .")
    public JSONArray documentFileListFiles (
            @RpcParameter(name = "folder") String folder
    ) throws Exception {
        JSONArray jsonArray = new JSONArray();
        String[] S = DocumentsUtils.listFiles(context,new File(folder));
        if(S==null) return null;
        for (String s : S)
            jsonArray.put(s);
        return jsonArray;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Rpc(description = "The same as documentTreeShowOpen .")
    public Uri documentFileShowOpen(
            @RpcParameter(name = "rootPath") String rootPath
    ) throws Exception {
        return documentTreeShowOpen(rootPath);
    }

    @Rpc(description = "The same as documentFileRenameTo .")
    public boolean documentFileMoveTo (
            @RpcParameter(name = "src") String src,
            @RpcParameter(name = "dest") String dest) throws Exception {
        return documentFileRenameTo(src,dest);
    }

    @Rpc(description = "The same as documentFileMkdir .")
    public boolean documentFileMkdirs (
            @RpcParameter(name = "dir") String dir) {
        return documentFileMkdir(dir);
    }

    @Rpc(description = "Document File Get Uri .")
    public Uri documentFileGetUri (
            @RpcParameter(name = "path") String path,
            @RpcParameter(name = "isDirectory") @RpcOptional Boolean isDirectory) {
        DocumentFile documentFile = DocumentsUtils.getDocumentFile(new File(path),isDirectory,context);
        if(documentFile == null) return null;
        else return documentFile.getUri();
    }

    @Rpc(description = "Document File Is Directory .")
    public Boolean documentFileIsDirectory (
            @RpcParameter(name = "path") String path
    ) {
        DocumentFile documentFile = DocumentsUtils.getDocumentFile(new File(path),null,context);
        if(documentFile == null) return null;
        else return documentFile.isDirectory();
    }

    @Rpc(description = "Document File Get Stat .")
    public Map<String,Object> documentFileGetStat(
            @RpcParameter(name = "path") String path
    ){
        DocumentFile documentFile = DocumentsUtils.getDocumentFile(new File(path),null,context);
        if(documentFile == null) return null;
        Map<String,Object> map = new HashMap<>();
        map.put("length",documentFile.length());
        map.put("lastModified",documentFile.lastModified());
        map.put("isDirectory",documentFile.isDirectory());
        map.put("canRead",documentFile.canRead());
        map.put("canWrite",documentFile.canWrite());//外置卡此处为true
        return map;
    }

    @Rpc(description = "get file stat .")
    public Map<String,Object> getFileStat(
            @RpcParameter(name = "path") String path
    ){
        File file = new File(path);
        Map<String,Object> map = new HashMap<>();
        map.put("length",file.length());
        map.put("lastModified",file.lastModified());
        map.put("canRead",file.canRead());
        map.put("canWrite",file.canWrite());//外置卡此处为false
        map.put("canExecute",file.canExecute());
        map.put("FreeSpace",file.getFreeSpace());
        map.put("TotalSpace",file.getTotalSpace());
        return map;
    }

    public Uri documentTreeAndroid(File file) throws Exception {
        String path = file.getAbsolutePath();
        String subPath = path.substring(DocumentsUtils.ANDROID_PATH.length());
        SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(context);
        String uriStr = perf.getString(path,null);
        Uri uri = null;
        DocumentFile documentFile = null;
        if(uriStr != null)
            uri = Uri.parse(uriStr);
        if(uri != null)
            documentFile = DocumentFile.fromTreeUri(context, uri);
        if(documentFile != null && documentFile.canWrite()) {
            addOnSdCardList(path);
            return uri;
        }
        String content = subPath.substring(0,subPath.indexOf("/"));
        content = DocumentsUtils.ANDROID_CONTENT[0] + subPathToContent(content) +
                DocumentsUtils.ANDROID_CONTENT[1] + subPathToContent(subPath);
        uri = Uri.parse(content);
        documentFile = DocumentFile.fromTreeUri(context, uri);
        if(documentFile == null)
            return null;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(DocumentsUtils.ANDROID_OPEN_INTENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        }
        Intent intentR = mAndroidFacade.startActivityForResultCode(intent);
        switch (intentR.getIntExtra("RESULT_CODE", -1025)) {
            case -1025:
                throw new Exception(intentR.getStringExtra("EXCEPTION"));
            case Activity.RESULT_OK:
                uri = intentR.getData();
                context.getContentResolver().takePersistableUriPermission(uri,DocumentsUtils.ANDROID_SAVE_INTENT);
                addOnSdCardList(path);
                perf.edit().putString(path, uri.toString()).apply();
                return uri;
            default:
                return null;
        }
    }

    public String subPathToContent(String subPath) {
        if (subPath.endsWith("/")) {
            subPath = subPath.substring(0, subPath.length() - 1);
        }
        return subPath.replace("%","%25").replace("/", "%2F").
                replace(" ","%20");
    }

    private void addOnSdCardList(String path){
        if(!DocumentsUtils.sExtSdCardPaths.contains(path))
            DocumentsUtils.sExtSdCardPaths.add(path);
    }

    @Override
    public void shutdown() {
    }
}