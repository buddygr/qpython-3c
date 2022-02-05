/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.facade;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Contacts.PhonesColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.quseit.base.QBaseActivity;

import org.qpython.qsl4a.qsl4a.MainThread;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.rpc.RpcStartEvent;
import org.qpython.qsl4a.qsl4a.rpc.RpcStopEvent;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Exposes TelephonyManager functionality.
 *
 * @author Damon Kohler (damonkohler@gmail.com)
 * @author Felix Arends (felix.arends@gmail.com)
 */
@SuppressWarnings("deprecation")
public class PhoneFacade extends RpcReceiver {

    private final AndroidFacade mAndroidFacade;
    private final EventFacade mEventFacade;
    private final TelephonyManager mTelephonyManager;
    private final Bundle mPhoneState;
    private final Service mService;
    private PhoneStateListener mPhoneStateListener;

    public PhoneFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelephonyManager = (TelephonyManager) mService.getSystemService(Context.TELEPHONY_SERVICE);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mEventFacade = manager.getReceiver(EventFacade.class);
        mPhoneState = new Bundle();
        mPhoneStateListener = MainThread.run(mService, new Callable<PhoneStateListener>() {
            @Override
            public PhoneStateListener call() throws Exception {
                return new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        mPhoneState.putString("incomingNumber", incomingNumber);
                        switch (state) {
                            case TelephonyManager.CALL_STATE_IDLE:
                                mPhoneState.putString("state", "idle");
                                break;
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                mPhoneState.putString("state", "offhook");
                                break;
                            case TelephonyManager.CALL_STATE_RINGING:
                                mPhoneState.putString("state", "ringing");
                                break;
                        }
                        mEventFacade.postEvent("phone", mPhoneState.clone());
                    }
                };
            }
        });
    }

    @Override
    public void shutdown() {
        stopTrackingPhoneState();
    }

    @Rpc(description = "Starts tracking phone state.")
    @RpcStartEvent("phone")
    public void startTrackingPhoneState() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Rpc(description = "Returns the current phone state and incoming number.", returns = "A Map of \"state\" and \"incomingNumber\"")
    public Bundle readPhoneState() {
        return mPhoneState;
    }

    @Rpc(description = "Stops tracking phone state.")
    @RpcStopEvent("phone")
    public void stopTrackingPhoneState() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Rpc(description = "Calls a contact/phone number by URI.")
    public void phoneCall(@RpcParameter(name = "uri") final String uriString) throws Exception {
        Uri uri = Uri.parse(uriString);
        if (uri.getScheme().equals("content")) {
            String phoneNumberColumn = PhonesColumns.NUMBER;
            String selectWhere = null;
            if ((FacadeManager.class.cast(mManager)).getSdkLevel() >= 5) {
                Class<?> contactsContract_Data_class =
                        Class.forName("android.provider.ContactsContract$Data");
                Field RAW_CONTACT_ID_field = contactsContract_Data_class.getField("RAW_CONTACT_ID");
                selectWhere = RAW_CONTACT_ID_field.get(null).toString() + "=" + uri.getLastPathSegment();
                Field CONTENT_URI_field = contactsContract_Data_class.getField("CONTENT_URI");
                uri = Uri.parse(CONTENT_URI_field.get(null).toString());
                Class<?> ContactsContract_CommonDataKinds_Phone_class =
                        Class.forName("android.provider.ContactsContract$CommonDataKinds$Phone");
                Field NUMBER_field = ContactsContract_CommonDataKinds_Phone_class.getField("NUMBER");
                phoneNumberColumn = NUMBER_field.get(null).toString();
            }
            ContentResolver resolver = mService.getContentResolver();
            Cursor c = resolver.query(uri, new String[]{phoneNumberColumn}, selectWhere, null, null);
            String number = "";
            if (c.moveToFirst()) {
                number = c.getString(c.getColumnIndexOrThrow(phoneNumberColumn));
            }
            c.close();
            phoneCallNumber(number);
        } else {
            mAndroidFacade.startActivity(Intent.ACTION_CALL, uriString, null, null, null, null, null);
        }
    }

    @Rpc(description = "Calls a phone number.")
    public void phoneCallNumber(@RpcParameter(name = "phone number") final String number)
            throws Exception {
        phoneCall("tel:" + URLEncoder.encode(number, "ASCII"));
    }

    @Rpc(description = "Dials a contact/phone number by URI.")
    public void phoneDial(@RpcParameter(name = "uri") final String uri) throws Exception {
        mAndroidFacade.startActivity(Intent.ACTION_DIAL, uri, null, null, null, null, null);
    }

    @Rpc(description = "Dials a phone number.")
    public void phoneDialNumber(@RpcParameter(name = "phone number") final String number)
            throws Exception, UnsupportedEncodingException {
        phoneDial("tel:" + URLEncoder.encode(number, "ASCII"));
    }

    public void check_Access_Fine_Location_Permission() throws Exception {
        if (ActivityCompat.checkSelfPermission(mService, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Rpc(description = "Returns the current cell location.")
    public CellLocation getCellLocation() throws Exception {
        check_Access_Fine_Location_Permission();
        return mTelephonyManager.getCellLocation();
    }

    @Rpc(description = "Returns the numeric name (MCC+MNC) of current registered operator.")
    public String getNetworkOperator() {
        return mTelephonyManager.getNetworkOperator();
    }

    @Rpc(description = "Returns the alphabetic name of current registered operator.")
    public String getNetworkOperatorName() {
        return mTelephonyManager.getNetworkOperatorName();
    }

    public void check_Read_Phone_State_Permission() throws Exception {
        if (ActivityCompat.checkSelfPermission(mService, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception(Manifest.permission.READ_PHONE_STATE);
        }
    }


    @Rpc(description = "Returns a the radio technology (network type) currently in use on the device.")
    public String getNetworkType() throws Exception {
        // TODO(damonkohler): API level 5 has many more types.
        check_Read_Phone_State_Permission();
        int networkType;
        //if (Build.VERSION.SDK_INT >= 24) {
            networkType = mTelephonyManager.getDataNetworkType();
        //} else {
        //    networkType = mTelephonyManager.getNetworkType();
        //}
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "WIFI";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "Unknown";
            default:
                return "Other";
        }
    }

    @Rpc(description = "Returns the device phone type.")
    public String getPhoneType() {
        // TODO(damonkohler): API level 4 includes CDMA.
        switch (mTelephonyManager.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.PHONE_TYPE_GSM:
                return "GSM";
            case TelephonyManager.PHONE_TYPE_SIP:
                return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE:
                return "NONE";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the ISO country code equivalent for the SIM provider's country code.")
    public String getSimCountryIso() {
        return mTelephonyManager.getSimCountryIso();
    }

    @Rpc(description = "Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM. 5 or 6 decimal digits.")
    public String getSimOperator() {
        return mTelephonyManager.getSimOperator();
    }

    @Rpc(description = "Returns the Service Provider Name (SPN).")
    public String getSimOperatorName() {
        return mTelephonyManager.getSimOperatorName();
    }

    @Rpc(description = "Returns the serial number of the SIM, if applicable. Return null if it is unavailable.")
    public String getSimSerialNumber() {
        return mTelephonyManager.getSimSerialNumber();
    }

    @Rpc(description = "Returns the state of the device SIM card.")
    public String getSimState() {
        switch (mTelephonyManager.getSimState()) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "uknown";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "pin_required";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "puk_required";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "network_locked";
            case TelephonyManager.SIM_STATE_READY:
                return "ready";
            default:
                return null;
        }
    }

    @Rpc(description = "Returns the unique subscriber ID, for example, the IMSI for a GSM phone. Return null if it is unavailable.")
    public String getSubscriberId() {
        return mTelephonyManager.getSubscriberId();
    }

    @Rpc(description = "Retrieves the alphabetic identifier associated with the voice mail number.")
    public String getVoiceMailAlphaTag() throws Exception {
        check_Read_Phone_State_Permission();
        return mTelephonyManager.getVoiceMailAlphaTag();
    }

    @Rpc(description = "Returns the voice mail number. Return null if it is unavailable.")
    public String getVoiceMailNumber() throws Exception {
        check_Read_Phone_State_Permission();
        return mTelephonyManager.getVoiceMailNumber();
    }

    @Rpc(description = "Returns true if the device is considered roaming on the current network, for GSM purposes.")
    public Boolean checkNetworkRoaming() {
        return mTelephonyManager.isNetworkRoaming();
    }

    @Rpc(description = "Returns the unique device ID, for example, the IMEI for GSM and the MEID for CDMA phones. Return null if device ID is not available.")
    public String getDeviceId(
            @RpcParameter(name = "index", description = "index parameter need Android >= 6.0 .") @RpcOptional Integer index
    ) throws Exception {
        check_Read_Phone_State_Permission();
        //if (index != null && Build.VERSION.SDK_INT >=23) {
            return mTelephonyManager.getDeviceId(index);
        //} else {
        //    return mTelephonyManager.getDeviceId();
        //}
    }

    @Rpc(description = "MEID for CDMA phones, Need Android >= 8.0 .")
    public String getMeid(
            @RpcParameter(name = "index") @RpcOptional Integer index
    ) throws Exception {
        check_Read_Phone_State_Permission();
        if (Build.VERSION.SDK_INT >=26) {
            if (index == null){
                return mTelephonyManager.getMeid();
            } else {
                return mTelephonyManager.getMeid(index);
            }
        } else {
            throw new Exception("getMeid need Android >= 8.0 .");
        }
    }

    @Rpc(description = "IMEI for GSM phones, Need Android >= 8.0 .")
    public String getImei(
            @RpcParameter(name = "index") @RpcOptional Integer index
    ) throws Exception {
        check_Read_Phone_State_Permission();
        if (Build.VERSION.SDK_INT >=26) {
            if (index == null){
                return mTelephonyManager.getImei();
            } else {
                return mTelephonyManager.getImei(index);
            }
        } else {
            throw new Exception("getImei need Android >= 8.0 .");
        }
    }

    @Rpc(description = "Returns the software version number for the device, for example, the IMEI/SV for GSM phones. Return null if the software version is not available.")
    public String getDeviceSoftwareVersion() throws Exception {
        check_Read_Phone_State_Permission();
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    @Rpc(description = "Returns the phone number string for line 1, for example, the MSISDN for a GSM phone. Return null if it is unavailable.")
    public String getLine1Number() throws Exception {
        if (ActivityCompat.checkSelfPermission(mService, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mService, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mService, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception(Manifest.permission.READ_SMS + "," + Manifest.permission.READ_PHONE_NUMBERS + "," + Manifest.permission.READ_PHONE_STATE);
        }
        return mTelephonyManager.getLine1Number();
    }

    @Rpc(description = "Returns the neighboring cell information of the device.")
    public List<CellInfo> getNeighboringCellInfo() throws Exception {
        check_Access_Fine_Location_Permission();
        return mTelephonyManager.getAllCellInfo();
    }



    /*@Rpc(description = "Mobile data traffic flow , need Android >= 8.0")
    public void setDataEnabled(
            @RpcParameter(name = "state") @RpcOptional Boolean state) throws Exception {
        if (Build.VERSION.SDK_INT>=26) {
            if (ActivityCompat.checkSelfPermission(mService, Manifest.permission.MODIFY_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                throw new Exception(Manifest.permission.MODIFY_PHONE_STATE);
            }
            mTelephonyManager.setDataEnabled(state);
        } else {
            throw new Exception("setDataEnabled need Android >= 8.0 .");
        }

    }*/
}
