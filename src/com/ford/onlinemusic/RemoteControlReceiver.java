package com.ford.onlinemusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by ford-pro2 on 16/4/16.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    private String TAG = "RemoteControlReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "get key code event: ");
//        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.i(TAG, "get key code event: " + event.getKeyCode());
            if(event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        start(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        pause(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        playNext(context);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        playPrev(context);
                        break;

                }
            }
//        }
    }
    private void start(Context context){
        sendCommand(context,MusicPlayerService.CMD_START);
    }
    private void pause(Context context){
        sendCommand(context,MusicPlayerService.CMD_PAUSE);
    }
    private void playNext(Context context){
        sendCommand(context,MusicPlayerService.CMD_NEXT);
    }
    private void playPrev(Context context){
        sendCommand(context,MusicPlayerService.CMD_PREV);
    }
    public void sendCommand(Context context, int command) {
        Intent intent = new Intent(MusicPlayerService.ACTION_COMMAND);
        intent.putExtra(MusicPlayerService.COMMAND, command);
        context.sendBroadcast(intent);
    }
}

