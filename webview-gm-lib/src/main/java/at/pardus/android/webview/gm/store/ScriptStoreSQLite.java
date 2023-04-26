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

package at.pardus.android.webview.gm.store;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.pardus.android.webview.gm.BuildConfig;
import at.pardus.android.webview.gm.model.Script;
import at.pardus.android.webview.gm.model.ScriptCriteria;
import at.pardus.android.webview.gm.model.ScriptId;
import at.pardus.android.webview.gm.model.ScriptRequire;
import at.pardus.android.webview.gm.model.ScriptResource;

/**
 * Implements a ScriptStore using an SQLite database to persist user scripts and
 * values.
 * 
 * Uses an LRU cache of user scripts matching URLs and a cache of all available
 * and enabled user script matching criteria to improve performance.
 */
public class ScriptStoreSQLite /*implements ScriptStore*/ {

	private static final String TAG = "fatal "+ScriptStoreSQLite.class.getName();

	private Activity context;

	private ScriptDbHelper dbHelper;

	private ScriptCache cache;
	
	public final HashMap<ScriptCriteria, ScriptCriteria> registryMap = new HashMap<>(1024);
	public final ArrayList<ScriptCriteria> registry = new ArrayList<>(1024);
	

	// @Override
	public ScriptCriteria[] get(String url, boolean enabled, boolean metaOnly) {
		// get matchingScripts to run
		ScriptCriteria[] scripts = cache.get(url);
		if (scripts == null) {
			if (dbHelper == null) {
				Log.w(TAG, "Cannot get user scripts (database not available)");
				return null;
			}
			List<ScriptCriteria> matches = new ArrayList<ScriptCriteria>();
			for (ScriptCriteria c : registry) { // getMatchingScriptIds
				if (!enabled || c.isEnabled()) {
					if (c.testUrl(url)) {
						matches.add(c);
					}
				}
			}
			ScriptCriteria[] matchingIds = matches.toArray(new ScriptCriteria[matches.size()]);
			CMN.debug("matchingIds::", matchingIds);
			CMN.debug(matchingIds);
			//scripts = matchingIds.length==0?null:dbHelper.selectScripts(matchingIds, null, metaOnly);
			cache.put(url, matchingIds);
		}
		return scripts;
	}

	// @Override
	public Script get(ScriptId id) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot get user script (database not available)");
			return null;
		}
		Script[] scripts = dbHelper.selectScripts(new ScriptId[] { id }, null, false);
		if (scripts.length == 0) {
			return null;
		}
		return scripts[0];
	}

	// @Override
	public Script[] getAll(boolean metaOnly) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot get user script (database not available)");
			return null;
		}
        return dbHelper.selectScripts(null, null, metaOnly);
	}

	/** ret -1 : fail   0 : added  1 : edited */
	// @Override
	//@AnyThread
	public int add(Script script) {
		// install or update
		if (dbHelper == null) {
			Log.e(TAG, "Cannot add user script (database not available)");
			return -1;
		}
		long rowId = -1;
		try {
			Cursor cursor = dbHelper.db.rawQuery("select rowid from " + dbHelper.TBL_SCRIPT + " where " + dbHelper.COL_NAME + " = ? AND " + dbHelper.COL_NAMESPACE
							+ " = ? limit 1",
					new String[]{script.getName(), script.getNamespace()});
			if (cursor.moveToNext()) {
				rowId = cursor.getLong(0);
			}
			cursor.close();
		} catch (Exception e) {
			CMN.debug(e);
		}
		//dbHelper.deleteScript(script);
		if (dbHelper.insertScript(script, rowId) != -1) {
			invalidateCache(script, false);
			return rowId == -1 ? 0 : 1;
		} else {
			return -1;
		}
	}

	// @Override
	public void enable(ScriptId id) { // todo 更新从表
		if (dbHelper == null) {
			Log.e(TAG, "Cannot enable user script (database not available)");
			return;
		}
		dbHelper.updateScriptEnabled(id, true);
		invalidateCache(id, false);
	}

	// @Override
	public void disable(ScriptId id) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot disable user script (database not available)");
			return;
		}
		dbHelper.updateScriptEnabled(id, false);
		invalidateCache(id, false);
	}

	// @Override
	public void delete(ScriptId id) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot delete user script (database not available)");
			return;
		}
		dbHelper.deleteScript(id);
		invalidateCache(id, true);
	}

	// @Override
	public String[] getValueNames(ScriptId id) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot get value names (database not available)");
			return null;
		}
		return dbHelper.selectValueNames(id);
	}

	// @Override
	public String getValue(ScriptId id, String name) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot get value (database not available)");
			return null;
		}
		return dbHelper.selectValue(id, name);
	}

	// @Override
	public void setValue(ScriptId id, String name, String value) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot set value (database not available)");
			return;
		}
		dbHelper.updateOrInsertValue(id, name, value);
	}

	// @Override
	public void deleteValue(ScriptId id, String name) {
		if (dbHelper == null) {
			Log.e(TAG, "Cannot delete value (database not available)");
			return;
		}
		dbHelper.deleteValue(id, name);
	}
	
	
	public boolean scriptHasRequire(ScriptId scriptId, String requiredUrl) {
		if (dbHelper == null) {
			return false;
		}
		return dbHelper.scriptHasRequire(scriptId, requiredUrl, true);
	}

	public boolean scriptHasResource(ScriptId scriptId, String resourceName) {
		if (dbHelper == null) {
			return false;
		}
		return dbHelper.scriptHasRequire(scriptId, resourceName, false);
	}

	/**
	 * Creates a new SQLite-backed ScriptStore object.
	 * 
	 * Call open to enable further access.
	 * 
	 * @param context
	 *            the application's context
	 */
	public ScriptStoreSQLite(Activity context) {
		this.context = context;
	}

	/**
	 * Opens access to the database and prepares the cache.
	 * 
	 * Synchronized since this method should not be run on the UI thread.
	 */
	public synchronized void open() {
		if (dbHelper != null) {
			return;
		}
		dbHelper = new ScriptDbHelper(this);
		initCache();
	}

	public synchronized void open(String name) {
		if (dbHelper != null) {
			return;
		}
		dbHelper = new ScriptDbHelper(this, name);
		initCache();
	}

	/**
	 * Closes access to the database.
	 * 
	 * Synchronized since the database may be in the process of being opened in
	 * a different thread.
	 */
	public synchronized void close() {
		File f = new File(dbHelper.db.getPath());
		dbHelper.close();
		dbHelper = null;
		
		if (BuildConfig.DEBUG) {
			try {
				FileInputStream input = new FileInputStream(f);
				
				InputStream b=input; int start=0; int end=-1;
				File path = new File(Environment.getExternalStorageDirectory(), f.getName()+".db");
				try {
					if(start>0)
						b.skip(start);
					File p = path.getParentFile();
					if(!p.exists()) p.mkdirs();
					FileOutputStream fo = new FileOutputStream(path);
					byte[] data = new byte[4096];
					int max;
					if (end>0) max = end-start;
					else max = Integer.MAX_VALUE;
					int total = 0;
					int len;
					while ((len=b.read(data, 0, Math.max(0, Math.min(max-total, 4096))))>0){
						total+=len;
						fo.write(data, 0, len);
					}
					fo.flush();
					fo.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
				CMN.debug(e);
			}
		}
		
	}
	
	private void invalidateCache(ScriptId script, boolean delete) {
		if (Thread.currentThread().getId() == CMN.mid) {
			doInvalidateCache(script, delete);
		} else {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					doInvalidateCache(script, delete);
				}
			});
		}
	}
	
	private void doInvalidateCache(ScriptId script, boolean delete) {
		cache.urlScripts.clear();
		ScriptCriteria stored = registryMap.get(script);
		if (delete) {
			if (stored != null) {
				registryMap.remove(stored);
				stored.release();
			}
		} else {
			ScriptCriteria tmp = dbHelper.getScriptCriteria(stored);
			if (stored == null) {
				registryMap.put(tmp, tmp);
				tmp.runtimeId = registry.size();
				tmp.secret = ""; // todo generate secret key to separate and secure script environs.
				registry.add(tmp);
			} else { // 更新
				stored.rights = tmp.rights;
				stored.setEnabled(tmp.isEnabled());
				tmp = stored;
			}
		}
	}
	
	/**
	 * Creates an empty ScriptCache object and initializes its cache of all
	 * available and enabled user script matching criteria.
	 */
	private void initCache() {
		cache = new ScriptCache();
		cache.setScriptCriteriaArr(dbHelper.selectScriptCriteria(null, null));
	}
	
	/**
	 * Private class to manage the database access.
	 */
	private static class ScriptDbHelper extends SQLiteOpenHelper {

		// V2 added tables for @require and @resource metadata directive.
		private static final int DB_SCHEMA_VERSION_4 = 4;
		private static final int DB_SCHEMA_VERSION_3 = 3;
		private static final int DB_SCHEMA_VERSION_2 = 2;
		private static final int DB_VERSION = DB_SCHEMA_VERSION_4;

		private static final String DB = "webviewgm";

		private static final String TBL_SCRIPT = "script";
		private static final String COL_NAME = "name";
		private static final String COL_NAMESPACE = "namespace";
		private static final String COL_DOWNLOADURL = "downloadurl";
		private static final String COL_UPDATEURL = "updateurl";
		private static final String COL_INSTALLURL = "installurl";
		private static final String COL_DESCRIPTION = "description";
		private static final String COL_ICON = "icon";
		private static final String COL_RUNAT = "runat";
		private static final String COL_UNWRAP = "unwrap";
		private static final String COL_VERSION = "version";
		private static final String COL_CONTENT = "content";
		private static final String COL_ENABLED = "enabled";
		private static final String COL_RIGHTS = "rights";
		private static final String TBL_SCRIPT_CREATE = "CREATE TABLE "
				+ TBL_SCRIPT + " (" + COL_NAME + " TEXT NOT NULL" + ", "
				+ COL_NAMESPACE + " TEXT NOT NULL" + ", " + COL_DESCRIPTION
				+ " TEXT" + ", " + COL_DOWNLOADURL + " TEXT" + ", "
				+ COL_UPDATEURL + " TEXT" + ", " + COL_INSTALLURL + " TEXT"
				+ ", " + COL_ICON + " TEXT" + ", " + COL_RUNAT + " TEXT" + ", "
				+ COL_UNWRAP + " INTEGER" + ", " + COL_VERSION + " TEXT" + ", "
				+ COL_CONTENT + " TEXT NOT NULL" + ", "
				+ COL_ENABLED + " INTEGER NOT NULL DEFAULT 1" + ", "
				+ COL_RIGHTS + " INTEGER NOT NULL DEFAULT 0" + ", "
				+ "PRIMARY KEY (" + COL_NAME + ", " + COL_NAMESPACE + "));";

//		private static final String COL_PATTERN = "pattern";
		
		private static final String COL_PATTERNS = "patterns";
		
		private static final String TBL_MATCH = TBL_SCRIPT + "_has_match";
		private static final String TBL_MATCH_CREATE = "CREATE TABLE "
				+ TBL_MATCH + " (" + COL_NAME + " TEXT NOT NULL" + ", "
				+ COL_NAMESPACE + " TEXT NOT NULL"
				+ ", " + COL_PATTERNS + " TEXT NOT NULL" + ", "
				+ COL_ENABLED + " INTEGER NOT NULL DEFAULT 1" + ", "
				+ COL_RIGHTS + " INTEGER NOT NULL DEFAULT 0" + ", "
				+ "PRIMARY KEY (" + COL_NAME + ", "
				+ COL_NAMESPACE + "), FOREIGN KEY ("
				+ COL_NAME + ", " + COL_NAMESPACE + ") REFERENCES "
				+ TBL_SCRIPT + " (" + COL_NAME + ", " + COL_NAMESPACE
				+ ") ON UPDATE CASCADE ON DELETE CASCADE);";

		private static final String TBL_REQUIRE = TBL_SCRIPT + "_has_require";
		private static final String TBL_REQUIRE_CREATE = "CREATE TABLE IF NOT EXISTS "
				+ TBL_REQUIRE
				+ " ("
				+ COL_NAME
				+ " TEXT NOT NULL"
				+ ", "
				+ COL_NAMESPACE
				+ " TEXT NOT NULL, "
				+ COL_DOWNLOADURL
				+ " TEXT NOT NULL, "
				+ COL_CONTENT
				+ " TEXT NOT NULL, PRIMARY KEY ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ", "
				+ COL_DOWNLOADURL
				+ "), FOREIGN KEY ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ") REFERENCES "
				+ TBL_SCRIPT
				+ " ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ") ON UPDATE CASCADE ON DELETE CASCADE);";

		private static final String TBL_RESOURCE = TBL_SCRIPT + "_has_resource";
		private static final String COL_DATA = "data";
		private static final String COL_RESOURCENAME = "resource_name";
		private static final String TBL_RESOURCE_CREATE = "CREATE TABLE IF NOT EXISTS "
				+ TBL_RESOURCE
				+ " ("
				+ COL_NAME
				+ " TEXT NOT NULL, "
				+ COL_NAMESPACE
				+ " TEXT NOT NULL, "
				+ COL_RESOURCENAME
				+ " TEXT NOT NULL, "
				+ COL_DOWNLOADURL
				+ " TEXT NOT NULL, "
				+ COL_DATA
				+ " BLOB NOT NULL, PRIMARY KEY ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ", "
				+ COL_RESOURCENAME
				+ "), FOREIGN KEY ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ") REFERENCES "
				+ TBL_SCRIPT
				+ " ("
				+ COL_NAME
				+ ", "
				+ COL_NAMESPACE
				+ ") ON UPDATE CASCADE ON DELETE CASCADE);";

		private static final String TBL_VALUE = TBL_SCRIPT + "_has_value";
		private static final String COL_VALUENAME = "valuename";
		private static final String COL_VALUE = "value";
		private static final String TBL_VALUE_CREATE = "CREATE TABLE "
				+ TBL_VALUE + " (" + COL_NAME + " TEXT NOT NULL" + ", " + COL_NAMESPACE + " TEXT NOT NULL"
				+ ", " + COL_VALUENAME + " TEXT NOT NULL"
				+ ", " + COL_VALUE + " TEXT NOT NULL, PRIMARY KEY (" + COL_NAME + ", "
				+ COL_NAMESPACE + ", " + COL_VALUENAME + "), FOREIGN KEY ("
				+ COL_NAME + ", " + COL_NAMESPACE + ") REFERENCES "
				+ TBL_SCRIPT + " (" + COL_NAME + ", " + COL_NAMESPACE
				+ ") ON UPDATE CASCADE ON DELETE CASCADE);";

		private static final String[] COLS_ID = new String[] { COL_NAME,
				COL_NAMESPACE };
		private static final String[] COLS_PATTERN = new String[] { COL_NAME,
				COL_NAMESPACE, COL_PATTERNS };
		private static final String[] COLS_PATTERN_ENABLED = new String[] { COL_NAME,
				COL_NAMESPACE, COL_PATTERNS, COL_ENABLED, COL_RIGHTS };
		private static final String[] COLS_REQUIRE = new String[] { COL_NAME,
				COL_NAMESPACE, COL_DOWNLOADURL, COL_CONTENT };
		private static final String[] COLS_RESOURCE = new String[] { COL_NAME,
				COL_NAMESPACE, COL_DOWNLOADURL, COL_RESOURCENAME, COL_DATA };
		private static final String[] COLS_SCRIPT = new String[] { COL_NAME
				, COL_NAMESPACE, COL_DESCRIPTION, COL_DOWNLOADURL, COL_UPDATEURL
				, COL_INSTALLURL, COL_ICON/*, COL_RUNAT*//*, COL_UNWRAP*/, COL_VERSION
				, COL_ENABLED, COL_RIGHTS, COL_CONTENT };
		private static final String[] COLS_SCRIPT_META = new String[] { COL_NAME
				, COL_NAMESPACE, COL_DESCRIPTION, COL_DOWNLOADURL, COL_UPDATEURL
				, COL_INSTALLURL, COL_ICON/*, COL_RUNAT*//*, COL_UNWRAP*/, COL_VERSION
				, COL_ENABLED, COL_RIGHTS };
		
		private SQLiteDatabase db;
		private final ScriptStoreSQLite scriptStore;

		public ScriptDbHelper(ScriptStoreSQLite scriptStore) {
			super(scriptStore.context, DB, null, DB_VERSION);
			this.scriptStore = scriptStore;
			db = getWritableDatabase();
			db.execSQL("PRAGMA foreign_keys = ON;");
		}
		
		public ScriptDbHelper(ScriptStoreSQLite scriptStore, String pathName) {
			super(scriptStore.context, pathName, null, DB_VERSION);
			this.scriptStore = scriptStore;
			db = getWritableDatabase();
			db.execSQL("PRAGMA foreign_keys = ON;");
		}

		// @Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TBL_SCRIPT_CREATE);
			db.execSQL(TBL_MATCH_CREATE);
			db.execSQL(TBL_VALUE_CREATE);
			db.execSQL(TBL_REQUIRE_CREATE);
			db.execSQL(TBL_RESOURCE_CREATE);
		}

		// @Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database " + DB + " from version "
					+ oldVersion + " to " + newVersion);
			for (int v = oldVersion; v <= newVersion; v++) {
				if (v == DB_SCHEMA_VERSION_2) {
					db.execSQL(TBL_REQUIRE_CREATE);
					db.execSQL(TBL_RESOURCE_CREATE);
				}
				if (v == DB_SCHEMA_VERSION_3) {
					db.execSQL("DROP TABLE IF EXISTS "+TBL_MATCH);
					db.execSQL(TBL_MATCH_CREATE);
				}
				if (v == DB_SCHEMA_VERSION_4) {
					db.execSQL("ALTER TABLE "+TBL_SCRIPT+" ADD COLUMN "+COL_RIGHTS+" INTEGER DEFAULT 0 NOT NULL");
					db.execSQL("ALTER TABLE "+TBL_MATCH+" ADD COLUMN "+COL_RIGHTS+" INTEGER DEFAULT 0 NOT NULL");
				}
			}
		}

		/**
		 * Retrieves scripts including their exclude/include/match criteria from
		 * the database.
		 * 
		 * @param ids
		 *            the script IDs to match; an empty array to match none;
		 *            null to get all
		 * @param enabled
		 *            true to only get enabled scripts; false to only get
		 *            disabled scripts; null to get all
		 * @param metaOnly
		 * @return an array of matching script objects; an empty array if none
		 *         found
		 * todo not 用于获取脚本对应的资源
		 */
		public Script[] selectScripts(ScriptId[] ids, Boolean enabled, boolean metaOnly) {
			// 用于获取需要运行的脚本
			// 用于显示脚本列表
			String selectionStr = null, selectionIdStr = null;
			String[] selectionArgsArr = null, selectionIdArgsArr = null;
			if (ids != null || enabled != null) {
				StringBuilder selection = new StringBuilder();
				List<String> selectionArgs = new ArrayList<String>();
				if (ids != null) {
					if (ids.length == 0) {
						return new Script[0];
					}
					makeScriptIdSelectionArgs(ids, selection, selectionArgs);
					selectionIdStr = selection.toString();
					selectionIdArgsArr = selectionArgs
							.toArray(new String[selectionArgs.size()]);
				}
				if (enabled != null) {
					if (ids != null) {
						selection.insert(0, "(").append(")").append(" AND ");
					}
					selection.append(COL_ENABLED).append(" = ?");
					selectionArgs.add((enabled) ? "1" : "0");
				}
				selectionStr = selection.toString();
				selectionArgsArr = selectionArgs
						.toArray(new String[selectionArgs.size()]);
			}
			// all matches
			// Map<ScriptId, String[]> matches = metaOnly?null:selectPatterns(TBL_MATCH, selectionIdStr, selectionIdArgsArr);
			
			// all requires
			Map<ScriptId, List<ScriptRequire>> requires = metaOnly?null:selectRequires(TBL_REQUIRE, selectionIdStr, selectionIdArgsArr);
			
			// all resources
			// Map<ScriptId, List<ScriptResource>> resources = metaOnly?null:selectResources(TBL_RESOURCE, selectionIdStr, selectionIdArgsArr);
			
			Cursor cursor = db.query(TBL_SCRIPT, metaOnly?COLS_SCRIPT_META:COLS_SCRIPT, selectionStr, selectionArgsArr
					, null, null, null);
			
			Script[] scriptsArr = new Script[cursor.getCount()];
			CMN.debug("select::", scriptsArr.length, selectionStr, selectionArgsArr);
			
			int i = 0;
			while (cursor.moveToNext()) {
				int cc = 0;
				final String name = cursor.getString(cc++); final String namespace = cursor.getString(cc++);
				final ScriptId id = new ScriptId(name, namespace);
				String[] matchArr = null;
				String description = cursor.getString(cc++);
				String downloadurl = cursor.getString(cc++);
				String updateurl = cursor.getString(cc++);
				String installurl = cursor.getString(cc++);
				String icon = cursor.getString(cc++);
				String runat = null;
				int unwrap = 0;
				String version = cursor.getString(cc++);
				boolean bEnable = cursor.getInt(cc++)!=0;
				int rights = cursor.getInt(cc++);
				String content;
				ScriptRequire[] requireArr;
				ScriptResource[] resourceArr = null;
				if (metaOnly) {
					content = null;
					requireArr = null;
				} else {
					List<ScriptRequire> require = requires.get(id);
					requireArr = (require == null) ? null : require
							.toArray(new ScriptRequire[require.size()]);
					content = cursor.getString(cc++);
					//matchArr = matches.get(cc++);
				}
				scriptsArr[i] = new Script(name, namespace, matchArr, description, downloadurl,
						updateurl, installurl, icon, runat, unwrap == 1,
						version, requireArr, resourceArr, bEnable, rights, content);
				
				i++;
			}
			cursor.close();
			return scriptsArr;
		}
		
		
		public ScriptCriteria getScriptCriteria(ScriptId id) {
			Cursor cursor = db.query(TBL_MATCH, COLS_PATTERN_ENABLED, "where name=? and namespace=? limit 1", new String[]{id.getName(), id.getNamespace()}
					, null, null, null);
			int cc=0;
			ScriptCriteria ret = null;
			if (cursor.moveToNext()) {
				String name = cursor.getString(0);
				String namespace = cursor.getString(1);
				boolean enable_ = cursor.getInt(3)==1;
				int rights = cursor.getInt(4);
				ret = new ScriptCriteria(name, namespace, cursor.getString(2).split("\n\0"), enable_, rights);
			}
			cursor.close();
			return ret;
		}
		
		/**
		 * Retrieves script criteria objects from the database.
		 * 
		 * @param ids
		 *            the script IDs to match; an empty array to match none;
		 *            null to get all
		 * @param enabled
		 *            true to only get enabled scripts; false to only get
		 *            disabled scripts; null to get all
		 * @return an array of matching script criteria objects; an empty array
		 *         if none found
		 */
		public ScriptCriteria[] selectScriptCriteria(ScriptId[] ids, Boolean enabled) {
			// 用于预取matcher，一般预取全部脚本的pattern，match后，再判断enable
			String selectionStr = null, selectionIdStr = null;
			String[] selectionArgsArr = null, selectionIdArgsArr = null;
			boolean bEnable = true;
			if (ids != null || enabled != null) {
				// not used
				StringBuilder selection = new StringBuilder();
				List<String> selectionArgs = new ArrayList<String>();
				if (ids != null) {
					if (ids.length == 0) {
						return new ScriptCriteria[0];
					}
					makeScriptIdSelectionArgs(ids, selection, selectionArgs);
					selectionIdStr = selection.toString();
					selectionIdArgsArr = selectionArgs
							.toArray(new String[selectionArgs.size()]);
				}
				if (enabled != null) {
					bEnable = enabled;
					if (ids != null) {
						selection.insert(0, "(").append(")").append(" AND ");
					}
					selection.append(COL_ENABLED).append(" = ?");
					selectionArgs.add((enabled) ? "1" : "0");
				}
				selectionStr = selection.toString();
				selectionArgsArr = selectionArgs.toArray(new String[selectionArgs.size()]);
			}
			else {
				// select all
				Cursor cursor = db.query(TBL_MATCH, COLS_PATTERN_ENABLED, null, null
						, null, null, null);
				ScriptCriteria[] ret = null;
				if(BuildConfig.DEBUG) ret = new ScriptCriteria[cursor.getCount()];
				boolean init = scriptStore.registry.size()==0;
				int cc=0;
				while (cursor.moveToNext()) {
					String name = cursor.getString(0);
					String namespace = cursor.getString(1);
					boolean enable_ = cursor.getInt(3)==1;
					int rights = cursor.getInt(4);
					ScriptCriteria tmp = new ScriptCriteria(name, namespace, cursor.getString(2).split("\n\0"), enable_, rights);
					ScriptCriteria stored = init?null:scriptStore.registryMap.get(tmp);
					if (stored == null) {
						scriptStore.registryMap.put(tmp, tmp);
						tmp.runtimeId = scriptStore.registry.size();
						tmp.secret = ""; // todo generate secret key to separate and secure script environs.
						scriptStore.registry.add(tmp);
					} else {
						stored.rights = tmp.rights;
						stored.setEnabled(tmp.isEnabled());
						tmp = stored;
					}
					if(BuildConfig.DEBUG)  ret[cc++] = tmp;
				}
				cursor.close();
				if(BuildConfig.DEBUG) {
					CMN.debug("get all patterns::", ret.length);
					CMN.debug(ret);
				}
				return ret;
			}
			
			// not used
			Map<ScriptId, String[]> matches = selectPatterns(TBL_MATCH, selectionIdStr, selectionIdArgsArr);
			
			Cursor cursor = db.query(TBL_SCRIPT, COLS_ID, selectionStr, selectionArgsArr
					, null, null, null);
			
			ScriptCriteria[] scriptCriteriaArr = new ScriptCriteria[cursor.getCount()];
			int i = 0;
			while (cursor.moveToNext()) {
				String name = cursor.getString(0); String namespace = cursor.getString(1);
				final ScriptId id = new ScriptId(name, namespace);
				
				String[] matchArr = matches.get(id);
				
				scriptCriteriaArr[i] = new ScriptCriteria(name, namespace, matchArr, bEnable, 0); // todo check is enabled?
				
				i++;
			}
			cursor.close();
			return scriptCriteriaArr;
		}

		/**
		 * Retrieves criteria patterns from the database.
		 * 
		 * @param tblName
		 *            the name of the table to query
		 * @param selection
		 *            the selection string (WHERE part of the query with
		 *            arguments replaced by ?)
		 * @param selectionArgs
		 *            the arguments to use in the selection string
		 * @return matching patterns found in the table mapped to script IDs; an
		 *         empty map if none found
		 */
		private Map<ScriptId, String[]> selectPatterns(String tblName, String selection, String[] selectionArgs) {
			Map<ScriptId, String[]> ret = new HashMap<>();
			Cursor cursor = db.query(tblName, COLS_PATTERN, selection, selectionArgs
					, null, null, null);
			while (cursor.moveToNext()) {
				ScriptId id = new ScriptId(cursor.getString(0), cursor.getString(1));
				ret.put(id, cursor.getString(2).split("\n\0"));
			}
			cursor.close();
			return ret;
		}

		/**
		 * Retrieves require content from the database.
		 *
		 * @param tblName
		 *            the name of the table to query
		 * @param selection
		 *            the selection string (WHERE part of the query with
		 *            arguments replaced by ?)
		 * @param selectionArgs
		 *            the arguments to use in the selection string
		 * @return matching requires found in the table mapped to script IDs; an
		 *         empty map if none found
		 */
		private Map<ScriptId, List<ScriptRequire>> selectRequires(
				String tblName, String selection, String[] selectionArgs) {
			Map<ScriptId, List<ScriptRequire>> contents = new HashMap<ScriptId, List<ScriptRequire>>();
			Cursor cursor = db.query(tblName, COLS_REQUIRE, selection,
					selectionArgs, null, null, null);
			while (cursor.moveToNext()) {
				ScriptId id = new ScriptId(cursor.getString(cursor
						.getColumnIndex(COL_NAME)), cursor.getString(cursor
						.getColumnIndex(COL_NAMESPACE)));
				List<ScriptRequire> content = contents.get(id);
				if (content == null) {
					content = new ArrayList<ScriptRequire>();
					contents.put(id, content);
				}
				String requireUrl = cursor.getString(cursor
						.getColumnIndex(COL_DOWNLOADURL));
				String requireContent = cursor.getString(cursor
						.getColumnIndex(COL_CONTENT));
				content.add(new ScriptRequire(requireUrl, requireContent));
			}
			cursor.close();
			return contents;
		}

		/**
		 * Retrieves resource content from the database.
		 *
		 * @param tblName
		 *            the name of the table to query
		 * @param selection
		 *            the selection string (WHERE part of the query with
		 *            arguments replaced by ?)
		 * @param selectionArgs
		 *            the arguments to use in the selection string
		 * @return matching resources found in the table mapped to script IDs;
		 *         an empty map if none found
		 */
		private Map<ScriptId, List<ScriptResource>> selectResources(
				String tblName, String selection, String[] selectionArgs) {
			Map<ScriptId, List<ScriptResource>> contents = new HashMap<ScriptId, List<ScriptResource>>();
			Cursor cursor = db.query(tblName, COLS_RESOURCE, selection,
					selectionArgs, null, null, null);
			while (cursor.moveToNext()) {
				ScriptId id = new ScriptId(cursor.getString(cursor
						.getColumnIndex(COL_NAME)), cursor.getString(cursor
						.getColumnIndex(COL_NAMESPACE)));
				List<ScriptResource> content = contents.get(id);
				if (content == null) {
					content = new ArrayList<ScriptResource>();
					contents.put(id, content);
				}
				String resourceName = cursor.getString(cursor
						.getColumnIndex(COL_RESOURCENAME));
				String resourceUrl = cursor.getString(cursor
						.getColumnIndex(COL_DOWNLOADURL));
				byte[] resourceData = cursor.getBlob(cursor
						.getColumnIndex(COL_DATA));
				content.add(new ScriptResource(resourceName, resourceUrl,
						resourceData));
			}
			cursor.close();
			return contents;
		}

		/**
		 * Fills the selection string and arguments for queries searching for
		 * script IDs.
		 * 
		 * @param ids
		 *            the script IDs to use as selection arguments (input)
		 * @param selection
		 *            the selection string to fill (output)
		 * @param selectionArgs
		 *            the arguments to use in the selection string (output)
		 */
		private void makeScriptIdSelectionArgs(ScriptId[] ids,
				StringBuilder selection, List<String> selectionArgs) {
			for (ScriptId id : ids) {
				selection.append(" OR (").append(COL_NAME).append(" = ? AND ")
						.append(COL_NAMESPACE).append(" = ?)");
				selectionArgs.add(id.getName());
				selectionArgs.add(id.getNamespace());
			}
			selection.delete(0, 4);
		}

		/**
		 * Inserts a script into the database.
		 * 
		 * @param script
		 *            the script to insert
		 * @return -1 if fail
		 */
		public int insertScript(Script script, long rowId) {
			final ContentValues fieldsId = new ContentValues();
			fieldsId.put(COL_NAME, script.getName());
			fieldsId.put(COL_NAMESPACE, script.getNamespace());
			List<ContentValues> fieldsExcludes = new ArrayList<ContentValues>();
			ContentValues fieldsMatch = null;
			String[] matches = script.getMatch();
			if (matches != null) {
				StringBuilder sb = new StringBuilder();
				for (String pattern : matches) {
					sb.append(pattern);
					sb.append("\n\0");
				}
				fieldsMatch = new ContentValues(fieldsId);
				fieldsMatch.put(COL_PATTERNS, sb.toString());
				fieldsMatch.put(COL_ENABLED, script.isEnabled());
			}
			List<ContentValues> fieldsRequires = new ArrayList<ContentValues>();
			ScriptRequire[] requires = script.getRequires();
			if (requires != null) {
				for (ScriptRequire require : requires) {
					if (require.getContent() != null) {
						ContentValues fieldsRequire = new ContentValues(fieldsId);
						fieldsRequire.put(COL_DOWNLOADURL, require.getUrl());
						fieldsRequire.put(COL_CONTENT, require.getContent());
						fieldsRequires.add(fieldsRequire);
					}
				}
			}
			List<ContentValues> fieldsResources = new ArrayList<ContentValues>();
			ScriptResource[] resources = script.getResources();
			if (resources != null) {
				for (ScriptResource resource : resources) {
					if (resource.getData() != null) {
						ContentValues fieldsResource = new ContentValues(fieldsId);
						fieldsResource.put(COL_RESOURCENAME, resource.getName());
						fieldsResource.put(COL_DOWNLOADURL, resource.getUrl());
						fieldsResource.put(COL_DATA, resource.getData());
						fieldsResources.add(fieldsResource);
					}
				}
			}
			ContentValues fieldsScript = new ContentValues(fieldsId);
			fieldsScript.put(COL_DESCRIPTION, script.getDescription());
			fieldsScript.put(COL_DOWNLOADURL, script.getDownloadurl());
			fieldsScript.put(COL_UPDATEURL, script.getUpdateurl());
			fieldsScript.put(COL_INSTALLURL, script.getInstallurl());
			fieldsScript.put(COL_ICON, script.getIcon());
			fieldsScript.put(COL_RUNAT, script.hasRightToRun());
			fieldsScript.put(COL_UNWRAP, script.hasRightUnwrap());
			fieldsScript.put(COL_VERSION, script.getVersion());
			fieldsScript.put(COL_CONTENT, script.getContent());
			fieldsScript.put(COL_ENABLED, script.isEnabled());
			fieldsScript.put(COL_RIGHTS, script.rights);
			//fieldsScript.put("time", System.currentTimeMillis());
			db.beginTransaction();
			try {
				if(rowId!=-1) {
					if (db.update(TBL_SCRIPT, fieldsScript, "rowid=?", new String[]{rowId+""}) == -1) {
						Log.e(TAG,
								"Error inserting new script into the database (table "
										+ TBL_SCRIPT + ")");
						return -1;
					}
					// todo 编辑脚本后，需要删除不再依赖的resource、require
				} else {
					//fieldsScript.put("create", System.currentTimeMillis());
					if (db.insert(TBL_SCRIPT, null, fieldsScript) == -1) {
						Log.e(TAG,
								"Error inserting new script into the database (table "
										+ TBL_SCRIPT + ")");
						return -1;
					}
				}
				
				if (fieldsMatch!=null) {
					if (db.insertWithOnConflict(TBL_MATCH, null, fieldsMatch, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
						Log.e(TAG,
								"Error inserting new script into the database (table "
										+ TBL_MATCH + ")");
						return -2;
					}
				} else {
					db.delete(TBL_MATCH, COL_NAME + " = ? AND " + COL_NAMESPACE
									+ " = ?", new String[] { script.getName(), script.getNamespace() });
				}
				for (ContentValues fieldsRequire : fieldsRequires) {
					if (db.insertWithOnConflict(TBL_REQUIRE, null, fieldsRequire, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
						Log.e(TAG,
								"Error inserting new script into the database (table "
										+ TBL_REQUIRE + ")");
						return -2;
					}
				}
				for (ContentValues fieldsResource : fieldsResources) {
					if (db.insertWithOnConflict(TBL_RESOURCE, null, fieldsResource, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
						Log.e(TAG,
								"Error inserting new script into the database (table "
										+ TBL_RESOURCE + ")");
						return -2;
					}
				}
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction(); // todo check
			}
			return 0;
		}

		/**
		 * Deletes a script from the database.
		 * 
		 * @param id
		 *            the ID of the script to delete
		 */
		public void deleteScript(ScriptId id) {
			db.beginTransaction();
			try {
				db.delete(TBL_SCRIPT, COL_NAME + " = ? AND " + COL_NAMESPACE
						+ " = ?",
						new String[] { id.getName(), id.getNamespace() });
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}

		/**
		 * Updates the enabled column of a script in the database.
		 * 
		 * @param id
		 *            the ID of the script to update
		 * @param enabled
		 *            the new enabled value
		 */
		public void updateScriptEnabled(ScriptId id, boolean enabled) {
			ContentValues fields = new ContentValues();
			fields.put(COL_ENABLED, (enabled) ? 1 : 0);
			db.beginTransaction();
			try {
				db.update(TBL_SCRIPT, fields, COL_NAME + " = ? AND "
						+ COL_NAMESPACE + " = ?", new String[] { id.getName(),
						id.getNamespace() });
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}

		/**
		 * Retrieves all names of values owned by id.
		 * 
		 * @param id
		 *            the owner script
		 * @return an array of all names or an empty array if none found
		 */
		public String[] selectValueNames(ScriptId id) {
			String selection = COL_NAME + " = ? AND " + COL_NAMESPACE + " = ?";
			String[] selectionArgs = new String[] { id.getName(),
					id.getNamespace() };
			Cursor cursor = db.query(TBL_VALUE, new String[] { COL_VALUENAME },
					selection, selectionArgs, null, null, null);
			ArrayList<String> valueNames = new ArrayList<String>();
			while (cursor.moveToNext()) {
				valueNames.add(cursor.getString(0));
			}
			cursor.close();
			return valueNames.toArray(new String[valueNames.size()]);
		}

		/**
		 * Retrieves the value identified by name owned by id.
		 * 
		 * @param id
		 *            the owner script
		 * @param name
		 *            the key
		 * @return the value belonging to key and script, null if none found
		 */
		public String selectValue(ScriptId id, String name) {
			String selection = COL_NAME + " = ? AND " + COL_NAMESPACE
					+ " = ? AND " + COL_VALUENAME + " = ?";
			String[] selectionArgs = new String[] { id.getName(),
					id.getNamespace(), name };
			Cursor cursor = db.query(TBL_VALUE, new String[] { COL_VALUE },
					selection, selectionArgs, null, null, null);
			try {
				if (cursor.moveToFirst()) {
					return cursor.getString(0); // todo check
				}
			} finally {
				cursor.close();
			}
			return null;
		}
		
		
		/**
		 * Updates or inserts a name/value pair owned by id in the database.
		 * 
		 * @param id
		 *            the owner script
		 * @param name
		 *            the key
		 * @param value
		 *            the updated or new value
		 */
		public void updateOrInsertValue(ScriptId id, String name, String value) {
			db.beginTransaction();
			ContentValues cv = new ContentValues();
			cv.put(COL_VALUE, value);
			try {
				int upd = db.update(TBL_VALUE, cv, COL_NAME + "=? AND " + COL_NAMESPACE
								+ "=? AND " + COL_VALUENAME + "=?"
						, new String[]{id.getName(), id.getNamespace(), name});
				if (upd != 1) {
					cv.put(COL_NAME, id.getName());
					cv.put(COL_NAMESPACE, id.getNamespace());
					cv.put(COL_VALUENAME, name);
					long insert = db.insert(TBL_VALUE, null, cv);
					//CMN.debug("insert::", insert);
					if (insert == -1) {
						Log.e(TAG, "Error inserting new value into the database (table " + TBL_VALUE + ")");
					} else {
						db.setTransactionSuccessful();
					}
				} else {
					db.setTransactionSuccessful();
				}
				//CMN.debug("upd::", upd, id, name, value);
			} finally {
				db.endTransaction();
			}
		}

		/**
		 * Deletes a name/value pair owned by id from the database.
		 * 
		 * @param id
		 *            the owner script
		 * @param name
		 *            the key
		 */
		public void deleteValue(ScriptId id, String name) {
			String selection = COL_NAME + " = ? AND " + COL_NAMESPACE
					+ " = ? AND " + COL_VALUENAME + " = ?";
			String[] selectionArgs = new String[] { id.getName(),
					id.getNamespace(), name };
			db.beginTransaction();
			try {
				db.delete(TBL_VALUE, selection, selectionArgs);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
		
		public boolean scriptHasRequire(ScriptId scriptId, String required, boolean js) {
			Cursor cursor = db.rawQuery("select rowid from " + (js ? TBL_REQUIRE : TBL_RESOURCE) + " where name=? and namespace=? and " + (js ? COL_DOWNLOADURL : COL_RESOURCENAME) + "=? limit 1", new String[]{scriptId.getName(), scriptId.getNamespace()});
			boolean ret = cursor.getCount() > 0;
			cursor.close();
			return ret;
		}
	}

	/**
	 * Cache of user scripts matching most recently accessed URLs and all
	 * available and enabled user script matching criteria.
	 */
	private static class ScriptCache {
		private static final int CACHE_SIZE = 1024;
		private LinkedHashMap<String, ScriptCriteria[]> urlScripts = new LinkedHashMap<String, ScriptCriteria[]>(
				CACHE_SIZE + 2, 1.0f, true) {
			private static final long serialVersionUID = 1L;
			// @Override
			protected boolean removeEldestEntry(
					Entry<String, ScriptCriteria[]> eldest) {
				return size() > CACHE_SIZE;
			}
		};

		private ScriptCriteria[] scriptCriteriaArr;

		/**
		 * Looks if the given URL has a cache of matching user scripts.
		 * 
		 * @param url
		 *            the URL to look up
		 * @return if the URL is cached either the found user scripts or an
		 *         empty array; if the URL is not cached then null
		 */
		public synchronized ScriptCriteria[] get(String url) {
			return urlScripts.get(url);
		}

		/**
		 * Caches a URL and its matching user scripts.
		 * 
		 * @param url
		 *            the URL to cache
		 * @param scripts
		 *            the user scripts to execute at that URL
		 */
		public synchronized void put(String url, ScriptCriteria[] scripts) {
			urlScripts.put(url, scripts);
		}

		/**
		 * Goes through all user script criteria to find all that need to be run
		 * for the given URL.
		 * 
		 * @param url
		 *            the URL to match
		 * @return an array of matching user script IDs; an empty array if none
		 *         matched
		 */
		public ScriptId[] getMatchingScriptIds(String url, boolean enabled) {
			List<ScriptId> matches = new ArrayList<ScriptId>();
			ScriptCriteria[] criteriaArr = scriptCriteriaArr;
			for (ScriptCriteria c : criteriaArr) {
				if (!enabled || c.isEnabled()) {
					if (c.testUrl(url)) {
						matches.add(c);
					}
				}
			}
			return matches.toArray(new ScriptId[matches.size()]);
		}

		/**
		 * Caches the array of user script criteria to be used when matching
		 * URLs.
		 *
		 * @param scriptCriteriaArr
		 *            the array to cache
		 */
		public void setScriptCriteriaArr(ScriptCriteria[] scriptCriteriaArr) {
			this.scriptCriteriaArr = scriptCriteriaArr;
		}

	}

}
