package org.cvut.bep.providers;

import org.cvut.bep.aglobex.gps.earth.ElevationMap;
import com.google.maps.model.LatLng;

/**
 * Created by janzaloudek on 20/05/16.
 */
public class TilesElevationProvider extends ElevationProvider {
    public final static String PROVIDER_NAME = "TILES";

    private String resourcePath;
    private byte maximumDepth;
    private ElevationMap elevationMap;

    public TilesElevationProvider(String resourcePath, byte maximumDepth, int resolution) {
        this.resourcePath = resourcePath;
        this.maximumDepth = maximumDepth;
        this.elevationMap = new ElevationMap(resourcePath, maximumDepth, resolution);
    }

    @Override
    public double getElevation(LatLng point) {
        return elevationMap.getElevationM(point.lat, point.lng);
    }

    @Override
    public double[] getElevations(LatLng[] points) {
        double[] result = new double[points.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = elevationMap.getElevationM(points[i].lat, points[i].lng);
        }

        return result;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
