package ai_negotiation.ai2018.group15;
import genius.core.misc.Range;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.bidding.BidDetailsSorterUtility;
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
	
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;
	
	/** Bid Details List */
	private List<BidDetails> BidList;
	
	public BidSelector(SortedOutcomeSpace ocs, double maxWindowSize, int maxBids, double concessSize) {
		outcomespace = ocs;
		sw = new SlidingWindow(outcomespace.getMaxBidPossible().getMyUndiscountedUtil(), maxWindowSize);
		BidList = outcomespace.getBidsinRange(sw.getRange());
		maxBidCount = maxBids;
		maxSwSize = maxWindowSize;
		maxConcessionAmount = concessSize;
	}
	
	/***
	 * 
	 * @return distance between sliding window lower bound and closest conceding bid
	 */
	private double getDistanceLowerBoundToClosestConcedingBid() {
		double lb = sw.getLower();
		//sort bidlist best to worst
		Collections.sort(BidList, new BidDetailsSorterUtility());
		double worstBidUtil = BidList.get(BidList.size() - 1).getMyUndiscountedUtil();
		double closestConcedingUtil = outcomespace.getAllOutcomes().get(outcomespace.getIndexOfBidNearUtility(worstBidUtil)+1).getMyUndiscountedUtil();
		
		/*System.out.println(">>> Calculating distance lb - next bid");
		System.out.println("lb:");
		System.out.println(lb);
		System.out.println("worstBidUtil:");
		System.out.println(worstBidUtil);
		System.out.println("closestConcedingUtil:");
		System.out.println(closestConcedingUtil);*/
		
		
		return lb - closestConcedingUtil;
	}
	
	/***
	 * 
	 * @return best ranked bid
	 */
	public BidDetails GetFirstBid() {
		return BidList.get(0);
	}
	
	/***
	 * 
	 * @param action 1 = normal update window, 2 = slide window
	 */
	public BidDetails GetNextBid(int action, double newConcessionAmount) {
		Update(action, newConcessionAmount);
		/*System.out.println("***** Getting next bid *****");
		System.out.println("BidList size:");
		System.out.println(BidList.size());*/
		return getRandomBid();
	}
	
	
	/***
	 * update window
	 * update bidlist
	 */
	public void Update(int action, double newConcessionAmount) {
		concessionAmount = newConcessionAmount;
		/*System.out.println("Chosen action id:");
		System.out.println(action);*/
		if(action == 1) {
			updateWindow();
		} else if (action == 2) {
			slideWindow();
		}
		updateBidList();	
		System.out.println("win size");
		System.out.println(sw.getUpper() - sw.getLower());
		System.out.println("upper");
		System.out.println(sw.getUpper());
		System.out.println("lower");
		System.out.println(sw.getLower());
		System.out.println(">> bid utils");
		
		for(int i = 0; i < BidList.size(); i++) {
			System.out.println(BidList.get(i).getMyUndiscountedUtil());
		}
	}
	
	/**
	 * slide the window...
	 * many ways to do this (rm best then add new worst?, slide whole window by amount, set new upper bound and let update() expand the window)
	 * make sure window has atleast 1 bid
	 * 
	 * not good! isBidListEmpty() doesnt take into account if max window size has been reached etc.. window sliding still needs some thought
	 */
	private void slideWindow() {
		System.out.println("Sliding down window");
		sw.slideDown(Math.min(concessionAmount, maxConcessionAmount));
		updateBidList();
		if(isBidListEmpty()) {
			slideWindow();
		}
	}
	
	private BidDetails getRandomBid() {
		Random rng = new Random();
		return BidList.get(rng.nextInt(BidList.size()));
	}
	
	
	/***
	 * expand window till max size
	 */
	private void updateWindow() {
		double distanceToNext = getDistanceLowerBoundToClosestConcedingBid();
		System.out.println(">>> Updating window");
		System.out.println("Distance to next:");
		System.out.println(distanceToNext);
		/*System.out.println("Max window size");
		System.out.println(maxSwSize);*/
		System.out.println("Can expand:");
		System.out.println(canExpandWindow(distanceToNext));
		//expand window to add next bid
		if(canExpandWindow(distanceToNext)) {
			sw.setLower(sw.getLower()-distanceToNext);
		}	
	}
	
	/***
	 * update bid list for new range
	 */
	private void updateBidList() {
		BidList = outcomespace.getBidsinRange(sw.getRange());
	}
	
	
	
	
	/***
	 * 
	 * @return if window can be expanded
	 */
	private boolean canExpandWindow(double distance) {		
		return !isBidListFull() && sw.canGrow(distance);
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
