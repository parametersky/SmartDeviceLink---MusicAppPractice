package com.ford.musicapppractice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private static ImageView lockscreen;
	private static boolean isAppRunning = false;
	private static boolean showLockscreen  = false;
	private final int ACTION_SHOWLOCKSCREEN = 1;
	private final int ACTION_REMOVELOCKSCREEN = 2;
	private  Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int action = msg.what;
			switch (action) {
			case ACTION_SHOWLOCKSCREEN:
				showLockscreen = true;
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isAppRunning)
							return;
						int visibility = lockscreen.getVisibility();
						if (visibility == View.GONE) {
							lockscreen.setVisibility(View.VISIBLE);
						}
					}
				});
				break;
			case ACTION_REMOVELOCKSCREEN:
				showLockscreen = false;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!isAppRunning)
							return;
						int visibility = lockscreen.getVisibility();
						if (visibility == View.VISIBLE) {
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
	public BroadcastReceiver mBR = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			boolean isLock = intent.getBooleanExtra("LOCK", false);
			if(isLock){
				mHandler.sendEmptyMessage(ACTION_SHOWLOCKSCREEN);
			} else {
				mHandler.sendEmptyMessage(ACTION_REMOVELOCKSCREEN);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startAppLinkService();
		lockscreen = (ImageView) findViewById(R.id.lockscreen);
		lockscreen.setImageResource(R.drawable.ic_launcher);
		isAppRunning = true;
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction("com.kyle.lockscreen");
		registerReceiver(mBR, intentfilter);
		showLockscreen = getIntent().getBooleanExtra("LOCKSCREEN", false);
	}

	public void onResume() {
		super.onResume();
		isAppRunning = true;
		if (getIntent().getBooleanExtra("LOCKSCREEN", false)) {
			showLockscreen();
		}
	}

	public void onPause() {
		super.onPause();
		isAppRunning = false;
	}
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(mBR);
	}

	public void startAppLinkService() {
		// if()
		Intent intent = new Intent();
		intent.setClass(this, FordService.class);
		startService(intent);
	}

	public static boolean getAppIsRunning() {
		return isAppRunning;
	}

	public static void showLockscreen() {
		
	}

	public static void removeLockscreen() {
		if (!isAppRunning)
			return;
		int visibility = lockscreen.getVisibility();
		if (visibility == View.VISIBLE) {
			lockscreen.setVisibility(View.GONE);
		}
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
		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}
}
