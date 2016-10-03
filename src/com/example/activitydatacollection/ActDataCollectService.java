package com.example.activitydatacollection;



import com.google.android.glass.app.Card;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//public class ActDataCollectService extends Service {

public class ActDataCollectService extends Service implements SensorEventListener, Callback {
    private static final String LIVE_CARD_TAG = "ActivityDataCollection";

    private ActDataCollectDrawer mCallback;
    
    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;

/////////////////////////////////////////////////
	private static boolean m_blnRecordStatus = false; // true: Recording; false: Stopped 
	private static long m_lSensorDataFileInterval = 0; /* 0: Always record in one file;
														 Other value: in milliseconds, split file accordingly
														 */
	private static final int DATA_TYPE_SENSOR = 1;
		
	private SensorManager m_smActData = null;
		
	/* Sensor is available or not */
	private static boolean m_blnGyroPresent = false;
	private static boolean m_blnAcclPresent = false;
	
	/* Sensor is selected or not */
	private static boolean m_blnGyroEnabled = false;
	private static boolean m_blnAcclEnabled = false;
	private static boolean m_blnVideoEnabled = false;
	private static boolean m_blnAudioEnabled = false;
	
	
	//private static boolean m_blnGravityEnabled = false; //Don't Record gravity 
	private static boolean m_blnGravityEnabled = true;  

	//(A three dimensional vector indicating the direction and
	// magnitude of gravity)
			
	/* Default delay mode for sensors */
	
//	private static int m_iGyroMode = SensorManager.SENSOR_DELAY_GAME;
//	private static int m_iAcclMode = SensorManager.SENSOR_DELAY_GAME;
//	private static int m_iGravityMode = SensorManager.SENSOR_DELAY_GAME;

	private int m_iGyroMode = SensorManager.SENSOR_DELAY_GAME;
	private int m_iAcclMode = SensorManager.SENSOR_DELAY_GAME;
	private int m_iGravityMode = SensorManager.SENSOR_DELAY_GAME;
	
	private String m_sRecordFile; //Sensor record file
	private String m_sFullPathFile; //Sensor record full pathfile
	private FileWriter m_fwSensorRecord = null;
	private ActDataCollectService m_actHome = this;
	private Date m_dtFileStart;	//The time sensor data file is created (for each data file intervally)
	private ResolveInfo m_riHome;
		
	private float[] m_arrfAcclValues = new float[3];	 // Store accelerometer values

	private float[] m_arrfGravityValues = new float[3];
	private float[] m_arrfLinearAcceleration  = new float[3];
	
	private AlarmReceiver m_receiverAlarm = null;
	
	static String m_sGyro = ",,,";
	static String m_sAccl = ",,,";
	
	static String m_sGravity = ",,,";
	
	static String m_sSensorAccuracy=",,";

	private String m_sRecordFullPathFile = "";
	
	///////////////
	private SurfaceHolder m_surfaceHolder = null;
    private SurfaceView m_surfaceView = null;
    public MediaRecorder m_videoRec = null;
    private Camera m_camera = null;	
    
    private MediaRecorder m_audioRec = null;
	///////////////
    
/////////////////////////////////////////////////
    
    public class ActDataCollectBinder extends Binder {
    	ActDataCollectService getService() 
    	{
    		return ActDataCollectService.this;
    	}
    	
    }
    
    private final IBinder mBinder = new ActDataCollectBinder();

    @Override
    public IBinder onBind(Intent intent) 
    {
        return mBinder;
    }

    public void updateText(String strInfo)
    {
    	mCallback.updateText(strInfo);
    }
    
    /* Check the availability of sensors, disable relative widgets */
    private void checkSensorAvailability() 
    {    	
	   	List<Sensor> lstSensor = m_smActData.getSensorList(Sensor.TYPE_GYROSCOPE);
	   	if (lstSensor.size() > 0) {
	   		m_blnGyroPresent = true;
	   	} else {
	   		m_blnGyroPresent = false;
	   	}
	
	   	ActDataCollectConfig.setGyroPresent(m_blnGyroPresent);
	   	 
	//   	 lstSensor = m_smPCO.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
	   	lstSensor = m_smActData.getSensorList(Sensor.TYPE_ACCELEROMETER);
	   	if (lstSensor.size() > 0) {
	   		m_blnAcclPresent = true;
	   	} else {
	   		m_blnAcclPresent = false;
	   	}
	   	 
	   	ActDataCollectConfig.setAcclPresent(m_blnAcclPresent);

   }
    
    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        
        mTimelineManager = TimelineManager.from(this);
               
    	m_smActData = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        //setContentView(R.layout.main);
                                  
        //setDefaultStatus();
      
 //       PackageManager pm = getPackageManager();
 //       m_riHome = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);

//        m_btnRecord.setOnClickListener(m_btnRecordListener);
        
        checkSensorAvailability();
        						
		//New added
		for (int i=0; i<3; i++) {
			m_arrfGravityValues[i] = 0.0f;
			m_arrfLinearAcceleration[i]  = 0.0f;
			m_arrfAcclValues[i] = 0.0f;
		}
		
//        m_surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
	//	m_surfaceView = mCallback.getSurfaceView();
    //    m_surfaceHolder = m_surfaceView.getHolder();
              
    }
  


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            Log.d(LIVE_CARD_TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);

            // Keep track of the callback to remove it before unpublishing.
            mCallback = new ActDataCollectDrawer(this);
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);

            Intent menuIntent = new Intent(this, ActDataCollectMenu.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(LIVE_CARD_TAG, "Done publishing LiveCard");
        } else {
            // TODO(alainv): Jump to the LiveCard when API is available.
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
    	
    	if (ActDataCollectConfig.getRecordingStatus()) {
    		stopDataCollecting();
    	} 
    	
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(LIVE_CARD_TAG, "Unpublishing LiveCard");
            if (mCallback != null) {
                mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            }
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        
        super.onDestroy();
    }

    
    
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void resetValues() {
		m_sGyro = ",,,";
		m_sAccl = ",,,";
	}
    
	private void stopRecord() {
		//Stop record. Close Sensor Record File
		if (m_fwSensorRecord != null) {
			try {
				m_fwSensorRecord.close();
				m_fwSensorRecord = null;
			} catch (IOException e) {
				//
			}
		}
				
//        m_tvShowInfo.setText(getString(R.string.defaultinfo));        			
//        m_btnRecord.setText(getString(R.string.btn_start));
        resetValues();
		m_blnRecordStatus = false;
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
	}
	
	
	
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
 			if (m_blnAudioEnabled) {
 				stopAudioRecording();
 			}
 			
 			if (m_blnVideoEnabled) {
 				stopVideoRecording();
 			}
			
			stopTimer();
			stopRecord();
		}
	}

	
	// Start a timer to stop recording when interval expires
	private void startTimer() {
		Intent intent;
		PendingIntent pIntent;
		AlarmManager am;
		Calendar cal;
		
		cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.SECOND, (int)(m_lSensorDataFileInterval/1000));
		
		intent = new Intent(getString(R.string.myalarm));
		pIntent = PendingIntent.getBroadcast(ActDataCollectService.this,0,intent,PendingIntent.FLAG_ONE_SHOT); 
///		am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
		am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
	}

	
	private void stopTimer() {
		Intent intent;
		PendingIntent pIntent;
		AlarmManager am;

		intent = new Intent(getString(R.string.myalarm));
		pIntent = PendingIntent.getBroadcast(ActDataCollectService.this,0,intent,PendingIntent.FLAG_ONE_SHOT);
////		am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
		am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		am.cancel(pIntent);
	}
		
	
	/* Set file identification and type 
	 * Add "_" + "O", "G", "A", "M", "P", "L" to represent each enabled sensor recorded in the data file
	 * Sensor data is recorded as "CSV" file 
	 */
	private String getFileType() {
		String sFileType = "";
		boolean blnHasUnderscore = false; //Indicate the first "_" before the letters representing sensors
				
		if (m_blnGyroEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "G";
			} else {
				sFileType = "_G";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnAcclEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "A";
			} else {
				sFileType = "_A";
				blnHasUnderscore = true;
			}
		}
	
		sFileType = sFileType + ".csv";
		
		return sFileType;
		
	}
	
	private void enableSensorSetting(boolean blnEnabled)
	{
		
	}
	
                       
    public void onSensorChanged(SensorEvent event) {
		if ((m_blnGyroEnabled == false) && 
     		(m_blnAcclEnabled == false)) {
			return;
		} else {			
			recordSensorGPS(DATA_TYPE_SENSOR, event, null);
		}
    }
    
    
    public void recordSensorGPS(int iType, SensorEvent event,Location location) {
    	String sRecordLine;
    	String sTimeField;
    	Date dtCurDate;
    	long lStartTime = 0;
    	long lCurrentTime = 0;
		SimpleDateFormat spdRecordTime,spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
        int nSensorReadingType = 0;  //1: Gyro   2:Accl
		
        final float alpha = 0.8f;
        
        if (m_blnRecordStatus == false) { //Stopped
        	return;
        }

        dtCurDate = new Date();

        if (m_lSensorDataFileInterval != 0) {
        	
        	// Need to check whether to split file
        	lCurrentTime = dtCurDate.getTime();
			lStartTime = m_dtFileStart.getTime();
			
			//Check whether to record sensor data to a new file
			if (lCurrentTime - lStartTime >= m_lSensorDataFileInterval) {
				if (m_fwSensorRecord != null) {
					//Close current file
					try {
						m_fwSensorRecord.close();
						m_fwSensorRecord = null;
					} catch (IOException e) {
						
					}
				}
				
				return;
								
			}
        }
        
		// Timestamp for the record
        spdRecordTime = new SimpleDateFormat(DATE_FORMAT_S);
		sTimeField = spdRecordTime.format(dtCurDate);
				
		if (iType == DATA_TYPE_SENSOR) {
			synchronized(this) {
	    		switch (event.sensor.getType()){		
		    		case Sensor.TYPE_GYROSCOPE:
		    			//X,Y,Z
		    			m_sGyro = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			
		    			nSensorReadingType = 1;
		
		    			break;
		    			
		    		case Sensor.TYPE_ACCELEROMETER:
		    			m_arrfGravityValues[0] = alpha * m_arrfGravityValues[0] + (1 - alpha) * event.values[0];
		    			m_arrfGravityValues[1] = alpha * m_arrfGravityValues[1] + (1 - alpha) * event.values[1];
		    			m_arrfGravityValues[2] = alpha * m_arrfGravityValues[2] + (1 - alpha) * event.values[2];

		    			m_arrfLinearAcceleration[0] = event.values[0] - m_arrfGravityValues[0];
		    			m_arrfLinearAcceleration[1] = event.values[1] - m_arrfGravityValues[1];
		    			m_arrfLinearAcceleration[2] = event.values[2] - m_arrfGravityValues[2];	
		    			
	    				m_sAccl = Float.toString(m_arrfLinearAcceleration[0]) + "," + 
	    						Float.toString(m_arrfLinearAcceleration[1]) + "," + 
	    						Float.toString(m_arrfLinearAcceleration[2]) + ",";
	    				
	    				nSensorReadingType = 2;
			
		    			break;
		    			
		    		case Sensor.TYPE_LINEAR_ACCELERATION:
		    			//X,Y,Z
		    			m_sAccl = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			
		    			break;
		    					    				    		
		    		case Sensor.TYPE_GRAVITY:
		    			m_arrfGravityValues = event.values.clone();
		    			
	    				m_sGravity = Float.toString(event.values[0]) + "," + 
    								 Float.toString(event.values[1]) + "," + 
    								 Float.toString(event.values[2]) + ",";
	    				return;
	    				//break;
	    		}
	    	}
		} 
		
		sRecordLine = sTimeField + ",";				
		
		//Add the timestamp from 1970.1.1
		if (iType == DATA_TYPE_SENSOR) {
			//Sensor Data, Timestamp in nanosecond
			sRecordLine = sRecordLine + Long.valueOf(event.timestamp).toString() + ",";
		} 
		
		sRecordLine = sRecordLine + Integer.valueOf(nSensorReadingType) + ",";   //Sensor Reading Type:  1--Gyro,  2--Accl
		
		if (m_blnGravityEnabled) {
			//sRecordLine = sRecordLine + m_sGravity;
		}
		
		if (m_blnGyroEnabled) {
			sRecordLine = sRecordLine + m_sGyro;
		}
		
		if (m_blnAcclEnabled) {
			sRecordLine = sRecordLine + m_sAccl;
		}
		
						
		/////sRecordLine = sRecordLine + m_sSensorAccuracy;
		
    	sRecordLine = sRecordLine + System.getProperty("line.separator");

    	if (m_blnGyroEnabled == false &&  m_blnAcclEnabled == false) {
    		//To avoid frequently update GUI, only when low-frequent data is presented, show on GUI
    		//Gyro and Accl are too frequent
    		//m_tvShowRecord.setText(sRecordLine);
    	}
    	
    	if (m_fwSensorRecord != null) {
			//Write information into file
			//Compose information into recordLine
    		try {
    			m_fwSensorRecord.write(sRecordLine);
    		} catch (IOException e) {
    			
    		}
    	}

    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	switch(sensor.getType()) {
    		case Sensor.TYPE_GYROSCOPE:
    			m_sSensorAccuracy = "1,"; //Gyro
    			break;

    		case Sensor.TYPE_ACCELEROMETER:
    			m_sSensorAccuracy = "2,"; //Accl
    			break;

    		case Sensor.TYPE_LINEAR_ACCELERATION:
    			m_sSensorAccuracy = "3,"; //LinearAccl
    			break;
    		
    		case Sensor.TYPE_GRAVITY:
    			m_sSensorAccuracy = "7,"; //Gravity		
    			break;
    			
    		default:
    			m_sSensorAccuracy = "8,"; //Other
    	}
    	
    	switch (accuracy) {
    		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
    			m_sSensorAccuracy = m_sSensorAccuracy + "1,"; //H
    			break;

    		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
    			m_sSensorAccuracy = m_sSensorAccuracy + "2,"; //M
    			break;
    			
    		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
    			m_sSensorAccuracy = m_sSensorAccuracy + "3,"; //L
    			break;
    			
    		case SensorManager.SENSOR_STATUS_UNRELIABLE:
    			m_sSensorAccuracy = m_sSensorAccuracy + "4,"; //U
    			break;
    	}
    }

    	
    private void makeRecordFullFilePath()
    {
    	//Date dtCurDate;
		SimpleDateFormat spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
		String sFilename;

		m_dtFileStart = new Date();
		spdCurDateTime = new SimpleDateFormat(DATE_FORMAT);
		sFilename = spdCurDateTime.format(m_dtFileStart);
		m_sRecordFullPathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + 
																		File.separator + 
																		getString(R.string.activitydata_folder) + 
																		File.separator + sFilename;   
    }
 
    
    private void startAudioRecording() {
        String sFullAudioPathFile = "";
       
    	m_audioRec = new MediaRecorder();
    	if (m_audioRec != null) {
    		// Append file type information to the file name
    		sFullAudioPathFile = m_sRecordFullPathFile + ".3gp";    	
    		
    		m_audioRec.setAudioSource(MediaRecorder.AudioSource.MIC);
    		m_audioRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    		m_audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    		m_audioRec.setOutputFile(sFullAudioPathFile);
    		try {
    			m_audioRec.prepare();
    			m_audioRec.start();
    		} catch (Exception e) {
    			m_audioRec.reset();   // clear recorder configuration
    			m_audioRec.release(); // release the recorder object
    			m_audioRec = null;
    		}
    	}
    }
    
    private void stopAudioRecording() {
    	if (m_audioRec != null) {
	    	m_audioRec.stop();
	    	m_audioRec.reset();   // You can reuse the object by going back to setAudioSource() step
	    	m_audioRec.release(); // Now the object cannot be reused
	    	m_audioRec = null;
    	}
    }
    

    private boolean prepareVideoRecording() 
    {
		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------3020");
   	
    	if (m_surfaceView == null) {
    		//m_surfaceView = mCallback.getSurfaceView();
    		m_surfaceView = ActDataCollectDrawer.getSurfaceView();
    		//m_surfaceView = new SurfaceView(this);
    	}
    	
    	if (m_surfaceView == null) {
    		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------302");
    		
    		return false;
    	}
    	
        m_surfaceHolder = m_surfaceView.getHolder();
 //       m_surfaceHolder = ActDataCollectDrawer.getSurfaceHolder();
    
        
		m_camera = Camera.open();
		if (m_camera == null) return false;

    	try {
            m_camera.setPreviewDisplay(m_surfaceHolder);
            m_camera.startPreview();
    	} catch (Exception e) {
    		return false;
    	}


/*		
		try {
            m_camera.setPreviewDisplay(null);
            m_camera.startPreview();
    	} catch (Exception e) {
    		return false;
    	}
		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------30220");
   	
    	m_camera.stopPreview();
    	m_camera.unlock();
*/    
    	return true;
    }
    
    private boolean startVideoRecording_1()
    {
    	
        String sFullVideoPathFile;
        boolean blnRet = false;
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------301");
       
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------30221");
   	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;

    	m_camera.unlock();

    	m_videoRec.setCamera(m_camera);

    	// store the quality profile required
//#Option 1    	
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
///        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
    	
        // Step 2: Set sources
//    	m_videoRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        if (m_blnAudioEnabled == false) {
        	m_videoRec.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3011");
    
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//#Option 1
    	m_videoRec.setOutputFormat(profile.fileFormat);

    	if (m_blnAudioEnabled == false) {
    		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
    	}
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3012");

//#Option 1		
    	m_videoRec.setVideoEncoder(profile.videoCodec);
    	m_videoRec.setVideoEncodingBitRate(profile.videoBitRate);
    	m_videoRec.setVideoFrameRate(profile.videoFrameRate);
    	m_videoRec.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
    	
        // Step 4: Set output file
    	m_videoRec.setOutputFile(sFullVideoPathFile);

        // Step 5: Set the preview output
//    	m_videoRec.setPreviewDisplay(m_surfaceView.getHolder().getSurface());
    	m_videoRec.setPreviewDisplay(m_surfaceHolder.getSurface());

    	Log.d(LIVE_CARD_TAG, "startVideoRecording---------3014");

        // Step 6: Prepare configured MediaRecorder
        try {
        	m_videoRec.prepare();
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3015");

        } catch (IllegalStateException e) {
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3016");

            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3017");
        	
            releaseMediaRecorder();
            return false;
        }
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------307");
        
        m_videoRec.start();
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------308");
        
        return true;

    }

    
    
    private boolean startVideoRecording_2()
    {
        String sFullVideoPathFile;
        boolean blnRet = false;
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------301");
       
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
    	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;

    	m_camera.unlock();

    	m_videoRec.setCamera(m_camera);
    	
        // Step 2: Set sources
//    	m_videoRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        if (m_blnAudioEnabled == false) {
        	m_videoRec.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3011");
    
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	
//#Option 2 
		m_videoRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    	//m_videoRec.setVideoEncodingBitRate(500000);
		m_videoRec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);    	
//    	m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    	if (m_blnAudioEnabled == false) {
    		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
    	}
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3012");

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
 /////   	m_videoRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));  //480p=720x480 best, QUALITY_720P work, others (High, Low, 1080p) don't work

        // Step 4: Set output file
    	m_videoRec.setOutputFile(sFullVideoPathFile);
    	//m_videoRec.setVideoFrameRate(20);
    	m_videoRec.setVideoSize(1280, 720); //work with FrameRate 30  1px*1px
    	//1280x720, 640x640, 640x480 work
    	//720x480, 640x360, 480x320,  480x360 do not work
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3013");

        // Step 5: Set the preview output
//    	m_videoRec.setPreviewDisplay(m_surfaceView.getHolder().getSurface());
    	m_videoRec.setPreviewDisplay(m_surfaceHolder.getSurface());

    	Log.d(LIVE_CARD_TAG, "startVideoRecording---------3014");

        // Step 6: Prepare configured MediaRecorder
        try {
        	m_videoRec.prepare();
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3015");

        } catch (IllegalStateException e) {
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3016");

            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
    		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3017");
        	
            releaseMediaRecorder();
            return false;
        }
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------307");
        
        m_videoRec.start();
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------308");
        
        return true;

    }
    
    
    
    
    private void releaseMediaRecorder()
    {
    	m_videoRec.release();
    	m_videoRec = null;
    	m_camera.lock();
    	
    	if (m_camera == null) return;

    	m_camera.stopPreview();
    	m_camera.release();
    	m_camera = null; 	
    }
    
    private void stopVideoRecording()
    {
		Log.d(LIVE_CARD_TAG, "stopVideoRecording---------300");

    	if (m_videoRec == null) return;
    	
    	m_videoRec.stop();
    //	m_videoRec.reset();
    	m_camera.stopPreview();
    	m_videoRec.release();
    	m_videoRec = null;
    	m_camera.lock();
    	
    	if (m_camera == null) return;
    	
    	m_camera.release();
    	m_camera = null;
    }

    
    public void startDataCollecting()
    {
 		String sDataDir;
 		File flDataFolder;
 		boolean blnSensorSelected = false;
 		boolean blnVideoSelected = false;
 		boolean blnAudioSelected = false;
 		String sShowInfo = "";
 		boolean blnRet = false;
 		
 		mCallback.updateText("In startDataCollecting");
 		
 		if (ActDataCollectConfig.getRecordingStatus()) return;
 				
 		m_blnGyroEnabled = ActDataCollectConfig.getGyroSelection();
 		m_blnAcclEnabled = ActDataCollectConfig.getAcclSelection();
		m_blnAudioEnabled = ActDataCollectConfig.getAudioSelection();
		m_blnVideoEnabled = ActDataCollectConfig.getVideoSelection();

		if (m_blnGyroEnabled == false && m_blnAcclEnabled == false && m_blnAudioEnabled == false && m_blnVideoEnabled == false) {
			// Ask user to select sensor/audio/video
			
			return;
		}
		
		if (m_blnGyroEnabled == true || m_blnAcclEnabled == true) {
			blnSensorSelected = true;
		}
		
		blnAudioSelected = m_blnAudioEnabled;
		blnVideoSelected = m_blnVideoEnabled;
				
		mCallback.updateText("Collecting Data...");
		
 		m_lSensorDataFileInterval = ActDataCollectConfig.getRecordDuration();
 		
		mCallback.updateText("Duration: " + Long.valueOf(m_lSensorDataFileInterval));

		m_iGyroMode = ActDataCollectConfig.getSensorDelayMode();
		m_iAcclMode = ActDataCollectConfig.getSensorDelayMode();
		m_iGravityMode = ActDataCollectConfig.getSensorDelayMode();
		
		// TODO Auto-generated method stub
		if (m_blnGyroEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_GYROSCOPE),m_iGyroMode);					
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		} 

if (false) {		//Old		
		if (m_blnAcclEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),m_iAcclMode);
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
		}
} else {
		if (m_blnAcclEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),m_iAcclMode);
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		}
}
					
		if (m_blnGravityEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_GRAVITY),m_iGravityMode);
		}
	
		/* Create /sdcard/ActivityData/ folder */
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			sDataDir = Environment.getExternalStorageDirectory().getAbsolutePath() + 
									File.separator + getString(R.string.activitydata_folder);
			flDataFolder = new File(sDataDir);
			//Check whether /mnt/sdcard/ActivityData/ exists
			if (!flDataFolder.exists()) {
				//Does not exist, create it
				if (flDataFolder.mkdir()) {
					     						
				} else {
					//Failed to create
					//m_tvShowInfo.setText("Failed to record Sensor data on SD Card!");
					return;
				}
				
			} else {
			} 
		} else {        				
			//NO SD Card
			//m_tvShowInfo.setText("Please insert SD Card!");
			return;
		}
	
		makeRecordFullFilePath();
		//m_sRecordFullPathFile = m_sFullPathFile;

		if (blnSensorSelected) {
			// Append file type information to the file name
			//m_sFullPathFile = m_sFullPathFile + getFileType();
			m_sFullPathFile = m_sRecordFullPathFile + getFileType();
			try {
				m_fwSensorRecord = new FileWriter(m_sFullPathFile);
 			} catch (IOException e) {
 				//m_tvShowInfo.setText("Failed to record Sensor data on SD Card!");
 				m_fwSensorRecord = null;
 				return;
 			}
		}
		     			
		if (m_blnAudioEnabled) {
			startAudioRecording();
		}
		
		if (m_blnVideoEnabled) {
			Log.d(LIVE_CARD_TAG, "startDataCollecting---------8");
			blnRet = startVideoRecording_1();
			if (blnRet == false) {
				sShowInfo = "Failed to record video";
				//m_tvShowInfo.setText(sShowInfo);
				return;
			}
			
		}
		
		//Disable setting for sensor when recording        			
		enableSensorSetting(false);
//	        m_btnRecord.setText(getString(R.string.btn_stop));
       
       if (blnSensorSelected == true) {
    	   if (blnVideoSelected == false && blnAudioSelected == false) {
       			sShowInfo = "Recording:" + m_sFullPathFile;
       		} else if (blnVideoSelected == true && blnAudioSelected == false) {
       			sShowInfo = "Recording Sensor & Video";
       		} else if (blnVideoSelected == false && blnAudioSelected == true) {
       			sShowInfo = "Recording Sensor & Audio";     	        		
       		}
       } else if (blnVideoSelected == true && blnAudioSelected == false) {
       		sShowInfo = "Recording Video";
       } else if (blnVideoSelected == false && blnAudioSelected == true) {
       		sShowInfo = "Recording Audio";
       }
            	             	        
//			m_tvShowInfo.setText(sShowInfo);
		
       //Save to file once an interval
		m_blnRecordStatus = true;
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
		if (m_lSensorDataFileInterval != 0) {
			//Need to stop after an interval
			m_receiverAlarm = new AlarmReceiver();
			registerReceiver(m_receiverAlarm, new IntentFilter(getString(R.string.myalarm)));
			startTimer();
		}
		
		mCallback.updateText("Collecting Data...");
			
    }
    
    
    
    public void stopDataCollecting()
    {
		Log.d(LIVE_CARD_TAG, "startDataCollecting---------12");
    	
    	if (ActDataCollectConfig.getRecordingStatus() == false) return;
    	
		//Stop record. Close Sensor Record File
		if (m_fwSensorRecord != null) {
			try {
				m_fwSensorRecord.close();
				m_fwSensorRecord = null;
			} catch (IOException e) {
				//
			}
		}
		
		// Enable sensor setting when recording stops
		enableSensorSetting(true);
//	        m_tvShowInfo.setText(getString(R.string.defaultinfo));        			
//	        m_btnRecord.setText(getString(R.string.btn_start));
//	        m_tvShowRecord.setText("");
        resetValues();
		m_blnRecordStatus = false;

		if (m_blnAudioEnabled) {
			stopAudioRecording();
		}
		
		if (m_blnVideoEnabled) {			
			stopVideoRecording();
		}
		
		if (m_lSensorDataFileInterval != 0) {
			
			//Need to stop after an interval
			stopTimer();
			unregisterReceiver(m_receiverAlarm);
		}   
 
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
		mCallback.updateText("Setting->Start Data Collecting...");
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
    
    
	
}
