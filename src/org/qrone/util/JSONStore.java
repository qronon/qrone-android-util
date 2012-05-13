package org.qrone.util;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONStore {
	private String baseurl;
	
	public JSONStore(String host){
		baseurl = "http://" + host;
	}


	public JSONArray list(String table, Map<String, String> filter, int length, String sort, boolean direction)
			 throws IOException, JSONException{
		QueryString q = new QueryString();
		
		for (String key : filter.keySet()) {
			q.add(key, filter.get(key));
		}
		
		if(length > 0)
			q.add(".length", String.valueOf(length));
		if(sort != null){
			if(direction){
				q.add(".order", sort);
			}else{				
				q.add(".order", "-" + sort);
			}
		}
		
		JSONObject obj = httpget(baseurl + "/" + table + "/list?" + q.toString());
		return obj.getJSONArray("list");
	}
	
	public JSONObject httpget(String url) throws IOException, JSONException{
		DefaultHttpClient client = Adapter.http();
		HttpResponse res = client.execute(new HttpGet(url));
		return new JSONObject(new String(Stream.read(res.getEntity().getContent()),"utf8"));
	}
	
	
}
