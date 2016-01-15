package com.ford.onlinemusic.applink;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.LayoutMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by ford-pro2 on 16/1/6.
 */

public class SdlList {

    public ArrayList<Album> mAlbumList;
    private int ID_BASE = 1310;
    private int LAST_ID = ID_BASE;
    private int CHOICESET_ID_BASE = 102;
    private int ID_CORRELATIONID = 0;
    private Vector<Integer> mChoiceSetIDSets;
    private String TAG = "SdlList";
    private LayoutMode mLayout = LayoutMode.LIST_ONLY;

    // for demo only
    private String ALBUM_NAME = "album_image_demo";

    private OnItemSelectedListener mListerner;

    public SdlList(ArrayList<String> alubmelist) {
        mAlbumList = new ArrayList<Album>();
        int i = 0;
        int length = alubmelist.size();
        for (; i < length; i++) {
            LAST_ID = ID_BASE + i;
            mAlbumList.add(new Album(alubmelist.get(i)));
        }
        mChoiceSetIDSets = new Vector<Integer>();
    }

    public SdlList(ArrayList<String> alubmelist, int choice_id_base, int choiceset_id_base) {
        ID_BASE = choice_id_base;
        CHOICESET_ID_BASE = choiceset_id_base;
        mAlbumList = new ArrayList<Album>();
        int i = 0;
        int length = alubmelist.size();
        for (; i < length; i++) {
            LAST_ID = ID_BASE + i;
            mAlbumList.add(new Album(alubmelist.get(i)));
        }
        mChoiceSetIDSets = new Vector<Integer>();

    }
    public LayoutMode getLayoutMode(){
        return mLayout;
    }
    public void setLayoutMode(LayoutMode mode){
        mLayout = mode;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener){
        mListerner = listener;
    }
    public void onItemSelected(int choice_id){
        if (mListerner != null){
            mListerner.onItemSelected(choice_id);
        }
    }

    public boolean hasID(int choice_id){
        return (choice_id - ID_BASE) < mAlbumList.size();
    }

    public void addAlbum(Album album) {
        mAlbumList.add(album);
    }

    public void updateAlbumToSync() {

    }

    public void downloadAndSendImageToSync(SdlProxyALM mProxy, AssetManager assetManager, int correlationID) {
        //just for demo
        sendFileToSdl(mProxy, assetManager, "cover.png", correlationID, ALBUM_NAME);
    }

    public void showList(SdlProxyALM mSdlProxy, String initialtext, String initial_prompt,  int correlationID){
        try {
            PerformInteraction pi = new PerformInteraction();
            pi.setInitialText(initialtext);
            pi.setInitialPrompt(TTSChunkFactory.createSimpleTTSChunks(initial_prompt));
            pi.setInteractionChoiceSetIDList(this.getmChoiceSetIDSets());
            pi.setInteractionMode(InteractionMode.MANUAL_ONLY);
            pi.setTimeout(30000);
            pi.setInteractionLayout(mLayout);
            pi.setCorrelationID(correlationID);
            mSdlProxy.sendRPCRequest(pi);

        } catch (SdlException e) {
            e.printStackTrace();
        }
    }
    public void sendCreateInteractionChoiceSet(SdlProxyALM mProxy,int correlationID) {

        CreateInteractionChoiceSet cics = new CreateInteractionChoiceSet();
        Vector<Choice> choices = new Vector<Choice>();

        int num = mAlbumList.size();
        int pack = num / 100;
        Album album;
        int count = 0;
        for ( int j = 0; j < pack ; j++) {
            count =  ((num-j*100) > 100 ) ? 100:(num - j*100);
            choices.clear();
            for (int i = 0; i < count; i++) {
                album = mAlbumList.get(i+j*100);
                Log.i(TAG, "create choice for album: " + album.toString() + " \n id is " + (ID_BASE + i));
                Choice choice = new Choice();
                choice.setChoiceID(ID_BASE + i+j*100);
                choice.setMenuName(album.mName);
                if( mLayout == LayoutMode.ICON_ONLY) {
                    Image image = new Image();
                    image.setImageType(ImageType.STATIC);
                    image.setValue(album.mAlbumName);
                    choice.setImage(image);
                }
                choice.setVrCommands(Arrays.asList(new String[]{album.mName}));
                choices.add(choice);
            }


            cics.setChoiceSet(choices);
            mChoiceSetIDSets.add(CHOICESET_ID_BASE+j);
            cics.setInteractionChoiceSetID(CHOICESET_ID_BASE+j);

            Log.i(TAG, "send albumlist createinteractionChoiceSet to SYNC: " + (CHOICESET_ID_BASE+j));
            cics.setCorrelationID(correlationID++);

            try {
                mProxy.sendRPCRequest(cics);
            } catch (SdlException e) {
                e.printStackTrace();
            }
        }
    }
    public Vector<Integer> getmChoiceSetIDSets(){
        return mChoiceSetIDSets;
    }
    private void sendFileToSdl(SdlProxyALM mProxy, AssetManager am, String path, int correlationID,
                               String SdlFileName) {
        try {
            Bitmap bm = BitmapFactory.decodeStream(am.open(path));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.i(TAG, "sendFileToSdl AM: length is " + os.toByteArray().length);
            sentFileToSdl(mProxy, os.toByteArray(), correlationID,
                    FileType.GRAPHIC_JPEG, SdlFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sentFileToSdl(SdlProxyALM mProxy, byte[] data, int correlationID, FileType type,
                               String SdlFileName) {
        PutFile putfile = new PutFile();
        putfile.setCorrelationID(correlationID);
        putfile.setBulkData(data);
        putfile.setFileType(type);
        putfile.setSdlFileName(SdlFileName);
        try {
            mProxy.sendRPCRequest(putfile);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public class Album {
        public String mName;
        public String mAlbumPath;
        public String mAlbumName;
        public FileType mType;
        public byte[] mAlbumContent;

        public Album(String name) {
            mName = name;
            mAlbumName = ALBUM_NAME;
            mAlbumPath = "cover.png";
        }

        public Album(String name, String path) {
            mName = name;
            mAlbumPath = path;
            mAlbumName = ALBUM_NAME;
        }
        public String toString(){
            return "mName: "+mName+"  mAlbumPath:"+mAlbumPath+" mAlbumName: "+mAlbumName+" mAlbumPath: "+mAlbumPath;
        }


    }
    public interface  OnItemSelectedListener{
        public void onItemSelected(int choice_id);
    }

}
