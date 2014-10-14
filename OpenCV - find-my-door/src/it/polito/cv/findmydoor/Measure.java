package it.polito.cv.findmydoor;

import org.opencv.core.Size;

public class Measure {
	
	/************ IMAGE *******************/
	
	// work image dimensions
	public static final Size dsSize = new Size(320, 240); // final dimension
	public static final double diag = 400;

	// Params Gaussian Blur
	public static Size kSize = new Size(9, 9);
	public static double sigmaX = 2.5, sigmaY = 2.5;
	
	// Params Canny
	public static int cannyLowThres = 70, cannyUpThres = 120;
	public static int apertureSize = 3; // This must be odd

	// Params Hough Line Transform
	public static int houghRho = 1;
	public static double houghTheta = Math.PI/180;
	public static int houghThreshold = 80;
	public static int houghMinLineSize = 40;
	public static int houghLineGap = 20;

	
	/************ CORNERS DETECTION *******************/

	// Params Shi Tomasi
	public static int maxCorners = 50;
	public static double qualityLevel = 0.2;
	public static double minDistance = 40;
	public static int blockSize1 = 10;
	public static boolean useHarrisDetector = false;
	public static double k1 = 0.04;
	
	
	/************ DOORS DETECTION *******************/
	
	// Lines
	public static double maxPointGap = 0.01; // 1% of camera diag (4px)
	
	// Params geometrici
	public static double heightThresL = 0.3; // 50% of camera diag
	public static double heightThresH = 0.7; // 80% of camera diag
	public static double widthThresL = 0.1; // 10% of camera diag
	public static double widthThresH = 0.8; // 80% of camera diag

	public static int dirThresL = 40;
	public static int dirThresH = 80;
	public static int parallelThres = 3;

	public static double HWThresL = 2.0;
	public static double HWThresH = 3.0;
	
	// Params Fill Ratio
	public static double FRThresL = 0.1;
	public static double FRThresH = 0.3;
}
