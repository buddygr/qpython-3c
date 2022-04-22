package org.qpython.qsl4a.qsl4a.facade;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import org.qpython.qsl4a.qsl4a.MainThread;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcStartEvent;
import org.qpython.qsl4a.qsl4a.rpc.RpcStopEvent;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Exposes SignalStrength functionality.
 *
 * @author Joerg Zieren (joerg.zieren@gmail.com)
 * 乘着船 修改 2022
 */

public class SignalStrengthFacade extends RpcReceiver {
  private final Service mService;
  private final TelephonyManager mTelephonyManager;
  private final EventFacade mEventFacade;
  private final PhoneStateListener mPhoneStateListener;
  private Bundle mSignalStrengths;

  public SignalStrengthFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mEventFacade = manager.getReceiver(EventFacade.class);
    mTelephonyManager =
            (TelephonyManager) manager.getService().getSystemService(Context.TELEPHONY_SERVICE);
    //旧版信号强度获取方式
    mPhoneStateListener = MainThread.run(mService, (Callable<PhoneStateListener>) () -> new PhoneStateListener() {
      @Override
      public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        mSignalStrengths = new Bundle();
        mSignalStrengths.putInt("gsm_signal_strength", signalStrength.getGsmSignalStrength());
        mSignalStrengths.putInt("gsm_bit_error_rate", signalStrength.getGsmBitErrorRate());
        mSignalStrengths.putInt("cdma_dbm", signalStrength.getCdmaDbm());
        mSignalStrengths.putInt("cdma_ecio", signalStrength.getCdmaEcio());
        mSignalStrengths.putInt("evdo_dbm", signalStrength.getEvdoDbm());
        mSignalStrengths.putInt("evdo_ecio", signalStrength.getEvdoEcio());
        mSignalStrengths.putInt("evdo_snr", signalStrength.getEvdoSnr());
        Method method;
        int dbm,Dbm=Integer.MAX_VALUE;
        try {
          method = signalStrength.getClass().getMethod("getDbm");
          Dbm = (int) method.invoke(signalStrength);
          mSignalStrengths.putInt("dbm", Dbm);
        } catch (Exception ignored){}
        String[] flags = {"Rsrp","Rsrq","Rssnr"};
        for (String flag : flags){
        try {
          method = signalStrength.getClass().getDeclaredMethod("getLte"+flag);
          method.setAccessible(true);
          dbm = (int) method.invoke(signalStrength);
          mSignalStrengths.putInt("lte_"+flag.toLowerCase(),dbm);
        } catch (Exception ignored){}}
          try {
            method = signalStrength.getClass().getDeclaredMethod("getWcdmaEcio");
            method.setAccessible(true);
            dbm = (int) method.invoke(signalStrength);
            if (dbm<0) mSignalStrengths.putInt("wcdma_rscp",Dbm);
            else mSignalStrengths.putInt("wcdma_rscp",Integer.MAX_VALUE);
            mSignalStrengths.putInt("wcdma_ecio",dbm);
          } catch (Exception ignored){}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
          mSignalStrengths.putInt("level", signalStrength.getLevel());
        mEventFacade.postEvent("signal_strengths", mSignalStrengths.clone());
      }
    });
  }

  @Rpc(description = "Returns the Telephone Signal Strength Level .")
  public int getTelephoneSignalStrengthLevel() throws Exception {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    return mTelephonyManager.getSignalStrength().getLevel();
    } else throw new Exception("getTelephoneSignalStrengthLevel only support Android >= 9.0");
  }

  @Rpc(description = "Returns the Telephone Signal Strength Detail .")
  public String getTelephoneSignalStrengthDetail() throws Exception {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      return mTelephonyManager.getSignalStrength().toString();
    } else throw new Exception("getTelephoneSignalStrengthDetail only support Android >= 9.0");
  }

@Rpc(description = "Starts tracking signal strengths. GSM/CDMA/EVDO")
  @RpcStartEvent("signal_strengths")
  public void startTrackingSignalStrengths() {
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
  }

  @Rpc(description = "Returns the current signal strengths. GSM/CDMA/EVDO", returns = "A map of \"gsm_signal_strength\"")
  public Bundle readSignalStrengths() {
    return mSignalStrengths;
  }

  @Rpc(description = "Stops tracking signal strength. GSM/CDMA/EVDO")
  @RpcStopEvent("signal_strengths")
  public void stopTrackingSignalStrengths() {
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
  }

  @Override
  public void shutdown() {
    stopTrackingSignalStrengths();
  }
}
