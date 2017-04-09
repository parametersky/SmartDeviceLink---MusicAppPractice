package com.ford.onlinemusic.applink;


import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * Receiver for listening for Android OS broad cast intents to start/stop AppLinkService
 */
public class AppLinkReceiver extends BroadcastReceiver {

	private final String TAG = "AppLinkReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		String action = intent.getAction();
//        String ba.getName()
		/*
		 * listen to ACL_CONNECTED to start service when user's phone bluetooth
		 * is turned on but not paired.
		 */
//		if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
//            int bluetoothState = intent.getIntExtra(
//                    BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
//            switch (bluetoothState) {
//			/*
//			 * stop service when the bluetooth is turning off.
//			 */
//                case BluetoothHeadset.STATE_DISCONNECTING:
//                    Log.i(TAG, "received action Turning_off");
//                    if (AppLinkService.getInstance() != null) {
//                        Log.i(TAG,
//                                "State turning off : send intent to stop AppLinkService");
//                        Intent serviceIntent = new Intent(context,
//                                AppLinkService.class);
//                        context.stopService(serviceIntent);
//                    }
//                    break;
//			/*
//			 * listen to STATEE_TURNNING_ON to start service. the broadcast is
//			 * sent to app earlier than ACL_CONNECTED. listening to this
//			 * broadcast can make sure that the app's name appear on SYNC once
//			 * bluetooth is connected rather than user press Find Mobile
//			 * Application.
//			 */
//                case BluetoothHeadset.STATE_CONNECTED:
//                    Log.i(TAG, "received action Turning_on");
//                    Log.i(TAG, "connected with sync");
//                    if (AppLinkService.getInstance() == null) {
//                        Log.i(TAG, "send intent to start AppLinkService");
//                        Intent serviceIntent = new Intent(context,
//                                AppLinkService.class);
//                        context.startService(serviceIntent);
//                    }
//                    break;
//                default:
//                    break;
//            }
//
//		} else
        if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
			int bluetoothState = intent.getIntExtra(
					BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
			switch (bluetoothState) {
			/*
			 * stop service when the bluetooth is turning off.
			 */
			case BluetoothA2dp.STATE_DISCONNECTING:
				Log.i(TAG, "received action Turning_off");
				if (AppLinkService.getInstance() != null) {
					Log.i(TAG,
							"State turning off : send intent to stop AppLinkService");
					Intent serviceIntent = new Intent(context,
							AppLinkService.class);
					context.stopService(serviceIntent);
				}
				break;
			/*
			 * listen to STATEE_TURNNING_ON to start service. the broadcast is
			 * sent to app earlier than ACL_CONNECTED. listening to this
			 * broadcast can make sure that the app's name appear on SYNC once
			 * bluetooth is connected rather than user press Find Mobile
			 * Application.
			 */
			case BluetoothA2dp.STATE_CONNECTED:
				Log.i(TAG, "received action Turning_on");
				Log.i(TAG, "connected with sync");
				if (AppLinkService.getInstance() == null) {
					Log.i(TAG, "send intent to start AppLinkService");
					Intent serviceIntent = new Intent(context,
							AppLinkService.class);
					context.startService(serviceIntent);
				}
				break;
			default:
				break;
			}
		}
	}
}