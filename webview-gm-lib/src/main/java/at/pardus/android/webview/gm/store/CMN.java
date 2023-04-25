package at.pardus.android.webview.gm.store;

import android.content.res.Resources;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;


//common
public class CMN {
    public final static String replaceReg =  " |:|\\.|,|-|\'|(|)";
    public final static String emptyStr = "";
    public static final HashMap<String, String> AssetMap = new HashMap<>();
	public static final Boolean OccupyTag = true;
	
	public static int GlobalPageBackground = 0;
	public static int MainBackground = 0;
	public static int FloatBackground;
	public static boolean touchThenSearch=true;
	public static int actionBarHeight;
	public static int lastFavorLexicalEntry = -1;
	public static int lastHisLexicalEntry = -1;
	public static int lastFavorLexicalEntryOff = 0;
	public static int lastHisLexicalEntryOff = 0;
    //static Boolean module_set_invalid = true;
	//public static dictionary_App_Options opt;
	//public static LayoutInflater inflater;
	//protected static ViewPager viewPager;
	public static int dbVersionCode = 2;
	public static long FloatLastInvokerTime=-1;
	public static int ShallowHeaderBlue;
	
	public static long stst;
	public static long ststrt;
	public static long stst_add;
	public static boolean testing;
	public static Resources mResource;
	public static int browserTaskId;
	public static long mid;
	public static TextPaint mTextPainter;
	
	public static void rt(Object... o) {
		ststrt = System.currentTimeMillis();
		Log(o);
	}
	public static void pt(Object...args) {
		CMN.Log(Arrays.toString(args)+" "+(System.currentTimeMillis()-ststrt));
	}
	
	public static void debug(Object... o) {
		Log(o);
	}
	
	public static void Log(Object... o) {
		StringBuilder msg = new StringBuilder();
		if (o != null) {
			Object[] var2 = o;
			int var3 = o.length;
			
			for(int var4 = 0; var4 < var3; ++var4) {
				Object o1 = var2[var4];
				if (o1 != null) {
					if (o1 instanceof Throwable) {
						ByteArrayOutputStream s = new ByteArrayOutputStream();
						PrintStream p = new PrintStream(s);
						((Throwable)o1).printStackTrace(p);
						msg.append(s.toString());
					} else {
						if (o1 instanceof int[]) {
							msg.append(Arrays.toString((int[])o1));
							continue;
						}
						
						if (o1 instanceof String[]) {
							msg.append(Arrays.toString((Object[])o1));
							continue;
						}
						
						if (o1 instanceof short[]) {
							msg.append(Arrays.toString((short[])o1));
							continue;
						}
						
						if (o1 instanceof byte[]) {
							msg.append(Arrays.toString((byte[])o1));
							continue;
						}
						
						if (o1 instanceof long[]) {
							msg.append(Arrays.toString((long[])o1));
							continue;
						}
					}
				}
				
				if (msg.length() > 0) {
					msg.append(", ");
				}
				
				msg.append(o1);
			}
		}
		
		if (testing) {
			System.out.println(msg.toString());
		} else {
			Log.d("fatal poison", msg.toString());
		}
		
	}
}