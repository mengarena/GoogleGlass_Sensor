package com.example.activitydatacollection;

import android.hardware.SensorManager;

public class ActDataCollectConfig {
	private static boolean m_blnAcclPresent = false;	//Whether the device has Accelerometer
	private static boolean m_blnGyroPresent = false;	//Whether the device has Gyroscope
	private static boolean m_blnLightPresent = false;   //Whether the device has Light sensor
	
	private static long m_lRecordDuration = 0;  // 0--No stop; otherwise--stop at the time
	
	private static boolean m_blnRecordingStatus = false;  //true: Recording;  false: Not Recording

	private static boolean m_blnAcclSelected = false;
	private static boolean m_blnGyroSelected = false;
	private static boolean m_blnLightSelected = false;
	private static boolean m_blnAudioSelected = true;
	private static boolean m_blnVideoSelected = true;
	
	public static int VIDEO_RESOLUTION_720P = 0;
	public static int VIDEO_RESOLUTION_480P = 1;
	
	private static int m_nVideoResolution = VIDEO_RESOLUTION_480P;  //0--720p;  1--480p
	
	//SensorManager.SENSOR_DELAY_GAME = 50 Hz
	//SensorManager.SENSOR_DELAY_UI = 23 Hz
	private static int m_nSensorDelayMode = SensorManager.SENSOR_DELAY_GAME;  //50 Hz
	
	
	public static boolean getRecordingStatus()
	{
		return m_blnRecordingStatus;
	}
	
	public static void setRecordingStatus(boolean blnRecordingStatus)
	{
		m_blnRecordingStatus = blnRecordingStatus;
	}

	
	public static int getSensorDelayMode()
	{
		return m_nSensorDelayMode;
	}
	
	public static void setSensorDelayMode(int nSensorDelayMode)
	{
		m_nSensorDelayMode = nSensorDelayMode;
	}

	
	public static long getRecordDuration()
	{
		return m_lRecordDuration;
	}
	

	public static void setRecordDuration(long lRecordDuration)
	{
		m_lRecordDuration = lRecordDuration;
	}

	public static boolean getGyroPresent()
	{
		return m_blnGyroPresent;
	}
	
	public static void setGyroPresent(boolean blnPresent)
	{
		m_blnGyroPresent = 	blnPresent;
		if (m_blnGyroPresent == false) {
			m_blnGyroSelected = false;
		}
	}

	
	public static boolean getAcclPresent()
	{
		return m_blnAcclPresent;
	}
	
	public static void setAcclPresent(boolean blnPresent)
	{
		m_blnAcclPresent = 	blnPresent;
		if (m_blnAcclPresent == false) {
			m_blnAcclSelected = false;
		}
	}

	
	public static boolean getLightPresent()
	{
		return m_blnLightPresent;
	}
	
	public static void setLightPresent(boolean blnPresent)
	{
		m_blnLightPresent = 	blnPresent;
		if (m_blnLightPresent == false) {
			m_blnLightSelected = false;
		}
	}


	public static boolean getGyroSelection()
	{
		return m_blnGyroSelected;
	}
	
	public static void setGyroSelection(boolean blnSelected)
	{
		m_blnGyroSelected = blnSelected;
	}
	
	
	public static boolean getAcclSelection()
	{
		return m_blnAcclSelected;
	}
	
	public static void setAcclSelection(boolean blnSelected)
	{
		m_blnAcclSelected = blnSelected;
	}


	public static boolean getLightSelection()
	{
		return m_blnLightSelected;
	}
	
	public static void setLightSelection(boolean blnSelected)
	{
		m_blnLightSelected = blnSelected;
	}
	

	
	public static boolean getAudioSelection()
	{
		return m_blnAudioSelected;
	}
	
	public static void setAudioSelection(boolean blnSelected)
	{
		m_blnAudioSelected = blnSelected;
	}

	
	public static boolean getVideoSelection()
	{
		return m_blnVideoSelected;
	}
	
	public static void setVideoSelection(boolean blnSelected)
	{
		m_blnVideoSelected = blnSelected;
	}
	
	public static int getVideoResolution()
	{
		return m_nVideoResolution;
	}
	
	public static void setVideoResolution(int nVideoResolution)
	{
		m_nVideoResolution = nVideoResolution;
	}
		
}
