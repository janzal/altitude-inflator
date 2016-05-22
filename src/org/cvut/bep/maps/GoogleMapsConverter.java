package org.cvut.bep.maps;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

/**
 * This class serves as tool for processing of loaded tiles from old google maps
 * data collected over API using IDX/IDY system instead of latlong center. It is
 * here solely for data fetched in past and new google images should be processed
 * using {@link GoogleMapsDownloader} class.
 * 
 * @author Kaiser Vojtech
 *
 */
public class GoogleMapsConverter {
    /**
     * Start IDX for more obvious iteration
     */
    private static final int START_IDX = 565699;
    
    /**
     * Start IDY for more obvious iteration
     */
    private static final int START_IDY = 355080;
    
    /**
     * Encapsulation class for latlong bounding box
     */
    private static class LatLongBounds{
        public double latitudeMin;
        public double longitudeMin;
        public double latitudeMax;
        public double longitudeMax;
        public LatLongBounds(double latitudeMin, double longitudeMin, double latitudeMax, double longitudeMax) {
            this.latitudeMax = latitudeMax;
            this.longitudeMax = longitudeMax;
            this.latitudeMin = latitudeMin;
            this.longitudeMin = longitudeMin;
        }
    }
    
    /**
     * Recalculation of IDX/IDY system from filenames into latlong bounds.
     * @param file to be processed (source of ID)
     * @return latlong bounds object
     */
    public static LatLongBounds getMapBounds(final File file) {
        StringTokenizer tokenizer = new StringTokenizer(file.getName().replace(".jpg", ""), "_");
        
        // skip "gs_" prefix
        tokenizer.nextElement();
        
        int x = Integer.parseInt( tokenizer.nextToken() );
        int y = Integer.parseInt( tokenizer.nextToken() );
        int zoom = Integer.parseInt( tokenizer.nextToken() );
        
        double n = Math.pow(2, zoom);
        double longitudeMin = x/n * 360 -180;
        double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y/n)));
        double latitudeMin = lat_rad * 180/Math.PI;

        double longitudeMax = (x + 1)/n * 360 -180;
        lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1)/n)));
        double latitudeMax = lat_rad * 180/Math.PI;
        
        return new LatLongBounds(latitudeMin, longitudeMin, latitudeMax, longitudeMax);
    }
    
    /**
     * Will merge multiple tiles into single bigger tile for easier manipulation.
     * @param startX tile index
     * @param startY tile index
     * @param size of new tile in tile counts
     * @return tiles specified by parameters as single image
     * @throws IOException in case of image read error
     */
    public static BufferedImage mergeTiles(int startX, int startY, int size) throws IOException {
        BufferedImage img = new BufferedImage(size*256,size*256,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        
        for(int x = startX;x<size+startX;x++) {
            for(int y = startY;y<size+startY;y++) {
                Image part = ImageIO.read(new File("../../Ruzyne/gmaps/gs_"+(START_IDX+x)+"_"+(START_IDY+y)+"_20.jpg"));
                g.drawImage(part, (x-startX)*256, (y-startY)*256, null);
            }
        }
        
        return img;
    }
    
    public static void main(String[] args) throws IOException {
        int size = 25;
        for(int y=0;y<150;y+=size) {
            for(int x = 0;x<225;x+=size) {
                //merge into bigger tiles
                System.out.println("Constructing "+x+" "+y);
                BufferedImage img = mergeTiles(x, y, size);
                
                //store bigger tile
                System.out.println("Storing "+x+" "+y);
                ImageIO.write(img, "jpg", new File("../../Ruzyne/tiles/si_"+y+"_"+x+"_"+size+".jpg"));
                
                System.out.println("Writing txt");
                //fetch bounds of border tiles
                LatLongBounds boundsFirst = getMapBounds(new File("../../Ruzyne/gmaps/gs_"+(START_IDX+x)+"_"+(START_IDY+y)+"_20.jpg"));
                LatLongBounds boundsLast = getMapBounds(new File("../../Ruzyne/gmaps/gs_"+(START_IDX+x+size-1)+"_"+(START_IDY+y+size-1)+"_20.jpg"));
                
                //write file with latlong bounds for earth layer provider QT
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File("../../Ruzyne/tiles/si_"+y+"_"+x+"_"+size+".txt")));
                bw.write("si_"+y+"_"+x+"_"+size+".jpg"+"\n");
                bw.write(boundsFirst.longitudeMin+"\n");
                bw.write(boundsLast.longitudeMax+"\n");
                //swapped for whatever the reason is
                bw.write(boundsLast.latitudeMax+"\n");
                bw.write(boundsFirst.latitudeMin+"\n");
                bw.close();
            }
        }
    }
}
