package com.example.activitydatacollection;

//
//This program is used for Google Glass
//

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.GeomagneticField;
import android.util.*;
import android.content.BroadcastReceiver;
import android.location.*;
import android.app.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class ActDataCollect extends Activity implements SensorEventListener, Callback {
    private static final String LIVE_CARD_TAG = "ActDataCollect";

	private int[] m_arrDurationMenuID = {
			R.id.no_limit,
			R.id.thirty_sec,
			R.id.one_min,
			R.id.three_min,
			R.id.five_min,
			R.id.ten_min,
			R.id.twenty_min,
			R.id.thirty_min,
			R.id.sixty_min,
			R.id.ninety_min,
			R.id.two_hour,
			R.id.four_hour
		};
		
	private long[] m_arrDuration = {
			0,
			30*1000,
			60*1000,
			180*1000,
			300*1000,
			600*1000,
			1200*1000,
			1800*1000,
			3600*1000,
			5400*1000,
			3600*2*1000,
			3600*4*1000
	};
	
	
	private int[] m_arrSensorDelayModeID = {
			R.id.fifty_hz,
			R.id.twenty_hz
	};
	
	
	private int[] m_arrSensorDelayMode = {
			SensorManager.SENSOR_DELAY_GAME,
			SensorManager.SENSOR_DELAY_UI
	};
		
	private int[] m_arrVideoResolutionID = {
			R.id.p480,
			R.id.p720
	};
	
	
	private int[] m_arrVideoResolution = {
			ActDataCollectConfig.VIDEO_RESOLUTION_480P,
			ActDataCollectConfig.VIDEO_RESOLUTION_720P
	};
       
	private static boolean m_blnRecordStatus = false; // true: Recording; false: Stopped 
	private static long m_lSensorDataFileInterval = 0; /* 0: Always record in one file;
														 Other value: in milliseconds, split file accordingly
														 */
	private static final int DATA_TYPE_SENSOR = 1;
		
	private SensorManager m_smActData = null;
		
	/* Sensor is available or not */
	private static boolean m_blnGyroPresent = false;
	private static boolean m_blnAcclPresent = false;
	private static boolean m_blnLightPresent = false;
	
	private static int SENSOR_EVENT_GYRO = 1;
	private static int SENSOR_EVENT_ACCL = 2;
	private static int SENSOR_EVENT_LIGHT = 3;
		
	/* Sensor is selected or not */
	private static boolean m_blnGyroEnabled = false;
	private static boolean m_blnAcclEnabled = false;
	private static boolean m_blnLightEnabled = false;
	
	private static boolean m_blnVideoEnabled = false;
	private static boolean m_blnAudioEnabled = false;
	
	
	//private static boolean m_blnGravityEnabled = false; //Don't Record gravity 
	private static boolean m_blnGravityEnabled = true;  

	private int m_iGyroMode = SensorManager.SENSOR_DELAY_GAME;
	private int m_iAcclMode = SensorManager.SENSOR_DELAY_GAME;
	private int m_iLightMode = SensorManager.SENSOR_DELAY_GAME;
	private int m_iGravityMode = SensorManager.SENSOR_DELAY_GAME;
	
	private String m_sRecordFile; //Sensor record file
	private String m_sFullPathFile; //Sensor record full pathfile
	private FileWriter m_fwSensorRecord = null;
	private ActDataCollect m_actHome = this;
	private Date m_dtFileStart;	//The time sensor data file is created (for each data file intervally)
	private ResolveInfo m_riHome;
		
	private float[] m_arrfAcclValues = new float[3];	 // Store accelerometer values

	private float[] m_arrfGravityValues = new float[3];
	private float[] m_arrfLinearAcceleration  = new float[3];
	
	private AlarmReceiver m_receiverAlarm = null;
	
	static String m_sGyro = ",,,";
	static String m_sAccl = ",,,";
	static String m_sLight = ",";
	static String m_sGravity = ",,,";
	
	static String m_sSensorAccuracy=",,";

	private String m_sRecordFullPathFile = "";
	
	private TextView m_tvShowInfo;

	///////////////
	private SurfaceHolder m_surfaceHolder = null;
    private SurfaceView m_surfaceView = null;
    public MediaRecorder m_videoRec = null;
    private Camera m_camera = null;	
    
    private MediaRecorder m_audioRec = null;
	///////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
      
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	int i;
    	int nVideoResolution = ActDataCollectConfig.getVideoResolution();
    	int nSensorDelayMode = ActDataCollectConfig.getSensorDelayMode();
    	long lRecordDuration = ActDataCollectConfig.getRecordDuration();    	
    	
    	if (ActDataCollectConfig.getGyroPresent()) {
    		menu.findItem(R.id.gyro).setEnabled(true);
	    	if (ActDataCollectConfig.getGyroSelection() == true) {
	    		menu.findItem(R.id.gyro).setIcon(R.drawable.ic_done);
	    	} else {
	    		menu.findItem(R.id.gyro).setIcon(null);    		
	    	}
    	} else {
    		menu.findItem(R.id.gyro).setEnabled(false);
    	}

    	
    	if (ActDataCollectConfig.getAcclPresent()) {
    		menu.findItem(R.id.accl).setEnabled(true);
    		
        	if (ActDataCollectConfig.getAcclSelection() == true) {
        		menu.findItem(R.id.accl).setIcon(R.drawable.ic_done);   		
        	} else {
        		menu.findItem(R.id.accl).setIcon(null);    		
        	}

    	} else {
    		menu.findItem(R.id.accl).setEnabled(false);
    	}
    	
    	
    	if (ActDataCollectConfig.getLightPresent()) {
    		menu.findItem(R.id.light).setEnabled(true);
    		
        	if (ActDataCollectConfig.getLightSelection() == true) {
        		menu.findItem(R.id.light).setIcon(R.drawable.ic_done);   		
        	} else {
        		menu.findItem(R.id.light).setIcon(null);    		
        	}

    	} else {
    		menu.findItem(R.id.light).setEnabled(false);
    	}

    	
    	if (ActDataCollectConfig.getAudioSelection() == true) {
    		menu.findItem(R.id.audio).setIcon(R.drawable.ic_done);
    	} else {
    		menu.findItem(R.id.audio).setIcon(null);    		
    	}

    	if (ActDataCollectConfig.getVideoSelection() == true) {
    		menu.findItem(R.id.video).setIcon(R.drawable.ic_done);
    	} else {
    		menu.findItem(R.id.video).setIcon(null);    		
    	}

    	for (i=0; i<m_arrVideoResolution.length; i++) {
    		if (m_arrVideoResolution[i] == nVideoResolution) {
    			menu.findItem(m_arrVideoResolutionID[i]).setIcon(R.drawable.ic_done);
    		} else {
    			menu.findItem(m_arrVideoResolutionID[i]).setIcon(null);
    		}
    	}
    	
    	for (i=0; i<m_arrSensorDelayMode.length; i++) {
    		if (m_arrSensorDelayMode[i] == nSensorDelayMode) {
    			menu.findItem(m_arrSensorDelayModeID[i]).setIcon(R.drawable.ic_done);
    		} else {
    			menu.findItem(m_arrSensorDelayModeID[i]).setIcon(null);
    		}
    	}
    	
    	for (i=0; i<m_arrDuration.length; i++) {
    		if (m_arrDuration[i] == lRecordDuration) {
        		menu.findItem(m_arrDurationMenuID[i]).setIcon(R.drawable.ic_done);    			
    		} else {
        		menu.findItem(m_arrDurationMenuID[i]).setIcon(null);    			
    		}
    	}
    	   		
    	//Set enable/disable status for menus based on m_blnRecordingStatus
    	
    	return true;
    	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	boolean blnRet = false;
        // Handle item selection.
        switch (item.getItemId()) {
        	case R.id.exit:
        		stopDataCollecting();
        		finish();
        		System.exit(0);
        		return true;
            case R.id.stop:
            	//Stop data collection
            	stopDataCollecting();
                //stopService(new Intent(this, ActDataCollectService.class));
                return true;
            case R.id.start:
            	//Start data collection
            	startDataCollecting();
            	return true;

            case R.id.gyro:
            	blnRet = ActDataCollectConfig.getGyroSelection();            	
            	ActDataCollectConfig.setGyroSelection(!blnRet);
            	return true;

            case R.id.accl:
            	blnRet = ActDataCollectConfig.getAcclSelection();            	
            	ActDataCollectConfig.setAcclSelection(!blnRet);
            	return true;

            case R.id.light:
            	blnRet = ActDataCollectConfig.getLightSelection();            	
            	ActDataCollectConfig.setLightSelection(!blnRet);
            	return true;

            case R.id.audio:
            	blnRet = ActDataCollectConfig.getAudioSelection();            	
            	ActDataCollectConfig.setAudioSelection(!blnRet);
            	return true;

            case R.id.video:
            	blnRet = ActDataCollectConfig.getVideoSelection();            	
            	ActDataCollectConfig.setVideoSelection(!blnRet);
            	return true;
            	
            case R.id.p480:
            	ActDataCollectConfig.setVideoResolution(ActDataCollectConfig.VIDEO_RESOLUTION_480P);
            	return true;
            	
            case R.id.p720:
            	ActDataCollectConfig.setVideoResolution(ActDataCollectConfig.VIDEO_RESOLUTION_720P);            	
            	return true;
            	
            case R.id.fifty_hz:
            	ActDataCollectConfig.setSensorDelayMode(SensorManager.SENSOR_DELAY_GAME);
            	return true;
            	
            case R.id.twenty_hz:
            	ActDataCollectConfig.setSensorDelayMode(SensorManager.SENSOR_DELAY_UI);   	
            	return true;
            	
            case R.id.no_limit:
            	ActDataCollectConfig.setRecordDuration(0);
            	return true;

            case R.id.thirty_sec:
            	ActDataCollectConfig.setRecordDuration(30*1000);
            	return true;
            	
            case R.id.one_min:
            	ActDataCollectConfig.setRecordDuration(60*1000);
            	return true;

            case R.id.three_min:
            	ActDataCollectConfig.setRecordDuration(180*1000);
            	return true;
            	
            case R.id.five_min:
            	ActDataCollectConfig.setRecordDuration(300*1000);
            	return true;

            case R.id.ten_min:
            	ActDataCollectConfig.setRecordDuration(600*1000);
            	return true;

            case R.id.twenty_min:
            	ActDataCollectConfig.setRecordDuration(1200*1000);
            	return true;

            case R.id.thirty_min:
            	ActDataCollectConfig.setRecordDuration(1800*1000);
            	return true;
 
            case R.id.sixty_min:
            	ActDataCollectConfig.setRecordDuration(3600*1000);
            	return true;

            case R.id.ninety_min:
            	ActDataCollectConfig.setRecordDuration(5400*1000);
            	return true;

            case R.id.two_hour:
            	ActDataCollectConfig.setRecordDuration(3600*2*1000);
            	return true;

            case R.id.four_hour:
            	ActDataCollectConfig.setRecordDuration(3600*4*1000);
            	return true;
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the Activity.
    	super.onOptionsMenuClosed(menu);
    	    	
       // finish();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
              openOptionsMenu();
              return true;
          }
          return false;
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
	   	 
	  	lstSensor = m_smActData.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
	//   	lstSensor = m_smActData.getSensorList(Sensor.TYPE_ACCELEROMETER);
	   	if (lstSensor.size() > 0) {
	   		m_blnAcclPresent = true;
	   	} else {
	   		m_blnAcclPresent = false;
	   	}
	   	 
	   	ActDataCollectConfig.setAcclPresent(m_blnAcclPresent);

	   	
	   	lstSensor = m_smActData.getSensorList(Sensor.TYPE_LIGHT);
	   	if (lstSensor.size() > 0) {
	   		m_blnLightPresent = true;
	   	} else {
	   		m_blnLightPresent = false;
	   	}
	
	   	ActDataCollectConfig.setLightPresent(m_blnLightPresent);
	   	
   }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	int i;
     	
    	super.onCreate(savedInstanceState);
     	
    	m_smActData = (SensorManager) getSystemService(SENSOR_SERVICE);
                
        setContentView(R.layout.card_info);
        
                                           
        //setDefaultStatus();
      
        PackageManager pm = getPackageManager();
        m_riHome = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);
        
        m_tvShowInfo = (TextView)findViewById(R.id.showInfo);  //Show Message to user
    	m_tvShowInfo.setText(getString(R.string.defaultinfo));

        checkSensorAvailability();
        						
		//New added
		for (i=0; i<3; i++) {
			m_arrfGravityValues[i] = 0.0f;
			m_arrfLinearAcceleration[i]  = 0.0f;
			m_arrfAcclValues[i] = 0.0f;
		}
		
        m_surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        m_surfaceHolder = m_surfaceView.getHolder();
    //    m_surfaceView.setBackgroundColor(Color.BLACK);
              
    }
  

    private void showInformation(String strInfo)
    {
    	m_tvShowInfo.setText(strInfo);
    }
    
	private void resetValues() {
		m_sGyro = ",,,";
		m_sAccl = ",,,";
		m_sLight = ",";		
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
				
        resetValues();
		m_blnRecordStatus = false;
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
	}
	
	
	
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
 		//	if (m_blnAudioEnabled) {
 		//		stopAudioRecording();
 		//	}
 			
 		//	if (m_blnVideoEnabled) {
 		//		stopVideoRecording();
 		//	}
			
 			stopDataCollecting();
 			
			stopTimer();
		//	stopRecord();
			
			///////
    		finish();
    		System.exit(0);
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
		pIntent = PendingIntent.getBroadcast(ActDataCollect.this,0,intent,PendingIntent.FLAG_ONE_SHOT); 
///		am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
		am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
	}

	
	private void stopTimer() {
		Intent intent;
		PendingIntent pIntent;
		AlarmManager am;

		intent = new Intent(getString(R.string.myalarm));
		pIntent = PendingIntent.getBroadcast(ActDataCollect.this,0,intent,PendingIntent.FLAG_ONE_SHOT);
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

		if (m_blnLightEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "L";
			} else {
				sFileType = "_L";
				blnHasUnderscore = true;
			}
		}
		
		sFileType = sFileType + ".csv";
		
		return sFileType;
		
	}
		
                       
    public void onSensorChanged(SensorEvent event) {
		if ((m_blnGyroEnabled == false) && 
     		(m_blnAcclEnabled == false) &&
     		(m_blnLightEnabled == false)) {
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
        int nSensorReadingType = 0; 
		
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
		    			
		    			nSensorReadingType = SENSOR_EVENT_GYRO;
		
		    			break;

/*		    			
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
	    				
	    				nSensorReadingType = SENSOR_EVENT_ACCL;
			
		    			break;
*/		    			
		    		case Sensor.TYPE_LINEAR_ACCELERATION:
		    			//X,Y,Z
		    			m_sAccl = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			
		    			nSensorReadingType = SENSOR_EVENT_ACCL;
		    			
		    			break;
		    				
		    		case Sensor.TYPE_LIGHT:
		    			// Ambient light level in SI lux units 
		    			m_sLight = Float.toString(event.values[0]) + ",";
		    			
		    			nSensorReadingType = SENSOR_EVENT_LIGHT;
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
		
		sRecordLine = sRecordLine + Integer.valueOf(nSensorReadingType) + ",";
		
		if (m_blnGravityEnabled) {
			//sRecordLine = sRecordLine + m_sGravity;
		}
		
		if (m_blnGyroEnabled) {
			sRecordLine = sRecordLine + m_sGyro;
		}
		
		if (m_blnAcclEnabled) {
			sRecordLine = sRecordLine + m_sAccl;
		}
		
		if (m_blnLightEnabled) {
			sRecordLine = sRecordLine + m_sLight;			
		}
						
		/////sRecordLine = sRecordLine + m_sSensorAccuracy;
		
    	sRecordLine = sRecordLine + System.getProperty("line.separator");

    	if (m_blnGyroEnabled == false &&  m_blnAcclEnabled == false &&  m_blnLightEnabled == false) {
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

/*    			
    		case Sensor.TYPE_ACCELEROMETER:
    			m_sSensorAccuracy = "2,"; //Accl
    			break;
*/
    			
    		case Sensor.TYPE_LINEAR_ACCELERATION:
    			m_sSensorAccuracy = "2,"; //LinearAccl
    			break;
    			
    		case Sensor.TYPE_LIGHT:
    			m_sSensorAccuracy = "3,"; //Light
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
		m_camera = Camera.open();
		if (m_camera == null) return false;

		
    	try {
            m_camera.setPreviewDisplay(m_surfaceHolder);
            m_camera.startPreview();
    	} catch (Exception e) {
    		return false;
    	}
        	
    	
    	/////m_camera.unlock();
//////////////////###############################

    	try {
			m_camera.setPreviewDisplay(null);
		} catch (java.io.IOException ioe) {
			Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------30221111");
			return false;
		}
		m_camera.stopPreview();
		m_camera.unlock(); 

		return true;
//////////////////###############################    	
    	
    }
    
    private boolean startVideoRecording_1()
    {
    	
        String sFullVideoPathFile;
        CamcorderProfile profile;
        boolean blnRet = false;
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------301");
       
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------30221");
   	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;
	
    	m_videoRec.setCamera(m_camera);

    	// store the quality profile required
//#Option 1    	
/////        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
////        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
    	
    	if (ActDataCollectConfig.getVideoResolution() == ActDataCollectConfig.VIDEO_RESOLUTION_480P) {
    		profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
    	} else {
    		profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
    	}
    	
        // Step 2: Set sources
//    	m_videoRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        if (m_blnAudioEnabled == false) {
        	m_videoRec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        	//m_videoRec.setAudioSource(MediaRecorder.AudioSource.MIC);
    		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------302210");

 //    		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
 //   		m_videoRec.setAudioEncoder(profile.audioCodec); 

        }
        
		Log.d(LIVE_CARD_TAG, "startVideoRecording---------3011");
    
//    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

    	//#Option 1
    	m_videoRec.setOutputFormat(profile.fileFormat);

    	if (m_blnAudioEnabled == false) {
    	////	m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
    		Log.d(LIVE_CARD_TAG, "prepareVideoRecording---------302211");
   		
    		m_videoRec.setAudioEncoder(profile.audioCodec); 
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

    
    private boolean startVideoRecording_3()
    {    	
        String sFullVideoPathFile;
        CamcorderProfile profile;
        boolean blnRet = false;
       
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
   	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;
	
    	m_videoRec.setCamera(m_camera);

    	m_videoRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	
    	m_videoRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)); //1280x720
//    	m_videoRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));  //176x144
    	
    	m_videoRec.setOutputFile(sFullVideoPathFile);
    	
    	m_videoRec.setPreviewDisplay(m_surfaceHolder.getSurface());
    	
        // Step 6: Prepare configured MediaRecorder
        try {
        	m_videoRec.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        
        m_videoRec.start();
        
        return true;
    }

    
        
    private void releaseMediaRecorder()
    {
    	Log.d(LIVE_CARD_TAG, "releaseMediaRecorder---------1");    	
    	if (m_videoRec != null) {
    		m_videoRec.reset();
    		m_videoRec.release();
    		m_videoRec = null;
    		m_camera.lock();
    	}

    	Log.d(LIVE_CARD_TAG, "releaseMediaRecorder---------2");    	
    	
    	if (m_camera == null) return;

    	m_camera.stopPreview();
    	m_camera.release();
    	m_camera = null; 
    	Log.d(LIVE_CARD_TAG, "releaseMediaRecorder---------3");    	
    	
    }
    
    private void stopVideoRecording()
    {
		Log.d(LIVE_CARD_TAG, "stopVideoRecording---------1");

    	if (m_videoRec != null) {    	
	    	m_videoRec.stop();
	    	
	    	m_videoRec.reset();
	    //	m_camera.stopPreview();
	    	m_videoRec.release();
	    	m_videoRec = null;
	    	
	    	//m_camera.lock();
    	}
    	
		Log.d(LIVE_CARD_TAG, "stopVideoRecording---------2");
    	
    	if (m_camera == null) return;
    	m_camera.stopPreview();
    	m_camera.release();
    	m_camera = null;
		Log.d(LIVE_CARD_TAG, "stopVideoRecording---------3");
    	
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
 				
 		if (ActDataCollectConfig.getRecordingStatus()) return;

 		showInformation("Data Collection Start");
 		
 		ActDataCollectConfig.setRecordingStatus(true);
 				
 		m_blnGyroEnabled = ActDataCollectConfig.getGyroSelection();
 		m_blnAcclEnabled = ActDataCollectConfig.getAcclSelection();
 		m_blnLightEnabled = ActDataCollectConfig.getLightSelection(); 		
		m_blnAudioEnabled = ActDataCollectConfig.getAudioSelection();
		m_blnVideoEnabled = ActDataCollectConfig.getVideoSelection();

		if (m_blnGyroEnabled == false && 
			m_blnAcclEnabled == false && 
			m_blnLightEnabled == false && 
			m_blnAudioEnabled == false && 
			m_blnVideoEnabled == false) {
			// Ask user to select sensor/audio/video
			showInformation("Please select data source");
			ActDataCollectConfig.setRecordingStatus(false);
			return;
		}
		
		if (m_blnGyroEnabled == true || 
			m_blnAcclEnabled == true ||
			m_blnLightEnabled == true) {
			blnSensorSelected = true;
		}
		
		blnAudioSelected = m_blnAudioEnabled;
		blnVideoSelected = m_blnVideoEnabled;
						
 		m_lSensorDataFileInterval = ActDataCollectConfig.getRecordDuration();
 		
		m_iGyroMode = ActDataCollectConfig.getSensorDelayMode();
		m_iAcclMode = ActDataCollectConfig.getSensorDelayMode();
		m_iLightMode = ActDataCollectConfig.getSensorDelayMode();		
		m_iGravityMode = ActDataCollectConfig.getSensorDelayMode();
		
		// TODO Auto-generated method stub
		if (m_blnGyroEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_GYROSCOPE),m_iGyroMode);					
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		} 

		if (m_blnAcclEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),m_iAcclMode);
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
		}

/*		
		if (m_blnAcclEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),m_iAcclMode);
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		}
*/
		
		if (m_blnLightEnabled) {
			m_smActData.registerListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LIGHT),m_iLightMode);					
		} else {
			m_smActData.unregisterListener(m_actHome, m_smActData.getDefaultSensor(Sensor.TYPE_LIGHT));
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
					showInformation("Failed to record data on SD Card!");
					ActDataCollectConfig.setRecordingStatus(false);
					return;
				}
				
			} else {
			} 
		} else {        				
			//NO SD Card
			showInformation("No SD Card!");
			ActDataCollectConfig.setRecordingStatus(false);
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
 				showInformation("Failed to record data on SD Card!");
 				ActDataCollectConfig.setRecordingStatus(false);
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
//			blnRet = startVideoRecording_3();

			if (blnRet == false) {
				showInformation("Failed to record video");
				if (m_blnAudioEnabled) {
					stopAudioRecording();
				}
				
				ActDataCollectConfig.setRecordingStatus(false);
				return;
			}			
		}
		
		
        if (blnSensorSelected == true) {
    	    if (blnVideoSelected == false && blnAudioSelected == false) {
       			sShowInfo = "Recording Sensor";
       		} else if (blnVideoSelected == true && blnAudioSelected == false) {
       			sShowInfo = "Recording Sensor & Video with sound";
       		} else if (blnVideoSelected == false && blnAudioSelected == true) {
       			sShowInfo = "Recording Sensor & Audio";     	        		
       		} else if (blnVideoSelected == true && blnAudioSelected == true) {
       			sShowInfo = "Recording Sensor & Audio & soundless Video";  
       		}
        } else if (blnVideoSelected == true && blnAudioSelected == false) {
       		sShowInfo = "Recording Video with sound";
        } else if (blnVideoSelected == false && blnAudioSelected == true) {
       		sShowInfo = "Recording Audio";
        } else if (blnVideoSelected == true && blnAudioSelected == true) {
        	sShowInfo = "Recording Audio & soundless Video"; 
        }
            	             	        
        showInformation(sShowInfo);
		
        //Save to file once an interval
		m_blnRecordStatus = true;
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
		if (m_lSensorDataFileInterval != 0) {
			//Need to stop after an interval
			m_receiverAlarm = new AlarmReceiver();
			registerReceiver(m_receiverAlarm, new IntentFilter(getString(R.string.myalarm)));
			startTimer();
		}
					
    }
    
       
    public void stopDataCollecting()
    {
		Log.d(LIVE_CARD_TAG, "startDataCollecting---------12");
    	
    	if (ActDataCollectConfig.getRecordingStatus() == false) return;

		if (m_blnAudioEnabled) {
			stopAudioRecording();
		}
		
		if (m_blnVideoEnabled) {			
			stopVideoRecording();
		}
    	
		//Stop record. Close Sensor Record File
		if (m_fwSensorRecord != null) {
			try {
				m_fwSensorRecord.close();
				m_fwSensorRecord = null;
			} catch (IOException e) {
				//
			}
		}
		
		showInformation(getString(R.string.defaultinfo));
			
		if (m_lSensorDataFileInterval != 0) {
			
			if (m_receiverAlarm != null) {
				//Need to stop after an interval
				stopTimer();
				unregisterReceiver(m_receiverAlarm);
			}
		}   

		resetValues();
		m_blnRecordStatus = false;
 
		ActDataCollectConfig.setRecordingStatus(m_blnRecordStatus);
		
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
