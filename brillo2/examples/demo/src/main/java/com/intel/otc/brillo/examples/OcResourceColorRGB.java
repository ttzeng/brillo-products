package com.intel.otc.brillo.examples;

import android.util.Log;

import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;

public class OcResourceColorRGB extends OcResourceBase {
    private static final String TAG = OcResourceColorRGB.class.getSimpleName();
    public  static final String RESOURCE_TYPE = "oic.r.colour.rgb";
    private static final String KEY_RGBVALUE = "rgbValue";
    private static final String KEY_RANGE = "range";
    private static final int MAX_VALUE = 255;

    private int red, green, blue;
    private OnRGBChangedListener rgbChangedListener = null;

    public interface OnRGBChangedListener {
        void onRGBChangedListener(int red, int green, int blue);
    }

    OcResourceColorRGB(String resourceUri, String resourceInterface, EnumSet<ResourceProperty> resourcePropertySet,
                       OnRGBChangedListener listener) {
        red = green = blue = MAX_VALUE;
        try {
            mHandle = OcPlatform.registerResource(resourceUri, RESOURCE_TYPE, resourceInterface, this, resourcePropertySet);
            rgbChangedListener = listener;
            Log.d(TAG, "Resource " + resourceUri + " of type '" + RESOURCE_TYPE + "' registered");
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
                rep.setValue(KEY_RGBVALUE, new int[] { red, green, blue});
                rep.setValue(KEY_RANGE, new int[] { 0, MAX_VALUE });
            } catch (OcException e) {
                error(e, "Failed to set RGB color representation value");
            }
        }
        return rep;
    }

    @Override
    protected void setOcRepresentation(OcRepresentation rep) {
        if (null != mHandle) {
            try {
                if (rep.hasAttribute(KEY_RGBVALUE)) {
                    int[] rgbValues = rep.getValue(KEY_RGBVALUE);
                    red   = rgbValues[0];
                    green = rgbValues[1];
                    blue  = rgbValues[2];
                }
                if (null != rgbChangedListener)
                    rgbChangedListener.onRGBChangedListener(red, green, blue);
                Log.d(TAG, "Color changed to (" + red + "," + green + "," + blue + ")");
            } catch (OcException e) {
                error(e, "Failed to get RGB color representation value");
            }
        }
    }
}
