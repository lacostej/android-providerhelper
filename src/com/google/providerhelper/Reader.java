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
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Provides a java-style iterator over objects retrieved from rows of an Android
 * ContentProvider; the objects' class must extend Builder.
 * <p>
 * Here is an example of the use of this class to process Call objects which
 * represent entries in the phone-call log, assuming that Call extends Builder.
 * </p>
 * 
 * <pre>
 * Reader&lt;Call&gt; calls = new Reader&lt;Call&gt;(Call.class, activity,
 *         CallLog.Calls.CONTENT_URI);
 * for (Call call : calls) {
 *     // do something with call
 * }
 * </pre>
 * 
 * @see Builder
 */
public class Reader<T extends Builder> implements Iterator<T>, Iterable<T> {
    private Class<T> rowClass;
    private Method loader;
    private Cursor cursor;
    private boolean moreToCome;

    /**
     * Creates a reader given a class to load, an Activity, and a
     * Content-Provider Uri.
     * 
     * @param row
     *            The class of the object which is to be loaded. Must extend
     *            Builder.
     * @param c
     *            The current context.
     * @param u
     *            The Uri which identifies the Content Provider to load from.
     */
    public Reader(Class<T> rowClass, Context c, Uri u) {
        this.rowClass = rowClass;
        try {
            loader = rowClass.getMethod("load", Cursor.class);
            cursor = c.getContentResolver().query(u, null, null, null, null);
            moreToCome = cursor == null ? false : cursor.moveToFirst();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a reader given a class to load and a Cursor to read from.
     * 
     * @param The
     *            class of the object which is to be loaded. Must extend
     *            Builder.
     * @param The
     *            android.database.Cursor to read from.
     */
    public Reader(Class<T> rowClass, Cursor cursor) {
        this.rowClass = rowClass;
        this.cursor = cursor;
        try {
            loader = rowClass.getMethod("load", Cursor.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        moreToCome = cursor.moveToFirst();
    }

    public boolean hasNext() {
        return moreToCome;
    }

    public T next() {
        try {
            T next = (T) loader.invoke(rowClass.newInstance(), cursor);
            moreToCome = cursor.moveToNext();
            if ( !moreToCome ) {
            	cursor.close();
            }
            return next;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Not implemented.
     */
    public void remove() {
        throw new RuntimeException(
                "Reader does not support removal from Content Providers");
    }

    public Iterator<T> iterator() {
        return this;
    }
}
