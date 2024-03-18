package org.qpython.qsl4a;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.qpython.qsl4a.qsl4a.AndroidProxy;
import org.qpython.qsl4a.qsl4a.util.SPFUtils;

import java.util.concurrent.CountDownLatch;

public class QPyScriptService extends Service {
    private static final String TAG = "QPyScriptService";
    //public static String scriptName;

    //private final static int NOTIFICATION_ID = NotificationIdFactory.create();
    private final CountDownLatch mLatch = new CountDownLatch(1);
    private final IBinder mBinder;
    @SuppressWarnings("unused")
    private       boolean killMe;
    //private InterpreterConfiguration mInterpreterConfiguration = null;
    //private RpcReceiverManager mFacadeManager;
    private AndroidProxy       mProxy;
    @SuppressWarnings("unused")
    private int                mStartId;

    // ------------------------------------------------------------------------------------------------------

    public QPyScriptService() {
//		super(NOTIFICATION_ID);
        super();
        mBinder = new LocalBinder();
    }
    // ------------------------------------------------------------------------------------------------------

    /*public static String getSP(Context context, String key) {
        String val;
        SharedPreferences obj = context.getSharedPreferences("qsl4a_db", 0);
        val = obj.getString(key, "");
        return val;
    }

    // ------------------------------------------------------------------------------------------------------

    public static void setSP(Context context, String key, String val) {
        SharedPreferences obj = context.getSharedPreferences("qsl4a_db", 0);
        SharedPreferences.Editor wobj;
        wobj = obj.edit();
        wobj.putString(key, val);
        wobj.commit();
    }*/

    // ------------------------------------------------------------------------------------------------------

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mProxy != null) {
            mProxy.shutdown();
        }

//        Intent intent = new Intent("org.qpython.qpysl4a.KeepAlive");
//        sendBroadcast(intent);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // ------------------------------------------------------------------------------------------------------

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
         return START_STICKY;
    }

    // ------------------------------------------------------------------------------------------------------

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        // clear before run
        /*File logFile = new File( Environment.getExternalStorageDirectory()+"/"+CONF.BASE_PATH+"/"+scriptName.substring(scriptName.lastIndexOf("/")+1)+".log" );
        if (logFile.exists()) {
        	logFile.delete();
        }
		killProcess();	*/
        // River Modify
        this.killMe = false;

        new startMyAsyncTask().execute(101);
    }

    // ------------------------------------------------------------------------------------------------------

    private void startMyMain() {
        try {
            mProxy = new AndroidProxy(this, null);

            mProxy.startLocal();
            SPFUtils.setSP(getApplicationContext(), "sl4a.hostname", mProxy.getAddress().getHostName());
            SPFUtils.setSP(getApplicationContext(), "sl4a.port", "" + mProxy.getAddress().getPort());
            SPFUtils.setSP(getApplicationContext(), "sl4a.secue", mProxy.getSecret());

            //Log.d(TAG, "startMyMain:" + mProxy.getAddress().getHostName() + ":" + mProxy.getAddress().getPort() + ":" + mProxy.getSecret());
            mLatch.countDown();
        } catch (Exception ignored) {

        }
    }

    /*RpcReceiverManager getRpcReceiverManager() throws InterruptedException {
        mLatch.await();

        if (mFacadeManager == null) { // Facade manage may not be available on startup.
            mFacadeManager = mProxy.getRpcReceiverManagerFactory()
                    .getRpcReceiverManagers().get(0);
        }
        return mFacadeManager;
    }*/

    public class LocalBinder extends Binder {
        public QPyScriptService getService() {
            return QPyScriptService.this;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class startMyAsyncTask extends AsyncTask<Integer, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            Log.d(TAG, "doInBackground");
            startMyMain();
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        @Override
        protected void onPostExecute(Boolean installStatus) {
            Log.d(TAG, "startMyAsyncTask:onPostExecute");
        }
    }

    public static void startToast(Context context){
        start(context);
        Toast.makeText(context, R.string.sl4a_start, Toast.LENGTH_SHORT).show();
    }

    public static void start(Context context){
        context.startService(new Intent(context,QPyScriptService.class));
    }

    public static void stop(Context context){
        context.stopService(new Intent(context,QPyScriptService.class));
    }

}