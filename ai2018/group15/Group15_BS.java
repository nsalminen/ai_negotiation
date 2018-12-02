package ai2018.group15;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.BidHistory;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.misc.Range;
import genius.core.uncertainty.UserModel;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy
 * adapted from [1]
 * 
 * The default strategy was extended to enable the usage of opponent models.
 * 
 * Note that this agent is not fully equivalent to the theoretical model,
 * loading the domain may take some time, which may lead to the agent skipping
 * the first bid. A better implementation is GeniusTimeDependent_Offering.
 * 
 * 1. S. Shaheen Fatima Michael Wooldridge Nicholas R. Jennings Optimal
 * Negotiation Strategies for Agents with Incomplete Information
 * http://eprints.ecs.soton.ac.uk/6151/1/atal01.pdf
 */
public class Group15_BS extends OfferingStrategy {

	private UserModel userModel;

	// k in [0, 1]. For k = 0 the agent starts with a bid of maximum utility
	private double k;
	// Maximum target utility
	private double Pmax;
	// Minimum target utility
	private double Pmin;
	// Concession factor
	private double e;
	// Outcome space
	private SortedOutcomeSpace outcomespace;
	// Best Opponent's bid
	private BidDetails bestOpponentBid;

	// Bid selector
	private BidSelector bs;
	// Max window size
	private double maxWindowSize = 0.05;
	// Max bid count in range
	private int maxBids = 5;

	// Concession amount, scale with time
	private double maxConcessionAmount = 0.16;
	private double concessionAmount = 0.08;
	// Concession probability when opponent makes a concession
	private int concessProba = 75;
	// Increase amount, scale with time
	private double maxIncreaseAmount = 0.04;
	// Increase probability when opponent makes an offer with increased utility
	private int increaseProba = 75;

	// Opponent last bid util
	private double opponentLastBidUtil = -1;
	// Opponent last concession amount
	private double opponentLastConcessionAmount = 0;

	/**
	 * Before last opponent bid util to compare with last opponent bid util to see
	 * if opponent conceded or not
	 */
	private double beforeLastOpponentBidUtil;
	// Most recent bid from opponent
	private double lastOpponentBidUtil;
	/**
	 * Minimum difference in opponent bid util before it is considered a concession
	 * or increase
	 */
	private double minimalUtilDifference = 0.02;

	// Minimum lower bound of sliding window
	private double minimumLowerBound = 0.80;

	private Random rng;

	// Size (in utility) of window to take target utility bids from
	private double windowSize = 0.05;

	/** Target utility at the start of a session */
	private double utilityGoal = 1.0;

	/**
	 * Determines by how much variables that depend on time should be scaled
	 * depending on the total session time
	 */
	private double timeScalar = 1.0;

	/**
	 * Method which initializes the agent by setting all parameters. The parameter
	 * "e" is the only parameter which is required.
	 */
	@Override
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
			Map<String, Double> parameters) throws Exception {
		super.init(negoSession, parameters);
		String timeType = negotiationSession.getTimeline().getType().name();
		if (timeType == "Rounds") {
			timeScalar = negotiationSession.getTimeline().getTotalTime() / 100;
		} else {
			timeScalar = negotiationSession.getTimeline().getTotalTime() * 4;
		}
		maxIncreaseAmount = maxIncreaseAmount / Math.sqrt(timeScalar);
		maxConcessionAmount = maxConcessionAmount / Math.sqrt(timeScalar);
		concessionAmount = concessionAmount / Math.sqrt(timeScalar);

		if (parameters.get("e") != null) {
			this.negotiationSession = negoSession;
			this.userModel = negotiationSession.getUserModel();

			if (userModel != null) {
				outcomespace = new SortedOutcomeSpace(new EstimateUtility(negotiationSession).getUtilitySpace());
				negotiationSession.setOutcomeSpace(outcomespace);
			} else {
				outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
				negotiationSession.setOutcomeSpace(outcomespace);
			}

			bs = new BidSelector(outcomespace, maxWindowSize, maxBids, maxConcessionAmount, maxIncreaseAmount);

			bestOpponentBid = null;
			beforeLastOpponentBidUtil = -1;

			rng = new Random();

			this.e = parameters.get("e");

			if (parameters.get("k") != null)
				this.k = parameters.get("k");
			else
				this.k = 0;

			if (parameters.get("min") != null)
				this.Pmin = parameters.get("min");
			else
				this.Pmin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();

			if (parameters.get("max") != null) {
				Pmax = parameters.get("max");
			} else {
				BidDetails maxBid = negoSession.getMaxBidinDomain();
				Pmax = maxBid.getMyUndiscountedUtil();
			}

			this.opponentModel = model;
			this.omStrategy = oms;
		} else {
			throw new Exception("Constant \"e\" for the concession speed was not set.");
		}

	}

	@Override
	public BidDetails determineOpeningBid() {
		return bs.getFirstBid();
	}

	/**
	 * Simple offering strategy which retrieves the target utility and looks for the
	 * nearest bid if no opponent model is specified. If an opponent model is
	 * specified, then the agent return a bid according to the opponent model
	 * strategy.
	 */
	@Override
	public BidDetails determineNextBid() {
		double time = negotiationSession.getTime();

		if (time > 0.99) { // near time limit -> conceding strategy
			if (bestOpponentBid == null) {
				BidHistory opponentBids = negotiationSession.getOpponentBidHistory();
				bestOpponentBid = opponentBids.getBestBidDetails();
			}

			nextBid = bestOpponentBid;
		} else {
			if (opponentModel instanceof NoModel) {
				/**
				 * Enough time left -> Hardheaded and tit-for-tat (concede if you concede)
				 * strategy myAction can take values : 1 = normal hard headed round, 2 = perform
				 * concession, 3 = increase target offer
				 */
				int myAction = 1;
				double opponentBidDiff = opponentBidDifference();
				if (opponentBidDiff < (-1 * minimalUtilDifference)) {
					if (randomConcede() && bs.getLower() > minimumLowerBound)
						myAction = 2;
				} else if (opponentBidDiff > minimalUtilDifference) {
					if (randomIncrease())
						myAction = 3;
				}
				nextBid = bs.GetNextBid(myAction, opponentBidDiff);
			} else {
				// Determine if opponent made a concession and if the concession has been
				// handled by our agent yet
				if (((Group15_OM) opponentModel).opponentConceeded()
						&& !((Group15_OM) opponentModel).getConcessionHandled()) {
					// Update target utility
					if (utilityGoal > Pmin) {
						if (randomConcede()) {
							utilityGoal -= Math.min(maxConcessionAmount,
									concessionAmount * negotiationSession.getTime() / (1 - utilityGoal + 0.0000001));
						}
						((Group15_OM) opponentModel).setConcessionHandled();
					}
				}
				Range targetRange = new Range(utilityGoal - windowSize / 2, utilityGoal + windowSize / 2);
				// Get set of similarly preferred bids
				List<BidDetails> preferredBids = outcomespace.getBidsinRange(targetRange);

				// Get a good bid for the opponent from our set of preferred bids
				nextBid = omStrategy.getBid(preferredBids);
			}
		}

		return nextBid;
	}

	/***
	 * This function returns the difference between the last two bids of the
	 * opponent
	 * 
	 * @return Difference in util between opponent's last two bids
	 */
	private double opponentBidDifference() {
		if (negotiationSession.getOpponentBidHistory().getHistory().isEmpty())
			return 0;
		lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		if (beforeLastOpponentBidUtil == -1) {
			beforeLastOpponentBidUtil = lastOpponentBidUtil;
			return 0;
		}
		double difference = lastOpponentBidUtil - beforeLastOpponentBidUtil;
		beforeLastOpponentBidUtil = lastOpponentBidUtil;
		return difference;
	}

	/***
	 * Determines whether the agent will concede in this round
	 * 
	 * @return if agent will concede this round
	 */
	private boolean randomConcede() {
		return rng.nextInt(100) < concessProba;
	}

	/***
	 * Determines whether the agent will increase the utility of its bid this round
	 * 
	 * @return if agent will increase the utility of its bid this round
	 */
	private boolean randomIncrease() {
		return rng.nextInt(100) < increaseProba;
	}

	/**
	 * From [1]:
	 * 
	 * A wide range of time dependent functions can be defined by varying the way in
	 * which f(t) is computed. However, functions must ensure that 0 <= f(t) <= 1,
	 * f(0) = k, and f(1) = 1.
	 * 
	 * That is, the offer will always be between the value range, at the beginning
	 * it will give the initial constant and when the deadline is reached, it will
	 * offer the reservation value.
	 * 
	 * For e = 0 (special case), it will behave as a Hardliner.
	 */
	public double f(double t) {
		if (e == 0)
			return k;
		double ft = k + (1 - k) * Math.pow(t, 1.0 / e);
		return ft;
	}

	/**
	 * Makes sure the target utility with in the acceptable range according to the
	 * domain Goes from Pmax to Pmin!
	 * 
	 * @param t
	 * @return double
	 */
	public double p(double t) {
		return Pmin + (Pmax - Pmin) * (1 - f(t));
	}

	public NegotiationSession getNegotiationSession() {
		return negotiationSession;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("e", 1.0, "Concession rate"));
		set.add(new BOAparameter("k", 0.0, "Offset"));
		set.add(new BOAparameter("min", 0.0, "Minimum utility"));
		set.add(new BOAparameter("max", 0.99, "Maximum utility"));

		return set;
	}

	@Override
	public String getName() {
		return "Group 15 Bidding Strategy";
	}
}