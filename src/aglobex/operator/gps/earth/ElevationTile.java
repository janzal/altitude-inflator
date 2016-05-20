package aglobex.operator.gps.earth;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.imageio.ImageIO;

import com.google.common.io.Files;

/**
 * This class represents an elevation map tile according to quad tree indexing.
 * It contains left upper corner of the tile in latlong in degrees and stream
 * of elevation data.<br><br>
 * 512x512 tiles<br>
 * <h5>Data format of binary file:</h5>
 * Unsigned 16bit values, little-endian (LO-HI order)<br>
 * The first value read corresponds to the top left-hand corner of the tile
 * <br><br>
 * Value 32768 ... 0 meters (or undefined)<br>
 * Value 33768 ... +1000 meters<br>
 * Value 31768 ... -1000 meters<br>
 * etc.<br><br>
 * 
 * <h5>Data format for 24 bit RGB</h5>
 * Elevation value is first offset by 1000 meters to get above zero everywhere 
 * on land, then is the value multiplied by 100 to convert it to centimeters and
 * at last, it is shifted bitwise to to the left by four to give more significance
 * to blue color on small differences.<br>
 * From pixel to value {@code ((pixel>>4)/100)-1000}<br>
 * From value to pixel {@code ((value+1000)*100)<<4}<br><br>
 * 
 * <h5>Data format for 16 bit grayscale</h5>
 * Elevation value is simply offset by 1000 meters to get all values into 
 * positive numbers. All values are rounded to whole meters.<br><br>
 * 
 * <h5>Data format for 8 bit grayscale</h5>
 * Elevation value is first offset by 100 to get positive values and then fitted
 * proportionally to range 0-255 by division of the value by 10000 meters and then
 * multiplication by 255. 10000 is arbitrary constant based on sea level offset 
 * and highest land point on earth.
 * 
 * @author Kaiser Vojtech
 */
public class ElevationTile {
    /**
     * Latitude of left upper corner of this tile in degrees
     */
    private final double minLatitude;
    
    /**
     * Longitude of left upper corner of this tile in degrees
     */
    private final double minLongitude;

    /**
     * How many degrees correspond to one "pixel" in elevation map in this 
     * tile. 
     */
    private final double degreesPerPixel;
    
    /**
     * Index in quad tree along X axis (=latitude axis)
     */
    public final int idxLatitude;
    
    /**
     * Index in quad tree along Y axis (=longitude axis)
     */
    public final int idxLongitude;
    
    /**
     * Depth in the quad tree
     */
    public final byte depth;
    
    /**
     * Array of elevation values for this tile in meters.
     */
    private double[][] data = null;
    
    /**
     * Will create tile with no data. Any request for elevation from tile
     * created by this constructor will result in runtime exception.
     * @param idxLatitude in quad tree
     * @param idxLongitude in quad tree
     * @param depth in quad tree
     */
    public ElevationTile(int idxLatitude, int idxLongitude, byte depth) {
        this.idxLatitude = idxLatitude;
        this.idxLongitude = idxLongitude;
        this.depth = depth;
        
        //calculate boundaries
        double degreesPerTile = 180.0/(1<<depth);
        minLatitude = (idxLatitude+1)*degreesPerTile-90;
        minLongitude = idxLongitude*degreesPerTile-180;
        degreesPerPixel = degreesPerTile/512;
    }
    
    /**
     * Will create new tile with given data and position settings.
     * @param data to be stored
     * @param idxLatitude in quad tree
     * @param idxLongitude in quad tree
     * @param depth in quad tree
     */
    public ElevationTile(double[][] data, int idxLatitude, int idxLongitude, byte depth) {
        this(idxLatitude, idxLongitude, depth);
        this.setData(data);
    }
    
    /**
     * Will create tile of given indices containing data from given file tied 
     * to given depth. In case file is binary, USHORT byte scheme will be used,
     * otherwise image file of 24 bit depth will be expected.
     * @param file to be loaded
     * @param idxLatitude of the tile
     * @param idxLongitude of the tile
     * @param depth in quad tree
     * @throws IOException in case read of the file failed
     */
    public ElevationTile(File file, int idxLatitude, int idxLongitude, byte depth) throws IOException {
        this(idxLatitude, idxLongitude, depth);
        
        if(file.getName().endsWith("bin")) {
            //load the file into memory as binary USHORT 512x512
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            byte[] buffer = new byte[(int)raf.length()];
            raf.readFully(buffer);
            raf.close();
            
            data = new double[512][512];
            for(int y = 0;y<512;y++) {
                for(int x = 0;x<512;x++) {
                    final int offset = ((x + y * 512) * 2);
                    data[x][y] = ((Byte.toUnsignedInt(buffer[offset]) + 
                            (Byte.toUnsignedInt(buffer[offset+1]) << 8)) - 32768);
                }
            }
        } else {
            this.setDataFromImage(ImageIO.read(file), 0, 0, 512, 512, 24);
        } 
    }
    
    /**
     * Will retrieve an altitude at given latlong coordinates.
     * @param latitude of the point in degrees
     * @param longitude of the point in degrees
     * @return altitude in meters
     */
    public double getElevationM(double latitude, double longitude) {
        if(data==null) {
            throw new RuntimeException("Cannot look up elevation in empty tile!"
                    + " ("+idxLatitude+", "+idxLongitude+", "+depth+")");
        }
        
        //locate what "pixel" of the tile corresponds to given latlong
        final int x = (int) Math.max(0, Math.min(511, Math.round(Math.abs((longitude-minLongitude)/degreesPerPixel))));
        final int y = (int) Math.max(0, Math.min(511, Math.round(Math.abs((minLatitude-latitude)/degreesPerPixel))));
        
        return data[x][y];
    }
    
    /**
     * Will check whether given latlong is inside area covered by this tile.
     * @param latitude in degrees
     * @param longitude in degrees
     * @return true if given latlong is inside of this tile
     */
    public boolean contains(double latitude, double longitude) {
        final double x = (longitude-minLongitude)/degreesPerPixel;
        final double y = (minLatitude-latitude)/degreesPerPixel;
        return x>=0 && x<=511 && y>=0 && y<=511;
    }
    
    /**
     * Will store this tile in given file.
     * @param file to have the tile stored in
     * @param precision at which is the file stored
     * @throws IOException in case of errors
     */
    public void store(File file, int precision) throws IOException {
        if(data==null) {
            throw new RuntimeException("Cannot store data from empty tile! ("+
                    idxLatitude+", "+idxLongitude+", "+depth+")");
        }
        Files.createParentDirs(file);
        String filepath = file.getAbsolutePath();
        ImageIO.write(this.getDataAsImage(precision), filepath.substring(filepath.length()-3, 
                filepath.length()), file);
    }

    /**
     * Will store exaggerated elevation map as image in file on given path.
     * @param file reference in which will be the image stored
     * @param scale to which is each value exaggerated
     * @param precision at which should be debug file stored
     * @throws IOException in case of store fail
     */
    public void storeDebugImage(File file, double scale, int precision) throws IOException {
        ElevationTile tmp = new ElevationTile(idxLatitude, idxLongitude, depth);
        double[][] originalData = this.getData();
        double[][] scaledData = new double[512][512];
        for(int y = 0;y<512;y++) {
            for(int x = 0;x<512;x++) {
                scaledData[x][y] = originalData[x][y]*scale;
            }
        }
        tmp.setData(scaledData);
        
        Files.createParentDirs(file);
        String filepath = file.getAbsolutePath();
        BufferedImage img = tmp.getDataAsImage(precision);
        ImageIO.write(img, filepath.substring(filepath.length()-3, filepath.length()), file);
    }
    
    /**
     * Will return child of this containing given tile.
     * @param that to be contained within returned tile
     * @return child tile of this, or this in case this and that are equal, 
     * <code>null</code> in case this and that are not having an overlap
     */
    public ElevationTile getChild(ElevationTile that) {
        //midpoint of that must be contained in returned tile
        double latitudeMidpoint = that.minLatitude-256*that.degreesPerPixel;
        double longitudeMidpoint = that.minLongitude+256*that.degreesPerPixel;
        
        //this does not even contain the midpoint, thus it does not contain 
        //whole given tile
        if(!this.contains(latitudeMidpoint, longitudeMidpoint)) {
            return null;
        }
        
        //if this and that have same depth and this contains midpoint of that,
        //this and that have to be equal
        if(this.depth==that.depth) {
            assert(this.equals(that));
            return this;
        }
        
        return getChild(latitudeMidpoint, longitudeMidpoint);
    }
    
    /**
     * Will return child containing given latlong coordinate.
     * @param latitude in degrees
     * @param longitude in degrees
     * @return new child tile with sampled data
     */
    public ElevationTile getChild(double latitude, double longitude) {
        double latitudeMidpoint = minLatitude-256*degreesPerPixel;
        double longitudeMidpoint = minLongitude+256*degreesPerPixel;
        int idxLatitudeChild = idxLatitude<<1;
        int idxLongitudeChild = idxLongitude<<1;
        return new ElevationTile(
                latitude>latitudeMidpoint?idxLatitudeChild+1:idxLatitudeChild, 
                longitude>longitudeMidpoint?idxLongitudeChild+1:idxLongitudeChild, 
                (byte)(depth+1));
    }
    
    /**
     * Will replace data in this tile by given array. Its dimensions must be 
     * 512x512. By setting <code>null</code> will be this tile turned into empty 
     * one.
     */
    public void setData(double[][] data) {
        assert(data.length==512 && data[0].length==512);
        this.data = data;
    }
    
    /**
     * @return data contained within this tile
     */
    public double[][] getData(){
        return this.data;
    }
    
    /**
     * Will set data to this tile from given tile. These will be possibly sampled. 
     * @param that to be used as source of data
     */
    public void setData(ElevationTile that) {
        //calculate scale of that in space of this
        int thatSize = 512;
        if(this.depth>that.depth) {
            thatSize<<=(this.depth-that.depth);
        }else if(this.depth<that.depth) {
            thatSize>>=(that.depth-this.depth);
        }

        //calculate position
        int thatPositionX = that.idxLongitude*thatSize-this.idxLongitude*512;
        int thatPositionY = 512-thatSize-(that.idxLatitude*thatSize-this.idxLatitude*512);
        
        //get image from that
        BufferedImage thatImage = that.getDataAsImage(24);
        
        //set data to this on that position from that image
        this.setDataFromImage(thatImage, thatPositionX, thatPositionY, thatSize, thatSize, 24);
    }
    
    /**
     * Will take data as an image and set them to this tile on given position and 
     * in given dimensions. Precision is used to choose correct model for working
     * image.
     * @param image data to be placed on this tile
     * @param x position in world coordinates
     * @param y position in world coordinates
     * @param width of placed image/data
     * @param height of placed image/data
     * @param precision of working image
     */
    public void setDataFromImage(BufferedImage image, int x, int y, int width, int height, int precision) {
        //re-scale given image to fit image in this
        BufferedImage replacement = null;
        switch(precision) {
            case 8:
                replacement = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
                break;
            case 16:
                replacement = new BufferedImage(512, 512, BufferedImage.TYPE_USHORT_GRAY);
                break;
            case 24:
                replacement = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
                break;
        }
        Graphics2D g = replacement.createGraphics();
        //draw original data if available
        if(data!=null) {
            g.drawImage(this.getDataAsImage(24), 0, 0, null);
        }
        //turn on anti-aliasing for added data - not, AA will mess up the data in 
        //case of 24 bit RGB model (possibly sampling on HSV?)
        //g.setRenderingHints(hints);
        //draw new data
        g.drawImage(image, x, y, width, height, null);
        g.dispose();
        image = replacement;
        
        //convert image double array
        Raster raster = image.getData();
        data = data==null?new double[512][512]:data;
        for(y = 0;y<512;y++) {
            for(x = 0;x<512;x++) {
                switch(precision) {
                    case 8:
                        data[x][y] = (((image.getRGB(x, y)&0xFF)/255.0)*10000.0)-1000;
                        break;
                    case 16:
                        data[x][y] = raster.getSample(x, y, 0)-1000;
                        break;
                    case 24:
                        data[x][y] = ((((image.getRGB(x, y)&0xFFFFFF)>>4)/100)-1000);
                        break;
                }
            }
        }
    }
    
    /**
     * Will take data in this tile and convert them into an image depending on 
     * given precision. Allowed precisions are 8 for 8 bit grayscale, 16 for 16
     * bit grayscale and 24 for RGB scale.
     * @param precision how many bits per pixel there are (either 8 or 16 or 24)
     * @return new image in gray scale 16 bit space in dimensions of 512x512 
     */
    public BufferedImage getDataAsImage(int precision) {
        assert(precision==16 || precision==8 || precision==24);
        if(data==null) {
            throw new RuntimeException("Cannot load elevation data from empty tile!");
        }
        
        //pick color model and create new image depending on precision
        BufferedImage retVal = null;
        switch(precision) {
            case 8:
                retVal = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
                break;
            case 16:
                retVal = new BufferedImage(512, 512, BufferedImage.TYPE_USHORT_GRAY);
                break;
            case 24:
                retVal = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
                break;
        }
        WritableRaster wr = retVal.getRaster();
        
        //will write down in the image all pixels depending on the precision
        for(int y = 0;y<512;y++) {
            for(int x = 0;x<512;x++) {
                switch(precision) {
                    case 8:
                        int div = (int)Math.abs(((data[x][y]+1000)/10000.0)*255.0);
                        retVal.setRGB(x, y, new Color(div,div,div).getRGB());
                        break;
                    case 16:
                        wr.setSample(x, y, 0, (int)Math.round(data[x][y]+1000));
                        break;
                    case 24:
                        int value = (int)Math.round((data[x][y]+1000)*100)<<4;
                        retVal.setRGB(x, y, value);
                        break;
                }
            }
        }
        return retVal;
    }
    
    /**
     * Will classify tiles as equal if they have same indices and depth. 
     * Presence of data is not taken into account! 
     */
    @Override
    public boolean equals(Object obj) {
        if(this==obj) {
            return true;
        }
        if(!(obj instanceof ElevationTile)) {
            return false;
        }
        ElevationTile that = (ElevationTile)obj;
        if(this.idxLatitude!=that.idxLatitude 
                || this.idxLongitude!=that.idxLongitude 
                || this.depth!=that.depth) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        String out = "ElevationTile ";
        out +="("+idxLatitude+","+idxLongitude+") ";
        out +="depth "+depth+" ";
        out +=(data!=null?"is not empty":"is empty");
        return out;
    }
}
