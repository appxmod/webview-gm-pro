/*
 *    Copyright 2012 Werner Bayer
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package at.pardus.android.webview.gm.run;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import at.pardus.android.webview.gm.model.ScriptCriteria;
import at.pardus.android.webview.gm.model.ScriptResource;
import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;

/**
 * Contains methods simulating GM functions that need access to the
 * app/database. Used as interface accessible from javascript code.
 */
public class WebViewGmApi {

	private static final String TAG = "fatal "+WebViewGmApi.class.getName();

	private WebView view;

	private ScriptStoreSQLite ScriptStoreSQLite;

	//private String secret;

	/**
	 * Constructor.
	 * 
	 * @param ScriptStoreSQLite
	 *            the database to query for values
	 * @param secret
	 *            the secret string to compare in each call
	 */
	public WebViewGmApi(WebView view, ScriptStoreSQLite ScriptStoreSQLite, String secret) {
		this.view = view;
		this.ScriptStoreSQLite = ScriptStoreSQLite;
		//this.secret = secret;
	}

	/**
	 * Equivalent of GM_listValues.
	 * 
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @return a string consisting of the names of all found values separated by
	 *         commas
	 * @see <a href="http://wiki.greasespot.net/GM_listValues">GM_listValues</a>
	 */
    @JavascriptInterface
	public String listValues(String runtimeId, String secret) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightListValues()) {
			String[] values = ScriptStoreSQLite.getValueNames(script);
			if (values == null || values.length == 0) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (String v : values) {
				sb.append(",");
				sb.append(v);
			}
			sb.deleteCharAt(0);
			return sb.toString();
		}
		return null;
	}

	/**
	 * Equivalent of GM_getValue.
	 * 
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param name
	 *            the name of the value to get
	 * @param defaultValue
	 *            the value to return in case the one to retrieve does not exist
	 * @return the value of name or defaultValue if not found
	 * @see <a href="http://wiki.greasespot.net/GM_getValue">GM_getValue</a>
	 */
    @JavascriptInterface
	public String getValue(String runtimeId, String secret, String name) {
		//CMN.debug("getValue::", runtimeId, secret, ScriptStoreSQLite.getRunningScript(runtimeId));
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightGetValue()) {
			String v = ScriptStoreSQLite.getValue(script, name);
			//CMN.debug("getValue::", name, v);
			return v;
		}
		return null;
	}

	/**
	 * Equivalent of GM_setValue.
	 * 
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param name
	 *            the name of the value to set
	 * @param value
	 *            the value to set
	 * @see <a href="http://wiki.greasespot.net/GM_setValue">GM_setValue</a>
	 */
    @JavascriptInterface
	public void setValue(String runtimeId, String secret, String name, String value) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightSetValue()) {
			//CMN.debug("setValue::", name, value==null?-1:value.length(), value);
			ScriptStoreSQLite.setValue(script, name, value);
		}
	}

	/**
	 * Equivalent of GM_deleteValue.
	 * 
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param name
	 *            the name of the value to delete
	 * @see <tt><a href="http://wiki.greasespot.net/GM_deleteValue">GM_deleteValue</a></tt>
	 */
    @JavascriptInterface
	public void deleteValue(String runtimeId, String secret, String name) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightDeleteValue()) {
			ScriptStoreSQLite.deleteValue(script, name);
		}
	}

	/**
	 * Equivalent of GM_log. Output in Android log.
	 * 
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param message
	 *            the message to log
	 * @see <tt><a href="http://wiki.greasespot.net/GM_log">GM_log</a></tt>
	 */
    @JavascriptInterface
	public void log(String runtimeId, String secret, String message) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightLog()) {
			Log.i("webview-gm", script.getName() + ", " + script.getNamespace() + ": " + message);
		}
	}

	/**
	 * Equivalent of GM_getResourceURL. Retrieve URL of @resource'd data.
	 *
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param resourceName
	 *            the name of the resource to retrieve from the database.
	 * @see <tt><a href="http://wiki.greasespot.net/GM_getResourceURL">GM_getResourceURL</a></tt>
	 */
    @JavascriptInterface
	public String getResourceURL(String runtimeId, String secret, String resourceName) {
		CMN.debug("getResourceURL::", resourceName);
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightResource()) {
			try {
				ScriptResource resource = ScriptStoreSQLite.getResources(script, resourceName);
				if (resource != null) {
					return resource.getJavascriptUrl(); // todo safety
				}
				Log.e(TAG, "Requested resource: " + resourceName + " not found!");
			} catch (Exception e) {
				CMN.debug(e);
			}
		}
		return "";
	}

	/**
	 * Equivalent of GM_getResourceText. Retrieve @resource'd data. as UTF-8
	 * encoded text.
	 *
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param resourceName
	 *            the name of the resource to retrieve from the database.
	 * @see <tt><a href="http://wiki.greasespot.net/GM_getResourceText">GM_getResourceText</a></tt>
	 */
    @JavascriptInterface
	public String getResourceText(String runtimeId, String secret, String resourceName) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script!=null && script.hasRightResource()) {
			try {
				ScriptResource resource = ScriptStoreSQLite.getResources(script, resourceName);
				if (resource != null) {
					return resource.getJavascriptString();
				}
			} catch (Exception e) {
				CMN.debug(e);
			}
		}
		return "";
	}

	/**
	 * Equivalent of GM_xmlHttpRequest.
	 *
	 * @param scriptName
	 *            the name of the calling script
	 * @param scriptNamespace
	 *            the namespace of the calling script
	 * @param secret
	 *            the transmitted secret to validate
	 * @param jsonRequestString
	 *            the HTTP Request object encoded as a JSON string.
	 * @see <tt><a href="http://wiki.greasespot.net/GM_xmlhttpRequest">GM_xmlhttpRequest</a></tt>
	 */
    @JavascriptInterface
	public String xmlHttpRequest(String runtimeId, String secret, String jsonRequestString) {
		try {
			ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
			if (script != null) {
				CMN.debug("xmlHttpRequest::", script.hasRightXmlHttpRequest(), script);
				if (script.hasRightXmlHttpRequest()) {
					boolean doit = false;
					WebViewXmlHttpRequest request = new WebViewXmlHttpRequest(this.view, jsonRequestString);
					String domain = getDomain(request.url);
					CMN.debug(domain, "xmlHttpRequest::connect", script.connect);
					if (script.connect != null) {
						for (String conn : script.connect) {
							if (domain.endsWith(conn)) {
								int num = domainSegNum(conn);
								if (num == 1) {
									doit = true;
									break;
								}
								if (num > 1 && conn.length() == domain.length()) {
									doit = true;
									break;
								}
							}
						}
					}
					CMN.debug("doit::", doit);
					// todo let user decide if the domain is not defined in metablock @connect sth.com
					if (doit) {
						WebViewXmlHttpResponse response = request.execute();
						if (response != null) {
							//CMN.debug("response::", response);
							return response.toJSONString();
						}
					}
				}
			}
		} catch (Exception e) {
			CMN.debug(e);
		}
		return "";
	}
	
	private String getDomain(String url) {
		int st = url.indexOf(":")+3;
		int ed = url.indexOf("/", st);
		if(ed==-1) ed = url.length();
		return url.substring(st, ed);
	}
	
	private int domainSegNum(String conn) {
		int cc=0, idx=0;
		while ((idx=conn.indexOf(".", idx+1))>0) cc++;
		return cc;
	}
	
	// https://github.com/Tampermonkey/tampermonkey/issues/465
    @JavascriptInterface
	public String cookieList(String runtimeId, String secret, String details, String callback) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if (script==null || !script.secret.equals(secret)) {
			Log.e(TAG,
					"Call to \"xmlHttpRequest\" did not supply correct secret");
			return "";
		}
		if(script.hasRightCookie()) {
			JSONObject response = new JSONObject();
			try {
				JSONObject json = new JSONObject(details);
				String url = json.getString("url");
				boolean doit = false;
				String domain = getDomain(url);
				if (script.connect != null) {
					for (String conn : script.connect) {
						if (conn.startsWith("cookie:") && domain.endsWith(conn = conn.substring(7))) {
							int num = domainSegNum(conn);
							if (num == 1) {
								doit = true;
								break;
							}
							if (num > 1 && conn.length() == domain.length()) {
								doit = true;
								break;
							}
						}
					}
				}
				// todo let user decide if the domain is not defined in metablock @connect cookie:sth.com
				if (doit) {
					CookieManager cookieManager = CookieManager.getInstance();
					String CookieStr = cookieManager.getCookie(url);
					CMN.debug("CookieStr::", CookieStr);
					response.put("value", CookieStr);
				}
			} catch (Exception e) {
				CMN.debug(e);
			}
			this.view.post(new Runnable() {
				@Override
				public void run() {
					view.evaluateJavascript("(function() { unsafeWindow[\""
							+ callback + "\"](JSON.parse(" + JSONObject.quote(response.toString())
							+ ")); ", null);
				}
			});
		}
		return "";
	}
	
	@JavascriptInterface
	public boolean configDomain(String runtimeId, String secret, boolean setIfNonSet, String options) {
		return false;
	}
	
	
    @JavascriptInterface
	public void blockImage(String runtimeId, String secret, boolean block, boolean reload) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if(script!=null && script.hasRightBlockImage()) {
		
		}
	}
	
    @JavascriptInterface
	public void blockCorsJump(String runtimeId, String secret, boolean block) {
		ScriptCriteria script = ScriptStoreSQLite.getRunningScript(runtimeId, secret);
		if(script!=null && script.hasRightBlockCorsJump()) {
		
		}
	}
	
	// https://github.com/Tampermonkey/tampermonkey/issues/322
    @JavascriptInterface
	public void isInstalled(String name, String namespace, String callback) {
		// console.log('Yes, ' + i.name + ' version ' + i.version + ' is installed and ' + (i.enabled ? 'enabled' : 'disabled') + '.');
		
	}
}
