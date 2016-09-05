package com.intel.otc.brillo.examples;

import android.util.Log;

import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class OcResourceMp3Player extends OcResourceBase implements Mp3Player.OnMediaStateChangeListener {
    private static final String TAG = OcResourceMp3Player.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "x.com.intel.demo.mp3player";
    private static final String KEY_MEDIASTATES = "mediaStates";
    private static final String KEY_STATE = "state";
    private static final String KEY_TITLE = "title";
    private static final long Notifier_Interval_In_Msec = 60000;

    private Mp3Player mp3Player;

    OcResourceMp3Player(String resourceUri, String resourceInterface, EnumSet<ResourceProperty> resourcePropertySet,
                        Mp3Player player) {
        try {
            mHandle = OcPlatform.registerResource(resourceUri, RESOURCE_TYPE, resourceInterface, this, resourcePropertySet);
            Log.d(TAG, "Resource " + resourceUri + " of type '" + RESOURCE_TYPE + "' registered");
        } catch (OcException e) {
            error(e, "Failed to register resource " + resourceUri);
            mHandle = null;
        }
        mp3Player = player;
        mNotifyInterval = Notifier_Interval_In_Msec;
    }

    @Override
    protected OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();
        try {
            rep.setValue(KEY_MEDIASTATES, new String[] {
                    Mp3Player.MediaState.Idle.toString(),
                    Mp3Player.MediaState.Playing.toString(),
                    Mp3Player.MediaState.Paused.toString(),
            });
            rep.setValue(KEY_STATE, mp3Player.getCurrentState().toString());
            String title = mp3Player.getCurrentTitle();
            if (null != title)
                rep.setValue(KEY_TITLE, title);
        } catch (OcException e) {
            error(e, "Failed to set representation value");
        }
        return rep;
    }

    @Override
    protected void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(KEY_STATE)) {
                String v = rep.getValue(KEY_STATE);
                if (v.equals(Mp3Player.MediaState.Idle.toString()))
                    mp3Player.Stop();
                else if (v.equals(Mp3Player.MediaState.Playing.toString()) ||
                         v.equals(Mp3Player.MediaState.Paused.toString()))
                    mp3Player.Play();
            }
        } catch (OcException e) {
            error(e, "Failed to get representation value");
        }
    }

    @Override
    public void onMediaStateChanged(Mp3Player.MediaState state) {
        try {
            if (mObservationIds.size() > 0) {
                Log.d(TAG, "Notifying observers as media state changed...");
                OcPlatform.notifyAllObservers(mHandle);
            }
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            mObservationIds.clear();
        }
    }
}
