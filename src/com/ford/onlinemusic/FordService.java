package com.ford.onlinemusic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;
import android.provider.MediaStore;
import android.provider.Settings.System;
import android.util.Log;
//import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.ford.musicapppractice.R;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.RPCRequest;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.AppHMIType;
import com.ford.syncV4.proxy.rpc.enums.AppInterfaceUnregisteredReason;
import com.ford.syncV4.proxy.rpc.enums.AudioStreamingState;
import com.ford.syncV4.proxy.rpc.enums.AudioType;
import com.ford.syncV4.proxy.rpc.enums.BitsPerSample;
import com.ford.syncV4.proxy.rpc.enums.ButtonEventMode;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.ButtonPressMode;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.HMILevel;
import com.ford.syncV4.proxy.rpc.enums.ImageType;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SamplingRate;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.proxy.rpc.enums.TriggerSource;
import com.ford.syncV4.proxy.rpc.enums.UpdateMode;
import com.ford.syncV4.transport.BTTransportConfig;
import com.ford.syncV4.transport.BaseTransportConfig;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.util.DebugTool;

public class FordService extends Service implements IProxyListenerALM {

	private static FordService instance = null;
	private SyncProxyALM mSyncProxy = null;
	private final String TAG = "FordService";
	private int correlationID = 1;
	private HMILevel hmilevel = null;

	private SongList favoritesSonglist1 = null;
	private SongList newAgeSonglist2 = null;
	private SongList localsongs = null;
	private SongList randomsongs = null;
	// QQ Music Data
	private Vector<SoftButton> mCommonSoftbutton = null;
	private Vector<Choice> mQQMusicChoiceSet = null;
	private SongList currentList = null;
	private SoftButton playbutton;
	private SoftButton pausebutton1;
	private SoftButton localbutton;
	private SoftButton listbutton;

	private SoftButton favoritebutton;
	private SoftButton unfavoritebutton;
	private SoftButton sharebutton;
	private SoftButton infobutton;

	private SoftButton mHighlightedSonglistButton = null;
	// init mode data
	private Vector<Choice> choicemode = null;
	private int SCENE_MODE = 0;// 1 for qq, 2 for track, 3 for list;
	private int INNER_MODE = 1;// 1 for first mode, 2 for second mode, 3 for
								// third mode.
	// Track Data
	private SoftButton trackplaySBT = null;
	private SoftButton trackPauseSBT = null;

	private boolean isPaused = false;
	private boolean isLock = false;
	private boolean isRandom = false;

	private BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("Kyle",
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
				try {

					if (status.equalsIgnoreCase("Buffering")) {
						mSyncProxy.show(name, artist, null, null, null, null,
								getStringValue(R.string.bufferring), null,
								null, null, null, correlationID++);
						mSyncProxy.setMediaClockTimer(null, null, null,
								UpdateMode.CLEAR, correlationID++);
					} else if (status.equalsIgnoreCase("paused")) {
						mCommonSoftbutton.remove(0);
						mCommonSoftbutton.add(0, pausebutton1);
						mSyncProxy.show(null, null, null, null, null, null,
								getStringValue(R.string.paused), null,
								mCommonSoftbutton, null, null, correlationID++);
						mSyncProxy.setMediaClockTimer(null, null, null,
								UpdateMode.PAUSE, correlationID++);

					} else {
						mCommonSoftbutton.remove(0);
						mCommonSoftbutton.add(0, playbutton);
						if (SCENE_MODE == 2) {
							if (favoritesSonglist1.findSong(name) != -1) {
								mCommonSoftbutton.remove(1);
								mCommonSoftbutton.add(1, unfavoritebutton);
							} else {
								mCommonSoftbutton.remove(1);
								mCommonSoftbutton.add(1, favoritebutton);
							}
						}
						mSyncProxy.show(name, artist, null, null, null, null,
								(currentList.CurrentSong + 1) + "/"
										+ currentList.size(), null,
								mCommonSoftbutton, null, null, correlationID++);
						int minutes = length / 60;
						int seconds = length % 60;
						Log.d("Kyle", "isPaused: " + isPaused);
						mSyncProxy.setMediaClockTimer(0, minutes, seconds,
								UpdateMode.COUNTUP, correlationID++);
						isPaused = false;
					}
				} catch (SyncException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public void buildLocalSongList() {
		// Cursor cursor = ContentProvider
		localsongs = new SongList("local");
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = getContentResolver().query(
				uri,
				new String[] { MediaStore.Audio.Media._ID,
						MediaStore.Audio.Media.DISPLAY_NAME,
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

	public void buildRandomSongList() {

		randomsongs = new SongList(getStringValue(R.string.mostpopular));
		randomsongs
				.addSong(new SongData("å°???¹æ??", "ç­·å?????å¼?", "",
						"http://cc.stream.qqmusic.qq.com/C1000035GveV3i9dBM.m4a?fromtag=52"));
		randomsongs
				.addSong(new SongData("?????????ç¼?", "??????", "",
						"http://cc.stream.qqmusic.qq.com/C100001iEkMd4CUugY.m4a?fromtag=52"));
		randomsongs
				.addSong(new SongData("???æ¡????ä½?", "??¡å??", "",
						"http://cc.stream.qqmusic.qq.com/C1000031zaiZ2ZmBYj.m4a?fromtag=52"));
		// randomsongs.addSong(new SongData("","","",""));
		// randomsongs.addSong(new SongData("","","",""));
		// randomsongs.addSong(new SongData("","","",""));
	}

	public void buildSongList() {
		buildLocalSongList();
		buildRandomSongList();
		favoritesSonglist1 = new SongList(getStringValue(R.string.favorites));
		newAgeSonglist2 = new SongList(getStringValue(R.string.newage));
		favoritesSonglist1
				.addSong(new SongData("??©å¥³å¹½é??", "å¼???½è??", "ç»????",
						"http://cc.stream.qqmusic.qq.com/C100001hZjYW0nOsTa.m4a?fromtag=52"));
		favoritesSonglist1
				.addSong(new SongData("å½???±å·²???å¾?äº?", "å¼???½è??", "ç»????",
						"http://cc.stream.qqmusic.qq.com/C100001UK2LJ0KU9ay.m4a?fromtag=52"));
		favoritesSonglist1
				.addSong(new SongData("é£?ç»§ç»­???", "å¼???½è??", "ç»????",
						"http://cc.stream.qqmusic.qq.com/C100002TvOb41nQrdx.m4a?fromtag=52"));
		newAgeSonglist2
				.addSong(new SongData("???ä¹¡ç?????é£????", "å®?æ¬¡é??", "è½»é?³ä??",
						"http://cc.stream.qqmusic.qq.com/C100003d4aYZ385awT.m4a?fromtag=52"));
		newAgeSonglist2
				.addSong(new SongData("å¤©ç©ºä¹????", "ä¹???³è??",
						getStringValue(R.string.playlists),
						"http://cc.stream.qqmusic.qq.com/C100001hPlsk2RVbtF.m4a?fromtag=52"));
	}

	public void sendCommand(int command) {
		Intent intent = new Intent(MusicPlayerService.ACTION_COMMAND);
		intent.putExtra(MusicPlayerService.COMMAND, command);
		sendBroadcast(intent);
	}

	public void buildQQMusicSoftButton() {
		mCommonSoftbutton = new Vector<SoftButton>();
		pausebutton1 = new SoftButton();
		pausebutton1.setSoftButtonID(1022);
		Image image = new Image();
		image.setImageType(ImageType.STATIC);
		image.setValue("0xD0");
		pausebutton1.setImage(image);
		pausebutton1.setIsHighlighted(false);
		pausebutton1.setSystemAction(SystemAction.DEFAULT_ACTION);
		pausebutton1.setType(SoftButtonType.SBT_IMAGE);

		playbutton = new SoftButton();
		playbutton.setSoftButtonID(1025);
		Image playimage = new Image();
		playimage.setImageType(ImageType.STATIC);
		playimage.setValue("0xCF");
		playbutton.setImage(playimage);
		playbutton.setIsHighlighted(false);
		playbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		playbutton.setType(SoftButtonType.SBT_IMAGE);
		mCommonSoftbutton.add(playbutton);

		localbutton = new SoftButton();
		localbutton.setSoftButtonID(1023);
		localbutton.setText("??????");
		localbutton.setType(SoftButtonType.SBT_TEXT);
		localbutton.setIsHighlighted(false);
		localbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		mCommonSoftbutton.add(localbutton);

		listbutton = new SoftButton();
		listbutton.setSoftButtonID(1024);
		listbutton.setText(getStringValue(R.string.playlists));
		listbutton.setType(SoftButtonType.SBT_TEXT);
		listbutton.setIsHighlighted(false);
		listbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		mCommonSoftbutton.add(listbutton);
	}

	public void buildQQMusicChoiceSet() {
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
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendQQMusicData() {
		buildSongList();
		buildQQMusicChoiceSet();
		buildQQMusicSoftButton();
		try {
			currentList = favoritesSonglist1;
			MusicPlayerService.setPlayList(favoritesSonglist1);
			startMediaPlayer();
			SongData song = currentList.getSong(currentList.CurrentSong);
			mSyncProxy.show(song.getName(), song.getArtist(), null, null, null,
					null,
					(currentList.CurrentSong + 1) + "/" + currentList.size(),
					null, mCommonSoftbutton, null, null, correlationID++);
			mSyncProxy.addSubMenu(1111, getStringValue(R.string.playlists),
					correlationID++);

			Image image = new Image();
			image.setImageType(ImageType.STATIC);
			image.setValue("0x11");

			MenuParams popular_menupara = new MenuParams();
			popular_menupara.setMenuName(getStringValue(R.string.mostpopular));
			popular_menupara.setParentID(1111);
			popular_menupara.setPosition(0);

			AddCommand popular_command = new AddCommand();
			popular_command.setMenuParams(popular_menupara);
			popular_command.setCmdID(1011);
			popular_command
					.setVrCommands(new Vector<String>(
							Arrays.asList(new String[] { getStringValue(R.string.mostpopular) })));
			popular_command.setCmdIcon(image);
			popular_command.setCorrelationID(correlationID++);
			mSyncProxy.sendRPCRequest(popular_command);

			MenuParams favorite_menupara = new MenuParams();
			favorite_menupara.setMenuName(getStringValue(R.string.favorites));
			favorite_menupara.setParentID(1111);
			favorite_menupara.setPosition(0);

			AddCommand favorite_command = new AddCommand();
			favorite_command.setMenuParams(favorite_menupara);
			favorite_command.setCmdID(1012);
			favorite_command
					.setVrCommands(new Vector<String>(
							Arrays.asList(new String[] { getStringValue(R.string.favorites) })));
			favorite_command.setCmdIcon(image);
			favorite_command.setCorrelationID(correlationID++);
			mSyncProxy.sendRPCRequest(favorite_command);

			MenuParams local_menupara = new MenuParams();
			local_menupara.setMenuName(getStringValue(R.string.local));
			local_menupara.setParentID(1111);
			local_menupara.setPosition(0);

			AddCommand local_command = new AddCommand();
			local_command.setMenuParams(local_menupara);
			local_command.setCmdID(1014);
			local_command.setVrCommands(new Vector<String>(Arrays
					.asList(new String[] { getStringValue(R.string.local) })));
			local_command.setCmdIcon(image);
			local_command.setCorrelationID(correlationID++);
			mSyncProxy.sendRPCRequest(local_command);

			// mSyncProxy
			// .addCommand(
			// 1011,
			// getStringValue(R.string.mostpopular),
			// 0,
			// new Vector<String>(
			// Arrays.asList(new String[] { getStringValue(R.string.mostpopular)
			// })),
			// "0x11", ImageType.STATIC, correlationID++);
			// mSyncProxy
			// .addCommand(
			// 1012,
			// getStringValue(R.string.favorites),
			// 0,
			// new Vector<String>(
			// Arrays.asList(new String[] { getStringValue(R.string.favorites)
			// })),
			// "0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							1013,
							null,/* getStringValue(R.string.playlists), */
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.playlists) })),
							"0x11", ImageType.STATIC, correlationID++);
			// mSyncProxy
			// .addCommand(
			// 1014,
			// getStringValue(R.string.local),
			// 0,
			// new Vector<String>(
			// Arrays.asList(new String[] { getStringValue(R.string.local) })),
			// "0x11", ImageType.STATIC, correlationID++);

			mSyncProxy
					.addCommand(
							1015,
							getStringValue(R.string.randomon),
							1,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.randomon) })),
							"0x11", ImageType.STATIC, correlationID++);

			mSyncProxy.createInteractionChoiceSet(mQQMusicChoiceSet, 1041,
					correlationID++);
			subscribeMusicButton();
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getStringValue(int id) {
		return getResources().getString(id);
	}

	public void buildInitChoiceMode() {
		choicemode = new Vector<Choice>();
		Choice qqmusic = new Choice();
		qqmusic.setChoiceID(1201);
		qqmusic.setMenuName("QQ Music");
		qqmusic.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { "QQ??³ä??" })));
		choicemode.add(qqmusic);

		Choice trackmusic = new Choice();
		trackmusic.setChoiceID(1202);
		trackmusic.setMenuName(getStringValue(R.string.songs));
		trackmusic.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { getStringValue(R.string.songs) })));
		choicemode.add(trackmusic);

		Choice listmusic = new Choice();
		listmusic.setChoiceID(1203);
		listmusic.setMenuName(getStringValue(R.string.playlists));
		listmusic.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { getStringValue(R.string.playlists) })));
		choicemode.add(listmusic);

	}

	public void sendInitPerformInteraction() {
		buildInitChoiceMode();
		try {
			mSyncProxy.subscribeButton(ButtonName.PRESET_0, correlationID++);
			mSyncProxy.createInteractionChoiceSet(choicemode, 1211,
					correlationID++);
			performInteraction(1211, getStringValue(R.string.selectmodes),
					getStringValue(R.string.selectmodes),
					InteractionMode.MANUAL_ONLY);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void buildTrackSoftButton() {
		mCommonSoftbutton = new Vector<SoftButton>();
		mCommonSoftbutton.add(playbutton);

		favoritebutton = new SoftButton();
		favoritebutton.setType(SoftButtonType.SBT_TEXT);
		favoritebutton.setIsHighlighted(false);
		favoritebutton.setSoftButtonID(1313);
		favoritebutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		favoritebutton.setText(getStringValue(R.string.save));
		mCommonSoftbutton.add(favoritebutton);

		unfavoritebutton = new SoftButton();
		unfavoritebutton.setType(SoftButtonType.SBT_TEXT);
		unfavoritebutton.setIsHighlighted(true);
		unfavoritebutton.setSoftButtonID(1316);
		unfavoritebutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		unfavoritebutton.setText(getStringValue(R.string.save));
		listbutton = new SoftButton();
		listbutton.setSoftButtonID(1024);
		listbutton.setText(getStringValue(R.string.playlists));
		listbutton.setType(SoftButtonType.SBT_TEXT);
		listbutton.setIsHighlighted(false);
		listbutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		mCommonSoftbutton.add(listbutton);

		sharebutton = new SoftButton();
		sharebutton.setType(SoftButtonType.SBT_TEXT);
		sharebutton.setIsHighlighted(false);
		sharebutton.setSoftButtonID(1314);
		sharebutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		sharebutton.setText(getStringValue(R.string.share));
		// mCommonSoftbutton.add(sharebutton);

		infobutton = new SoftButton();
		infobutton.setType(SoftButtonType.SBT_TEXT);
		infobutton.setIsHighlighted(false);
		infobutton.setSoftButtonID(1315);
		infobutton.setSystemAction(SystemAction.DEFAULT_ACTION);
		infobutton.setText(getStringValue(R.string.info));
		// mCommonSoftbutton.add(infobutton);
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
		buildQQMusicSoftButton();
		buildQQMusicChoiceSet();
		buildTrackSoftButton();
		buildSongList();
		try {
			currentList = favoritesSonglist1;
			MusicPlayerService.setPlayList(favoritesSonglist1);
			startMediaPlayer();
			SongData song = currentList.getSong(currentList.CurrentSong);
			mSyncProxy.show(song.getName(), song.getArtist(), null, null, null,
					null,
					(currentList.CurrentSong + 1) + "/" + currentList.size(),
					null, mCommonSoftbutton, null, null, correlationID++);

			mSyncProxy.subscribeButton(ButtonName.OK, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_7, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_8, correlationID++);

			mSyncProxy
					.addCommand(
							1011,
							getStringValue(R.string.mostpopular),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.mostpopular) })),
							"0x11", ImageType.STATIC, correlationID++);
			// AddCommand addcommand = new AddCommand();
			// addcommand.setCmdIcon(cmdIcon)
			mSyncProxy
					.addCommand(
							1012,
							getStringValue(R.string.favorites),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.favorites) })),
							"0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							1013,
							getStringValue(R.string.playlists),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.playlists) })),
							"0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							1014,
							getStringValue(R.string.local),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.local) })),
							null, null, correlationID++);

			mSyncProxy.createInteractionChoiceSet(mQQMusicChoiceSet, 1041,
					correlationID++);
			mSyncProxy
					.addCommand(
							1017,
							getStringValue(R.string.addfavorite),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.addfavorite) })),
							"0x11", ImageType.STATIC, correlationID++);
			mSyncProxy
					.addCommand(
							1018,
							getStringValue(R.string.songinfo),
							0,
							new Vector<String>(
									Arrays.asList(new String[] { getStringValue(R.string.songinfo) })),
							"0x11", ImageType.STATIC, correlationID++);
			subscribeMusicButton();
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendTrackListData() {
		try {
			mSyncProxy.subscribeButton(ButtonName.PRESET_7, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_8, correlationID++);
			mSyncProxy.subscribeButton(ButtonName.PRESET_9, correlationID++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendQQMusicData();
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

	public static FordService getInstance() {
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
		Log.d(TAG, "onCreate");
		instance = this;
		Intent intent = new Intent();
		intent.setClass(this, MusicPlayerService.class);
		startService(intent);
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(MusicPlayerService.ACTION_CONTENT_PLAYING);
		registerReceiver(mBR, intentfilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flag, int startId) {
		Log.d(TAG, "onStartCommand");
		String configpath = intent.getStringExtra("config_path");
		// mActivity = CaseIteratorActivity.getInstance();

		startProxy();
		return 0;
	}

	public static String _FUNC_() {
		StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
		return traceElement.getMethodName() + "\n";
	}

	public void startProxy() {
		try {
			Log.d(TAG, "onStartCommand to connect with SYNC using SyncProxyALM");
			Language language = null;
			Locale locale = Locale.getDefault();
			String lang = locale.getDisplayLanguage();

			Log.d("Kyle", "language is " + lang);
			if (lang.contains("ä¸????")) {
				language = Language.ZH_CN;
			} else {
				language = Language.EN_US;
			}
			mSyncProxy = new SyncProxyALM(this,
					getStringValue(R.string.musicappdemo), true, language,
					language, "1234566799081");

		} catch (SyncException e) {
			// TODO Auto-generated catch block
			Log.d("Kyle", e.getMessage());
			if (mSyncProxy == null)
				stopSelf();
			e.printStackTrace();
		}
	}

	public void onDestroy() {
		instance = null;
		try {
			if (mSyncProxy != null)
				mSyncProxy.dispose();
			mSyncProxy = null;
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "dispose mSyncProxy failed");
			e.printStackTrace();
		}
		unregisterReceiver(mBR);
		super.onDestroy();
	}

	public void subscribleButton(ButtonName button) {
		try {
			mSyncProxy.subscribeButton(button, correlationID++);
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		// TODO Auto-generated method stub
		// mActivity.addLogToPanel(this._FUNC_());
		hmilevel = notification.getHmiLevel();
		AudioStreamingState state = notification.getAudioStreamingState();
		switch (hmilevel) {
		case HMI_BACKGROUND:
			Log.d("Kyle", "HMI_BACKGOUND");
			break;
		case HMI_FULL:
			Log.d("Kyle", "HMI_FULL");
			showLockscreen();
			if (notification.getFirstRun()) {
				// sendQQMusicData();
				sendInitPerformInteraction();
			} else {
				if (!isPaused) {
					startMediaPlayer();
				}
			}
			break;
		case HMI_NONE:
			removeLockscreen();
			stopMediaPlayer();
			Log.d("Kyle", "HMI_NONE");
		case HMI_LIMITED:
			Log.d("Kyle", "HMI_LIMITED");
		default:
			break;
		}
		switch (state) {
		case AUDIBLE:
			break;
		case NOT_AUDIBLE:
			break;
		case ATTENUATED:
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
	public void onProxyClosed(String info, Exception e) {
		// TODO Auto-generated method stub
		Log.d("Kyle", "onProxyClosed");
		if (mSyncProxy != null) {
			try {
				mSyncProxy.resetProxy();
				Intent intent = new Intent();
				intent.setClass(this, MusicPlayerService.class);
				stopService(intent);
			} catch (SyncException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		removeLockscreen();
		stopSelf();
	}

	public static String FormatStackTrace(Throwable throwable) {
		if (throwable == null)
			return "";
		String rtn = throwable.getStackTrace().toString();
		try {
			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			throwable.printStackTrace(printWriter);
			printWriter.flush();
			writer.flush();
			rtn = writer.toString();
			printWriter.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception ex) {
		}
		return rtn + "\n";
	}

	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		// TODO Auto-generated method stub
		Log.d("Kyle", "Add command done for " + response.getCorrelationID()
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
		Log.e("Kyle", "CreateChoiceSet response: " + response.getSuccess()
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
		// ms.onVoiceCommand(notification, correlationID++);
		int id = notification.getCmdID();
		TriggerSource ts = notification.getTriggerSource();
		switch (id) {
		case 1011:
			currentList = randomsongs;
			MusicPlayerService.setPlayList(currentList);
			startMediaPlayer();
			break;
		case 1012:
			// if(ts.equals(TriggerSource.TS_MENU)){
			// pump(getStringValue(R.string.alreadychoose),
			// getStringValue(R.string.favorites));
			// } else {
			// voicePump(getStringValue(R.string.alreadychoose),
			// getStringValue(R.string.favorites));
			// }
			currentList = favoritesSonglist1;
			MusicPlayerService.setPlayList(currentList);
			startMediaPlayer();
			if (mHighlightedSonglistButton != null)
				mHighlightedSonglistButton.setIsHighlighted(false);
			try {
				mSyncProxy.show(null, null, null, null, null, null, null, null,
						mCommonSoftbutton, null, null, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 1013:
			performInteraction(1041, getStringValue(R.string.selectplaylist),
					getStringValue(R.string.selectplaylist),
					InteractionMode.BOTH);

			break;
		case 1014:
			// if (SCENE_MODE == 3 && INNER_MODE == 2) {
			// pump(getStringValue(R.string.alreadychoose),
			// getStringValue(R.string.local));
			// }
			if (localsongs.size() > 1) {
				currentList = localsongs;
				MusicPlayerService.setPlayList(currentList);
				startMediaPlayer();
				if (mHighlightedSonglistButton != null)
					mHighlightedSonglistButton.setIsHighlighted(false);
				mHighlightedSonglistButton = localbutton;
				localbutton.setIsHighlighted(true);
				try {
					mSyncProxy.show(null, null, null, null, null, null, null,
							null, mCommonSoftbutton, null, null,
							correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				voicePump(getStringValue(R.string.nolocalmusic), null);
			}
			break;
		case 1015:
			currentList.setRandom(true);
			isRandom = true;
			if (ts.equals(TriggerSource.TS_MENU)) {
				pump(getStringValue(R.string.randomon), null);
			} else {
				voicePump(getStringValue(R.string.randomon), null);
			}
			try {
				mSyncProxy.deleteCommand(1015, correlationID++);
				mSyncProxy
						.addCommand(
								1016,
								getStringValue(R.string.randomoff),
								1,
								new Vector<String>(
										Arrays.asList(new String[] { getStringValue(R.string.randomoff) })),
								"0x11", ImageType.STATIC, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 1016:
			currentList.setRandom(false);
			isRandom = false;
			if (ts.equals(TriggerSource.TS_MENU)) {
				pump(getStringValue(R.string.randomoff), null);
			} else {
				voicePump(getStringValue(R.string.randomoff), null);
			}
			try {
				mSyncProxy.deleteCommand(1016, correlationID++);
				mSyncProxy
						.addCommand(
								1015,
								getStringValue(R.string.randomon),
								1,
								new Vector<String>(
										Arrays.asList(new String[] { getStringValue(R.string.randomon) })),
								"0x11", ImageType.STATIC, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case 1017:
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
		case 1018:
			SongData song = currentList.getSong(currentList.CurrentSong);
			if (ts.equals(TriggerSource.TS_MENU)) {
				pump(song.getArtist(), song.getName());
			} else {
				voicePump(song.getArtist(), song.getName());
			}
			break;
		default:
			break;
		}
		if (currentList != null)
			currentList.setRandom(isRandom);
	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		// TODO Auto-generated method stub

		Log.d("Kyle", "response info is " + response.getInfo() + " code is "
				+ response.getResultCode());

		if (response.getSuccess()) {
			int choiceid = response.getChoiceID();
			TriggerSource ts = response.getTriggerSource();
			switch (choiceid) {
			case 1031:
				if (currentList.equals(favoritesSonglist1)) {
					voicePump(getStringValue(R.string.nowplayinglist),
							favoritesSonglist1.ListName);
					return;
				}
				currentList = favoritesSonglist1;
				MusicPlayerService.setPlayList(currentList);
				// if (SCENE_MODE == 3 && INNER_MODE == 2) {
				// pump(getStringValue(R.string.alreadychoose),
				// currentList.ListName);
				// } else if (SCENE_MODE == 3 && INNER_MODE == 3) {
				// voicePump(getStringValue(R.string.alreadychoose),
				// currentList.ListName);
				// }
				startMediaPlayer();
				if (mHighlightedSonglistButton != null)
					mHighlightedSonglistButton.setIsHighlighted(false);
				try {
					mSyncProxy.show(null, null, null, null, null, null, null,
							null, mCommonSoftbutton, null, null,
							correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SongData song = currentList.getSong(currentList.CurrentSong);
				try {
					mSyncProxy.show(song.getName(), song.getArtist(), null,
							null, null, null, (currentList.CurrentSong + 1)
									+ "/" + currentList.size(), null, null,
							null, null, correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 1032:
				if (currentList.equals(newAgeSonglist2)) {
					voicePump(getStringValue(R.string.nowplayinglist),
							newAgeSonglist2.ListName);
					return;
				}
				currentList = newAgeSonglist2;
				MusicPlayerService.setPlayList(currentList);
				// if (SCENE_MODE == 3 && INNER_MODE == 2) {
				// pump(getStringValue(R.string.alreadychoose),
				// currentList.ListName);
				// } else if (SCENE_MODE == 3 && INNER_MODE == 3) {
				// voicePump(getStringValue(R.string.alreadychoose),
				// currentList.ListName);
				// }
				startMediaPlayer();
				if (mHighlightedSonglistButton != null)
					mHighlightedSonglistButton.setIsHighlighted(false);
				try {
					mSyncProxy.show(null, null, null, null, null, null, null,
							null, mCommonSoftbutton, null, null,
							correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SongData song1 = currentList.getSong(currentList.CurrentSong);
				try {
					mSyncProxy.show(song1.getName(), song1.getArtist(), null,
							null, null, null, (currentList.CurrentSong + 1)
									+ "/" + currentList.size(), null, null,
							null, null, correlationID++);
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 1201:
				SCENE_MODE = 1;
				sendQQMusicData();
				break;
			case 1202:
				SCENE_MODE = 2;
				sendTrackData();
				break;
			case 1203:
				SCENE_MODE = 3;
				sendTrackListData();
				break;
			}
		}
		if (currentList != null)
			currentList.setRandom(isRandom);
		// ms.onPerformInteractionResponse(response);
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
		Log.e("Kyle", "MediaTimerClock response: " + response.getSuccess()
				+ " code is " + response.getResultCode() + " info is:"
				+ response.getInfo());
	}

	@Override
	public void onShowResponse(ShowResponse response) {
		// TODO Auto-generated method stub
		Log.e("Kyle", "Show response: " + response.getSuccess() + " code is "
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
		Log.d("Kyle", "onButtonPress " + name);
		if (name.equals(ButtonName.CUSTOM_BUTTON)) {
			int id = notification.getCustomButtonName();

			switch (id) {
			case 1022:
				if (INNER_MODE == 2) {
					pump(getStringValue(R.string.play), null);
				}
				startMediaPlayer();
				break;

			case 1025:
				if (INNER_MODE == 2) {
					pump(getStringValue(R.string.paused), null);
				}
				pauseMediaPlayer();
				break;

			case 1023:
				voicePump(getStringValue(R.string.alreadychoose),
						getStringValue(R.string.local));
				if (localsongs.size() > 1) {
					currentList = localsongs;
					MusicPlayerService.setPlayList(currentList);
					startMediaPlayer();
					if (mHighlightedSonglistButton != null)
						mHighlightedSonglistButton.setIsHighlighted(false);
					localbutton.setIsHighlighted(true);
					mHighlightedSonglistButton = localbutton;
					try {
						mSyncProxy.show(null, null, null, null, null, null,
								null, null, mCommonSoftbutton, null, null,
								correlationID++);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					voicePump(getStringValue(R.string.nolocalmusic), null);
				}
				break;
			case 1024:
				performInteraction(1041,
						getStringValue(R.string.selectplaylistmanually),
						getStringValue(R.string.selectplaylist),
						InteractionMode.MANUAL_ONLY);

				break;
			case 1313:
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
			case 1314:
				pump(getStringValue(R.string.sharesuccess), null);
				break;
			case 1315:
				SongData song = currentList.getSong(currentList.CurrentSong);
				pump(song.getArtist(), song.getName());
				break;
			case 1316:
				pump(getStringValue(R.string.cancelsave), null);
				mCommonSoftbutton.remove(1);
				mCommonSoftbutton.add(1, favoritebutton);
				favoritesSonglist1.removeSong(favoritesSonglist1
						.findSong(currentList.getCurrSong().getName()));
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
			sendCommand(MusicPlayerService.CMD_PREV);
			try {
				mSyncProxy.setMediaClockTimer(null, null, null,
						UpdateMode.CLEAR, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (name.equals(ButtonName.SEEKRIGHT)) {
			sendCommand(MusicPlayerService.CMD_NEXT);
			try {
				mSyncProxy.setMediaClockTimer(null, null, null,
						UpdateMode.CLEAR, correlationID++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// }
		} else if (name.equals(ButtonName.PRESET_0)) {
			sendInitPerformInteraction();
		} else if (name.equals(ButtonName.PRESET_7)) {
			if (SCENE_MODE == 3 || SCENE_MODE == 2) {
				INNER_MODE = 1;
			}
		} else if (name.equals(ButtonName.PRESET_8)) {
			if (SCENE_MODE == 3 || SCENE_MODE == 2) {
				INNER_MODE = 2;
			}
		} else if (name.equals(ButtonName.PRESET_9)) {
			if (SCENE_MODE == 3) {
				INNER_MODE = 3;
			}
		} else if (name.equals(ButtonName.OK)) {
			if (isPaused) {
				startMediaPlayer();
				if (INNER_MODE == 2) {
					pump(getStringValue(R.string.play), null);
				}
			} else {
				pauseMediaPlayer();

				if (INNER_MODE == 2) {
					pump(getStringValue(R.string.pause), null);
				}
			}
		}
		if (currentList != null)
			currentList.setRandom(isRandom);

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
		Log.d(TAG, "onPerformAudioPassThru response: " + response.getInfo());

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
	public void onDialNumberResponse(DialNumberResponse arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onError(String arg0, Exception arg1) {
		// TODO Auto-generated method stub

	}

}