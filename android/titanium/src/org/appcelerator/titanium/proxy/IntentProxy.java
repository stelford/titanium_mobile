/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import java.util.Arrays;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

@Kroll.proxy
public class IntentProxy extends KrollProxy 
{
	private static final String TAG = "TiIntent";
	private static boolean DBG = TiConfig.LOGD;
	
	protected Intent intent;
	protected boolean forService = false;
	
	public IntentProxy(TiContext tiContext) {
		super(tiContext);
	}
	
	public IntentProxy(TiContext tiContext, Intent intent) {
		super(tiContext);
		this.intent = intent;
	}
	
	protected static char[] escapeChars = new char[] {
		'\\', '/', ' ', '.', '$', '&', '@'
	};
	
	protected static String getActivityURLClassName(String url) {
		return getURLClassName(url, "Activity");
	}
	
	protected static String getServiceURLClassName(String url) {
		return getURLClassName(url, "Service");
	}
	
	protected static String getURLClassName(String url, String appendage) {
		List<String> parts = Arrays.asList(url.split("/"));
		if (parts.size() == 0) return null;
		
		int start = 0;
		if (parts.get(0).equals("app:") && parts.size() >= 3) {
			start = 2;
		}
		
		String className = TextUtils.join("_", parts.subList(start, parts.size()));
		if (className.endsWith(".js")) {
			className = className.substring(0, className.length()-3);
		}
		
		if (className.length() > 1) {
			className = className.substring(0, 1).toUpperCase() + className.substring(1);
		} else {
			className = className.toUpperCase();
		}
		
		for (char escapeChar : escapeChars) {
			className = className.replace(escapeChar, '_');
		}
		
		return className+appendage;
	}
	
	public void handleCreationDict(KrollDict dict) {
		intent = new Intent();
		
		// See which set of options we have to work with.
		String action = dict.getString(TiC.PROPERTY_ACTION);
		String url = dict.getString(TiC.PROPERTY_URL);
		String data = dict.getString(TiC.PROPERTY_DATA);
		String className = dict.getString(TiC.PROPERTY_CLASS_NAME);
		String packageName = dict.getString(TiC.PROPERTY_PACKAGE_NAME);
		String type = dict.getString(TiC.PROPERTY_TYPE);

		if (action != null) {
			if (DBG) {
				Log.d(TAG, "Setting action: " + action);
			}
			intent.setAction(action);
		}
		
		if (data != null) {
			if (DBG) {
				Log.d(TAG, "Setting data uri: " + data);
			}
			intent.setData(Uri.parse(data));
		}
		
		if (packageName != null) {
			if (DBG) {
				Log.d(TAG, "Setting package: " + packageName);
			}
			intent.setPackage(packageName);
		}
		
		if (url != null) {
			if (DBG) {
				Log.d(TAG, "Creating intent for JS Activity/Service @ " + url);
			}
			packageName = TiApplication.getInstance().getPackageName();
			className = packageName + "." + (forService ? getServiceURLClassName(url) : getActivityURLClassName(url));
		}
		
		if (className != null) {
			if (packageName != null) {
				if (DBG) {
					Log.d(TAG, "Both className and packageName set, using intent.setClassName(packageName, className");
				}
				intent.setClassName(packageName, className);
			} else {
				try {
					Class<?> c = getClass().getClassLoader().loadClass(className);
					intent.setClass(getTiContext().getActivity().getApplicationContext(), c);
				} catch (ClassNotFoundException e) {
					Log.e(TAG, "Unable to locate class for name: " + className);
					throw new IllegalStateException("Missing class for name: " + className, e);
				}
			}
		}
		
		if (type != null) {
			if (DBG) {
				Log.d(TAG, "Setting type: " + type);
			} 
			intent.setType(type);
		} else {
			if (action != null && action.equals(Intent.ACTION_SEND)) {
				if (DBG) {
					Log.d(TAG, "Intent type not set, defaulting to text/plain because action is a SEND action");
				}
				intent.setType("text/plain");
			}
		}
	}	
	
	@Kroll.method
	public void putExtra(String key, Object value) 
	{
		if (value instanceof String) {
			intent.putExtra(key, (String) value);
		} else if (value instanceof Boolean) {
			intent.putExtra(key, (Boolean) value);
		} else if (value instanceof Double) {
			intent.putExtra(key, (Double) value);
		} else if (value instanceof Integer) {
			intent.putExtra(key, (Integer) value);
		} else if (value instanceof Long) {
			intent.putExtra(key, (Long) value);
		}
		else {
			Log.w(TAG, "Warning unimplemented put conversion for " + value.getClass().getCanonicalName() + " trying String");
			intent.putExtra(key, TiConvert.toString(value));
		}
	}
	
	@Kroll.method
	public void addCategory(String category) {
		if (category != null) {
			if (DBG) {
				Log.d(TAG, "Adding category: " + category);
			}
			intent.addCategory(category);
		}
	}
	
	@Kroll.method
	public String getStringExtra(String name) {
		return intent.getStringExtra(name);
	}
	
	@Kroll.method
	public boolean getBooleanExtra(String name, boolean defaultValue) {
		return intent.getBooleanExtra(name, defaultValue);
	}
	
	@Kroll.method
	public int getIntExtra(String name, int defaultValue) {
		return intent.getIntExtra(name, defaultValue);
	}
	
	@Kroll.method
	public long getLongExtra(String name, long defaultValue) {
		return intent.getLongExtra(name, defaultValue);
	}
	
	@Kroll.method
	public double getDoubleExtra(String name, double defaultValue) {
		return intent.getDoubleExtra(name, defaultValue);
	}
	
	@Kroll.method @Kroll.getProperty
	public String getData() {
		return intent.getDataString();
	}
	
	public Intent getIntent() { 
		return intent;
	}
	
	public void setForService(boolean value) {
		forService = value;
	}
	
	@Kroll.method
	public boolean hasExtra(String name)
	{
		if (intent != null) {
			return intent.hasExtra(name);
		}
		return false;
	}

}
