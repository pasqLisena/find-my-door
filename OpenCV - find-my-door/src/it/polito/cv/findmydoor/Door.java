package it.polito.cv.findmydoor;

import org.opencv.core.Point;

import android.util.Log;

public class Door implements Comparable<Door> {
	private static final String TAG = "DOOR";

	protected static RuntimeException noDoorException = new RuntimeException(
			"This is not a door");

	private Point p1, p2, p3, p4;
	private double avgFillRatio;
	private double geomRate;// punteggio calcolato sui controlli geometrici

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

				if (!checkGeometry()) 
					throw noDoorException;

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

	public void setAvgFillRatio(double avgFillRatio) {
		this.avgFillRatio = avgFillRatio;
	}

	@Override
	public int compareTo(Door another) {
		// pesi per la composizione del rate totale
		int fillW = 100, geomW = 200;
		// diminuisco fillW del parametro thickness dell'activity 
		// TODO: sistemare
		fillW /= 5;

		int rate = (int) (fillW * (this.avgFillRatio - another.avgFillRatio) + geomW
				* (this.geomRate - another.geomRate));
		return rate;
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

		double cSize12 = siz12 - Measure.heightThresL;
		double cSize41 = siz41 - Measure.widthThresH;
		if (cSize12 < 0 || cSize41 > 0) {
			return false;
		}

		double dir12 = calcDirection(p1, p2);
		double dir41 = calcDirection(p1, p4);

		double cDir12 = dir12 - Measure.dirThresH;
		double cDir41 = dir41 - Measure.dirThresL;
		if (cDir12 < 0 || cDir41 > 0) {
			return false;
		}

		double siz23 = calcRelDistance(p2, p3);
		double dir23 = calcDirection(p2, p3);
		double siz34 = calcRelDistance(p3, p4);
		double dir34 = calcDirection(p3, p4);

		double cSiz34 = siz34 - Measure.heightThresH;
		double cSiz23 = siz23 - Measure.widthThresL;
		double cDir23 = dir23 - Measure.dirThresL;
		double cDir34 = dir34 - Measure.dirThresH;
		double cParal = Math.abs(dir12 - dir34) - Measure.parallelThres;

		if (cSiz34 > 0 || cSiz23 < 0 || cDir23 > 0 || cDir34 < 0 || cParal > 0) {
			return false;
		}

		double sizRatio = (siz12 + siz34) / (siz23 + siz41);
		double cSRatioDown = sizRatio - Measure.HWThresL;
		double cSRatioUp = sizRatio - Measure.HWThresH;

		if (cSRatioDown < 0 || cSRatioUp > 0) {
			return false;
		}

		// pesi per comporre il geomRate
		double sizeW = 1, dirW = 2;

		// normalizzazioni per num di elementi
		sizeW /= 6;
		dirW /= 5;

		// normalizzazioni per range
		sizeW /= 1; // (0, 1]
		dirW /= 90; // [0, 90]

		geomRate = (cSize12 - cSize41 - cSiz34 + cSiz23 + cSRatioDown - cSRatioUp)
				* sizeW + (cDir12 - cDir41 - cDir23 + cDir34 - cParal) * dirW;

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
