package org.cvut.bep.providers;

import com.google.maps.ElevationApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.ElevationResult;
import com.google.maps.model.LatLng;

/**
 * Created by janzaloudek on 20/05/16.
 */
public class GoogleMapsElevationProvider extends ElevationProvider {
    public final static String PROVIDER_NAME = "GMAPS";

    GeoApiContext context;

    public GoogleMapsElevationProvider(GeoApiContext context) {
        this.context = context;
    }

    public static GoogleMapsElevationProvider createWithApiKey(String key) {
        GeoApiContext geoApiContext = new GeoApiContext().setApiKey(key);
        return new GoogleMapsElevationProvider(geoApiContext);
    }

    @Override
    public double getElevation(LatLng point) {
        PendingResult<ElevationResult> result = ElevationApi.getByPoint(context, point);
        double elevation = 0;

        try {
            elevation = result.await().elevation;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return elevation;
    }

    @Override
    public double[] getElevations(LatLng[] points) {
        PendingResult<ElevationResult[]> result = ElevationApi.getByPoints(context, points);
        ElevationResult[] elevations = null;
        try {
            elevations = result.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] elevationDoubles = new double[elevations.length];
        for (int i = 0; i < elevations.length; i++) {
            elevationDoubles[i] = elevations[i].elevation;
        }

        return elevationDoubles;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
