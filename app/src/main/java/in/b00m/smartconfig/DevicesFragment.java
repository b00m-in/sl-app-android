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


package in.b00m.smartconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import in.b00m.smartconfig.utils.Constants;
import in.b00m.smartconfig.utils.Device_Type_Enum;
import in.b00m.smartconfig.utils.NetworkUtil;
import in.b00m.smartconfig.utils.SharedPreferencesInterface_;
import in.b00m.smartconfig.utils.Device;
import in.b00m.smartconfig.utils.DeviceListAdapter;
import in.b00m.smartconfig.utils.RecentDeviceListAdapter;
import in.b00m.smartconfig.utils.SmartConfigConstants;
import in.b00m.smartconfig.utils.TimerReceiver;


@EFragment(R.layout.tab_devices_view)
public class DevicesFragment extends Fragment {
	private static final String TAG = "DevicesFragment";
	private Logger mLogger = LoggerFactory.getLogger(DevicesFragment.class);
	SharedPreferences sharedpreferences;
	public static final String mypreference = "iot";
	public static final String Name = "deviceIP";
    public MainActivity mainActivity;
    public static View chosenDFromList;

	@Pref
	SharedPreferencesInterface_ prefs;
	@ViewById
	ImageView devices_refresh_button;
	@ViewById
	ProgressBar devices_refresh_spinner;
	@ViewById
	ListView devices_list_listView;
	@ViewById
	ListView devices_recent_listView;
	@Bean
	DeviceListAdapter deviceListAdapter;
	@Bean
	RecentDeviceListAdapter recentDeviceListAdapter;
	/**
	 * mDNS device found receiver
	 */
	BroadcastReceiver scanFinishedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			hideRefreshProgress();
			MainActivity mainActivity = (MainActivity) getActivity();
			mainActivity.setMDNSDevice(null);
		}
	};
	/**
	 * Receiver for updating device list
	 */

	private BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			updateDeviceListTimer(intent);
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
	 * Called after fragment is initialized
	 */
	@AfterViews
	void afterViews() {

		prefs.scanningDisable().put(false);
		MainActivity mainActivity = (MainActivity) getActivity();
		if (!prefs.isScanning().get()) {
			mainActivity.scanForDevices();
		}
		mainActivity.startPing();
		devices_list_listView.setAdapter(deviceListAdapter);
		//initialize timer receiver which responsible to delay the device list refresh (every 5 seconds)
		mainActivity.startService(new Intent(getActivity(), TimerReceiver.class));
		//Log.w(TAG, "Started service");
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				updateDeviceList();
			}
		}, 5000);

		devices_list_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
                if (chosenDFromList != null) {
                    chosenDFromList.setBackgroundColor(getResources().getColor(R.color.color_general_background));
                }
                chosenDFromList = view;
                view.setBackgroundColor(getResources().getColor(R.color.color_connection_text_sc_holo_grey));

				Device dev = (Device) devices_list_listView.getAdapter().getItem(position);
				sharedpreferences = getActivity().getSharedPreferences(mypreference, Context.MODE_PRIVATE);
		        String simplelinkDeviceIp = dev.host;
		        SharedPreferences.Editor editor = sharedpreferences.edit();
				editor.putString(Name, simplelinkDeviceIp);
				editor.commit();
				//Log.i(TAG, "Entered IP into SP: " + dev.host);
				mLogger.info("*AP* Entered IP into SP: " + dev.host);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new OTAAndType().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, simplelinkDeviceIp);
                } else {
                    new OTAAndType().execute(simplelinkDeviceIp);
                }
				Toast.makeText(getActivity(),"IP: " + dev.host, Toast.LENGTH_LONG).show();
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		if (prefs.isScanning().get()) {
			showRefreshProgress();
		} else {
			prefs.scanningDisable().put(false);
			((MainActivity) getActivity()).scanForDevices();
			hideRefreshProgress();
			devices_refresh_button();
		}
		
		getActivity().registerReceiver(scanFinishedReceiver, new IntentFilter(SmartConfigConstants.SCAN_FINISHED_BROADCAST_ACTION));
		getActivity().registerReceiver(deviceFoundReceiver, new IntentFilter(TimerReceiver.COUNTDOWN_BR));
		//Log.i(TAG, "Registered broadcast received");

		updateDeviceList();
		updateRecentDeviceList();
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.startService(new Intent(getActivity(), TimerReceiver.class));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(scanFinishedReceiver);
		getActivity().unregisterReceiver(deviceFoundReceiver);
		//Log.i(TAG, "Unregistered broadcast receivers");
		//Log.w(TAG, "onPause stop scanning mDNS");
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.stopScanning();
		mainActivity.setMDNSDevice(null);
		mainActivity.stopService(new Intent(getActivity(), TimerReceiver.class));
	}
	
	@Click
	void devices_refresh_button() {
		updateDeviceList();
		if (prefs.isScanning().get()) {
			showRefreshProgress();
			//Log.d(TAG,"DevicesFragment/refresh button - already scanning");
		} else {
			prefs.scanningDisable().put(false);
			((MainActivity) getActivity()).scanForDevices();
			//Log.d(TAG,"DevicesFragment/refresh button - start scan");
		}
	}
	
	@ItemClick
	void devices_list_listView(Device device) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + device.host));
		startActivity(browserIntent);
	}

	@ItemClick
	void devices_recent_listView(Device device) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + device.host));
		startActivity(browserIntent);
	}

	@UiThread

	void updateDeviceList() {
				deviceListAdapter.notifyDataSetChanged();
	}
	
	@UiThread
	void updateRecentDeviceList() {
				recentDeviceListAdapter.notifyDataSetChanged();
	}

	@UiThread
	void showRefreshProgress() {
		devices_refresh_button.setVisibility(View.INVISIBLE);
		devices_refresh_spinner.setVisibility(View.VISIBLE);
	}
	
	@UiThread
	void hideRefreshProgress() {
		devices_refresh_spinner.setVisibility(View.INVISIBLE);
		devices_refresh_button.setVisibility(View.VISIBLE);
	}
	private void updateDeviceListTimer(Intent intent) {
		if (intent.getExtras() != null) {
			long millisUntilFinished = intent.getLongExtra("countdown", 0);

			if(millisUntilFinished<2000){
				//fire the updateDeviceList method after 30 sec delay
				updateDeviceList();
				//Log.i(TAG, "Update table after 30 seconds delay ");

			}
		}
	}

	class OTAAndType extends AsyncTask<String,Void,Device_Type_Enum> {
		@Override
		protected Device_Type_Enum doInBackground(String... params) {
			//Log.i(TAG, "OTAAndType doInBackground");
			Device_Type_Enum deviceTypeEnum = null;
			String baseUrl = "://" + params[0];
			//Log.i(TAG,"OTAAndType baseUrl: " + baseUrl);
			try {
				deviceTypeEnum = NetworkUtil.slDeviceOTAAndType(baseUrl);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//Log.i(TAG, "OTAAndType: " + deviceTypeEnum);
			return deviceTypeEnum;
		}

		@Override
		protected void onPostExecute(Device_Type_Enum deviceTypeEnum) {
			super.onPostExecute(deviceTypeEnum);
			//Log.i(TAG, "OTAAndType onPost, result: " + deviceTypeEnum);
			if (mainActivity != null) {
				mainActivity.deviceTypeEnum = deviceTypeEnum;
				//Log.i(TAG, "OTAAndType set result to main: " + mainActivity.deviceTypeEnum);
                //refresh tabs in order to display extra tabs
				mainActivity.clearAllTabs();
				mainActivity.initTabs(Constants.DEVICES_TAB);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mainActivity = (MainActivity) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
        mainActivity = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity mainActivity=(MainActivity)getActivity();
		mainActivity.stopService(new Intent(getActivity(), TimerReceiver.class));
	}
}