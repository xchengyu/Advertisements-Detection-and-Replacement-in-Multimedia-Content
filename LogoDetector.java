import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingWorker;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;

import java.awt.image.DataBufferInt;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;
class Frame{
	private int selfPoints;
	public int index;
	private double variance;
	private double mean;
	private Map<Integer, Integer> neigPoints;
	
	public Frame(int index) {
		this.index = index;
		neigPoints = new HashMap<Integer, Integer>();
		variance = -1;
		mean = -1;
	}
	
	public Frame (Frame a) {
		this.index = a.index;
		this.neigPoints = new HashMap<Integer, Integer>(a.neigPoints);
		this.variance = a.variance;
		this.mean = a.mean;
		this.selfPoints = a.selfPoints;
	}
	public void setSelfPoints(int points) {
		this.selfPoints = points;
	}
	
	public int getSelfPonts() {
		return selfPoints;
	}

	public int compareSimple(Frame b) {
		if(this.getSelfPonts() != b.getSelfPonts()) {
			return this.getSelfPonts() - b.getSelfPonts();
		}
//		if(this.getVariance() != b.getVariance()) {
//			return this.getVariance() - b.getVariance() > 0 ? 1 : -1;
//		}
		if (this.getMean() != b.getMean()) {
			return this.getMean() - b.getMean() > 0 ? 1 : -1;
		}
		return 0;
	}
	
	public int compareHard(Frame b) {
		if(this.getSelfPonts() != b.getSelfPonts()) {
			return this.getSelfPonts() - b.getSelfPonts();
		}
		if(this.getVariance() != b.getVariance()) {
			return this.getVariance() - b.getVariance() > 0 ? -1 : 1;
		}
		if (this.getMean() != b.getMean()) {
			return this.getMean() - b.getMean() > 0 ? 1 : -1;
		}
		return 0;
	}
	
	public double getMean() {
		if(mean == -1) {
			int sum = selfPoints;
			for (Map.Entry<Integer, Integer> entry: neigPoints.entrySet()) {
				sum += entry.getValue();
			}
			mean = (double)(sum/(neigPoints.size() + 1));
		}
		return mean;
		
	}
	
	public double getVariance() {
		if (variance == -1) {
			double mean = getMean();
	        double temp = 0;
	        for (Map.Entry<Integer, Integer> entry: neigPoints.entrySet()) {
	        	temp += ((double)entry.getValue() - mean)*((double)entry.getValue() - mean);
			}
	        
	        variance = (double)(temp/(neigPoints.size() + 1));
		}
        return variance;
	}
	
	public  Map<Integer, Integer> getNeigPoints() {
		return neigPoints;
	}

}

public class LogoDetector extends SwingWorker<Void, Void>{
	private String inputVideoFile;
	private String inputAudioFile;
	private String outputVideoFile;
	private String outputAudioFile;
	private List<String> detectLogoImages;
	private List<String> insertVideos;
	private List<String> insertAudios; 
	private Map<Integer, Integer> videoInsertIndexes;//key is frame index, value is video/audio index
	private Map<Integer, Integer> audioInsertIndexes;//key is frame index, value is video/audio index
	public String bookObject = null;
    public Mat objectImage = null;
    public HashMap<Integer, Frame> frameInfor;
    public int width = MediaPlayer.width;
    public int height = MediaPlayer.height;
    public static Frame special;
    public static int startFrame = 2;
    public static int endFrame = 7976;
    public static int threshold;
    public static long startTime = 0;

	public LogoDetector(String inputVideoFile, String inputAudioFile, 
			String outputVideoFile, String outputAudioFile, 
			List<String> detectLogoImages, List<String> insertVideos, List<String> insertAudios) {
		this.inputVideoFile = inputVideoFile;
		this.inputAudioFile = inputAudioFile;
		this.outputVideoFile = outputVideoFile;
		this.outputAudioFile = outputAudioFile;
		this.detectLogoImages = detectLogoImages;
		this.insertVideos = insertVideos;
		this.insertAudios = insertAudios;
		this.videoInsertIndexes = new HashMap<Integer, Integer>();
		this.audioInsertIndexes = new HashMap<Integer, Integer>();
	}
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		startTime = System.currentTimeMillis();
        File lib = null;
        String os = System.getProperty("os.name");
        String bitness = System.getProperty("sun.arch.data.model");

        if (os.toUpperCase().contains("WINDOWS")) {
            if (bitness.endsWith("64")) {
                lib = new File("libs//x64//" + System.mapLibraryName("opencv_java2411"));
            } else {
                lib = new File("libs//x86//" + System.mapLibraryName("opencv_java2411"));
            }
        }

        System.out.println(lib.getAbsolutePath());
        System.load(lib.getAbsolutePath());

        int curr = 0;
        while (curr < detectLogoImages.size()) {
	        //bookObject = "images//starbucks_logo.bmp";
        	special = null;
        	threshold = 3;
	    	bookObject = detectLogoImages.get(curr);
	        objectImage = Highgui.imread(bookObject, Highgui.CV_LOAD_IMAGE_COLOR);
	        frameInfor  = new HashMap<Integer, Frame>();
	        Set<Integer> sample = new HashSet<Integer>();
	        
	        int count = startFrame + 2;
	        while (count <= endFrame - 2) {
	        	sample.add(count);
	        	count += 10;
	        }
	        filter(sample, inputVideoFile);
	        startDetect(sample, 64, inputVideoFile);
	        System.out.println("logo: "+ curr + ", Index: " + special.index + ", point: " + special.getSelfPonts());
	        videoInsertIndexes.put(special.index, curr);
	        audioInsertIndexes.put(special.index / 3, curr);
	        curr++;
        }
        
		return null;
	}

	private void filter(Set<Integer> sample, String filename) {
    	try {
 			File file = new File(filename);
 			InputStream is = new FileInputStream(file);
 			BufferedImage image = null;

 			long len = 480 * 270 * 3;
 			byte[] bytes = null;
 			int count = 0;
 			
 			while (true) {
 				bytes = new byte[(int)len]; 
 				int offset = 0;
 				int numRead = 0;
 				while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
 					offset += numRead;
 				}
 				if (offset >= bytes.length && count >= startFrame && count <= endFrame ) {
 					
 					if (sample.contains(count)) {
	 					image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		 				
		 				int ind = 0;
		 				for(int y = 0; y < height; y++) {
		 					for(int x = 0; x < width; x++){
		 						byte a = 0;
		 						byte r = bytes[ind];
		 						byte g = bytes[ind+height*width];
		 						byte b = bytes[ind+height*width*2]; 
		 	
		 						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		 						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
		 						image.setRGB(x,y,pix);
		 						ind++;
		 					}
		 				}
	
		 				LogoDetector.saveImage(image, "temp");
		 				String sceneName = "temp.bmp";

		 		        Mat sceneImage = Highgui.imread(sceneName, Highgui.CV_LOAD_IMAGE_COLOR);

		 				int points = detect(sceneImage, count); //get points of each frame
		 				if (points <= threshold) {
		 					sample.remove(count);
		 				}
		 				
 					}
 					count++;
 				} else if (count > endFrame){
 					break;
 				} else {
 					count ++;
 					continue;
 				}
 			}


 		} catch (FileNotFoundException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
    	threshold++;
    }
	
	private void startDetect(Set<Integer> sample, int queueSize, String filename) {
    	
    	Queue<Frame> queue = new PriorityQueue<Frame>(queueSize, new Comparator<Frame>() {
    		public int compare(Frame a, Frame b) {
    			if(a.getSelfPonts() != b.getSelfPonts()) {
    				return a.getSelfPonts() - b.getSelfPonts();
    			}
    			if (queueSize <= 4) {
	    			if(a.getVariance() != b.getVariance()) {
	    				return a.getVariance() - b.getVariance() > 0 ? -1 : 1;
	    			}
    			}
    			if (a.getMean() != b.getMean()) {
    				return a.getMean() - b.getMean() > 0 ? 1 : -1;
    			}
    			return 0;
    		}
    	});
    	Set<Integer> newSample = new HashSet<Integer>();
    	Map<Integer, Frame> curMap = new HashMap<Integer, Frame>();
    	for (Integer num : sample) {
    		for (int i = -2; i <= 2; i++) {
    			newSample.add(num + i);
    			if (frameInfor.containsKey(num + i)) {
    				curMap.put(num + i, new Frame(frameInfor.get(num + i)));
    				continue;
    			} else {
    				Frame cur = new Frame(num + i);
    				curMap.put(num + i, cur);
    			}
    		}
    	}
    	System.out.println("queue size: "+ queueSize + ", sample size: " + newSample.size());
    	filter(newSample, filename);
    	try {
 			File file = new File(filename);
 			InputStream is = new FileInputStream(file);
 			BufferedImage image = null;

 			long len = 480 * 270 * 3;
 			byte[] bytes = null;
 			int count = 0;
 			
 			while (true) {
 				bytes = new byte[(int)len]; 
 				int offset = 0;
 				int numRead = 0;
 				while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
 					offset += numRead;
 				}
 				if (offset >= bytes.length && count >= startFrame && count <= endFrame ) {
 					List<Integer> addList = null;
 					if ((addList = exist(newSample, count)) != null) {
	 					image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		 				
		 				int ind = 0;
		 				for(int y = 0; y < height; y++) {
		 					for(int x = 0; x < width; x++){
		 						byte a = 0;
		 						byte r = bytes[ind];
		 						byte g = bytes[ind+height*width];
		 						byte b = bytes[ind+height*width*2]; 
		 	
		 						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		 						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
		 						image.setRGB(x,y,pix);
		 						ind++;
		 					}
		 				}
	
		 				LogoDetector.saveImage(image, "temp");
		 				String sceneName = "temp.bmp";

		 		        Mat sceneImage = Highgui.imread(sceneName, Highgui.CV_LOAD_IMAGE_COLOR);

		 				int points = detect(sceneImage, count); //get points of each frame
		 				updateFrame(addList, count, points, curMap);
 					}
 					count++;
 				} else if (count > endFrame){
 					break;
 				} else {
 					count++;
 					continue;
 				}
 			}


 		} catch (FileNotFoundException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
    	
    	updateMap(curMap, frameInfor);
    	for (Map.Entry<Integer, Frame> entry: curMap.entrySet()) {
    		if (queue.size() >= queueSize) {
    			Frame top = queue.peek();
    			Frame cur = entry.getValue();
    			if(queueSize <= 4) {
	    			if (top.compareHard(cur) < 0) {
	    				queue.poll();
	    				queue.offer(cur);
	    			}
    			} else {
    				if (top.compareSimple(cur) < 0) {
	    				queue.poll();
	    				queue.offer(cur);
	    			}
    			}
    		} else {
    			queue.offer(entry.getValue());
    		}
    	}
    	Set<Integer> nextSample = new HashSet<Integer>();
    	while (!queue.isEmpty()) {
    		nextSample.add(queue.poll().index);
    	}
    	if (nextSample.size() == 1) {
    		for (Integer num : nextSample) {
    			special = frameInfor.get(num);
    		}
    		return;
    	}
    	startDetect(nextSample, queueSize / 2, filename);
    	return;
    }
    
    private List<Integer> exist(Set<Integer> sample, int index) {
    	List<Integer> addList = new ArrayList<Integer>();
    	for (int i = -2; i <= 2; i++) {
    		if (sample.contains(index + i)) {
    			addList.add(index + i);
    		}
    	}
    	return addList.size() == 0? null : addList;
    }
    
    private void updateFrame(List<Integer> list, int index, int point, Map<Integer, Frame> curMap) {
    	for (Integer num : list) {
    		if (!curMap.containsKey(num)) {
    			curMap.put(num, new Frame(num));
    		}
    		Frame cur = curMap.get(num);
    		if (!cur.getNeigPoints().containsKey(index)) {
    			cur.getNeigPoints().put(index, point);
    		}
    		
//    		if (num == index) {
//    			curMap.get(num).setSelfPoints(point);
//    		}
    	}
    }
    
    private void updateMap(Map<Integer, Frame> curMap, Map<Integer, Frame> totalMap) {
    	for (Map.Entry<Integer, Frame> entry : curMap.entrySet()) {
    		Frame cur = entry.getValue();
    		cur.getMean();
//    		cur.getVariance();
    	}
    	totalMap.putAll(curMap);
    }
    
    public static void saveImage(BufferedImage img, String name) {
		try{
		      BufferedImage bi = img;
		      File f = new File(name + ".bmp");
		      ImageIO.write(bi, "BMP", f);
		  }
		  catch(Exception e){
		      e.printStackTrace();
		  }
		return;
	}
	
	
    public int detect(Mat image, int index) {

//      System.out.println("Started....");
//      System.out.println("Loading images...");
//      Mat objectImage = Highgui.imread(bookObject, Highgui.CV_LOAD_IMAGE_COLOR);
      Mat sceneImage = image;

      MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
      FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SIFT);
//      System.out.println("Detecting key points...");
      featureDetector.detect(objectImage, objectKeyPoints);
      KeyPoint[] keypoints = objectKeyPoints.toArray();
//      System.out.println(keypoints);

      MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
      DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
//      System.out.println("Computing descriptors...");
      descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);

      // Create the matrix for output image.
      Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
      Scalar newKeypointColor = new Scalar(255, 0, 0);

//      System.out.println("Drawing key points on object image...");
      Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);

      // Match object image with the scene image
      MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
      MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
//      System.out.println("Detecting key points in background image...");
      featureDetector.detect(sceneImage, sceneKeyPoints);
//      System.out.println("Computing descriptors in background image...");
      descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);

      Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
      Scalar matchestColor = new Scalar(0, 255, 0);

      List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
      DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
//       System.out.println("Matching object and scene images...");
      descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

//       System.out.println("Calculating good match list...");
      LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

      float nndrRatio = 0.7f;

      for (int i = 0; i < matches.size(); i++) {
          MatOfDMatch matofDMatch = matches.get(i);
          DMatch[] dmatcharray = matofDMatch.toArray();
          DMatch m1 = dmatcharray[0];
          DMatch m2 = dmatcharray[1];

          if (m1.distance <= m2.distance * nndrRatio) {
              goodMatchesList.addLast(m1);

          }
      }

      return goodMatchesList.size();
  }
  
    
    
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		//saveResult();
		new VideoInsertWorker(inputVideoFile, outputVideoFile, insertVideos, videoInsertIndexes).execute();
		new AudioInsertWorker(inputAudioFile, outputAudioFile, insertAudios, audioInsertIndexes).execute();
	}

	
	private void saveResult() {
		try {
 			File file = new File(inputVideoFile);
 			InputStream is = new FileInputStream(file);
 			BufferedImage image = null;

 			long len = 480 * 270 * 3;
 			byte[] bytes = null;
 			int count = 0;
 			
 			while (true) {
 				bytes = new byte[(int)len]; 
 				int offset = 0;
 				int numRead = 0;
 				while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
 					offset += numRead;
 				}
 				if (offset >= bytes.length && count >= startFrame && count <= endFrame ) {

 					if (videoInsertIndexes.containsKey(count)) {
	 					image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		 				
		 				int ind = 0;
		 				for(int y = 0; y < height; y++) {
		 					for(int x = 0; x < width; x++){
		 						byte a = 0;
		 						byte r = bytes[ind];
		 						byte g = bytes[ind+height*width];
		 						byte b = bytes[ind+height*width*2]; 
		 	
		 						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		 						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
		 						image.setRGB(x,y,pix);
		 						ind++;
		 					}
		 				}
	
		 				LogoDetector.saveImage(image, "temp");
		 				String sceneName = "temp.bmp";

		 		        Mat sceneImage = Highgui.imread(sceneName, Highgui.CV_LOAD_IMAGE_COLOR);

		 				specialDetect(sceneImage, count, videoInsertIndexes.get(count)); //get points of each frame
 					}
 					count++;
 				} else if (count > endFrame){
 					break;
 				} else {
 					count++;
 					continue;
 				}
 			}


 		} catch (FileNotFoundException e) {
 			e.printStackTrace();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
	}
	private void specialDetect(Mat sceneImage, int count, Integer integer) {
		// TODO Auto-generated method stub
    	String bookObject = detectLogoImages.get(integer);
        System.out.println("Started....");
        System.out.println("Loading images...");
        Mat objectImage = Highgui.imread(bookObject, Highgui.CV_LOAD_IMAGE_COLOR);

        MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SIFT);
        System.out.println("Detecting key points...");
        featureDetector.detect(objectImage, objectKeyPoints);
        KeyPoint[] keypoints = objectKeyPoints.toArray();
        System.out.println(keypoints);

        MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
        System.out.println("Computing descriptors...");
        descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);

        // Create the matrix for output image.
        Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
        Scalar newKeypointColor = new Scalar(255, 0, 0);

        System.out.println("Drawing key points on object image...");
        Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);

        // Match object image with the scene image
        MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
        MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
        System.out.println("Detecting key points in background image...");
        featureDetector.detect(sceneImage, sceneKeyPoints);
        System.out.println("Computing descriptors in background image...");
        descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);

        Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
        Scalar matchestColor = new Scalar(0, 255, 0);

        List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        System.out.println("Matching object and scene images...");
        descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

        System.out.println("Calculating good match list...");
        LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

        float nndrRatio = 0.7f;

        for (int i = 0; i < matches.size(); i++) {
            MatOfDMatch matofDMatch = matches.get(i);
            DMatch[] dmatcharray = matofDMatch.toArray();
            DMatch m1 = dmatcharray[0];
            DMatch m2 = dmatcharray[1];

            if (m1.distance <= m2.distance * nndrRatio) {
                goodMatchesList.addLast(m1);

            }
        }

//        if (goodMatchesList.size() >= 7) {
            System.out.println("Object Found !!!");
            List<KeyPoint> objKeypointlist = objectKeyPoints.toList();
            List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

            LinkedList<Point> objectPoints = new LinkedList<>();
            LinkedList<Point> scenePoints = new LinkedList<>();

            for (int i = 0; i < goodMatchesList.size(); i++) {
                objectPoints.addLast(objKeypointlist.get(goodMatchesList.get(i).queryIdx).pt);
                scenePoints.addLast(scnKeypointlist.get(goodMatchesList.get(i).trainIdx).pt);
            }

            MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
            objMatOfPoint2f.fromList(objectPoints);
            MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
            scnMatOfPoint2f.fromList(scenePoints);

            Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

            Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
            Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

            obj_corners.put(0, 0, new double[]{0, 0});
            obj_corners.put(1, 0, new double[]{objectImage.cols(), 0});
            obj_corners.put(2, 0, new double[]{objectImage.cols(), objectImage.rows()});
            obj_corners.put(3, 0, new double[]{0, objectImage.rows()});

//            System.out.println("Transforming object corners to scene corners...");
            Core.perspectiveTransform(obj_corners, scene_corners, homography);

            Mat img = sceneImage;

            Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);

//            System.out.println("Drawing matches image...");
            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(goodMatchesList);

            Features2d.drawMatches(objectImage, objectKeyPoints, sceneImage, sceneKeyPoints, goodMatches, matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);

            Highgui.imwrite("output//outputImage" + count + ".jpg", outputImage);
            Highgui.imwrite("output//matchoutput" + count + ".jpg", matchoutput);
            Highgui.imwrite("output//img" + count + ".jpg", img);
//        } else {
//            System.out.println("Object Not Found");
//        }

        System.out.println("Ended....");
		
	}
}

class VideoInsertWorker extends SwingWorker<Void, Void> {
	private String inputFile;
	private String outputFile;
	private List<String> insertVideos;
	private Map<Integer, Integer> insertIndexes;
	private int width;
	private int height;
	public VideoInsertWorker(String inputFile, String outputFile, List<String> insertVideos, Map<Integer, Integer> insertIndexes) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.insertVideos = insertVideos;
		this.insertIndexes = insertIndexes;
		this.width = MediaPlayer.width;
		this.height = MediaPlayer.height;
	}
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		File inFile = new File(inputFile);
    	InputStream inputStream = null;
    	FileOutputStream fos = null;
    	int len = width * height * 3;
    	
    	try {
        	inputStream = new FileInputStream(inFile);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	try {
    		fos = new FileOutputStream(outputFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	int count = 0;
    	while (true) {
			byte[] bytes = new byte[(int)len]; 
			int offset = 0;
			int numRead = 0;
			
			try {
				while (offset < bytes.length && (numRead= inputStream.read(bytes, offset, bytes.length-offset)) >= 0) {
					offset += numRead;
				}
				if (offset >= bytes.length) {
					count++;
					fos.write(bytes);
					if (!insertIndexes.containsKey(count)) {
						continue;
					} else {
				    	File insertFile = new File(insertVideos.get(insertIndexes.get(count)));
				    	InputStream insertStream = null;
			        	insertStream = new FileInputStream(insertFile);
			        	while (true) {
							byte[] insertBytes = new byte[(int)len]; 
							int insertOffset = 0;
							int insertNumRead = 0;
							try {
								while (insertOffset < insertBytes.length && (insertNumRead= insertStream.read(insertBytes, insertOffset, insertBytes.length - insertOffset)) >= 0) {
									insertOffset += insertNumRead;
								}
								if (insertOffset >= insertBytes.length) {
									fos.write(insertBytes);
								} else {
									break;
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							}	
						}
			        	insertStream.close();
					}
				} else {
					break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}	
		}

		inputStream.close();
		fos.close();
		return null;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		System.out.println("Video insertion has finished!");
		long endTime = System.currentTimeMillis();
        System.out.println("use: " + (endTime - LogoDetector.startTime) + " ms");
	}
	
	
}

class AudioInsertWorker extends SwingWorker<Void, Void> {
	private String inputFile;
	private String outputFile;
	private List<String> insertAudios;
	private Map<Integer, Integer> insertIndexes;
	public AudioInsertWorker(String inputFile, String outputFile, List<String> insertAudios, Map<Integer, Integer> insertIndexes) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.insertAudios = insertAudios;
		this.insertIndexes = insertIndexes;
	}
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		FileInputStream inputStream = null;
	    InputStream bufferedIn = null;
	    AudioInputStream audioInputStream = null;
	    
		try {
			inputStream = new FileInputStream(inputFile);
			bufferedIn = new BufferedInputStream(inputStream);
		    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e1) {
		    throw new PlayWaveException(e1);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
		
		File out = new File(outputFile);
	    AudioFormat audioFormat = audioInputStream.getFormat();
	    AudioInputStream audioInputStream_ForOutput = null;
		int readBytes = 0;
		int count = 0;
		int total = 0;
		int time = 0;
		try {
		    	byte[] audioBuffer = new byte[MediaPlayer.fileSize];
//		    	byte[] junkBuffer = new byte[PlaySound.EXTERNAL_BUFFER_SIZE];
		    	float[] buffer = new float[audioBuffer.length / 2];
		    	int offset = 0;
		    	while (readBytes != -1 && total < MediaPlayer.fileSize - PlaySound.EXTERNAL_BUFFER_SIZE * MediaPlayer.deleteAudio) {
		    		count++;
		    		readBytes = audioInputStream.read(audioBuffer, offset, PlaySound.EXTERNAL_BUFFER_SIZE);
					offset += readBytes; 
					total += readBytes;
					if (!insertIndexes.containsKey(count)) {
						continue;
					} else {
						FileInputStream insertStream = null;
					    InputStream insertBufferedIn = null;
					    AudioInputStream insertAudioInputStream = null;
						try {
						    insertStream = new FileInputStream(insertAudios.get(insertIndexes.get(count)));
//							insertStream = new FileInputStream(one);
						    insertBufferedIn = new BufferedInputStream(insertStream);
						    insertAudioInputStream = AudioSystem.getAudioInputStream(insertBufferedIn);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (UnsupportedAudioFileException e1) {
						    throw new PlayWaveException(e1);
						} catch (IOException e1) {
						    throw new PlayWaveException(e1);
						}
						
						try {
							int oldReadBytes = readBytes;
							readBytes = 0;
						    while (readBytes != -1) {
								
								readBytes = insertAudioInputStream.read(audioBuffer, offset, PlaySound.EXTERNAL_BUFFER_SIZE);
								offset += readBytes; 
//								total += readBytes;
						    }
						    offset += 1;
						    readBytes = oldReadBytes;
						} catch (IOException e1) {
							throw new PlayWaveException(e1);
						}
						insertAudioInputStream.close();
						insertBufferedIn.close();
						insertStream.close();
					}
		    	}
		    	System.out.println("time: " + time);
		    	ByteArrayInputStream bais = new ByteArrayInputStream(audioBuffer);
		    	audioInputStream_ForOutput = new AudioInputStream(bais, audioFormat, buffer.length);
		    	AudioSystem.write(audioInputStream_ForOutput, AudioFileFormat.Type.WAVE, out);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
		
		System.out.println("total: " + total);
		

		audioInputStream_ForOutput.close();
	    audioInputStream.close();
	    bufferedIn.close();
	    inputStream.close();

		return null;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		System.out.println("Audio insertion has finished!");
	}
}