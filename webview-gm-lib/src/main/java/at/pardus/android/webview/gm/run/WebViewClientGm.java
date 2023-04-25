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

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.knziha.metaline.Metaline;

import java.util.UUID;

import at.pardus.android.webview.gm.model.Script;
import at.pardus.android.webview.gm.model.ScriptRequire;
import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;

/**
 * A user script enabled WebViewClient to be used by WebViewGm.
 */
public class WebViewClientGm extends WebViewClient {

	private static final String TAG = WebViewClientGm.class.getName();

	private static final String JSCONTAINERSTART = "(function() {";

	private static final String JSCONTAINEREND = "\n})()";

   /**
	unsafeWindow = (function() {
		var el = document.createElement('p');
		el.setAttribute('onclick', 'return window;');
		return el.onclick();
	}());
	window.wrappedJSObject = unsafeWindow;
	var GM_wv={};
	function GM_listValues() {
		return GM_wv.bg.listValues(GM_wv.n, GM_wv.ns, GM_wv.sec).split(",");
	}
	function GM_getValue(name, defaultValue) {
		return GM_wv.bg.getValue(GM_wv.n, GM_wv.ns, GM_wv.sec, name, defaultValue);
	}
	function GM_setValue(name, value) {
		GM_wv.bg.setValue(GM_wv.n, GM_wv.ns, GM_wv.sec, name, value);
	}
	function GM_deleteValue(name) {
		GM_wv.bg.deleteValue(GM_wv.n, GM_wv.ns, GM_wv.sec, name);
	}
	function GM_addStyle(css) {
		var style = document.createElement("style");
		style.type = "text/css";
		style.innerHTML = css;
		document.getElementsByTagName('head')[0].appendChild(style);
	}
	function GM_log(message) {
		GM_wv.bg.log(GM_wv.n, GM_wv.ns, GM_wv.sec, message);
	}
	function GM_getResourceURL(resourceName) {
		return GM_wv.bg.getResourceURL(GM_wv.n, GM_wv.ns, GM_wv.sec, resourceName);
	}
	function GM_getResourceText(resourceName) {
		return GM_wv.bg.getResourceText(GM_wv.n, GM_wv.ns, GM_wv.sec, resourceName);
	}
	function GM_xmlhttpRequest(details) {
		var sig = '_' + Math.ceil(Math.random() * 10000) + ('' + Date.now()).slice(7);
		var pfx = GM_wv.hash;
		var key, he=details, his=[];
		function hook(n, b) {
			if(he[n]) {
				key = sig + b + pfx;
				unsafeWindow[key] = he[n];
				he[n] = key;
				his.push(key);
			}
		}
		details.ondone = function() {
			for(var i=0,he;he=his[i++];) {
				delete unsafeWindow[he];
			}
		};
		hook('ondone', 'GM_onDone');
		hook('onabort', 'GM_onAbortCallback');
		hook('onerror', 'GM_onErrorCallback');
		hook('onload', 'GM_onLoadCallback');
		hook('onprogress', 'GM_onProgressCallback');
		hook('onreadystatechange', 'GM_onReadyStateChange');
		hook('ontimeout', 'GM_onTimeoutCallback');
		he=details.upload;
		if (he) {
			hook('onabort', 'GM_uploadOnAbortCallback');
			hook('onerror', 'GM_uploadOnErrorCallback');
			hook('onload', 'GM_uploadOnLoadCallback');
			hook('onprogress', 'GM_uploadOnProgressCallback');
		}
		return JSON.parse(GM_wv.bg.xmlHttpRequest(GM_wv.n, GM_wv.ns, GM_wv.sec, JSON.stringify(details)));
	}
	function nonimpl(n) {
		GM_log(n+" is not yet implemented");
	}
	function GM_openInTab() {
		nonimpl('GM_openInTab');
	}
	function GM_registerMenuCommand() {
		nonimpl('GM_registerMenuCommand');
	}
	function GM_notification() {
		nonimpl('GM_notification');
	}
	function GM_unregisterMenuCommand() {
		nonimpl('GM_unregisterMenuCommand');
	}
	function GM_setClipboard() {
		nonimpl('GM_setClipboard');
	}
 */
	@Metaline()
	private static final String JSUNSAFEWINDOW = "https://wiki.greasespot.net/Greasemonkey_Manual:API";

	/**var GM_info = {
		script: {
			name: GM_wv.n
			, namespace: GM_wv.ns
			, version: GM_wv.ver
		}
		, scriptMetaStr: GM_wv.meta
		, scriptHandler: 'webview-gm'
		, version: 'infinite'
	};*/
	@Metaline()
	private static final String JSGMINFO = "https://wiki.greasespot.net/GM.info";

//	private static final String JSMISSINGFUNCTIONS = "var GM_openInTab = " + JSMISSINGFUNCTION
//			+ "var GM_registerMenuCommand = " + JSMISSINGFUNCTION
//			+ "var GM_notification = " + JSMISSINGFUNCTION
//			+ "var GM_unregisterMenuCommand = " + JSMISSINGFUNCTION
//			+ "var GM_setClipboard = " + JSMISSINGFUNCTION;

	private ScriptStoreSQLite ScriptStoreSQLite;

	private String jsBridgeName;

	private String secret;

	/**
	 * Constructs a new WebViewClientGm with a ScriptStoreSQLite.
	 *
	 * @param ScriptStoreSQLite
	 *            the script database to query for scripts to run when a page
	 *            starts/finishes loading
	 * @param jsBridgeName
	 *            the variable name to access the webview GM functions from
	 *            javascript code
	 * @param secret
	 *            a random string that is added to calls of the GM API
	 */
	public WebViewClientGm(ScriptStoreSQLite ScriptStoreSQLite, String jsBridgeName,
			String secret) {
		this.ScriptStoreSQLite = ScriptStoreSQLite;
		this.jsBridgeName = jsBridgeName;
		this.secret = secret;
	}
	
	StringBuilder buffer = new StringBuilder();
	
	/**
	 * Runs user scripts enabled for a given URL.
	 *
	 * Unless a script specifies unwrap it is executed inside an anonymous
	 * function to hide it from access from the loaded page. Calls to the global
	 * JavaScript bridge methods require a secret that is set inside of each
	 * user script's anonymous function.
	 *
	 * @param view
	 *            the view to load scripts in
	 * @param url
	 *            the current address
	 * @param pageFinished
	 *            true if scripts with runAt property set to document-end or
	 *            null should be run, false if set to document-start
	 * @param jsBeforeScript
	 *            JavaScript code to add between the GM API and the start of the
	 *            user script code (may be null)
	 * @param jsAfterScript
	 *            JavaScript code to add after the end of the user script code
	 *            (may be null)
	 */
	protected void runMatchingScripts(WebView view, String url,
			boolean pageFinished, String jsBeforeScript, String jsAfterScript) {
		if (ScriptStoreSQLite == null) {
			Log.w(TAG, "Property ScriptStoreSQLite is null - not running any scripts");
			return;
		}
		Script[] matchingScripts = ScriptStoreSQLite.get(url, true, false);
		//CMN.debug("matchingScripts::", matchingScripts);
		CMN.debug(matchingScripts);
		if (matchingScripts == null) {
			return;
		}
		if (jsBeforeScript == null) {
			jsBeforeScript = "";
		}
		if (jsAfterScript == null) {
			jsAfterScript = "";
		}
		for (Script script : matchingScripts) {
			if ((!pageFinished && Script.RUNATSTART.equals(script.getRunAt()))
					|| (pageFinished && (script.getRunAt() == null || Script.RUNATEND
							.equals(script.getRunAt())))) {
				Log.i(TAG, "Running script \"" + script + "\" on " + url);
				
				buffer.setLength(0);
				buffer.ensureCapacity(JSUNSAFEWINDOW.length()*3+script.getContent().length());
				
				boolean unwrap = script.isUnwrap();
				boolean bigcake = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
				if (!bigcake) {
					buffer.append("javascript:\n");
					unwrap = false;
				}
				if (!unwrap) {
					buffer.append(JSCONTAINERSTART);
				}
				buffer.append(JSUNSAFEWINDOW);
				buffer.append("GM_wv.n=\"").append(script.getName().replace("\"", "\\\"")).append("\"");
				buffer.append(";GM_wv.ns=\"").append(script.getNamespace().replace("\"", "\\\"")).append("\"");
				buffer.append(";GM_wv.ver=\"").append(script.getVersion().replace("\"", "\\\"")).append("\"");
				buffer.append(";GM_wv.sec=\"").append(secret).append("\"");
				buffer.append(";GM_wv.bg=").append(jsBridgeName);
				buffer.append(";GM_wv.hash=\"").append(("GM_"
						+ script.getName()
						+ script.getNamespace()
						+ UUID.randomUUID().toString())
						.replaceAll("[^0-9a-zA-Z_]", "")).append("\"");
				buffer.append(";GM_wv.bg=").append(jsBridgeName)
						.append(";").append(JSGMINFO).append("\n");

				// Get @require'd scripts to inject for this script.
				ScriptRequire[] requires = script.getRequires();
				if (requires != null) {
					for (ScriptRequire currentRequire : requires) {
						//CMN.debug("currentRequire::", currentRequire.getContent());
						buffer.append(currentRequire.getContent());
						buffer.append("\n");
					}
				}
				
				buffer.append(jsBeforeScript)
						.append(script.getContent())
						.append(jsAfterScript);
				if (!unwrap) {
					buffer.append(JSCONTAINEREND);
				}
				
				// todo FIXME java.lang.OutOfMemoryError: Failed to allocate a 16 byte allocation with 1795200 free bytes and 1753KB until OOM
				String jsCode = buffer.toString();
				
                if (bigcake) {
                    view.evaluateJavascript(jsCode, null);
                } else {
                    view.loadUrl(jsCode);
                }
			}
		}
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		runMatchingScripts(view, url, false, null, null);
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		runMatchingScripts(view, url, true, null, null);
	}

	/**
	 * @return the ScriptStoreSQLite
	 */
	public ScriptStoreSQLite getScriptStoreSQLite() {
		return ScriptStoreSQLite;
	}

	/**
	 * @param ScriptStoreSQLite
	 *            the ScriptStoreSQLite to set
	 */
	public void setScriptStoreSQLite(ScriptStoreSQLite ScriptStoreSQLite) {
		this.ScriptStoreSQLite = ScriptStoreSQLite;
	}

	/**
	 * @return the jsBridgeName
	 */
	public String getJsBridgeName() {
		return jsBridgeName;
	}

	/**
	 * @param jsBridgeName
	 *            the jsBridgeName to set
	 */
	public void setJsBridgeName(String jsBridgeName) {
		this.jsBridgeName = jsBridgeName;
	}

	/**
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * @param secret
	 *            the secret to set
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

}
