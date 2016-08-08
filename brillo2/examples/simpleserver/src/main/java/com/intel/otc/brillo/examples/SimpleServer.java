package com.intel.otc.brillo.examples;

import android.content.Context;
import android.service.headless.HomeService;
import android.util.Log;
import android.view.InputEvent;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.util.LinkedList;
import java.util.List;

public class SimpleServer extends HomeService {
    private static final String TAG = SimpleServer.class.getSimpleName();

    List<Light> lights = new LinkedList<>();

    private void startSimpleServer() {
        Context context = this;

        PlatformConfig platformConfig = new PlatformConfig(
                null,
                context,
                ServiceType.IN_PROC,
                ModeType.SERVER,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );

        msg("Configuring platform.");
        OcPlatform.Configure(platformConfig);

        createNewLightResource("/a/light", "Tonny's light");

        msg("Waiting for the requests...");
        printLine();
    }

    public void createNewLightResource(String resourceUri, String resourceName){
        msg("Creating a light");
        Light light = new Light(
                resourceUri,     //URI
                resourceName,    //name
                false,           //state
                0                //power
        );
        msg(light.toString());
        light.setContext(this);

        msg("Registering light as a resource");
        try {
            light.registerResource();
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to register a light resource");
        }
        lights.add(light);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Headless service started");
        new Thread(new Runnable() {
            public void run() {
                startSimpleServer();
            }
        }).start();
    }

    @Override
    public void onInputEvent(InputEvent event) {
        Log.d(TAG, "Input event received: " + event);
    }

    private void msg(final String text) {
        Log.i(TAG, text);
    }

    private void printLine() {
        msg("------------------------------------------------------------------------");
    }
}
