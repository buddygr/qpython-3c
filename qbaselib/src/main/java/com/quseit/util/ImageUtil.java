package com.quseit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.quseit.config.BASE_CONF;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class ImageUtil {
	public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int originWidth  = bitmap.getWidth();
        int originHeight = bitmap.getHeight();

        // no need to resize
        if (originWidth < maxWidth && originHeight < maxHeight) {
            return bitmap;
        }

        int width  = originWidth;
        int height = originHeight;

        // 若图片过宽, 则保持长宽比缩放图片
        if (originWidth > maxWidth) {
            width = maxWidth;

            double i = originWidth * 1.0 / maxWidth;
            height = (int) Math.floor(originHeight / i);

            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        }

        // 若图片过长, 则从上端截取
        if (height > maxHeight) {
            height = maxHeight;
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        }

//        Log.i(TAG, width + " width");
//        Log.i(TAG, height + " height");

        return bitmap;
    }
		/*public static String getImageString(String imgFilePath){
		  Bitmap mBitmap=BitmapFactory.decodeFile(imgFilePath);
		  Matrix matrix = new Matrix();
		  matrix.postScale(0.5f, 0.5f);
		  Bitmap newBitmap=Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
		    
		  ByteArrayOutputStream out=new ByteArrayOutputStream();
		  newBitmap.compress(CompressFormat.JPEG, 100, out);
		  byte []bytes=out.toByteArray();
		  String imageString=Base64.encodeToString(bytes, Base64.DEFAULT);
		  return imageString;
		}*/
	
		public static Bitmap getBitFromImg(String imgFilePath) {
			//try {
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
	            bitmapOptions.inSampleSize = 1;
	    		bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

				Bitmap mBitmap=BitmapFactory.decodeFile(imgFilePath, bitmapOptions);
				return mBitmap;
			/*} catch (OutOfMemoryError e) {
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				
				bitmapOptions.inJustDecodeBounds = true;
				//BitmapFactory.decodeFile(imgFilePath, bitmapOptions);  
				bitmapOptions.inSampleSize = computeSampleSize(bitmapOptions, -1, 128*128);  
				//bitmapOptions.inJustDecodeBounds = false; 
				
	            //bitmapOptions.inSampleSize = 2;
	            
	            try {
					Bitmap mBitmap=BitmapFactory.decodeFile(imgFilePath, bitmapOptions);
					return mBitmap;
	            } catch (OutOfMemoryError E){
		            //bitmapOptions.inSampleSize = 4;
					//Bitmap mBitmap=BitmapFactory.decodeFile(imgFilePath, bitmapOptions);
					return null;
	            } 

			}*/
		}
		
        public static InputStream getRequest(String path) throws Exception {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                if (conn.getResponseCode() == 200){
                	return conn.getInputStream();
                }
                return null;
        }
        

        public static Bitmap getURLAsBitmap(URL url) {
        	try {
        		URLConnection conn = url.openConnection();
        		conn.connect();
        		InputStream isCover = conn.getInputStream();
        		Bitmap bmpCover = BitmapFactory.decodeStream(isCover);
        		isCover.close();
        		return bmpCover;
        	} catch (Exception e) {
        		return null;
        	}
        }
        public static String saveBitmap(String imgHashPath, Bitmap image) {
	    	byte[] bmpb = ImageUtil.Bitmap2Bytes(image);

			try {
	        	File imgCache = new File(imgHashPath);
		    	if (!imgCache.exists()) {
		    		imgCache.createNewFile();
		    	}
	
				RandomAccessFile accessFile = new RandomAccessFile(imgCache.getAbsoluteFile(), "rwd");
				accessFile.setLength(bmpb.length);
				accessFile.seek(0);
				accessFile.write(bmpb, 0, bmpb.length);
				accessFile.close();
				return imgHashPath;
	    	} catch (FileNotFoundException e) {

				e.printStackTrace();
				return "";
			} catch (IOException e) {

				e.printStackTrace();
				return "";
			
        	}
        }

        public static byte[] readInputStream(InputStream inStream) throws Exception {
                ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len = 0;
                while ((len = inStream.read(buffer)) != -1) {
                        outSteam.write(buffer, 0, len);
                }
                outSteam.close();
                inStream.close();
                return outSteam.toByteArray();
        }
        
        public static Drawable loadImageFromUrl(String url){
	        URL m;
	        InputStream i = null;
	        try {
	            m = new URL(url);
	            i = (InputStream) m.getContent();
	        } catch (MalformedURLException e1) {
	            e1.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        Drawable d = Drawable.createFromStream(i, "src");
	        return d;
        }
        
        public static Drawable getDrawableFromUrl(String url) throws Exception{
                 return Drawable.createFromStream(getRequest(url),null);
        }
        
        public static Bitmap getBitmapFromUrl(String url) throws Exception{
                byte[] bytes = getBytesFromUrl(url);
                return byteToBitmap(bytes);
        }
        
        public static Bitmap getRoundBitmapFromUrl(String url,int pixels) throws Exception{
                byte[] bytes = getBytesFromUrl(url);
                Bitmap bitmap = byteToBitmap(bytes);
                return toRoundCorner(bitmap, pixels);
        } 
        
        public static Drawable geRoundDrawableFromUrl(String url,int pixels) throws Exception{
            byte[] bytes = getBytesFromUrl(url);
            BitmapDrawable bitmapDrawable = (BitmapDrawable)byteToDrawable(bytes);
            return toRoundCorner(bitmapDrawable, pixels);
        } 
        
        public static byte[] getBytesFromUrl(String url) throws Exception{
             return readInputStream(getRequest(url));
        }
        
        public static Bitmap byteToBitmap(byte[] byteArray){
	        if(byteArray.length!=0){ 
	            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length); 
	        } 
	        else { 
	            return null; 
	        }  
        }
        
        public static Drawable byteToDrawable(byte[] byteArray){
            ByteArrayInputStream ins = new ByteArrayInputStream(byteArray);
            return Drawable.createFromStream(ins, null);
        }
        
        public static byte[] Bitmap2Bytes(Bitmap bm){ 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        }
        
        public static Bitmap drawableToBitmap(Drawable drawable) {
            Bitmap bitmap = Bitmap
                            .createBitmap(
                                            drawable.getIntrinsicWidth(),
                                            drawable.getIntrinsicHeight(),
                                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                                            : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
        
                 /**
              * 图片去色,返回灰度图片
              * @param bmpOriginal 传入的图片
             * @return 去色后的图片
             */
        public static Bitmap toGrayscale(Bitmap bmpOriginal) {
            int width, height;
            height = bmpOriginal.getHeight();
            width = bmpOriginal.getWidth();    
    
            Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(bmpGrayscale);
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            paint.setColorFilter(f);
            c.drawBitmap(bmpOriginal, 0, 0, paint);
            return bmpGrayscale;
        }
        
        
        /**
         * 去色同时加圆角
         * @param bmpOriginal 原图
         * @param pixels 圆角弧度
         * @return 修改后的图片
         */
        public static Bitmap toGrayscale(Bitmap bmpOriginal, int pixels) {
            return toRoundCorner(toGrayscale(bmpOriginal), pixels);
        }
        
        /**
         * 把图片变成圆角
         * @param bitmap 需要修改的图片
         * @param pixels 圆角的弧度
         * @return 圆角图片
         */
        public static Bitmap toRoundCorner(Bitmap bitmap) {
        	return ImageUtil.toRoundCorner(bitmap, BASE_CONF.ROUND_PIX);
        }
        public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
        	if (pixels == 0) 
        		return bitmap;
        	
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
    
            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);
            final float roundPx = pixels;
    
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
    
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
    
            return output;
        }
    
        
       /**
         * 使圆角功能支持BitampDrawable
         * @param bitmapDrawable 
         * @param pixels 
         * @return
         */
        public static BitmapDrawable toRoundCorner(BitmapDrawable bitmapDrawable, int pixels) {
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmapDrawable = new BitmapDrawable(toRoundCorner(bitmap, pixels));
            return bitmapDrawable;
        }
            
            
        public static int computeSampleSize(BitmapFactory.Options options,  
                int minSideLength, int maxNumOfPixels) {  
            int initialSize = computeInitialSampleSize(options, minSideLength,maxNumOfPixels);  
          
            int roundedSize;  
            if (initialSize <= 8 ) {  
                roundedSize = 1;  
                while (roundedSize < initialSize) {  
                    roundedSize <<= 1;  
                }  
            } else {  
                roundedSize = (initialSize + 7) / 8 * 8;  
            }  
          
            return roundedSize;  
        }  
          
        private static int computeInitialSampleSize(BitmapFactory.Options options,int minSideLength, int maxNumOfPixels) {  
            double w = options.outWidth;  
            double h = options.outHeight;  
          
            int lowerBound = (maxNumOfPixels == -1) ? 1 :  
                    (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));  
            int upperBound = (minSideLength == -1) ? 128 :  
                    (int) Math.min(Math.floor(w / minSideLength),  
                    Math.floor(h / minSideLength));  
          
            if (upperBound < lowerBound) {  
                // return the larger one when there is no overlapping zone.  
                return lowerBound;  
            }  
          
            if ((maxNumOfPixels == -1) &&  
                    (minSideLength == -1)) {  
                return 1;  
            } else if (minSideLength == -1) {  
                return lowerBound;  
            } else {  
                return upperBound;  
            }  
        }

    public static float dp2px(float dp) {
        Resources r = Resources.getSystem();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }
}
