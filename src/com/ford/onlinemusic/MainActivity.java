package com.ford.onlinemusic;



import com.ford.onlinemusic.applink.AppLinkService;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/*
 * This is the main activity just for demo, in a real app this activity handles the UI
 */
public class MainActivity extends Activity implements View.OnClickListener {

	private final String TAG = "MainActivity";
	private SongList favoritesSonglist1 = null;
	private SongList newAgeSonglist2 = null;
	private SongList localsongs = null;
	private SongList mostpopularsongs = null;

	private static String[] PLAYLISTS = { "本地歌曲", "随便听听", "星标歌曲", "轻音乐" };
	private ListView listview = null;
	private ImageView seekleft = null;
	private ImageView seekright = null;
	private ImageView playorpause = null;
	private TextView title = null;

	private ArrayAdapter<String> localsongadapter = null;
	private ArrayAdapter<String> favoritesongadapter = null;
	private ArrayAdapter<String> newagesongadapter = null;
	private ArrayAdapter<String> randomsongadapter = null;
	private ArrayAdapter<String> songlistapter = null;
	private SongList currentPlayingList = null;
	private boolean isPlaying = false;

	public enum SelectionMode {
		LIST_MODE, SONG_MODE
	}

	private BroadcastReceiver mBRS = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.i(TAG,
					"mBR song'status "
							+ intent.getStringExtra(MusicPlayerService.DATA_STATUS));
			String action = intent.getAction();
			if (action.equals(MusicPlayerService.ACTION_CONTENT_PLAYING)) {
				String name = intent
						.getStringExtra(MusicPlayerService.DATA_NAME);
				String artist = intent
						.getStringExtra(MusicPlayerService.DATA_ARTIST);
				String status = intent
						.getStringExtra(MusicPlayerService.DATA_STATUS);
				String index = intent
						.getStringExtra(MusicPlayerService.DATA_INDEX);
				int length = intent.getIntExtra(MusicPlayerService.DATA_LENGTH,
						0);

				title.setText(name+"  -  "+artist);
				if (status.equalsIgnoreCase("Buffering")) {
					title.append(" "+getStringValue(R.string.bufferring));
				} else if (status.equalsIgnoreCase("paused")) {

				} else if (status.equalsIgnoreCase("network_error")) {

				} else if (status.equalsIgnoreCase("playing")) {
					playorpause.setImageResource(R.drawable.pause);
					isPlaying = true;
				}

			}
		}
	};

	private SelectionMode mode = SelectionMode.LIST_MODE;
	
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
		Log.i(TAG, "onCreate ");
		setContentView(R.layout.activity_main);
		buildSongList();
		initUIComponent();
		IntentFilter intentfilters = new IntentFilter();
		intentfilters.addAction(MusicPlayerService.ACTION_CONTENT_PLAYING);
		registerReceiver(mBRS, intentfilters);
		startMusicService();
		
		// start applink service if needed
		startAppLinkService();
		lockscreen = (ImageView) findViewById(R.id.lockscreen);

		// register broadcast receiver for lockscreen broadcast
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction("com.kyle.lockscreen");
		registerReceiver(mBR, intentfilter);

		// if MainActivity is started by AppLinkService, get the lockscreen status
		showLockscreen = getIntent().getBooleanExtra("LOCKSCREEN", false);

	}

	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume showlockscreen:  " + showLockscreen);
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
		Log.i(TAG, "onPause ");
		isAppRunning = false;
	}

	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy ");

		// stop applink service
		resetAppLinkService();
		unregisterReceiver(mBR);
		unregisterReceiver(mBRS);
	}

	// handler to show/remove lockscreen
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
							Log.i(TAG, "set lockscreen on");
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
							Log.i(TAG, "set lockscreen off");
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

	// Receiver to receive lockscreen broadcast from AppLinkService
	public BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			boolean isLock = intent.getBooleanExtra("LOCK", false);
			Log.i("Kyle MainActivity", "onReceive lockscreen: " + isLock);
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
//		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
//		if (mBtAdapter != null && mBtAdapter.isEnabled()) {
			Intent intent = new Intent();
			intent.setClass(this, AppLinkService.class);
			startService(intent);
//		}
	}

	/*
	 * Called in onDestroy() to reset AppLink service when user exits the app or
	 * the app crashes
	 */
	public void resetAppLinkService() {
		AppLinkService service = AppLinkService.getInstance();
		if(service != null){
			service.resetProxy();
		}
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

	public void initUIComponent() {
		listview = (ListView) findViewById(R.id.listview);
		title = (TextView) findViewById(R.id.title);
		seekleft = (ImageView) findViewById(R.id.seekleft);
		seekright = (ImageView) findViewById(R.id.seekright);
		playorpause = (ImageView) findViewById(R.id.playorpause);
		
		seekleft.setOnClickListener(this);
		seekright.setOnClickListener(this);
		playorpause.setOnClickListener(this);

	    songlistapter = new ArrayAdapter<String>(
				this.getBaseContext(), android.R.layout.simple_list_item_1,
				PLAYLISTS);
		listview.setAdapter(songlistapter);

		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				if (mode == SelectionMode.LIST_MODE) {
					switch (arg2) {
					case 0:
						if (localsongadapter == null) {
							String[] songlist = new String[localsongs.size()];
							localsongs.getAllSongInfo().toArray(songlist);
							localsongadapter = new ArrayAdapter<String>(
									getBaseContext(),
									android.R.layout.simple_list_item_1,
									songlist);
						}
						listview.setAdapter(localsongadapter);
						listview.invalidate();
						currentPlayingList = localsongs;
						break;
					case 1:
						if (randomsongadapter == null) {
							String[] songlist = new String[mostpopularsongs
									.size()];
							mostpopularsongs.getAllSongInfo().toArray(songlist);
							randomsongadapter = new ArrayAdapter<String>(
									getBaseContext(),
									android.R.layout.simple_list_item_1,
									songlist);
						}
						listview.setAdapter(randomsongadapter);
						listview.invalidate();
						currentPlayingList = mostpopularsongs;
						break;
					case 2:
						if (favoritesongadapter == null) {
							String[] songlist = new String[favoritesSonglist1
									.size()];
							favoritesSonglist1.getAllSongInfo().toArray(
									songlist);
							favoritesongadapter = new ArrayAdapter<String>(
									getBaseContext(),
									android.R.layout.simple_list_item_1,
									songlist);
						}
						listview.setAdapter(favoritesongadapter);
						listview.invalidate();
						currentPlayingList = favoritesSonglist1;
						break;
					case 3:
						if (newagesongadapter == null) {
							String[] songlist = new String[newAgeSonglist2
									.size()];
							newAgeSonglist2.getAllSongInfo().toArray(songlist);
							newagesongadapter = new ArrayAdapter<String>(
									getBaseContext(),
									android.R.layout.simple_list_item_1,
									songlist);
						}
						listview.setAdapter(newagesongadapter);
						listview.invalidate();
						currentPlayingList = newAgeSonglist2;
						break;
					}
					mode = SelectionMode.SONG_MODE;
				} else {
					currentPlayingList.CurrentSong = arg2;
					MusicPlayerService.setPlayList(currentPlayingList);
					sendCommand(MusicPlayerService.CMD_START);
				}
			}
		});
	}

	public void sendCommand(int command) {
		Intent intent = new Intent(MusicPlayerService.ACTION_COMMAND);
		intent.putExtra(MusicPlayerService.COMMAND, command);
		sendBroadcast(intent);
	}

	public void buildLocalSongList() {
		localsongs = new SongList(getStringValue(R.string.local));
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = getContentResolver().query(
				uri,
				new String[] { MediaStore.Audio.Media._ID,
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media.ARTIST,
						MediaStore.Audio.Media.ALBUM,
						MediaStore.Audio.Media.DATA }, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				localsongs
						.addSong(new SongData(cursor.getString(1), cursor
								.getString(2), cursor.getString(3), cursor
								.getString(4)));
			} while (cursor.moveToNext());
		}
	}

	public void buildSongList() {
		buildLocalSongList();
		mostpopularsongs = new SongList(getStringValue(R.string.mostpopular));
		mostpopularsongs
				.addSong(new SongData("小苹果", "筷子兄弟", "",
						"http://cc.stream.qqmusic.qq.com/C1000035GveV3i9dBM.m4a?fromtag=52"));
		mostpopularsongs
				.addSong(new SongData("金玉良缘", "李琦", "",
						"http://cc.stream.qqmusic.qq.com/C100001iEkMd4CUugY.m4a?fromtag=52"));
		mostpopularsongs
				.addSong(new SongData("同桌的你", "胡夏", "",
						"http://cc.stream.qqmusic.qq.com/C1000031zaiZ2ZmBYj.m4a?fromtag=52"));
		favoritesSonglist1 = new SongList(getStringValue(R.string.favorites));
		newAgeSonglist2 = new SongList(getStringValue(R.string.newage));
		favoritesSonglist1
				.addSong(new SongData("倩女幽魂", "张国荣", "",
						"http://cc.stream.qqmusic.qq.com/C100001hZjYW0nOsTa.m4a?fromtag=52"));
		favoritesSonglist1
				.addSong(new SongData("当爱已成往事", "张国荣", "",
						"http://cc.stream.qqmusic.qq.com/C100001UK2LJ0KU9ay.m4a?fromtag=52"));
		favoritesSonglist1
				.addSong(new SongData("风继续吹", "张国荣", "",
						"http://cc.stream.qqmusic.qq.com/C100002TvOb41nQrdx.m4a?fromtag=52"));
		newAgeSonglist2
				.addSong(new SongData("故乡的原风景", "宗次郎", "轻音乐",
						"http://cc.stream.qqmusic.qq.com/C100003d4aYZ385awT.m4a?fromtag=52"));
		newAgeSonglist2
				.addSong(new SongData("天空之城", "久石让",
						getStringValue(R.string.playlists),
						"http://cc.stream.qqmusic.qq.com/C100001hPlsk2RVbtF.m4a?fromtag=52"));
		currentPlayingList = localsongs;
	}

	public String getStringValue(int id) {
		return getResources().getString(id);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		switch (id) {
		case R.id.seekleft:
			sendCommand(MusicPlayerService.CMD_PREV);
			isPlaying = true;
			break;
		case R.id.playorpause:
			sendCommand(isPlaying ? MusicPlayerService.CMD_PAUSE
					: MusicPlayerService.CMD_START);
			isPlaying = !isPlaying;
			playorpause.setImageResource(isPlaying?R.drawable.pause:R.drawable.play);
			
			break;
		case R.id.seekright:
			sendCommand(MusicPlayerService.CMD_NEXT);
			isPlaying = true;
			break;
		}
	}
	public void startMusicService(){
		Intent intent = new Intent();
		intent.setClass(this, MusicPlayerService.class);
		startService(intent);
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		switch(keyCode){
		case KeyEvent.KEYCODE_BACK:
			if(mode == SelectionMode.LIST_MODE){
				this.finish();
				Intent intent = new Intent();
				intent.setClass(this, MusicPlayerService.class);
				stopService(intent);
			} else {
				listview.setAdapter(songlistapter);
				listview.invalidate();
				mode = SelectionMode.LIST_MODE;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
