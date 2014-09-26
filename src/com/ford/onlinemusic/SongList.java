package com.ford.onlinemusic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.media.MediaPlayer;
import android.util.Log;

public class SongList {
	public String ListName = "";
	private List<SongData> SongList = null;
	public int CurrentSong = 0;

	private boolean isRandom = false;

	public SongList(String name) {
		ListName = name;
		SongList = new ArrayList<SongData>();
	}

	public int size() {
		return SongList.size();
	}

	public void addSong(SongData song) {
		SongList.add(song);
	}

	public void clearSong() {
		SongList.clear();
	}

	public SongData getSong(int index) {
		if (index < SongList.size()) {
			return SongList.get(index);
		}
		return null;
	}

	public void removeSong(int index) {
		if (index < SongList.size()) {
			SongList.remove(index);
		}
	}

	public int findSong(String name) {
		for (int index = 0; index < SongList.size(); index++) {
			if (SongList.get(index).getName().equals(name)) {
				return index;
			}
		}
		return -1;
	}

	public int nextRandomIndex(int currentindex, int totalindex) {
		if (totalindex < 2) {
			return currentindex;
		}
		Random rand = new Random();
		int next = -1;
		do {
			next = rand.nextInt(totalindex);
		} while (next == currentindex);

		return next;
	}
	public void setRandom(boolean on){
		isRandom = on;
	}
	public SongData getNextSong() {
		if (isRandom) {
			int random = nextRandomIndex(CurrentSong, size());
			CurrentSong = random;
			Log.d("Kyle","current index is "+CurrentSong + "; next song is "+random);
			return SongList.get(random);
		} else {
			if (CurrentSong < (SongList.size() - 1)) {
				SongData song = SongList.get(++CurrentSong);
				return song;
			} else {
				CurrentSong = 0;
				return SongList.get(0);
			}
		}
	}

	public SongData getPrevSong() {
		if (isRandom) {
			int random = nextRandomIndex(CurrentSong, size());
			CurrentSong = random;
			Log.d("Kyle","current index is "+CurrentSong + "; next song is "+random);
			return SongList.get(random);
		} else {
			if (CurrentSong > 0) {
				SongData song = SongList.get(--CurrentSong);
				return song;
			} else {
				SongData song = SongList.get(SongList.size() - 1);
				CurrentSong = SongList.size() - 1;
				return song;
			}
		}
	}

	public SongData getCurrSong() {
		return SongList.get(CurrentSong);
	}
	
	public ArrayList<String> getAllSongInfo(){
		ArrayList<String> songsinfo = new ArrayList<String>();
		for( SongData song:SongList){
			songsinfo.add(song.getArtist()+" - "+song.getName());
		}
		return songsinfo;
	}
}
