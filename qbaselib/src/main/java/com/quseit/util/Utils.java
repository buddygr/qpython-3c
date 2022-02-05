package com.quseit.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
	private static final String TAG = "Utils";

	public static <T> List<T> copyIterator(Iterator<T> iter) {
	    List<T> copy = new ArrayList<T>();
	    while (iter.hasNext())
	        copy.add(iter.next());
	    return copy;
	}
	
	public static boolean ifPyRunOk(String file) {
    	/*String content = FileHelper.getFileContents(file);
    	if (content.contains("Traceback")
    			|| content.contains("Error:")) {
    		return false;
    	} else {
    		return true;
    	}*/
		return true;

	}
	public static String getFileExtension(String sFileName) {
	  int dotIndex = sFileName.lastIndexOf('.');
	  if (dotIndex == -1) {
	    return null;
	  }
	  return sFileName.substring(dotIndex);
	}
 
	//-------------------------------------------------------------------------------------------------

	public static boolean unzip(InputStream inputStream, String dest, boolean replaceIfExists) {
		Log.d(TAG, "unzip:"+dest);
		final int BUFFER_SIZE = 4096;

		BufferedOutputStream bufferedOutputStream = null;

		boolean succeed = true;

		if (replaceIfExists) {
			File file2 = new File(dest);
			if (file2.exists()) {
				try {
					//boolean b = deleteDir(file2);
				} catch (Exception e) {
				}
			}
		}

		try {
			ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
			ZipEntry zipEntry;

			while ((zipEntry = zipInputStream.getNextEntry()) != null){

				String zipEntryName = zipEntry.getName();
				String fs = dest + zipEntryName;

				if (!dest.endsWith("/")) {
					fs = dest;
				}
				//Log.d(TAG, "zipEntryName:"+zipEntryName+"-file2:"+fs+"-"+fs.indexOf('/'));

//		       if(!zipEntry.isDirectory()) {
//		 	       File fil = new File(dest + zipEntryName);
//		 	       fil.getParent()
//		       }

				// file exists ? delete ?
	 	       /*File file2 = new File(fs);
	 	       if(file2.exists()) {
	 		        if (replaceIfExists) {

	 		 	       try {
	 		 	    	  boolean b = deleteDir(file2);
	 		 	    		  if(!b) {
	 		 						Log.e(TAG, "Unzip failed to delete " + dest + zipEntryName);
	 		 	    		  }
	 		 	    		  else {
	 		 						Log.d(TAG, "Unzip deleted " + dest + zipEntryName);
	 		 	    		  }
	 					} catch (Exception e) {
	 						Log.e(TAG, "Unzip failed to delete " + dest + zipEntryName, e);
	 					}
	 		        }
	 	       }*/

				// extract
				File file = new File(fs);

				if (!replaceIfExists && file.exists()){
					Log.d(TAG, "unzip exists");
				} else {
					if(zipEntry.isDirectory()){
						file.mkdirs();
						FileUtils.chmod(file, 0755);

					}else{

						// create parent file folder if not exists yet
						if(!file.getParentFile().exists()) {
							file.getParentFile().mkdirs();
							FileUtils.chmod(file.getParentFile(), 0755);
						}

						byte buffer[] = new byte[BUFFER_SIZE];
						bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
						int count;

						while ((count = zipInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
							bufferedOutputStream.write(buffer, 0, count);
						}

						bufferedOutputStream.flush();
						bufferedOutputStream.close();
					}
				}

				if(file.getName().endsWith(".so")) {
					FileUtils.chmod(file, 0755);
				}

				Log.d(TAG,"Unzip extracted " + dest + zipEntryName);
			}


			zipInputStream.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG,"Unzip error, file not found", e);
			succeed = false;
		}catch (Exception e) {
			Log.e(TAG,"Unzip error: ", e);
			succeed = false;
		}

		return succeed;
	}

	  
	  //-------------------------------------------------------------------------------------------------

	  public static boolean deleteDir(File dir) {
		  try {
		      if (dir.isDirectory()) {
		          String[] children = dir.list();
		          for (int i=0; i<children.length; i++) {
		              boolean success = deleteDir(new File(dir, children[i]));
		              if (!success) {
		                  return false;
		              }
		          }
		      } 
		  
		      // The directory is now empty so delete it
		      return dir.delete();
		      
		} catch (Exception e) {
			Log.e(TAG,"Failed to delete " + dir + " : " + e);
			return false;
		}
	  }
	  
	public static void createDirectoryOnExternalStorage(String path) {
        try {
    		if(Environment.getExternalStorageState().equalsIgnoreCase("mounted")) {
    		    File file = new File(Environment.getExternalStorageDirectory(), path);
    		    if (!file.exists()) {
    		    	try {
    		    		file.mkdirs();

    		    		Log.d(TAG, "createDirectoryOnExternalStorage created " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +path);
    				} catch (Exception e) {
    		            Log.e(TAG,"createDirectoryOnExternalStorage error: ", e);
    				}
    		    }		
    		}
    		else {
                Log.e(TAG,"createDirectoryOnExternalStorage error: " + "External storage is not mounted");		
    		}
		} catch (Exception e) {
            Log.e(TAG,"createDirectoryOnExternalStorage error: " + e);		
		}

	}

	private static final String Util_LOG = makeLogTag(Utils.class);

	public static String makeLogTag(Class<?> cls) {
		return cls.getName();
	}

	public static void showToast(Context context, String str) {
		Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 检查是否存在SD卡
	 *
	 * @return
	 */
	public static boolean hasSdcard() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 创建目录
	 *
	 * @param context
	 * @param dirName
	 *            文件夹名称
	 * @return
	 */
	public static File createFileDir(Context context, String dirName) {
		String filePath;
		// 如SD卡已存在，则存储；反之存在data目录下
		if (hasSdcard()) {
			// SD卡路径
			filePath = Environment.getExternalStorageDirectory()
					+ File.separator + dirName;
		} else {
			filePath = context.getCacheDir().getPath() + File.separator
					+ dirName;
		}
		File destDir = new File(filePath);
		if (!destDir.exists()) {
			boolean isCreate = destDir.mkdirs();
			Log.i(Util_LOG, filePath + " has created. " + isCreate);
		}
		return destDir;
	}

	/**
	 * 删除文件（若为目录，则递归删除子目录和文件）
	 *
	 * @param file
	 * @param delThisPath
	 *            true代表删除参数指定file，false代表保留参数指定file
	 */
	public static void delFile(File file, boolean delThisPath) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] subFiles = file.listFiles();
			if (subFiles != null) {
				int num = subFiles.length;
				// 删除子目录和文件
				for (int i = 0; i < num; i++) {
					delFile(subFiles[i], true);
				}
			}
		}
		if (delThisPath) {
			file.delete();
		}
	}

	/**
	 * 获取文件大小，单位为byte（若为目录，则包括所有子目录和文件）
	 *
	 * @param file
	 * @return
	 */
	public static long getFileSize(File file) {
		long size = 0;
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] subFiles = file.listFiles();
				if (subFiles != null) {
					int num = subFiles.length;
					for (int i = 0; i < num; i++) {
						size += getFileSize(subFiles[i]);
					}
				}
			} else {
				size += file.length();
			}
		}
		return size;
	}

	/**
	 * 保存Bitmap到指定目录
	 *
	 * @param dir
	 *            目录
	 * @param fileName
	 *            文件名
	 * @param bitmap
	 * @throws IOException
	 */
	public static void savaBitmap(File dir, String fileName, Bitmap bitmap) {
		Log.d("Utils", "savaBitmap:"+dir+fileName+"|"+bitmap);
		if (bitmap == null) {
			return;
		}
		File file = new File(dir, fileName);
		try {
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			Log.d("Utils", "savaBitmap IOException:"+e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 判断某目录下文件是否存在
	 *
	 * @param dir
	 *            目录
	 * @param fileName
	 *            文件名
	 * @return
	 */
	public static boolean isFileExists(File dir, String fileName) {
		return new File(dir, fileName).exists();
	}


	public static String getSP(Context context, String key)	{
		String val;
		SharedPreferences obj = context.getSharedPreferences("qpyspf",0);
		val = obj.getString(key,"");
		return val;
	}

	public static boolean isOpenGL2supported(Context context) {

		final ActivityManager activityManager =
				(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo =
				activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
		return supportsEs2;
	}
	public static boolean httpPing(String url, int timeout) {
		//Log.d(TAG, "httpPing:"+url+"-"+timeout);
		url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			//Log.d(TAG, "responseCode:"+responseCode);
			return (responseCode>0);
			//return (200 <= responseCode && responseCode <= 399);
		} catch (IOException exception) {
			Log.d(TAG, "exception:"+exception.getLocalizedMessage());

			return false;
		}
	}
	static public boolean isSrvOk(String srv) {
		try {
			URL u = new URL(srv);
			int port = 80;
			if (u.getPort() != -1) {
				port = u.getPort();
			}
			String url = u.getProtocol() + "://" +u.getHost()+":"+port+"/";
			boolean ret =  httpPing(url, 1000);
			return ret;

		} catch (MalformedURLException e) {
			//Log.d("Bean", "MalformedURLException:"+e);
			return false;
		}
	}


}
