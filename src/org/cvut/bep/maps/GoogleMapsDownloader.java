package org.cvut.bep.maps;

import aglobe.util.Logger;
import aglobex.simulation.global.GpsTools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * This class serves for fetching and processing of images fetched from google
 * maps static API.<br>
 * This class is NOT thread safe!<br>
 *
 * <h4>Anonymous limits</h4>
 * 1000 requests per day per IP<br>
 * 50 requests per minute per IP<br>
 * 3 requests per second<br>
 *
 * <h4>API key binding limits</h4>
 * 25 000 requests per day per IP<br>
 * 5 requests per second<br>
 *
 * @author Kaiser Vojtech
 * @author Blovsky Tomas
 */
public class GoogleMapsDownloader {
//    public static void main(String[] args) throws Exception{
//        //whole prague airport + surroundings
//        /*GoogleMapsDownloader.storeMaps(50.118635, 14.216575, 50.085602, 14.292707,
//                16, 16, 19, 12, "./work/datasets/GOOGLE_prague_in");*/
//        GoogleMapsDownloader.storeMaps(50.69308, 14.74416, 50.53656, 14.96543, 2, 2, 14, 14, "./work/datasets/GOOGLE_forest_in");
//    }
//
//    /**
//     * Enum for allowed types in google maps requests
//     */
//	public static enum MapType{
//	    /**
//	     * Specifies a standard roadmap image, as is normally shown on the
//	     * Google Maps website.
//	     */
//	    ROADMAP("roadmap"),
//	    /**
//	     * Specifies a satellite image.
//	     */
//	    SATELLITE("satellite"),
//	    /**
//	     * Specifies a hybrid of the satellite and roadmap image, showing a
//	     * transparent layer of major streets and place names on the satellite
//	     * image.
//	     */
//	    HYBRID("hybrid"),
//	    /**
//	     * Specifies a physical relief map image, showing terrain and vegetation.
//	     */
//	    TERRAIN("terrain");
//	    /**
//	     * Name that should be used in URL request
//	     */
//	    public final String name;
//	    MapType(String name){
//	        this.name=name;
//	    }
//	};
//
//	/**
//	 * Timestamp of last fetch of a map from google maps static API
//	 */
//	private static long lastFetch = 0;
//
//	/**
//	 * Debug flag enabling writing of progress in temporary files.
//	 */
//	private final static boolean DEBUG = false;
//
//	/**
//	 * Debug flag that enables prints of current progress
//	 */
//	private final static boolean VERBOSE = true;
//
//	/**
//	 * Folder in which resides cache folder structure.
//	 */
//	private final static String CACHE_FOLDER = "D:/WORK/cache/";
//
//	/**
//	 * How much will initial area of a map expand with every level in each direction.
//	 * Resulting area will be thus (width+width*2*g)*(height+height*2*g).<br>
//	 * growth lesser or equal 0.2 will result in decrease of surface of each iteration<br>
//	 * growth bigger than 0.2 will result in increase of surface of each iteration
//	 */
//	private final static double MAP_AREA_GROWTH = 0.5;
//
//	/**
//	 * Time offset between fetch calls to google servers in milliseconds
//	 */
//	private final static long FETCH_OFFSET = 400;
//
//	/**
//	 * Timeout after which is map fetch restarted in method
//	 * {@link GoogleMapsDownloader#getMap(double, double, double, double, int, double, double, int, MapType)}
//	 */
//	private final static long FETCH_TIMEOUT = 2000;
//
//	/**
//	 * API key binding request to an account and reducing the limits. Leave
//	 * <code>null</code> if no account binding should be done.
//	 */
//	private final static String API_KEY = null;
//
//	/**
//	 * Extension of images used in cache
//	 */
//	private final static String CACHE_FILES_EXTENSION = "png";
//
//	/**
//	 * Will return how many meters there is on mercator projection map per pixel
//	 * at given degree of latitude
//	 * @param latitude in degrees to be considered
//	 * @param zoomLevel to be considered
//	 * @return meters per pixel
//	 */
//	private static double getLongitudeMetersPerPixel(double latitude, int zoomLevel, int mapWidth) {
//	    return (Math.cos(latitude*(Math.PI / 180)) * 2 * Math.PI * 6378137)
//	            / (mapWidth * Math.pow(2, zoomLevel));
//	}
//
//	/**
//	 * Will calculate distance between two given colors by their respective components.
//	 * @param color1
//	 * @param color2
//	 * @return positive distance of components
//	 */
//	private static int getDistance(int color1, int color2) {
//	    int c1R = ((color1>>16)&255);
//        int c1G = ((color1>>8)&255);
//        int c1B = (color1&255);
//
//        int c2R = ((color2>>16)&255);
//        int c2G = ((color2>>8)&255);
//        int c2B = (color2&255);
//
//        int additionR = Math.abs(c1R-c2R);
//        int additionG = Math.abs(c1G-c2G);
//        int additionB = Math.abs(c1B-c2B);
//
//        return additionR+additionG+additionB;
//	}
//
//    /**
//     * Will take given chunk image and find best overlay of given addition over it.
//     * new image is then created and addition is painted with found offset in chunk
//     * image. This merge is done horizontally to the right, which means addition
//     * is vertically checked for matching column.
//     * @param chunk to have addition panted over and be expanded. May be <code>null</code>
//     * and in that case is returned addition instance as resulting image
//     * @param addition to be positioned over chunk
//     * @return merged images as new image instance
//     */
//	private static BufferedImage mergeRight(BufferedImage chunk, BufferedImage addition) {
//	    if(chunk==null) {
//	        return addition;
//	    }
//
//	    assert(chunk.getWidth()>2 && addition.getWidth()>2);
//
//	    long minValue = Long.MAX_VALUE;
//	    int minIndex = 0;
//
//	    //for every column in addition
//	    for(int x = 2;x<addition.getWidth()-2;x++) {
//	        long distance = 0;
//	        //for every pixel in column
//	        for(int y = 0;y<addition.getHeight();y++) {
//                distance+=getDistance(chunk.getRGB(chunk.getWidth()-5, y), addition.getRGB(x-2, y));
//                distance+=getDistance(chunk.getRGB(chunk.getWidth()-4, y), addition.getRGB(x-1, y));
//                distance+=getDistance(chunk.getRGB(chunk.getWidth()-3, y), addition.getRGB(x, y));
//                distance+=getDistance(chunk.getRGB(chunk.getWidth()-2, y), addition.getRGB(x+1, y));
//                distance+=getDistance(chunk.getRGB(chunk.getWidth()-1, y), addition.getRGB(x+2, y));
//                if(distance>minValue) {
//                    break;
//                }
//	        }
//	        if(distance<minValue) {
//	            minValue = distance;
//	            minIndex = x;
//	        }
//	    }
//
//	    //do the merging
//	    BufferedImage retVal = new BufferedImage(chunk.getWidth()+(addition.getWidth()-2-minIndex),
//	            chunk.getHeight(),BufferedImage.TYPE_INT_ARGB);
//        Graphics g = retVal.getGraphics();
//        g.drawImage(chunk, 0, 0, null);
//        g.drawImage(addition, chunk.getWidth()-2-minIndex, 0, null);
//	    g.dispose();
//
//	    return retVal;
//	}
//
//	/**
//	 * Will take given chunk image and find best overlay of given addition over it.
//	 * new image is then created and addition is painted with found offset in chunk
//	 * image. This merge is done vertically downwards, which means addition is
//	 * horizontally checked for matching row.
//	 * @param chunk to have addition panted over and be expanded. May be <code>null</code>
//	 * and in that case is returned addition instance as resulting image
//	 * @param addition to be positioned over chunk
//	 * @return merged images as new image instance
//	 */
//	private static BufferedImage mergeDown(BufferedImage chunk, BufferedImage addition) {
//	    if(chunk==null) {
//	        return addition;
//	    }
//
//        long minValue = Long.MAX_VALUE;
//        int minIndex = 0;
//
//        //for every row in addition
//        for(int y = 2;y<Math.min(addition.getHeight(), chunk.getHeight())-2;y++) {
//            long distance = 0;
//            //for every pixel in row
//            for(int x = 0;x<Math.min(addition.getWidth(), chunk.getWidth());x++) {
//                distance+=getDistance(chunk.getRGB(x, chunk.getHeight()-5), addition.getRGB(x, y-2));
//                distance+=getDistance(chunk.getRGB(x, chunk.getHeight()-4), addition.getRGB(x, y-1));
//                distance+=getDistance(chunk.getRGB(x, chunk.getHeight()-3), addition.getRGB(x, y));
//                distance+=getDistance(chunk.getRGB(x, chunk.getHeight()-2), addition.getRGB(x, y+1));
//                distance+=getDistance(chunk.getRGB(x, chunk.getHeight()-1), addition.getRGB(x, y+2));
//                if(distance>minValue) {
//                    break;
//                }
//            }
//            if(distance<minValue) {
//                minValue = distance;
//                minIndex = y;
//            }
//        }
//
//        //do the merge
//        BufferedImage retVal = new BufferedImage(chunk.getWidth(),
//                chunk.getHeight()+(addition.getHeight()-2-minIndex),BufferedImage.TYPE_INT_ARGB);
//        Graphics g = retVal.getGraphics();
//        g.drawImage(chunk, 0, 0, null);
//        g.drawImage(addition, 0, chunk.getHeight()-2-minIndex, null);
//        g.dispose();
//
//        return retVal;
//	}
//
//	/**
//	 * Will fetch tiles for map in given latlong bounds and connect them into single
//	 * map tile.
//	 * @param startLatitude in degrees
//	 * @param startLongitude in degrees
//	 * @param endLatitude in degrees
//	 * @param endLongitude in degrees
//	 * @param workingTileSize tile size no bigger than 320
//	 * @param zoomLevel in range between 1 and 20
//	 * @param mapType
//	 * @return collected tiles
//	 * @throws Exception
//	 */
//	public static BufferedImage getMap(
//	        double startLatitude, double startLongitude, double endLatitude, double endLongitude,
//	        int workingTileSize, int zoomLevel, MapType mapType) throws Exception {
//
//	    //turn longitude values into positive ones (iterating positive modulo)
//	    startLongitude = (startLongitude<0)?startLongitude+360:startLongitude;
//	    endLongitude = (endLongitude<0)?endLongitude+360:endLongitude;
//
//	    //verify start latitude is larger than end latitude (iterating downwards)
//	    if(startLatitude<endLatitude) {
//	        double tmp = endLatitude;
//	        endLatitude = startLatitude;
//	        startLatitude = tmp;
//	    }
//
//	    //find out correct step based on latitude
//	    //approximation for midpoint of target area
//	    double midpoint = GpsTools.midpoint(startLatitude, startLongitude, endLatitude, endLongitude).x;
//	    //calculate pixel density for lat and long
//        double metersPerPixelLat = getLongitudeMetersPerPixel(0, zoomLevel,640);
//        double metersPerPixelLong = getLongitudeMetersPerPixel(midpoint, zoomLevel,320);
//        //calculate scale in given latitude
//        double metersPerDegreeLat = GpsTools.getMetersPerDegreeOfLatitude(midpoint);
//        double metersPerDegreeLong = GpsTools.getMetersPerDegreeOfLongitude(midpoint);
//        //calculate ideal step approximation, a bit decreased to guarantee good overlap
//        double bestStepLat = ((320*metersPerPixelLat)/metersPerDegreeLat);
//        double bestStepLong = ((320*metersPerPixelLong)/metersPerDegreeLong);
//        //fit calculated ideal step on requested size so it is divisible
//        double latitudeStep = Math.abs(endLatitude-startLatitude)
//                /Math.ceil(Math.abs(endLatitude-startLatitude)/bestStepLat);
//        double longitudeStep = Math.abs(endLongitude-startLongitude)
//                /Math.ceil(Math.abs(endLongitude-startLongitude)/bestStepLong);
//	    //calculate number of columns and rows
//        int latitudeStepCount = (int)Math.round(Math.abs(endLatitude-startLatitude)/latitudeStep);
//        int longitudeStepCount = (int)Math.round(Math.abs(endLongitude-startLongitude)/longitudeStep);
//
//	    BufferedImage full = null;
//
//	    //progressing stripes at different latitudes
//	    for(double latitude = startLatitude, y = 0;
//	            y<=latitudeStepCount;
//	            latitude-=latitudeStep, y++) {
//	        //stripe being collected
//	        BufferedImage horizontalStripe = null;
//
//	        //progressing stripes at different
//	        for(double longitude = startLongitude, x = 0;
//	                x<=longitudeStepCount;
//	                longitude=(longitude+longitudeStep)%(360), x++) {
//	            BufferedImage part = null;
//                //start executor with two threads - one for getMap itself and
//                //one for scheduled timeout cancel
//                ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
//                do {
//                    //submit get map task for immediate execution
//                    Future<BufferedImage> handler = executor.submit(new GetMapTask(latitude,
//                            longitude, workingTileSize*2, workingTileSize*2, zoomLevel, mapType));
//                    //schedule cancel of the task after timeout
//                    executor.schedule(new Runnable(){
//                        @Override
//                        public void run(){
//                            handler.cancel(true);
//                        }
//                    }, FETCH_TIMEOUT, TimeUnit.MILLISECONDS);
//
//                    //await result of map get - is either filled with image
//                    //or null in case task was canceled
//                    try {
//                        part = handler.get();
//                    } catch(CancellationException e) {
//                        Logger.logWarning("Image fetch timeout!");
//                    } catch(ExecutionException e) {
//                        if(e.getCause()!=null && (e.getCause() instanceof LimitExceededException)) {
//                            throw (LimitExceededException)e.getCause();
//                        }
//                        Logger.logSevere("Fetch failed "+e.getMessage()+" (cause: "+e.getCause()+")");
//                    }
//
//                    //try until a request gets through
//                }while(part==null);
//                executor.shutdownNow();
//
//                if(DEBUG) {
//                    FileOutputStream fos = new FileOutputStream(new File("./work/partRaw.png"));
//                    ImageIO.write(part, "png", fos);
//                    fos.close();
//                }
//
//                //add image to the rest
//	            part = part.getSubimage(0, 0, workingTileSize, workingTileSize);
//
//	            if(DEBUG) {
//                    FileOutputStream fos = new FileOutputStream(new File("./work/partCrop.png"));
//                    ImageIO.write(part, "png", fos);
//                    fos.close();
//                }
//
//	            horizontalStripe = mergeRight(horizontalStripe, part);
//
//	            if(DEBUG) {
//	                FileOutputStream fos = new FileOutputStream(new File("./work/stripeProgress.png"));
//	                ImageIO.write(horizontalStripe, "png", fos);
//	                fos.close();
//	            }
//	        }
//
//	        full = mergeDown(full, horizontalStripe);
//
//	        if(DEBUG) {
//	            FileOutputStream fos = new FileOutputStream(new File("./work/fullProgress.png"));
//	            ImageIO.write(full, "png", fos);
//	            fos.close();
//	        }
//	    }
//
//	    //return result without top and left stripe of width of workingTileSize
//	    //since desired start latitude is it right bottom corner of starting tile
//	    return full.getSubimage(workingTileSize, workingTileSize,
//	            full.getWidth()-workingTileSize, full.getHeight()-workingTileSize);
//	}
//
//	/**
//	 * Will return path to cache for map of given parameters.
//	 * @param centerLatitude in degrees
//	 * @param centerLongitude in degrees
//	 * @param imageWidth in pixels
//	 * @param imageHeight in pixels
//     * @param zoomLevel in range from 1 to 20
//	 * @param mapType
//	 * @return string filename representation
//	 */
//	private static String getCacheFilename(double centerLatitude, double centerLongitude,
//	        int imageWidth, int imageHeight, int zoomLevel, MapType mapType) {
//	    return CACHE_FOLDER+"/"+zoomLevel+"/map_"+centerLatitude+"_"+centerLongitude+"_"+zoomLevel+"_"+
//	        imageWidth+"_"+imageHeight+"_"+mapType.name+"."+CACHE_FILES_EXTENSION;
//	}
//
//	/**
//	 * Will check whether map of given parameters is in cache.
//     * @param centerLatitude in degrees
//     * @param centerLongitude in degrees
//     * @param imageWidth in pixels
//     * @param imageHeight in pixels
//     * @param zoomLevel in rage from 1 to 20
//     * @param mapType
//	 * @return true if map with given parameters is in cache
//	 */
//	private static boolean isCached(double centerLatitude, double centerLongitude,
//	        int imageWidth, int imageHeight, int zoomLevel, MapType mapType) {
//	    return new File(getCacheFilename(centerLatitude, centerLongitude, imageWidth,
//	            imageHeight, zoomLevel, mapType)).exists();
//	}
//
//	/**
//	 * Will save given image in cache under name constructed from given parameters.
//	 * <br>
//	 * NOTE: Directories leading to cache file are created by this function.
//	 * @param image to be stored
//	 * @param centerLatitude in degrees
//	 * @param centerLongitude in degrees
//	 * @param imageWidth in pixels
//	 * @param imageHeight in pixels
//     * @param zoomLevel in rage from 1 to 20
//	 * @param mapType
//	 */
//	private static void saveToCache(BufferedImage image, double centerLatitude,
//	        double centerLongitude, int imageWidth, int imageHeight, int zoomLevel, MapType mapType) {
//	    String filename = getCacheFilename(centerLatitude, centerLongitude,
//	            imageWidth, imageHeight, zoomLevel, mapType);
//	    File file = new File(filename);
//	    file.getParentFile().mkdirs();
//	    try {
//            ImageIO.write(image, CACHE_FILES_EXTENSION, file);
//        } catch (IOException e) {
//            Logger.logSevere("Failed to cache map image! ("+filename+")");
//        }
//	}
//
//	/**
//	 * Will load file from cache.
//	 * @param centerLatitude in degrees
//	 * @param centerLongitude in degrees
//	 * @param zoomLevel in rage from 1 to 20
//	 * @param imageWidth in pixels
//	 * @param imageHeight in pixels
//	 * @param mapType
//	 * @return loaded image or <code>null</code> in case it was not found or load
//	 * failed for reasons logged.
//	 */
//	private static BufferedImage loadFromCache(double centerLatitude, double centerLongitude,
//	        int imageWidth, int imageHeight, int zoomLevel, MapType mapType) {
//        //check existence of the file
//	    String filename = getCacheFilename(centerLatitude, centerLongitude, imageWidth,
//	            imageHeight, zoomLevel, mapType);
//	    File file = new File(filename);
//	    if(!file.exists()) {
//	        return null;
//	    }
//
//	    //attempt the load itself
//	    try {
//	        return ImageIO.read(file);
//	    }catch(Exception e) {
//	        Logger.logSevere("Failed to read the cache! ("+filename+")");
//	    }
//	    return null;
//	}
//
//	/**
//	 * Will try to load current request count for today from appropriate file.
//	 * If such file is not found, zero is returned.
//	 * @return count of request sent to google this day
//	 */
//	public static int requestCount() {
//	    //construct name of information file
//        String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
//        File file = new File("./work/limit_image_"+date+(API_KEY!=null?"_"+API_KEY:"")+".txt");
//
//        //load previous value
//        int count = 0;
//        if(file.exists()) {
//            try {
//                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
//                count = ois.readInt();
//                ois.close();
//            } catch (IOException e) {
//                Logger.logSevere("Failed to retrieve current request count!");
//            };
//        }
//
//        return count;
//	}
//
//	/**
//	 * Will check count of map requests for the day and compare it to limit
//	 * related the fact whether there is API KEY set.
//	 * @return true if limit was exceeded, otherwise false
//	 */
//	private static boolean checkAndIncrementRequestCount() {
//	    //construct name of information file
//	    String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
//	    File file = new File("./work/limit_image_"+date+(API_KEY!=null?"_"+API_KEY:"")+".dat");
//
//	    //load previous value
//	    int current = 0;
//	    if(file.exists()) {
//	        try {
//                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
//                current = ois.readInt();
//                ois.close();
//            } catch (IOException e) {
//                Logger.logSevere("Failed to retrieve current request count!");
//            };
//	    }
//
//	    //write new value
//	    try {
//	        file.createNewFile();
//            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
//            oos.writeInt(current+1);
//            oos.close();
//        } catch (FileNotFoundException e) {
//            Logger.logSevere("Failed to create request counter file!");
//        } catch (IOException e) {
//            Logger.logSevere("Failed to write request counter file!");
//        }
//
//
//	    //check the limit
//	    if(API_KEY!=null) {
//	        //limit is 25000
//	        if(current==20000) {
//	            Logger.logWarning("You are getting close to the limit of map "
//	                    + "requests today! ("+(current+1)+"/25000)");
//	        }else if(current>=25000) {
//	            Logger.logSevere("You have exceeded the limit of requests for today!");
//	            return true;
//	        }
//	    }else {
//	        //limit is 1000
//            if(current==800) {
//                Logger.logWarning("You are getting close to the limit of map "
//                        + "requests today! ("+(current+1)+"/1000)");
//            }else if(current>=1000) {
//                Logger.logSevere("You have exceeded the limit of requests for today!");
//                return true;
//            }
//	    }
//
//	    return false;
//	}
//
//	/**
//	 * Will construct URL to image specified by given parameters. API key from
//	 * static constant is added to the URL (if it is set there).
//	 * @param centerLatitude in degrees
//	 * @param centerLongitude in degrees
//	 * @param imageWidth in pixels
//	 * @param imageHeight in pixels
//	 * @param zoomLevel in range from 1 to 20
//	 * @param mapType
//	 * @return URL instance
//	 * @throws MalformedURLException
//	 */
//	private static URL getURL(double centerLatitude, double centerLongitude,
//            int imageWidth, int imageHeight, int zoomLevel, MapType mapType) throws MalformedURLException {
//
//	    String url = "http://maps.google.com/maps/api/staticmap";
//        url += "?zoom="+zoomLevel;
//        url += "&size=" + imageWidth + "x" + imageHeight;
//        url += "&maptype="+mapType.name;
//        //default is set to 1. This scale means how will be original picture
//        //scaled with same amount of information. Another acceptable value is 2,
//        //witch will make the image twice as big.
//        url += "&scale=1";
//        //string format is used to prevent scientific notations of exponents which
//        //are not exactly edible for google maps
//        url += "&center=" + String.format("%f,%f", centerLatitude, centerLongitude);
//        //format of returned image to shift between bandwidth and image quality.
//        //default png(png8) seems to be sufficient, but png32 is available in case
//        //of need
//        url += "&format=png";
//        //not required in new version of the API
//        url += "&sensor=true";
//        if(API_KEY!=null) {
//            url += "&key="+API_KEY;
//        }
//
//        return new URL(url);
//	}
//
//	/**
//	 * Will return false if half and more of checked pixels correspond to color
//	 * of google image saying there is no image for requested map.
//	 * @param img to be checked
//	 * @return true if map seems to contain actual map image
//	 */
//	public static boolean isValidMap(BufferedImage img) {
//	    final int color = new Color(228,226,222).getRGB();
//	    int count = 0;
//	    for(int x = 0;x<img.getWidth();x+=10) {
//	        for (int y = 0; y < img.getHeight(); y+=10) {
//                if(img.getRGB(x, y)==color) {
//                    count++;
//                }
//            }
//	    }
//	    return count<((img.getWidth()/10)*(img.getHeight()/10))/2;
//
//	}
//
//	/**
//	 * Will fetch map image around given center. The fetching itself may be done
//	 * from cache of previously loaded images instead of loading from google
//	 * servers to avoid unnecessary use of daily limit.
//	 * @param centerLatitude in degrees
//	 * @param centerLongitude in degrees
//	 * @param imageWidth in pixels no bigger than 640
//	 * @param imageHeight in pixels no bigger than 640
//	 * @param zoomLevel in range 1(whole earth) to 20(maximum detail)
//	 * @param mapType specifying image content
//	 * @return loaded image or <code>null</code> in case both load from cache and
//	 * load from google server failed
//	 * @throws Exception
//	 */
//	public static BufferedImage getMap(double centerLatitude, double centerLongitude,
//	        int imageWidth, int imageHeight, int zoomLevel, MapType mapType) throws Exception{
//	    centerLongitude = (centerLongitude>180)?centerLongitude-360:centerLongitude;
//
//	    //try loading from cache of previously loaded maps
//	    BufferedImage img = null;
//	    if(isCached(centerLatitude, centerLongitude, imageWidth, imageHeight, zoomLevel, mapType)) {
//	        img = loadFromCache(centerLatitude, centerLongitude, imageWidth, imageHeight, zoomLevel, mapType);
//	        if(img!=null) {
//	            if(VERBOSE) {
//	                System.out.print(".");
//	            }
//	            return img;
//	        }
//	    }
//
//	    //check daily limit
//	    if(checkAndIncrementRequestCount()) {
//	        throw new LimitExceededException();
//	    }
//
//	    //check time offset from last fetch and possibly wait before executing
//	    //this fetch to avoid block by google server
//	    if(lastFetch+FETCH_OFFSET>System.currentTimeMillis()) {
//            try {
//                Thread.sleep((lastFetch+FETCH_OFFSET)-System.currentTimeMillis());
//            }catch (InterruptedException e) {}
//	    }
//        lastFetch = System.currentTimeMillis();
//
//	    //fetch from the google server
//	    URL url = getURL(centerLatitude, centerLongitude, imageWidth, imageHeight, zoomLevel, mapType);
//
//	    if(DEBUG) {
//	        System.out.println("\n"+url);
//	    }
//	    if(VERBOSE) {
//	        System.out.print("!");
//	    }
//
//	    try {
//	        img = ImageIO.read(url);
//	    }catch(IOException e) {
//	        throw new Exception("Rejected while fetching map! "+e.getMessage()+"\n"+url.toString());
//	    }
//
//	    if(!isValidMap(img)){
//	        throw new MissingImageryException(centerLatitude,centerLongitude,zoomLevel);
//	    }
//
//	    //if fetch did not fail(faulty URL, limit exceeded, ...) store result in cache
//	    if(img!=null) {
//	        saveToCache(img, centerLatitude, centerLongitude, imageWidth, imageHeight, zoomLevel, mapType);
//	    }
//
//	    return img;
//	}
//
//	/**
//	 * Will store maps divided into given number of tiles that cover are defined
//	 * by given latlong boundary.
//     * @param startLatitude in degrees
//     * @param startLongitude in degrees
//     * @param endLatitude in degrees
//     * @param endLongitude in degrees
//	 * @param latitudeTileCount
//	 * @param longitudeTileCount
//	 * @param zoomLevel
//	 * @param destinationPath to have tiles stored
//	 * @throws LimitExceededException in case limit was exceeded
//	 */
//	public static void storeMaps(
//	        double startLatitude, double startLongitude,
//	        double endLatitude, double endLongitude,
//	        int latitudeTileCount, int longitudeTileCount,
//	        int zoomLevel, String destinationPath) throws LimitExceededException {
//	    //turn longitude values into positive ones (iterating positive modulo)
//        startLongitude = (startLongitude<0)?startLongitude+360:startLongitude;
//        endLongitude = (endLongitude<0)?endLongitude+360:endLongitude;
//
//        //verify start latitude is larger than end latitude (iterating downwards)
//        if(startLatitude<endLatitude) {
//            double tmp = endLatitude;
//            endLatitude = startLatitude;
//            startLatitude = tmp;
//        }
//
//        //build directory structure for requested destination
//        new File(destinationPath).mkdirs();
//
//        //calculate steps by number of tiles requested
//        double latitudeStep = Math.abs(startLatitude-endLatitude)/latitudeTileCount;
//        double longitudeStep = (endLongitude-startLongitude+(startLongitude>endLongitude?360:0))/longitudeTileCount;
//
//        for(double latitude = startLatitude, y = 0;
//                y<latitudeTileCount;
//                latitude-=latitudeStep,y++) {
//
//            for(double longitude = startLongitude, x = 0;
//                    x<longitudeTileCount;
//                    longitude+=longitudeStep, x++) {
//                try {
//                    if(VERBOSE) {
//                        System.out.println("Starting "+x+" "+y);
//                    }
//
//                    //skip existing files as if they are fine
//                    if(new File(String.format(destinationPath+"/map_%.0f_%.0f.png", y, x)).exists()) {
//                        continue;
//                    }
//
//                    //construct tile
//                    BufferedImage result = GoogleMapsDownloader.getMap(
//                            latitude,
//                            longitude,
//                            latitude-latitudeStep,
//                            longitude+longitudeStep,
//                            320,
//                            zoomLevel,
//                            GoogleMapsDownloader.MapType.SATELLITE);
//
//                    if(VERBOSE) {
//                        System.out.println(" Storing\n");
//                    }
//
//                    //write image file
//                    ImageIO.write(result, "png", new File(
//                            String.format(destinationPath+"/map_%.0f_%.0f.png", y, x)));
//
//                    //write file with latlong bounds
//                    //image name, long min, long max, lat min, lat max
//                    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
//                            String.format(destinationPath+"/map_%.0f_%.0f.txt", y, x))));
//                    bw.write(String.format("map_%.0f_%.0f.png", y, x)+"\n");
//                    bw.write(longitude+"\n");
//                    bw.write(longitude+longitudeStep+"\n");
//                    bw.write(latitude-latitudeStep+"\n");
//                    bw.write(latitude+"\n");
//                    bw.close();
//                } catch (MissingImageryException e) {
//                    //images are not available, skip this tile
//                    System.err.println(e.getMessage());
//                    continue;
//                } catch (LimitExceededException e) {
//                    throw e;
//                } catch (Exception e) {
//                    //something else went wrong, make full report
//                    e.printStackTrace();
//                    return;
//                }
//            }
//        }
//	}
//
//	/**
//	 * Will call
//	 * {@link GoogleMapsDownloader#storeMaps(double, double, double, double, int, int, int, int, String)}
//	 * with zoom levels in given range and surface increasing with each coarser
//	 * level.
//     * @param startLatitude in degrees
//     * @param startLongitude in degrees
//     * @param endLatitude in degrees
//     * @param endLongitude in degrees
//	 * @param latitudeTileCount that is on first processed level and then increased
//	 * with every level
//	 * @param longitudeTileCount that is on first processed level and then increased
//     * with every level
//	 * @param startZoomLevel
//	 * @param endZoomLevel
//	 * @param destinationPath that will be suffixed with zoom level
//	 */
//	public static void storeMaps(double startLatitude, double startLongitude,
//            double endLatitude, double endLongitude,
//            double latitudeTileCount, double longitudeTileCount,
//            int startZoomLevel, int endZoomLevel, String destinationPath) {
//	    try {
//    	    //verify start is bigger than end (starting from more precise)
//    	    if(startZoomLevel<endZoomLevel) {
//    	        int tmp = startZoomLevel;
//    	        startZoomLevel = endZoomLevel;
//    	        endZoomLevel = tmp;
//    	    }
//
//    	    //verify start latitude is larger than end latitude (iterating downwards)
//            if(startLatitude<endLatitude) {
//                double tmp = endLatitude;
//                endLatitude = startLatitude;
//                startLatitude = tmp;
//            }
//
//            if(VERBOSE) {
//                System.out.println("----------------------------------------------------");
//                System.out.println("Downloading area around ("+startLatitude+", "+startLongitude+") "
//                        + "-> ("+endLatitude+", "+endLongitude+")");
//                System.out.println("Tile counts "+longitudeTileCount+"|"+latitudeTileCount);
//                System.out.println("Zoom level "+startZoomLevel);
//            }
//
//            //always download start without edits
//            GoogleMapsDownloader.storeMaps(startLatitude, startLongitude, endLatitude,
//                    endLongitude, (int)latitudeTileCount, (int)longitudeTileCount, startZoomLevel,
//                    destinationPath+"/"+startZoomLevel+"/");
//
//
//            double increase = MAP_AREA_GROWTH;
//            latitudeTileCount=(int)Math.max(1, Math.ceil(latitudeTileCount*(1+increase*2))/2.0);
//            longitudeTileCount=(int)Math.max(1, Math.ceil(longitudeTileCount*(1+increase*2))/2.0);
//
//            for(int zoomLevel = startZoomLevel-1;zoomLevel>=endZoomLevel;zoomLevel--) {
//                //tile counts for particular sides
//                int longAB = (int)Math.ceil(longitudeTileCount/2.0);
//                int shortAB = (int)Math.max(1, Math.ceil(latitudeTileCount*increase/2.0));
//                int shortCD = (int)Math.max(1, Math.ceil(longitudeTileCount*increase/2.0));
//                int longCD = (int)Math.ceil(latitudeTileCount*(1+increase*2)/2.0);
//
//                //tile counts for overall frame
//                latitudeTileCount=(int)Math.max(1, Math.ceil(latitudeTileCount*(1+increase*2))/2.0);
//                longitudeTileCount=(int)Math.max(1, Math.ceil(longitudeTileCount*(1+increase*2))/2.0);
//
//                if(VERBOSE) {
//                    System.out.println("----------------------------------------------------");
//                    System.out.println("Downloading area around ("+startLatitude+", "+startLongitude+") "
//                            + "-> ("+endLatitude+", "+endLongitude+")");
//                    System.out.println("Tile counts "+longitudeTileCount+"|"+latitudeTileCount);
//                    System.out.println("Zoom level "+zoomLevel);
//                }
//
//                //calculate difference for bigger rectangle
//                double diffLat = Math.abs(startLatitude-endLatitude)*increase;
//                double diffLong = (startLongitude>endLongitude?
//                        (360-startLongitude+endLongitude):(endLongitude-startLongitude))*increase;
//
//                //take tile map pictures according to:
//                //     |   |   a   |   |
//                //     |   ---------   |
//                //     |   |       |   |
//                //     | c |       | d |
//                //     |   |       |   |
//                //     |   ---------   |
//                //     |   |   b   |   |
//                //this will save portion of downloaded images depending on size of
//                //MAP_AREA_GROWTH
//                if(VERBOSE) {
//                    System.out.println("-----------------------");
//                    System.out.println("starting a ("+shortAB+"x"+longAB+")");
//                }
//                GoogleMapsDownloader.storeMaps(startLatitude+diffLat, startLongitude, startLatitude,
//                        endLongitude, shortAB, longAB, zoomLevel,
//                        destinationPath+"/"+zoomLevel+"a/");
//
//                if(VERBOSE) {
//                    System.out.println("-----------------------");
//                    System.out.println("starting b ("+shortAB+"x"+longAB+")");
//                }
//                GoogleMapsDownloader.storeMaps(endLatitude, startLongitude, endLatitude-diffLat,
//                        endLongitude, shortAB, longAB, zoomLevel,
//                        destinationPath+"/"+zoomLevel+"b/");
//
//                if(VERBOSE) {
//                    System.out.println("-----------------------");
//                    System.out.println("starting c ("+longCD+"x"+shortCD+")");
//                }
//                GoogleMapsDownloader.storeMaps(startLatitude+diffLat,
//                        startLongitude-diffLong+(startLongitude-diffLong<0?360:0),
//                        endLatitude-diffLat, startLongitude, longCD, shortCD, zoomLevel,
//                        destinationPath+"/"+zoomLevel+"c/");
//
//                if(VERBOSE) {
//                    System.out.println("-----------------------");
//                    System.out.println("starting d ("+longCD+"x"+shortCD+")");
//                }
//                GoogleMapsDownloader.storeMaps(startLatitude+diffLat, endLongitude, endLatitude-diffLat,
//                        (endLongitude+diffLong)%360, longCD, shortCD, zoomLevel,
//                        destinationPath+"/"+zoomLevel+"d/");
//
//                //enlarge target rectangle by differences
//                startLatitude+=diffLat;
//                endLatitude-=diffLat;
//                startLongitude=startLongitude-diffLong+(startLongitude-diffLong<0?360:0);
//                endLongitude=(endLongitude+diffLong)%360;
//            }
//	    }catch(LimitExceededException e) {
//	        Logger.logSevere("Google maps downloader quitting: "+e.getMessage());
//	        return;
//	    }
//	}
//
//	/**
//	 * This Exception is thrown when requested image is not available on google
//	 * maps API
//	 */
//	private static class MissingImageryException extends Exception{
//        private static final long serialVersionUID = 1L;
//        public MissingImageryException(double latitude, double longitude, int zoom) {
//	        super("Missing images at center ("+latitude+", "+longitude+") zoom "+zoom);
//	    }
//	}
//
//	/**
//	 * This Exception is thrown when limit for map requests for the day was exceeded.
//	 */
//	private static class LimitExceededException extends Exception{
//        private static final long serialVersionUID = 1L;
//        public LimitExceededException() {
//            super("Limit was exceeded!");
//        }
//    }
//
//    /**
//     * This class serves for application of timeout on map fetch. It just wraps
//     * content for call of
//     * {@link GoogleMapsDownloader#getMap(double, double, int, int, int, MapType)}
//     */
//    private static class GetMapTask implements Callable<BufferedImage>{
//        private final double centerLatitude;
//        private final double centerLongitude;
//        private final int imageWidth;
//        private final int imageHeight;
//        private final int zoomLevel;
//        private final MapType mapType;
//        public GetMapTask(double centerLatitude, double centerLongitude,
//                int imageWidth, int imageHeight, int zoomLevel, MapType mapType) {
//            this.centerLatitude = centerLatitude;
//            this.centerLongitude = centerLongitude;
//            this.imageWidth = imageWidth;
//            this.imageHeight = imageHeight;
//            this.zoomLevel = zoomLevel;
//            this.mapType = mapType;
//        }
//        @Override
//        public BufferedImage call() throws Exception {
//            return getMap(centerLatitude, centerLongitude, imageWidth, imageHeight, zoomLevel, mapType);
//        }
//    }
}
