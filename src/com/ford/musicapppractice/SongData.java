package com.ford.musicapppractice;

public class SongData {
	private String Name = "";
	private String Artist = "";
	private String Album = "";
	private String Url = "";

	public SongData(String name, String artist, String album) {
		Name = name;
		Artist = artist;
		Album = album;
	}

	public SongData(String name, String artist, String album, String url) {
		Name = name;
		Artist = artist;
		Album = album;
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
}
