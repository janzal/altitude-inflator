package org.cvut.bep.providers;

import com.google.maps.model.LatLng;

/**
 * Created by janzaloudek on 20/05/16.
 */
public abstract class ElevationProvider {
    public abstract double getElevation(LatLng point);

    public abstract double[] getElevations(LatLng[] points);

    public abstract String getProviderName();
}
