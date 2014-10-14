package it.polito.cv.findmydoor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.opencv.ml.Ml;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
	private static final Scalar white = new Scalar(255, 255, 255);

	private Mat mRgba; // original image
	private Mat mEdit; // work image (canny)
	private Mat mReturn; // pointer to the image to show
	private Mat mLine; // founded edge image

	private List<Point> corners;

	private Size imgSize;
	private double imgDiag; // diagonal

	private static boolean freeze; // stop the image as is

	private CameraBridgeViewBase mOpenCvCameraView; // interface to camera

	private double dsRatio;

	private boolean willResize;

	private List<Door> doors;
	private List<Point> cornersList;
	private ArrayList<Line> lineList;

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
		case R.id.incr_fr:
			incrFR();
			return true;
		case R.id.decr_fr:
			decrFR();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void incrFR() {
		double incr = 0.5;
		Measure.FRThresL += incr;
		Measure.FRThresH += incr;
		Log.e(TAG, "Soglia FR: " + Measure.FRThresH);
	}

	private void decrFR() {
		double decr = -0.5;
		if (Measure.FRThresL >= decr) {
			Measure.FRThresL += decr;
			Measure.FRThresH += decr;
		}
	}

	private void incrDivariousCanny() {
		int decr = 10;
		if (Measure.cannyLowThres >= decr) {
			Measure.cannyUpThres += decr;
			Measure.cannyLowThres -= decr;
		}
	}

	private void decreaseCanny() {
		int decr = 20;
		if (Measure.cannyUpThres >= decr && Measure.cannyLowThres >= decr) {
			Measure.cannyUpThres -= decr;
			Measure.cannyLowThres -= decr;
		}
		Log.d(TAG_CANNY, "Canny Thres changed in: " + Measure.cannyLowThres);
	}

	private void increaseCanny() {
		int incr = 20;
		Measure.cannyUpThres += incr;
		Measure.cannyLowThres += incr;
		Log.d(TAG_CANNY, "Canny Thres changed in: " + Measure.cannyLowThres);
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
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this,
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
		willResize = !Measure.dsSize.equals(imgSize);

		dsRatio = imgDiag / Measure.diag;

		mRgba = new Mat(imgSize, CvType.CV_8UC4);
		corners = new ArrayList<Point>();

		Log.d(TAG, "width: " + height);
		Log.d(TAG, "width: " + width);
		freeze = false;
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	private Mat addBorder(Mat img, int borderSize) {
		// add an inner white border in order to create a corner
		// when the real one is occluded

		Size roiSize = new Size(img.width() - borderSize * 2, img.height()
				- borderSize * 2);
		Rect roi = new Rect(new Point(borderSize, borderSize), roiSize);

		Mat mCrop = img.submat(roi);

		Mat mBorder = img.clone();

		Imgproc.copyMakeBorder(mCrop, mBorder, borderSize, borderSize,
				borderSize, borderSize, Imgproc.BORDER_ISOLATED, white);

		return mBorder;
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		if (freeze) {
			Core.putText(mReturn, " FREEZED ", new Point(10, 10),
					Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255));
			return mReturn;
		}
		mRgba = inputFrame.rgba();
		mEdit = new Mat();
		corners.clear();

		// Smoothing
		Imgproc.GaussianBlur(mRgba, mEdit, Measure.kSize, Measure.sigmaX,
				Measure.sigmaY);

		// Down-sampling
		if (willResize) {
			Imgproc.resize(mEdit, mEdit, Measure.dsSize);
		}
		mEdit = addBorder(mEdit, 2);

		// Detecting edge
		Imgproc.Canny(mEdit, mEdit, Measure.cannyLowThres,
				Measure.cannyUpThres, Measure.apertureSize, false);

		// Detect lines
		mLine = new Mat();
		Imgproc.HoughLinesP(mEdit, mLine, Measure.houghRho, Measure.houghTheta,
				Measure.houghThreshold, Measure.houghMinLineSize,
				Measure.houghLineGap);

		lineList = matToListLines(mLine);
		// Detect corners
		cornersList = new ArrayList<Point>();

		mLine = Mat.zeros(mEdit.height(), mEdit.width(), CvType.CV_32FC1);
		Scalar white = new Scalar(255, 255, 255);
		int thickness = 1;

		for (int i = 0; i < lineList.size(); i++) {
			Line line1 = lineList.get(i);
			Point p1a = line1.start, p1b = line1.end;
			int x1 = (int) p1a.x, y1 = (int) p1a.y, x2 = (int) p1b.x, y2 = (int) p1b.y;

			Core.line(mLine, p1a, p1b, white, thickness);

			for (int j = i + 1; j < lineList.size(); j++) {
				Line line2 = lineList.get(j);
				Point p2a = line2.start, p2b = line2.end;
				int x3 = (int) p2a.x, y3 = (int) p2a.y, x4 = (int) p2b.x, y4 = (int) p2b.y;

				float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

				if (d != 0) {
					int ix = (int) (((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2)
							* (x3 * y4 - y3 * x4)) / d);
					int iy = (int) (((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2)
							* (x3 * y4 - y3 * x4)) / d);

					if (ix > 0 && ix < mEdit.width() && iy > 0
							&& iy < mEdit.height()) {
						cornersList.add(new Point(ix, iy));
					}
				}

			}
		}

		int cornersSize = cornersList.size();

		// Detect Doors
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

		// Collections.sort(doors);

		// Up-sampling mEdit (edge image)
		if (willResize) {
			// down-sampling points position
			for (Point c : cornersList) {
				c.x = dsRatio * (c.x);
				c.y = dsRatio * (c.y);
			}

			for (Line l : lineList) {
				Point[] pts = { l.start, l.end };
				for (Point p : pts) {
					p.x = dsRatio * (p.x);
					p.y = dsRatio * (p.y);
				}
			}

			Imgproc.resize(mEdit, mEdit, imgSize);

		}

		mReturn = mEdit;

		return printMat(mReturn);
	}

	private ArrayList<Line> matToListLines(Mat src) {
		ArrayList<Line> dstList = new ArrayList<Line>();

		for (int x = 0; x < src.cols(); x++) {
			double[] vec = src.get(0, x);
			double x1 = vec[0], y1 = vec[1], x2 = vec[2], y2 = vec[3];

			Point start = new Point(x1, y1);
			Point end = new Point(x2, y2);

			try {
				dstList.add(new Line(start, end));
			} catch (RuntimeException e) {
				// do nothing
			}
		}

		return dstList;
	}

	private Mat printMat(Mat img) {
		if (img.type() != mRgba.type()) {
			Imgproc.cvtColor(img, img, Imgproc.COLOR_GRAY2RGBA);
		}

		for (int i = 0; i < lineList.size(); i++) {
			Line line = lineList.get(i);
			Point p1 = line.start;
			Point p2 = line.end;

			Scalar yellow = new Scalar(0, 255, 255);
			Core.line(img, p1, p2, yellow, 3);
		}

		// if (doors.size() > 0) {
		// // disegna la porta più probabile
		// drawDoor(img, doors.get(0));
		// }
		for (Door door : doors) {
			drawDoor(img, door);
		}

		// Draw Corners
		for (Point c : cornersList) {
			Core.circle(img, c, 15, new Scalar(255, 0, 0), 2, 8, 0);
			Core.putText(mRgba, " " + cornersList.indexOf(c), c,
					Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255));
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

		try {
			detectedDoor = new Door(p1, p2, p3, p4);
		} catch (RuntimeException re) {
			// do nothing
			return null;
		}

		// Compare with edge img
		// double FR12 = calculateFillRatio(detectedDoor.getP1(),
		// detectedDoor.getP2());
		//
		// if (FR12 < Measure.FRThresL) {
		// return null;
		// }
		// // Log.d(TAG, "fill ratio 12: " + FR12);
		//
		// double FR23 = calculateFillRatio(detectedDoor.getP2(),
		// detectedDoor.getP3());
		//
		// if (FR23 < Measure.FRThresL) {
		// return null;
		// }
		//
		// double FR34 = calculateFillRatio(detectedDoor.getP3(),
		// detectedDoor.getP4());
		//
		// if (FR34 < Measure.FRThresL) {
		// return null;
		// }
		//
		// double FR41 = calculateFillRatio(detectedDoor.getP4(),
		// detectedDoor.getP1());
		// // Log.d(TAG, "fillRatio41: " + FR41);
		//
		// if (FR41 < Measure.FRThresL) {
		// return null;
		// }
		//
		// double avgFR = (FR12 + FR23 + FR34 + FR41) / 4;
		// Log.w(TAG, "AVGfr: "+avgFR);
		// if (avgFR < Measure.FRThresH)
		// return null;
		//
		// detectedDoor.setAvgFillRatio(avgFR);

		return detectedDoor;
	}

	private double calculateFillRatio(Point pA, Point pB) {
		Mat lineImg = Mat.zeros(mEdit.height(), mEdit.width(), CvType.CV_32FC1);

		Scalar white = new Scalar(255, 255, 255);
		int thickness = 2;
		Core.line(lineImg, pA, pB, white, thickness);

		int overLapAB = 0, lenghtAb = 0; // length white px in this line

		double linePx = 0, oldLinePx;
		// oldpix = previous px

		Size roiSize = new Size(Math.abs(pA.x - pB.x), Math.abs(pA.y - pB.y));
		Rect roi = new Rect(new Point(Math.min(pA.x, pB.x),
				Math.min(pA.y, pB.y)), roiSize);

		Mat lineCrop = lineImg.submat(roi);
		Mat allLinesCrop = mLine.submat(roi);

		for (int i = 0; i < roiSize.width; i++) {
			for (int j = 0; j < roiSize.height; j++) {

				oldLinePx = linePx;
				linePx = lineCrop.get(j, i)[0];
				if (linePx == 0) {
					if (oldLinePx != 0) // border passed
						break;
					else
						continue;
				}
				lenghtAb++;

				if (allLinesCrop.get(j, i)[0] != 0) {
					overLapAB++;
				}
			}
		}

		lineCrop.release();
		allLinesCrop.release();

		if (overLapAB == 0) {
			return 0;
		}

		double fillRatio = (double) overLapAB / (lenghtAb / thickness);
		if (fillRatio > 0.3) {
			Log.w(TAG, "overLap :" + fillRatio);
			Core.line(mReturn, pA, pB, white, 6);
		}

		return fillRatio;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		freeze = !freeze;
		return false;
	}

}