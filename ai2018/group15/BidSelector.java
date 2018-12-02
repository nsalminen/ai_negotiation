package ai2018.group15;

import java.util.List;
import java.util.Random;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.SortedOutcomeSpace;

public class BidSelector {
	// Sliding window
	private SlidingWindow sw;
	// Max sliding window size
	private double maxSwSize;
	// Max bidcount in window
	private int maxBidCount;
	// Max concession amount
	private double maxConcessionAmount;
	// Concession amount, how much utility to concede per concession
	private double concessionAmount;
	// Max increase amount
	private double maxIncreaseAmount;
	// Increase amount, how much utility to move up
	private double increaseAmount;
	// Outcome space
	private SortedOutcomeSpace outcomespace;

	// Bid Details List
	private List<BidDetails> BidList;

	private Random rng;

	public BidSelector(SortedOutcomeSpace ocs, double maxWindowSize, int maxBids, double maxConcession,
			double maxIncrease) {
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
	 * Returns the bid that is ranked the highest
	 * 
	 * @return best ranked bid
	 */
	public BidDetails getFirstBid() {
		return BidList.get(0);
	}

	// Get lower bound of sliding window
	public double getLower() {
		return sw.getLower();
	}

	/***
	 * Updates window and returns a random bid from the list of bids
	 * 
	 * @param action        action to perform: 1 = normal update window, 2 = slide
	 *                      window
	 * @param newDifference difference between most recent bid pair of opponent
	 */
	public BidDetails GetNextBid(int action, double newDifference) {
		update(action, newDifference);
		return getRandomBid();
	}

	/***
	 * Returns a random bid from the list of bids
	 * 
	 * @return random bid from the bid list
	 */
	private BidDetails getRandomBid() {
		return BidList.get(rng.nextInt(Math.min(BidList.size(), maxBidCount)));
	}

	// Slide the window upwards: increase utility
	private void increaseUtility() {
		sw.slideUp(Math.min(increaseAmount, maxIncreaseAmount));

		updateBidList();
		if (isBidListEmpty()) {
			if (sw.getUpper() < 1) { // if possible to slide window up more do it
				increaseUtility();
			} else {
				sw.setUpper(outcomespace.getMaxBidPossible().getMyUndiscountedUtil()); // set upper to worst
				sw.setLower(outcomespace.getMaxBidPossible().getMyUndiscountedUtil()); // set lower to worst
			}
		}
	}

	/***
	 * Checks whether the list of bids is empty
	 * 
	 * @return whether bid list is empty
	 */
	private boolean isBidListEmpty() {
		if (BidList.size() == 0) {
			return true;
		}
		return false;
	}

	/***
	 * Checks whether the list of bids has reached the maximum amount of bids
	 * 
	 * @return whether the size of the bid list has reached the maximum
	 */
	private boolean isBidListFull() {
		if (BidList.size() >= maxBidCount) {
			return true;
		}
		return false;
	}

	// Slide the window downwards: perform concession
	private void performConcession() {
		sw.slideDown(Math.min(concessionAmount, maxConcessionAmount));
		updateBidList();
		if (isBidListEmpty()) {
			if (sw.getLower() > 0) { // if possible to slide window down more do it
				performConcession();
			} else {
				sw.setUpper(outcomespace.getMinBidPossible().getMyUndiscountedUtil()); // set upper to worst
				sw.setLower(outcomespace.getMinBidPossible().getMyUndiscountedUtil()); // set lower to worst
			}
		}
	}

	/***
	 * Updates window of list of bids and performs action.
	 *
	 * @param action        determines the action that needs to be performed
	 * @param newDifference difference between most recent bid pair of opponent
	 */
	public void update(int action, double newDifference) {
		switch (action) {
		case 1:
			sw.setLower(sw.getUpper() - maxSwSize);
			break;
		case 2:
			concessionAmount = Math.abs(newDifference);
			performConcession();
			break;
		case 3:
			increaseAmount = Math.abs(newDifference);
			increaseUtility();
			break;
		}
		updateBidList();
	}

	// Updates the list of bids for a new range
	private void updateBidList() {
		BidList = outcomespace.getBidsinRange(sw.getRange());
	}

}
