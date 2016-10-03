package com.example.activitydatacollection;


import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.hardware.SensorManager;

public class ActDataCollectMenu extends Activity {

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
	
	
    private ActDataCollectService mActDataCollectService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        	ActDataCollectService.ActDataCollectBinder binder = (ActDataCollectService.ActDataCollectBinder) service;
        	mActDataCollectService = binder.getService();
  //          if (service instanceof ActDataCollectService) {
  //          	mActDataCollectService = (ActDataCollectService.ActivityBinder) service;
            openOptionsMenu();
  //          }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Nothing to do here.
        }
    };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, ActDataCollectService.class), mConnection, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	int i;
    	int nSensorDelayMode = ActDataCollectConfig.getSensorDelayMode();
    	long lRecordDuration = ActDataCollectConfig.getRecordDuration();
    	
    	
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
            case R.id.stop:
            	//Stop data collection
            	mActDataCollectService.stopDataCollecting();
                stopService(new Intent(this, ActDataCollectService.class));
                return true;
            case R.id.start:
            	//Start data collection
            	mActDataCollectService.updateText("Start pressed!");
            	mActDataCollectService.startDataCollecting();
            	return true;
            case R.id.accl:
            	blnRet = ActDataCollectConfig.getAcclSelection();            	
            	ActDataCollectConfig.setAcclSelection(!blnRet);
            	return true;

            case R.id.gyro:
            	blnRet = ActDataCollectConfig.getGyroSelection();            	
            	ActDataCollectConfig.setGyroSelection(!blnRet);
            	return true;

            case R.id.audio:
            	blnRet = ActDataCollectConfig.getAudioSelection();            	
            	ActDataCollectConfig.setAudioSelection(!blnRet);
            	return true;

            case R.id.video:
            	blnRet = ActDataCollectConfig.getVideoSelection();            	
            	ActDataCollectConfig.setVideoSelection(!blnRet);
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
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the Activity.
    	super.onOptionsMenuClosed(menu);
    	
    	//unbindService(mConnection);
    	
        finish();
    }
	
    
}
