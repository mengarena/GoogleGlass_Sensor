package com.example.activitydatacollection;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ActDataCollectDrawer implements SurfaceHolder.Callback {

	private static ActDataCollectView mActDataCollectView;
    private SurfaceHolder mHolder;
    
    public ActDataCollectDrawer(Context context) {
    	mActDataCollectView = new ActDataCollectView(context);
    	mActDataCollectView.updateText("Welcome...");
    }
    
    public static void updateText(String strInfo)
    {
    	mActDataCollectView.updateText(strInfo);
    }
    
    public static SurfaceHolder getSurfaceHolder()
    {
 //   	return mHolder;
    	return ActDataCollectView.getSurfaceHolder();
    }
    
    public static SurfaceView getSurfaceView()
    {
    	//return mActDataCollectView.getSurfaceView();
    	return ActDataCollectView.getSurfaceView();
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
        // Measure and layout the view with the canvas dimensions.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

        mActDataCollectView.measure(measuredWidth, measuredHeight);
        mActDataCollectView.layout(0, 0, mActDataCollectView.getMeasuredWidth(), mActDataCollectView.getMeasuredHeight());
        draw();
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mHolder = holder;
		draw();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mHolder = null;
	}
	

    public void draw() {
        Canvas canvas;
        try {
            canvas = mHolder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
        	mActDataCollectView.draw(canvas);
            mHolder.unlockCanvasAndPost(canvas);
        }
    }
	
}
