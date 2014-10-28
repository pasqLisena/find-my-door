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
		if (other.isHorizontal == this.isHorizontal)
			return false;

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

	public Point getIntersection(Line other) {
		if (other.isHorizontal == this.isHorizontal)
			return null;

		Point p1a = this.start, p1b = this.end;
		Point p2a = other.start, p2b = other.end;
		int x1 = (int) p1a.x, y1 = (int) p1a.y, x2 = (int) p1b.x, y2 = (int) p1b.y;
		int x3 = (int) p2a.x, y3 = (int) p2a.y, x4 = (int) p2b.x, y4 = (int) p2b.y;

		// find intersection
		float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

		if (d != 0) {
			int ix = (int) (((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2)
					* (x3 * y4 - y3 * x4)) / d);
			int iy = (int) (((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2)
					* (x3 * y4 - y3 * x4)) / d);

			if (ix > 0 && iy > 0)
				return new Point(ix, iy);
		}
		return null;
	}
}
