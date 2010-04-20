package com.google.providerhelper;

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.provider.CallLog;

/**
 * This was supposed to help me generate the code of the Loader class automatically.
 * 
 * I ran into the following issues:
 * <ul>
 * <li> Cursor doesn't expose the column types
 * <li>one way to access the column types would be to use the CursorWindow class (that uses JNI under the hood). Yet in early Android SDK, this class lacked a lot of is$Type() methods, rendering it unusable for that purpose
 * <li>some types are still not exposed. E.g. isDouble() doesn't exist.
 * </ul>
 * 
 * Some links:
 * http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/database/CursorWindow.java;hb=HEAD
 * http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/jni/CursorWindow.cpp;hb=HEAD
 * 
 *  Potential workarounds:
 *  * use a user supplied mapping of fields, type
 *  * make the code incompatible
 *  * patch the CursorWindow class to support more types (will only be usable on Android > 2.1.x)
 *  * find an alternative ?
 */
public class LoaderGenerator {
	public static String generateLoader(String packageName, String className, Cursor cursor) {

		String[] cols = cursor.getColumnNames();
				
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("package ").append(packageName).append(";\n");
        buffer.append("\n");
        
        buffer.append("import ").append(cursor.getClass().getName()).append(";\n");
        buffer.append("\n");

        buffer.append("public class ").append(className).append(" {\n");
        buffer.append("\t").append("Cursor cursor;\n");
        buffer.append("\n");
        buffer.append("\t").append("public ").append(className).append("(Cursor cursor) {\n");
        buffer.append("\t").append("\t").append("this.cursor = cursor;\n");
        buffer.append("\t").append("}\n");

        for (String col : cols) {
        	String type = "FIXME";
        	String columnIndex = "FIXME";
        	buffer.append("\t").append(type).append(" get")
        	.append(col.substring(0, 1).toUpperCase()).append(col.substring(1))
        	.append("() {\n");
        	buffer.append("\t").append("\t").append("return cursor.get").append(type)
        	.append("(cursor.getColumnIndex(").append(columnIndex).append("));\n");
        	buffer.append("\t").append("}\n");
/*        	
        	  String getPhoneNumber() {
        		    return cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
        		  }
        		  */        	
        }
        
        
        buffer.append("\n");
        
        return buffer.toString();
	}
}
