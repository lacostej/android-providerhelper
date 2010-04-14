/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.providerhelper;

import java.lang.reflect.Method;

import android.database.Cursor;

/**
 * Builds an object using fields extracted from an Android ContentProvider's
 * Cursor object.
 * 
 * <p>
 * To use it, extend this class, and for each ContentProvider column name (e.g.
 * "foo") which you wish to process, provide a method named setFoo(), i.e., the
 * field's name capitalized and prefixed by "set". This method must take one
 * argument, which must be of type int, long, String, float, or double,
 * depending on the type provided by the ContentProvider, which you have to know
 * because the ContentProvider doesn't expose them. If you want to process the
 * _id field, provide a set_id method.
 * </p>
 * 
 * <p>
 * Builder provides a constructor of one argument, an Android.database.Cursor.
 * This extracts fields from the Cursor and for each field xxx, calls your
 * setXxx method if provided, extracting the expected type of argument from the
 * Cursor.
 * </p>
 * 
 * <p>
 * Because of Java constructor inheritance weirdness, if you add any more
 * constructors to your inheritor class, you must ensure that it also has a
 * zero-argument constructor, which however need not do anything.
 * </p>
 * 
 * <p>
 * This class may be used standalone with Cursor objects obtained anyhow, and is
 * also designed to with the associated Reader class.
 * </p>
 * 
 * <p>
 * Here's an example of a small class which stores the phone number and time of
 * a phone call, and can be initialized using a Cursor from the CallLog.Calls
 * Content Provider, which contains "number" and "date" fields.
 * </p>
 * 
 * <pre>
 *  public class Call extends Builder {
 *    String phoneNumber;
 *    String date;
 *    public void setNumber(String number) {
 *      phoneNumber = number;
 *    }
 *    public void setDate(Long date) {
 *     this.date = 
 *       DateUtils.formatDateTime(this, date, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE);
 *    }
 *    public String toString() {
 *     return "Called " + number + " at " + date;
 *    }
 *  }
 *  
 *  // elsewhere
 *  Cursor cursor = activity.managedQuery(CallLog.Calls.CONTENT_URI, ... );
 *  // ... navigate cursor ...
 *  Call call = new Call(cursor); // call.number and call.date are now set
 * </pre>
 * 
 * @see Reader
 */
public abstract class Builder {

    private static Method[] setters = null;
    private static Class<?> currentClass = null;
    private static int[] types = null;
    private static final Class<?>[] classes = { Integer.TYPE, String.class,
            Long.TYPE, Float.TYPE, Double.TYPE };
    private static final int INTEGER_TYPE = 0;
    private static final int STRING_TYPE = 1;
    private static final int LONG_TYPE = 2;
    private static final int FLOAT_TYPE = 3;
    private static final int DOUBLE_TYPE = 4;

    /**
     * Only exists to work around Java introspection awkwardness; probably not
     * useful externally.
     * 
     * @hide
     */
    public Builder() {
    }

    /**
     * Any class which extends Builder may use this as a constructor.
     * 
     * @param cursor
     *            An android.database.Cursor associated with a Content Provider
     */
    public Builder(Cursor cursor) {
        load(cursor);
    }

    /**
     * This is required because java introspection doesn't allow you to call a
     * non-nullary constructor which is is inherited from a superclass. So the
     * introspect code in Reader calls the nullary constructor and then this
     * method to fill in the fields. This is probably not useful externally.
     * 
     * @param cursor
     *            An android.database.Cursor associated with a Content Provider
     * @return the object that was loaded up.
     */
    public Object load(Cursor cursor) {
        findSetters(cursor);

        try {
            for (int i = 0; i < setters.length; i++) {
                if ((setters[i] == null) || cursor.isNull(i))
                    continue;
                switch (types[i]) {
                case STRING_TYPE:
                    setters[i].invoke(this, cursor.getString(i));
                    break;
                case INTEGER_TYPE:
                    setters[i].invoke(this, cursor.getInt(i));
                    break;
                case LONG_TYPE:
                    setters[i].invoke(this, cursor.getLong(i));
                    break;
                case FLOAT_TYPE:
                    setters[i].invoke(this, cursor.getFloat(i));
                    break;
                case DOUBLE_TYPE:
                    setters[i].invoke(this, cursor.getDouble(i));
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    private void findSetters(Cursor cursor) {
        if (getClass() == currentClass)
            return;
        currentClass = getClass();
        String[] cols = cursor.getColumnNames();
        setters = new Method[cols.length];

        // Strictly speaking, "types" is unnecessary. You could look at the
        // Method object and extract
        // the type of the argument and dispatch to the right invoker based on
        // that. But this makes the
        // runtime code a little more compact. Probably premature optimization.
        types = new int[cols.length];

        // for each field, synthesize the name of the setter we're looking for
        for (int i = 0; i < cols.length; i++) {
            String name = "set" + cols[i].substring(0, 1).toUpperCase()
                    + cols[i].substring(1);

            // for each of our supported types, see if we have a setter with the
            // right name and a
            // single argument of that type. First one wins.
            for (int classInd = 0; classInd < classes.length; classInd++) {
                try {
                    Method method = currentClass.getMethod(name,
                            classes[classInd]);

                    // if getMethod() doesn't succeed, it throws an exception
                    setters[i] = method;
                    types[i] = classInd;
                    continue;
                } catch (NoSuchMethodException e) {
                }
            }
        }
    }
}
