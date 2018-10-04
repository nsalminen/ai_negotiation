package ai_negotiation.ai2018.group15;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy
 * adapted from [1] [1] S. Shaheen Fatima Michael Wooldridge Nicholas R.
 * Jennings Optimal Negotiation Strategies for Agents with Incomplete
 * Information http://eprints.ecs.soton.ac.uk/6151/1/atal01.pdf
 * 
 * The default strategy was extended to enable the usage of opponent models.
 * 
 * Note that this agent is not fully equivalent to the theoretical model,
 * loading the domain may take some time, which may lead to the agent skipping
 * the first bid. A better implementation is GeniusTimeDependent_Offering.
 */
public class Group15_BS extends OfferingStrategy {

	/**
	 * k in [0, 1]. For k = 0 the agent starts with a bid of maximum utility
	 */
	private double k;
	/** Maximum target utility */
	private double Pmax;
	/** Minimum target utility */
	private double Pmin;
	/** Concession factor */
	private double e;
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;
	
	/** Bid selector */
	private BidSelector bs;
	/** max window size */
	private double maxWindowSize = 0.12;
	/** max bid count in range*/
	private int maxBids = 5;
	/** concession amount */
	private double concessSize = 0.06; 	
	/** concession probability */
	private int concessProba = 90;
	
	/** opponent last bid util */
	private double opponentLastBidUtil = -1;
	/** opponent last concession amount */
	private double opponentLastConcessionAmount = 0;
	
	/**
	 * Method which initializes the agent by setting all parameters. The
	 * parameter "e" is the only parameter which is required.
	 */
	@Override
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
			Map<String, Double> parameters) throws Exception {
		super.init(negoSession, parameters);
		if (parameters.get("e") != null) {
			this.negotiationSession = negoSession;

			outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
			negotiationSession.setOutcomeSpace(outcomespace);
			
			bs = new BidSelector(outcomespace, maxWindowSize, maxBids, concessSize);			 

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
		return bs.GetFirstBid();
	}

	/**
	 * Simple offering strategy which retrieves the target utility and looks for
	 * the nearest bid if no opponent model is specified. If an opponent model
	 * is specified, then the agent return a bid according to the opponent model
	 * strategy.
	 */
	@Override
	public BidDetails determineNextBid() {
		/*double time = negotiationSession.getTime();
		double utilityGoal;
		utilityGoal = p(time);*/
		
		
		// myAction can take values : 1 = update window, 2 = slide window (concession)
		int myAction = 1;
		if(didOpponentConcede() && randomConcede())
			myAction = 2;
		nextBid = bs.GetNextBid(myAction, opponentLastConcessionAmount);

		return nextBid;
	}
	
	/**
	 * 
	 * @return if opponent's bid was a conceeding bid, relative to the opponent's previous bid and the agent's utility function
	 */
	private boolean didOpponentConcede() {
		if (!negotiationSession.getOpponentBidHistory().getHistory().isEmpty()) {
			double opponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
			if(opponentLastBidUtil < opponentBidUtil) {
				if(opponentLastBidUtil != -1) {
	            	opponentLastConcessionAmount = opponentBidUtil - opponentLastBidUtil;
	            	System.out.println("Opponent conceeded!");
	            	System.out.println(opponentLastConcessionAmount);
				}
	            opponentLastBidUtil = opponentBidUtil;
	            if(opponentLastBidUtil != -1)
	            	return true;
			} else {
				opponentLastBidUtil = opponentBidUtil;
				return false;
			}
		}
		return false;
	}
	
	private boolean randomConcede() {
		Random rng = new Random();
		return rng.nextInt(100) > concessProba;
	}

	/**
	 * From [1]:
	 * 
	 * A wide range of time dependent functions can be defined by varying the
	 * way in which f(t) is computed. However, functions must ensure that 0 <=
	 * f(t) <= 1, f(0) = k, and f(1) = 1.
	 * 
	 * That is, the offer will always be between the value range, at the
	 * beginning it will give the initial constant and when the deadline is
	 * reached, it will offer the reservation value.
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
	 * Makes sure the target utility with in the acceptable range according to
	 * the domain Goes from Pmax to Pmin!
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
		return "Group 15 BS";
	}
}