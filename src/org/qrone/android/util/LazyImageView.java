package org.qrone.android.util;

import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

public class LazyImageView extends ImageView{

	private static Map<String, float[]> cache = new WeakHashMap<String, float[]>(100);
	private static Map<String, Bitmap> cacheBmp = new WeakHashMap<String, Bitmap>(5);
	
	public LazyImageView(Context context) {
		super(context);
	}
	
    public LazyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public LazyImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    private int width = -1;
    public void setWidth(int w){
    	this.width = w;
    }
    
    private float[] calcSize(Bitmap bmp){
    	float[] result = new float[4];
    	
		float iw = bmp.getWidth();
		float ih = bmp.getHeight();
		float factor = ((float)width) / iw;
		
		result[0] = (float)width;
		result[1] = ih * factor;
		result[2] = factor;
		result[3] = factor;
		return result;
    }
    

    private long settime = System.currentTimeMillis();
    @Override
    public void setImageBitmap(Bitmap bitm){
    	
		super.setImageBitmap(bitm);
		if(width >= 0 && bitm != null){
			float[] size = calcSize(bitm);
			Matrix m = getImageMatrix();
			m.reset();
			m.postScale(size[2], size[3]);
			
			getLayoutParams().width = (int)size[0];
			getLayoutParams().height = (int)size[1];
		}
    }

    private String url;
    public String getURL(){
    	return url;
    }
    
	public void setURL(final String url, final String filename){
		this.url = url;
		Bitmap bitm = cacheBmp.get(filename);
		if(bitm != null){
			setImageBitmap(bitm);
			invalidate();
			return;
		}
			
		setImageBitmap(null);
		settime = System.currentTimeMillis();
		float[] rect = cache.get(filename);
		if(rect != null){
			getLayoutParams().width = (int)rect[0];
			getLayoutParams().height = (int)rect[1];
		}
		invalidate();
		
		ImageAsyncTask task = new ImageAsyncTask("Android/data/org.qrone.dl"){
			
			@Override
			protected void onPostExecute(Bitmap[] bitm){
				if(bitm[0] == null){
					setImageResource(R.drawable.notfound);

					float factor = ((float)width) / 525F;
					Matrix m = getImageMatrix();
					m.reset();
					m.postScale(factor, factor);
					
					getLayoutParams().width = (int)width;
					getLayoutParams().height = (int)(362F * factor);
					return;
				}
				setImageBitmap(bitm[0]);
				invalidate();

				float[] size = calcSize(bitm[0]);
				cache.put(filename, size);
				cacheBmp.put(filename,bitm[0]);
				
				if(System.currentTimeMillis() - settime > 300){
			        float fromAlpha = 0.0F;
			        float toAlpha = 1.0F;
			 
			        AlphaAnimation animation = new AlphaAnimation(fromAlpha, toAlpha);
			        animation.setDuration(300);
			        startAnimation(animation);
				}
			}
		};
		
		task.execute(new WebSource(url, filename));
	}
	
}
