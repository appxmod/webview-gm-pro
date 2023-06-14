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

package at.pardus.android.webview.gm.util;

import at.pardus.android.webview.gm.store.CMN;

/**
 * Class offering static functions to compare a script's exclude/include/match
 * criteria with a URL.
 */
public class CriterionMatcher {

	/**
	 * Tests a URL against a criterion (may be a regex or simple glob-type
	 * pattern). Case-insensitive.
	 * 
	 * @param criterion
	 *            the pattern to test against
	 * @param url
	 *            the URL to test
	 * @return true if the URL matches the criterion, false else
	 * @see <tt><a href="http://wiki.greasespot.net/Include_and_exclude_rules">Rules</a></tt>
	 * @see <tt><a href="http://code.google.com/chrome/extensions/match_patterns.html">Match Syntax</a></tt>
	 */
	public static boolean test(String criterion, String url, boolean match) {
		if (criterion.length() == 0) {
			return true;
		}
		criterion = criterion.toLowerCase();
		url = url.toLowerCase();
		if (match) {
			return matchPattern(criterion, url);
		} else {
			if (isRegExp(criterion)) {
				return url.matches(".*" + convertJsRegExp(criterion) + ".*");
			}
			return testGlob(criterion, url);
		}
		//CMN.debug("testGlob", "pattern = [" + criterion + "], url = [" + url + "]", testGlob(criterion, url));
	}
	
	
	
	/** https://www.dre.vanderbilt.edu/~schmidt/android/android-4.0/external/chromium/chrome/common/extensions/docs/match_patterns.html */
	public static boolean matchPattern(String pattern, String url) {
		int schemaIdx_pattern = pattern.indexOf("://");
		int schemaIdx_url = url.indexOf("://");
		if (schemaIdx_pattern>0 && schemaIdx_url>0
				&& (pattern.charAt(0)=='*' || schemaIdx_pattern==schemaIdx_url && url.regionMatches(0, pattern, 0, schemaIdx_pattern) )) {
			// <scheme> := '*' | 'http' | 'https' | 'file' | 'ftp'
			int pathIdx_pattern = pattern.indexOf("/", schemaIdx_pattern+4);
			int pathIdx_url = url.indexOf("/", schemaIdx_url+4);
			int pathPatttern = pathIdx_pattern > 0 ? pathIdx_pattern : pattern.length();
			int pathUrl = pathIdx_url > 0 ? pathIdx_url : url.length();
			boolean matchHost = pattern.charAt(schemaIdx_pattern+3)=='*';
			if(pathPatttern < schemaIdx_pattern+4) { // sanity check
				return false;
			} else if (pathPatttern==schemaIdx_pattern+4) { // 如果 pattern 的 host 只有一个字符
				// matchHost = true;
			} else if (matchHost) {
				matchHost = pattern.charAt(schemaIdx_pattern+4)=='.'; // 如果起始为 *.
				if (matchHost) {
					// *.org also matches greasyfork.org
					matchHost = url.regionMatches(schemaIdx_url+3, pattern, schemaIdx_pattern+5, pathPatttern - (schemaIdx_pattern+5));
					if (!matchHost) {
						int skipSubDomainIdx = url.indexOf(".", schemaIdx_url+3);
						matchHost = url.regionMatches(skipSubDomainIdx, pattern, schemaIdx_pattern+4, pathPatttern - (schemaIdx_pattern+4));
					}
				}
			} else if(pathUrl-schemaIdx_url==pathPatttern-schemaIdx_pattern) {
				matchHost = url.regionMatches(schemaIdx_url+3, pattern, schemaIdx_pattern+3, pathPatttern - (schemaIdx_pattern+3));
			}
			if (matchHost) {
				//System.out.println("matchHost::"+pathIdx_pattern+" "+ pathIdx_url);
				if (pathIdx_pattern > 0 && pathIdx_url > 0) {
					//System.out.println("matchHost::"+" "+url.substring(pathIdx_url)+" "+pattern.substring(pathIdx_pattern));
					boolean matchPath;
					if (pattern.length() <= pathIdx_pattern + 2) {
						// matchPath = true;
						if (pattern.length() < pathIdx_pattern + 2) {
							matchPath = url.length() < pathIdx_url + 2;
						} else {
							matchPath = pattern.charAt(pathIdx_pattern + 1) == '*';
						}
					} else {
						matchPath = testGlob(pattern, pathIdx_pattern + 2, url, pathIdx_url + 2);
						//System.out.println("testGlob::"+" "+ matchPath);
					}
					return matchPath;
				} else {
					if (pathIdx_pattern < 0 || pattern.length() < pathIdx_pattern + 2) {
						return pathIdx_url < 0 || url.length() < pathIdx_url + 2;
					} else {
						return pattern.charAt(pathIdx_pattern + 1) == '*';
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Tests a string against a glob-type pattern (supporting only * and the
	 * escape character \).
	 * 
	 * @param pattern
	 *            the glob pattern
	 * @param str
	 *            the string to match against the pattern
	 * @return true if the string matches the pattern, false else
	 */
	private static boolean testGlob(String pattern, String str) {
		return testGlob(pattern, 0, str, 0);
	}

	/**
	 * Recursively tests a string against a glob-type pattern (supporting only *
	 * and the escape character \).
	 * 
	 * @param pattern
	 *            the glob pattern
	 * @param pInd
	 *            the index in the pattern to start testing from
	 * @param str
	 *            the string to match against the pattern
	 * @param sInd
	 *            the index in the string to start testing from
	 * @return true if the string from the given index to its end matches the
	 *         pattern from the given index to its end, false else
	 */
	public static boolean testGlob(String pattern, int pInd, String str,
			int sInd) {
		int pLen = pattern.length();
		int sLen = str.length();
		while (true) {
			if (pInd == pLen) {
				return sInd == sLen;
			}
			char pChar = pattern.charAt(pInd);
			if (pChar == '*') {
				pInd++;
				if (pInd >= pLen) {
					return true;
				}
				while (true) {
					if (testGlob(pattern, pInd, str, sInd)) {
						return true;
					}
					if (sInd == sLen) {
						return false;
					}
					sInd++;
				}
			}
			if (sInd == sLen) {
				return false;
			}
			if (pChar == '\\') {
				pInd++;
				if (pInd >= pLen) {
					return false;
				}
				pChar = pattern.charAt(pInd);
			}
			if (sInd >= sLen) {
				return false;
			}
			char sChar = str.charAt(sInd);
			if (pChar != sChar) {
				return false;
			}
			pInd++;
			sInd++;
		}
	}

	/**
	 * Converts a JS RegExp to a Java string to be used in pattern matching.
	 * 
	 * @param jsRegExp
	 *            the JS RegExp
	 * @return the JS regular expression as Java-compatible string
	 */
	private static String convertJsRegExp(String jsRegExp) {
        return jsRegExp.substring(1, jsRegExp.length() - 1);
	}

	/**
	 * Tests whether a given string is a JS RegExp.
	 * 
	 * @param str
	 *            the string to test
	 * @return true if the string starts with a / and ends with another /
	 */
	private static boolean isRegExp(String str) {
		return str.length() >= 2 && str.charAt(0)=='/' && str.endsWith("/");
	}

	/**
	 * Private constructor.
	 */
	private CriterionMatcher() {

	}

}
