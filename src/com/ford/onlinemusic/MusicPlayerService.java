package com.ford.onlinemusic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class MusicPlayerService extends Service implements OnErrorListener,
		OnCompletionListener, OnPreparedListener {

	private MediaPlayer mPlayer = null;
	private static SongList mCurrentPlayList = null;

	private WifiLock wifilock = null;

	private Notification notification = null;
	private PendingIntent pi = null;

	public final static String ACTION_CONTENT_PLAYING = "com.kyle.webmusic.PLAYING_CONTENT";
	public final static String DATA_NAME = "song_name";
	public final static String DATA_ARTIST = "song_artist";
	public final static String DATA_INDEX = "song_index";
	public final static String DATA_STATUS = "song_status";
	public final static String DATA_LENGTH = "song_length";

	public final static String ACTION_COMMAND = "com.kyle.webmusic.CMD";
	public final static String COMMAND = "command";
	public final static int CMD_START = 110;
	public final static int CMD_PAUSE = 111;
	public final static int CMD_NEXT = 112;
	public final static int CMD_PREV = 109;

	private AudioManager mAudioManager = null;
	private static boolean isPaused = false;
	private boolean mInitailized = false;
	
	private SongData mPlayingSong = null;

	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
		
		@Override
		public void onAudioFocusChange(int focusChange) {
			// TODO Auto-generated method stub
			Log.d("Kyle","onAudioFocus Change: "+focusChange);
			switch(focusChange){
			case AudioManager.AUDIOFOCUS_GAIN:
				start();
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				pause();
				break;
			}
		}
	};
	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Debug.DebugLog(action+" received");
			if (action.equals(ACTION_COMMAND)) {
				int command = intent.getIntExtra(COMMAND, 0);
				Debug.DebugLog(command + " received");
				switch (command) {
				case CMD_START:
					start();
					break;
				case CMD_PAUSE:
					pause();
					break;
				case CMD_NEXT:
					playNext();
					break;
				case CMD_PREV:
					playPrev();
					break;
				default:
					Log.e("Kyle", "Unknow Command");
					break;
				}
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		super.onCreate();
		Debug.DebugLog(Debug._FUNC_());
		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setLooping(false);
		mPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
		wifilock = ((WifiManager) getSystemService(WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		wifilock.acquire();
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnPreparedListener(this);
		mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		
		pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(
				getApplicationContext(), MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		notification = new Notification();
		notification.tickerText = "Service Start";
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this, "playing", "playing", pi);
		startForeground(1011, notification);
		

		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(ACTION_COMMAND);
		registerReceiver(mBR, intentfilter);
	}

	public static void setPlayList(SongList songs) {
		Debug.DebugLog(Debug._FUNC_());
		mCurrentPlayList = songs;
		isPaused = false;
	}

	public void start() {
		Debug.DebugLog(Debug._FUNC_());
		
		mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (isPaused) {
			mPlayer.start();
			updateDisplay(mCurrentPlayList.getCurrSong(), "Playing");
			isPaused = false;
			return;
		}
		
		if (mCurrentPlayList == null || mCurrentPlayList.size() < 1) {
			Toast.makeText(this, "Play List is invalid", Toast.LENGTH_SHORT)
					.show();
			return;
		} else {
			if(mPlayingSong != null && mPlayingSong.equals(mCurrentPlayList.getCurrSong()))return;
			startPlay(mCurrentPlayList.getCurrSong());
			mPlayingSong = mCurrentPlayList.getCurrSong();
		}
	}
	public void setSongToPlay(SongData song){
		
	}
	public void pause() {
		Debug.DebugLog(Debug._FUNC_());
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
			isPaused = true;
			updateDisplay(mCurrentPlayList.getCurrSong(), "Paused");
		}
		
	}

	public void playNext() {
		Debug.DebugLog(Debug._FUNC_());
		startPlay(mCurrentPlayList.getNextSong());
	}

	public void playPrev() {
		Debug.DebugLog(Debug._FUNC_());
		startPlay(mCurrentPlayList.getPrevSong());
	}

	public int startPlay(SongData song) {
		Debug.DebugLog(Debug._FUNC_());
		String url = song.getUrl();
		if (url.isEmpty()) {
			Debug.DebugLog("url is empty");
			return -1;
		}
		try {
			mPlayer.reset();
			mPlayer.setDataSource(url);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		mInitailized = false;
		mPlayer.prepareAsync();
		updateNotification(song);
		updateDisplay(song, "Buffering");
		return 0;
	}

	public void updateNotification(SongData song) {
		notification.setLatestEventInfo(this, "playing", song.getName(), pi);
	}

	public void updateDisplay(SongData song, String status) {
		sendData(song, status);
	}

	public void sendData(SongData song, String status) {
		Intent intent = new Intent();
		intent.setAction(ACTION_CONTENT_PLAYING);
		intent.putExtra(DATA_NAME, song.getName());
		intent.putExtra(DATA_ARTIST, song.getArtist());
		intent.putExtra(DATA_INDEX, (mCurrentPlayList.CurrentSong+1) + "/"
				+ mCurrentPlayList.size());
		intent.putExtra(DATA_STATUS, status);
		if(mInitailized)
		intent.putExtra(DATA_LENGTH,(mPlayer.getCurrentPosition()/1000));
		this.sendBroadcast(intent);
	}

	public int onStartCommand(Intent intent, int flag, int startId) {
		super.onStartCommand(intent, flag, startId);

		return 0;
	}

	public void onDestroy() {
		super.onDestroy();
		if(mPlayer != null)mPlayer.release();
		wifilock.release();
		unregisterReceiver(mBR);
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	}


	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		startPlay(mCurrentPlayList.getNextSong());
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		Log.e("Kyle", "MediaPlayer Error: " + what + " Info is " + extra);
		mp.reset();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		updateDisplay(mCurrentPlayList.getCurrSong(), "Playing");
		mp.start();
		mInitailized = true;
	}

}
