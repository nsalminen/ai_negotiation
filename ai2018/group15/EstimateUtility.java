package ai_negotiation.ai2018.group15;

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

/* The EstimateUtility class is used to estimate the utilityspace when the negotiation is
*  done under preference uncertainty.
*/
public class EstimateUtility{

	NegotiationSession negotiationSession;

	/*
	* Constructor
	*/
	public EstimateUtility(NegotiationSession negoSession) {
		this.negotiationSession = negoSession;
	}

	/* Return an estimated utilityspace based on the BidRanking
	 *
	 */
	public AbstractUtilitySpace getUtilitySpace() {
		Domain domain = negotiationSession.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
		List<Bid> bidOrder = negotiationSession.getUserModel().getBidRanking().getBidOrder();

		double[] weights = new double[domain.getIssues().size()]; //Weights of the Issues
		/* In order to estimate the weights of the Issues, the program will compare the values
		 * of the issues of two bids starting from the 2 bids with the best rank. If both bids
		 * have the same value for one Issue, we increment the weight of this issue.
		 */
		for(int i=0;i<bidOrder.size()-1;i++) {
				for(int j =1;j<domain.getIssues().size()+1; j++) {
					Value val1 = bidOrder.get(bidOrder.size()-1-i).getValue(j);
					Value val2 = bidOrder.get(bidOrder.size()-2-i).getValue(j);

					if(val1.equals(val2)) {
						weights[j-1] += 1; //Value to fix depending the ranking TODO
					}
				}
		}
		weights = normalisation(weights); //normalisation of the weights

		/* The estimation of the evalution value for the each possible Value for an Issue
		 * is done by incrementing the weight of some value each time we have a bid with this value.
		 * The program also keeps a rank of the values of an Issues. (The first value that appears starting from
		 * the best ranked bid has the rank 1) Then for each value, the final weight is the weight divided by the rank of this value.
		 */
		List<HashMap<Value, List<Integer>>> valueWeights = new ArrayList<HashMap<Value, List<Integer>>>();
		for(int j =0;j<domain.getIssues().size(); j++) {
			valueWeights.add(new HashMap<Value, List<Integer>>());
		}
		for(int i=0;i<bidOrder.size()-1;i++) {
			for(int j =0;j<domain.getIssues().size(); j++) {
				Value val = bidOrder.get(bidOrder.size()-1-i).getValue(j+1);
				if(valueWeights.get(j).containsKey(val)) {
					int a = valueWeights.get(j).get(val).get(1) + 1;
					valueWeights.get(j).get(val).set(1, a);
				}
				else {
					List<Integer> list = new ArrayList<Integer>();
					list.add(valueWeights.get(j).size() +1);
					list.add(1);
					valueWeights.get(j).put(val, list);
				}
			}
		}

		for(int j =0;j<domain.getIssues().size(); j++) {
			for(Value val : valueWeights.get(j).keySet()) {
				int weight = valueWeights.get(j).get(val).get(1)/valueWeights.get(j).get(val).get(0);
				factory.setUtility(domain.getIssues().get(j), (ValueDiscrete) val, weight);
			}
		}

		factory.getUtilitySpace().setWeights(domain.getIssues(), weights);

		return factory.getUtilitySpace();

	}
	/*
	 * Small function used for the normalisation of a vector.
	 */
	private double[] normalisation(double[] array) {
		double sum = 0.0;
		for(int i =0; i< array.length;i++) {
			sum+= array[i];
		}
		for(int i =0; i< array.length;i++) {
			array[i] = array[i]/sum;
		}
		return array;
	}
}
