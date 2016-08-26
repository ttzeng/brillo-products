package com.intel.otc.brillo.examples;

import android.util.Log;

import java.util.concurrent.TimeUnit;

public class LcdDisplayManager implements Runnable,
        OcResourceBrightness.OnBrightnessChangeListener
{
    private static final String TAG = LcdDisplayManager.class.getSimpleName();
    private static final int Service_Interval_In_Msec = 500;

    private Mp3Player mp3Player;
    private LcdRgbBacklight lcd;

    public LcdDisplayManager(Mp3Player player) {
        mp3Player = player;
        lcd = new LcdRgbBacklight();
    }

    @Override
    public void run() {
        Log.d(TAG, "LCD display manager started");

        lcd.begin(16, 2, LcdRgbBacklight.LCD_5x10DOTS);
        int timeEscapedInMsec = 0;
        boolean showTimeEscaped = false;
        Mp3Player.MediaState state, last = mp3Player.getCurrentState();
        while (true)
            try {
                if (last != (state = mp3Player.getCurrentState()))
                    switch (last = state) {
                        case Idle:
                            lcd.clear();
                            timeEscapedInMsec = 0;
                            showTimeEscaped = false;
                            break;
                        case Playing:
                            display(0, mp3Player.getCurrentTitle());
                            showTimeEscaped = true;
                            break;
                    }
                TimeUnit.MILLISECONDS.sleep(Service_Interval_In_Msec);
                switch (state) {
                    case Idle:
                        continue;
                    case Playing:
                        timeEscapedInMsec += Service_Interval_In_Msec;
                        break;
                    case Paused:
                        showTimeEscaped = !showTimeEscaped;
                        break;
                }
                int second = timeEscapedInMsec / 1000;
                int minute = second / 60;
                second %= 60;
                int hour = minute / 60;
                minute %= 60;
                display(1, showTimeEscaped? (toLeadingZeroNumber(minute) + ":" + toLeadingZeroNumber(second)) : "     ");
            } catch (InterruptedException e) {
                // Ignore sleep nterruption
            }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        if (0 <= brightness && brightness <= 100) {
            int c = brightness * 255 / 100;
            lcd.setRGB(c, c, c);
        }
    }

    private void display(int row, String s) {
        lcd.setCursor(0, row);
        lcd.write(s);
    }

    private String toLeadingZeroNumber(int n) {
        n %= 100;
        return ((n < 10)? "0" : "") + n;
    }
}
