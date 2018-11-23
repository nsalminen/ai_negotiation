package ai_negotiation.ai2018.group15;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.pow;

import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;

/**
 * This AS determines whether the agent should accept or reject the latest bid. Before the negotiation has 
 * reached a time t, the agent will only accept the opponent's bid if its utility is higher than it would be 
 * in their own next bid. After time t, the agent will accept any bid that is higher than some variable c.
 */
public class Group15_AS extends AcceptanceStrategy {

	private double a;
	private double b;
	private double t;
	private double c;
	
	private UserModel userModel;
	
	
	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group15_AS() {
	}

	public Group15_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta, double gamma, double cee) {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		
		this.a = alpha;
		this.b = beta;
		this.t = gamma;
		this.c = cee;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		userModel = negotiationSession.getUserModel();
		
		if(userModel != null) {
			negotiationSession.setOutcomeSpace(new SortedOutcomeSpace(new EstimateUtility(negotiationSession).getUtilitySpace()));
		}

		if (parameters.get("a") != null || parameters.get("b") != null || parameters.get("g") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
			t = parameters.get("t");
			c = parameters.get("c");
					
		} else {
			a = 1;
			b = 0;
			t = 0.99;
			c = 0.7;
		}
	}

	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + " t: " + t + " c: " + c + " ]";
		return str;
	}

	/**
	 * Determines whether the last bid should be accepted or rejected based on the AS.
	 * @return An action object specifying Accept or Reject.
	 */
	@Override
	public Actions determineAcceptability() {
		
		Bid lastOpponentBid = negotiationSession.getOpponentBidHistory().getLastBid();
		
		if (lastOpponentBid == null) {
			return Actions.Reject;
		}
		
		
		//System.out.println(userModel.getBidRanking().getBidOrder());
		/*if(userModel != null) {
			System.out.println("UNCERTAINTY");
			//Uncertainty, very basic acceptance strategy : TO REWORK
			List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
			System.out.println(bidOrder);
			if(bidOrder.contains(lastOpponentBid))	
			{
				double percentile = (bidOrder.size()
						- bidOrder.indexOf(lastOpponentBid))
						/ (double) bidOrder.size();
				if (percentile < 0.1)
					return Actions.Accept;
			}
		}
		else {*/
			System.out.println(" NO UNCERTAINTY");
			//No uncertainty, normal procedure
			double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
			double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
			
			if (a * lastOpponentBidUtil + b >= nextMyBidUtil) {
				return Actions.Accept;
			}
			else if ( negotiationSession.getTime() > t && lastOpponentBidUtil >= c - getCDiscount()){
				return Actions.Accept;
			}
			else if (negotiationSession.getTime() > 0.99) {
				return Actions.Accept;
			}
		//}
		return Actions.Reject;
	}
	
	public double getCDiscount() {
		return (c * pow(((negotiationSession.getTime() - t)/(1-t)), 2d));
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
		set.add(new BOAparameter("b", 0.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
	    set.add(new BOAparameter("t", 0.99, "If time greater than t, then accept"));
	    set.add(new BOAparameter("c", 0.7, "If time is greater than t and the opponent's utility is greater than c, accept"));
		return set;
	}

	@Override
	public String getName() {
		return "Group 15 AS";
	}
}