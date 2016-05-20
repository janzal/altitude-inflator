package maps;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;

/**
 * This class serves for conversion of various formats elevation maps may be in
 * into different ones, perhaps compatible with currently used elevation map system
 * in visio.
 * 
 * @author Kaiser Vojtech
 *
 */
public class ElevationMapConvertor {
    public static void main(String[] args) throws IOException {
        convertBinaryElevationMap(new File("/Users/janzaloudek/Library/Containers/com.apple.mail/Data/Library/Mail Downloads/BB2E169B-1076-42E6-91B5-B9EC7599A3F7/obora_zidlov/data/"), false, false);
    }
    
    /**
     * Will convert all binary maps found in given root directory or file, 
     * possibly delete them and insert a png file in the same location with the 
     * same name.
     * @param root file or directory to be recursively searched and converted
     * @param delete true in case source binary files should be deleted
     * @param jpg true if jpg image files should be created. These serve for 
     * display of height maps as textures on the earth.
     * @throws IOException thrown in case of file load or save errors
     */
    public static void convertBinaryElevationMap(File root, boolean delete, boolean jpg) throws IOException {
        Queue<File> toProcess = new LinkedList<>();
        toProcess.add(root);
        while(!toProcess.isEmpty()) {
            File current = toProcess.poll();
            if(current.isDirectory()) {
                toProcess.addAll(Arrays.asList(current.listFiles()));
                System.out.println("> Explore "+current);
                continue;
            }
            
            if(current.getName().endsWith("bin")) {
                System.out.println("> converting "+current);
                BufferedImage original = loadMapBinary16(current, 512, 512);
                BufferedImage replacement = convertG16ToRGB24(original);
                ImageIO.write(replacement, "png", new File(current.getAbsolutePath().replace("bin", "png")));
                if(jpg) {
                    ImageIO.write(replacement, "jpg", new File(current.getAbsolutePath().replace("bin", "jpg")));
                }
                if(delete) {
                    current.delete();
                }
            }
            
        }
    }
    
    /**
     * Will export the tile as binary and text representation along with coordinates
     * stored in separate file. These files are named using quad tree indices 
     * and depth.
     * @param idxLatitude of the tile
     * @param idxLongitude of the tile
     * @param depth in the quad tree
     * @param data to be stored
     * @param path to have the files stored
     * @throws IOException in case of failure while writing the files
     */
    public static void exportTile(int idxLatitude, int idxLongitude, byte depth, 
            double[][] data, String path) throws IOException {
        assert(data.length!=0 && data.length==data[0].length);
        int sampleCount = data.length;
        System.out.println("sample count "+sampleCount);
        BufferedWriter bw;
        
        //calculate min, max latlong
        double degreesPerTile = 180.0/(1<<depth);
        double degreesPerSample = degreesPerTile/sampleCount;
        System.out.println("Degrees per sample "+degreesPerSample);
        double minLatitude = (idxLatitude+1)*degreesPerTile-90;
        double maxLatitude = (idxLatitude)*degreesPerTile-90;
        double minLongitude = idxLongitude*degreesPerTile-180;
        double maxLongitude = (idxLongitude+1)*degreesPerTile-180;
        
        //write text representation
        StringBuilder sb = new StringBuilder();
        for(int y = 0; y<sampleCount; y++) {
            for(int x = 0; x< sampleCount; x++) {
                sb.append(Double.toString(data[x][y]));
                sb.append(" ");
            }
            sb.append('\n');
        }
        bw = new BufferedWriter(new FileWriter(new File(
                path+"/data_"+idxLatitude+"_"+idxLongitude+"_"+depth+".txt")));
        bw.write(sb.toString());
        bw.close();
        
        //write binary representation
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(
                path+"/data_"+idxLatitude+"_"+idxLongitude+"_"+depth+".bin")));
        for(int y = 0; y<sampleCount; y++) {
            for(int x = 0; x< sampleCount; x++) {
                oos.writeDouble(data[x][y]);
            }
        }
        oos.close();
        
        //write coordinates
        bw = new BufferedWriter(new FileWriter(new File(
                String.format(path+"/coords_%d_%d_%d.txt", idxLatitude, idxLongitude, depth))));
        bw.write(minLongitude+"\n");
        bw.write(maxLongitude+"\n");
        bw.write(minLatitude+"\n");
        bw.write(maxLatitude+"\n");
        bw.close();
    }
    
    /**
     * Will build 24 bit RGB height map out of given array of data. Indexing in 
     * given array is expected to be [x][y].
     * @param data
     * @return
     */
    public static BufferedImage buildRGB24(double[][] data) {
        int width = data.length;
        int height = data[0].length;
        BufferedImage retVal = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for(int x = 0;x<width;x++) {
            for(int y = 0;y<height;y++) {
                //fixed addition of 1000m to get out of negative numbers everywhere
                //multiply by 100 to get the value in centimeters
                //this should comfortably fit in 24 bits considering lowest point
                //-418m and highest point 8800m give or take.
                //four bit left shift is for better visual representation of small
                //values (now they are represented with higher values of blue and
                //green)
                int value = (int)Math.round((data[x][y]+1000)*100)<<4;
                
                retVal.setRGB(x, y, value);
            }
        }
        
        return retVal;
    }
    
    
    /**
     * Will convert 16 bit grayscale image to 24 bit RGB image height map. In 
     * resulting image is used value of height in centimeters with 1000 meter
     * offset as replacement of negative numbers.
     * @param source of the data
     * @return new image in RGB scheme
     */
    public static BufferedImage convertG16ToRGB24(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage retVal = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        Raster raster = source.getData();
        for(int x = 0;x<width;x++) {
            for(int y = 0;y<height;y++) {
                int original = raster.getSample(x, y, 0);
                //fixed addition of 1000m to get out of negative numbers everywhere
                //multiply by 100 to get the value in centimeters
                //this should comfortably fit in 24 bits considering lowest point
                //-418m and highest point 8800m give or take.
                //four bit left shift is for better visual representation of small
                //values (now they are represented with higher values of blue and
                //green)
                int replacement = ((original+1000)*100)<<4;
                
                retVal.setRGB(x, y, replacement);
            }
        }
        
        return retVal;
    }
    
    /**
     * Will load a binary 16 bit grayscale map from given source that has given
     * dimensions.
     * @param source to be used to get data buffer
     * @param width of the map in pixels
     * @param height of the map in pixels
     * @return loaded image in USHORT grayscale
     * @throws IOException in case of file read errors
     */
    public static BufferedImage loadMapBinary16(File source, int width, int height) throws IOException {
        //load binary into byte buffer
        RandomAccessFile raf = new RandomAccessFile(source, "r");
        byte[] buffer = new byte[(int)raf.length()];
        raf.readFully(buffer);
        raf.close();
        
        assert(buffer.length==width*height*2);
        
        //create new gray scale image
        BufferedImage retVal = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster wr = retVal.getRaster();
        
        //write in it line by line
        for(int y = 0;y<height;y++) {
            for(int x = 0;x<width;x++) {
                int offset = ((x + y * width) * 2);
                int value = (short)((Byte.toUnsignedInt(buffer[offset]) + 
                        (Byte.toUnsignedInt(buffer[offset+1]) << 8)) - 32768);
                wr.setSample(x, y, 0, value);
            }
        }
        
        return retVal;
    }
    
    /**
     * Will retrieve 8 bit grayscale value from given image at given coordinates.
     * @param image as source
     * @param x coordinate
     * @param y coordinate
     * @return value between 0 and 255
     */
    private static int getG8(BufferedImage image, int x, int y) {
        return image.getRGB(x, y)&0xFF;
    }
    
    /**
     * Will set given 8 bit grayscale value to given image at given coordinates.
     * @param image as destination
     * @param x coordinate
     * @param y coordinate
     * @param value between 0 and 255
     */
    private static void setG8(BufferedImage image, int x, int y, int value) {
        assert(value>=0 && value<=255);
        image.setRGB(x, y, value|(value<<8)|(value<<16));
    }
    
    /**
     * Will lower elevation map found in source image, move it to its current 
     * non-zero minimum and return it as 8 bit grayscale image. This method is 
     * intended for moving elevation down as close to zero as possible while 
     * preserving relative differences. 
     * @param source containing map in 8 bit grayscale
     */
    public static BufferedImage lowerMap8(BufferedImage source) {
        //load image
        int width = source.getWidth();
        int height = source.getHeight();
        
        //find non zero minimum
        int min = Integer.MAX_VALUE;
        for(int x = 0;x<width;x++) {
            for(int y = 0;y<height;y++) {
                if(getG8(source, x, y)<min && getG8(source, x, y)!=0) {
                    min = getG8(source, x, y);
                }
            }
        }
        
        //subtract minimum from everything
        for(int x = 0;x<width;x++) {
            for(int y = 0;y<height;y++) {
                if(getG8(source, x, y)!=0) {
                    setG8(source, x, y, getG8(source, x, y)-min);
                }
            }
        }
        
        return source;
    }
}
