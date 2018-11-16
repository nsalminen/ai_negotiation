package ai2018.group15;
import java.util.List;
import java.util.Random;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.SortedOutcomeSpace;

public class BidSelector {
	/** Sliding window*/
	private SlidingWindow sw;
	private double maxSwSize;
	/** max bidcount in window*/
	private int maxBidCount;
	/** max concession amount */
	private double maxConcessionAmount;
	/** concession amount, how much util to concede per concession */
	private double concessionAmount;
	/** max increase amount */
	private double maxIncreaseAmount;
	/** increase amount, how much util to move up */
	private double increaseAmount;
	
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;
	
	/** Bid Details List */
	private List<BidDetails> BidList;
	
	/** rng */
	private Random rng;
	
	public BidSelector(SortedOutcomeSpace ocs, double maxWindowSize, int maxBids, double maxConcession, double maxIncrease) {
		outcomespace = ocs;
		sw = new SlidingWindow(outcomespace.getMaxBidPossible().getMyUndiscountedUtil(), maxWindowSize);
		BidList = outcomespace.getBidsinRange(sw.getRange());
		maxBidCount = maxBids;
		maxSwSize = maxWindowSize;
		maxConcessionAmount = maxConcession;
		rng = new Random();
		maxIncreaseAmount = maxIncrease;
	}
	
	
	/***
	 * 
	 * @return best ranked bid
	 */
	public BidDetails getFirstBid() {
		return BidList.get(0);
	}
	
	/***
	 * 
	 * @param action 1 = normal update window, 2 = slide window
	 */
	public BidDetails GetNextBid(int action, double newDifference) {
		update(action, newDifference);
		return getRandomBid();
	}
	
	
	/***
	 * update window
	 * update bidlist
	 */
	public void update(int action, double newDifference) {
		
		System.out.println("Update action: " + action);
		
		switch (action) {
			case 1: sw.setLower(sw.getUpper()-maxSwSize);
					break;
			case 2: concessionAmount = Math.abs(newDifference);
					performConcession();
					break;
			case 3: increaseAmount = Math.abs(newDifference);
					increaseUtility();
					break;
			default: System.out.println("Action id incorrect: " + action);
		}
		
		updateBidList();	

		//more insight
		//printBidList();
	}
	
	/***
	 * get sliding window lower bound
	 */
	public double getLower() {
		return sw.getLower();
	}
	
	/***
	 * debugging purposes
	 */
	private void printBidList() {
		System.out.println(">>> BidList.size(): " + BidList.size());
		System.out.println(">> bid utils");
		for(int i = 0; i < Math.min(BidList.size(), maxBidCount); i++) {
			System.out.println(BidList.get(i).getMyUndiscountedUtil());
		}
	}

	/***
	 * slide window down (perform concession)
	 */
	private void performConcession() {
		//todo: stay above reservation value?
		
		System.out.println("performing concession");
		
		sw.slideDown(Math.min(concessionAmount, maxConcessionAmount));
		
		updateBidList();
		if(isBidListEmpty()) {
			if(sw.getLower() > 0) { //if possible to slide window down more do it
				performConcession();
			} else {
				sw.setUpper(outcomespace.getMinBidPossible().getMyUndiscountedUtil()); //set upper to worst
				sw.setLower(outcomespace.getMinBidPossible().getMyUndiscountedUtil()); //set lower to worst
			}
		}
	}
	
	/***
	 * slide window up 
	 */
	private void increaseUtility() {
		
		System.out.println("performing increase");
		
		sw.slideUp(Math.min(increaseAmount, maxIncreaseAmount));
		
		updateBidList();
		if(isBidListEmpty()) {
			if(sw.getUpper() < 1) { //if possible to slide window up more do it
				increaseUtility();
			} else {
				sw.setUpper(outcomespace.getMaxBidPossible().getMyUndiscountedUtil()); //set upper to worst
				sw.setLower(outcomespace.getMaxBidPossible().getMyUndiscountedUtil()); //set lower to worst
			}
		}
	}
	
	/***
	 * 
	 * @return randomly selected bid from bidlist[0:maxBidCount]
	 */
	private BidDetails getRandomBid() {
		return BidList.get(rng.nextInt(Math.min(BidList.size(), maxBidCount)));
	}
	
	
	/***
	 * update bid list for new range
	 */
	private void updateBidList() {
		BidList = outcomespace.getBidsinRange(sw.getRange());
	}
	
	
	private boolean isBidListEmpty() {
		if(BidList.size() == 0) {
			return true;
		}
		return false;
	}
	
	/***
	 * 
	 * @return if bidlist.size() == maxBidCount
	 */
	private boolean isBidListFull() {
		if(BidList.size() >= maxBidCount){
			return true;
		}
		return false;
	}
	
}
