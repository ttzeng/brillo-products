package com.intel.otc.brillo.examples.demo.companion;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.iotivity.base.ObserveType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardMp3Player extends CardOcResource implements AdapterView.OnItemSelectedListener {
    private static final String TAG = CardMp3Player.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "x.com.intel.demo.mp3player";
    private static final String KEY_MEDIASTATES = "mediaStates";
    private static final String KEY_STATE = "state";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PLAYLIST = "playList";
    private static final String KEY_SELECT = "select";
    private enum PlayerState {
        Idle, Playing, Paused
    }

    private List<String> mValidStates = null;
    private int mCurrentStateIndex = 0;
    private String[] mPlayList = null;
    private int mLastTitlePos = 0;
    private Spinner mSpinnerTitle;
    private ImageButton mImageButtonPlayPause;
    private ImageButton mImageButtonStop;

    CardMp3Player(View parentView, Context context) {
        super(parentView, context);
        mSpinnerTitle = (Spinner) parentView.findViewById(R.id.spinner_title);
        mSpinnerTitle.setOnItemSelectedListener(this);
        mImageButtonPlayPause = (ImageButton) parentView.findViewById(R.id.btn_play_pause);
        mImageButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setOcRepresentation((mCurrentStateIndex == PlayerState.Playing.ordinal())?
                        PlayerState.Paused : PlayerState.Playing);
            }
        });
        mImageButtonStop = (ImageButton) parentView.findViewById(R.id.btn_stop);
        mImageButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setOcRepresentation(PlayerState.Idle);
            }
        });
    }

    @Override
    public void bindResource(OcResource resource) {
        super.bindResource(resource);
        getOcRepresentation();
        observeOcRepresentation();
    }

    @Override
    public synchronized void onGetCompleted(List<OcHeaderOption> list, final OcRepresentation rep) {
        showPlayerState(rep);
    }

    @Override
    public synchronized void onPostCompleted(List<OcHeaderOption> list, OcRepresentation rep) {
        getOcRepresentation();
    }

    @Override
    public synchronized void onPostFailed(Throwable throwable) {
        super.onPostFailed(throwable);
        getOcRepresentation();
    }

    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation rep, int sequenceNumber) {
        super.onObserveCompleted(list, rep, sequenceNumber);
        showPlayerState(rep);
    }

    private void getOcRepresentation() {
        display("Getting representation of " + mOcResource.getHost() + mOcResource.getUri());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> queryParams = new HashMap<String, String>();
                    mOcResource.get(queryParams, CardMp3Player.this);
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    private void setOcRepresentation(final PlayerState state) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OcRepresentation rep = new OcRepresentation();
                    rep.setValue(KEY_STATE, mValidStates.get(state.ordinal()));
                    Map<String, String> queryParams = new HashMap<>();
                    mOcResource.post(rep, queryParams, CardMp3Player.this);
                    display("Change to '" + state.toString() + "' state");
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    private void setOcRepresentation(final int pos) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OcRepresentation rep = new OcRepresentation();
                    rep.setValue(KEY_SELECT, pos);
                    Map<String, String> queryParams = new HashMap<>();
                    mOcResource.post(rep, queryParams, CardMp3Player.this);
                    display("Select '" + mPlayList[pos] + "'");
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        Log.e(TAG, "Selected: " + pos);
        if (mLastTitlePos != pos) {
            setOcRepresentation(pos);
            mLastTitlePos = pos;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void observeOcRepresentation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mOcResource.observe(ObserveType.OBSERVE, new HashMap<String, String>(), CardMp3Player.this);
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }).start();
    }

    private void showPlayerState(OcRepresentation rep) {
        try {
            if (null == mValidStates && rep.hasAttribute(KEY_MEDIASTATES))
                mValidStates = Arrays.asList((String[]) rep.getValue(KEY_MEDIASTATES));
            if (rep.hasAttribute(KEY_PLAYLIST))
                mPlayList = rep.getValue(KEY_PLAYLIST);
            int state = mValidStates.indexOf(rep.getValue(KEY_STATE));
            if (PlayerState.Idle.ordinal() <= state && state <= PlayerState.Paused.ordinal()) {
                mCurrentStateIndex = state;
                String title = rep.hasAttribute(KEY_TITLE) ? (String) rep.getValue(KEY_TITLE) : null;
                showPlayerState(state, title);
            }
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void showPlayerState(final int state, final String title) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayList != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, mPlayList);
                    mSpinnerTitle.setAdapter(adapter);
                    int pos;
                    if (title != null && (pos = adapter.getPosition(title)) >= 0)
                        mLastTitlePos = pos;
                    mSpinnerTitle.setSelection(mLastTitlePos);
                }
                mImageButtonStop.setEnabled(state != PlayerState.Idle.ordinal());
                mImageButtonPlayPause.setImageResource((state == PlayerState.Playing.ordinal())?
                        android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });
    }
}
