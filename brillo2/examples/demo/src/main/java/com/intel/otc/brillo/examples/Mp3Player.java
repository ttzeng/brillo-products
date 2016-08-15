package com.intel.otc.brillo.examples;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Process;
import android.util.Log;

import java.io.IOException;

public class Mp3Player implements Runnable,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private static final String TAG = Mp3Player.class.getSimpleName();

    private Context mContext;
    private SongsManager sm;
    private int currentSongIndex = 0;
    private AudioManager am;
    private MediaPlayer mp;
    public enum MediaState {
        Idle, Playing, Paused
    }
    private MediaState mState;

    public Mp3Player(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        Log.d(TAG, "Initialize MP3 player...");
        // Set the current Thread priority as standard audio threads
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        // Get an instance of AudioManager for volume control
        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sm = new SongsManager();
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mState = MediaState.Idle;
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        mState = MediaState.Idle;
        if (++currentSongIndex < sm.size())
            Play();
        else currentSongIndex = 0;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        mp.start();
        mState = MediaState.Playing;
    }

    public void Play() {
        switch (mState) {
            case Idle:
                playSong(currentSongIndex);
                break;
            case Playing:
                mp.pause();
                mState = MediaState.Paused;
                break;
            case Paused:
                mp.start();
                mState = MediaState.Playing;
                break;
        }
    }

    public void Stop() {
        if (mState != MediaState.Idle) {
            mp.stop();
            mState = MediaState.Idle;
        }
    }

    public MediaState getCurrentState() {
        return mState;
    }

    public String getCurrentTitle() {
        return (mState == MediaState.Playing)? sm.getSongTitle(currentSongIndex) : null;
    }

    private void playSong(int index) {
        try {
            Log.d(TAG, "Playing " + sm.getSongTitle(index));
            mp.reset();
            mp.setDataSource(sm.getSongPath(index));
            mp.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
