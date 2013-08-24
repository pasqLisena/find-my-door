package it.polito.cv.findmydoor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import it.polito.cv.findmydoor.R;
import android.R.integer;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class FindMyDoorActivity extends Activity implements
		CvCameraViewListener2 {
	private static final String TAG = "OCV::Activity";

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Mat mEdit;
	private Mat mDst;
	private Mat mDstSc;
	private List<Point> corners;

	double heightThresL, heightThresH, widthThresL, widthThresH;
	int dirThresL, dirThresH, parallelThres;
	double HWThresL, HWThresH;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				// mOpenCvCameraView.setOnTouchListener(FindMyDoorActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	private double imgDiag;

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
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		corners = new ArrayList<Point>();

		imgDiag = Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2));
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mEdit = new Mat();
		mDst = new Mat(mRgba.size(), CvType.CV_32FC1);
		corners.clear();

		// Smoothing
		Size kSize = new Size(5, 5);
		double sigmaX = 2.5, sigmaY = 2.5;
		Imgproc.GaussianBlur(mRgba, mEdit, kSize, sigmaX, sigmaY);

		// Down-sampling
		int newHeight = mRgba.height() / 2;
		int newWidth = mRgba.width() / 2;
		Imgproc.pyrDown(mEdit, mEdit, new Size(newWidth, newHeight));

		// Detecting edge
		int lowThres = 70, upThres = 80;
		Imgproc.Canny(mEdit, mEdit, lowThres, upThres, 3, true);

		// Harris Detector parameters
		int blockSize = 5;
		int apertureSize = 5;
		double k = 0.04;

		// Detecting corners with Shi-Tomasi algorithm
		int maxCorners = 25;
		
		if (maxCorners < 1) {
			maxCorners = 1;
		}

		double qualityLevel = 0.01;
		double minDistance = 10;
		int blockSize1 = 5;
		boolean useHarrisDetector = false;
		double k1 = 0.04;

		MatOfPoint corners = new MatOfPoint();
		// Apply corner detection

		int borderSize = 10;
		Mat mEditBorder = new Mat(new Size(mEdit.height() + borderSize * 2,
				mEdit.width() + borderSize * 2), mEdit.type());

		Imgproc.copyMakeBorder(mEdit, mEditBorder, borderSize, borderSize,
				borderSize, borderSize, Imgproc.BORDER_CONSTANT, new Scalar(255,
						255, 255));

		Imgproc.goodFeaturesToTrack(mEditBorder, corners, maxCorners, qualityLevel,
				minDistance, new Mat(), blockSize1, useHarrisDetector, k1);


		// TODO trasformare in costanti (quando saranno definitive)
		// heightThresL = 0.5; // 50% of camera height
		// heightThresH = 0.9; // 80% of camera height
		// widthThresL = 0.1; // 10% of camera width
		// widthThresH = 0.8; // 80% of camera width
		//
		// dirThresL = 35;
		// dirThresH = 80;
		// parallelThres = 6;
		//
		// HWThresL = 2.0;
		// HWThresH = 3.0;
		//
		// int cornersSize = corners.size();
		// Door door = null;
		//
		// // For each point
		// for (int i = 0; i < cornersSize; i++) {
		// Point p1 = corners.get(i);
		// // Consider each successive point
		// for (int j = i + 1; j < cornersSize; j++) {
		// Point p2 = corners.get(j);
		//
		// double siz12 = calcRelDistance(p1, p2);
		// double dir12 = calcDirection(p1, p2);
		//
		// if (siz12 < heightThresL || dir12 < dirThresH)
		// continue;
		//
		// // and so on with 3rd and 4th points
		// // TODO sto considerando una porta DRITTA: valutare la
		// // prospettiva
		// for (int l = j + 1; l < cornersSize; l++) {
		// Point p4 = corners.get(l);
		//
		// double siz41 = calcRelDistance(p1, p4);
		// double dir41 = calcDirection(p1, p4);
		//
		// if (siz41 > widthThresH || dir41 > dirThresL)
		// continue;
		//
		// for (int m = l + 1; m < cornersSize; m++) {
		// Point p3 = corners.get(m);
		//
		// double siz23 = calcRelDistance(p2, p3);
		// double dir23 = calcDirection(p2, p3);
		// double siz34 = calcRelDistance(p3, p4);
		// double dir34 = calcDirection(p3, p4);
		//
		// if (siz34 > heightThresL || siz23 < widthThresL
		// || dir23 > dirThresL || dir34 < dirThresH
		// || Math.abs(dir12 - dir34) > parallelThres)
		// continue;
		//
		// double sizRatio = (siz12 + siz34) / (siz23 + siz34);
		// if (sizRatio < HWThresL || sizRatio > HWThresH)
		// continue;
		//
		// door = new Door(p1, p2, p3, p4);
		// // if here, 1234 is a rectangle
		// }
		// }
		// }
		// }
		//

		
//		Rect roi = new Rect(borderSize, borderSize, mEdit.width(), mEdit.height());
//		mEdit = new Mat(mEditBorder, roi);
//
//		Imgproc.pyrUp(mEdit, mEdit, new Size(mRgba.width(), mRgba.height()));

		// if (door != null) {
		// Core.line(mRgba, door.getP1(), door.getP2(), new Scalar(0, 255, 0),
		// 4);
		// Core.line(mRgba, door.getP2(), door.getP3(), new Scalar(0, 255, 0),
		// 4);
		// Core.line(mRgba, door.getP3(), door.getP4(), new Scalar(0, 255, 0),
		// 4);
		// Core.line(mRgba, door.getP4(), door.getP1(), new Scalar(0, 255, 0),
		// 4);
		// }

		// Draw Corners
		for (Point c : corners.toList()) {
			Point temp = c.clone();
			temp.x = 2*(temp.x-borderSize);
			temp.y = 2*(temp.y-borderSize);
			Core.circle(mRgba, temp, 5, new Scalar(255, 0, 0), 2, 8, 0);
		}
		
		return mRgba;
	}

	/*
	 * Calculate the relative distance between two points.
	 * 
	 * @return the distance within a range of [0,1]
	 */
	private double calcRelDistance(Point i, Point j) {
		double sizX = Math.pow((i.x - j.x), 2);
		double sizY = Math.pow((i.y - j.y), 2);
		return Math.sqrt(sizX + sizY) / imgDiag;
	}

	private double calcDirection(Point i, Point j) {
		double dfX = Math.abs(i.x - j.x);
		double dfY = Math.abs(i.y - j.y);
		double dfRatio = dfX / dfY;
		return Math.atan(dfRatio * 180 / Math.PI);
	}

}