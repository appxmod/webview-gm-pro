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

	private static final String JSCONTAINERSTART = "(function() {\n";

	private static final String JSCONTAINEREND = "\n})()";

	private static final String JSUNSAFEWINDOW = "unsafeWindow = (function() { var el = document.createElement('p'); el.setAttribute('onclick', 'return window;'); return el.onclick(); }()); window.wrappedJSObject = unsafeWindow;\n";

	private static final String JSMISSINGFUNCTION = "function() { GM_log(\"Called function not yet implemented\"); };\n";

	private static final String JSMISSINGFUNCTIONS = "var GM_info = "
			+ JSMISSINGFUNCTION + "var GM_openInTab = " + JSMISSINGFUNCTION
			+ "var GM_registerMenuCommand = " + JSMISSINGFUNCTION
			+ "var GM_notification = " + JSMISSINGFUNCTION
			+ "var GM_unregisterMenuCommand = " + JSMISSINGFUNCTION
			+ "var GM_setClipboard = " + JSMISSINGFUNCTION;

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
				String defaultSignature = "\""
						+ script.getName().replace("\"", "\\\"") + "\", \""
						+ script.getNamespace().replace("\"", "\\\"")
						+ "\", \"" + secret + "\"";
				String callbackPrefix = ("GM_"
						+ script.getName()
						+ script.getNamespace()
						+ UUID.randomUUID().toString())
						.replaceAll("[^0-9a-zA-Z_]", "");
				String jsApi = JSUNSAFEWINDOW;
				jsApi += "var GM_listValues = function() { return "
						+ jsBridgeName + ".listValues(" + defaultSignature
						+ ").split(\",\"); };\n";
				jsApi += "var GM_getValue = function(name, defaultValue) { return "
						+ jsBridgeName
						+ ".getValue("
						+ defaultSignature
						+ ", name, defaultValue); };\n";
				jsApi += "var GM_setValue = function(name, value) { "
						+ jsBridgeName + ".setValue(" + defaultSignature
						+ ", name, value); };\n";
				jsApi += "var GM_deleteValue = function(name) { "
						+ jsBridgeName + ".deleteValue(" + defaultSignature
						+ ", name); };\n";
				jsApi += "var GM_addStyle = function(css) { "
						+ "var style = document.createElement(\"style\"); "
						+ "style.type = \"text/css\"; style.innerHTML = css; "
						+ "document.getElementsByTagName('head')[0].appendChild(style); };\n";
				jsApi += "var GM_log = function(message) { " + jsBridgeName
						+ ".log(" + defaultSignature + ", message); };\n";
				jsApi += "var GM_getResourceURL = function(resourceName) { return "
						+ jsBridgeName
						+ ".getResourceURL("
						+ defaultSignature
						+ ", resourceName); };\n";
				jsApi += "var GM_getResourceText = function(resourceName) { return "
						+ jsBridgeName
						+ ".getResourceText("
						+ defaultSignature
						+ ", resourceName); };\n";
				
				jsApi += "var GM_xmlhttpRequest = function(details) { \n"
						+ "var sig='_'+Math.ceil(Math.random()*10000)"
							+ "+(''+Date.now()).slice(7)\n"
						+ "var pfx = '"+callbackPrefix+"';\n"
						+ "var key;\n"
						+ "if (details.onabort) { key=sig+'GM_onAbortCallback'+pfx; unsafeWindow[key]=details.onabort; details.onabort=key; }\n"
						+ "if (details.onerror) { key=sig+'GM_onErrorCallback'+pfx; unsafeWindow[key]=details.onerror; details.onerror=key; }\n"
						+ "if (details.onload) { key=sig+'GM_onLoadCallback'+pfx; unsafeWindow[key]=details.onload; details.onload=key; }\n"
						+ "if (details.onprogress) { key=sig+'GM_onProgressCallback'+pfx; unsafeWindow[key]=details.onprogress; details.onprogress=key; }\n"
						+ "if (details.onreadystatechange) { key=sig+'GM_onReadyStateChange'+pfx; unsafeWindow[key]=details.onreadystatechange; details.onreadystatechange=key; }\n"
						+ "if (details.ontimeout) { key=sig+'GM_onTimeoutCallback'+pfx; unsafeWindow[key]=details.ontimeout; details.ontimeout=key; }\n"
						+ "if (details.upload) {\n"
						+ "if (details.upload.onabort) { key=sig+'GM_uploadOnAbortCallback'+pfx; unsafeWindow[key]=details.upload.onabort; details.upload.onabort=key; }\n"
						+ "if (details.upload.onerror) { key=sig+'GM_uploadOnErrorCallback'+pfx; unsafeWindow[key]=details.upload.onerror; details.upload.onerror=key; }\n"
						+ "if (details.upload.onload) { key=sig+'GM_uploadOnLoadCallback'+pfx; unsafeWindow[key]=details.upload.onload; details.upload.onload=key; }\n"
						+ "if (details.upload.onprogress) { key=sig+'GM_uploadOnProgressCallback'+pfx; unsafeWindow[key]=details.upload.onprogress; details.upload.onprogress=key; }\n"
						+ "}\n"
						+ "return JSON.parse("
						+ jsBridgeName
						+ ".xmlHttpRequest("
						+ defaultSignature
						+ ", JSON.stringify(details))); };\n";
				// TODO implement missing functions
				jsApi += JSMISSINGFUNCTIONS;
				jsApi += "var GM_info = {";
				jsApi += "script:{version:\""+script.getVersion().replace("\"", "\\\"")+"\"}";
				jsApi += "};\n";

				// Get @require'd scripts to inject for this script.
				String jsAllRequires = "";
				ScriptRequire[] requires = script.getRequires();
				if (requires != null) {
					for (ScriptRequire currentRequire : requires) {
						CMN.debug("currentRequire::", currentRequire.getContent());
						jsAllRequires += (currentRequire.getContent() + "\n");
					}
				}

                String jsCode = jsApi + jsAllRequires + jsBeforeScript + script
                        .getContent() + jsAfterScript;
                if (!script.isUnwrap()) {
					// todo FIXME java.lang.OutOfMemoryError: Failed to allocate a 16 byte allocation with 1795200 free bytes and 1753KB until OOM
                    jsCode = JSCONTAINERSTART + jsCode + JSCONTAINEREND;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript(jsCode, null);
                } else {
                    view.loadUrl("javascript:\n" + jsCode);
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
