package com.quseit.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.webkit.WebView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.quseit.android.R;
import com.quseit.config.BASE_CONF;
import com.quseit.common.db.CacheLog;
import com.quseit.common.db.UserLog;
import com.quseit.util.DateTimeHelper;
import com.quseit.util.NAction;
import com.quseit.util.NUtil;
import com.quseit.util.Utils;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

public abstract class QBaseActivity extends Activity {
	protected static final String TAG = "QBaseActivity";


	protected int limit = BASE_CONF.PAGE_NUM;

	protected int start = 0;

	protected int total = 0;

	protected ProgressDialog waitingWindow;

	protected QBaseDialog WBase;

	protected int dialogIndex;

	public WebView wv;

	private ProgressDialog pDialog;


	public void progress(String title, String msg, int x) {
		pDialog = ProgressDialog.show(this, title, msg, true, false);
		if (x != 0) {
			pDialog.setContentView(x);
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WBase = new QBaseDialog(this, this);
		dialogIndex = 1;
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public Dialog onCreateDialog(int id) {
		return WBase.onCreateDialog(id);
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void openWaitWindow() {
		Log.d(TAG, "openWaitWindow");
		if (waitingWindow == null) {
			waitingWindow = NUtil.progressWindow(this, R.string.wating_title);
		}
		else {
			waitingWindow.cancel();
			waitingWindow = NUtil.progressWindow(this, R.string.wating_title);
		}
		try {
			waitingWindow.show();
		}
		catch (BadTokenException e) {
			Log.d(TAG, "openWaitWindow: e:" + e.getLocalizedMessage());

		}
	}
	
	public void closeWaitWindow() {
		if (waitingWindow != null) {
			try {
				if (waitingWindow.isShowing()) {
					waitingWindow.dismiss();
				}
			}
			catch (Exception e) {

			}
		}
	}

	public void onBack(View v) {
		finish();
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void checkUpdate(final Context context, final boolean auto) {
		String upVer = NAction.getExtP(this, "conf_update_ver");

		if (upVer.equals(""))
			upVer = "0";
		int upVerNum = Integer.parseInt(upVer);
		// Log.d(TAG, "vercheck:"+upVerNum+"-"+BASE_CONF.UPDATE_VER);
		if (upVerNum < BASE_CONF.UPDATE_VER) {

			NAction.setUpdateHost(this, "");
		}

		if (NUtil.netCheckin(getApplicationContext())) {
			// clear db cache
			CacheLog cDB = new CacheLog(context);
			cDB.cleanCache();

			if (!auto)
				openWaitWindow();
			RequestParams myParam = new RequestParams();
			String types = NAction.getExtP(context, "conf_get_log_types");
			String limit = NAction.getExtP(context, "conf_get_log_limit");
			if (limit.equals("")) {
				limit = BASE_CONF.LOG_LIMIT;
			}
			UserLog pq = new UserLog(getApplicationContext());
			String xlogs;
			try {
				xlogs = pq.getLogs(types, 0, limit, "ASC");
			}
			catch (OutOfMemoryError e) {
				Log.d(TAG, "err when getLogs:" + e.getMessage());
				xlogs = "";
				pq.deleteAllStat_0_Log();
			}
			final String logs = xlogs;

			myParam.put("time", DateTimeHelper.getDateMin());
			myParam.put("logs", logs);

			// Log.d(TAG, "up logs:"+logs);

			/* 手机客户信息 */
			String collectInfos = NAction.getExtP(context, "conf_get_log_cls");
			if (collectInfos.equals("")) {
				collectInfos = BASE_CONF.COLLECT_INFO;
			}
			Field[] fields = Build.class.getDeclaredFields();
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					String k = field.getName();
					if (k != null) {
						k = k.toLowerCase().trim();
					}
					if (collectInfos.contains("#" + k + "#")) {
						myParam.put(k, field.get(null).toString());
					}
					// Log.d(TAG, field.getName() + " : " + field.get(null));

				}
				catch (Exception e) {
					Log.e(TAG, "an error occured when collect crash info", e);
				}
			}

			if (BASE_CONF.DEBUG)
				Log.d(TAG, "checkUpdate:" + logs);
			String updateUrl = NAction.getUpdateHost(getApplicationContext());
			if (updateUrl.equals("")) {
				updateUrl = confGetUpdateURL(1);
			}
			if (!BASE_CONF.DEBUG)
				Log.d(TAG, "checkUpdate:" + updateUrl + "?" + NAction.getUserUrl(getApplicationContext()));

			QBaseApp.getInstance().getAsyncHttpClient().post(updateUrl + "?" + NAction.getUserUrl(getApplicationContext()),
				myParam,
				new JsonHttpResponseHandler() {
			    @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject result) {
					if (!auto)
						closeWaitWindow();
					if (BASE_CONF.DEBUG)
						Log.d(TAG, "checkUpdate-result:" + result.toString());

					// if (!logs.equals("")) {
					UserLog pq = new UserLog(context);
					pq.deleteAllStat_0_Log();
					// }

					int localVersion = NUtil.getVersinoCode(context);

					try {
						List<String> ks = Utils.copyIterator(result.keys());
						String k;
						if (ks.size() == 1) {
							k = ks.get(0);
						}
						else {
							k = NAction.getCode(context);
						}
						JSONObject info = result.getJSONObject(k);
						int serverVersion = info.getInt("ver");
						String versionTitle = info.getString("verName");
						String versionDesc = info.getString("verDesc");
						String versionLink = info.getString("link");
						String versionType = info.getString("type");

						NAction.setInstallLink(getApplicationContext(), versionLink);

						if (BASE_CONF.DEBUG)
							Log.d(TAG, "serverVersion:" + serverVersion + "-versionTitle:" + versionTitle
									+ "-versionDesc:" + versionDesc + "-versionLink:" + versionLink);
						// 检查
						if (localVersion < serverVersion) {
							alertUpdateDialog(getString(R.string.up_soft) + versionTitle + "\n" + versionDesc,
									versionLink, versionType);

						}
						else {
							// 提示已经是最新版本，无需升级
							if (!auto) {
								alertUpdateDialog2(versionTitle);
							}
						}

						String adWho = info.getString("adWho");
						String adBanner = info.getString("adBanner");
						String adLink = info.getString("adLink");
						String adKey = info.getString("adKey");
						String adTerm = info.getString("adTerm");
						String adAct = info.getString("adAct");

						NAction.setAd(context, adWho, adBanner, adLink, adKey, adTerm, adAct);

						String extConf = "";
						if (info.has("ext2")) {
							JSONObject xx = info.getJSONObject("ext2");
							if (xx != null) {
								extConf = xx.toString();
							}
						}
						NAction.setExtConf(context, extConf);

						String extAdConf = "";
						if (info.has("ext_ad")) {
							JSONObject xx = info.getJSONObject("ext_ad");
							if (xx != null) {
								extAdConf = xx.toString();
							}
						}
						NAction.setExtAdConf(context, extAdConf);

						// 设置已经升级
						NAction.setUpdateCheckTime(context);
					}
					catch (JSONException e) {
						Log.d(TAG, "checkUpdate e:" + e.getMessage());
						e.printStackTrace();
					}

				}
                @Override
				public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
					// waitingWindow.dismiss();
					Log.d(TAG, "error:" + throwable.getMessage());
					if (!auto)
						closeWaitWindow();

				}
			});
		}
	}


	protected void alertUpdateDialog2(String desc) {
		try {
			if (BASE_CONF.DEBUG)
				Log.d(TAG, "no need to update");
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.up_soft))
					.setMessage(getString(R.string.up_soft_state_no_found) + "\n" + desc)
					.setPositiveButton(getString(R.string.promote_ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setNegativeButton(getString(R.string.promote_cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alert.create().show();
		}
		catch (Exception e) {
			Toast.makeText(getApplicationContext(), R.string.up_soft_state_no_found, Toast.LENGTH_SHORT).show();
		}
	}

	protected void alertUpdateDialog(String desc, final String updatelink, final String type) {
		try {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.up_soft))
					.setMessage(getString(R.string.up_soft_state_found) + "\n" + desc)
					.setPositiveButton(getString(R.string.up_soft), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (BASE_CONF.DEBUG)
								Log.d(TAG, "alertUpdateDialog updatelink:" + updatelink);
							NAction.recordAdLog(getApplicationContext(), "update", "");

							if (type.equals("link")) {
								Intent intent = NAction.getLinkAsIntent(getApplicationContext(), updatelink);
								startActivity(intent);

							}
							else {
								Intent updateIntent = new Intent(getApplicationContext(), getUpdateSrv());
								updateIntent.putExtra(BASE_CONF.EXTRA_CONTENT_URL1, R.string.app_name);
								updateIntent.putExtra(BASE_CONF.EXTRA_CONTENT_URL2, updatelink);
								updateIntent.putExtra(BASE_CONF.EXTRA_CONTENT_URL3, "apk");
								updateIntent.putExtra(BASE_CONF.EXTRA_CONTENT_URL4, "");
								updateIntent.putExtra(BASE_CONF.EXTRA_CONTENT_URL5, "");

								startService(updateIntent);
							}
						}
					}).setNegativeButton(getString(R.string.promote_cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alert.create().show();
		}
		catch (Exception e) {
			Toast.makeText(getApplicationContext(), R.string.up_soft_state_no_found, Toast.LENGTH_SHORT).show();
		}

	}

	abstract public Class<?> getUpdateSrv();

	public abstract String confGetUpdateURL(int flag);
}