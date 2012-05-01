package org.qrone.android.util;

public class WebSource {
	private String url;
	private String filename;
	
	public String getURL() {
		return url;
	}

	public String getFilename() {
		return filename;
	}

	public WebSource(String url) {
		super();
		this.url = url;
	}
	
	public WebSource(String url, String filename) {
		super();
		this.url = url;
		this.filename = filename;
	}
}
