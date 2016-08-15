package com.intel.otc.brillo.examples;

import android.os.Handler;
import android.os.HandlerThread;
import android.service.headless.HomeService;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;

public class MyTestService extends HomeService {
    private static final String TAG = "MyTestService";
    private static final int BUTTON_EDISON_RM_KEYCODE = 148;
    private static final int BUTTON_EDISON_PWR_KEYCODE = 116;

    private HandlerThread mRunnerThread;
    private Handler mRunnerThreadHandler;
    private Mp3Player mp3Player;

    @Override
    public void onCreate() {
        Log.d(TAG, "Headless service created");

        mRunnerThread = new HandlerThread("runnerThread");
        mRunnerThread.start();
        mRunnerThreadHandler = new Handler(mRunnerThread.getLooper());

        mp3Player = new Mp3Player(this);
        new Thread(mp3Player).start();
    }

    @Override
    public void onDestroy() {
        if (mRunnerThread != null) {
            mRunnerThread.quitSafely();
        }
        Log.d(TAG, "Headless service destroyed");
    }

    @Override
    public void onInputEvent(InputEvent event) {
        Log.d(TAG, "Input event received: " + event);
        if (((KeyEvent) event).getAction() == KeyEvent.ACTION_DOWN) {
            switch (((KeyEvent) event).getScanCode()) {
                case BUTTON_EDISON_RM_KEYCODE:
                    mp3Player.Play();
                    break;
                case BUTTON_EDISON_PWR_KEYCODE:
                    mp3Player.Stop();
                    break;
            }
        }
    }
}
