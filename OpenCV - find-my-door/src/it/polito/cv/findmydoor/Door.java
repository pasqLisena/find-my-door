package it.polito.cv.findmydoor;

import org.opencv.core.Point;

public class Door implements Comparable<Door> {

	private Point p1, p2, p3, p4;
	private double avgFillRatio;

	public Door(Point p1, Point p2, Point p3, Point p4) {
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.p4 = p4;
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

}
