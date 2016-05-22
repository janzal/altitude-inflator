package maps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import aglobe.util.Logger;


/**
 * This class serves for transformation of tiles connected with information about
 * what surface they cover into set of tiles.
 *
 * @author Tomas Blovsky
 *
 */
public class TileCreator {
    /**
     * Maximum expected zoom level at 25, though currently available is only 20.
     */
    private static final int MAX_ZOOM = 25;

    /**
     * Starting value of zoom as minimum, where 0 would be whole earth. Using this
     * constant can be changed whether only detailed layers should be generated.
     */
    private static final int MIN_ZOOM = -1;

	public static void main(String[] args) throws IOException {
        //createTiles(new File("./work/datasets/GOOGLE_prague_in"), new File("./work/datasets/GOOGLE_prague_out"), BufferedImage.TYPE_INT_RGB);
        //createTiles(new File("./work/datasets/NASA_night_in"), new File("./work/datasets/NASA_night_out"), BufferedImage.TYPE_INT_RGB);
        //createTiles(new File("./work/datasets/NASA_blue_marble_in"), new File("./work/datasets/NASA_blue_marble_out"), BufferedImage.TYPE_INT_RGB);
	    createTiles(new File("./work/datasets/GOOGLE_forest_in"), new File("./work/datasets/GOOGLE_forest_out"), BufferedImage.TYPE_INT_RGB);
	}

	/**
	 * Will construct quad tree tiles from images found in source folder and store
	 * them in destination folders grouped by zoom level.
	 * @param sourceFolder containing text files with latlong boundaries and images
	 * @param destinationFolder where output tiles are stored
	 * @param imageType of images that this method generates.
	 * Use {@link BufferedImage#TYPE_USHORT_GRAY} for elevation maps.
	 * @throws IOException in case of failure while writing resulting images
	 */
	public static void createTiles(File sourceFolder, File destinationFolder, int imageType) throws IOException {
		//load input images
		ArrayList<Rect> sources = loadSources(sourceFolder, null);
        System.out.println("SCAN DONE");

		//create unprocessed output images
		Stack<Rect> toProcess = new Stack<>();
		toProcess.add(new Rect(-180, 0, -90, 90, 0, 0, 0, imageType));
		toProcess.add(new Rect(0, 180, -90, 90, 0, 1, 0, imageType));

		//for all pending rectangles
		while(!toProcess.isEmpty()){
			Rect processed = toProcess.pop();

			double minResolutionRatio = Double.MAX_VALUE;

			//iterate over all source images and paint their contents over processed
			//in case an overlap is found
			for (Rect source : sources) {
				if(processed.intersects(source)){
					minResolutionRatio = Math.min(processed.getHorizontalResolutionRatio(source), minResolutionRatio);
					if(processed.zoomLevel > MIN_ZOOM){
						processed.paint(source);
					}
				}
			}

			//process only if current level offered reasonable amount of content
			//but always process the zero level
			if(minResolutionRatio < 2 || processed.zoomLevel == 0){
			    //store only images on desired levels and complete ones
				if(processed.zoomLevel > MIN_ZOOM && processed.isComplete()){
		            processed.writeImage(destinationFolder);
				}
				System.out.println(processed+" ... "+minResolutionRatio);

				//proceed with processing of children in quad tree in case there
				//is enough detail and max zoom level was not exceeded
				if(minResolutionRatio < 1 && processed.zoomLevel < MAX_ZOOM){
					toProcess.addAll(Arrays.asList(processed.splitQuad()));
				}
			}else {
			    System.out.println(processed+" ... "+minResolutionRatio+" : end");
			}
		}
	}

	/**
	 * Will load source images with their properties from given source folder.
	 * @param sourceFolder containing images and their property files
	 * @return list of loaded sources sorted by their resolution from crudest
	 * to finest
	 */
	private static ArrayList<Rect> loadSources(File sourceFolder, ArrayList<Rect> list) {
	    ArrayList<Rect> retVal = list;
	    if(retVal==null) {
	         retVal = new ArrayList<Rect>();
	    }
		//go through all files in the target folder and look for text files
		//containing image name and latlong bounds belonging to those images
		for (File file : sourceFolder.listFiles()) {
		    if(file.isDirectory()) {
		        System.out.println("enter "+file);
		        loadSources(file, retVal);
		        System.out.println("exit "+file);
		    }else {
    		    try {
        			if(file.getName().endsWith(".txt")){
        				System.out.println(file +" loading...");

        				//read tile properties from the text file
        				BufferedReader metaReader = new BufferedReader(new FileReader(file));
        				String imageFileName = metaReader.readLine();
        				double longitudeMin = Double.parseDouble(metaReader.readLine());
        				double longitudeMax = Double.parseDouble(metaReader.readLine());
        				double latitudeMin = Double.parseDouble(metaReader.readLine());
        				double latitudeMax = Double.parseDouble(metaReader.readLine());
        				metaReader.close();

        				//create new rectangle for processing
        				retVal.add(new Rect(longitudeMin, longitudeMax, latitudeMin,
        				        latitudeMax, new File(file.getParent()+"/"+imageFileName)));
        			}
    		    }catch(IOException e) {
					Logger.log("Hello");
    		        Logger.logSevere("Failed to load tile! ("+file+")");
    		    }
		    }
		}

		//sort images by their resolution so the worse quality ones are used first
		//and then possibly be painted over by better ones
		Collections.sort(retVal, new Comparator<Rect>() {
			@Override
			public int compare(Rect o1, Rect o2) {
			    if(o1.getHorizontalResolution() == o2.getHorizontalResolution()) {
			        return 0;
			    }
				return o1.getHorizontalResolution() < o2.getHorizontalResolution()? -1:1;
			}
		});
		return retVal;
	}

	/**
	 * This class serves as inner wrapper of image with its boundaries and indices.
	 *
	 * TODO buffered image wrapper that saves image before is image GCd away. With
	 * this remove hard reference to image and use only soft one.
	 */
	private static class Rect{
	    /**
	     * Hard reference to an image. If there is an instance, soft reference is
	     * ignored altogether.
	     */
	    private BufferedImage image = null;

	    /**
	     * Bitmask for spotting whether all pixels in the rectangle were filled
	     */
	    private BufferedImage bitmap = null;

	    /**
	     * Soft reference to an image in this rectangle. Image in this reference
	     * must be re-acquirable.
	     */
		private SoftReference<BufferedImage> imageReference = new SoftReference<BufferedImage>(null);

		/**
		 * File containing image for this rectangle
		 */
		private File imageFilepath = null;

		/**
		 * Dimensions of image represented by this rectangle in pixels.
		 */
		private final Dimension dimensions = new Dimension();

		/**
		 * Minimum longitude of bounding rectangle in degrees
		 */
		private final double longitudeMin;

		/**
		 * Maximum longitude of bounding rectangle in degrees
		 */
		private final double longitudeMax;

		/**
		 * Minimum latitude of bounding rectangle in degrees
		 */
		private final double latitudeMin;

		/**
		 * Maximum latitude of bounding rectangle in degrees
		 */
		private final double latitudeMax;

		/**
		 * Zoom level at which resides this rectangle
		 */
		private final int zoomLevel;

		/**
		 * Longitude index of IDX system
		 */
		private final int longitudeIndex;

		/**
		 * Latitude index of IDX system
		 */
		private final int latitudeIndex;

		/**
		 * Will create new instance of rectangle with given boundary and image.
		 * Zoom level and indices are set to -1 and operations over this rectangle
		 * that use them will be blocked.<br>
		 * This image is stored in soft reference on use and any changes done to
		 * it may not prevail.
		 * @param longitudeMin boundary in degrees
		 * @param longitudeMax boundary in degrees
		 * @param latitudeMin boundary in degrees
		 * @param latitudeMax boundary in degrees
		 * @param imageFilepath to be stored
		 */
        public Rect(double longitudeMin, double longitudeMax, double latitudeMin,
                double latitudeMax, File imageFilepath) {
            this.longitudeMin = longitudeMin;
            this.longitudeMax = longitudeMax;
            this.latitudeMin = latitudeMin;
            this.latitudeMax = latitudeMax;
            this.zoomLevel = 0;
            this.longitudeIndex = 0;
            this.latitudeIndex = 0;
            this.imageFilepath = imageFilepath;
            this.dimensions.setSize(getImageDimensions(imageFilepath));
        }

        /**
         * Will create new instance of rectangle with given boundary, IDX and
         * zoom level, and with blank image of 512x512 pixels.<br>
         * This image is in memory using hard reference and any changes done to
         * it are permanent.
         * @param longitudeMin boundary in degrees
         * @param longitudeMax boundary in degrees
         * @param latitudeMin boundary in degrees
         * @param latitudeMax boundary in degrees
         * @param zoomLevel at which this rectangle resides
         * @param longitudeIndex
         * @param latitudeIndex
         * @param imageType specifying color model for created image
         * @see BufferedImage#TYPE_USHORT_GRAY
         * @see BufferedImage#TYPE_INT_ARGB
         */
		public Rect(double longitudeMin, double longitudeMax, double latitudeMin,
		        double latitudeMax, int zoomLevel, int longitudeIndex, int latitudeIndex, int imageType) {
            this.longitudeMin = longitudeMin;
            this.longitudeMax = longitudeMax;
            this.latitudeMin = latitudeMin;
            this.latitudeMax = latitudeMax;
            this.zoomLevel = zoomLevel;
            this.longitudeIndex = longitudeIndex;
            this.latitudeIndex = latitudeIndex;

            //create new image with black background
            this.image = new BufferedImage(512, 512, imageType);
            //technically unnecessary since only complete images are accepted
            Graphics2D g = this.image.createGraphics();
            g.setBackground(Color.BLACK);
            g.clearRect(0, 0, 512, 512);
            g.dispose();

            //create image for bitmap that marks where was something painted
            this.bitmap = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_BINARY);
            g = this.bitmap.createGraphics();
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, 512, 512);
            g.dispose();

            this.dimensions.setSize(512, 512);
		}

		/**
		 * Will create four new rectangles covering the same area as this, split
		 * in even grid. These will have their level increased and indices moved
		 * to higher precision.<br>
		 * This function will fail in rectangles that do not have zoom and indices
		 * specified.
		 * @return array of new rectangles
		 */
		public Rect[] splitQuad(){
		    assert(zoomLevel>-1 && latitudeIndex>-1 && longitudeIndex>-1);

			Rect[] retVal = new Rect[4];

			double longitudeMid = (longitudeMin+longitudeMax)/2;
			double latitudeMid = (latitudeMin+latitudeMax)/2;

			retVal[0] = new Rect(longitudeMin, longitudeMid, latitudeMin, latitudeMid, zoomLevel+1, longitudeIndex*2, latitudeIndex*2, image.getType());
			retVal[1] = new Rect(longitudeMid, longitudeMax, latitudeMin, latitudeMid, zoomLevel+1, longitudeIndex*2+1, latitudeIndex*2, image.getType());
			retVal[2] = new Rect(longitudeMin, longitudeMid, latitudeMid, latitudeMax, zoomLevel+1, longitudeIndex*2, latitudeIndex*2+1, image.getType());
			retVal[3] = new Rect(longitudeMid ,longitudeMax, latitudeMid, latitudeMax, zoomLevel+1, longitudeIndex*2+1, latitudeIndex*2+1, image.getType());

			return retVal;
		}

		/**
		 * Will check whether this rectangle intersects that rectangle.
		 * @param that to be checked against
		 * @return true if there is some intersection
		 */
		public boolean intersects(Rect that) {
			if(this.longitudeMax<=that.longitudeMin || this.longitudeMin>=that.longitudeMax){
				return false;
			}
			if(this.latitudeMax<=that.latitudeMin || this.latitudeMin>=that.latitudeMax){
				return false;
			}
			return true;
		}

		/**
		 * Will paint given source rectangle on this
		 * @param source rectangle to be placed on this
		 */
		public void paint(Rect source) {
		    BufferedImage thisImage = this.acqiureImage();
		    if(thisImage==null) {
		        return;
		    }

            Graphics2D g2d = (Graphics2D) thisImage.getGraphics();
		    try {
		        //causes issues around the edges
    			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    			//calculate resolutions of images for estimate of position in projection
    			double thisResX = this.getHorizontalResolution();
    			double srcResX = source.getHorizontalResolution();
    			double thisResY = this.getVerticalResolution();
                double srcResY = source.getVerticalResolution();

                //position of overlap in source image
    		    int srcX = (int) Math.max((longitudeMin-source.longitudeMin) * srcResX, 0);
    		    int srcY =  (int) Math.max((source.latitudeMax-latitudeMax) * srcResY, 0);
    		    int srcWidth = (int) Math.ceil(Math.min((longitudeMax-source.longitudeMin) * srcResX, source.dimensions.getWidth())-srcX);
    		    int srcHeight =  (int) Math.ceil(Math.min((source.latitudeMax-latitudeMin) * srcResY, source.dimensions.getHeight())-srcY);

    		    //position of overlap in this
    			int thisX = (int) Math.max((source.longitudeMin-longitudeMin) * thisResX, 0);
    			int thisY = (int) Math.max((latitudeMax-source.latitudeMax) * thisResY, 0);
    			int thisWidth = (int) Math.ceil((srcWidth) * (thisResX/srcResX));
    			int thisHeight =(int) Math.ceil((srcHeight) * (thisResY/srcResY));

    			//if resulting overlap is not empty in pixels, draw source on this
    			if(srcWidth<=0 || srcHeight<=0 || thisWidth<=0 || thisHeight<=0){
    			    return;
    			}

			    BufferedImage sourceImage = source.acqiureImage();
			    if(sourceImage==null) {
			        return;
			    }

                BufferedImage clip = sourceImage.getSubimage(srcX, srcY, srcWidth, srcHeight);
                g2d.drawImage(clip , thisX, thisY, thisWidth, thisHeight, null);

			    if(this.bitmap==null) {
			        return;
			    }

			    //paint black the same area in mask
			    Graphics2D g = (Graphics2D) this.bitmap.getGraphics();
			    g.setColor(Color.BLACK);
			    g.clearRect(thisX, thisY, thisWidth, thisHeight);
			    g.dispose();

		    }finally {
		        g2d.dispose();
		    }
		}

		/**
		 * @return true if all pixels of this rectangle were covered with some
		 * images
		 */
		public boolean isComplete() {
		    if(bitmap==null) {
		        return false;
		    }

		    for(int x = 0;x<512;x++) {
		        for(int y = 0;y<512;y++) {
		            if(bitmap.getRGB(x, y)==Color.WHITE.getRGB()) {
		                return false;
		            }
		        }
		    }

		    return true;
		}

		/**
		 * @return how many pixels there is per degree of longitude
		 */
		public double getHorizontalResolution(){
			return dimensions.getWidth() / (longitudeMax-longitudeMin);
		}

		/**
		 * @return how many pixels there is per degree of latitude
		 */
		public double getVerticalResolution() {
		    return dimensions.getHeight() / (latitudeMax-latitudeMin);
		}

		/**
		 * @param that rectangle to be used as compared
		 * @return ratio between resolution of this and that
		 */
		public double getHorizontalResolutionRatio(Rect that){
			return this.getHorizontalResolution()/that.getHorizontalResolution();
		}

		/**
		 * Will construct output path for this rectangle and store image in this
		 * to it.
		 * @param destinationFolder that will contain stored file
		 * @throws IOException in case of failed storing of the image
		 */
		public void writeImage(File destinationFolder) throws IOException {
		    //zoom part is over two digits
            String zoom = Integer.toString(zoomLevel);
			while(zoom.length()<2){
				zoom = "0"+zoom;
			}

			//latlong parts have either 4 or 8 digits depending on precision
			String longitude = Integer.toString(longitudeIndex);
            String latitude = Integer.toString(latitudeIndex);
			int stringLength = Math.max(longitude.length(), latitude.length()) > 4 ? 8:4;
			while(longitude.length()<stringLength){
				longitude = "0"+longitude;
			}
			while(latitude.length()<stringLength){
				latitude = "0"+latitude;
			}



			BufferedImage img = acqiureImage();
			if(img==null) {
			    return;
			}

			if(img.getType()==BufferedImage.TYPE_USHORT_GRAY) {
			    //construct path to target file
	            File file = new File(destinationFolder+"/"+zoom+"/"+longitude+"_"+latitude+".bin");
	            file.getParentFile().mkdirs();

			    //in case of elevation gray scale images write down binary file
			    //of shorts
			    DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
			    int[] pixels = img.getData().getPixels(0, 0, img.getWidth(), img.getHeight(), new int[img.getWidth()*img.getHeight()]);
			    for (int i = 0;i<pixels.length;i++) {
                    dos.writeShort(pixels[i]);
                }
			    dos.close();

			    file = new File(destinationFolder+"/"+zoom+"/"+longitude+"_"+latitude+".txt");
			    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(this.longitudeMin+"\n");
                bw.write(this.longitudeMax+"\n");
                bw.write(this.latitudeMin+"\n");
                bw.write(this.latitudeMax+"\n");
                bw.close();

                file = new File(destinationFolder+"/"+zoom+"/"+longitude+"_"+latitude+".jpg");
                ImageIO.write(img, "jpg", file);
			} else {
			    //construct path to target file
	            File file = new File(destinationFolder+"/"+zoom+"/"+longitude+"_"+latitude+".jpg");
	            file.getParentFile().mkdirs();

    			//paint image from this rectangle in new image and store it in desired
    			//location
    			BufferedImage retVal = new BufferedImage(512, 512, BufferedImage.TYPE_INT_BGR);
    			Graphics2D g = retVal.createGraphics();
    			g.drawImage(img, 0, 0, 512, 512, null);
    			ImageIO.write(retVal, "jpg", file);
			}
		}

		@Override
		public String toString() {
			return "Rect(longitude "+longitudeIndex+", latitude "+latitudeIndex+", zoom "+zoomLevel+")";
		}

		/**
		 * Will try to get image that belongs to this rectangle from hard reference,
		 * soft reference or file on the disk.
		 * @return image or <code>null</code> in case of image not being available
		 */
		private BufferedImage acqiureImage() {
		    //if there is present hard reference to image, return it right away
		    if(image!=null) {
		        return image;
		    }

		    //try to get image from soft reference
		    BufferedImage retVal = this.imageReference.get();

		    //if failed getting image from soft reference and there is some image
		    //file path present, try to load it and store it back to the soft
		    //reference for future use
		    if(retVal==null && imageFilepath!=null) {
		        try {
		            retVal = ImageIO.read(imageFilepath);
		            imageReference = new SoftReference<BufferedImage>(retVal);
		        }catch(Exception e) {
		            Logger.logSevere("Failed to acquire image!");
		        }
		    }

		    return retVal;
		}

		/**
		 * Will return dimensions of image in given file without loading it, if
		 * possible.
		 * @param file to be checked
		 * @return dimensions of the image or zero dimensions in case of errors
		 */
		private Dimension getImageDimensions(File file) {
		    try(ImageInputStream in = ImageIO.createImageInputStream(file)){
		        final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
		        if (readers.hasNext()) {
		            ImageReader reader = readers.next();
		            try {
		                reader.setInput(in);
		                return new Dimension(reader.getWidth(0), reader.getHeight(0));
		            } finally {
		                reader.dispose();
		            }
		        }
		    }catch(Exception e) {
		        Logger.logWarning("Failed to acquire image dimensions without opening it!");
		    }

		    //load the image
		    try {
		        BufferedImage img = ImageIO.read(file);
		        if(img!=null) {
		            //while it was read, put it in soft reference so it is only
		            //possibly in vain, and not completely
                    imageReference = new SoftReference<BufferedImage>(img);
	                return new Dimension(img.getWidth(), img.getHeight());
		        }
		        Logger.logSevere("No reader for image in "+file+" was foun!");
            } catch (IOException e) {
                Logger.logSevere("Failed to load image "+file+"!");
            } catch (Exception e) {
                Logger.logSevere("Failed to acquire image dimensions!");
                e.printStackTrace();
            }

		    //on zero dimensions will be this rectangle simply ignored without
		    //any further modifications
            return new Dimension();
		}
	}
}
