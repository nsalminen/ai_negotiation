package ai2018.group15;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.bidding.BidDetailsSorterUtility;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;

/**
 * This class uses an opponent model to determine the next bid for the opponent,
 * while taking the opponent's preferences into account. The opponent model is
 * used to select the best bid.
 *
 */
public class Group15_OMS extends OMStrategy {

	/**
	 * When to stop updating the opponent model. Note that this value is not
	 * exactly one as a match sometimes lasts slightly longer.
	 */
	private UserModel userModel;
	
	double updateThreshold = 1.1;

	private int nBids = 1;
	
	private BidDetailsSorterUtility comparer = new BidDetailsSorterUtility();

	private AbstractUtilitySpace utilitySpace;
	/**
	 * Initializes the opponent model strategy. If values for the parameter t
	 * and n are given, then it is set to this value. Otherwise, the default 
	 * values are used.
	 * 
	 * @param negotiationSession
	 *            state of the negotiation.
	 * @param model
	 *            opponent model used in conjunction with this opponent modelling
	 *            strategy.
	 * @param parameters
	 *            set of parameters for this opponent model strategy.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		this.userModel = negotiationSession.getUserModel();
		if(userModel !=null) {
			this.utilitySpace = new EstimateUtility(negotiationSession).getUtilitySpace();
		}
		else {
			this.utilitySpace = negotiationSession.getUtilitySpace();
		}
		negotiationSession.setOutcomeSpace(new SortedOutcomeSpace(utilitySpace));
		
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
		if(parameters.get("n") != null) {
			nBids = parameters.get("n").intValue();
		}
		else {
			System.out.println("OMStrategy assumed n = 3");
		}
	}

	/**
	 * Returns the best bid for the opponent given a set of similarly preferred
	 * bids.
	 * 
	 * @param list
	 *            of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {

		// If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}

		/** 
		 * We need to have an array with the opponents bids and their utility for the opponent
		 * according to our opponent model, so we can sort them later.
		 */
		ArrayList<BidDetails> opponentBids = new ArrayList<BidDetails>(allBids.size());
		
		/**
		 * For each opponent bid, evaluate them according to the opponent model and put them
		 * into the array.
		 */
		for (BidDetails bidDetail : allBids) {
			Bid bid = bidDetail.getBid();
			double evaluation = model.getBidEvaluation(bid);
			BidDetails OpponentBidDetails = new BidDetails(bid, evaluation, negotiationSession.getTime());
			opponentBids.add(OpponentBidDetails);
		}
		
		/**
		 * Use the BidDetailsSorter Utility to then sort these bids
		 * The bid with the highest utility is at the front of the list.
		 */
		Collections.sort(opponentBids, comparer);
		
		
		/**
		 * Now randomly select a bid from the top N.
		 * (Indexing starts at 0 so we want to generate random numbers from 0 to N-1 for the top N)
		 */
		int randomBidIndex = (int)(Math.random() * Math.min(nBids, opponentBids.size()));
		

		Bid bestOpponentBid = opponentBids.get(randomBidIndex).getBid();
		
		
		// Now return this bid in the form for our agent.
		BidDetails bestBid = new BidDetails(bestOpponentBid,
									utilitySpace.getUtility(bestOpponentBid),
									negotiationSession.getTime());
		
		System.out.println("Estimated utility for opponent: " + model.getBidEvaluation(bestBid.getBid()));
		
		return bestBid;
	}

	/**
	 * The opponent model may be updated, unless the time is higher than a given
	 * constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
		set.add(new BOAparameter("n", 3.0, "Number of best bids from which to randomly select"));
		return set;
	}

	@Override
	public String getName() {
		return "Group 15 OMS";
	}
}
