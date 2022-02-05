package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.pm.PackageManager;
import android.util.Base64;

import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherFacade extends RpcReceiver {

    private final Service mService;
    private final PackageManager mPackageManager;
    //加密引擎
    private Cipher cipherEncrypt;
    //解密引擎
    private Cipher cipherDecrypt;
    //字符串编码
    private String EncodingFormat;
    public static final int MAX_BUFFER_SIZE = 5242848;//max buffer size (5MB-32B)

    public CipherFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mPackageManager = mService.getPackageManager();
    }

    private static byte[] readFromFile(String filePath) {
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            byte[] data = new byte[length];
            fis.read(data);
            fis.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }}

    private static void writeToFile(String filePath, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Exception e) {e.printStackTrace();}}

    /**
     * @function cipherInit 加密解密引擎设置
     * @params key 密钥
     * @params algorithm 加密算法(默认为AES/CBC/PKCS5Padding)
     * @params encodingFormat 字符串编码格式(默认为空白(Base64)，也可为UTF-8,GBK)
     * @params initialVector 初始向量(CBC模式时使用)
     */
    @Rpc(description = "Initialize Encrypt Engine / Decrypt Engine .")
    public void cipherInit(
            @RpcParameter(name = "key") final String key,
            @RpcParameter(name = "algorithm") @RpcDefault("AES/CBC/PKCS5Padding") final String algorithm,
            @RpcParameter(name = "encodingFormat") @RpcDefault("") final String encodingFormat,
            @RpcParameter(name = "initialVector") @RpcDefault("") final String initialVector ) throws Exception {
        cipherEncrypt = Cipher.getInstance(algorithm);
        cipherDecrypt = Cipher.getInstance(algorithm);
        byte[] Key;
        byte[] InitialVector;
        EncodingFormat = encodingFormat.toUpperCase();
        if (EncodingFormat.equals("")) {
            //Base64解码
            Key = Base64.decode( key, Base64.DEFAULT );
            if (initialVector.equals("")) {
                InitialVector = Key;
            } else {
               InitialVector = Base64.decode( initialVector, Base64.DEFAULT );
            }
        } else {
            //其他字符串编码解码
            Key = key.getBytes( encodingFormat );
            if (initialVector.equals("")) {
                InitialVector = Key;
            } else {
                InitialVector = initialVector.getBytes(encodingFormat);
            }
            /*if (InitialVector.length > 16){
                byte[] IV = {};
                System.arraycopy(InitialVector,0,IV,0,16);
                InitialVector = IV;
            }*/
        }
        //Algorithm 加密算法如AES
        String Algorithm;
        int pos = algorithm.indexOf("/");
        if ( pos == -1 ) {
            Algorithm = algorithm;
        }
        else {
            Algorithm = algorithm.substring( 0, pos );
        }
        SecretKeySpec keySpec = new SecretKeySpec(Key, Algorithm);
        //使用CBC模式，需要一个向量iv，可增加加密算法的强度
        IvParameterSpec iv = new IvParameterSpec(InitialVector);
        if (algorithm.contains("CBC")) {
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, keySpec, iv);
        } else {//ECB模式
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, keySpec);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, keySpec);
        }
    }

    @Override
    public void shutdown() {
    }

    /**  全字符串传输模式  */

    @Rpc(description = "Encrypt a Normal / Base64 String to another Base64 String .")
    public String encryptString(
            @RpcParameter(name = "srcString") final String srcString)
            throws Exception {
        byte[] rst;
        if (EncodingFormat.equals("")) {
            rst = Base64.decode( srcString, Base64.DEFAULT );
        } else {
            rst = srcString.getBytes( EncodingFormat );
        }
        return Base64.encodeToString(
                cipherEncrypt.doFinal( rst )
                , Base64.DEFAULT);
    }

    @Rpc(description = "Decrypt a Base64 String to another Normal / Base64 String .")
    public String decryptString(
            @RpcParameter(name = "srcString") final String srcString)
            throws Exception {
        byte[] rst = cipherDecrypt.doFinal( Base64.decode( srcString, Base64.DEFAULT ));
        if (EncodingFormat.equals("")) {
            return Base64.encodeToString( rst, Base64.DEFAULT );
        } else {
            return new String( rst, EncodingFormat );
        }
    }

    /**  半字符串半文件模式  */

    @Rpc(description = "Encrypt a Normal / Base64 String to another File .")
    public void encryptStringToFile(
            @RpcParameter(name = "srcString") final String srcString,
            @RpcParameter(name = "dstFile") final String dstFile)
            throws Exception {
        byte[] rst;
        if (EncodingFormat.equals("")) {
            rst = Base64.decode( srcString, Base64.DEFAULT );
        } else {
            rst = srcString.getBytes( EncodingFormat );
        }
        writeToFile( dstFile ,
                cipherEncrypt.doFinal( rst ));
    }

    @Rpc(description = "Decrypt a File to another Normal / Base64 String .")
    public String decryptFileToString(
            @RpcParameter(name = "srcFile") final String srcFile)
            throws Exception {
        byte[] rst = cipherDecrypt.doFinal( readFromFile( srcFile ));
        if (EncodingFormat.equals("")) {
            return Base64.encodeToString( rst, Base64.DEFAULT );
        } else {
            return new String( rst, EncodingFormat );
        }
    }

    /**  全二进制文件模式  */

    private void cipherFile(final String srcFile,final String dstFile,final Cipher cipher)
            throws Exception {
        FileInputStream fis = new FileInputStream(srcFile);
        FileOutputStream fos = new FileOutputStream(dstFile);
        int len = fis.available();
        if (len>MAX_BUFFER_SIZE) len=MAX_BUFFER_SIZE; //max buffer size 5MB
        byte[] data = new byte[len];
        while((len=fis.read(data))==MAX_BUFFER_SIZE)
            fos.write(cipher.update(data));
        fos.write(cipher.doFinal(data,0,len));
        fos.flush();
        fis.close();
        fos.close();
    }

    @Rpc(description = "Encrypt a File to another File .")
    public void encryptFile(
            @RpcParameter(name = "srcFile") final String srcFile,
            @RpcParameter(name = "dstFile") final String dstFile)
            throws Exception {
        cipherFile(srcFile,dstFile,cipherEncrypt);
    }

    @Rpc(description = "Decrypt a File to another File .")
    public void decryptFile(
            @RpcParameter(name = "srcFile") final String srcFile,
            @RpcParameter(name = "dstFile") final String dstFile)
            throws Exception  {
        cipherFile(srcFile,dstFile,cipherDecrypt);
    }
}