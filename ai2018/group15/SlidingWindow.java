package ai2018.group15;

import genius.core.misc.Range;

public class SlidingWindow {
	// Window range
	private Range r;
	// Max size of window
	private double maxSize;

	public SlidingWindow(double bound, double maxs) {
		r = new Range(bound, bound);
		maxSize = maxs;
	}

	public void slideDown(double delta) {
		if (delta < getLower()) { // If enough space to slide
			setUpper(getUpper() - delta);
			setLower(getLower() - delta);
		} else if (getLower() > 0) { // If not enough space, set window lb to 0
			setUpper(maxSize);
			setLower(0);
		}
	}

	public void slideUp(double delta) {
		if (delta < (1 - getUpper())) { // If enough space to slide
			setUpper(getUpper() + delta);
			setLower(getLower() + delta);
		} else if (getUpper() < 1) { // If not enough space set window ub to 1
			setUpper(1);
			setLower(1 - maxSize);
		}
	}

	public void setUpper(double u) {
		r.setUpperbound(u);
	}

	public void setLower(double l) {
		r.setLowerbound(l);
	}

	public double getUpper() {
		return r.getUpperbound();
	}

	public double getLower() {
		return r.getLowerbound();
	}

	public Range getRange() {
		return r;
	}
}
