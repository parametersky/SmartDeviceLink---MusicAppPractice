package com.ford.onlinemusic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FordReceiver extends BroadcastReceiver {

	private final String TAG = "FordReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		String action = intent.getAction();

		if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
			Log.d(TAG, "received action CONNECTED");
			BluetoothDevice bd = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Log.d(TAG, "connected with device: " + bd.getName());
			if (bd.getName().contains("SYNC")) {
				Log.d(TAG, "connected with sync");
				if (FordService.getInstance() == null) {
					Log.d(TAG, "send intent to start FordService");
					Intent serviceIntent = new Intent(context,
							FordService.class);
					context.startService(serviceIntent);
				} else if (FordService.getInstance().getProxy() == null) {
					FordService.getInstance().startProxy();
				}
			}
		} else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
			int bluetoothState = intent.getIntExtra(
					BluetoothAdapter.EXTRA_STATE, -1);
			switch (bluetoothState) {
			case BluetoothAdapter.STATE_TURNING_OFF:
				Log.d(TAG, "received action Turning_off");
				if (FordService.getInstance() != null) {
					Log.d(TAG,
							"State turning off : send intent to stop FordService");
//					Intent serviceIntent = new Intent(context,
//							FordService.class);
//					context.stopService(serviceIntent);
				}
				break;
			case BluetoothAdapter.STATE_TURNING_ON:
				Log.d(TAG, "received action Turning_on");
				Log.d(TAG, "connected with sync");
				if (FordService.getInstance() == null) {
					Log.d(TAG, "send intent to start FordService");
					Intent serviceIntent = new Intent(context,
							FordService.class);
					context.startService(serviceIntent);
				}
				break;
			default:
				break;
			}
		} 
	}
}