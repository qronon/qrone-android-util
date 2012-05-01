package org.qrone.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

public class MicroSD {
	private static final String TAG = "Util";
	private static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

	public static Uri addImageAsApplication(ContentResolver cr, String name,
			long dateTaken, String directory, String filename, Bitmap source,
			byte[] jpegData) {

		//saveData(directory, filename, jpegData);

		String filePath = directory + "/" + filename;
		ContentValues values = new ContentValues(7);
		values.put(Images.Media.TITLE, name);
		values.put(Images.Media.DISPLAY_NAME, filename);
		values.put(Images.Media.DATE_TAKEN, dateTaken);
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.DATA, filePath);
		return cr.insert(IMAGE_URI, values);
	}
	
	public static InputStream loadData(String directory, String filename) {
		try {
			File file = new File(Environment.getExternalStorageDirectory() + "/" + directory, filename);
			if(file.exists()){
				return new FileInputStream(file);
			}
			
		} catch (IOException ex) {
			Log.w(TAG, ex);
		}
		
		return null;
	}

	public static OutputStream saveData(String directory, String filename) {
		try {
			File dir = new File(Environment.getExternalStorageDirectory(), directory);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File file = new File(Environment.getExternalStorageDirectory() + "/" + directory, filename);
			if (file.createNewFile()) {
				return new FileOutputStream(file);
			}

		} catch (FileNotFoundException ex) {
			Log.w(TAG, ex);
		} catch (IOException ex) {
			Log.w(TAG, ex);
		}
		return null;
		
	}
	
	public static boolean removeData(String directory, String filename) {
		File dir = new File(Environment.getExternalStorageDirectory(), directory);
		if (!dir.exists()) {
			return false;
		}
		File file = new File(Environment.getExternalStorageDirectory() + "/" + directory, filename);
		if (file.exists()) {
			return file.delete();
		}
		return false;
	}
}
