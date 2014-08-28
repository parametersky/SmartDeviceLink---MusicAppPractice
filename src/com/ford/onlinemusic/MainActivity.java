package com.ford.onlinemusic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


/*
 * This is the main activity just for demo, in a real app this activity handles the UI
 */
public class MainActivity extends Activity {

	private final String TAG = "MainActivity";
	// lockscreen view
	private static ImageView lockscreen;
	
	// used to indicate whether the activity is foreground.
	private static boolean isAppRunning = false;
	
	// used to indicate whether the lockscreen should be show.
	private static boolean showLockscreen = false;
	
	private final int ACTION_SHOWLOCKSCREEN = 1;
	private final int ACTION_REMOVELOCKSCREEN = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate ");
		setContentView(R.layout.activity_main);
		
		//start applink service if needed
		startAppLinkService();
		lockscreen = (ImageView) findViewById(R.id.lockscreen);
		
		//register broadcast receiver for lockscreen broadcast
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction("com.kyle.lockscreen");
		registerReceiver(mBR, intentfilter);
		
		//if MainActivity is started by FordService, get the lockscreen status 
		showLockscreen = getIntent().getBooleanExtra("LOCKSCREEN", false);
		
	}

	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume showlockscreen:  " + showLockscreen);
		isAppRunning = true;
		
		// to show lockscreen if needed
		if (showLockscreen) {
			showLockscreen();
		} else {
			removeLockscreen();
		}
		
	}

	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause ");
		isAppRunning = false;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy ");
		
		//stop applink service
		stopAppLinkService();
		unregisterReceiver(mBR);
	}

	//handler to show/remove lockscreen
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int action = msg.what;
			switch (action) {
			case ACTION_SHOWLOCKSCREEN:
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isAppRunning)
							return;
						int visibility = lockscreen.getVisibility();
						if (visibility == View.GONE) {
							Log.d(TAG, "set lockscreen on");
							lockscreen.setVisibility(View.VISIBLE);
						}
					}
				});
				break;
			case ACTION_REMOVELOCKSCREEN:
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isAppRunning)
							return;
						int visibility = lockscreen.getVisibility();
						if (visibility == View.VISIBLE) {
							Log.d(TAG, "set lockscreen off");
							lockscreen.setVisibility(View.GONE);
						}
					}
				});
				break;
			default:
				break;
			}
		}
	};

	
	//Receiver to receive lockscreen broadcast from FordService
	public BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			boolean isLock = intent.getBooleanExtra("LOCK", false);
			Log.d("Kyle MainActivity", "onReceive lockscreen: " + isLock);
			showLockscreen = isLock;
			if (isLock) {
				mHandler.sendEmptyMessage(ACTION_SHOWLOCKSCREEN);
			} else {
				mHandler.sendEmptyMessage(ACTION_REMOVELOCKSCREEN);
			}
		}
	};

	/*
	 * Called in onCreate() to start AppLink service so that the app is
	 * listening for a SYNC connection in the case the app is installed or
	 * restarted while the phone is already connected to SYNC.
	 */
	public void startAppLinkService() {
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter != null && mBtAdapter.isEnabled()) {
			Intent intent = new Intent();
			intent.setClass(this, FordService.class);
			startService(intent);
		}
	}

	/*
	 * Called in onDestroy() to stop AppLink service when user exits the app or
	 * the app crashes
	 */
	public void stopAppLinkService() {
		Intent intent = new Intent();
		intent.setClass(this, FordService.class);
		stopService(intent);
	}

	public static boolean getAppIsRunning() {
		return isAppRunning;
	}

	/*
	 * called in lockscreen receiver to show lockscreen
	 */
	public void showLockscreen() {
		mHandler.removeMessages(ACTION_REMOVELOCKSCREEN);
		mHandler.sendEmptyMessage(ACTION_SHOWLOCKSCREEN);
	}

	/*
	 * called in lockscreen receiver to remove lockscreen
	 */
	public void removeLockscreen() {
		mHandler.removeMessages(ACTION_SHOWLOCKSCREEN);
		mHandler.sendEmptyMessage(ACTION_REMOVELOCKSCREEN);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}
}
