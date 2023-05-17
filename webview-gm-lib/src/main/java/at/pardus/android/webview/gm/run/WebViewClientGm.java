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

import java.util.Arrays;
import java.util.LinkedHashMap;

import at.pardus.android.webview.gm.model.Script;
import at.pardus.android.webview.gm.model.ScriptCriteria;
import at.pardus.android.webview.gm.model.ScriptRequire;
import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;

/**
 * A user script enabled WebViewClient to be used by WebViewGm.
 */
public class WebViewClientGm extends WebViewClient {

	private static final String TAG = "fatal "+WebViewClientGm.class.getName();

	private static final String JSCONTAINERSTART = "(function() {";

	private static final String JSCONTAINEREND = "\n})()";
	
	private static final boolean bigcake = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

   /**
	unsafeWindow = (function() {
		var el = document.createElement('p');
		el.setAttribute('onclick', 'return window;');
		return el.onclick();
	}());
	window.wrappedJSObject = unsafeWindow;
	var GM_wv={};
	function GM_isInstalled(a,b,c) {
		GM_wv.bg.isInstalled(a,b,c);
	}
	function GM_listValues() {
		return GM_wv.bg.listValues(GM_wv.id, GM_wv.sec).split(",");
	}
	function GM_getValue(name, v) {
		//console.log('GM_getValue', name, v);
		var ret =  GM_wv.bg.getValue(GM_wv.id, GM_wv.sec, name);
		if(ret==undefined) ret=v;
		//console.log('get=', ret);
		if(ret!=undefined && ret!=null) try{ret=JSON.parse(ret)}catch(e){console.log(e)};
		//console.log(new Error());
		return ret;
	}
	function GM_setValue(name, value) {
		//console.log('GM_setValue', name, value);
		//console.log(new Error());
		GM_wv.bg.setValue(GM_wv.id, GM_wv.sec, name, JSON.stringify(value));
	}
	function GM_deleteValue(name) {
		GM_wv.bg.deleteValue(GM_wv.id, GM_wv.sec, name);
	}
	function GM_addStyle(css) {
		var style = document.createElement("style");
		style.type = "text/css";
		style.innerHTML = css;
		document.getElementsByTagName('head')[0].appendChild(style);
	}
	function GM_log(message) {
		GM_wv.bg.log(GM_wv.id, GM_wv.sec, message);
	}
	function GM_getResourceURL(resourceName) {
		return GM_wv.bg.getResourceURL(GM_wv.id, GM_wv.sec, resourceName);
	}
	function GM_getResourceText(resourceName) {
		return GM_wv.bg.getResourceText(GM_wv.id, GM_wv.sec, resourceName);
	}
	function GM_xmlhttpRequest(details) {
		var sig = Math.ceil(Math.random() * 10000) + ('' + Date.now()).slice(7);
		var pfx = GM_wv.hash;
		var key=sig+pfx, he=details, his=unsafeWindow[key]={}, bfx='';
		pfx = '["'+key+'"].';
		his.ondone = function() {
			delete unsafeWindow[key];
			//console.log('done!!!');
		};
		function hook(n, d) {
			if(d) {
				his['d'+n] = he[n];
				he[n] = function(res){ if(his['d'+n]) his['d'+n](res); his.ondone(); };
			}
			if(he[n]) {
				var b = bfx+n;
				his[b] = he[n];
				he[n] = pfx+b;
			}
		}
		hook('ondone');
		hook('onabort');
		hook('onerror', 1);
		hook('onload', 1);
		hook('onprogress');
		hook('onreadystatechange');
		hook('ontimeout');
		he=details.upload;
		bfx = '';
		if (he) {
			hook('onabort');
			hook('onerror', 1);
			hook('onload', 1);
			hook('onprogress');
		}
		return JSON.parse(GM_wv.bg.xmlHttpRequest(GM_wv.id, GM_wv.sec, JSON.stringify(details)));
	}
	function nonimpl(n) {
		GM_log(n+" is not yet implemented");
	}
	function GM_openInTab() {
		nonimpl('GM_openInTab');
	}
	function GM_registerMenuCommand(caption, commandFunc, accessKey) {
		var sigKey = null;
		while(!sigKey || unsafeWindow[sigKey])
			sigKey = 'mn_'+Math.ceil(Math.random() * 10000) + ('' + Date.now()).slice(7);
		unsafeWindow[sigKey] = commandFunc;
		GM_wv.bg.registerMenuCommand(GM_wv.id, GM_wv.sec, caption, sigKey);
		return sigKey;
	}
	function GM_notification() {
		nonimpl('GM_notification');
	}
	function GM_unregisterMenuCommand(id) {
		if(unsafeWindow[id]) {
			delete unsafeWindow[id];
			GM_wv.bg.unregisterMenuCommand(GM_wv.id, GM_wv.sec, id);
		}
	}
	function GM_setClipboard() {
		nonimpl('GM_setClipboard');
	}
	function GM_configDomain(setIfNonSet, options) {
		return GM_wv.bg.configDomain(GM_wv.id, GM_wv.sec, setIfNonSet, options);
	}
	function GM_blockImage(block, reload) {
		return GM_wv.bg.blockImage(GM_wv.id, GM_wv.sec, block, reload);
	}
	function GM_block(substr, pattern, block) {
		return GM_wv.bg.block(GM_wv.id, GM_wv.sec, substr, pattern, block);
	}
	function GM_blockCorsJump(block) {
		return GM_wv.bg.blockCorsJump(GM_wv.id, GM_wv.sec, block);
	}
	function GM_blockJS(block) {
		return GM_wv.bg.blockJS(GM_wv.id, GM_wv.sec, block);
	}
	function hookCallback(cb, b) {
		var sig = Math.ceil(Math.random() * 10000) + ('' + Date.now()).slice(7)
		, pfx = GM_wv.hash
		, key=sig+b+pfx, fn=unsafeWindow[key]=function(a,b){delete unsafeWindow[key]; if(cb)cb(a,b);};
		return key;
	}
	var GM_cookie={
		list : function(details , cb){
			GM_wv.bg.cookieList(GM_wv.id, GM_wv.sec, JSON.stringify(details), hookCallback(cb, 'CL'));
		}
		, set : function(details , cb){
			GM_wv.bg.cookieSet(GM_wv.id, GM_wv.sec, JSON.stringify(details), hookCallback(cb, 'CS'));
		}
		, delete : function(details , cb){
			GM_wv.bg.cookieDelete(GM_wv.id, GM_wv.sec, JSON.stringify(details), hookCallback(cb, 'CD'));
		}
	};
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
		, scriptHandler: 'android-webview-gm'
		, version: 'infinite'
	};*/
	@Metaline()
	private static final String JSGMINFO = "https://wiki.greasespot.net/GM.info";

//	private static final String JSMISSINGFUNCTIONS = "var GM_openInTab = " + JSMISSINGFUNCTION
//			+ "var GM_registerMenuCommand = " + JSMISSINGFUNCTION
//			+ "var GM_notification = " + JSMISSINGFUNCTION
//			+ "var GM_unregisterMenuCommand = " + JSMISSINGFUNCTION
//			+ "var GM_setClipboard = " + JSMISSINGFUNCTION;

	private ScriptStoreSQLite scriptStore;

	private String jsBridgeName;

	private String secret;
	
	private final StringBuilder buffer;
	
	private final  LinkedHashMap<ScriptCriteria, String> bufferScript ;
	
	/**
	 * Constructs a new WebViewClientGm with a scriptStore.
	 *
	 * @param scriptStore
	 *            the script database to query for scripts to run when a page
	 *            starts/finishes loading
	 * @param jsBridgeName
	 *            the variable name to access the webview GM functions from
	 *            javascript code
	 * @param secret
	 *            a random string that is added to calls of the GM API
	 */
	public WebViewClientGm(ScriptStoreSQLite scriptStore, String jsBridgeName,
			String secret) {
		this.scriptStore = scriptStore;
		this.jsBridgeName = jsBridgeName;
		this.secret = secret;
		buffer = scriptStore.buffer;
		bufferScript = scriptStore.bufferedScript;
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
	public ScriptCriteria[] runMatchingScripts(WebView view, String url,
			boolean pageFinished, String jsBeforeScript, String jsAfterScript) {
		if (scriptStore == null) {
			Log.w(TAG, "not running any scripts");
			return null;
		}
		ScriptCriteria[] matchingScripts = scriptStore.get(url, true, false);
		CMN.debug("matchingScripts::", Arrays.toString(matchingScripts));
		if (matchingScripts == null) {
			return matchingScripts;
		}
		if (jsBeforeScript == null) {
			jsBeforeScript = "";
		}
		if (jsAfterScript == null) {
			jsAfterScript = "";
		}
		for (ScriptCriteria key : matchingScripts) {
			//CMN.debug("hasRightRunStart::", key, key.hasRightRunStart(), key.hasRightRunEnd());
			if (key.isEnabled() && (!pageFinished && key.hasRightRunStart() || pageFinished && key.hasRightRunEnd())) {
				//Log.i(TAG, "Running script \"" + key + "\" on " + url);
				String jsCode = bufferScript.get(key);
				if (jsCode == null) {
					Script script = scriptStore.get(key);
					buffer.setLength(0);
					buffer.ensureCapacity(JSUNSAFEWINDOW.length()*3+script.getContent().length());
					boolean unwrap = false;//key.hasRightUnwrap();
					if (!bigcake) {
						buffer.append("javascript:\n");
						unwrap = false;
					}
					if (!unwrap) {
						buffer.append(JSCONTAINERSTART);
					}
					buffer.append(JSUNSAFEWINDOW);
					if (!key.hasRightNone()) {
						key.register();
						buffer.append("GM_wv.n=\"").append(key.getName().replace("\"", "\\\"")).append("\"");
						buffer.append(";GM_wv.ns=\"").append(key.getNamespace().replace("\"", "\\\"")).append("\"");
						buffer.append(";GM_wv.ver=\"").append(script.getVersion().replace("\"", "\\\"")).append("\"");
						buffer.append(";GM_wv.id=\"").append(key.runtimeId).append("\"");
						buffer.append(";GM_wv.sec=\"").append(key.secret).append("\"");
						buffer.append(";GM_wv.bg=").append(jsBridgeName);
						buffer.append(";GM_wv.hash=\"").append(key.hash).append("\"");
						buffer.append(";GM_wv.bg=").append(jsBridgeName)
								.append(";").append(JSGMINFO).append("\n");
					}
					
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
					jsCode = buffer.toString();
					bufferScript.put(key, jsCode);
				}
                if (bigcake) {
                    view.evaluateJavascript(jsCode, null);
                } else {
                    view.loadUrl(jsCode);
                }
			}
		}
		return matchingScripts;
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
	public ScriptStoreSQLite getScriptStore() {
		return scriptStore;
	}

	/**
	 * @param scriptStore
	 *            the scriptStore to set
	 */
	public void setScriptStore(ScriptStoreSQLite scriptStore) {
		this.scriptStore = scriptStore;
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
