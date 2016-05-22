package org.cvut.bep.aglobex.gps.earth;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import aglobe.util.Logger;
import sun.misc.SoftCache;

/**
 * This class represents elevation map that dynamically loads elevation data from
 * quad tree structure on disk.
 *
 * @author Kaiser Vojtech
 */
public class ElevationMap {
    /**
     * Maximum depth of tile that will be tried when lookup of tile is done without
     * specified preferred depth. This value is dependent on used dataset and
     * has to be changed along with dataset directory!
     */
    private final static byte DEFAULT_MAXIMUM_DEPTH = 6;

    private final static int DEFAULT_RESOLUTION = 512;

    /**
     * Data extension that is used for map tiles. It can be either any image
     * extension {@link ImageIO} can read out of the box, or {@code bin} extension
     * marking the data as 16 bit USHORT grayscale.
     */
    private final static String DATA_EXTENSION = "bin";

    /**
     * If true, extra system outs are printed.
     */
    private final static boolean VERBOSE = true;

    /**
     * If true, will store extra debug images.
     */
    private final static boolean DEBUG = true;

    /**
     * Directory containing quad tree structure of elevation map tiles. It is
     * expected this directory will contain at least complete level zero! This
     * directory path should be relative to workspace directory.
     */
    /*
    unused
     */
    private final static String DATASET_DIRECTORY = "/_elevation_earth/";

    /**
     * Cache containing altitudes that have been looked up in recent past.
     * TODO measure time savings of this cache
     */
    private final SoftCache altitudeCache = new SoftCache();

    /**
     * Maximum depth of tile that will be tried when lookup of tile is done without
     * specified preferred depth. This value is dependent on used dataset and
     * has to be changed along with dataset directory!
     */
    private final byte maximumDepth;


    /**
     * Tiles resolution
     */
    private final int resolution;

    /**
     * Cache containing tiles loaded in recent past. Key is combination of indices
     * and depth in following scheme:<br>
     * <pre>
     * {@code
     * long key = (idxLatitude<<30)+(idxLongitude<<5)+(depth&&255);
     * }
     * </pre>
     */
    private Map<Long, SoftReference<ElevationTile>> tilesCache = new HashMap<Long, SoftReference<ElevationTile>>();

    /**
     * True if this elevation map should always return zero. Zero map performs
     * no loads and will not crash on missing elevation dataset on the given path.
     */
    private final boolean isZeroMap;

    /**
     * Path to repository used to find path to files in elevation dataset.
     */
    private final String repositoryPath;

    /**
     * Will create elevation map that always returns zero
     */
    public ElevationMap() {
        this(null);
    }

    /**
     * Will create elevation map that looks for its data in {@link #DATASET_DIRECTORY}
     * directory under given repository path.
     *
     * @param repositoryPath to be used
     */
    public ElevationMap(String repositoryPath) {
        this(repositoryPath, DEFAULT_MAXIMUM_DEPTH);
    }

    /**
     * Will create new elevation map from given repository and in maximum depth in default resolution.
     *
     * @param repositoryPath to be used
     * @param maximumDepth   that is tested
     */
    public ElevationMap(String repositoryPath, byte maximumDepth) {
        this(repositoryPath, maximumDepth, DEFAULT_RESOLUTION);
    }

    /**
     * Will create new elevation map from given repository and in maximum depth in defined resolution.
     *
     * @param repositoryPath to be used
     * @param maximumDepth   that is tested
     */
    public ElevationMap(String repositoryPath, byte maximumDepth, int resolution) {
        this.isZeroMap = repositoryPath == null;
        this.repositoryPath = repositoryPath;
        this.maximumDepth = maximumDepth;
        this.resolution = resolution;
    }

    /**
     * Will try to load tile of given indices at given depth, but if such record
     * does not exist in currently used elevation map, requirements are shifted
     * level up repeatedly until a tile meeting these requirements is encountered.
     *
     * @param idxLatitude  in quad tree
     * @param idxLongitude in quad tree
     * @param depth        in the quad tree (preferred)
     * @return tile or <code>null</code> if not found at all
     */
    private ElevationTile fetchTile(int idxLatitude, int idxLongitude, byte depth) {
        for (; depth >= 0; depth--, idxLatitude /= 2, idxLongitude /= 2) {
            //try cache
            long key = (((long) idxLatitude) << 30) + (((long) idxLongitude) << 5) + (depth & 31);
            SoftReference<ElevationTile> tileReference = tilesCache.get(key);
            ElevationTile tile = null;
            if (tileReference != null && (tile = tileReference.get()) != null) {
                return tile;
            }

            //try to load the file
            File file = getTileFile(idxLatitude, idxLongitude, depth);
            if (!file.exists()) {
                continue;
            }
            try {
                tile = new ElevationTile(file, idxLatitude, idxLongitude, depth, resolution);
            } catch (IOException e) {
                Logger.logWarning("Failed to read elevation data file '" + file + "'!");
                continue;
            }

            //cache tile
            tilesCache.put(key, new SoftReference<ElevationTile>(tile));

            return tile;
        }
        return null;
    }

    /**
     * Will construct tile file from given indices, depth and preset repository
     * path with dataset directory. Caution, this structure is prepared to depth
     * at most 13 (2^14 is five digit index violating currently set system).
     *
     * @param idxLatitude  in quad tree
     * @param idxLongitude in quad tree
     * @param depth        in quad tree
     * @return created file reference which may or may not exist
     */
    private File getTileFile(int idxLatitude, int idxLongitude, byte depth) {
        assert (depth < 14);
        return new File(String.format(repositoryPath + "/"
                + "/data_%04d_%04d_%02d.%s", idxLatitude, idxLongitude, depth, DATA_EXTENSION));
    }

    /**
     * Will calculate to which tile at desired depth does point at given latlong
     * belong and retrieve it.
     *
     * @param latitude  in degrees
     * @param longitude in degrees
     * @param depth     in quad tree (preferred)
     * @return found tile or <code>null</code> in case there is no elevation map
     * at all (that is critical case)
     */
    private ElevationTile fetchTile(double latitude, double longitude, byte depth) {
        //calculate indices
        double degreesPerTile = 360.0 / (2 << depth);
        int idxLatitude = (int) Math.floor((latitude + 90) / degreesPerTile) - (latitude == 90 ? 1 : 0);
        int idxLongitude = (int) Math.floor((longitude + 180) / degreesPerTile) - (longitude == 180 ? 1 : 0);

        //load tile and save it as previous
        ElevationTile tile = this.fetchTile(idxLatitude, idxLongitude, depth);

        return tile;
    }

    /**
     * Will locate given latlong in elevation map and return most precise altitude
     * that is available in current dataset.
     *
     * @param latitude  in degrees
     * @param longitude in degrees
     * @return altitude in meters
     */
    public double getElevationM(double latitude, double longitude) {
        return this.getElevationM(latitude, longitude, maximumDepth);
    }

    /**
     * Will retrieve altitude in meters at specified point in latlong and in
     * given depth. When depth is larger than best available, best available is
     * used.
     *
     * @param latitude  in degrees
     * @param longitude in degrees
     * @param depth     in quad tree, is equivalent to zoom level
     * @return altitude in meters
     */
    public double getElevationM(double latitude, double longitude, byte depth) {
        if (isZeroMap) {
            return 0;
        }

        //try position cache / return value
        PositionKey key = new PositionKey(latitude, longitude);
        Double altitude = (Double) altitudeCache.get(key);
        if (altitude != null) {
            return altitude.doubleValue();
        }

        //fetch related tile
        ElevationTile tile = fetchTile(latitude, longitude, depth);

        //fetch specific position from tile
        altitude = tile.getElevationM(latitude, longitude);

        //cache position
        altitudeCache.put(key, altitude);

        return altitude.doubleValue();
    }

    /**
     * @return true if this map always returns zero and loads no data
     */
    public boolean isZeroMap() {
        return isZeroMap;
    }

    /**
     * Will insert new elevation map tile in currently used elevation map structure
     * and will update all the levels above it. This method is here to couple all
     * methods operating data for this class.
     *
     * @param idxLatitude  in the quad tree
     * @param idxLongitude in the quad tree
     * @param depth        in the quad tree
     * @param data         array containing elevations in meters where (0,0) corresponds
     *                     to left upper corner of the tile and (lenght.x, length.y) to right lower
     *                     corner. Indexing follows (x,y).
     * @throws IOException in case of failed save of some tile
     */
    public void buildElevationMapTile(int idxLatitude, int idxLongitude, byte depth,
                                      double[][] data, byte propagationDepth) throws IOException {
        if (VERBOSE) {
            System.out.println("> Construction of tile structure");
        }
        //fetch first available tile in current tree
        ElevationTile current = fetchTile(idxLatitude >> (depth - propagationDepth),
                idxLongitude >> (depth - propagationDepth), propagationDepth);

        if (DEBUG) {
            current.storeDebugImage(new File("./work/test_" + "current" + ".png"), 1, 24);
        }

        //create target tile and store it
        ElevationTile target = new ElevationTile(data, idxLatitude, idxLongitude, depth, resolution);

        if (DEBUG) {
            target.storeDebugImage(new File("./work/test_" + "target" + ".png"), 1, 24);
        }

        //while current tile is not target tile
        while (!current.equals(target)) {
            //pick tile out of four children containing target tile as next
            ElevationTile next = current.getChild(target);

            if (VERBOSE) {
                System.out.println("> Building " + next);
            }

            //try to acquire existing tile from definition next has. In case that
            //tile already exists, it may have data from previous runs in some
            //of its parts and those should be preserved. Depth needs to be compared
            //since fetch tile call gets closest available and not tile on the 
            //exact coordinates
            ElevationTile existing = fetchTile(next.idxLatitude, next.idxLongitude, next.depth);
            if (existing != null && next.depth == existing.depth) {
                next = existing;
                if (VERBOSE) {
                    System.out.println("> Source replaced " + next);
                }
            } else {
                //copy data from current tile to next, as next is completely empty
                //tile and data from current is the closest available stuff
                next.setData(current);
            }

            if (DEBUG) {
                File file = getTileFile(next.idxLatitude, next.idxLongitude, next.depth);
                if (VERBOSE) {
                    System.out.println("Storing debug file " + file.getAbsolutePath());
                }
                next.storeDebugImage(file, 1, 24);
            }

            //copy input data to next (override)
            next.setData(target);

            //save next in folder structure
            next.store(getTileFile(next.idxLatitude, next.idxLongitude, next.depth), 24);

            if (DEBUG) {
                File file = getTileFile(next.idxLatitude, next.idxLongitude, next.depth);
                ;
                if (VERBOSE) {
                    System.out.println("Storing debug file " + file.getAbsolutePath());
                }
                next.storeDebugImage(file, 1, 24);
            }

            //set next as current
            current = next;
        }
        if (VERBOSE) {
            System.out.println("> Construction done\n");
        }
    }

    /**
     * This class serves as key for altitude value for cache lookup. It encapsulates
     * latitude and longitude in degrees. This key makes sense only on guaranteed
     * repeated requests, as full double precision is compared.
     */
    private final class PositionKey {
        /**
         * Latitude in degrees
         */
        private final double latitude;

        /**
         * Longitude in degrees
         */
        private final double longitude;

        /**
         * Pre-calculated hash
         */
        private final int hash;

        /**
         * Will create key for given latlong and pre-calculate hash.
         *
         * @param latitude  in degrees
         * @param longitude in degrees
         */
        public PositionKey(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;

            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(latitude);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(longitude);
            result = prime * result + (int) (temp ^ (temp >>> 32));

            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null) {
                return false;
            }
            if (this.getClass() != that.getClass()) {
                return false;
            }
            double thatLatitude = ((PositionKey) that).latitude;
            if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(thatLatitude)) {
                return false;
            }
            double thatLongitude = ((PositionKey) that).longitude;
            if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(thatLongitude)) {
                return false;
            }
            return true;
        }
    }
}
