package com.intel.otc.brillo.examples;

import android.util.Log;

import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class OcResourceBrightness extends OcResourceBase {
    private static final String TAG = OcResourceBrightness.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.light.brightness";
    private static final String KEY_BRIGHTNESS = "brightness";

    private int brightness;
    private OnBrightnessChangeListener brightnessChangeListener = null;

    public interface OnBrightnessChangeListener {
        void onBrightnessChanged(int brightness);
    }

    OcResourceBrightness(String resourceUri, String resourceInterface, EnumSet<ResourceProperty> resourcePropertySet,
                         int initBrightness, OnBrightnessChangeListener listener) {
        try {
            mHandle = OcPlatform.registerResource(resourceUri, RESOURCE_TYPE, resourceInterface, this, resourcePropertySet);
            brightness = initBrightness;
            brightnessChangeListener = listener;
            Log.d(TAG, "Resource " + resourceUri + " of type '" + RESOURCE_TYPE + "' registered");
        } catch (OcException e) {
            error(e, "Failed to register resource " + resourceUri);
            mHandle = null;
        }
    }

    protected EntityHandlerResult handleGetRequest(OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        new Thread(new Runnable() {
            @Override
            public void run() {
                response.setErrorCode(SUCCESS);
                response.setResponseResult(EntityHandlerResult.OK);
                response.setResourceRepresentation(getOcRepresentation());
                sendResponse(response);
            }
        }).start();
        return EntityHandlerResult.SLOW;
    }

    protected OcRepresentation getOcRepresentation() {
        OcRepresentation rep = null;
        if (null != mHandle) {
            rep = new OcRepresentation();
            try {
                rep.setValue(KEY_BRIGHTNESS, brightness);
            } catch (OcException e) {
                error(e, "Failed to set '" + KEY_BRIGHTNESS + "' representation value");
            }
        }
        return rep;
    }

    protected void setOcRepresentation(OcRepresentation rep) {
        if (null != mHandle) {
            try {
                if (rep.hasAttribute(KEY_BRIGHTNESS)) brightness = rep.getValue(KEY_BRIGHTNESS);
                if (null != brightnessChangeListener)
                    brightnessChangeListener.onBrightnessChanged(brightness);
                Log.d(TAG, "Brightness changed to " + brightness);
            } catch (OcException e) {
                error(e, "Failed to get '" + KEY_BRIGHTNESS + "' representation value");
            }
        }
    }
}
