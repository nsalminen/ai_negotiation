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
		System.out.println("delta");
		System.out.println(delta);
		System.out.println("lb");
		System.out.println(getLower());
		if(delta < getLower()) {
			setUpper(getUpper() - delta);
			setLower(getLower() - delta);
		} else if(getLower() > 0){
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
	
	/***
	 * distance is between (lower, next bid)
	 * @return true if window can grow to include next concessive bid
	 */
	public boolean canGrow(double distance) {
		if(getUpper() - getLower() + distance >= maxSize) {
			return false;
		} 
		return true;
	}
	
}
