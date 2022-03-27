package org.qpython.qsl4a.qsl4a.facade;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.MainThread;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.rpc.RpcStartEvent;
import org.qpython.qsl4a.qsl4a.rpc.RpcStopEvent;

import java.util.List;
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
    mPhoneStateListener = MainThread.run(mService, new Callable<PhoneStateListener>() {
      @Override
      public PhoneStateListener call() throws Exception {
        return new PhoneStateListener() {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
              mSignalStrengths.putInt("level", signalStrength.getLevel());
            mEventFacade.postEvent("signal_strengths", mSignalStrengths.clone());
          }
        };
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
