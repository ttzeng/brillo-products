package com.intel.otc.brillo.examples.demo.companion;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.iotivity.base.OcException;
import org.iotivity.base.OcResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResourceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ResourceAdapter.class.getSimpleName();
    private enum CardTypes {
        Brightness,
        AudioControl,
        Mp3Player,
    }

    private Context mContext;
    private Map<String, CardTypes> mSupportedResourceType = new HashMap<>();
    private ArrayList<Pair<CardTypes, OcResource>> mCardList = new ArrayList<>();

    public ResourceAdapter(Context context) {
        mContext = context;
        mSupportedResourceType.put(CardBrightness.RESOURCE_TYPE, CardTypes.Brightness);
        mSupportedResourceType.put(CardAudioControl.RESOURCE_TYPE, CardTypes.AudioControl);
        mSupportedResourceType.put(CardMp3Player.RESOURCE_TYPE, CardTypes.Mp3Player);
    }

    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType(" + position + ")");
        return mCardList.get(position).first.ordinal();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder(" + viewType + ")");
        RecyclerView.ViewHolder card = null;
        if (viewType == CardTypes.Brightness.ordinal()) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_brightness, parent, false);
            card = new CardBrightness(v, mContext);
        } else if (viewType == CardTypes.AudioControl.ordinal()) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_volume, parent, false);
            card = new CardAudioControl(v, mContext);
        } else if (viewType == CardTypes.Mp3Player.ordinal()) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_audioplayer, parent, false);
            card = new CardMp3Player(v, mContext);
        }
        return card;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder(" + position + ")");
        if (CardTypes.Brightness == mCardList.get(position).first) {
            ((CardBrightness) holder).bindResource(mCardList.get(position).second);
        } else if (CardTypes.AudioControl == mCardList.get(position).first) {
            ((CardAudioControl) holder).bindResource(mCardList.get(position).second);
        } else if (CardTypes.Mp3Player == mCardList.get(position).first) {
            ((CardMp3Player) holder).bindResource(mCardList.get(position).second);
        }
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }

    public void add(final OcResource resource) {
        for (String rt : resource.getResourceTypes())
            if (mSupportedResourceType.containsKey(rt)) {
                mCardList.add(new Pair<CardTypes, OcResource>(mSupportedResourceType.get(rt), resource));
            }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void clear() {
        try {
            for (Pair<CardTypes, OcResource> card : mCardList)
                card.second.cancelObserve();
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
        mCardList.clear();
        notifyDataSetChanged();
    }
}
