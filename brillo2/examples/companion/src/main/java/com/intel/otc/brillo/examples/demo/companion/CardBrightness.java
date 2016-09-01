package com.intel.otc.brillo.examples.demo.companion;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardBrightness extends CardOcResource implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = CardBrightness.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.light.brightness";
    private static final String KEY_BRIGHTNESS = "brightness";

    private SeekBar mSeekBarBrightness;

    CardBrightness(View parentView, Context context) {
        super(parentView, context);
        mSeekBarBrightness = (SeekBar) parentView.findViewById(R.id.brightnessLevel);
        mSeekBarBrightness.setOnSeekBarChangeListener(this);
    }

    @Override
    public void bindResource(OcResource resource) {
        super.bindResource(resource);
        getOcRepresentation();
    }

    @Override
    public synchronized void onGetCompleted(List<OcHeaderOption> list, OcRepresentation rep) {
        try {
            int brightness = rep.getValue(KEY_BRIGHTNESS);
            mSeekBarBrightness.setProgress(brightness);
            display("\tBrightness: " + brightness);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public synchronized void onPostCompleted(List<OcHeaderOption> list, OcRepresentation rep) {
        try {
            mSeekBarBrightness.setProgress((int) rep.getValue(KEY_BRIGHTNESS));
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        try {
            OcRepresentation rep = new OcRepresentation();
            rep.setValue(KEY_BRIGHTNESS, progress);
            Map<String, String> queryParams = new HashMap<>();
            mOcResource.post(rep, queryParams, this);
            display("\tChange brightness to " + progress);
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

    private void getOcRepresentation() {
        display("Getting representation of " + mOcResource.getHost() + mOcResource.getUri());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> queryParams = new HashMap<String, String>();
                try {
                    mOcResource.get(queryParams, CardBrightness.this);
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }
}
