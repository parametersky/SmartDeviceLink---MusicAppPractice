package com.ford.onlinemusic.applink;

import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;

import org.json.JSONException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;

import com.ford.onlinemusic.MainActivity;
import com.ford.onlinemusic.MusicPlayerService;
import com.ford.onlinemusic.R;
import com.ford.onlinemusic.SongData;
import com.ford.onlinemusic.SongList;
import com.ford.onlinemusic.R.string;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleData;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.Image;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnSyncPData;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.SyncPDataResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.AudioStreamingState;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.HMILevel;
import com.ford.syncV4.proxy.rpc.enums.ImageType;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.proxy.rpc.enums.TriggerSource;
import com.ford.syncV4.proxy.rpc.enums.UpdateMode;

//import android.util.Log;
/*
 * Main implementation of AppLink, responsible for:
 * 1. create/dispose SyncProxyALM, handle connection with SYNC
 * 2. building UI on SYNC, when get HMI_FULL first time, send show, addcommand, createchoiceset to SYNC.
 * 3. handling user action with SYNC and notification from SYNC.
 * 4. send lockscreen broadcast to MainActivity.
 * 5. update Music Player Service status on SYNC
 */
public class AppLinkService extends Service implements IProxyListenerALM {

	private static final int CMD_ID_MOSTPOPULAR = 1011;
	private static final int CMD_ID_FAVORITES = 1012;
	private static final int CMD_ID_PLAYLISTS = 1013;
	private static final int CMD_ID_LOCAL = 1014;
	private static final int CMD_ID_ADDFAVORITE = 1017;
	private static final int CMD_ID_SONGINFO = 1018;
	private static final int CMD_ID_NEWAGE = 1019;
	private static final int BTN_ID_PLAY = 1022;
	private static final int BTN_ID_PAUSE = 1025;
	private static final int BTN_ID_PLAYLISTS = 1024;
	private static final int BTN_ID_FAVORITE = 1313;
	private static final int BTN_ID_UNFAVORITE = 1316;
	private static final int CHS_ID_PLAYLISTS = 1041;
	private static AppLinkService instance = null;
	private SyncProxyALM mSyncProxy = null;
	private final String TAG = "AppLinkService";
	private int correlationID = 1;
	private HMILevel hmilevel = null;

	private SongList favoritesSonglist1 = null;
	private SongList newAgeSonglist2 = null;
	private SongList localsongs = null;
	private SongList mostpopularsongs = null;

	private Vector<SoftButton> mCommonSoftbutton = null;
	private Vector<Choice> mQQMusicChoiceSet = null;
	private SongList currentList = null;
	private SoftButton playbutton;
	private SoftButton pausebutton1;
	private SoftButton localbutton;
	private SoftButton listbutton;

	private SoftButton favoritebutton;
	private SoftButton unfavoritebutton;
	// private SoftButton sharebutton;
	// private SoftButton infobutton;

	private SoftButton mHighlightedSonglistButton = null;
	// init mode data

	private boolean isPaused = false;
	private boolean isLock = false;
	private boolean isRandom = false;
	private boolean getFirstRun = false;

	private String mPlayerStatus = null;

	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Log.i("Kyle", "lenght is " + msg.arg1);
			handlePlayerStatusChange(mPlayerStatus, msg.arg1);
		}

	};
	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (!getFirstRun) {
				return;
			}
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

				mPlayerStatus = status;
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = length;
				mHandler.removeMessages(1);
				mHandler.sendMessageDelayed(msg, 200);

			}
		}
	};

	public void handlePlayerStatusChange(String status, int length) {
		Log.i("Kyle", "handle player status change: " + status);
		SongData song = currentList.getCurrSong();
		String name = song.getName();
		String artist = song.getArtist();
		try {
			if (status.equalsIgnoreCase("Buffering")) {
				mSyncProxy.setMediaClockTimer(null, null, null,
						UpdateMode.PAUSE, correlationID++);
				mSyncProxy.show(name, artist, null, null, null, null,
						getStringValue(R.string.bufferring), null, null, null,
						TextAlignment.CENTERED, correlationID++);
			} else if (status.equalsIgnoreCase("paused")) {
				mCommonSoftbutton.remove(0);
				mCommonSoftbutton.add(0, pausebutton1);
				mSyncProxy.setMediaClockTimer(null, null, null,
						UpdateMode.PAUSE, correlationID++);
				mSyncProxy.show(null, null, null, null, null, null,
						getStringValue(R.string.paused), null,
						mCommonSoftbutton, null, null, correlationID++);

			} else if (status.equalsIgnoreCase("network_error")) {
				mSyncProxy.show(null, null, null, null, null, null,
						getStringValue(R.string.networkerror), null, null,
						null, null, correlationID++);

			} else if (status.equalsIgnoreCase("playing")) {
				mCommonSoftbutton.remove(0);
				mCommonSoftbutton.add(0, playbutton);
				int minutes = length / 60;
				int seconds = length % 60;
				Log.i(TAG, "isPaused: " + isPaused);
				mSyncProxy.setMediaClockTimer(0, minutes, seconds,
						UpdateMode.COUNTUP, correlationID++);
				mSyncProxy.show(name, artist, null, null, null, null,
						currentList.ListName, null, mCommonSoftbutton, null,
						TextAlignment.CENTERED, correlationID++);

				isPaused = false;
			}
		} catch (SyncException e) {
			e.printStackTrace();
		}
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
	}

	public void sendCommand(int command) {
		Intent intent = new Intent(MusicPlayerService.ACTION_COMMAND);
		intent.putExtra(MusicPlayerService.COMMAND, command);
		sendBroadcast(intent);
	}

	public void buildPlaylistChoiceSet() {
		mQQMusicChoiceSet = new Vector<Choice>();

		Choice choice1 = new Choice();
		choice1.setChoiceID(1031);
		choice1.setMenuName(favoritesSonglist1.ListName);
		choice1.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { favoritesSonglist1.ListName })));
		Image image = new Image();
		image.setImageType(ImageType.STATIC);
		image.setValue("0x11");
		choice1.setImage(image);

		mQQMusicChoiceSet.add(choice1);

		Choice choice2 = new Choice();
		choice2.setChoiceID(1032);
		choice2.setMenuName(newAgeSonglist2.ListName);
		choice2.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { newAgeSonglist2.ListName })));
		mQQMusicChoiceSet.add(choice2);

	}

	public void subscribeMusicButton() {
		try {
			mSyncProxy.subscribeButton(ButtonName.SEEKLEFT, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.SEEKRIGHT, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.OK, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_0, correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getStringValue(int id) {
		return getResources().getString(id);
	}

	public void buildTrackSoftButton() {
		mCommonSoftbutton = new Vector<SoftButton>();
		pausebutton1 = new SoftButton();
		pausebutton1.setSoftButtonID(BTN_ID_PLAY);
		Image image = new Image();
		image.setImageType(ImageType.STATIC);
		image.setValue("0xD0");
		pausebutton1.setImage(image);
		pausebutton1.setIsHighlighted(false);
		pausebutton1.setSystemAction(SystemAction.DEFAULT_ACTION);
		pausebutton1.setType(SoftButtonType.SBT_IMAGE);

		playbutton = new SoftButton();
		playbutton.setSoftButtonID(BTN_ID_PAUSE);
		Image playimage = new Image();
		playimage.setImageType(ImageType.STATIC);
		playimage.setValue("0xCF");
		playbutton.setImage(playimage);
		playbutton.setIsHighlighted(false);
		playbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		playbutton.setType(SoftButtonType.SBT_IMAGE);
		mCommonSoftbutton.add(pausebutton1);

		favoritebutton = new SoftButton();
		favoritebutton.setType(SoftButtonType.SBT_TEXT);
		favoritebutton.setIsHighlighted(false);
		favoritebutton.setSoftButtonID(BTN_ID_FAVORITE);
		favoritebutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		favoritebutton.setText(getStringValue(R.string.save));
		mCommonSoftbutton.add(favoritebutton);

		listbutton = new SoftButton();
		listbutton.setSoftButtonID(BTN_ID_PLAYLISTS);
		listbutton.setText(getStringValue(R.string.playlists));
		listbutton.setType(SoftButtonType.SBT_TEXT);
		listbutton.setIsHighlighted(false);
		listbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		mCommonSoftbutton.add(listbutton);

		unfavoritebutton = new SoftButton();
		unfavoritebutton.setType(SoftButtonType.SBT_TEXT);
		unfavoritebutton.setIsHighlighted(true);
		unfavoritebutton.setSoftButtonID(BTN_ID_UNFAVORITE);
		unfavoritebutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		unfavoritebutton.setText(getStringValue(R.string.save));

	}

	public void pump(String content, String text2) {
		try {
			mSyncProxy.alert(content, text2, false, 3000, correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void voicePump(String content, String text2) {
		try {
			String voicestring = null;
			if (text2 == null) {
				voicestring = content;
			} else {
				voicestring = content + "," + text2;
			}
			mSyncProxy.alert(
					TTSChunkFactory.createSimpleTTSChunks(voicestring),
					content, text2, false, 3000, correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void scrollableMessage(String message, Vector<SoftButton> softButtons) {
		try {
			mSyncProxy.scrollablemessage(message, 10000, softButtons,
					correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void performInteraction(int choicesetid, String initprompt,
			String displaytext, InteractionMode mode) {
		try {
			mSyncProxy.performInteraction(initprompt, displaytext, choicesetid,
					null, null, mode, 10000, correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendTrackData() {
		buildSongList();
		buildPlaylistChoiceSet();
		buildTrackSoftButton();
		try {
			startPlayList(favoritesSonglist1);

			mSyncProxy.subscribeButton(ButtonName.OK, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_7, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_8, correlationID++);

			mSyncProxy
					.addCommand(
							CMD_ID_MOSTPOPULAR,
							getStringValue(R.string.mostpopular),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.mostpopular) })),
							"0x11", ImageType.STATIC, correlationID++);
			// AddCommand addcommand = new AddCommand();
			// addcommand.setCmdIcon(cmdIcon)
			mSyncProxy
					.addCommand(
							CMD_ID_FAVORITES,
							getStringValue(R.string.favorites),
							1,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.favorites) })),
							"0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							CMD_ID_LOCAL,
							getStringValue(R.string.local),
							3,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.local) })),
							null, null, correlationID++);

			mSyncProxy.createInteractionChoiceSet(mQQMusicChoiceSet,
					CHS_ID_PLAYLISTS, correlationID++);
			mSyncProxy
					.addCommand(
							CMD_ID_ADDFAVORITE,
							getStringValue(R.string.addfavorite),
							5,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.addfavorite) })),
							"0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							CMD_ID_SONGINFO,
							getStringValue(R.string.songinfo),
							4,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.songinfo) })),
							"0x11", ImageType.STATIC, correlationID++);
			// mSyncProxy
			// .addCommand(
			// CMD_ID_PLAYLISTS,
			// getStringValue(R.string.playlists),
			// 0,
			// new Vector<String>(
			// Arrays.asList(new String[] { getStringValue(R.string.playlists)
			// })),
			// "0x11", ImageType.STATIC, correlationID++);

			mSyncProxy
					.addCommand(
							CMD_ID_PLAYLISTS,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.playlists) })),
							correlationID++);

			mSyncProxy
					.addCommand(
							CMD_ID_NEWAGE,
							getStringValue(R.string.newage),
							2,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.newage) })),
							"0x11", ImageType.STATIC, correlationID++);

			subscribeMusicButton();
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void startPlayList(SongList list) {
		if (currentList == list) {
			return;
		}
		currentList = list;
		MusicPlayerService.setPlayList(list);
		startMediaPlayer();
		updateScreenSongInfo(currentList.getSong(currentList.CurrentSong));

	}

	public void updateScreenSongInfo(SongData song) {
		// default set the song is not in the favorite list.
		Log.i("Kyle", "update Screen Song Info");
		mCommonSoftbutton.remove(0);
		mCommonSoftbutton.add(0, pausebutton1);
		if (favoritesSonglist1.findSong(song.getName()) != -1) {
			mCommonSoftbutton.remove(1);
			mCommonSoftbutton.add(1, unfavoritebutton);
		} else {
			mCommonSoftbutton.remove(1);
			mCommonSoftbutton.add(1, favoritebutton);
		}
		try {
			mSyncProxy.setMediaClockTimer(null, null, null, UpdateMode.CLEAR,
					correlationID++);
			mSyncProxy.show(song.getName(), song.getArtist(), null, null, null,
					null, getStringValue(R.string.bufferring), null,
					mCommonSoftbutton, null, null, correlationID++);

		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void playNext() {
		sendCommand(MusicPlayerService.CMD_NEXT);
		updateScreenSongInfo(currentList.getCurrSong());
	}

	public void playPrev() {
		sendCommand(MusicPlayerService.CMD_PREV);
		updateScreenSongInfo(currentList.getCurrSong());
	}

	public void startMediaPlayer() {
		sendCommand(MusicPlayerService.CMD_START);
		// isPaused = false;
	}

	public void pauseMediaPlayer() {
		sendCommand(MusicPlayerService.CMD_PAUSE);
		isPaused = true;
	}

	public void stopMediaPlayer() {
		sendCommand(MusicPlayerService.CMD_PAUSE);
	}

	public static AppLinkService getInstance() {
		return instance;
	}

	public SyncProxyALM getProxy() {
		return mSyncProxy;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");
		instance = this;

		// start music player service
		Intent intent = new Intent();
		intent.setClass(this, MusicPlayerService.class);
		startService(intent);

		// register broadcast receiver to get music player service's status
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(MusicPlayerService.ACTION_CONTENT_PLAYING);
		registerReceiver(mBR, intentfilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flag, int startId) {
		Log.i(TAG, "onStartCommand");
		startProxy();
		return 0;
	}

	public static String _FUNC_() {
		StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
		return traceElement.getMethodName() + "\n";
	}

	/*
	 * called by onStartCommand() to create proxy, if proxy has been created
	 * then do nothing, else create a new one.
	 */
	public void startProxy() {

		// check whether current proxy is available, we should avoid to override
		// current proxy. because that will cause SYNC_UNAVAILABLE exception
		if (mSyncProxy != null && mSyncProxy.getIsConnected()) {
			return;
		}
		try {
			Log.i(TAG, "onStartCommand to connect with SYNC using SyncProxyALM");

			// get current system's language to decide HMI language.
			// this is just for this particular app.
			Language language = null;
			Locale locale = Locale.getDefault();
			String lang = locale.getDisplayLanguage();
			Log.i(TAG, "language is " + lang);
			if (lang.contains("中文")) {
				language = Language.ZH_CN;
			} else {
				language = Language.EN_US;
			}

			mSyncProxy = new SyncProxyALM(this,
			getStringValue(R.string.musicappdemo) , true,
					language, language, "1234325");
			SyncProxyALM.enableDebugTool();
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			Log.i(TAG, e.getMessage());

			if (mSyncProxy == null)
				stopSelf();
			e.printStackTrace();
		}
	}

	public void onDestroy() {
		instance = null;

		// dispose proxy when service is destroyed.
		try {
			if (mSyncProxy != null)
				mSyncProxy.dispose();
			mSyncProxy = null;
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "dispose mSyncProxy failed");
			e.printStackTrace();
		}
		Log.i(TAG, "AppLink Service OnDestroy");
		removeLockscreen();
		stopMusicService();
		unregisterReceiver(mBR);
		super.onDestroy();
	}

	public void stopMusicService() {
		// Intent intent = new Intent();
		// intent.setClass(this, MusicPlayerService.class);
		// stopService(intent);
		sendCommand(MusicPlayerService.CMD_PAUSE);
	}

	public void startMainActivity() {
		Intent intent = new Intent();
		intent.setClass(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("LOCKSCREEN", true);
		startActivity(intent);
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		// TODO Auto-generated method stub

		hmilevel = notification.getHmiLevel();

		AudioStreamingState state = notification.getAudioStreamingState();

		switch (state) {
		case AUDIBLE:
			if (!isPaused)
				startMediaPlayer();
			break;
		case NOT_AUDIBLE:
			stopMediaPlayer();
			break;
		case ATTENUATED:
			break;
		}

		switch (hmilevel) {
		case HMI_BACKGROUND:
			Log.i(TAG, "HMI_BACKGOUND");
			break;
		case HMI_FULL:
			Log.i(TAG, "HMI_FULL");
			// when HMI_FULL arrives, see if the lockscreen is showed.
			// when the app is exited and the user enter the app again, first
			// run is false,so we should make sure the lockscreen is on.
			showLockscreen();
			if (notification.getFirstRun()) {
				// when first run comes, start the activity and register
				// commands, subscribe buttons, create choicesets(which will not
				// change through the
				// entire life cycle).
				startMainActivity();
				sendTrackData();
				getFirstRun = true;
			} else {
				if (!isPaused) {
					startMediaPlayer();
				}
			}
			break;
		case HMI_NONE:
			Log.i(TAG, "HMI_NONE");
			// remove the lockscreen and stop playing music.
			removeLockscreen();
			stopMediaPlayer();
		case HMI_LIMITED:
			Log.i(TAG, "HMI_LIMITED");
		default:
			break;
		}

	}

	public void showLockscreen() {
		if (isLock)
			return;
		Intent intent = new Intent("com.kyle.lockscreen");
		intent.putExtra("LOCK", true);
		sendBroadcast(intent);
		isLock = true;
	}

	public void removeLockscreen() {
		if (!isLock)
			return;
		Intent intent = new Intent("com.kyle.lockscreen");
		intent.putExtra("LOCK", false);
		sendBroadcast(intent);
		isLock = false;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ford.syncV4.proxy.interfaces.IProxyListenerBase#onProxyClosed(java
	 * .lang.String, java.lang.Exception) Called when proxy detects that
	 * connection between SYNC and the Phone breaks
	 */
	public void onProxyClosed(String info, Exception e) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onProxyClosed:" + e.getLocalizedMessage());
		removeLockscreen();
		stopMusicService();
		SyncExceptionCause cause = ((SyncException) e).getSyncExceptionCause();
		if (cause != SyncExceptionCause.SYNC_PROXY_CYCLED
				&& cause != SyncExceptionCause.BLUETOOTH_DISABLED) {
			if (mSyncProxy != null) {
				try {
					mSyncProxy.resetProxy();
				} catch (SyncException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} else {
			stopSelf();
		}
	}

	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		// TODO Auto-generated method stub
		Log.i(TAG, "Add command done for " + response.getCorrelationID()
				+ " result is " + response.getResultCode());
	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub
		Log.e(TAG, "CreateChoiceSet response: " + response.getSuccess()
				+ " code is " + response.getResultCode() + " info is:"
				+ response.getInfo());
	}

	@Override
	public void onAlertResponse(AlertResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGenericResponse(GenericResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnCommand(OnCommand notification) {
		// TODO Auto-generated method stub

		// get the command id
		int id = notification.getCmdID();
		// get the trigger source, a command can be triggered by pressing menu
		// item in More or using voice command
		TriggerSource ts = notification.getTriggerSource();

		switch (id) {
		case CMD_ID_MOSTPOPULAR:
			startPlayList(mostpopularsongs);
			break;
		case CMD_ID_FAVORITES:
			startPlayList(favoritesSonglist1);
			break;
		case CMD_ID_PLAYLISTS:
			// use different response on the trigger source.
			if (ts.equals(TriggerSource.TS_MENU)) {
				performInteraction(CHS_ID_PLAYLISTS,
						getStringValue(R.string.selectplaylist),
						getStringValue(R.string.selectplaylist),
						InteractionMode.MANUAL_ONLY);
			} else {
				performInteraction(CHS_ID_PLAYLISTS,
						getStringValue(R.string.selectplaylist),
						getStringValue(R.string.selectplaylist),
						InteractionMode.BOTH);
			}

			break;
		case CMD_ID_LOCAL:
			if (localsongs.size() > 1) {
				startPlayList(localsongs);
			} else {
				voicePump(getStringValue(R.string.nolocalmusic), null);
			}
			break;

		case CMD_ID_ADDFAVORITE:

			// different response on different trigger source, if triggered by
			// menu, give a silent pop-up, else give a voice pop-up
			if (ts.equals(TriggerSource.TS_MENU)) {
				pump(getStringValue(R.string.savesuccess), null);
			} else {
				voicePump(getStringValue(R.string.savesuccess), null);
			}
			mCommonSoftbutton.remove(1);
			mCommonSoftbutton.add(1, unfavoritebutton);
			favoritesSonglist1.addSong(currentList.getCurrSong());
			try {
				mSyncProxy.show(null, null, null, null, null,
						mCommonSoftbutton, null, null, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case CMD_ID_SONGINFO:
			SongData song = currentList.getSong(currentList.CurrentSong);
			if (ts.equals(TriggerSource.TS_MENU)) {
				pump(song.getArtist(), song.getName());
			} else {
				voicePump(song.getArtist(), song.getName());
			}
			break;

		case CMD_ID_NEWAGE:
			startPlayList(newAgeSonglist2);
			break;
		default:
			break;
		}

		// useless code for setting play mode.
		// if (currentList != null)
		// currentList.setRandom(isRandom);
	}

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ford.syncV4.proxy.interfaces.IProxyListenerBase#
	 * onPerformInteractionResponse
	 * (com.ford.syncV4.proxy.rpc.PerformInteractionResponse) Handle response of
	 * PerformInteraction
	 */
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		// TODO Auto-generated method stub

		Log.i(TAG, "response info is " + response.getInfo() + " code is "
				+ response.getResultCode());
		// if success continue,else do nothing
		if (response.getSuccess()) {
			// get choice id user has selected. If choices never change, they
			// can be sent to SYNC using RPC CreateInteractionChoiceSet when get
			// HMI_FULL first time. Else, they should be sent before
			// PerformInteraction is called.
			int choiceid = response.getChoiceID();
			switch (choiceid) {
			case 1031:
				// if user select playlist that is playing, give the user a
				// notification
				if (currentList.equals(favoritesSonglist1)) {
					voicePump(getStringValue(R.string.nowplayinglist),
							favoritesSonglist1.ListName);
					return;
				}
				startPlayList(favoritesSonglist1);
				break;
			case 1032:
				if (currentList.equals(newAgeSonglist2)) {
					voicePump(getStringValue(R.string.nowplayinglist),
							newAgeSonglist2.ListName);
					return;
				}
				startPlayList(newAgeSonglist2);
				break;
			default:
				break;
			}
		}

		// // useless code
		// if (currentList != null)
		// currentList.setRandom(isRandom);
	}

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetGlobalPropertiesResponse(
			SetGlobalPropertiesResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
		// TODO Auto-generated method stub
		Log.e(TAG, "MediaTimerClock response: " + response.getSuccess()
				+ " code is " + response.getResultCode() + " info is:"
				+ response.getInfo());
	}

	@Override
	public void onShowResponse(ShowResponse response) {
		// TODO Auto-generated method stub
		Log.e(TAG, "Show response: " + response.getSuccess() + " code is "
				+ response.getResultCode() + " info is:" + response.getInfo());
	}

	@Override
	public void onSpeakResponse(SpeakResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		// TODO Auto-generated method stub

		ButtonName name = notification.getButtonName();
		Log.i(TAG, "onButtonPress " + name);
		// if button name is custom_button, that means the soft button has been
		// pressed
		// so get the id of the button and do something.
		if (name.equals(ButtonName.CUSTOM_BUTTON)) {
			int id = notification.getCustomButtonName();

			switch (id) {
			case BTN_ID_PLAY:
				startMediaPlayer();
				break;

			case BTN_ID_PAUSE:
				pauseMediaPlayer();
				break;

			case BTN_ID_PLAYLISTS:
				performInteraction(CHS_ID_PLAYLISTS,
						getStringValue(R.string.selectplaylistmanually),
						getStringValue(R.string.selectplaylist),
						InteractionMode.MANUAL_ONLY);

				break;
			case BTN_ID_FAVORITE:
				mCommonSoftbutton.remove(1);
				mCommonSoftbutton.add(1, unfavoritebutton);
				favoritesSonglist1.addSong(currentList.getCurrSong());
				pump(getStringValue(R.string.savesuccess), null);
				try {
					mSyncProxy.show(null, null, null, null, null,
							mCommonSoftbutton, null, null, correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case BTN_ID_UNFAVORITE:
				pump(getStringValue(R.string.cancelsave), null);
				mCommonSoftbutton.remove(1);
				mCommonSoftbutton.add(1, favoritebutton);
				favoritesSonglist1.removeSong(favoritesSonglist1
						.findSong(currentList.getCurrSong().getName()));
				if (currentList == favoritesSonglist1) {
					sendCommand(MusicPlayerService.CMD_NEXT);
				}
				try {
					mSyncProxy.show(null, null, null, null, null,
							mCommonSoftbutton, null, null, correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		} else if (name.equals(ButtonName.SEEKLEFT)) {
			playPrev();

		} else if (name.equals(ButtonName.SEEKRIGHT)) {
			playNext();
		} else if (name.equals(ButtonName.OK)) {
			if (isPaused) {
				startMediaPlayer();
			} else {
				pauseMediaPlayer();
			}
		} else if (name.equals(ButtonName.PRESET_0)) {
			getVehicleData();
		}
		if (currentList != null)
			currentList.setRandom(isRandom);
	}

	public void getVehicleData() {
		GetVehicleData vehicledata = new GetVehicleData();
		vehicledata.setOdometer(true);
		vehicledata.setSpeed(true);
		vehicledata.setGps(true);
		vehicledata.setCorrelationID(correlationID++);
		try {
			Log.i(TAG, "vehicledata is "
					+ vehicledata.serializeJSON().toString());
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			mSyncProxy.sendRPCRequest(vehicledata);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSubscribeVehicleDataResponse(
			SubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i(TAG, "reponse is " + response.getInfo());
		try {
			Log.i(TAG, "reponse string is "
					+ response.serializeJSON().toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response.getSuccess()) {
			Log.i(TAG,
					"onGetVehileDataResponse Odometer is : "
							+ response.getOdometer());
			Log.i(TAG,
					"onGetVehileDataREsponse Speed is : " + response.getSpeed());
		}

		double speed = response.getSpeed().doubleValue();
		int odometer = response.getOdometer() != null ? response.getOdometer()
				.intValue() : 0;
		Log.i(TAG, "speed is " + speed);
		Log.i(TAG, " odometer is " + odometer);
	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnVehicleData(OnVehicleData notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPerformAudioPassThruResponse(
			PerformAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPerformAudioPassThru response: " + response.getInfo());

	}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPutFileResponse(PutFileResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onListFilesResponse(ListFilesResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnLanguageChange(OnLanguageChange notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSliderResponse(SliderResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(String arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnSyncPData(OnSyncPData arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSyncPDataResponse(SyncPDataResponse arg0) {
		// TODO Auto-generated method stub

	}

}
