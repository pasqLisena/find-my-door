package it.polito.cv.findmydoor;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import android.util.Log;

public class Door implements Comparable<Door> {
	private static final String TAG = "DOOR";

	protected static RuntimeException noDoorException = new RuntimeException(
			"This is not a door");

	private Point p1, p2, p3, p4;
	private Line l1, l2, l3, l4;
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

	public Door(Line line1, Line line2, Line line3, Line line4) {
		List<Line> horLines = new ArrayList<Line>();
		List<Line> verLines = new ArrayList<Line>();

		if (line1.isHorizontal())
			horLines.add(line1);
		else
			verLines.add(line1);

		if (line2.isHorizontal())
			horLines.add(line2);
		else
			verLines.add(line2);

		if (line3.isHorizontal())
			horLines.add(line3);
		else
			verLines.add(line3);

		if (line4.isHorizontal())
			horLines.add(line4);
		else
			verLines.add(line4);

		if (horLines.size() > 2 || verLines.size() > 2)
			throw noDoorException;

		l1 = horLines.get(0);
		l2 = verLines.get(0);
		l3 = horLines.get(1);
		l4 = verLines.get(1);

		if (!(l1.isConsecutive(l2) && l2.isConsecutive(l3)
				&& l3.isConsecutive(l4) && l4.isConsecutive(l1)))
			throw noDoorException;

		if (Math.abs(l2.dir - l4.dir) > Measure.parallelThres)
			throw noDoorException;

		double ratio = (l4.siz + l2.siz) / (l3.siz + l1.siz);

		if (ratio < Measure.HWThresL || ratio > Measure.HWThresH)
			throw noDoorException;
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
			this.p1 = p2;
			this.p2 = p3;
			this.p3 = p4;
			this.p4 = temp;

			siz41 = siz12;
			siz12 = calcRelDistance(p1, p2);
		}

		double dir12 = calcDirection(p1, p2);
		double dir41 = calcDirection(p1, p4);
		double dir23 = calcDirection(p2, p3);
		double dir34 = calcDirection(p3, p4);

		double cDir23 = dir23 - Measure.dirThresL;
		double cDir34 = dir34 - Measure.dirThresH;
		double cParal = Math.abs(dir12 - dir34) - Measure.parallelThres;

		double cDir12 = dir12 - Measure.dirThresH;
		double cDir41 = dir41 - Measure.dirThresL;
		if (cDir12 < 0 || cDir41 > 0 || cDir23 > 0 || cDir34 < 0 || cParal > 0) {
			return false;
		}
		// Log.e(TAG, "paral "+cParal);

		double cSize12d = siz12 - Measure.heightThresL;
		double cSize12u = siz12 - Measure.heightThresH;
		double cSize41d = siz41 - Measure.widthThresL;
		double cSize41u = siz41 - Measure.widthThresH;
		if (cSize12d < 0 || cSize12u > 0 || cSize41d < 0 || cSize41u > 0) {
			return false;
		}

		double siz23 = calcRelDistance(p2, p3);
		double siz34 = calcRelDistance(p3, p4);

		double cSiz34d = siz34 - Measure.heightThresL;
		double cSiz34u = siz34 - Measure.heightThresH;
		double cSiz23d = siz23 - Measure.widthThresL;
		double cSiz23u = siz23 - Measure.widthThresH;

		if (cSiz34d < 0 || cSiz34u > 0 || cSiz23d < 0 || cSiz23u > 0) {
			return false;
		}

		double sizRatio = (siz12 + siz34) / (siz23 + siz41);
		double cSRatioDown = sizRatio - Measure.HWThresL;
		double cSRatioUp = sizRatio - Measure.HWThresH;

		Log.e(TAG, "siz " + siz12 + " " + siz34 + " " + siz23 + " " + siz41);
		Log.e(TAG, "ratio " + sizRatio);

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

		geomRate = (cSize12d - cSize12u + cSize41d - cSize41u + cSiz34d
				- cSiz34u + cSiz23d - cSiz23u + cSRatioDown - cSRatioUp)
				* sizeW + (cDir12 - cDir41 - cDir23 + cDir34 - cParal) * dirW;

		// if here, 1234 is a door
		return true;
	}

	/*
	 * Calculate the relative distance between two points.
	 * 
	 * @return the distance within a range of [0,1]
	 */
	public static double calcRelDistance(Point i, Point j) {
		return calcDistance(i, j) / Measure.diag;
	}

	/*
	 * Calculate the absolute distance between two points.
	 * 
	 * @return the distance
	 */
	public static double calcDistance(Point i, Point j) {
		double sizX = Math.pow((i.x - j.x), 2);
		double sizY = Math.pow((i.y - j.y), 2);
		return Math.sqrt(sizX + sizY);
	}

	public static double calcDirection(Point i, Point j) {
		double dfX = i.x - j.x;
		double dfY = i.y - j.y;
		double dfRatio = Math.abs(dfX / dfY);
		return Math.atan(dfRatio) * 180 / Math.PI;
	}

}
