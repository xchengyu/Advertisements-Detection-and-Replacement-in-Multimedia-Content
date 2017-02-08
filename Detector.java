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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class Detector {
	public static String bookObject = null;
    public static Mat objectImage = null;
    public static Map<Double, Integer> data = new HashMap<Double, Integer>();
    public static List<Integer> point = new ArrayList<Integer>();
    public static void main(String[] args) {

        File lib = null;
        String os = System.getProperty("os.name");
        String bitness = System.getProperty("sun.arch.data.model");
        int height = MediaPlayer.height;
        int width = MediaPlayer.width;
        if (os.toUpperCase().contains("WINDOWS")) {
            if (bitness.endsWith("64")) {
                lib = new File("libs//x64//" + System.mapLibraryName("opencv_java2411"));
            } else {
                lib = new File("libs//x86//" + System.mapLibraryName("opencv_java2411"));
            }
        }

        System.out.println(lib.getAbsolutePath());
        System.load(lib.getAbsolutePath());
        List<Double> time = new ArrayList<Double>();
        bookObject = "images//starbucks_logo.bmp";
        objectImage = Highgui.imread(bookObject, Highgui.CV_LOAD_IMAGE_COLOR);
        
        try {
 			File file = new File(args[0]);
 			InputStream is = new FileInputStream(file);
 			BufferedImage image = null;
 			//long len = file.length();
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
 				if (offset >= bytes.length && count <= 5850 && count >= 4050) {
	 				image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	 				count++;
	 				
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
//	 				if (count == 753) {
//	 					Detector.saveImage(image, "753");
//	 				}
//	 				if (count == 1510) {
//	 					Detector.saveImage(image, "1510");
//	 				}
	 				Detector.saveImage(image, "temp");
	 				String sceneName = "temp.bmp";
//	 				Mat sceneImage = Highgui.imread(sceneName);
	 		        Mat sceneImage = Highgui.imread(sceneName, Highgui.CV_LOAD_IMAGE_COLOR);
//	 				Mat sceneImage = matify(image);
	 				detect(sceneImage, count, time);
 				} else if (count > 5850){
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
        for (Double num : time) {
          System.out.println("time: " + (int)(num / 60) + " min " + num % 60 +  " s, count: " + data.get(num));
        }
        System.out.println("----------------------------------------------------------------------");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("----------------------------------------------------------------------");
        for (Integer num: point) {
        	
        	System.out.println("Index: " + num + ", time: " + (num / 1800) + " min " + ((num % 1800) / 1800.0) * 60.0 +  " s");
        }
    }
    
    private static void saveImage(BufferedImage img, String name) {
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
    
    public static Mat matify(BufferedImage im) {
    	int[] data = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4); 
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(data);

        Mat mat = new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, byteBuffer.array());
        return mat;

    }
    
    public static void detect(Mat image, int index, List<Double> time) {
//    	String bookObject = "images//starbucks_logo.bmp";
//
        System.out.println("Started....");
        System.out.println("Loading images...");
//        Mat objectImage = Highgui.imread(bookObject, Highgui.CV_LOAD_IMAGE_COLOR);
        Mat sceneImage = image;

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

        if (goodMatchesList.size() >= 7) {
            System.out.println("Object Found in " + index + " frame!!!");
            data.put(((double)index / 30.0), goodMatchesList.size());
            point.add(index);
//            System.out.println("time: " + ((double)index / 30.0) + "s");
            time.add(((double)index / 30.0));
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

            Mat img = image;

            Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
            Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);

//            System.out.println("Drawing matches image...");
            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(goodMatchesList);

            Features2d.drawMatches(objectImage, objectKeyPoints, sceneImage, sceneKeyPoints, goodMatches, matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);

            Highgui.imwrite("output//outputImage" + index + ".jpg", outputImage);
            Highgui.imwrite("output//matchoutput" + index + ".jpg", matchoutput);
            Highgui.imwrite("output//img" + index + ".jpg", img);
        } else {
            System.out.println("Object Not Found");
        }

        System.out.println("Ended....");
    }
}
