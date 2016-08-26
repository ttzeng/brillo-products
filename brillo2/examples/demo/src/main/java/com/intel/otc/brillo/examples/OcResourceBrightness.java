package com.intel.otc.brillo.examples;

import android.util.Log;

import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.RequestHandlerFlag;
import org.iotivity.base.RequestType;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class OcResourceBrightness implements OcPlatform.EntityHandler {
    private static final String TAG = OcResourceBrightness.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.light.brightness";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static int SUCCESS = 200;

    private OcResourceHandle mHandle;
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

    public synchronized void destroy() {
        if (null != mHandle) {
            try {
                OcPlatform.unregisterResource(mHandle);
                Log.d(TAG, "Resource unregistered");
            } catch (OcException e) {
                error(e, "Failed to unregister resource");
            }
            mHandle = null;
        }
    }

    @Override
    public synchronized EntityHandlerResult handleEntity(final OcResourceRequest request) {
        EntityHandlerResult result = EntityHandlerResult.ERROR;
        if (null != request && null != mHandle) {
            // Get the request flags
            EnumSet<RequestHandlerFlag> requestFlags = request.getRequestHandlerFlagSet();
            if (requestFlags.contains(RequestHandlerFlag.INIT)) {
                Log.d(TAG, "\tRequest Flag: Init");
                result = EntityHandlerResult.OK;
            }
            if (requestFlags.contains(RequestHandlerFlag.REQUEST)) {
                Log.d(TAG, "\tRequest Flag: Request");
                result = handleRequest(request);
            }
            if (requestFlags.contains(RequestHandlerFlag.OBSERVER)) {
                Log.e(TAG, "\tRequest Flag: Observer - Not implemented yet");
            }
        }
        return result;
    }

    private EntityHandlerResult handleRequest(final OcResourceRequest request) {
        EntityHandlerResult result = EntityHandlerResult.ERROR;
        RequestType requestType = request.getRequestType();
        switch (requestType) {
            case GET:
                result = handleGetRequest(request);
                break;
            case POST:
                result = handlePostRequest(request);
        }
        return result;
    }

    private EntityHandlerResult handleGetRequest(final OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        new Thread(new Runnable() {
            @Override
            public void run() {
                response.setErrorCode(SUCCESS);
                response.setResponseResult(EntityHandlerResult.OK);
                response.setResourceRepresentation(getRepresentation());
                sendResponse(response);
            }
        }).start();
        return EntityHandlerResult.SLOW;
    }

    private EntityHandlerResult handlePostRequest(final OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());
        setRepresentation(request.getResourceRepresentation());
        response.setErrorCode(SUCCESS);
        return sendResponse(response);
    }

    private EntityHandlerResult sendResponse(OcResourceResponse response) {
        EntityHandlerResult result = EntityHandlerResult.ERROR;
        try {
            OcPlatform.sendResponse(response);
            result = EntityHandlerResult.OK;
        } catch (OcException e) {
            error(e, "Failed to send response");
        }
        return result;
    }

    public OcRepresentation getRepresentation() {
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

    public void setRepresentation(OcRepresentation rep) {
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

    private void error(OcException e, String msg) {
        Log.e(TAG, msg + "\n" + e.toString());
    }
}
