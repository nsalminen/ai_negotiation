package ai2018.group15;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.boaframework.NegotiationSession;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.utility.AbstractUtilitySpace;

/* The EstimateUtility class is used to estimate the utility space when the negotiation is
*  done under preference uncertainty.
*/
public class EstimateUtility {

	NegotiationSession negotiationSession;

	public EstimateUtility(NegotiationSession negoSession) {
		this.negotiationSession = negoSession;
	}

	/**
	 * This function firstly estimates the weights of the Issues. It does this by
	 * comparing the values of the issues of pairs of bids, starting from the two
	 * bids with the best rank. If both bids have the same value for one Issue, the
	 * weight of this issue is incremented.
	 * 
	 * The estimation of the evaluation value for each possible Value for an Issue
	 * is done by incrementing the weight of some value each time we have a bid with
	 * this value. For each value, the final weight is the weight divided by the
	 * rank of the Issues.
	 * 
	 * 
	 * @return estimated utility space based on the bid ranking
	 * 
	 */

	public AbstractUtilitySpace getUtilitySpace() {
		Domain domain = negotiationSession.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
		List<Bid> bidOrder = negotiationSession.getUserModel().getBidRanking().getBidOrder();

		double[] weights = new double[domain.getIssues().size()]; // Get current weights of the Issues

		for (int i = 0; i < bidOrder.size() - 1; i++) { // Reweigh issues based on bid pairs
			for (int j = 1; j < domain.getIssues().size() + 1; j++) {
				Value val1 = bidOrder.get(bidOrder.size() - 1 - i).getValue(j);
				Value val2 = bidOrder.get(bidOrder.size() - 2 - i).getValue(j);

				if (val1.equals(val2)) {
					weights[j - 1] += ((double) i) / bidOrder.size(); // Add weight based on rank
				}
			}
		}

		weights = normalisation(weights); // Normalize the weights

		List<HashMap<Value, List<Integer>>> valueWeights = new ArrayList<HashMap<Value, List<Integer>>>();

		for (int j = 0; j < domain.getIssues().size(); j++) {
			valueWeights.add(new HashMap<Value, List<Integer>>());
		}

		for (int i = 0; i < bidOrder.size() - 1; i++) {
			for (int j = 0; j < domain.getIssues().size(); j++) {
				Value val = bidOrder.get(bidOrder.size() - 1 - i).getValue(j + 1);
				if (valueWeights.get(j).containsKey(val)) {
					int a = valueWeights.get(j).get(val).get(1) + 1;
					valueWeights.get(j).get(val).set(1, a);
				} else {
					List<Integer> list = new ArrayList<Integer>();
					list.add(valueWeights.get(j).size() + 1);
					list.add(1);
					valueWeights.get(j).put(val, list);
				}
			}
		}

		for (int j = 0; j < domain.getIssues().size(); j++) {
			for (Value val : valueWeights.get(j).keySet()) {
				int weight = valueWeights.get(j).get(val).get(1) / valueWeights.get(j).get(val).get(0);
				factory.setUtility(domain.getIssues().get(j), (ValueDiscrete) val, weight);
			}
		}

		factory.getUtilitySpace().setWeights(domain.getIssues(), weights);

		return factory.getUtilitySpace();
	}

	/*
	 * This function normalizes an array an returns it.
	 * 
	 * @param Array with values of type double
	 * 
	 * @return Normalized array
	 */
	private double[] normalisation(double[] array) {
		double sum = 0.0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		for (int i = 0; i < array.length; i++) {
			array[i] = array[i] / sum;
		}
		return array;
	}
}
