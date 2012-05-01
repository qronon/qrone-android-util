package org.qrone.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class BranchReader extends Reader {
	private Reader in;
	private Writer[] out;
	
	public BranchReader(Reader in, Writer... out){
		this.in = in;
		
		int c = out.length;
		for (int i = 0; i < out.length; i++) {
			if(out[i] == null) c--;
		}
		
		Writer[] o = new Writer[c];
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
	public void close() throws IOException{
		int b;
		char[] buf = new char[1024];
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
	public void reset() throws IOException{
		throw new IOException();
	}
	
	public long skip(long byteCount) throws IOException{
		char[] b = new char[(int)byteCount];
		int r = in.read(b);
		if( r > 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(b,0,r);
			}
		}
		return r;
	}


	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int r = in.read(cbuf,off,len);
		if( r > 0 ){
			for (int i = 0; i < out.length; i++) {
				out[i].write(cbuf, 0, r);
			}
		}
		return r;
	}

}
