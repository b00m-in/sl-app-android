/*
* Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
*
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*    Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
*    Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the
*    distribution.
*
*    Neither the name of Texas Instruments Incorporated nor the names of
*    its contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
*  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
*  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
*  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
*  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
*  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
*  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/

package in.b00m.smartconfig.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import android.os.AsyncTask;
import android.util.Log;

public class AddProfileAsyncTask extends AsyncTask<ArrayList<Object>, Void, Boolean> {

	private static final String TAG = "AddProfileAsyncTask";
	private AddProfileAsyncTaskCallback mAddProfileAsyncTaskCallback;
	public DeviceVersion mDeviceVersion;
	public String mDeviceName;

	public AddProfileAsyncTask(AddProfileAsyncTaskCallback callBack) {
		mAddProfileAsyncTaskCallback = callBack;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		////Log.d(TAG,"AddProfileAsyncTask onPost started");
		mAddProfileAsyncTaskCallback.addProfileCompleted();
		super.onPostExecute(result);
	}

	@Override
	protected Boolean doInBackground(ArrayList<Object>... params) {
		////Log.d(TAG,"AddProfileAsyncTask doInBackground started");

		ArrayList<Object> list = params[0];
		String deviceName = (String)list.get(0);
		SecurityType ssidToAddSecurityType = (SecurityType)list.get(1);
		String ssidToAdd = (String)list.get(2);
		String ssidToAddSecurityKey = (String)list.get(3);
		String ssidToAddPriority = (String)list.get(4);
		String iotUuid = (String)list.get(5);
                String configurer = (String)list.get(6);
                String coords = (String)list.get(7);

		//Log.d(TAG,"AddProfileAsyncTask doInBackground " + ssidToAdd + " " + ssidToAddSecurityKey);
		try {
			mDeviceVersion = NetworkUtil.getSLVersion(Constants.BASE_URL_NO_HTTP);
		} catch (IOException e) {
			e.printStackTrace();
                        //Log.e(TAG, "AddProfile getSLVersion exception: " + e.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
                        //Log.e(TAG, "AddProfile getSLVersion exception: " + e.toString());
                        mDeviceVersion = DeviceVersion.UNKNOWN;
		}
		////Log.i(TAG,"SL device version: " + mDeviceName);

		if (mDeviceVersion == DeviceVersion.UNKNOWN||mDeviceVersion == null) {
			mAddProfileAsyncTaskCallback.addProfileFailed("Failed to get version of the device");
			return false;
		}

                // set the date and time from phone
                Calendar rightNow = Calendar.getInstance();
                int yea = rightNow.get(Calendar.YEAR);
                int mon = rightNow.get(Calendar.MONTH);
                int dat = rightNow.get(Calendar.DATE);
                int hou = rightNow.get(Calendar.HOUR);
                int min = rightNow.get(Calendar.MINUTE);
                int sec = rightNow.get(Calendar.SECOND);
                String dateTime = "" + yea + "," + mon + "," + dat + "," + hou + "," + min + "," + sec;

                try {
                        if (NetworkUtil.setDatetime(dateTime, Constants.BASE_URL_NO_HTTP, mDeviceVersion)) {
                            print("Set Date " + dateTime);
                        }
                } catch (CertificateException e) {
                        //Log.e(TAG, "AddProfile exception: " + e.toString());
                        e.printStackTrace();
                }


                // set the configurer (owner) - custom post endpoint on device - this post doesn't actually succeed but the headers are read on th cc3220s 
                try {
                        print("Setting Owner " + configurer);
                        if (NetworkUtil.setOwner(configurer, coords, Constants.BASE_URL_NO_HTTP, mDeviceVersion)) {
                            print("Set Owner " + configurer);
                        }
                        else {
                            print("setOwner returned false");
                        }
                } catch (CertificateException e) {
                        e.printStackTrace();
                        //Log.e(TAG,"setOwner exception: " + e.toString());
                }

		if ( !iotUuid.equals("") ) {
			try {
				if (NetworkUtil.setIotUuid(iotUuid, Constants.BASE_URL_NO_HTTP)) {
                    print("Set UUID" + iotUuid);
                }
			} catch (CertificateException e) {
				e.printStackTrace();
			}
		}



		if (!deviceName.equals("")) {
                    try {
                        if (NetworkUtil.setNewDeviceName(deviceName, Constants.BASE_URL_NO_HTTP, mDeviceVersion)) {
                            mDeviceName = deviceName;
                            print("Set a new device name " + deviceName);
                        }
                        else {
                            mAddProfileAsyncTaskCallback.addProfileFailed("Failed to set a new device name");
                            return false;
                        }
                    } catch (CertificateException e) {
                            e.printStackTrace();
                    }
		}
		else {     
			//read device name from device only if not set by the application
			mDeviceName = NetworkUtil.getDeviceName(Constants.BASE_URL_NO_HTTP, mDeviceVersion);
		}
		
		mAddProfileAsyncTaskCallback.addProfileDeviceNameFetched(mDeviceName);
		print("Device name was changed to " + mDeviceName);

		print("Set a new Wifi configuration");
		if (!NetworkUtil.addProfile(Constants.BASE_URL_NO_HTTP, ssidToAddSecurityType, ssidToAdd, ssidToAddSecurityKey, ssidToAddPriority, mDeviceVersion, configurer, coords)) {
			mAddProfileAsyncTaskCallback.addProfileFailed(Constants.DEVICE_LIST_FAILED_ADDING_PROFILE);
			return false;
		}

		print("Starting configuration verification");
		try {
                    NetworkUtil.moveStateMachineAfterProfileAddition(Constants.BASE_URL_NO_HTTP, ssidToAdd, mDeviceVersion);
		} catch (CertificateException e) {
                    //Log.e(TAG, "moveStateMachineAfterProfileAddition exception: " + e.toString());
                    e.printStackTrace();
		}

		return true;
	}

	private void print(String msg) {
		////Log.i(TAG, msg);
		mAddProfileAsyncTaskCallback.addProfileMsg(msg);
	}

	public interface AddProfileAsyncTaskCallback {
		void addProfileCompleted();
		void addProfileDeviceNameFetched(String deviceName);
		void addProfileFailed(String errorMessage);
		void addProfileMsg(String errorMessage);
	}
}
