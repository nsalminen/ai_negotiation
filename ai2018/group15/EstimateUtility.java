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

public class EstimateUtility{
	NegotiationSession negotiationSession;
	
	public EstimateUtility(NegotiationSession negoSession) {
		this.negotiationSession = negoSession;
	}
	
	public AbstractUtilitySpace getUtilitySpace() {
		Domain domain = negotiationSession.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
		List<Bid> bidOrder = negotiationSession.getUserModel().getBidRanking().getBidOrder();
		
		double[] weights = new double[domain.getIssues().size()];
		for(int i=0;i<bidOrder.size()-1;i++) {
				for(int j =1;j<domain.getIssues().size()+1; j++) {
					Value val1 = bidOrder.get(bidOrder.size()-1-i).getValue(j);
					Value val2 = bidOrder.get(bidOrder.size()-2-i).getValue(j);
					
					if(val1.equals(val2)) {
						weights[j-1] += 1; //Value to fix depending the ranking TODO
					}
				}
		}
		weights = normalisation(weights);
		
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