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

import android.text.TextUtils;

import org.knziha.metaline.Metaline;

import java.util.Objects;
import java.util.UUID;

import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.util.CriterionMatcher;

/**
 * Immutable object containing a user script's matching criteria regarding URLs.
 * 
 * @see <a href="http://wiki.greasespot.net/Metadata_Block">Metadata Block</a>
 */
public class ScriptCriteria extends ScriptId {
	private String[] match;
	private String[] connect;
	protected boolean enabled;
	public long rights;
	public String secret;
	public int runtimeId;
	public String hash;
	public long rowID;
	
	public ScriptCriteria(String name, String namespace, String[] match) {
		super(name, namespace);
		this.match = match;
		this.enabled = true;
	}
	
	public ScriptCriteria(String name, String namespace, String[] match, boolean bEnable, long rights) {
		super(name, namespace);
		this.match = match;
		this.enabled = bEnable;
		this.rights = rights;
	}

	/**
	 * Checks if a URL matches the criteria of this object.
	 * 
	 * @param url
	 *            the URL to test
	 * @return true if the URL does not match any of the exclude patterns and
	 *         does match one of the patterns in include or match (or include
	 *         and match do not contain any patterns), false else
	 */
	public boolean testUrl(String url) {
		boolean matched = false;
		if (match != null) {
			//CMN.debug("match::", match);
			CMN.debug(match);
			for (int i = 0; i < match.length-1; i+=2) {
				String type = match[i];
				String pattern = match[i+1];
				//CMN.debug("testUrl::", type, pattern, CriterionMatcher.test(pattern, url));
				if ("=".equals(type) || "+".equals(type)) {
					if (!matched) {
						matched = CriterionMatcher.test(pattern, url);
					}
				} else {
					//if ("!".equals(type))
					if(CriterionMatcher.test(pattern, url)) {
						return false;
					}
				}
			}
		}
		return matched;
	}

	public String[] getMatch() {
		return match;
	}
	
	public void setMatch(String[] match) {
		this.match = match;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		return (enabled?"":"//")+name + "@" + namespace;
	}
	
	static int Z = 0;
	@Metaline(flagPos=7) public void hasRightGetValue(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=7) public boolean hasRightGetValue(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=8) public void hasRightSetValue(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=8) public boolean hasRightSetValue(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=9) public void hasRightOpenInTab(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=9) public boolean hasRightOpenInTab(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=10) public void hasRightRegisterMenuCommand(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=10) public boolean hasRightRegisterMenuCommand(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=11) public void hasRightDeleteValue(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=11) public boolean hasRightDeleteValue(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=12) public void hasRightListValues(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=12) public boolean hasRightListValues(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=13) public void hasRightAddStyle(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=13) public boolean hasRightAddStyle(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=14) public void hasRightCookie(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=14) public boolean hasRightCookie(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=15) public void hasRightInfo(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=15) public boolean hasRightInfo(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=16) public void hasRightNotification(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=16) public boolean hasRightNotification(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=17) public void hasRightUnregisterMenuCommand(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=17) public boolean hasRightUnregisterMenuCommand(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=18) public void hasRightSetClipboard(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=18) public boolean hasRightSetClipboard(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=19) public void hasRightAddValueChangeListener(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=19) public boolean hasRightAddValueChangeListener(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=20) public void hasRightRemoveValueChangeListener(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=20) public boolean hasRightRemoveValueChangeListener(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=21) public void hasRightGetResourceText(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=21) public boolean hasRightGetResourceText(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=22) public void hasRightGetResourceURL(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=22) public boolean hasRightGetResourceURL(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=23) public void hasRightAddElement(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=23) public boolean hasRightAddElement(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=24) public void hasRightXmlHttpRequest(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=24) public boolean hasRightXmlHttpRequest(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=25) public void hasRightDownload(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=25) public boolean hasRightDownload(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=26) public void hasRightRunStart(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=26) public boolean hasRightRunStart(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=27) public void hasRightRunEnd(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=27) public boolean hasRightRunEnd(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=26, flagSize=2) public int hasRightToRun(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=28) public void hasRightUnwrap(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=28) public boolean hasRightUnwrap(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=29) public void hasRightResource(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=29) public boolean hasRightResource(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=30) public void hasRightRequire(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=30) public boolean hasRightRequire(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=31) public void hasRightLog(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=31) public boolean hasRightLog(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=31) public void hasRightNone(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=31) public boolean hasRightNone(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=32) public void hasRightSetTab(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=32) public boolean hasRightSetTab(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=33) public void hasRightGetTab(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=33) public boolean hasRightGetTab(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=34) public void hasRightGetTabs(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=34) public boolean hasRightGetTabs(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=35) public void hasRightSaveTab(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=35) public boolean hasRightSaveTab(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=36) public void hasRightBlockImage(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=36) public boolean hasRightBlockImage(){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=37) public void hasRightBlockCorsJump(boolean val){ rights|=Z; throw new RuntimeException(); }
	@Metaline(flagPos=37) public boolean hasRightBlockCorsJump(){ rights|=Z; throw new RuntimeException(); }
	
	public void release() {
		this.name = null;
		this.namespace = null;
		this.secret = null;
		this.match = null;
		this.hash = null;
		this.enabled = false;
		this.rights = 0;
	}
	
	public void register() {
		if (secret==null) {
			hash = (name+namespace).replaceAll("[^0-9a-zA-Z_]", "");
			secret = UUID.randomUUID().toString();
			CMN.debug("registered::secret::", runtimeId, secret, hash, this);
			// todo retrieve connects on the run
		}
	}
}
