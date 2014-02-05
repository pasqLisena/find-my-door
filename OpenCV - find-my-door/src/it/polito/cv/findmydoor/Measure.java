package it.polito.cv.findmydoor;

import org.opencv.core.Size;

public class Measure {
	// IMAGE
	
	// Dimensioni immagine di lavoro
	public static final Size dsSize = new Size(320, 240); // dimensione finale
	public static final double diag = 400;

	// Parametri Gaussian Blur
	public static Size kSize = new Size(5, 5);
	public static double sigmaX = 2.5, sigmaY = 2.5;
	
	// Parametri Canny
	public static int cannyLowThres = 70, cannyUpThres = 120;
	public static int apertureSize = 3; // This must be odd

	
	// CORNERS DETECTION
	
	// Parametri Shi Tomasi
	public static int maxCorners = 50;
	public static double qualityLevel = 0.2;
	public static double minDistance = 40;
	public static int blockSize1 = 25;
	public static boolean useHarrisDetector = false;
	public static double k1 = 0.04;

	
	// DOOR DETECTION
	
	// Parametri geometrici
	public static double heightThresL = 0.3; // 50% of camera diag
	public static double heightThresH = 0.6; // 80% of camera diag
	public static double widthThresL = 0.1; // 10% of camera diag
	public static double widthThresH = 0.8; // 80% of camera diag

	public static int dirThresL = 40;
	public static int dirThresH = 80;
	public static int parallelThres = 3;

	public static double HWThresL = 2.0;
	public static double HWThresH = 3.0;
	
	// Parametri Fill Ratio
	public static double FRThresL = 0.4;
	public static double FRThresH = 0.9;
}