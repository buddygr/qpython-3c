package com.quseit.common.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.util.Log;

import com.quseit.config.BASE_CONF;
import com.quseit.service.DownloadInfo;


/**
 * 
 * 一个业务类
 */
public class DownloadLog {
	private Context context;
	private static final String TAG = "DownloadLog";

	public DownloadLog(Context context) {
		this.context = context;
	}

	/**
	 * 查看数据库中是否有数据
	 */
	public DownloadInfo getInfoByPath(String path) {
		DBHelper dbHelper = new DBHelper(context);

		try {
			SQLiteDatabase database = dbHelper.getReadableDatabase();
			String sql = "select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album ,service_stat,service_json from download_info where path=? ORDER BY _id DESC";
			Cursor cursor = database.rawQuery(sql, new String[] { path });
			if (cursor.moveToNext()) {
				DownloadInfo info = new DownloadInfo(cursor.getInt(0),
						cursor.getInt(1), cursor.getLong(2), cursor.getLong(3),
						cursor.getString(4), cursor.getString(5),
						cursor.getString(6), cursor.getInt(7),
						cursor.getInt(8), cursor.getString(9),
						cursor.getString(10), cursor.getString(11),cursor.getInt(12),cursor.getString(13));
				cursor.close();
				database.close();
				// Log.d(TAG, "getInfoByTerm");
				dbHelper.close();
				return info;
			} else {
				dbHelper.close();
			}
		} catch (Exception e) {
			dbHelper.close();

		}
		return null;
	}

	public DownloadInfo getInfoByUrl(String urlstr) {
		DBHelper dbHelper = new DBHelper(context);

		try {
			SQLiteDatabase database = dbHelper.getReadableDatabase();
			String sql = "select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album ,service_stat,service_json from download_info where url=? ORDER BY _id DESC";
			Cursor cursor = database.rawQuery(sql, new String[] { urlstr });
			if (cursor.moveToNext()) {
				DownloadInfo info = new DownloadInfo(cursor.getInt(0),
						cursor.getInt(1), cursor.getLong(2), cursor.getLong(3),
						cursor.getString(4), cursor.getString(5),
						cursor.getString(6), cursor.getInt(7),
						cursor.getInt(8), cursor.getString(9),
						cursor.getString(10), cursor.getString(11),cursor.getInt(12),cursor.getString(13));
				cursor.close();
				database.close();
				// Log.d(TAG, "getInfoByTerm");
				dbHelper.close();
				return info;
			} else {
				dbHelper.close();
			}
		} catch (Exception e) {
			dbHelper.close();

		}
		return null;
	}
	
	public DownloadInfo getInfoByTerm(String urlstr, String path) {
		DBHelper dbHelper = new DBHelper(context);

		SQLiteDatabase database = dbHelper.getReadableDatabase();
		String sql = "select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album from download_info where url=? AND path=? ORDER BY _id DESC";
		Cursor cursor = database.rawQuery(sql, new String[] { urlstr, path });
		if (cursor.moveToNext()) {
			DownloadInfo info = new DownloadInfo(cursor.getInt(0),
					cursor.getInt(1), cursor.getLong(2), cursor.getLong(3),
					cursor.getString(4), cursor.getString(5),
					cursor.getString(6), cursor.getInt(7), cursor.getInt(8),
					cursor.getString(9), cursor.getString(10),
					cursor.getString(11));
			cursor.close();
			database.close();
			// Log.d(TAG, "getInfoByTerm");
			dbHelper.close();
			return info;
		} else {
			dbHelper.close();
		}
		return null;
	}

	/*
	 * public boolean isHasInfors(String urlstr, String path) { DBHelper
	 * dbHelper = new DBHelper(context); int count = 0; try { SQLiteDatabase
	 * database = dbHelper.getReadableDatabase(); String sql =
	 * "select count(*)  from download_info where url=? AND path=? ORDER BY _id DESC"
	 * ; Cursor cursor = database.rawQuery(sql, new String[] { urlstr , path});
	 * cursor.moveToFirst(); count = cursor.getInt(0); cursor.close();
	 * database.close(); } catch (Exception e) {
	 * 
	 * }
	 * 
	 * Log.d(TAG, "isHasInfors:"+path+"-"+urlstr); dbHelper.close(); return
	 * count > 0; }
	 */

	/**
	 * 保存 下载的具体信息
	 */
	public void saveInfos(final List<DownloadInfo> infos, final int count) {
		if (count < BASE_CONF.TRY_COUNT) {

			DBHelper dbHelper = new DBHelper(context);
			try {
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				for (DownloadInfo info : infos) {
					String sql = "insert into download_info(thread_id,start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album,service_stat) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object[] bindArgs = { info.getThreadId(),
							info.getStartPos(), info.getEndPos(),
							info.getCompeleteSize(), info.getUrl(),
							info.getPath(), info.getOrgLink(),
							info.getQuality(), info.getStat(), info.getTitle(),
							info.getArtist(), info.getAlbum(),
							info.getService_stat() };
					database.execSQL(sql, bindArgs);
					/*if (!BASE_CONF.DEBUG)
						Log.d(TAG, "saveInfos:path(" + info.getPath()
								+ ")orgLink:(" + info.getOrgLink()
								+ ")quality:(" + info.getQuality() + ")url("
								+ info.getUrl() + ")");*/

				}
				database.close();

			} catch (SQLiteException e) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						saveInfos(infos, count + 1);
					}

				}, BASE_CONF.TRY_DELAY);
			}
			dbHelper.close();
		}

	}

	/**
	 * 得到下载具体信息
	 */
	public List<DownloadInfo> getInfos(String urlstr) {
		DBHelper dbHelper = new DBHelper(context);

		List<DownloadInfo> list = new ArrayList<DownloadInfo>();
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		String sql = "select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album from download_info where url=?";
		Cursor cursor = database.rawQuery(sql, new String[] { urlstr });
		while (cursor.moveToNext()) {
			DownloadInfo info = new DownloadInfo(cursor.getInt(0),
					cursor.getInt(1), cursor.getLong(2), cursor.getLong(3),
					cursor.getString(4), cursor.getString(5),
					cursor.getString(6), cursor.getInt(7), cursor.getInt(8),
					cursor.getString(9), cursor.getString(10),
					cursor.getString(11));
			if (BASE_CONF.DEBUG)
				Log.d(TAG, "getInfos info:" + info.toString());
			list.add(info);
		}
		cursor.close();
		database.close();
		dbHelper.close();
		return list;
	}

	public List<DownloadInfo> getInfosByPath(String path) {
		DBHelper dbHelper = new DBHelper(context);

		List<DownloadInfo> list = new ArrayList<DownloadInfo>();
		try {
			SQLiteDatabase database = dbHelper.getReadableDatabase();
			String sql = "select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album from download_info where path=?";
			Cursor cursor = database.rawQuery(sql, new String[] { path });
			while (cursor.moveToNext()) {
				DownloadInfo info = new DownloadInfo(cursor.getInt(0),
						cursor.getInt(1), cursor.getLong(2), cursor.getLong(3),
						cursor.getString(4), cursor.getString(5),
						cursor.getString(6), cursor.getInt(7),
						cursor.getInt(8), cursor.getString(9),
						cursor.getString(10), cursor.getString(11));
				if (BASE_CONF.DEBUG)
					Log.d(TAG, "getInfos info:" + info.toString());

				list.add(info);
			}
			cursor.close();
			database.close();
			dbHelper.close();
		} catch (SQLiteException e) {
			// for (int i=0;i<1000;i++) {}
			// return getInfosByPath(path);
			dbHelper.close();
		}

		return list;
	}

	public List<DownloadInfo> getWaitlist() {
		DBHelper dbHelper = new DBHelper(context);
		List<DownloadInfo> list = new ArrayList<DownloadInfo>();
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		Cursor c = database
				.rawQuery(
						"select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album,service_stat,service_json from download_info where thread_id=?",
						new String[] { "0" });
		while (c.moveToNext()) {
			DownloadInfo info = new DownloadInfo(c.getInt(0), c.getInt(1),
					c.getLong(2), c.getLong(3), c.getString(4), c.getString(5),
					c.getString(6), c.getInt(7), c.getInt(8), c.getString(9),
					c.getString(10), c.getString(11), c.getInt(12),
					c.getString(13));
			if (BASE_CONF.DEBUG)
				Log.d(TAG, "getInfos info:" + info.toString());

			list.add(info);
		}
		c.close();
		database.close();
		dbHelper.close();
		return list;
	}

	/**
	 * 更新数据库中的下载信息
	 */
	public void updataStatAndUrlByPath(final String path, final String url,
			final int stat, final int count) {

		if (count < BASE_CONF.TRY_COUNT) {
			DBHelper dbHelper = new DBHelper(context);

			try {
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				String sql = "update download_info set stat=?,url=? where path=?";
				Object[] bindArgs = { stat, url, path };
				database.execSQL(sql, bindArgs);
				database.close();
			} catch (SQLiteException e) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						updataStatAndUrlByPath(path, url, stat, count + 1);
					}

				}, BASE_CONF.TRY_DELAY);
			}

			dbHelper.close();
		}
		if (BASE_CONF.DEBUG)
			Log.d(TAG, "updataStatByPath-path(" + path + ")stat:(" + stat + ")");

	}

	public void updataStatByPath(final String path, final int stat,
			final int count) {
		if (count < BASE_CONF.TRY_COUNT) {
			DBHelper dbHelper = new DBHelper(context);

			try {
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				String sql = "update download_info set stat=? where path=?";
				Object[] bindArgs = { stat, path };
				database.execSQL(sql, bindArgs);
				database.close();
				if (BASE_CONF.DEBUG)
					Log.d(TAG, "updataStatByPath-path(" + path + ")stat:("
							+ stat + ")");
			} catch (SQLiteException e) {

				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						updataStatByPath(path, stat, count + 1);
					}

				}, BASE_CONF.TRY_DELAY);
			}
			dbHelper.close();

		}

	}

	public void updateInfos(final int threadId, final long totalSize,
			final long compeleteSize, final String urlstr, final String path,
			final int stat, final int count) {
		if (count < BASE_CONF.TRY_COUNT) {

			DBHelper dbHelper = new DBHelper(context);
			try {
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				String sql = "update download_info set end_pos=?,compelete_size=?,stat=? where thread_id=? and url=? and path=?";
				Object[] bindArgs = { totalSize, compeleteSize, stat, threadId,
						urlstr, path };
				database.execSQL(sql, bindArgs);
				database.close();
			} catch (SQLiteException e) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						updateInfos(threadId, totalSize, compeleteSize, urlstr,
								path, stat, count + 1);
					}

				}, BASE_CONF.TRY_DELAY);
			}
			dbHelper.close();
			// if (BASE_CONF.DEBUG) Log.d(TAG,
			// "path("+path+")totalSize:("+totalSize+")compepeteSize:("+compeleteSize+")");
		}

	}

	public void updatefileleng(long end, String urlstr, String path) {
		DBHelper dbHelper = new DBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(
				"update download_info set end_pos=? where  url=? and path=?",
				new Object[] { end, urlstr, path });
		db.close();
		dbHelper.close();
	}

	public void updateInfos(final long start_pos, final long compeleteSize,
			final String urlstr, final String path, final int count) {
		if (count < BASE_CONF.TRY_COUNT) {
			DBHelper dbHelper = new DBHelper(context);

			try {
				SQLiteDatabase database = dbHelper.getWritableDatabase();
				String sql = "update download_info set compelete_size=?,start_pos=? where  url=? and path=?";
				Object[] bindArgs = { compeleteSize, start_pos, urlstr, path };
				database.execSQL(sql, bindArgs);
				database.close();
			} catch (SQLiteException e) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						updateInfos(start_pos, compeleteSize, urlstr, path,
								count);
					}

				}, BASE_CONF.TRY_DELAY);
			}
			dbHelper.close();

		}
		if (BASE_CONF.DEBUG)
			Log.d(TAG, "path(" + path + ")compepeteSize:(" + compeleteSize
					+ ")");

	}

	public int getDownLoadStat() {
		DBHelper dbHelper = new DBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		Cursor c = db.rawQuery(
				"select count(*) from download_info where service_stat like ? ",
				new String[] { "4" });
		int state = -1;
		if (c.moveToNext()) {
			state = c.getInt(0);
		}
		c.close();
		db.close();
		dbHelper.close();
		return state;
		
	}

	public int queryReeat(String url) {
		DBHelper dbHelper = new DBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Cursor c = db.rawQuery(
				"select count(*) from download_info where url like ?",
				new String[] { url});
		int state = -1;
		if (c.moveToNext()) {
			state = c.getInt(0);
		}
		c.close();
		dbHelper.close();
		return state;
	}

	public void updateDownLoadState(int service_stat, String service_json,int stat, String urlstr, String path) {
		DBHelper dbHelper = new DBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(
				"update download_info set service_stat=?,service_json=?,stat=? where  url=? and path=?",
				new Object[] { service_stat, service_json, stat, urlstr, path });
		db.close();
		dbHelper.close();
	}

	public void updateDownLoadState(int service_stat, int stat, String urlstr,
			String path) {
		DBHelper dbHelper = new DBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.execSQL(
				"update download_info set service_stat=?,stat=? where  url=? and path=?",
				new Object[] { service_stat, stat, urlstr, path });
		db.close();
		dbHelper.close();
	}

	public List<DownloadInfo> query() {
		DBHelper dbHelper = new DBHelper(context);
		List<DownloadInfo> list = new ArrayList<DownloadInfo>();
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		Cursor c = database
				.rawQuery(
						"select thread_id, start_pos, end_pos,compelete_size,url,path,orglink,quality,stat,title,artist,album,service_stat,service_json from download_info where service_stat=?",
						new String[] { "1" });
		while (c.moveToNext()) {
			DownloadInfo info = new DownloadInfo(c.getInt(0), c.getInt(1),
					c.getLong(2), c.getLong(3), c.getString(4), c.getString(5),
					c.getString(6), c.getInt(7), c.getInt(8), c.getString(9),
					c.getString(10), c.getString(11), c.getInt(12),
					c.getString(13));
			if (BASE_CONF.DEBUG)
				Log.d(TAG, "getInfos info:" + info.toString());

			list.add(info);
		}
		c.close();
		database.close();
		dbHelper.close();
		return list;
	}

	/**
	 * 关闭数据库
	 */
	

	/**
	 * 下载完成后删除数据库中的数据
	 */
	public void deleteP(String path) {
		DBHelper dbHelper = new DBHelper(context);

		try {
			SQLiteDatabase database = dbHelper.getWritableDatabase();
			database.delete("download_info", "path=?", new String[] { path });
			database.close();
		} catch (Exception e) {

		}
		dbHelper.close();
		if (BASE_CONF.DEBUG)
			Log.d(TAG, "deleteP:" + path);
	}

	public void deleteL(String orgLink, String path) {
		DBHelper dbHelper = new DBHelper(context);
		try {
			SQLiteDatabase database = dbHelper.getWritableDatabase();
			database.delete("download_info", "orglink=? AND path=?",
					new String[] { orgLink, path });
			database.close();
		} catch (SQLiteException e) {

		}
		dbHelper.close();
		if (BASE_CONF.DEBUG)
			Log.d(TAG, "deleteL:" + path + "-" + orgLink);
	}

	public void delete(String url, String path) {
		DBHelper dbHelper = new DBHelper(context);

		SQLiteDatabase database = dbHelper.getWritableDatabase();
		database.delete("download_info", "url=? AND path=?", new String[] {
				url, path });
		database.close();
		dbHelper.close();
		if (BASE_CONF.DEBUG)
			Log.d(TAG, "delete:" + path + "-" + url);

	}
}
