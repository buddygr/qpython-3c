package com.quseit.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDownLoader {
    private static final String ImageDownLoader_Log = Utils
            .makeLogTag(ImageDownLoader.class);
    private static final String DIR_CACHE                 = "imagecache";
    private static final long   DIR_CACHE_LIMIT           = 10 * 1024 * 1024;
    private static final int    IMAGE_DOWNLOAD_FAIL_TIMES = 2;
    private Hashtable<String, Integer> taskCollection;
    private LruCache<String, Bitmap>   lruCache;
    private ExecutorService            threadPool;
    private File                       cacheFileDir;

    @SuppressLint("NewApi")
    public ImageDownLoader(Context context) {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        lruCache = new LruCache<String, Bitmap>(maxMemory / 8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        taskCollection = new Hashtable<String, Integer>();
        threadPool = Executors.newFixedThreadPool(10);
        cacheFileDir = Utils.createFileDir(context, DIR_CACHE);
    }

    public static void setImageFromUrl(Context context, final ImageView imageView, String url) {
        ImageDownLoader loader = new ImageDownLoader(context);
        Bitmap bitmap = loader.getBitmapCache(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (loader.getTaskCollection().containsKey(url)) {
                return;
            }
            loader.loadImage(url, imageView.getWidth(), imageView.getHeight(), new AsyncImageLoaderListener() {
                @Override
                public void onImageLoader(Bitmap bitmap) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            });
        }

    }

    public static void setBlurImageFromUrl(Context context, final ImageView imageView, String url) {
        ImageDownLoader loader = new ImageDownLoader(context);
        Bitmap bitmap = loader.getBitmapCache(url);
        if (bitmap != null) {
            bitmap = Blur.apply(context, bitmap,1);
            imageView.setImageBitmap(bitmap);
        } else {
            if (loader.getTaskCollection().containsKey(url)) {
                return;
            }
            loader.loadImage(url, imageView.getWidth(), imageView.getHeight(), new AsyncImageLoaderListener() {
                @Override
                public void onImageLoader(Bitmap bitmap) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    @SuppressLint("NewApi")
    private void addLruCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            lruCache.put(key, bitmap);
        }
    }

    @SuppressLint("NewApi")
    private Bitmap getBitmapFromMemCache(String key) {
        return lruCache.get(key);
    }

    public void loadImage(final String url, final int width, final int height,
                          AsyncImageLoaderListener listener) {
        Log.i(ImageDownLoader_Log, "loadImage:" + url);
        final ImageHandler handler = new ImageHandler(listener);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //Log.i(ImageDownLoader_Log, "loadImage run:" + url);
                Bitmap bitmap = downloadImage(url, width, height);
                Message msg = handler.obtainMessage();
                msg.obj = bitmap;
                handler.sendMessage(msg);
                addLruCache(url, bitmap);
                long cacheFileSize = Utils.getFileSize(cacheFileDir);
                if (cacheFileSize > DIR_CACHE_LIMIT) {
                    Log.i(ImageDownLoader_Log, cacheFileDir
                            + " size has exceed limit." + cacheFileSize);
                    Utils.delFile(cacheFileDir, false);
                    taskCollection.clear();
                }
                String urlKey = url.replaceAll("[^\\w]", "");
                Utils.savaBitmap(cacheFileDir, urlKey, bitmap);
            }
        };
        taskCollection.put(url, 0);
        threadPool.execute(runnable);
    }

    public Bitmap getBitmapCache(String url) {
        String urlKey = url.replaceAll("[^\\w]", "");
        if (getBitmapFromMemCache(url) != null) {
            return getBitmapFromMemCache(url);
        } else if (Utils.isFileExists(cacheFileDir, urlKey)
                && Utils.getFileSize(new File(cacheFileDir, urlKey)) > 0) {
            Bitmap bitmap = BitmapFactory.decodeFile(cacheFileDir.getPath()
                    + File.separator + urlKey);
            addLruCache(url, bitmap);
            return bitmap;
        }
        return null;
    }

    private Bitmap downloadImage(String url, int width, int height) {
        Bitmap bitmap = null;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getParams().setParameter(
                    CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();
                byte[] byteIn = EntityUtils.toByteArray(entity);
                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
                bmpFactoryOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(byteIn, 0, byteIn.length,
                        bmpFactoryOptions);

                Log.d("ImageDownLoader", "downloadImage:" + bmpFactoryOptions.outHeight + "|" + height + "|" + bmpFactoryOptions.outWidth + "|" + width);

                bmpFactoryOptions.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeByteArray(byteIn, 0,
                        byteIn.length, bmpFactoryOptions);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpClient != null && httpClient.getConnectionManager() != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }

        if (taskCollection.get(url) != null) {
            int times = taskCollection.get(url);
            if (bitmap == null
                    && times < IMAGE_DOWNLOAD_FAIL_TIMES) {
                times++;
                taskCollection.put(url, times);
                bitmap = downloadImage(url, width, height);
                Log.i(ImageDownLoader_Log, "Re-download " + url + ":" + times);
            }
        }
        return bitmap;
    }

    public synchronized void cancelTasks() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    public Hashtable<String, Integer> getTaskCollection() {
        return taskCollection;
    }

    public interface AsyncImageLoaderListener {
        void onImageLoader(Bitmap bitmap);
    }

    static class ImageHandler extends Handler {

        private AsyncImageLoaderListener listener;

        public ImageHandler(AsyncImageLoaderListener listener) {
            this.listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            listener.onImageLoader((Bitmap) msg.obj);
        }
    }
}