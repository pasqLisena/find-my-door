
package it.polito.cv.findmydoor;

import org.opencv.core.Point;

public class Line {
	public Point start, end;
	public double dir, siz;
	public boolean isHorizontal;

	public Line(Point start, Point end) {
		this.start = start;
		this.end = end;

		dir = Door.calcDirection(start, end);

		if (dir < Measure.dirThresL) {
			// short line
			isHorizontal = true;
			siz = Door.calcRelDistance(start, end);
			return;
		} else if (dir > Measure.dirThresH) {
			// long line
			isHorizontal = false;
			siz = Door.calcRelDistance(start, end);
			return;
		}

		throw new RuntimeException("Useless line.");
	}

	public boolean isConsecutive(Line other) {
		boolean a, b, c, d;

		a = checkProximity(this.start, other.start);
		b = checkProximity(this.start, other.end);
		c = checkProximity(this.end, other.start);
		d = checkProximity(this.end, other.end);

		return a ^ b ^ c ^ d; // xor
	}

	private static boolean checkProximity(Point a, Point b) {
		double xGap = (a.x - b.x) / Measure.diag;
		double yGap = (a.y - b.y) / Measure.diag;

		return xGap < Measure.maxPointGap && yGap < Measure.maxPointGap;
	}
}
