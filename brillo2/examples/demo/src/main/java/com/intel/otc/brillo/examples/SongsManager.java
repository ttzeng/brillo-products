package com.intel.otc.brillo.examples;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * This class is modified from "Android Building Audio Player Tutorial"
 * http://www.androidhive.info/2012/03/android-building-audio-player-tutorial/
 */
public class SongsManager {
    private static final String TAG = SongsManager.class.getSimpleName();
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    final String MEDIA_PATH = new String("/sdcard/Music/");

    public SongsManager() {
        getPlayList();
    }

    /*
     * Function to read all mp3 files from sdcard and store the details in ArrayList
     */
    public ArrayList<HashMap<String, String>> getPlayList() {
        File home = new File(MEDIA_PATH);
        File[] mp3_files = home.listFiles(new Mp3FileFilter());

        songsList.clear();
        if (mp3_files.length > 0) {
            for (File file : mp3_files) {
                HashMap<String, String> song = new HashMap<String, String>();
                song.put("songTitle", file.getName().substring(0, (file.getName().length() - 4)));
                song.put("songPath", file.getPath());

                // Adding each song to SongList
                songsList.add(song);
                Log.d(TAG, "Found " + file.getPath());
            }
            Log.i(TAG, "Total " + songsList.size() + " songs found.");
        }
        // return songs list array
        return songsList;
    }

    /*
     * Class to filter files which are having .mp3 extension
     */
    class Mp3FileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".mp3") || name.endsWith(".MP3"));
        }
    }

    public String getSongTitle(int index) {
        return (0 <= index && index < songsList.size())? songsList.get(index).get("songTitle") : null;
    }

    public String getSongPath(int index) {
        return (0 <= index && index < songsList.size())? songsList.get(index).get("songPath") : null;
    }

    public int size() {
        return songsList.size();
    }
}
