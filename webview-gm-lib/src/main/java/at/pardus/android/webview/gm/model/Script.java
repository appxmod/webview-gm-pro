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

package at.pardus.android.webview.gm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.store.ScriptStoreSQLite;
import at.pardus.android.webview.gm.util.DownloadHelper;

/**
 * Immutable object describing all sections of a user script. The class includes
 * a static function to create a new Script object from a string.
 */
public class Script extends ScriptMetadata {

	private String content;

	public Script(String name, String namespace, String version, String[] match, String[] connect, String description,
			String downloadurl, String updateurl, String installurl,
			String icon, String runAt, boolean unwrap,
			ScriptRequire[] requires, ScriptResource[] resources
			, boolean bEnable, long rights, String content) {
		super(name, namespace, match, description,
				downloadurl, updateurl, installurl, icon, runAt, unwrap,
				version, requires, resources, bEnable);
		this.content = content;
		this.rights = rights;
		this.connect = connect;
	}
	
	public Script toMetadata(){
		content = null;
		setMatch(null);
		requires = null;
		resources = null;
		return this;
	}
	
	public String getContent() {
		return content;
	}

	/**
	 * Extracts a script's properties from its string content and creates a new
	 * Script object with the extracted data.
	 * 
	 * Should not be run on the UI thread due to possible network activity for
	 * any required scripts or resources.
	 * 
	 * Pattern to extract single metadata property taken from Greasemonkey's
	 * parseScript.js (MIT license, Copyright 2004-2007 Aaron Boodman).
	 * 
	 * @param scriptStr
	 *            the string to parse
	 * @param url
	 *            the address the script was downloaded from, to derive the
	 *            script's name/namespace/downloadURL from if those properties
	 *            are not provided
	 * @return the newly created object or null if the string is not a valid
	 *         user script
	 * @see <tt><a href="http://wiki.greasespot.net/Metadata_Block">Metadata Block</a></tt>
	 * @see <tt><a href="https://github.com/greasemonkey/greasemonkey/blob/master/modules/parseScript.js">parseScript.js</a></tt>
	 */
	public static Script parse(String scriptStr, String url, ScriptStoreSQLite scriptStore) {
		CMN.debug("fatal parsing::", url);
		String name = null, namespace = null, description = null, downloadurl = null, updateurl = null, installurl = null, icon = null, runAt = null, version = null;
		boolean unwrap = false;
		if (url != null) {
			int filenameStart = url.lastIndexOf("/") + 1;
			if (filenameStart != 0 && filenameStart != url.length()) {
				name = url.substring(filenameStart).replace(".user.js", "");
			}
			int hostStart = url.indexOf("://");
			if (hostStart != -1) {
				hostStart += 3;
				int hostEnd = url.indexOf("/", hostStart);
				if (hostEnd != -1) {
					namespace = url.substring(hostStart, hostEnd);
				}
			}
			downloadurl = url;
			updateurl = downloadurl;
		}
		ArrayList<String> match = new ArrayList<>();
		ArrayList<String> connect = new ArrayList<>();
		ArrayList<ScriptRequire> requires = new ArrayList<>();
		ArrayList<ScriptResource> resources = new ArrayList<>();

		Pattern pattern = Pattern.compile("// @(\\S+)(?:\\s+(.*))?");
		Scanner scanner = new Scanner(scriptStr);
		boolean inMetaBlock = false;
		boolean metaBlockEnded = false;
		ScriptCriteria tmp = new ScriptCriteria(name, namespace, null);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!inMetaBlock) {
				if (line.startsWith("// ==UserScript==")) {
					inMetaBlock = true;
				}
				continue;
			}
			if (line.startsWith("// ==/UserScript==")) {
				metaBlockEnded = true;
				break;
			}
			line = line.trim();
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				String propertyName = matcher.group(1);
				String propertyValue = matcher.groupCount()>=2?matcher.group(2):null;
				if (propertyValue != null && propertyValue.equals("")) {
					propertyValue = null;
				}
				if (propertyValue != null) {
					if (propertyName.equals("name")) {
						name = propertyValue;
					} else if (propertyName.equals("namespace")) {
						namespace = propertyValue;
					} else if (propertyName.equals("description")) { // todo parse localized name and description
						description = propertyValue;
					} else if (propertyName.equals("downloadURL")) {
						downloadurl = propertyValue;
					} else if (propertyName.equals("updateURL")) {
						updateurl = propertyValue;
					} else if (propertyName.equals("installURL")) {
						installurl = propertyValue;
					} else if (propertyName.equals("icon")) {
						icon = DownloadHelper.resolveURL(propertyValue, url);
					} else if (propertyName.equals("run-at")) {
						if (propertyValue.equals(RUNATSTART)) {
							tmp.hasRightRunStart(true);
						}
						if (propertyValue.equals(RUNATEND)) {
							tmp.hasRightRunEnd(true);
						}
					}
					else if (propertyName.equals("grant")) {
						//CMN.debug("grant::", propertyValue, "GM_xmlhttpRequest".equals(propertyValue));
						switch (propertyValue) {
							case "GM_getValue":
								tmp.hasRightGetValue(true);
							break;
							case "GM_setValue":
								tmp.hasRightSetValue(true);
							break;
							case "GM_openInTab":
								tmp.hasRightOpenInTab(true);
							break;
							case "GM_registerMenuCommand":
								tmp.hasRightRegisterMenuCommand(true);
							break;
							case "GM_deleteValue":
								tmp.hasRightDeleteValue(true);
							break;
							case "GM_listValues":
								tmp.hasRightListValues(true);
							break;
							case "GM_addStyle":
								tmp.hasRightAddStyle(true);
							break;
							case "GM_info":
								tmp.hasRightInfo(true);
							break;
							case "GM_notification":
								tmp.hasRightNotification(true);
							break;
							case "GM_unregisterMenuCommand":
								tmp.hasRightUnregisterMenuCommand(true);
							break;
							case "GM_setClipboard":
								tmp.hasRightSetClipboard(true);
							break;
							case "GM_addValueChangeListener":
								tmp.hasRightAddValueChangeListener(true);
							break;
							case "GM_removeValueChangeListener":
								tmp.hasRightRemoveValueChangeListener(true);
							break;
							case "GM_getResourceText":
								tmp.hasRightGetResourceText(true);
							break;
							case "GM_getResourceURL":
								tmp.hasRightGetResourceURL(true);
							break;
							case "GM_addElement":
								tmp.hasRightAddElement(true);
							break;
							case "GM_xmlhttpRequest":
								tmp.hasRightXmlHttpRequest(true);
								//CMN.debug("----GM_xmlhttpRequest::!!!", tmp.hasRightXmlHttpRequest());
							break;
							case "GM_download":
								tmp.hasRightDownload(true);
							break;
							case "GM_log":
								tmp.hasRightLog(true);
							break;
							case "none":
								tmp.hasRightNone(true);
							break;
							case "GM_cookie":
								tmp.hasRightCookie(true);
							break;
							case "GM_getTab":
								tmp.hasRightGetTab(true);
							break;
							case "GM_saveTab":
								tmp.hasRightSaveTab(true);
							break;
							case "GM_getTabs":
								tmp.hasRightGetTabs(true);
							break;
							case "GM_blockImage":
								tmp.hasRightBlockImage(true);
								CMN.debug("GM_blockImage::", tmp.hasRightBlockImage());
							break;
							case "GM_blockCorsJump":
								tmp.hasRightBlockCorsJump(true);
							break;
							case "GM_blockJS":
								tmp.hasRightBlockJS(true);
							break;
//							default:
//								CMN.debug("!!!GM_xmlhttpRequest::", propertyValue, tmp.hasRightXmlHttpRequest(), "GM_xmlhttpRequest".equals(propertyValue), propertyValue.equals("GM_xmlhttpRequest"));
//							break;
						}
					}
					else if (propertyName.equals("version")) {
						version = propertyValue;
					}
					else if (propertyName.equals("require")) {
						tmp.hasRightRequire(true);
						String required = DownloadHelper.resolveURL(propertyValue, url);
						ScriptRequire require = new ScriptRequire(required, null);
						if (!requires.contains(require)) {
							requires.add(require);
						}
					}
					else if (propertyName.equals("resource")) {
						tmp.hasRightResource(true);
						Pattern resourcePattern = Pattern.compile("(\\S+)\\s+(.*)");
						Matcher resourceMatcher = resourcePattern.matcher(propertyValue);
						if (!resourceMatcher.matches()) {
							CMN.debug("fatal parsing::", "!resourceMatcher.matches()");
							return null;
						}
						String required = resourceMatcher.group(1);
						ScriptResource resource = new ScriptResource(required, DownloadHelper.resolveURL(resourceMatcher.group(2), url), null);
						if (!resources.contains(resource)) {
							resources.add(resource);
						}
					} else if (propertyName.equals("exclude")) {
						match.add("!");
						match.add(propertyValue);
					} else if (propertyName.equals("include")) {
						match.add("+");
						match.add(propertyValue);
					} else if (propertyName.equals("match")) {
						match.add("=");
						match.add(propertyValue);
					} else if (propertyName.equals("connect")) {
						connect.add(propertyValue);
					}
				}
				if (propertyName.equals("unwrap")) {
					unwrap = true;
				}
			}
		}
		//CMN.debug("parsing::", metaBlockEnded+" "+name+" "+namespace);
		if (!metaBlockEnded) {
			return null;
		}
		if (name == null || namespace == null) {
			return null;
		}
		String[] matchArr = null;
		String[] connectArr = null;
		ScriptRequire[] requireArr = null;
		ScriptResource[] resourceArr = null;
		if (requires.size() > 0) {
			for (ScriptRequire req:requires) {
				if (!scriptStore.scriptHasRequire(new ScriptId(name, namespace), req.getUrl())) {
					CMN.debug("下载::脚本::", req.getUrl());
					req.setContent(DownloadHelper.downloadScript(req.getUrl()));
				}
			}
			requireArr = requires.toArray(new ScriptRequire[requires.size()]);
		}
		if (resources.size() > 0) {
			for (ScriptResource res:resources) {
				if (!scriptStore.scriptHasResource(new ScriptId(name, namespace), res.getName())) {
					CMN.debug("下载::资源::", res.getName(), res.getUrl());
					res.setData(DownloadHelper.downloadBytes(res.getUrl()));
				}
				//CMN.debug("scriptStore.scriptHasResource::", scriptStore.scriptHasResource(new ScriptId(name, namespace), res.getName()));
			}
			resourceArr = resources.toArray(new ScriptResource[resources.size()]);
		}
		if (match.size() > 0) {
			matchArr = match.toArray(new String[match.size()]);
		}
		if (connect.size() > 0) {
			connectArr = connect.toArray(new String[connect.size()]);
		}
		if (tmp.hasRightToRun()==0) {
			tmp.hasRightRunEnd(true);
		}
		CMN.debug("parsed tmp.rights::", tmp.rights, tmp.hasRightRunEnd(), tmp.hasRightXmlHttpRequest(), Arrays.toString(requireArr));
		return new Script(name, namespace, version, matchArr, connectArr,
				description, downloadurl, updateurl, installurl, icon, runAt,
				unwrap, requireArr, resourceArr, true, tmp.rights, scriptStr);
	}
}
