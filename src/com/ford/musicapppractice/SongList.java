package com.ford.musicapppractice;

import java.util.ArrayList;
import java.util.List;

import android.media.MediaPlayer;
import android.util.Log;


public class SongList {
	public String ListName = "";
	private List<SongData> SongList = null;
	public int CurrentSong = 0;

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

	public SongData getNextSong() {
		if (CurrentSong < (SongList.size() - 1)) {
			SongData song = SongList.get(++CurrentSong);
			return song;
		} else {
			CurrentSong = 0;
			return SongList.get(0);
		}
	}

	public SongData getPrevSong() {
		if (CurrentSong > 0) {
			SongData song = SongList.get(--CurrentSong);
			return song;
		} else {
			SongData song = SongList.get(SongList.size() - 1);
			CurrentSong = SongList.size() - 1;
			return song;
		}
	}

	public SongData getCurrSong() {
		return SongList.get(CurrentSong);
	}
}


