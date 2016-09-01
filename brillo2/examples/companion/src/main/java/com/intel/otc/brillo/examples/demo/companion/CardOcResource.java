package com.intel.otc.brillo.examples.demo.companion;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;

import java.util.List;

abstract class CardOcResource extends RecyclerView.ViewHolder implements
        OcResource.OnGetListener,
        OcResource.OnPostListener,
        OcResource.OnObserveListener
{
    private static final String TAG = CardOcResource.class.getSimpleName();

    protected Context mContext;
    protected OcResource mOcResource = null;

    CardOcResource(View parentView, Context context) {
        super(parentView);
        mContext = context;
    }

    public void bindResource(OcResource resource) {
        mOcResource = resource;
    }

    @Override
    public synchronized void onGetFailed(Throwable throwable) {
        raiseException("GET", throwable);
    }

    @Override
    public synchronized void onPostFailed(Throwable throwable) {
        raiseException("POST", throwable);
    }

    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list, OcRepresentation rep, int sequenceNumber) {
        if (OcResource.OnObserveListener.REGISTER == sequenceNumber) {
            display("Observe registration on " + mOcResource.getUri() + " successful with SequenceNumber:" + sequenceNumber);
        } else if (OcResource.OnObserveListener.DEREGISTER == sequenceNumber) {
            display("Observe De-registration on " + mOcResource.getUri() + " successful");
        } else if (OcResource.OnObserveListener.NO_OPTION == sequenceNumber) {
            display("Failed to observe registration or de-registration");
        }
    }

    @Override
    public synchronized void onObserveFailed(Throwable throwable) {
        raiseException("Observe", throwable);
    }

    protected synchronized void raiseException(String op, Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocException = (OcException) throwable;
            Log.e(TAG, "Failed to " + op + " representation [" + ocException.getErrorCode() + "]\n" + ocException.toString());
        }
    }

    protected void display(final String text) {
        final MainActivity activity = (MainActivity) mContext;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.display(text);
            }
        });
    }
}
