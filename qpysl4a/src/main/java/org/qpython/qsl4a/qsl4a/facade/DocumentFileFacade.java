package org.qpython.qsl4a.qsl4a.facade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import com.quseit.util.DocumentsUtils;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

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
    public boolean documentTreeShowOpen(
            @RpcParameter(name = "rootPath") String rootPath
    ) throws Exception {
        if (DocumentsUtils.checkWritableRootPath(context, rootPath)) {
            Intent intent = null;
            StorageManager sm = context.getSystemService(StorageManager.class);
            StorageVolume volume = sm.getStorageVolume(new File(rootPath));
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
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Rpc(description = "The same as documentTreeShowOpen .")
    public boolean documentFileShowOpen(
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

    @Override
    public void shutdown() {
    }
}