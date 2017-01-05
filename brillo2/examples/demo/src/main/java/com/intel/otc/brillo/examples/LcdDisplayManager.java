package com.intel.otc.brillo.examples;

import android.util.Log;

import java.util.concurrent.TimeUnit;

public class LcdDisplayManager implements Runnable,
        Mp3Player.OnMediaStateChangeListener,
        Mp3Player.OnVisualizerDataListener,
        OcResourceColorRGB.OnRGBChangedListener,
        OcResourceBrightness.OnBrightnessChangeListener
{
    private static final String TAG = LcdDisplayManager.class.getSimpleName();
    private static final int Service_Interval_In_Msec = 500;
    private static final byte charSpeaker = 0;
    private static final byte charLevel0  = 1;
    private static final byte charLevel1  = 2;
    private static final byte charLevel2  = 3;
    private static final byte charLevel3  = 4;
    private static final byte charLevel4  = 5;
    private static final byte charLevel5  = 6;
    private static final byte charLevel6  = 7;
    private static final byte charmapLevel0 [] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F };
    private static final byte charmapLevel1 [] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x1F };
    private static final byte charmapLevel2 [] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F, 0x1F, 0x1F };
    private static final byte charmapLevel3 [] = { 0x00, 0x00, 0x00, 0x00, 0x1F, 0x1F, 0x1F, 0x1F };
    private static final byte charmapLevel4 [] = { 0x00, 0x00, 0x00, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F };
    private static final byte charmapLevel5 [] = { 0x00, 0x00, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F };
    private static final byte charmapLevel6 [] = { 0x00, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F };
    private static final byte charmapSpeaker[] = { 0x02, 0x06, 0x1E, 0x1E, 0x1E, 0x06, 0x02, 0x00 };

    private Mp3Player mp3Player;
    private int timeEscapedInMsec = 0;
    private LcdRgbBacklight lcd;

    public LcdDisplayManager(Mp3Player player) {
        mp3Player = player;
        mp3Player.subscribeVisualizerData(this);
        lcd = new LcdRgbBacklight();
    }

    @Override
    public void run() {
        Log.d(TAG, "LCD display manager started");

        lcd.begin(16, 2, LcdRgbBacklight.LCD_5x10DOTS);
        lcd.createChar(charLevel0 , charmapLevel0 );
        lcd.createChar(charLevel1 , charmapLevel1 );
        lcd.createChar(charLevel2 , charmapLevel2 );
        lcd.createChar(charLevel3 , charmapLevel3 );
        lcd.createChar(charLevel4 , charmapLevel4 );
        lcd.createChar(charLevel5 , charmapLevel5 );
        lcd.createChar(charLevel6 , charmapLevel6 );
        lcd.createChar(charSpeaker, charmapSpeaker);
        boolean showTimeEscaped = false;
        while (true)
            try {
                TimeUnit.MILLISECONDS.sleep(Service_Interval_In_Msec);
                Mp3Player.MediaState state = mp3Player.getCurrentState();
                switch (state) {
                    case Idle:
                        continue;
                    case Playing:
                        timeEscapedInMsec += Service_Interval_In_Msec;
                        showTimeEscaped = true;
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
    public void onMediaStateChanged(Mp3Player.MediaState state) {
        switch (state) {
            case Idle:
                lcd.clear();
                timeEscapedInMsec = 0;
                break;
            case Playing:
                display(0, mp3Player.getCurrentTitle());
                break;
        }
    }

    @Override
    public void onVisualizerDataCaptured(byte[] frequency) {
        if (mp3Player.getCurrentState() != Mp3Player.MediaState.Idle) {
            int size = frequency.length / 16;
            if (size > 8) size = 8;
            byte db[] = new byte[size + 3];
            for (int i = 1; i < size; i++) {
                byte rfk = frequency[16 * i], ifk = frequency[16 * i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                int dbValue = (int) (10 * Math.log10(magnitude));
                if (dbValue < 0) dbValue = 0;
                if (dbValue > 6) dbValue = 6;
                db[i] = (byte) (dbValue + charLevel0);
            }
            db[0] = db[size] = ' ';
            db[size + 1] = charSpeaker;
            int level = Math.round(9f * mp3Player.getCurrentVolume() / mp3Player.getMaxVolume());
            db[size + 2] = (byte) ((level > 0)? ('0' + level) : 'x');
            lcd.setCursor(5, 1);
            lcd.write(new String(db));
        }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        if (0 <= brightness && brightness <= 100) {
            int c = brightness * 255 / 100;
            lcd.setRGB(c, c, c);
        }
    }

    @Override
    public void onRGBChangedListener(int red, int green, int blue) {
        lcd.setRGB(red, green, blue);
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
