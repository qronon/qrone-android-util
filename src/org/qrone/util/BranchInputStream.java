package org.qrone.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BranchInputStream extends InputStream {
	private InputStream in;
	private OutputStream[] out;
	
	public BranchInputStream(InputStream in, OutputStream... out){
		this.in = in;
		
		int c = out.length;
		for (int i = 0; i < out.length; i++) {
			if(out[i] == null) c--;
		}
		
		OutputStream[] o = new OutputStream[c];
		int j = 0;
		for (int i = 0; i < out.length; i++) {
			if(out[i] != null){
				o[j] = out[i];
				j++;
			}
		}
		
		this.out = o;
	}

	@Override
	public int available() throws IOException{
		return in.available();
	}

	@Override
	public void close() throws IOException{
		int b;
		byte[] buf = new byte[1024];
		while((b = in.read(buf)) > 0){
			for (int i = 0; i < out.length; i++) {
				out[i].write(buf, 0, b);
			}
		}
		
		in.close();
		for (int i = 0; i < out.length; i++) {
			out[i].close();
		}
	}

	@Override
	public void mark(int readlimit){
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] buffer)  throws IOException{
		int r = in.read(buffer);
		if( r > 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(buffer, 0, r);
			}
		}
		return r;
	}

	@Override
	public int read() throws IOException {
		int r = in.read();
		if( r >= 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(r);
			}
		}
		return r;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException{
		int r = in.read(buffer,offset,length);
		if( r > 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(buffer, 0, r);
			}
		}
		return r;
	}

	@Override
	public void reset() throws IOException{
		throw new IOException();
	}
	
	public long skip(long byteCount) throws IOException{
		byte[] b = new byte[(int)byteCount];
		int r = in.read(b);
		if( r > 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(b,0,r);
			}
		}
		return r;
	}

}
