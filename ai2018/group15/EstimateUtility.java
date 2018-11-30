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

/* 
* The EstimateUtility class is used to estimate the utility space when the negotiation is
* done under preference uncertainty.
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
	 * this value. For each value, the final weight TODO (update comment, code has
	 * changed) is the weight divided by the rank of the Issues.
	 * 
	 * 
	 * @return estimated utility space based on the bid ranking
	 */

	public AbstractUtilitySpace getUtilitySpace() {
		Domain domain = negotiationSession.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
		List<Bid> bidOrder = negotiationSession.getUserModel().getBidRanking().getBidOrder();

		double[] weights = new double[domain.getIssues().size()]; // Get current weights of the Issues

		for (int i = 0; i < bidOrder.size() - 1; i++) { // Reweigh issues based on bid pairs
			for (int j = 1; j < domain.getIssues().size() + 1; j++) {
				Value val1 = bidOrder.get(i).getValue(j);
				Value val2 = bidOrder.get(i + 1).getValue(j);

				if (val1.equals(val2)) {
					weights[j - 1] += (((double) i) + 1) / bidOrder.size(); // Add weight based on rank
				}
			}
		}

		weights = normalisation(weights); // Normalize the weights

		// Estimate the estimation value for each possible Value of an Issue

		List<HashMap<Value, Double>> valueWeights = new ArrayList<HashMap<Value, Double>>();

		for (int j = 0; j < domain.getIssues().size(); j++) {
			valueWeights.add(new HashMap<Value, Double>());
		}

		for (int i = 0; i < bidOrder.size() - 1; i++) {
			for (int j = 0; j < domain.getIssues().size(); j++) {
				Value val = bidOrder.get(i).getValue(j + 1);
				if (valueWeights.get(j).containsKey(val)) {
					double a = valueWeights.get(j).get(val) + (((double) i) + 1) / bidOrder.size();
					valueWeights.get(j).put(val, a);
				} else {
					valueWeights.get(j).put(val, (((double) i) + 1) / bidOrder.size());
				}
			}
		}

		for (int j = 0; j < domain.getIssues().size(); j++) {
			double weightSum = 0;
			for (Value val : valueWeights.get(j).keySet()) {
				weightSum += valueWeights.get(j).get(val);
			}
			for (Value val : valueWeights.get(j).keySet()) {
				double weight = valueWeights.get(j).get(val) / weightSum;
				factory.setUtility(domain.getIssues().get(j), (ValueDiscrete) val, weight);
			}
		}

		factory.getUtilitySpace().setWeights(domain.getIssues(), weights);

		return factory.getUtilitySpace();
	}

	/*
	 * This function normalizes an array an returns it.
	 * 
	 * @param sourceArray array with values of type double
	 * 
	 * @return normalized array
	 */
	private double[] normalisation(double[] sourceArray) {
		double sum = 0.0;
		for (int i = 0; i < sourceArray.length; i++) {
			sum += sourceArray[i];
		}
		for (int i = 0; i < sourceArray.length; i++) {
			sourceArray[i] = sourceArray[i] / sum;
		}
		return sourceArray;
	}
}
