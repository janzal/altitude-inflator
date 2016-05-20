package org.cvut.bep.providers;

import com.google.maps.model.LatLng;

/**
 * Created by janzaloudek on 20/05/16.
 */
public class TilesElevationProvider extends ElevationProvider {
    public final static String PROVIDER_NAME = "TILES";

    public TilesElevationProvider() {

    }

    @Override
    public double getElevation(LatLng point) {
        return 0;
    }

    @Override
    public double[] getElevations(LatLng[] points) {
        return new double[0];
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
