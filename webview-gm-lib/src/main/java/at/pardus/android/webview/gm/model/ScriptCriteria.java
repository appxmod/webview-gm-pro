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

import at.pardus.android.webview.gm.store.CMN;
import at.pardus.android.webview.gm.util.CriterionMatcher;

/**
 * Immutable object containing a user script's matching criteria regarding URLs.
 * 
 * @see <a href="http://wiki.greasespot.net/Metadata_Block">Metadata Block</a>
 */
public class ScriptCriteria extends ScriptId {
	private String[] match;

	public ScriptCriteria(String name, String namespace, String[] match) {
		super(name, namespace);
		this.match = match;
		this.enabled = true;
	}
	
	public ScriptCriteria(String name, String namespace, String[] match, boolean bEnable) {
		super(name, namespace);
		this.match = match;
		this.enabled = bEnable;
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
	
	
	public boolean isEnabled() {
		return enabled;
	}
}
