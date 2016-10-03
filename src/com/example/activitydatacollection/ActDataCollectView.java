package com.example.activitydatacollection;

import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.AttributeSet;
import android.content.Context;

public class ActDataCollectView extends FrameLayout {

    private static TextView mRunningInfoView = null;
    private static SurfaceView m_surfaceView = null;
    private static SurfaceHolder m_surfaceHolder = null;
    
    public ActDataCollectView(Context context) {
        this(context, null, 0);
    }

    public ActDataCollectView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActDataCollectView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        LayoutInflater.from(context).inflate(R.layout.card_info, this);

        mRunningInfoView =  (TextView) findViewById(R.id.showInfo);
        m_surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        m_surfaceHolder = m_surfaceView.getHolder();

    }
	
    
    public void updateText(String strInfo) {
    	 mRunningInfoView.setText(strInfo);
    }
    
    public static SurfaceView getSurfaceView()
    {
    	return m_surfaceView;
    }
    
    public static SurfaceHolder getSurfaceHolder()
    {
    	return m_surfaceHolder;
    }
    
}
