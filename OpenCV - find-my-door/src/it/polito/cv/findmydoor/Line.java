package it.polito.cv.findmydoor;

import org.opencv.core.Point;

public class Line {
	public Point start, end;
	public double dir, siz;
	private boolean horizontalOriented;
	
	public Line(Point start, Point end) {
		this.start = start;
		this.end = end;

		dir = Door.calcDirection(start, end);

		if (dir < Measure.dirThresL) {
			// lato corto
			horizontalOriented = true;
			siz = Door.calcRelDistance(start, end);
			
			if(siz > Measure.widthThresL && siz < Measure.widthThresH ){
				return;
			}
		} else if (dir > Measure.dirThresH) {
			// lato lungo
			horizontalOriented = false;
			siz = Door.calcRelDistance(start, end);

			if(siz > Measure.heightThresL && siz < Measure.heightThresH ){
				return;
			}
		}

		throw new RuntimeException("Linea non utile");
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

	public boolean isHorizontal() {
		return horizontalOriented;
	}


}
