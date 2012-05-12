package org.qrone.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.qrone.util.Adapter;
import org.qrone.util.BranchReader;
import org.qrone.util.Stream;

import android.os.AsyncTask;

public abstract class WebAsyncTask extends AsyncTask<WebSource, Integer, String[]> {
	private String path;
	private DefaultHttpClient dhc;

	public WebAsyncTask(){
		this(null);
	}
	
	public WebAsyncTask(String path){
		this(new DefaultHttpClient(), path);
	}
	
	public WebAsyncTask(DefaultHttpClient dhc, String path){
		super();
		this.path = path;
		this.dhc = dhc;
	}
	
	public String getCachePath(){
		return path;
	}

	@Override
	protected String[] doInBackground(WebSource... params) {
		int l = params.length;
		String[] results = new String[l];
		
		
		for (int i = 0; i < l; i++) {
			String url = null;
			String filename = null;
			
			WebSource source = params[i];
			
			InputStream inf = null;
			if(path != null && source.getFilename() != null)
				inf = MicroSD.loadData(path, source.getFilename());
			
			if(inf == null){
				try{
					if(dhc == null)
						dhc = Adapter.http();
					HttpResponse httpResponse = dhc.execute(new HttpGet(source.getURL()));
					if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						HttpEntity httpEntity = httpResponse.getEntity();
						InputStream in = httpEntity.getContent();
						
						
						Reader r = getReader(in, 
								httpResponse.getFirstHeader("Content-Type").getValue());
						
						if(source.getFilename() != null){
							OutputStream out = MicroSD.saveData(getCachePath(), source.getFilename());
							r = new BranchReader(r, new OutputStreamWriter(out, "utf8"));
						}
						
						results[i] = memoryProcess(r, source);
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}else{
				Reader r = null;
				try {
					r = new InputStreamReader(inf, "utf8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				results[i] = memoryProcess(r, source);
			}
			
			publishProgress(i);
		}
		
		
		return results;
	}

    public static Reader getReader(InputStream in, String contentType){
    	String encoding = null;
    	if(contentType != null){
    		int idx = contentType.indexOf("charset=");
			if(idx >= 0){
				encoding = contentType.substring(idx + "charset=".length());
				
			}
    	}else{
    		encoding = "utf8";
    	}
    	
    	Reader r = null;
    	try{
    		r = new InputStreamReader(in, encoding);
    	}catch(UnsupportedEncodingException e){
    		try {
				r = new InputStreamReader(in, "utf8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
    	}
		return r;
    }
    
	protected String memoryProcess(Reader r, WebSource source){
		try {
			return Stream.read(r);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected abstract void onPostExecute(String[] result);
}
