package com.intel.otc.brillo.examples.demo.companion;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardAudioControl extends CardOcResource implements
        Switch.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener
{
    private static final String TAG = CardAudioControl.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.audio";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_MUTE = "mute";

    private Switch mSwitchVolume;
    private SeekBar mSeekBarVolume;

    CardAudioControl(View parentView, Context context) {
        super(parentView, context);
        mSwitchVolume = (Switch) parentView.findViewById(R.id.sw_volume);
        mSwitchVolume.setOnCheckedChangeListener(this);
        mSeekBarVolume = (SeekBar) parentView.findViewById(R.id.volumeLevel);
        mSeekBarVolume.setOnSeekBarChangeListener(this);
    }

    @Override
    public void bindResource(OcResource resource) {
        super.bindResource(resource);
        getOcRepresentation();
    }

    @Override
    public synchronized void onGetCompleted(List<OcHeaderOption> list, OcRepresentation rep) {
        try {
            boolean isMuted = rep.getValue(KEY_MUTE);
            int volume = rep.getValue(KEY_VOLUME);
            updateAudioStete(isMuted, volume);
            if (isMuted)
                display("\tAudio is muted");
            else
                display("\tAudio Volume: " + volume);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onPostCompleted(List<OcHeaderOption> list, OcRepresentation rep) {
    }

    private void getOcRepresentation() {
        display("Getting representation of " + mOcResource.getHost() + mOcResource.getUri());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> queryParams = new HashMap<String, String>();
                try {
                    mOcResource.get(queryParams, CardAudioControl.this);
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    private void setOcRepresentation() {
        try {
            OcRepresentation rep = new OcRepresentation();
            rep.setValue(KEY_MUTE, !mSwitchVolume.isChecked());
            int volume = mSeekBarVolume.getProgress();
            rep.setValue(KEY_VOLUME, volume);
            Map<String, String> queryParams = new HashMap<>();
            mOcResource.post(rep, queryParams, this);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setOcRepresentation();
        display("\tChange volume to " + progress);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, final boolean isChecked) {
        mSeekBarVolume.setEnabled(isChecked);
        setOcRepresentation();
        display("\tAudio is " + (isChecked? "unmuted" : "muted"));
    }

    private void updateAudioStete(final boolean mute, final int volume) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSwitchVolume.isChecked() == mute)
                    mSwitchVolume.setChecked(!mute);
                mSeekBarVolume.setEnabled(!mute);
                mSeekBarVolume.setProgress(volume);
            }
        });
    }
}
