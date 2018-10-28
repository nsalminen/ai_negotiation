package ai2018.group15;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 * 
 * Group15:
 * adapted the hard headed frequency model using the paper [...] to compare sets of bids instead of on a pair basis
 */
public class Group15_OM extends OpponentModel {
	
	// The constant factor in the weight update function
	private double alpha;
	// The constant factor in the weight update function
	private double beta;
	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	
	/*
	 * determines the size of the sets of bids
	 */
	private int bidSetSize;
	
	private ArrayList<BidDetails> oppBidSet;
	private ArrayList<BidDetails> prevOppBidSet;

	@Override
	public void init(NegotiationSession negotiationSession,
			Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
		if (parameters != null && parameters.get("s") != null) {
			bidSetSize = (int) Math.round(parameters.get("s"));
		} else {
			bidSetSize = 30;
		}
		if (parameters != null && parameters.get("a") != null) {
			alpha = (int) Math.round(parameters.get("a"));
		} else {
			alpha = 10;
		}
		if (parameters != null && parameters.get("b") != null) {
			beta = (int) Math.round(parameters.get("b"));
		} else {
			beta = 5;
		}
		
		oppBidSet = new ArrayList<BidDetails>(bidSetSize);
		prevOppBidSet = new ArrayList<BidDetails>(bidSetSize);
		
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		initializeModel();

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		
		//add the most recent opp bid
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		oppBidSet.add(oppBid);
		
		//if the set is full perform comparison
		if(oppBidSet.size() == bidSetSize) {
			System.out.println("> current opp bid set is full");
			
			Set<Integer> issueSet = oppBid.getBid().getValues().keySet();
			Set<Integer> noConcedeSet = new HashSet<Integer>();
			//per issue perform pval test and compare new set's estimated utility with previous set's new estimated utility
			boolean concession = false;
			
			if(!prevOppBidSet.isEmpty()) {// if only one set has been filled no comparison can be made
				//compare the sets; hashmap format is HashMap<IssueNumber, HashMap<Value, frequency>>
				HashMap<Integer, HashMap<Value, Integer>> fc = frequencyCount(oppBidSet);
				HashMap<Integer, HashMap<Value, Integer>> prevFc = frequencyCount(prevOppBidSet);
				
				//todo pval test x^2 - test(fc = prevfc)
				
				//loop over all issues
				for(Integer i : issueSet) {
					if(false) { //todo null hypothesis
						noConcedeSet.add(i);
					} else { //null hypothesis rejected, check for concession
						System.out.println("***** Estimating utility *****");
						int EU = estimateSetUtility(fc.get(i), i);
						System.out.println("EU: " + EU);
						int prevEU = estimateSetUtility(prevFc.get(i), i);
						System.out.println("prevEU: " + prevEU);
						concession = (EU < prevEU) ? true : concession; //if new estimated utility is lower then a concession has been made
						System.out.println("Concession: " + concession);
					}
				}
			}
			
			if (concession && noConcedeSet.size() != issueSet.size()) {
				for (Integer issueIndex : noConcedeSet) {
					Objective issue = opponentUtilitySpace.getDomain()
							.getObjectivesRoot().getObjective(issueIndex);
					double currentWeight = opponentUtilitySpace.getWeight(issueIndex);
					double newWeight = currentWeight + (alpha * Math.pow(time, beta));
					
					opponentUtilitySpace.setWeight(issue, newWeight);
				}
			}
			
			// prepare for new set: prevBidSet is empty, copy bidSet to prevBidSet and empty bidSet
			prevOppBidSet = (ArrayList<BidDetails>) oppBidSet.clone();
			oppBidSet.clear();
		}
		
		
	}
	
	/***
	 * 
	 * @param fcount value frequency count of a set of bids for a specific issue
	 * @return the estimated utility (not scaled) depending on fcount
	 */
	public int estimateSetUtility(HashMap<Value, Integer> fcount, int issueNumber) {
		int result = 0;
		ArrayList<BidDetails> allOppBids = (ArrayList<BidDetails>) negotiationSession.getOpponentBidHistory().getHistory();
		for(BidDetails bidDetails : allOppBids) { // loop over all bids
			Bid bid = bidDetails.getBid();
			Value bidIssueValue = bid.getValue(issueNumber);
			if(fcount.containsKey(bidIssueValue)) {
				result += fcount.get(bidIssueValue); //add frequency count to result
			}
		}
		return result;
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group 15 Window Frequency Model";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
		set.add(new BOAparameter("s", (double) 30,
				"The size of the bid sets to compare"));
		set.add(new BOAparameter("a", (double) 10,
				"The constant deduction in the weight update"));
		set.add(new BOAparameter("b", (double) 5,
				"The variable deduction in the weight update, which will determine the decay"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public HashMap<Integer, HashMap<Value, Integer>> frequencyCount(ArrayList<BidDetails> bids) {
		
		HashMap<Integer, HashMap<Value, Integer>> fcount = new HashMap<Integer, HashMap<Value, Integer>>();

		try {
			for (BidDetails bid : bids) {//loop over bids
				HashMap<Integer, Value> bidValues = bid.getBid().getValues();
				for(Integer i : bidValues.keySet()) {//loop over issues
					//get value of bid for issue
					Value bidIssueValue = bidValues.get(i);
					
					HashMap<Value, Integer> valueCount = new HashMap<Value, Integer>();
					if(fcount.containsKey(i)) {//if already a count for issue
						valueCount = fcount.get(i);
						if(valueCount.containsKey(bidIssueValue)) {//if already a count for issue value
							valueCount.put(bidIssueValue, valueCount.get(bidIssueValue) + 1);
						} else { //start count for issue value
							valueCount.put(bidIssueValue,  1);
						}
					} else { //start count for issue
						valueCount.put(bidIssueValue,  1);
					}
					fcount.put(i, valueCount);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return fcount;
	}
}