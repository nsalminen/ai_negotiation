package ai_negotiation.ai2018.group15;
import genius.core.misc.Range;

public class SlidingWindow {
	/** window range */
	private Range r;
	/** max size */
	private double maxSize;

	public SlidingWindow(double bound, double maxs) {
		r = new Range(bound, bound);
		maxSize = maxs;
	}
	
	public void slideDown(double delta) {
		if(delta < getLower()) {//if enough space to slide
			setUpper(getUpper() - delta);
			setLower(getLower() - delta);
		} else if(getLower() > 0){//if not enough space set window lb to 0
			setUpper(maxSize);
			setLower(0);
		} else {
			System.out.println("Cannot slide window down - no space");
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
