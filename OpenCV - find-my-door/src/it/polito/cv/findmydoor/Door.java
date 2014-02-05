package it.polito.cv.findmydoor;

import org.opencv.core.Point;

import android.util.Log;

public class Door implements Comparable<Door> {
	private static final String TAG = "DOOR";

	protected static RuntimeException noDoorException = new RuntimeException(
			"This is not a door");

	private Point p1, p2, p3, p4;
	private double avgFillRatio;

	public Door(Point p1, Point p2, Point p3, Point p4) {
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;

		if (!checkGeometry()) {
			// commute long side points
			this.p1 = p1;
			this.p2 = p3;
			this.p3 = p2;
			this.p4 = p4;

			if (!checkGeometry()) {
				// commute short side points
				this.p1 = p1;
				this.p2 = p2;
				this.p3 = p4;
				this.p4 = p3;

				if (!checkGeometry()) {
					Log.d(TAG, "FAIL **");

					throw noDoorException;
				}

				Log.d(TAG, "OOOOOOOOOOOOOOOOOOOK");

			}
		}
	}

	public Point getP1() {
		return p1;
	}

	public Point getP2() {
		return p2;
	}

	public Point getP3() {
		return p3;
	}

	public Point getP4() {
		return p4;
	}

	public double getAvgFillRatio() {
		return avgFillRatio;
	}

	public void setAvgFillRatio(double avgFillRatio) {
		this.avgFillRatio = avgFillRatio;
	}

	@Override
	public int compareTo(Door another) {
		return (int) (100 * (this.avgFillRatio - another.getAvgFillRatio()));
	}

	/*
	 * Check if the rectangle p1-p2-p3-p4 is a door. The four point are in
	 * order.
	 */
	private boolean checkGeometry() {
		double siz12 = calcRelDistance(p1, p2);
		double siz41 = calcRelDistance(p1, p4);

		if (siz12 < siz41) {
			// commute the sides
			// return checkGeometry(p2, p3, p4, p1);
			Point temp = p1;
			p1 = p2;
			p2 = p3;
			p3 = p4;
			p4 = temp;

			siz41 = siz12;
			siz12 = calcRelDistance(p1, p2);
		}

		if (siz12 < Measure.heightThresL || siz41 > Measure.widthThresH) {
			return false;
		}

		double dir12 = calcDirection(p1, p2);
		double dir41 = calcDirection(p1, p4);

		if (dir12 < Measure.dirThresH || dir41 > Measure.dirThresL) {
			return false;
		}

		double siz23 = calcRelDistance(p2, p3);
		double dir23 = calcDirection(p2, p3);
		double siz34 = calcRelDistance(p3, p4);
		double dir34 = calcDirection(p3, p4);

		if (siz34 < Measure.heightThresL || siz23 < Measure.widthThresL
				|| dir23 > Measure.dirThresL || dir34 < Measure.dirThresH
				|| Math.abs(dir12 - dir34) > Measure.parallelThres) {
			return false;
		}

		double sizRatio = (siz12 + siz34) / (siz23 + siz41);

		if (sizRatio < Measure.HWThresL || sizRatio > Measure.HWThresH) {
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
	private static double calcRelDistance(Point i, Point j) {
		return calcDistance(i, j) / Measure.diag;
	}

	/*
	 * Calculate the absolute distance between two points.
	 * 
	 * @return the distance
	 */
	private static double calcDistance(Point i, Point j) {
		double sizX = Math.pow((i.x - j.x), 2);
		double sizY = Math.pow((i.y - j.y), 2);
		return Math.sqrt(sizX + sizY);
	}

	private static double calcDirection(Point i, Point j) {
		double dfX = i.x - j.x;
		double dfY = i.y - j.y;
		double dfRatio = Math.abs(dfX / dfY);
		return Math.atan(dfRatio) * 180 / Math.PI;
	}

}
