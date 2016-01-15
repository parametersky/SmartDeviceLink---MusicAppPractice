package com.ford.onlinemusic;

import android.util.Log;

public class SongData {
	private String Name = "";
	private String Artist = "";
	private String Album = "";
	private int  AlbumID = 0;
	private String Url = "";
	private String AlbumPath = null;

	public SongData(String name, String artist, String album) {
		Name = name;
		Artist = artist;
		Album = album;
	}

	public SongData(String name, String artist, String album, String albumpath, String url) {
		Name = name;
		Artist = artist;
		Album = album;
		Url = url;
		AlbumPath = albumpath;
		Log.d("SongData","new song: "+ Name+ " "+ AlbumPath);
	}

	public SongData(String name, String artist, int albumID, String url) {
		Name = name;
		Artist = artist;
		AlbumID = albumID;
		Url = url;
	}
	
	public void setUrl(String str) {
		Url = str;
	}

	public String getUrl() {
		return Url;
	}

	public String getName() {
		return Name;
	}

	public String getArtist() {
		return Artist;
	}

	public String getAlbum() {
		return Album;
	}
	public String getAlbumPath(){
		return AlbumPath;
	}
}
