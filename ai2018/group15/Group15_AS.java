package ai_negotiation.ai2018.group15;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is
 * higher than the bid the agent is ready to present
 * 
 * If the other party concedes, 
 * 
 */
public class Group15_AS extends AcceptanceStrategy {

	private double a;
	private double b;
	private double t;
	private double c;

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

		if (parameters.get("a") != null || parameters.get("b") != null || parameters.get("g") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
			t = parameters.get("t");
			c = parameters.get("c");
					
		} else {
			a = 1;
			b = 0;
			t = 0.9;
			c = 0.5;
		}
	}

	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + " t: " + t + " c: " + c + " ]";
		return str;
	}

	@Override
	public Actions determineAcceptability() {
		double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
				.getMyUndiscountedUtil();

		if (a * lastOpponentBidUtil + b >= nextMyBidUtil) {
			return Actions.Accept;
		}
		else if ( negotiationSession.getTime() > t && lastOpponentBidUtil >= c){
			return Actions.Accept;
		}
		else if (negotiationSession.getTime() > 0.99) {
			return Actions.Accept;
		}
		return Actions.Reject;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 1.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
		set.add(new BOAparameter("b", 0.0,
				"Accept when the opponent's utility * a + b is greater than the utility of our current bid"));
	    set.add(new BOAparameter("t", 0.9, "If time greater than t, then accept"));
	    set.add(new BOAparameter("c", 0.5, "If time is greater than t and the opponent's utility is greater than c, accept"));

		return set;
	}

	@Override
	public String getName() {
		return "Group 15 AS";
	}
}