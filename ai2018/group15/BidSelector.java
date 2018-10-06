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
	
	/** rng */
	private Random rng;
	
	public BidSelector(SortedOutcomeSpace ocs, double maxWindowSize, int maxBids, double concessSize) {
		outcomespace = ocs;
		sw = new SlidingWindow(outcomespace.getMaxBidPossible().getMyUndiscountedUtil(), maxWindowSize);
		BidList = outcomespace.getBidsinRange(sw.getRange());
		maxBidCount = maxBids;
		maxSwSize = maxWindowSize;
		maxConcessionAmount = concessSize;
		rng = new Random();
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
	public BidDetails GetNextBid(int action, double newConcessionAmount) {
		update(action, newConcessionAmount);
		return getRandomBid();
	}
	
	
	/***
	 * update window
	 * update bidlist
	 */
	public void update(int action, double newConcessionAmount) {
		concessionAmount = newConcessionAmount;

		if(action == 1) {
			//make sure window is max size
			sw.setLower(sw.getUpper()-maxSwSize);
		} else if (action == 2) {
			//perform concession
			performConcession();
		}
		updateBidList();	

		printBidList();
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
		
		System.out.println("performing concession:");
		
		sw.slideDown(Math.min(concessionAmount, maxConcessionAmount));
		
		updateBidList();
		if(isBidListEmpty()) {
			if(sw.getLower() > 0) { //if possible to slide window down more do it
				performConcession();
			} else {
				outcomespace.getMinBidPossible(); //get worst
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
