package com.intel.otc.brillo.examples.demo.companion;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.iotivity.base.OcResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResourceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ResourceAdapter.class.getSimpleName();
    private enum CardTypes {
        Brightness,
    }

    private Context mContext;
    private Map<String, CardTypes> mSupportedResourceType = new HashMap<>();
    private ArrayList<Pair<CardTypes, OcResource>> mCardList = new ArrayList<>();

    public ResourceAdapter(Context context) {
        mContext = context;
        mSupportedResourceType.put(CardBrightness.RESOURCE_TYPE, CardTypes.Brightness);
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_brightness, parent, false);             ;
            card = new CardBrightness(v, mContext);
        }
        return card;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder(" + position + ")");
        if (CardTypes.Brightness == mCardList.get(position).first) {
            ((CardBrightness) holder).bindResource(mCardList.get(position).second);
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
        mCardList.clear();
        notifyDataSetChanged();
    }
}
