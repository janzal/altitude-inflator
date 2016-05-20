package org.cvut.bep;

import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.cvut.bep.missionplanner.WaypointFile;
import org.cvut.bep.missionplanner.WaypointItem;
import org.cvut.bep.providers.ElevationProvider;
import org.cvut.bep.providers.GoogleMapsElevationProvider;
import org.cvut.bep.providers.TilesElevationProvider;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by janzaloudek on 19/05/16.
 */
public class AltitudeInflatorApp {
    String fileName;
    String outputFileName;
    WaypointFile waypointFile;
    double offset;
    GeoApiContext geoApiContext;
    ElevationProvider elevationProvider;

    public AltitudeInflatorApp(String fileName, String outputFileName, double offset, ElevationProvider elevationProvider) {
        this.offset = offset;
        this.fileName = fileName;
        this.outputFileName = outputFileName;
        this.elevationProvider = elevationProvider;
    }

    public static AltitudeInflatorApp createFromArgs(CommandLine args) throws AltitudeInflatorException {
        double offset = Double.parseDouble(args.getOptionValue("offset", "0"));
        String apiKey = args.getOptionValue("api-key");
        String fileName = args.getOptionValue("file");
        String altOutputFilename = FilenameUtils.concat(FilenameUtils.getPath(fileName), FilenameUtils.getBaseName(fileName)
                + ".alt." + FilenameUtils.getExtension(fileName));
        String outputFilename = args.getOptionValue("output", altOutputFilename);

        ElevationProvider elevationProvider = null;
        String provider = args.getOptionValue("elevation-provider");
        if (provider == "gmaps") {
            if (!args.hasOption("api-key")) {
                throw new AltitudeInflatorException("API key is mandatory when gmaps elevation provider is used");
            }

            elevationProvider = GoogleMapsElevationProvider.createWithApiKey(apiKey);
        } else if (provider == "tiles") {
            if (!args.hasOption("tiles")) {
                throw new AltitudeInflatorException("Tiles folder (--tiles) is mandatory when tiles provider is used");
            }

            elevationProvider = new TilesElevationProvider();
        } else if (provider == null) {
            throw new AltitudeInflatorException("You have to define elevation provider");
        } else {
            throw new AltitudeInflatorException("Unknown provider \"" + provider + "\"");
        }

        return new AltitudeInflatorApp(fileName, outputFilename, offset,
                elevationProvider);
    }

    private void inflateFile() {
        final ArrayList<WaypointItem> items = waypointFile.getItems();

        LatLng[] points = new LatLng[items.size()];
        for (int i = 0; i < items.size(); i++) {
            points[i] = new LatLng(items.get(i).getLatitude(), items.get(i).getLongitude());
        }

        System.out.println("Retrieving elevation points from " + elevationProvider.getProviderName());
        double[] elevations = elevationProvider.getElevations(points);

        System.out.println("Updating altitudes...");
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setAltitude(elevations[i] + offset);
        }
    }

    private void loadWaypointFile() throws FileNotFoundException {
        waypointFile = null;
        waypointFile = WaypointFile.createFromFile(fileName);
    }

    private void saveOutput() throws FileNotFoundException {
        waypointFile.saveToFile(outputFileName);
    }

    public void run() throws Exception {
        System.out.println("Parsing input file: " + fileName);
        loadWaypointFile();
        System.out.println("Inflating original file...");
        inflateFile();
        System.out.println("Saving output file into: " + outputFileName);
        saveOutput();
    }
}
