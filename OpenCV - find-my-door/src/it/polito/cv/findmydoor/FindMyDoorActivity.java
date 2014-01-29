package it.polito.cv.findmydoor;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

public class FindMyDoorActivity extends Activity implements
		CvCameraViewListener2, OnTouchListener {
	private static final String TAG = "OCV::Activity";
	private static final String TAG_CANNY = "OCV::Activity CANNY";


	private Mat mRgba; // immagine originale
	private Mat mEdit; // immagine modificata (canny)
	private Mat mReturn; // puntatore all'immagine da visualizzare
	private List<Point> corners;

	private Size imgSize;
	private double imgDiag; // diagonale

	private static final Size dsSize = new Size(320, 240); // dimensione finale
	private static final double dsDiag = 400;


	private static boolean freeze;

	double heightThresL, heightThresH, widthThresL, widthThresH;
	int dirThresL, dirThresH, parallelThres;
	double HWThresL, HWThresH;

	private int cannyLowThres = 70, cannyUpThres = 120;

	private CameraBridgeViewBase mOpenCvCameraView; // collegamento alla camera

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(FindMyDoorActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	private double dsRatio;

	private boolean willResize;

	private List<Door> doors;

	private List<Point> cornersList;

	public FindMyDoorActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.find_my_door_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.find_my_door_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.more_canny:
			increaseCanny();
			return true;
		case R.id.less_canny:
			decreaseCanny();
			return true;
		case R.id.incr_divarious_canny:
			incrDivariousCanny();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void incrDivariousCanny() {
		int decr = 10;
		if (cannyLowThres >= decr) {
			cannyUpThres += decr;
			cannyLowThres -= decr;
		}
	}

	private void decreaseCanny() {
		int decr = 20;
		if (cannyUpThres >= decr && cannyLowThres >= decr) {
			cannyUpThres -= decr;
			cannyLowThres -= decr;
		}
		Log.d(TAG_CANNY, "Canny Thres changed in: "+cannyLowThres);
	}

	private void increaseCanny() {
		int incr = 20;
		cannyUpThres += incr;
		cannyLowThres += incr;
		Log.d(TAG_CANNY, "Canny Thres changed in: "+cannyLowThres);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		imgSize = new Size(width, height);
		imgDiag = Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2));
		willResize = !dsSize.equals(imgSize);
		Log.i(TAG, "size: " + imgSize.width + " x " + imgSize.height);
		dsRatio = imgDiag / dsDiag;

		mRgba = new Mat(imgSize, CvType.CV_8UC4);
		mReturn = mRgba;
		corners = new ArrayList<Point>();

		Log.d(TAG, "width: " + height);
		Log.d(TAG, "width: " + width);
		freeze = false;
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		if (freeze) {
			Core.putText(mReturn, " FREEZED ", new Point(10, 10),
					Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255));
			return mReturn;
		}
		mRgba = inputFrame.rgba();
		mEdit = new Mat();
		mReturn = mEdit;
		corners.clear();

		// prova cattura resScreen
		Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
//		display.getSize(size);
//		int wScreen = size.x;
//		int hScreen = size.y;
		// final Size dsSize = new Size(wScreen, hScreen);

		// Smoothing
		Size kSize = new Size(5, 5);
		double sigmaX = 2.5, sigmaY = 2.5;
		Imgproc.GaussianBlur(mRgba, mEdit, kSize, sigmaX, sigmaY);

		// Down-sampling
		if (willResize) {
			Imgproc.resize(mEdit, mEdit, dsSize);
		}

		// Detecting edge
		Imgproc.Canny(mEdit, mEdit, cannyLowThres, cannyUpThres, 3, false);

		// Harris Detector parameters
		int blockSize = 5;
		int apertureSize = 5;
		double k = 0.04;

		// Detecting corners with Shi-Tomasi algorithm
		int maxCorners = 50;

		if (maxCorners < 1) {
			maxCorners = 1;
		}

		double qualityLevel = 0.01;
		double minDistance = 40;
		int blockSize1 = 5;
		boolean useHarrisDetector = false;
		double k1 = 0.04;

		MatOfPoint corners = new MatOfPoint();
		// Apply corner detection

		int borderSize = 2;
		Mat mEditBorder = new Mat(new Size(mEdit.height() + borderSize * 2,
				mEdit.width() + borderSize * 2), mEdit.type());

		Imgproc.copyMakeBorder(mEdit, mEditBorder, borderSize, borderSize,
				borderSize, borderSize, Imgproc.BORDER_CONSTANT, new Scalar(
						255, 255, 255));

		Imgproc.goodFeaturesToTrack(mEditBorder, corners, maxCorners,
				qualityLevel, minDistance, new Mat(), blockSize1,
				useHarrisDetector, k1);

		cornersList = corners.toList();
		int cornersSize = cornersList.size();
		// Reset points position (with no border)
		for (Point c : cornersList) {
			c.x = (c.x - borderSize);
			if (c.x > mEdit.cols()) {
				c.x = mEdit.cols();
			} else if (c.x < 0) {
				c.x = 0;
			}
			c.y = (c.y - borderSize);
			if (c.y > mEdit.rows()) {
				c.y = mEdit.rows();
			} else if (c.y < 0) {
				c.y = 0;
			}
		}

		// TODO trasformare in costanti (quando saranno definitive)
		heightThresL = 0.3; // 50% of camera diag
		heightThresH = 0.6; // 80% of camera diag
		widthThresL = 0.1; // 10% of camera diag
		widthThresH = 0.8; // 80% of camera diag

		dirThresL = 40;
		dirThresH = 80;
		parallelThres = 3;

		// HWThresL = 1.2;
		// HWThresH = 2.4;

		HWThresL = 2.0;
		HWThresH = 3.0;

		doors = new ArrayList<Door>();

		// For each point
		for (int i = 0; i < cornersSize; i++) {
			Point p1 = cornersList.get(i);
			// Consider each successive point
			for (int j = i + 1; j < cornersSize; j++) {
				Point p2 = cornersList.get(j);

				// and so on with 3rd and 4th points
				for (int l = j + 1; l < cornersSize; l++) {
					Point p3 = cornersList.get(l);

					for (int m = l + 1; m < cornersSize; m++) {
						Point p4 = cornersList.get(m);

						Door newDoor = doorDetect(p1, p2, p3, p4);
						if (newDoor != null) {
							Log.d(TAG, "Door found!");
							doors.add(newDoor);
						}
					}
				}
			}
		}

		// Up-sampling mEdit (edge image)
		if (willResize) {
			// down-sampling points position
			for (Point c : cornersList) {
				c.x = dsRatio * (c.x);
				c.y = dsRatio * (c.y);
			}

			Imgproc.resize(mEdit, mEdit, imgSize);
		}

		return printMat(mReturn);
	}

	private Mat printMat(Mat img) {
		Imgproc.cvtColor(img, img, Imgproc.COLOR_GRAY2RGBA);
		for (Door door : doors) {
			drawDoor(img, door);
		}

		// Draw Corners
		for (Point c : cornersList) {
			Core.circle(img, c, 15, new Scalar(255, 0, 0), 2, 8, 0);
			// Core.putText(mRgba, " "+cornersList.indexOf(c), c,
			// Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0,0,255));
		}

		return img;
	}

	private void drawDoor(Mat image, Door door) {
		Scalar doorColor = new Scalar(0, 255, 0);
		Core.line(image, door.getP1(), door.getP2(), doorColor, 4);
		Core.line(image, door.getP2(), door.getP3(), doorColor, 4);
		Core.line(image, door.getP3(), door.getP4(), doorColor, 4);
		Core.line(image, door.getP4(), door.getP1(), doorColor, 4);
	}

	/*
	 * Check if one of rectangles formed by points p1,p2,p3,p4 is a door. The
	 * four point can be not in order.
	 */
	private Door doorDetect(Point p1, Point p2, Point p3, Point p4) {
		Door detectedDoor = null;

		if (checkGeometry(p1, p2, p3, p4)) {
			detectedDoor = new Door(p1, p2, p3, p4);
		} else if (checkGeometry(p1, p3, p2, p4)) {
			// commute long side points
			detectedDoor = new Door(p1, p3, p2, p4);
		} else if (checkGeometry(p1, p2, p4, p3)) {
			// commute short side points
			detectedDoor = new Door(p1, p2, p4, p3);
		}

		// TODO rimuovi il true
		if (true && detectedDoor != null) {
			// TODO trasformare in costanti
			double FRThresL = 0.3, FRThresH = 0.4;

			// Compare with edge img
			double FR12 = calculateFillRatio(detectedDoor.getP1(),
					detectedDoor.getP2());

			if (FR12 < FRThresL) {
				return null;
			}
			Log.d(TAG, "fill ratio 12: " + FR12);

			double FR23 = calculateFillRatio(detectedDoor.getP2(),
					detectedDoor.getP3());

			if (FR23 < FRThresL) {
				return null;
			}

			double FR34 = calculateFillRatio(detectedDoor.getP3(),
					detectedDoor.getP4());

			if (FR34 < FRThresL) {
				return null;
			}

			double FR41 = calculateFillRatio(detectedDoor.getP4(),
					detectedDoor.getP1());
			Log.d(TAG, "fillRatio41: " + FR41);

			if (FR41 < FRThresL) {
				return null;
			}

			double avgFR = (FR12 + FR23 + FR34 + FR41) / 4;
			if (avgFR < FRThresH)
				return null;

		}

		return detectedDoor;
	}

	private double calculateFillRatio(Point pA, Point pB) {
		Mat lineImg = Mat.zeros(mEdit.height(), mEdit.width(), CvType.CV_32FC1);

		Scalar white = new Scalar(255, 255, 255);
		int thickness = 5;
		Core.line(lineImg, pA, pB, white, thickness);

		int overLapAB = 0, lenghtAb = 0; // lenght px bianchi nella linea che ho
											// disegnato

		double linePx = 0, oldLinePx;
		// oldpix = px precedente a quello che sto considerando

		Size roiSize = new Size(Math.abs(pA.x - pB.x), Math.abs(pA.y - pB.y));
		Rect roi = new Rect(new Point(Math.min(pA.x, pB.x),
				Math.min(pA.y, pB.y)), roiSize);

		Mat lineCrop = lineImg.submat(roi);

		Mat editCrop = mEdit.submat(roi);

		for (int i = 0; i < roiSize.width; i++) {
			for (int j = 0; j < roiSize.height; j++) {

				oldLinePx = linePx;
				linePx = lineCrop.get(j, i)[0];
				if (linePx == 0) {
					if (oldLinePx != 0) // ho già passato il bordo
						break;
					else
						continue;
				}
				lenghtAb++;

				if (editCrop.get(j, i)[0] != 0) {
					overLapAB++;
				}
			}
		}

		lineCrop.release();
		editCrop.release();

		if (overLapAB == 0) {
			return 0;
		}

		double fillRatio = (double) overLapAB / (lenghtAb / thickness);
		if (fillRatio > 0.3) {
			Log.w(TAG, "overLap :" + fillRatio);
			Core.line(mReturn, pA, pB, white, 4);
		}

		return fillRatio;
	}

	/*
	 * Check if the rectangle p1-p2-p3-p4 is a door. The four point are in
	 * order.
	 */
	private boolean checkGeometry(Point p1, Point p2, Point p3, Point p4) {
		double siz12 = calcRelDistance(p1, p2);
		double siz41 = calcRelDistance(p1, p4);

		if (siz12 < siz41) {
			// commute the sides
			return checkGeometry(p2, p3, p4, p1);
		}

		if (siz12 < heightThresL || siz41 > widthThresH) {
			return false;
		}

		double dir12 = calcDirection(p1, p2);
		double dir41 = calcDirection(p1, p4);

		if (dir12 < dirThresH || dir41 > dirThresL) {
			return false;
		}

		double siz23 = calcRelDistance(p2, p3);
		double dir23 = calcDirection(p2, p3);
		double siz34 = calcRelDistance(p3, p4);
		double dir34 = calcDirection(p3, p4);

		if (siz34 < heightThresL || siz23 < widthThresL || dir23 > dirThresL
				|| dir34 < dirThresH || Math.abs(dir12 - dir34) > parallelThres) {
			return false;
		}

		double sizRatio = (siz12 + siz34) / (siz23 + siz41);

		if (sizRatio < HWThresL || sizRatio > HWThresH) {
			return false;
		}

		// if here, 1234 is a door
		return true;
	}

	/*
	 * Calculate the relative distance between two points.
	 * 
	 * @return the distance within a range of [0,1]
	 */
	private double calcRelDistance(Point i, Point j) {
		return calcDistance(i, j) / dsDiag;
	}

	/*
	 * Calculate the absolute distance between two points.
	 * 
	 * @return the distance
	 */
	private double calcDistance(Point i, Point j) {
		double sizX = Math.pow((i.x - j.x), 2);
		double sizY = Math.pow((i.y - j.y), 2);
		return Math.sqrt(sizX + sizY);
	}

	private double calcDirection(Point i, Point j) {
		double dfX = i.x - j.x;
		double dfY = i.y - j.y;
		double dfRatio = Math.abs(dfX / dfY);
		return Math.atan(dfRatio) * 180 / Math.PI;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		freeze = !freeze;
		return false;
	}

}