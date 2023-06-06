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
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
	
	public Script toMetadata() {
		content = null;
		setMatch(null);
		requires = null;
		resources = null;
		return this;
	}
	
	public String getContent() {
		return content;
	}
	
	public static int hashCode(String toHash, int start, int len) {
		int h = 0;
		len = Math.min(toHash.length(), len);
		for (int i = start; i < len; i++) {
			h = 31 * h + Character.toLowerCase(toHash.charAt(i));
		}
		return h;
	}
	
	static TreeMap<String, Integer> allRightsLookup = new TreeMap<>();
	
	
	
	/**
	 * Extracts a script's properties from its string content and creates a new
	 * Script object with the extracted data.
	 * <p>
	 * Should not be run on the UI thread due to possible network activity for
	 * any required scripts or resources.
	 * <p>
	 * Pattern to extract single metadata property taken from Greasemonkey's
	 * parseScript.js (MIT license, Copyright 2004-2007 Aaron Boodman).
	 *
	 * @param scriptStr the string to parse
	 * @param url       the address the script was downloaded from, to derive the
	 *                  script's name/namespace/downloadURL from if those properties
	 *                  are not provided
	 * @return the newly created object or null if the string is not a valid
	 * user script
	 * @see <tt><a href="http://wiki.greasespot.net/Metadata_Block">Metadata Block</a></tt>
	 * @see <tt><a href="https://github.com/greasemonkey/greasemonkey/blob/master/modules/parseScript.js">parseScript.js</a></tt>
	 */
	public static Script parse(String scriptStr, String url, ScriptStoreSQLite scriptStore) {
		CMN.debug("fatal parsing::", url);
		String name = null, nameLocal = null, namespace = null, description = null, downloadurl = null, updateurl = null, installurl = null, icon = null, runAt = null, version = null;
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
		
		final Pattern pattern = Pattern.compile("// @(\\S+)(?:\\s+(.*))?");
		Scanner scanner = new Scanner(scriptStr);
		boolean inMetaBlock = false;
		boolean metaBlockEnded = false;
		ScriptCriteria tmp = new ScriptCriteria(name, namespace, null);
		StringBuilder sb = new StringBuilder(24);
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
				//CMN.debug("propertyName::", propertyName);
				String propertyValue = matcher.groupCount() >= 2 ? matcher.group(2) : null;
				if (propertyValue != null && propertyValue.equals("")) {
					propertyValue = null;
				}
				if (propertyValue != null) {
					if (propertyName.startsWith("name")) {
						if (propertyName.equals("namespace")) {
							namespace = propertyValue;
						} else {
							if (propertyName.equals("name") || name==null) {
								name = propertyValue;
							}
							if (nameLocal==null || isLocaleField(propertyName)) {
								nameLocal = propertyValue;
							}
						}
					} else if (propertyName.startsWith("description")) {
						// parse localized name and description
						if (description==null || isLocaleField(propertyName)) {
							description = propertyValue;
						}
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
					} else if (propertyName.equals("grant")) {
						//CMN.debug("grant::", propertyValue, "GM_xmlhttpRequest".equals(propertyValue));
						int length = propertyValue.length();
						if (length >= 7) {
							if (propertyValue.charAt(0)==71) { // GM_***
								sb.setLength(0);
								for (int i = 3; i < length; i++) {
									sb.append(propertyValue.charAt(i));
								}
								if (allRightsLookup.size()==0) {
									synchronized (allRightsLookup) {
										int cc=7;
										allRightsLookup.put("GM_getValue".substring(3), cc++);
										allRightsLookup.put("GM_setValue".substring(3), cc++);
										allRightsLookup.put("GM_openInTab".substring(3), cc++);
										allRightsLookup.put("GM_registerMenuCommand".substring(3), cc++);
										allRightsLookup.put("GM_deleteValue".substring(3), cc++);
										allRightsLookup.put("GM_listValues".substring(3), cc++);
										allRightsLookup.put("GM_addStyle".substring(3), cc++);
										allRightsLookup.put("GM_cookie".substring(3), cc++);
										allRightsLookup.put("GM_info".substring(3), cc++);
										allRightsLookup.put("GM_notification".substring(3), cc++);
										allRightsLookup.put("GM_unregisterMenuCommand".substring(3), cc++);
										allRightsLookup.put("GM_setClipboard".substring(3), cc++);
										allRightsLookup.put("GM_addValueChangeListener".substring(3), cc++);
										allRightsLookup.put("GM_removeValueChangeListener".substring(3), cc++);
										allRightsLookup.put("GM_getResourceText".substring(3), cc++);
										allRightsLookup.put("GM_getResourceURL".substring(3), cc++);
										allRightsLookup.put("GM_addElement".substring(3), cc++);
										allRightsLookup.put("GM_xmlhttpRequest".substring(3), cc++);
										allRightsLookup.put("GM_download".substring(3), cc++);
										
										cc=31;
										allRightsLookup.put("GM_log".substring(3), cc++);
										allRightsLookup.put("GM_setTab".substring(3), cc++);
										allRightsLookup.put("GM_getTab".substring(3), cc++);
										allRightsLookup.put("GM_getTabs".substring(3), cc++);
										allRightsLookup.put("GM_saveTab".substring(3), cc++);
										allRightsLookup.put("GM_blockImage".substring(3), cc++);
										allRightsLookup.put("GM_blockCorsJump".substring(3), cc++);
										allRightsLookup.put("GM_blockJS".substring(3), cc++);
										
										cc=42;
										allRightsLookup.put("GM_turnOnScreen".substring(3), cc++);
									}
								}
								Integer flagPos = allRightsLookup.get(sb.toString());
								CMN.debug("flagPos::", flagPos, propertyValue);
								if (flagPos!=null) {
									tmp.rights |= 1L<<flagPos;
								}
								if(flagPos==null)
									CMN.debug("!!!xmlhttpRequest::", propertyValue, tmp.hasRightXmlHttpRequest(), "GM_xmlhttpRequest".equals(propertyValue), propertyValue.equals("GM_xmlhttpRequest"));
								if(propertyValue.equals("GM_blockImage"))
									CMN.debug("blockImage::", tmp.hasRightBlockImage());
								if(propertyValue.equals("GM_xmlhttpRequest"))
									CMN.debug("----xmlhttpRequest::!!!", tmp.hasRightXmlHttpRequest());
							} else if(length==4){
								// "none"
								tmp.hasRightNone(true);
							}
						}
					} else if (propertyName.equals("version")) {
						version = propertyValue;
					} else if (propertyName.equals("require")) {
						tmp.hasRightRequire(true);
						String required = DownloadHelper.resolveURL(propertyValue, url);
						ScriptRequire require = new ScriptRequire(required, null);
						if (!requires.contains(require)) {
							requires.add(require);
						}
					} else if (propertyName.equals("resource")) {
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
			for (ScriptRequire req : requires) {
				if (!scriptStore.scriptHasRequire(new ScriptId(name, namespace), req.getUrl())) {
					//CMN.debug("下载::脚本::", req.getUrl());
					req.setContent(DownloadHelper.downloadScript(req.getUrl()));
				}
			}
			requireArr = requires.toArray(new ScriptRequire[requires.size()]);
		}
		if (resources.size() > 0) {
			for (ScriptResource res : resources) {
				if (!scriptStore.scriptHasResource(new ScriptId(name, namespace), res.getName())) {
					//CMN.debug("下载::资源::", res.getName(), res.getUrl());
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
		if (tmp.hasRightToRun() == 0) {
			tmp.hasRightRunEnd(true);
		}
		tmp.needReplaceWindowGM_(scriptStr.indexOf(".GM_")>0);
		CMN.debug("parsed tmp.rights::", tmp.rights, tmp.hasRightRunEnd(), tmp.hasRightXmlHttpRequest(), Arrays.toString(requireArr));
		Script ret = new Script(name, namespace, version, matchArr, connectArr,
				description, downloadurl, updateurl, installurl, icon, runAt,
				unwrap, requireArr, resourceArr, true, tmp.rights, scriptStr);
		if (nameLocal!=null && !nameLocal.equals(name)) {
			ret.nameLocal = nameLocal;
		}
		return ret;
	}
	
	public static String localeStr;
	public static String localeCountryStr;
	
	private static boolean isLocaleField(String propertyName) {
		if (localeStr==null) {
			Locale defaultLocale = Locale.getDefault();
			String languageCode = defaultLocale.getLanguage();
			String countryCode = defaultLocale.getCountry();
			String localeCode = languageCode + "-" + countryCode;
			localeStr = ":"+localeCode;
			localeCountryStr = ":"+countryCode;
		}
		return propertyName.endsWith(localeStr)||propertyName.endsWith(localeCountryStr);
	}
}
