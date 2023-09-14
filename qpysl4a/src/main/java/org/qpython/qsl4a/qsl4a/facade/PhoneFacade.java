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
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Contacts.PhonesColumns;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.qpython.qsl4a.qsl4a.MainThread;
import org.qpython.qsl4a.qsl4a.jsonrpc.RpcReceiver;
import org.qpython.qsl4a.qsl4a.rpc.Rpc;
import org.qpython.qsl4a.qsl4a.rpc.RpcDefault;
import org.qpython.qsl4a.qsl4a.rpc.RpcOptional;
import org.qpython.qsl4a.qsl4a.rpc.RpcParameter;
import org.qpython.qsl4a.qsl4a.rpc.RpcStartEvent;
import org.qpython.qsl4a.qsl4a.rpc.RpcStopEvent;
import org.qpython.qsl4a.qsl4a.util.PermissionUtil;

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
 * 乘着船 修改 2021-2022
 */
@SuppressWarnings("deprecation")
public class PhoneFacade extends RpcReceiver {

    private final AndroidFacade mAndroidFacade;
    private final EventFacade mEventFacade;
    private final TelephonyManager mTelephonyManager;
    private final Bundle mPhoneState;
    private final Service mService;
    private final PhoneStateListener mPhoneStateListener;
    private final Context context;

    public PhoneFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mTelephonyManager = (TelephonyManager) mService.getSystemService(Context.TELEPHONY_SERVICE);
        mAndroidFacade = manager.getReceiver(AndroidFacade.class);
        mEventFacade = manager.getReceiver(EventFacade.class);
        context = mAndroidFacade.context;
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
        PermissionUtil.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void getCellLocation(CellInfo cellInfo,JSONObject result,boolean complex) throws Exception {
        if(complex)
            result.put("Registered",cellInfo.isRegistered());
        if (Build.VERSION.SDK_INT >= 29 && cellInfo instanceof CellInfoNr){
            CellIdentityNr nrCellInfoIdetity = (CellIdentityNr) ((CellInfoNr) cellInfo).getCellIdentity();
            result.put("Nci",nrCellInfoIdetity.getNci());
            result.put("Pci",nrCellInfoIdetity.getPci());
            result.put("Tac",nrCellInfoIdetity.getTac());
            CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr) cellInfo).getCellSignalStrength();
            result.put("NrDbm",cellSignalStrengthNr.getDbm());
            if (complex) {
                result.put("Mcc,Mnc",nrCellInfoIdetity.getMccString()+","+nrCellInfoIdetity.getMncString());
                result.put("NrArfcn",nrCellInfoIdetity.getNrarfcn());
                if (Build.VERSION.SDK_INT >= 30) {
                    int[] bands = nrCellInfoIdetity.getBands();
                    JSONArray Bands = new JSONArray();
                    for (int band : bands)
                        Bands.put(band);
                    result.put("Bands", Bands);
                }
            } else result.put("Operator",nrCellInfoIdetity.getOperatorAlphaShort());
        }
        else if (cellInfo instanceof CellInfoLte){
            CellIdentityLte lteCellInfoIdetity = ((CellInfoLte) cellInfo).getCellIdentity();
            result.put("Ci", lteCellInfoIdetity.getCi());
            result.put("Pci",lteCellInfoIdetity.getPci());
            result.put("Tac",lteCellInfoIdetity.getTac());
            CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
            result.put("LteDbm",cellSignalStrengthLte.getDbm());
            if (complex) {
                result.put("Mcc,Mnc",lteCellInfoIdetity.getMcc()+","+lteCellInfoIdetity.getMnc());
                result.put("Earfcn",lteCellInfoIdetity.getEarfcn());
                //result.put("Bandwidth",lteCellInfoIdetity.getBandwidth()); //总是2147483647
                if (Build.VERSION.SDK_INT >= 30) {
                    int[] bands = lteCellInfoIdetity.getBands();
                    JSONArray Bands = new JSONArray();
                    for (int band : bands)
                        Bands.put(band);
                    result.put("Bands", Bands);
                }
            } else if(Build.VERSION.SDK_INT >= 28)
                result.put("Operator",lteCellInfoIdetity.getMobileNetworkOperator());
        }
        else if (cellInfo instanceof CellInfoWcdma){
            CellIdentityWcdma wcdmaCellInfoIdetity = ((CellInfoWcdma) cellInfo).getCellIdentity();
            result.put("Lac",wcdmaCellInfoIdetity.getLac());
            result.put("Cid",wcdmaCellInfoIdetity.getCid());
            result.put("Psc",wcdmaCellInfoIdetity.getPsc());
            CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
            result.put("WcdmaDbm",cellSignalStrengthWcdma.getDbm());
            if (complex) {
                result.put("Mcc,Mnc",wcdmaCellInfoIdetity.getMcc()+","+wcdmaCellInfoIdetity.getMnc());
                result.put("Uarfcn",wcdmaCellInfoIdetity.getUarfcn());
            }
        }
        else if (cellInfo instanceof CellInfoCdma){
            CellIdentityCdma cdmaCellInfoIdetity = ((CellInfoCdma) cellInfo).getCellIdentity();
            result.put("Sid",cdmaCellInfoIdetity.getSystemId());
            result.put("Nid",cdmaCellInfoIdetity.getNetworkId());
            result.put("Bid",cdmaCellInfoIdetity.getBasestationId());
            CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cellInfo).getCellSignalStrength();
            result.put("CdmaDbm",cellSignalStrengthCdma.getDbm());
            if(complex){
                result.put("Lat,Long",tudeIntToStr(cdmaCellInfoIdetity.getLatitude())+","+tudeIntToStr(cdmaCellInfoIdetity.getLongitude()));
            }
        }
        else if (cellInfo instanceof CellInfoGsm){
            CellIdentityGsm gsmCellInfoIdetity = ((CellInfoGsm) cellInfo).getCellIdentity();
            result.put("Lac",gsmCellInfoIdetity.getLac());
            result.put("Cid",gsmCellInfoIdetity.getCid());
            CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
            result.put("GsmDbm",cellSignalStrengthGsm.getDbm());
            if (complex) {
                result.put("Mcc,Mnc",gsmCellInfoIdetity.getMcc()+","+gsmCellInfoIdetity.getMnc());
                result.put("Arfcn",gsmCellInfoIdetity.getArfcn());
            }
        }
    }

    @Rpc(description = "Returns the current cell location.")
    public JSONObject getCellLocation() throws Exception {
        check_Access_Fine_Location_Permission();
        //return mTelephonyManager.getCellLocation().toString();
        JSONObject result = new JSONObject();
        //CellLocation cellLocation = mTelephonyManager.getCellLocation();
        try{
        List<CellInfo> cellInfos = mTelephonyManager.getAllCellInfo();
        result.put("CellCount",cellInfos.size());
        byte n = 1;
        for (CellInfo cellInfo : cellInfos) {
            if(!cellInfo.isRegistered()) continue;
            JSONObject cell = new JSONObject();
            getCellLocation(cellInfo,cell,false);
            result.put("Registered"+n,cell);
            n+=1;
        }
        return result;
       } catch (Exception e){
           throw new Exception("getCellLocation can't get any information because of No Location Permission or Completely No Signal .");
       }
    }

    @Rpc(description = "Returns all the cells location.")
    public JSONArray getAllCellsLocation() throws Exception {
        check_Access_Fine_Location_Permission();
        try{
        List<CellInfo> cellInfos = mTelephonyManager.getAllCellInfo();
        JSONArray Result = new JSONArray();
        for (CellInfo cellInfo : cellInfos) {
            JSONObject result = new JSONObject();
            getCellLocation(cellInfo,result,true);
            Result.put(result);
        }
        return Result;
    } catch (Exception e){
        throw new Exception("getAllCellsLocation can't get any information because of No Location Permission or Completely No Signal .");
    }}

    @Rpc(description = "Returns the numeric name (MCC+MNC) of current registered operator.")
    public String getNetworkOperator() {
        return mTelephonyManager.getNetworkOperator();
    }

    @Rpc(description = "Returns the alphabetic name of current registered operator.")
    public String getNetworkOperatorName() {
        return mTelephonyManager.getNetworkOperatorName();
    }

    public void check_Read_Phone_State_Permission() throws Exception {
        PermissionUtil.checkPermission(Manifest.permission.READ_PHONE_STATE);
        }

    @SuppressLint("NewApi")
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
            //case TelephonyManager.NETWORK_TYPE_IWLAN:
                //return "WIFI";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "3G HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G HSPA+";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "3G UMTS";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "3G Other";
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "2G GSM";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "2G CDMA";
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

    @SuppressLint("HardwareIds")
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

    @Rpc(description = "get Android ID")
    public String getAndroidID() throws Exception {
        return Settings.System.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @SuppressLint("NewApi")
    @Rpc(description = "Returns the unique device ID, for example, the IMEI for GSM and the MEID for CDMA phones. Return null if device ID is not available.")
    public String getDeviceId(
            @RpcParameter(name = "index", description = "index parameter need Android >= 6.0 .") @RpcDefault("0") Integer index
    ) throws Exception {
        if (index == null) return mTelephonyManager.getDeviceId();
        else return mTelephonyManager.getDeviceId(index);
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
            return getDeviceId(index);
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
            return getDeviceId(index);
        }
    }

    @Rpc(description = "Returns the software version number for the device, for example, the IMEI/SV for GSM phones. Return null if the software version is not available.")
    public String getDeviceSoftwareVersion() throws Exception {
        check_Read_Phone_State_Permission();
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    @SuppressLint("HardwareIds")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Rpc(description = "Returns the phone number string for line 1, for example, the MSISDN for a GSM phone. Return null if it is unavailable.")
    public String getLine1Number() throws Exception {
        PermissionUtil.checkPermission(new String[]{Manifest.permission.READ_SMS,Manifest.permission.READ_PHONE_NUMBERS,Manifest.permission.READ_PHONE_STATE});
        return mTelephonyManager.getLine1Number();
    }

    @Rpc(description = "Returns all the neighboring cell information of the device.")
    public List<CellInfo> getAllCellInfo() throws Exception {
        check_Access_Fine_Location_Permission();
        return mTelephonyManager.getAllCellInfo();
    }

    private String tudeIntToStr(int x){
        return String.valueOf(Math.round((double) x/0.144D)/100000D);
    }
    
}
