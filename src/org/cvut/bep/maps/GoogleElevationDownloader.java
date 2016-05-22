package org.cvut.bep.maps;

import aglobe.util.Logger;
import org.cvut.bep.aglobex.gps.earth.ElevationMap;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class serves for downloading of elevation map tiles from google elevation
 * API. Similarly to {@link GoogleMapsDownloader} is use of this class subject to
 * API limits, that effectively allow to download five tiles a day with assigned
 * API key. Resulting tile downloaded using this class is cached and should be
 * processed by call {@link ElevationMap#buildElevationMapTile(int, int, byte, double[][], byte)}
 * to create appropriate QT structure.
 *
 * @author Kaiser Vojtech
 * @author Jan Zaloudek
 */
public class GoogleElevationDownloader {
    /**
     * Flag controlling amount of runtime info in console.
     */
    private final static boolean VERBOSE = false;

    /**
     * Time offset between fetch calls to google servers in milliseconds
     */
    private final static long FETCH_OFFSET = 400;

    private String tempFolder = "./work/";

    /**
     * Placement of cache folder where all downloaded JSON files will be stored
     */
    private String cacheFolder = tempFolder + "cache/";

    /**
     * API key binding request to an account and reducing the limits. Leave
     * <code>null</code> if no account binding should be done.
     */
    private String apiKey = null;

    /**
     * Timestamp of last request to google maps elevation API
     */
    private long lastFetch = 0;

    /**
     * Will return cache file constructed from given file key and predefined path
     * to cache folder.
     *
     * @param key to be used in filename
     * @return file reference
     */
    private File getCacheFile(String key) {
        return new File(cacheFolder + "/" + key + ".txt");
    }

    /**
     * Will load JSON string representation from cache if possible.
     *
     * @param key to be used to construct the file path
     * @return loaded string representation
     * @throws IOException in case of loading error
     */
    private String loadFromCache(String key) throws IOException {
        return new String(Files.readAllBytes(getCacheFile(key).toPath()));
    }

    /**
     * Will check whether there is file cached under given key.
     *
     * @param key to be checked
     * @return true if file under given key is cached
     */
    private boolean isCached(String key) {
        return getCacheFile(key).exists();
    }

    /**
     * Will try to load current request count for today from appropriate file.
     * If such file is not found, zero is returned.
     *
     * @return count of request sent to google this day
     */
    public int requestCount() {
        //construct name of information file
        String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
        File file = new File(tempFolder + "limit_elevation_" + date + (apiKey != null ? "_" + apiKey : "") + ".txt");

        //load previous value
        int count = 0;
        if (file.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                count = ois.readInt();
                ois.close();
            } catch (IOException e) {
                Logger.logSevere("Failed to retrieve current request count!");
            }
            ;
        }

        return count;
    }

    /**
     * Will check count of map requests for the day and compare it to limit
     * related the fact whether there is API KEY set.
     *
     * @return true if limit was exceeded, otherwise false
     */
    private boolean checkAndIncrementRequestCount() {
        //construct name of information file
        String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
        File file = new File(tempFolder + "limit_elevation_" + date + (apiKey != null ? "_" + apiKey : "") + ".dat");

        //load previous value
        int current = 0;
        if (file.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                current = ois.readInt();
                ois.close();
            } catch (IOException e) {
                Logger.logSevere("Failed to retrieve current request count!");
            }
            ;
        }

        //write new value
        try {
            file.createNewFile();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeInt(current + 1);
            oos.close();
        } catch (FileNotFoundException e) {
            Logger.logSevere("Failed to create request counter file!");
        } catch (IOException e) {
            Logger.logSevere("Failed to write request counter file!");
        }


        //check the limit
        if (apiKey != null) {
            //limit is 2500
            if (current == 2000) {
                Logger.logWarning("You are getting close to the limit of elevation "
                        + "requests today! (" + (current + 1) + "/2500)");
            } else if (current >= 2500) {
                Logger.logSevere("You have exceeded the limit of requests for today!");
                return true;
            }
        } else {
            //limit is 500
            if (current == 400) {
                Logger.logWarning("You are getting close to the limit of elevation "
                        + "requests today! (" + (current + 1) + "/500)");
            } else if (current >= 500) {
                Logger.logSevere("You have exceeded the limit of requests for today!");
                return true;
            }
        }

        return false;
    }

    /**
     * Will store give data in cache file constructed out of given key.
     *
     * @param key  to be used for file path construction
     * @param data to be stored in the file
     */
    private void storeToCache(String key, String data) {
        File file = getCacheFile(key);
        file.getParentFile().mkdirs();
        try {
            Files.write(file.toPath(), data.getBytes());
        } catch (IOException e) {
            Logger.logWarning("Failed to store data in cache (" + file + ")! ", e);
        }
    }

    /**
     * Will construct URL for google maps API for sampled path request.
     *
     * @param startLatitude  in degrees
     * @param startLongitude in degrees
     * @param endLatitude    in degrees
     * @param endLongitude   in degrees
     * @param sampleCount         in range from 2 up to 512
     * @return URL leading to JSON file containing elevation information
     * @throws MalformedURLException should be never thrown due to the way URL
     *                               is constructed
     */
    private URL getElevationPathURL(double startLatitude, double startLongitude, double endLatitude, double endLongitude, int sampleCount) {
        assert (sampleCount > 1 && sampleCount < 513);
        String url = "https://maps.googleapis.com/maps/api/elevation/json";
        url += "?path=" + startLatitude + "," + startLongitude + "|" + endLatitude + "," + endLongitude;
        url += "&samples=" + sampleCount;
        if (apiKey != null) {
            url += "&key=" + apiKey;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Logger.logSevere("Malformed URL was generated (" + url + ")!", e);
            return null;
        }
    }

    /**
     * Will construct URL for google maps API for elevation point request.
     *
     * @param latitude  in degrees
     * @param longitude in degrees
     * @return URL leading to JSON file containing elevation information
     * @throws MalformedURLException
     */
    @SuppressWarnings("unused")
    private URL getElevationPointURL(double latitude, double longitude) {
        String url = "https://maps.googleapis.com/maps/api/elevation/json";
        url += "?location=" + latitude + "," + longitude;
        if (apiKey != null) {
            url += "&key=" + apiKey;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Logger.logSevere("Malformed URL was generated (" + url + ")!", e);
            return null;
        }
    }

    /**
     * Will retrieve elevation on given URL with given cache key.
     *
     * @param url to data on google APU
     * @param key in the cache
     * @return JSON object in a string representation or <code>null<code> in case
     * of errors.
     * @throws LimitExceededException
     */
    private String getElevationData(URL url, String key) throws LimitExceededException {
        String data = null;

        //try to load from cache
        if (isCached(key)) {
            try {
                data = loadFromCache(key);
                if (data != null) {
                    if (VERBOSE) {
                        System.out.print(".");
                    }
                    return data;
                }
            } catch (IOException e) {
                Logger.logWarning("Failed to read cache! (" + getCacheFile(key) + ")", e);
            }
        }

        //check daily limit
        if (checkAndIncrementRequestCount()) {
            throw new LimitExceededException();
        }

        //check time offset from last fetch and possibly wait before executing
        //this fetch to avoid block by google server
        if (lastFetch + FETCH_OFFSET > System.currentTimeMillis()) {
            try {
                Thread.sleep((lastFetch + FETCH_OFFSET) - System.currentTimeMillis());
            } catch (InterruptedException e) {
            }
        }
        lastFetch = System.currentTimeMillis();

        //load data from the URL
        try {
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            data = CharStreams.toString(new InputStreamReader(bis, Charsets.UTF_8));
            Closeables.closeQuietly(bis);

            //parse JSON object status for data validity
            JSONObject jsonResponse = new JSONObject(data);
            String status = jsonResponse.getString("status");
            if (!status.equals("OK")) {
                Logger.logSevere("Request URL failed! (" + status + ")");
                return null;
            }

            //store data in cache
            storeToCache(key, data);

            if (VERBOSE) {
                System.out.print("!");
            }

            return data;
        } catch (IOException e) {
            Logger.logSevere("Failed to read data from URL! (" + url + ")", e);
            return null;
        } catch (JSONException e) {
            Logger.logSevere("Failed to parse success status out of JSON response!", e);
            return null;
        }
    }

    /**
     * Will load from google maps API elevation at points on given tile in quad
     * tree structure in given precision.
     *
     * @param idxLatitude  in quad tree
     * @param idxLongitude in quad tree
     * @param depth        in quad tree
     * @param sampleCount  on both axes
     * @return array of values where value at index (0,0) corresponds to left
     * upper corner of the tile, (sampleCount-1, sampleCount-1) right lower
     * corner and all values in between are evenly spaced. Indexing in returned
     * array is [x][y].
     * @throws LimitExceededException
     */
    public double[][] getElevationMapTile(int idxLatitude, int idxLongitude, byte depth, int sampleCount) throws LimitExceededException {
        double[][] retVal = new double[sampleCount][sampleCount];

        //calculate min, max latlong
        double degreesPerTile = 180.0 / (1 << depth);
        double degreesPerSample = degreesPerTile / sampleCount;
        double minLatitude = (idxLatitude + 1) * degreesPerTile - 90;
        double minLongitude = idxLongitude * degreesPerTile - 180;
        double maxLongitude = (idxLongitude + 1) * degreesPerTile - 180;

        System.out.println("Downloading tile [" + idxLatitude + ", " + idxLongitude + "] depth: " + depth + " /" + sampleCount);

        //for each row
        for (int y = 0; y < sampleCount; y++) {
            System.out.print("\rRows " + y + " of " + sampleCount + " fetched");
            //load data from google in JSON form
            double offset = degreesPerSample * y;
            String key = String.format("%f_%f_%f_%f_%d", minLatitude - offset, minLongitude, minLatitude - offset, maxLongitude, sampleCount);
            URL url = getElevationPathURL(minLatitude - offset, minLongitude, minLatitude - offset, maxLongitude, sampleCount);
            String data = getElevationData(url, key);
            if (data == null) {
                continue;
            }

            //parse JSON array of values
            JSONObject jsonResponse;
            try {
                jsonResponse = new JSONObject(data);
                JSONArray results = jsonResponse.getJSONArray("results");
                for (int x = 0; x < results.length(); x++) {
                    retVal[x][y] = results.getJSONObject(x).getDouble("elevation");
                }
            } catch (JSONException e) {
                Logger.logSevere("Failed to parse the JSON object!", e);
            }
        }
        if (VERBOSE) {
            System.out.println(" Done");
        }

        return retVal;
    }


    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    public String getCacheFolder() {
        return cacheFolder;
    }

    public void setTempFolder(String tempFolder) {
        this.tempFolder = tempFolder;
    }

    public String getTempFolder() {
        return tempFolder;
    }

    /**
     * This Exception is thrown when limit for map requests for the day was exceeded.
     */
    public class LimitExceededException extends Exception {
        private static final long serialVersionUID = 1L;

        public LimitExceededException() {
            super("Limit was exceeded!");
        }
    }
}
