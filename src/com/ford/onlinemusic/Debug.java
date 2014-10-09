package com.ford.onlinemusic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import android.util.Log;
/*
 * Debug utils
 * Written by Kyle
 */
public class Debug {
	
	public static String TAG = "DEBUG";
	public static boolean DEBUG = true;
	
	public static void setDebugable(boolean enable){
		DEBUG = enable;
	}
	
	public static void setTAG(String tag){
		TAG = tag;
	}
	public static void LogFunc(){
		
	}
	public static String _FUNC_() {
		StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
		return traceElement.getMethodName() + "\n";
	}

	public static String FormatStackTrace(Throwable throwable) {
		if (throwable == null)
			return "";
		String rtn = throwable.getStackTrace().toString();
		try {
			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			throwable.printStackTrace(printWriter);
			printWriter.flush();
			writer.flush();
			rtn = writer.toString();
			printWriter.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception ex) {
		}
		return rtn + "\n";
	}
	public static void DebugLog(String str){
		if (DEBUG){
			Log.i(TAG, str);
		}
	}
}
