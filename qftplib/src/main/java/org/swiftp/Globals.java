package org.swiftp;

import java.io.File;

import org.swiftp.server.ProxyConnector;

import android.content.Context;

// TODO: this must all be removed
//       if you need a setting, get it from the settings

public class Globals {
	private static Context context;
	private static String lastError;
	private static File chrootDir = null;
	private static ProxyConnector proxyConnector = null;
	private static String username = null;

	public static ProxyConnector getProxyConnector() {
		if(proxyConnector != null) {
			if(!proxyConnector.isAlive()) {
				return null;
			}
		}
		return proxyConnector;
	}

	public static void setProxyConnector(ProxyConnector proxyConnector) {
		Globals.proxyConnector = proxyConnector;
	}

	public static File getChrootDir() {
		return chrootDir;
	}

	public static void setChrootDir(File chrootDir) {
		if(chrootDir.isDirectory()) {
			Globals.chrootDir = chrootDir;
		}
	}

	public static String getLastError() {
		return lastError;
	}

	public static void setLastError(String lastError) {
		Globals.lastError = lastError;
	}

	public static Context getContext() {
		return context;
	}

	public static void setContext(Context context) {
		if(context != null) {
			Globals.context = context;
		}
	}

	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		Globals.username = username;
	}

}
