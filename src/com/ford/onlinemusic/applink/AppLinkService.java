package com.ford.onlinemusic.applink;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.ford.onlinemusic.MainActivity;
import com.ford.onlinemusic.MusicPlayerService;
import com.ford.onlinemusic.R;
import com.ford.onlinemusic.SongData;
import com.ford.onlinemusic.SongList;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.AlertManeuverResponse;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleData;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendLocation;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimer;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SoftButton;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StartTime;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.UpdateTurnListResponse;
import com.smartdevicelink.proxy.rpc.VrHelpItem;
import com.smartdevicelink.proxy.rpc.enums.AudioStreamingState;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.LayoutMode;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.SoftButtonType;
import com.smartdevicelink.proxy.rpc.enums.SystemAction;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.proxy.rpc.enums.UpdateMode;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;

//import android.util.Log;
/*
 * Main implementation of AppLink, responsible for:
 * 1. create/dispose SdlProxyALM, handle connection with Sdl
 * 2. building UI on Sdl, when get HMI_FULL first time, send show, addcommand, createchoiceset to Sdl.
 * 3. handling user action with Sdl and notification from Sdl.
 * 4. send lockscreen broadcast to MainActivity.
 * 5. update Music Player Service status on Sdl
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
    private static SdlProxyALM mSdlProxy = null;
    private final String TAG = "AppLinkService";
    private int correlationID = 100;
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
    private boolean firstHMINone = true;

    private int putFileIconID = 22;
    private String iconFileName = "icon.png";
    private String mPlayerStatus = null;
    private String mCoverName = null;
    private boolean mPicToShow = false;
    private int putCoverID = 23;

    private boolean isGen3 = true;
    private SdlList albumelist;
    private SdlList novellist;
    private int ALBUM_CHOICE_ID = 1700;
    private int ALBUM_CHOICESET_ID = 1600;

    private int NOVEL_CHOICE_ID = 2000;
    private int NOVEL_CHOICESET_ID = 2100;

    public int[] COMMANDS = new int[]{R.string.mostpopular,
            R.string.favorites, R.string.newage, R.string.local,
            R.string.addfavorite, R.string.songinfo};

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.i("Kyle", "lenght is " + msg.arg1);
            handlePlayerStatusChange(mPlayerStatus, msg.arg1, msg.arg2);
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
                int position = intent.getIntExtra(
                        MusicPlayerService.DATA_POSITION, 0);
                int length = intent.getIntExtra(MusicPlayerService.DATA_LENGTH,
                        0);
                Log.i(TAG, "get song info position: " + position + "  length: "
                        + length);
                mPlayerStatus = status;
                Message msg = new Message();
                msg.what = 1;
                msg.arg1 = position;
                msg.arg2 = length;
                mHandler.removeMessages(1);
                mHandler.sendMessageDelayed(msg, 200);

            }
        }
    };

    public void handlePlayerStatusChange(String status, int position, int length) {
        Log.i("Kyle", "handle player status change: " + status);
        SongData song = currentList.getCurrSong();
        String name = song.getName();
        String artist = song.getArtist();
        try {
            if (status.equalsIgnoreCase("Buffering")) {
                mSdlProxy.setMediaClockTimer(null, null, null,
                        UpdateMode.PAUSE, correlationID++);
                mSdlProxy.show(name, artist, null, null, null, null,
                        getStringValue(R.string.bufferring), null, null, null,
                        TextAlignment.CENTERED, correlationID++);
                // updateScreenSongInfo(song);
            } else if (status.equalsIgnoreCase("paused")) {
                if (!isGen3) {
                    mCommonSoftbutton.remove(0);
                    mCommonSoftbutton.add(0, pausebutton1);
                }
                SetMediaClockTimer mTimer = new SetMediaClockTimer();
                mTimer.setCorrelationID(correlationID++);
                mTimer.setUpdateMode(UpdateMode.PAUSE);
                mSdlProxy.sendRPCRequest(mTimer);
                mSdlProxy.show(null, null, null, null, null, null,
                        getStringValue(R.string.paused), null,
                        mCommonSoftbutton, null, null, correlationID++);

            } else if (status.equalsIgnoreCase("network_error")) {
                mSdlProxy.show(null, null, null, null, null, null,
                        getStringValue(R.string.networkerror), null, null,
                        null, null, correlationID++);

            } else if (status.equalsIgnoreCase("playing")) {
                if (!isGen3) {
                    mCommonSoftbutton.remove(0);
                    mCommonSoftbutton.add(0, playbutton);
                }
                int minutes = position / 60;
                int seconds = position % 60;
                Log.i(TAG, "isPaused: " + isPaused + " duration: " + length);
                SetMediaClockTimer mTimer = new SetMediaClockTimer();
                StartTime starttime = new StartTime();
                starttime.setMinutes(minutes);
                starttime.setSeconds(seconds);
                starttime.setHours(0);
                StartTime endtime = new StartTime();
                endtime.setMinutes(length / 60);
                endtime.setSeconds(length % 60);
                endtime.setHours(0);
                mTimer.setStartTime(starttime);
                mTimer.setEndTime(endtime);
                mTimer.setCorrelationID(correlationID++);
                mTimer.setUpdateMode(UpdateMode.COUNTUP);
                mSdlProxy.sendRPCRequest(mTimer);
                mSdlProxy.show(name, artist, null, null, null, null,
                        currentList.ListName, null, mCommonSoftbutton, null,
                        TextAlignment.CENTERED, correlationID++);

                isPaused = false;
            }
        } catch (SdlException e) {
            e.printStackTrace();
        }
    }

    public void buildLocalSongList() {
        localsongs = new SongList(getStringValue(R.string.local));
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(
                uri,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DATA}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                localsongs.addSong(new SongData(cursor.getString(1), cursor
                        .getString(2), cursor.getString(3), null, cursor
                        .getString(4)));
            } while (cursor.moveToNext());
        }
    }

    public void buildSongList() {
        buildLocalSongList();
        mostpopularsongs = new SongList(getStringValue(R.string.mostpopular));
        mostpopularsongs
                .addSong(new SongData("小苹果", "筷子兄弟", "", "cover_11.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C1000035GveV3i9dBM.m4a?fromtag=52"));
        mostpopularsongs
                .addSong(new SongData("金玉良缘", "李琦", "", "cover_22.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C100001iEkMd4CUugY.m4a?fromtag=52"));
        mostpopularsongs
                .addSong(new SongData("同桌的你", "胡夏", "", "cover_33.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C1000031zaiZ2ZmBYj.m4a?fromtag=52"));
        favoritesSonglist1 = new SongList(getStringValue(R.string.favorites));
        newAgeSonglist2 = new SongList(getStringValue(R.string.newage));
        favoritesSonglist1
                .addSong(new SongData("倩女幽魂", "张国荣", "", "cover_4412365.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C100001hZjYW0nOsTa.m4a?fromtag=52"));
        favoritesSonglist1
                .addSong(new SongData("当爱已成往事", "张国荣", "", "cover_55.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C100001UK2LJ0KU9ay.m4a?fromtag=52"));
        favoritesSonglist1
                .addSong(new SongData("风继续吹", "张国荣", "", "cover_66.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C100002TvOb41nQrdx.m4a?fromtag=52"));
        newAgeSonglist2
                .addSong(new SongData("故乡的原风景", "宗次郎", "轻音乐", "cover_77.jpeg",
                        "http://cc.stream.qqmusic.qq.com/C100003d4aYZ385awT.m4a?fromtag=52"));
        newAgeSonglist2
                .addSong(new SongData("天空之城", "久石让",
                        getStringValue(R.string.playlists), "cover_88.jpeg",
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
                .asList(new String[]{favoritesSonglist1.ListName})));
        Image image = new Image();
        image.setImageType(ImageType.STATIC);
        image.setValue("0x11");
        choice1.setImage(image);

        mQQMusicChoiceSet.add(choice1);

        Choice choice2 = new Choice();
        choice2.setChoiceID(1032);
        choice2.setMenuName(newAgeSonglist2.ListName);
        choice2.setVrCommands(new Vector<String>(Arrays
                .asList(new String[]{newAgeSonglist2.ListName})));
        mQQMusicChoiceSet.add(choice2);

    }

    public void subscribeMusicButton() {
        try {
            mSdlProxy.subscribeButton(ButtonName.SEEKLEFT, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.SEEKRIGHT, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.OK, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_1, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_2, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_3, correlationID++);

        } catch (SdlException e) {
            // TODO Auto-generated catch onblock
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
        if (!isGen3) {
            mCommonSoftbutton.add(pausebutton1);
        }
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
            mSdlProxy.alert(content, text2, false, 3000, correlationID++);
        } catch (SdlException e) {
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
            mSdlProxy.alert(TTSChunkFactory.createSimpleTTSChunks(voicestring),
                    content, text2, false, 3000, correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void scrollableMessage(String message, Vector<SoftButton> softButtons) {
        try {
            mSdlProxy.scrollablemessage(message, 10000, softButtons,
                    correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void performInteraction(int choicesetid, String initprompt,
                                   String displaytext, InteractionMode mode) {
        try {
            mSdlProxy.performInteraction(initprompt, displaytext, choicesetid,
                    null, null, mode, 10000, correlationID++);
        } catch (SdlException e) {
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

            mSdlProxy.subscribeButton(ButtonName.OK, correlationID++);
//            mSdlProxy.subscribeButton(ButtonName.PRESET_7, correlationID++);
//            mSdlProxy.subscribeButton(ButtonName.PRESET_8, correlationID++);

            mSdlProxy.addCommand(
                    CMD_ID_MOSTPOPULAR,
                    getStringValue(R.string.mostpopular),
                    0,
                    new Vector<String>(Arrays.asList(new String[]{
                            getStringValue(R.string.mostpopular), "最流行的"})),
                    "0x11", ImageType.STATIC, correlationID++);
            // AddCommand addcommand = new AddCommand();
            // addcommand.setCmdIcon(cmdIcon)
            mSdlProxy.addCommand(
                    CMD_ID_FAVORITES,
                    getStringValue(R.string.favorites),
                    1,
                    new Vector<String>(Arrays.asList(new String[]{
                            getStringValue(R.string.favorites), "我最喜欢的"})),
                    "0x11", ImageType.STATIC, correlationID++);
            mSdlProxy.addCommand(
                    CMD_ID_LOCAL,
                    getStringValue(R.string.local),
                    3,
                    new Vector<String>(Arrays.asList(new String[]{
                            getStringValue(R.string.local), "本地", "本地音乐"})),
                    null, null, correlationID++);

            mSdlProxy.createInteractionChoiceSet(mQQMusicChoiceSet,
                    CHS_ID_PLAYLISTS, correlationID++);

            albumelist.sendCreateInteractionChoiceSet(mSdlProxy, 13149);
            novellist.sendCreateInteractionChoiceSet(mSdlProxy,12345);
            mSdlProxy.addCommand(
                    CMD_ID_ADDFAVORITE,
                    getStringValue(R.string.addfavorite),
                    5,
                    new Vector<String>(Arrays.asList(new String[]{
                            getStringValue(R.string.addfavorite), "添加收藏"})),
                    "0x11", ImageType.STATIC, correlationID++);
            mSdlProxy.addCommand(
                    CMD_ID_SONGINFO,
                    getStringValue(R.string.songinfo),
                    4,
                    new Vector<String>(Arrays.asList(new String[]{
                            getStringValue(R.string.songinfo), "信息"})),
                    "0x11", ImageType.STATIC, correlationID++);
            // mSdlProxy
            // .addCommand(
            // CMD_ID_PLAYLISTS,
            // getStringValue(R.string.playlists),
            // 0,
            // new Vector<String>(
            // Arrays.asList(new String[] { getStringValue(R.string.playlists)
            // })),
            // "0x11", ImageType.STATIC, correlationID++);

            mSdlProxy
                    .addCommand(
                            CMD_ID_PLAYLISTS,
                            new Vector<String>(
                                    Arrays.asList(new String[]{getStringValue(R.string.playlists)})),
                            correlationID++);

            mSdlProxy
                    .addCommand(
                            CMD_ID_NEWAGE,
                            getStringValue(R.string.newage),
                            2,
                            new Vector<String>(
                                    Arrays.asList(new String[]{getStringValue(R.string.newage)})),
                            "0x11", ImageType.STATIC, correlationID++);

            subscribeMusicButton();
        } catch (SdlException e) {
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

    private void updateGraphicOnSdl(Image graphic) {
        try {
            mSdlProxy.show(null, null, null, null, null, null, null, graphic,
                    null, null, null, correlationID++);

        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void resetGraphiconSdl() {
        Image image = new Image();
        image.setValue("");
        image.setImageType(ImageType.DYNAMIC);
        updateGraphicOnSdl(image);
    }

    public void updateScreenSongInfo(SongData song) {
        // default set the song is not in the favorite list.
        Log.i("Kyle", "update Screen Song Info: " + song.getName() + "	"
                + song.getAlbumPath());
        if(!isGen3) {
            mCommonSoftbutton.remove(0);
            mCommonSoftbutton.add(0, pausebutton1);
        }
        if (favoritesSonglist1.findSong(song.getName()) != -1) {
            if(mCommonSoftbutton.contains(favoritebutton)) {
                mCommonSoftbutton.remove(favoritebutton);
            }
            if(!mCommonSoftbutton.contains(unfavoritebutton)) {
                mCommonSoftbutton.add(unfavoritebutton);
            }
        } else {
            if(mCommonSoftbutton.contains(unfavoritebutton)) {
                mCommonSoftbutton.remove(unfavoritebutton);
            }
            if(!mCommonSoftbutton.contains(favoritebutton)){
                mCommonSoftbutton.add(favoritebutton);
            }
        }
        if (isGen3) {
            resetGraphiconSdl();
        }
        try {
            mSdlProxy.setMediaClockTimer(null, null, null, UpdateMode.CLEAR,
                    correlationID++);
            mSdlProxy.show(song.getName(), song.getArtist(), null, null, null,
                    null, getStringValue(R.string.bufferring), null,
                    mCommonSoftbutton, null, null, correlationID++);
            if (song.getAlbumPath() != null && isGen3) {
                AssetManager am = getResources().getAssets();
                mPicToShow = true;
                mCoverName = song.getAlbumPath();
                Log.i(TAG, "send file to Sdl: path " + mCoverName);
                sendFileToSdl(am, mCoverName, putCoverID, mCoverName);
            }

        } catch (SdlException e) {
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

    public SdlProxyALM getProxy() {
        return mSdlProxy;
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

    public void resetProxy() {
        if (mSdlProxy == null) {
            startProxy();
        } else {
            try {
                mSdlProxy.resetProxy();
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     * called by onStartCommand() to create proxy, if proxy has been created
     * then do nothing, else create a new one.
     */
    public void startProxy() {

        // check whether current proxy is available, we should avoid to override
        // current proxy. because that will cause Sdl_UNAVAILABLE exception
        if (mSdlProxy != null && mSdlProxy.getIsConnected()) {
            return;
        } else if (mSdlProxy != null) {
            try {
                mSdlProxy.dispose();
            } catch (SdlException e) {

            }
        }
        try {
            Log.i(TAG, "onStartCommand to connect with Sdl using SdlProxyALM");

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

            mSdlProxy = new SdlProxyALM(this,
                    "SyncProxyTester", true, language,
                    language, "584421907");
//	      mSdlProxy = new SdlProxyALM()

        } catch (SdlException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, e.getMessage());

            if (mSdlProxy == null)
                stopSelf();
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        instance = null;

        // dispose proxy when service is destroyed.
        try {
            if (mSdlProxy != null)
                mSdlProxy.dispose();
            mSdlProxy = null;
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "dispose mSdlProxy failed");
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

    private void sendFileToSdl(int resID, int correalationID, String SdlFileName) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resID);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, os);
        sentFileToSdl(os.toByteArray(), correalationID, FileType.GRAPHIC_PNG,
                SdlFileName);
    }

    private void sendFileToSdl(AssetManager am, String path, int correlationID,
                               String SdlFileName) {
        try {
            Bitmap bm = BitmapFactory.decodeStream(am.open(path));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bm.compress(CompressFormat.JPEG, 100, os);
            Log.i(TAG, "sendFileToSdl AM: length is " + os.toByteArray().length);
            sentFileToSdl(os.toByteArray(), correlationID,
                    FileType.GRAPHIC_JPEG, SdlFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sentFileToSdl(byte[] data, int correlationID, FileType type,
                               String SdlFileName) {
        PutFile putfile = new PutFile();
        putfile.setCorrelationID(correlationID);
        putfile.setBulkData(data);
        putfile.setFileType(type);
        putfile.setSdlFileName(SdlFileName);
        try {
            mSdlProxy.sendRPCRequest(putfile);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendFileToSdl(String filepath, int correalationID,
                               String SdlFileName) {
        Bitmap bitmap = BitmapFactory.decodeFile(filepath);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 100, os);
        sentFileToSdl(os.toByteArray(), correalationID, FileType.GRAPHIC_JPEG,
                SdlFileName);
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        // TODO Auto-generated method stub

        hmilevel = notification.getHmiLevel();

        AudioStreamingState state = notification.getAudioStreamingState();
        Log.i(TAG, "get ononHmiStatus" + state);
        if (firstHMINone) {
            Log.i(TAG, "send icon to sdl");
            try {
                mSdlProxy.listfiles(correlationID++);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            sendFileToSdl(R.drawable.ic_launcher, putFileIconID, iconFileName);
            firstHMINone = false;
            try {
                isGen3 = mSdlProxy.getDisplayCapabilities().getGraphicSupported();
                Log.i(TAG,"isGen3 "+isGen3);
            } catch (SdlException e) {
                e.printStackTrace();
            }
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < 150; i++) {
                list.add("小说 " + i);
            }

            albumelist = new SdlList(list,ALBUM_CHOICE_ID,ALBUM_CHOICESET_ID);
            albumelist.setLayoutMode(LayoutMode.ICON_ONLY);
            albumelist.downloadAndSendImageToSync(mSdlProxy, getAssets(), 1122);

            albumelist.setOnItemSelectedListener(new SdlList.OnItemSelectedListener() {
                @Override
                public void onItemSelected(int choice_id) {
                    novellist.showList(mSdlProxy,"请选择章节","请选择章节",correlationID++);
                }
            });
            ArrayList<String> list1 = new ArrayList<String>();
            for (int i = 0; i < 100; i++) {
                list1.add("章节 " + i);
            }

            novellist = new SdlList(list1,NOVEL_CHOICE_ID,NOVEL_CHOICESET_ID);

        }
        switch (state) {
            case AUDIBLE:
                if (!isPaused)
                    startMediaPlayer();
                break;
            case NOT_AUDIBLE:
                try {
                    SetMediaClockTimer mTimer = new SetMediaClockTimer();
                    mTimer.setCorrelationID(correlationID++);
                    mTimer.setUpdateMode(UpdateMode.PAUSE);
                    mSdlProxy.sendRPCRequest(mTimer);
                } catch (SdlException exception) {

                }
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
                    Vector<VrHelpItem> items = new Vector<VrHelpItem>();
                    int count = COMMANDS.length;
                    for (int j = 0; j < count; j++) {
                        VrHelpItem item = new VrHelpItem();
                        item.setPosition(j);
                        item.setText(getString(COMMANDS[j]));
                        item.setImage(null);
                        Log.i(TAG, "add command help item:" + item.getText());
                        items.add(item);
                    }

                    try {
                        mSdlProxy
                                .setGlobalProperties(
                                        TTSChunkFactory
                                                .createSimpleTTSChunks("我来帮你"),
                                        TTSChunkFactory
                                                .createSimpleTTSChunks("没时间了, 有话快说"),
                                        "让哥来帮你", items, correlationID++);
                        SetGlobalProperties properties = new SetGlobalProperties();

                    } catch (SdlException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
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
	 * com.smartdevicelink.proxy.interfaces.IProxyListenerBase#onProxyClosed
	 * (java .lang.String, java.lang.Exception) Called when proxy detects that
	 * connection between Sdl and the Phone breaks
	 */
    public void onProxyClosed(String info, Exception e,
                              SdlDisconnectedReason arg2) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onProxyClosed:" + e.getLocalizedMessage());
        removeLockscreen();
        stopMusicService();
        firstHMINone = true;
//	  SdlExceptionCause cause = ((SdlException) e).getSdlExceptionCause();
//	  if (cause != SdlExceptionCause.SDL_PROXY_CYCLED
//		  && cause != SdlExceptionCause.BLUETOOTH_DISABLED) {
//	      if (mSdlProxy != null) {
//		  try {
//		      mSdlProxy.resetProxy();
//		  } catch (SdlException e1) {
//		      // TODO Auto-generated catch block
//		      e1.printStackTrace();
//		  }
//	      }
//	  } else {
//	      // stopSelf();
//	  }
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
                + response.getInfo() + " correlationID  " + response.getCorrelationID());
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
                mCommonSoftbutton.remove(favoritebutton);
                mCommonSoftbutton.add(unfavoritebutton);
                favoritesSonglist1.addSong(currentList.getCurrSong());
                try {
                    mSdlProxy.show(null, null, null, null, null, mCommonSoftbutton,
                            null, null, correlationID++);
                } catch (SdlException e) {
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
	 * @see com.smartdevicelink.proxy.interfaces.IProxyListenerBase#
	 * onPerformInteractionResponse
	 * (com.smartdevicelink.proxy.rpc.PerformInteractionResponse) Handle
	 * response of PerformInteraction
	 */
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        // TODO Auto-generated method stub

        Log.i(TAG, "response " + response.getFunctionName() + " info is " + response.getInfo() + " code is "
                + response.getResultCode() + " Text Entry: " + response.getManualTextEntry());
        // if success continue,else do nothing
        if (response.getSuccess()) {
            // get choice id user has selected. If choices never change, they
            // can be sent to Sdl using RPC CreateInteractionChoiceSet when get
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
                    if(albumelist.hasID(choiceid)){
                        albumelist.onItemSelected(choiceid);
                    }
                    if(novellist.hasID(choiceid)){
                        novellist.onItemSelected(choiceid);
                    }
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
        Log.e(TAG, "SetGlobalProperties response: " + response.getSuccess()
                + " code is " + response.getResultCode() + " info is:"
                + response.getInfo());
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
                    mCommonSoftbutton.remove(favoritebutton);
                    mCommonSoftbutton.add(unfavoritebutton);
                    favoritesSonglist1.addSong(currentList.getCurrSong());
                    pump(getStringValue(R.string.savesuccess), null);
                    try {
                        mSdlProxy.show(null, null, null, null, null,
                                mCommonSoftbutton, null, null, correlationID++);
                    } catch (SdlException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;

                case BTN_ID_UNFAVORITE:
                    pump(getStringValue(R.string.cancelsave), null);
                    mCommonSoftbutton.remove(unfavoritebutton);
                    mCommonSoftbutton.add(favoritebutton);
                    favoritesSonglist1.removeSong(favoritesSonglist1
                            .findSong(currentList.getCurrSong().getName()));
                    if (currentList == favoritesSonglist1) {
                        sendCommand(MusicPlayerService.CMD_NEXT);
                    }
                    try {
                        mSdlProxy.show(null, null, null, null, null,
                                mCommonSoftbutton, null, null, correlationID++);
                    } catch (SdlException e) {
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
        } else if (name.equals(ButtonName.PRESET_1)) {
//			getVehicleData();

            SendLocation sendlocation = new SendLocation();
            ArrayList<String> list = new ArrayList<String>();
            list.add("博霞路104号");
            list.add("上海市浦东新区");

//			list.add("上海市浦东新区");
            sendlocation.setLocationDescription("location");
            sendlocation.setLocationName("location name");
            sendlocation.setPhoneNumber("13918720164");
//			sendlocation.setAddressLines(list);
            sendlocation.setLongitudeDegrees(121.5625);
            sendlocation.setLatitudeDegrees(31.2080);
            sendlocation.setCorrelationID(correlationID++);
            try {
                mSdlProxy.sendRPCRequest(sendlocation);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (name.equals(ButtonName.PRESET_2)) {

            Log.i(TAG, "send pi to SYNC, correlationID : " + correlationID);

            albumelist.showList(mSdlProxy,"请选择专辑","请选择专辑",correlationID++);
        }
        if (currentList != null)
            currentList.setRandom(isRandom);
    }

    public void getVehicleData() {
        GetVehicleData vehicledata = new GetVehicleData();
        vehicledata.setOdometer(true);
        vehicledata.setSpeed(true);
        vehicledata.setGps(true);
//        vehicledata.getDeviceStatus()
        vehicledata.setCorrelationID(correlationID++);
        try {
            Log.i(TAG, "vehicledata is "
                    + vehicledata.serializeJSON().toString());
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            mSdlProxy.sendRPCRequest(vehicledata);
        } catch (SdlException e) {
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
        Log.i(TAG, "onOnPermisstionChange");
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
        } else if (response.getResultCode().name().equals("DISALLOWED")) {
            Log.i(TAG, "onGetVehicleDataReponse get DISABLLOWED");
            try {
                mSdlProxy
                        .alert("GetVehicleData is Disallowed, please go to Application Settings and request Update",
                                true, correlationID++);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
        Log.i(TAG, "onPerformAudioPassThru response: " + response.getInfo() + " correlationID: " + response.getCorrelationID());

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
        Log.i(TAG, "onPutFileResponse: " + response.getCorrelationID() + "  "
                + response.getSuccess() + " " + response.getInfo());
        int corID = response.getCorrelationID();
        if (corID == putFileIconID && response.getSuccess()) {
            try {
                mSdlProxy.setappicon("icon.png", correlationID++);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (corID == putCoverID && response.getSuccess() && mPicToShow) {
            Log.i(TAG, "onPutFile  putCoverID : " + mCoverName);
            Image image = new Image();
            image.setImageType(ImageType.DYNAMIC);
            image.setValue(mCoverName);
            updateGraphicOnSdl(image);
            mPicToShow = false;
        }
    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        // TODO Auto-generated method stub
        // for (String name : response.getFilenames()) {
        // Log.i(TAG, "listFile result: " + name);
        // }

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
    public void onOnTBTClientState(OnTBTClientState arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnHashChange(OnHashChange arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnSystemRequest(OnSystemRequest arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnTouchEvent(OnTouchEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDialNumberResponse(DialNumberResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnStreamRPC(OnStreamRPC arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendLocationResponse(SendLocationResponse arg0) {
        // TODO Auto-generated method stub
        Log.i(TAG, "send location response: info " + arg0.getInfo() + " resultCode: " + arg0.getResultCode());
        try {
            mSdlProxy.speak("please confirm", correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDataACK() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceEnded(OnServiceEnded arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceNACKed(OnServiceNACKed arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onShowConstantTbtResponse(ShowConstantTbtResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStreamRPCResponse(StreamRPCResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse arg0) {
        // TODO Auto-generated method stub

    }

}
