package com.google.providerhelper;

import android.database.Cursor;

public interface Loader<T> {

	/**
	 * @param cursor
	 *            An android.database.Cursor associated with a Content Provider
	 * @return the object that was loaded up.
	 */
	T load(Cursor cursor, T instance);

}