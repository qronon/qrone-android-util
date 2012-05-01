package org.qrone.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.qrone.util.BranchInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public abstract class ImageAsyncTask extends AsyncTask<WebSource, Integer, Bitmap[]>{

	private String path;
	private static DefaultHttpClient dhc;
	
	public ImageAsyncTask(){
		this(null);
	}
	
	public ImageAsyncTask(String path){
		this.path = path;
	}

	public String getCachePath(){
		return path;
	}
	
	@Override
	protected Bitmap[] doInBackground(WebSource... params) {
		int l = params.length;
		Bitmap[] results = new Bitmap[l];
		
		for (int i = 0; i < l; i++) {
			WebSource source = params[i];

			InputStream inf = null;
			if(path != null && source.getFilename() != null)
				inf = MicroSD.loadData(path, source.getFilename());

			try{
				if(inf == null){
						if(dhc == null)
							dhc = new DefaultHttpClient();
						HttpResponse httpResponse = dhc.execute(new HttpGet(source.getURL()));
						if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
							HttpEntity httpEntity = httpResponse.getEntity();
							InputStream in = httpEntity.getContent();
							
							if(source.getFilename() != null){
								OutputStream out = MicroSD.saveData(getCachePath(), source.getFilename());
								in = new BranchInputStream(in, out);
							}
							
							results[i] = memoryProcess(in);
						}
				}else{
					results[i] = memoryProcess(inf);
				}
				
				if(results[i] == null){
					MicroSD.removeData(getCachePath(), source.getFilename());
				}
			}catch(IOException e){
				e.printStackTrace();
			}catch(OutOfMemoryError e){
				e.printStackTrace();
			}
			
			publishProgress(i);
		}
		
		return results;
	}
	
	private static synchronized Bitmap memoryProcess(InputStream in){
		return BitmapFactory.decodeStream(in);
	}
}
