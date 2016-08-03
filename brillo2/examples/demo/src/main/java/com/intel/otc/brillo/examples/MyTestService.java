package com.intel.otc.brillo.examples;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.service.headless.HomeService;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;

import java.io.IOException;

public class MyTestService extends HomeService implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "MyTestService";
    private static final int BUTTON_EDISON_RM_KEYCODE = 148;

    private SongsManager sm;
    private int currentSongIndex = 0;
    private AudioManager am;
    private MediaPlayer mp;

    @Override
    public void onCreate() {
        Log.d(TAG, "Headless service created");
        sm = new SongsManager();
        // Get an instance of AudioManager for volume control
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        playSong(currentSongIndex);
    }

    @Override
    public void onInputEvent(InputEvent event) {
        Log.d(TAG, "Input event received: " + event);
        if (((KeyEvent) event).getAction() == KeyEvent.ACTION_DOWN) {
            switch (((KeyEvent) event).getScanCode()) {
                case BUTTON_EDISON_RM_KEYCODE:
                    if (mp.isPlaying()) {
                        mp.pause();
                    } else {
                        mp.start();
                    }
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        if (++currentSongIndex >= sm.size())
            currentSongIndex = 0;
    }

    public void playSong(int index) {
        try {
            Log.d(TAG, "Playing " + sm.getSongTitle(index));
            mp.reset();
            mp.setDataSource(sm.getSongPath(index));
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
