package com.intel.otc.brillo.examples;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.iotivity.base.OcPlatform;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Mp3Player mp3Player;
    private LcdDisplayManager lcdDisplayManager;

    private OcServer ocServer;
    private OcResourceBrightness ocBrightness;
    private OcResourceColorRGB ocColorRGB;
    private OcResourceMp3Player ocMp3Player;
    private OcResourceAudioControl ocAudioControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mp3Player = new Mp3Player(this);
        new Thread(mp3Player).start();

        lcdDisplayManager = new LcdDisplayManager(mp3Player);
        new Thread(lcdDisplayManager).start();
        mp3Player.subscribeStateChangeNotification(lcdDisplayManager);

        ocServer = new OcServer(this);
        ocBrightness = new OcResourceBrightness(
                "/brillo/mp3player/brightness",
                OcPlatform.DEFAULT_INTERFACE,
                EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE),
                100, lcdDisplayManager);
        ocColorRGB = new OcResourceColorRGB(
                "/brillo/mp3player/rgb",
                OcPlatform.DEFAULT_INTERFACE,
                EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE),
                lcdDisplayManager);
        ocMp3Player = new OcResourceMp3Player(
                "/brillo/mp3player",
                OcPlatform.DEFAULT_INTERFACE,
                EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE),
                mp3Player);
        mp3Player.subscribeStateChangeNotification(ocMp3Player);
        ocAudioControl = new OcResourceAudioControl(
                "/brillo/mp3player/volume",
                OcPlatform.DEFAULT_INTERFACE,
                EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE),
                mp3Player);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
