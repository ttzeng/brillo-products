package com.intel.otc.brillo.examples;

import android.content.Context;
import android.util.Log;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

public class OcServer {
    private static final String TAG = OcServer.class.getSimpleName();

    private Context mContext;

    public OcServer(Context context) {
        Log.d(TAG, "Configuring platform...");
        PlatformConfig platformConfig = new PlatformConfig(
                mContext = context,
                ServiceType.IN_PROC,
                ModeType.SERVER,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );
        OcPlatform.Configure(platformConfig);
    }
}
