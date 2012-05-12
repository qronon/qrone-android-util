package org.qrone.util;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class Adapter {
	private static DefaultHttpClient client;
	public static DefaultHttpClient http(){
		if(client == null){
			SchemeRegistry schreg = new SchemeRegistry();
			schreg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schreg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

			HttpParams params = new BasicHttpParams();

			ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(params, schreg);
			client = new DefaultHttpClient(connManager, params);
		}
		
		return client;
	}

}
