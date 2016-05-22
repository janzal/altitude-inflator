package org.cvut.bep;

import org.cvut.bep.maps.ElevationMapConvertor;
import org.cvut.bep.maps.GoogleElevationDownloader;
import org.apache.commons.cli.CommandLine;
import org.cvut.bep.missionplanner.WaypointFile;
import org.cvut.bep.missionplanner.WaypointItem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by janzaloudek on 21/05/16.
 */
public class InflatorTilesDownloader {
    public static final byte DEFAULT_DEPTH = 12;
    public static final int DEFAULT_SAMPLES_COUNT = 512;

    public byte depth = DEFAULT_DEPTH;
    public int samplesCount = DEFAULT_SAMPLES_COUNT;

    WaypointFile waypointFile;
    String fileName, outputFolder;
    Queue<String> apiKeys;

    public InflatorTilesDownloader(String fileName, String outputFolder, String[] apiKeys) {
        this.fileName = fileName;
        this.outputFolder = outputFolder;

        this.apiKeys = new LinkedList<String>(Arrays.asList(apiKeys));
    }

    public static InflatorTilesDownloader createFromArgs(CommandLine args) throws AltitudeInflatorException {
        String[] apiKeys = args.getOptionValues("api-key");
        String fileName = args.getOptionValue("file");
        String outputFolder = args.getOptionValue("tiles-folder");
        byte depth = Byte.parseByte(args.getOptionValue("tiles-depth",
                new Byte(InflatorTilesDownloader.DEFAULT_DEPTH).toString()));
        int samplesCount = Integer.parseInt(args.getOptionValue("tiles-samples-count",
                new Integer(InflatorTilesDownloader.DEFAULT_SAMPLES_COUNT).toString()));

        final InflatorTilesDownloader downloader = new InflatorTilesDownloader(fileName, outputFolder, apiKeys);
        downloader.setDepth(depth);
        downloader.setSamplesCount(samplesCount);
        return downloader;
    }

    private void loadWaypoints() throws FileNotFoundException {
        waypointFile = WaypointFile.createFromFile(fileName);
    }

    private double getDegreesPerTile(byte depth) {
        return 180.0 / (1 << depth);
    }

    private int getLatitudeIndex(double latitude) {
        double degreesPerTile = 360.0 / (2 << depth);
        int idxLatitude = (int) Math.floor((latitude + 90) / degreesPerTile) - (latitude == 90 ? 1 : 0);
        return idxLatitude;
    }

    private int getLongitudeIndex(double longitude) {
        double degreesPerTile = 360.0 / (2 << depth);
        int idxLongitude = (int) Math.floor((longitude + 180) / degreesPerTile) - (longitude == 180 ? 1 : 0);
        return idxLongitude;
    }

    private Set<IdPair> detectTiles() {
        final ArrayList<WaypointItem> items = waypointFile.getItems();
        Set<IdPair> pairs = new HashSet<IdPair>();

        for (WaypointItem item : items) {
            int idLat = getLatitudeIndex(item.getLatitude());
            int idLong = getLongitudeIndex(item.getLongitude());
            IdPair pair = new IdPair(idLat, idLong);
            pairs.add(pair);
        }

        return pairs;
    }

    private double[][] downloadTile(GoogleElevationDownloader downloader, IdPair pair) throws GoogleElevationDownloader.LimitExceededException {
        double[][] data = downloader.getElevationMapTile(pair.latitudeIndex,
                pair.longitudeIndex, depth, samplesCount);

        return data;
    }

    private void saveTiles(Set<IdPair> pairs) throws GoogleElevationDownloader.LimitExceededException, IOException {
        GoogleElevationDownloader downloader = new GoogleElevationDownloader();
        downloader.setApiKey(apiKeys.poll());

        for (IdPair pair : pairs) {
            double[][] data = null;
            try {
                data = downloadTile(downloader, pair);
            } catch (GoogleElevationDownloader.LimitExceededException e) {
                String apiKey = apiKeys.poll();
                downloader.setApiKey(apiKey);
                data = downloadTile(downloader, pair);
            }

            ElevationMapConvertor.exportTile(pair.latitudeIndex, pair.longitudeIndex, this.depth, data, outputFolder);
        }

//        ElevationMapConvsertor.exportTile(
//                idxLat, idxLong, (byte)12, data, "./work/datasets/GOOGLE_forest_elevation/converted 12/");
    }

    public void setDepth(byte depth) {
        this.depth = depth;
    }

    public void setSamplesCount(int samplesCount) {
        this.samplesCount = samplesCount;
    }

    public byte getDepth() {
        return depth;
    }

    public int getSamplesCount() {
        return samplesCount;
    }

    public void run() throws IOException {
        loadWaypoints();
        final Set<IdPair> idPairs = detectTiles();
        try {
            saveTiles(idPairs);
        } catch (GoogleElevationDownloader.LimitExceededException e) {
            e.printStackTrace();
        }
    }

    class IdPair {
        public int latitudeIndex, longitudeIndex;

        public IdPair(int latIndex, int longIndex) {
            latitudeIndex = latIndex;
            longitudeIndex = longIndex;
        }

        @Override
        public String toString() {
            return "Pair <" + latitudeIndex + ", " + longitudeIndex + ">";
        }

        @Override
        public boolean equals(Object obj) {
            IdPair pair = (IdPair) obj;
            return (latitudeIndex == pair.latitudeIndex && longitudeIndex == pair.longitudeIndex);
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 37 * result + latitudeIndex;
            result = 19 * result + longitudeIndex;
            return result;
        }
    }
}
