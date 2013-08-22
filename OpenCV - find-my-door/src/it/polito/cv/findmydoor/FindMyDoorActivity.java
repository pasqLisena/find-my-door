package it.polito.cv.findmydoor;

import java.text.Normalizer;
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
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import it.polito.cv.findmydoor.R;
import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.BoringLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class FindMyDoorActivity extends Activity implements OnTouchListener,
		CvCameraViewListener2 {
	private static final String TAG = "OCV::Activity";

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Mat mEdit;
	private Mat mDst;
	private Mat mDstSc;
	private int thresh = 200;

	private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;
	private DoorDetector mDetector;
	private Mat mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar CONTOUR_COLOR;

	private CameraBridgeViewBase mOpenCvCameraView;

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
		mDetector = new DoorDetector();
		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	public boolean onTouch(View v, MotionEvent event) {
//		int cols = mRgba.cols();
//		int rows = mRgba.rows();
//
//		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
//		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
//
//		int x = (int) event.getX() - xOffset;
//		int y = (int) event.getY() - yOffset;
//
//		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
//
//		if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
//			return false;
//
//		Rect touchedRect = new Rect();
//
//		touchedRect.x = (x > 4) ? x - 4 : 0;
//		touchedRect.y = (y > 4) ? y - 4 : 0;
//
//		touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols
//				- touchedRect.x;
//		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows
//				- touchedRect.y;
//
//		Mat touchedRegionRgba = mRgba.submat(touchedRect);
//
//		Mat touchedRegionHsv = new Mat();
//		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv,
//				Imgproc.COLOR_RGB2HSV_FULL);
//
//		// Calculate average color of touched region
//		mBlobColorHsv = Core.sumElems(touchedRegionHsv);
//		int pointCount = touchedRect.width * touchedRect.height;
//		for (int i = 0; i < mBlobColorHsv.val.length; i++)
//			mBlobColorHsv.val[i] /= pointCount;
//
//		mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
//
//		Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", "
//				+ mBlobColorRgba.val[1] + ", " + mBlobColorRgba.val[2] + ", "
//				+ mBlobColorRgba.val[3] + ")");
//
//		mDetector.setHsvColor(mBlobColorHsv);
//
//		Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
//
//		mIsColorSelected = true;
//
//		touchedRegionRgba.release();
//		touchedRegionHsv.release();

		return false; // don't need subsequent touch events
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mEdit = new Mat();
		mDst = new Mat(mRgba.size(), CvType.CV_32FC1);

		// Smoothing
		// TODO modificare i parametri di smoothing e canny per cercare di migliorare la situazione
		Size kSize = new Size(5, 5);
		double sigmaX = 5, sigmaY = 5;
		Imgproc.GaussianBlur(mRgba, mEdit, kSize, sigmaX, sigmaY);

		// Detecting edge
		int lowThres = 60, upThres = 90;
		Imgproc.Canny(mEdit, mEdit, lowThres, upThres, 3, true);
//		 return mEdit;

		
//		Imgproc.cvtColor(mEdit, mEdit, Imgproc.COLOR_RGBA2GRAY);
		// Harris Detector parameters
		int blockSize = 3;
		int apertureSize = 1;
		double k = 0.04;

		// Detecting corners
		Imgproc.cornerHarris(mEdit, mDst, blockSize, apertureSize, k,
				Imgproc.BORDER_DEFAULT);
		// Normalizing
		Core.normalize(mDst, mDst, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1);
		mDstSc = mDst.clone();
		Core.convertScaleAbs(mDst, mDstSc);

		// / Drawing a circle around corners
		for (int j = 0; j < mDst.rows(); j++) {
			for (int i = 0; i < mDst.cols(); i++) {
				if (((int) mDst.get(j, i)[0]) > thresh) {
					Core.circle(mRgba, new Point(i, j), 5, new Scalar(0), 2,
							8, 0);
				}
			}
		}

		return mRgba;
	}
	

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);

		return new Scalar(pointMatRgba.get(0, 0));
	}
}