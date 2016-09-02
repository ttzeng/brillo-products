package com.intel.otc.brillo.examples;

import android.util.Log;

import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class OcResourceAudioControl extends OcResourceBase {
    private static final String TAG = OcResourceAudioControl.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.audio";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_MUTE = "mute";

    private Mp3Player mp3Player;

    OcResourceAudioControl(String resourceUri, String resourceInterface, EnumSet<ResourceProperty> resourcePropertySet,
                           Mp3Player player) {
        try {
            mHandle = OcPlatform.registerResource(resourceUri, RESOURCE_TYPE, resourceInterface, this, resourcePropertySet);
            Log.d(TAG, "Resource " + resourceUri + " of type '" + RESOURCE_TYPE + "' registered");
            mp3Player = player;
        } catch (OcException e) {
            error(e, "Failed to register resource " + resourceUri);
            mHandle = null;
        }
    }

    @Override
    protected OcRepresentation getOcRepresentation() {
        OcRepresentation rep = null;
        if (null != mHandle) {
            rep = new OcRepresentation();
            try {
                rep.setValue(KEY_VOLUME, mp3Player.getCurrentVolume() * 100 / mp3Player.getMaxVolume());
                rep.setValue(KEY_MUTE, mp3Player.isMuted());
            } catch (OcException e) {
                error(e, "Failed to set representation values");
            }
        }
        return rep;
    }

    @Override
    protected void setOcRepresentation(OcRepresentation rep) {
        if (null != mHandle) {
            try {
                if (rep.hasAttribute(KEY_VOLUME)) {
                    mp3Player.setVolume((int) rep.getValue(KEY_VOLUME) * mp3Player.getMaxVolume() / 100);
                }
                boolean currentMuteState = mp3Player.isMuted();
                if (rep.hasAttribute(KEY_MUTE)) {
                    boolean mute = rep.getValue(KEY_MUTE);
                    if (!currentMuteState && mute)
                        mp3Player.mute();
                    else if (currentMuteState && !mute)
                        mp3Player.unmute();
                }
            } catch (OcException e) {
                error(e, "Failed to get representation values");
            }
        }
    }
}

