package ai2018.group15;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import agents.org.apache.commons.math.MathException;
import agents.org.apache.commons.math.stat.inference.ChiSquareTestImpl;
import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
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
	 * The learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * Value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	
	/*
	 * Determines the size of the sets of bids
	 */
	private int bidSetSize;
	private int minBidSetSize = 4;//specific to party domain
	private int maxBidSetSize = 150;
	
	private ArrayList<BidDetails> oppBidSet;
	private ArrayList<BidDetails> prevOppBidSet;
	
	private boolean Conceeded = false;
	
	private boolean ConcessionHandled = false;
	
	ChiSquareTestImpl test;

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
			if(bidSetSize > maxBidSetSize) {
				bidSetSize = maxBidSetSize;
			}
			if(bidSetSize < minBidSetSize) {
				bidSetSize = minBidSetSize;
			}
		} else {
			bidSetSize = 5;
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

		test = new ChiSquareTestImpl();
	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		
		// Add the most recent opponent bid
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		oppBidSet.add(oppBid);
		
		// If the set is full perform comparison
		if(oppBidSet.size() == bidSetSize) {
			System.out.println("updatingOM");
			Set<Integer> issueSet = oppBid.getBid().getValues().keySet();
			Set<Integer> noConcedeSet = new HashSet<Integer>();
			// Per issue, perform pval test and compare new set's estimated utility with previous set's new estimated utility
			boolean concession = false;
			if(!prevOppBidSet.isEmpty()) {// if only one set has been filled no comparison can be made
				// Compare the sets; hashmap format is HashMap<IssueNumber, HashMap<Value, frequency>>
				HashMap<Integer, HashMap<Value, Integer>> fc = frequencyCount(oppBidSet);
				HashMap<Integer, HashMap<Value, Integer>> prevFc = frequencyCount(prevOppBidSet);
				
				// Loop over all issues
				for(Integer i : oppBid.getBid().getValues().keySet()) {
					HashMap<Value, Integer> frequencyCount = fc.get(i);
					HashMap<Value, Integer> prevFrequencyCount = prevFc.get(i);
					
					// Prepare test
					double[] expected = new double[frequencyCount.keySet().size()];
					long[] observed = new long[frequencyCount.keySet().size()];		
					Arrays.fill(expected, 0);
					Arrays.fill(observed, 0);
					int iteration = 0;
					for(Value v : frequencyCount.keySet()) {
						expected[iteration] = frequencyCount.get(v);
						if(prevFrequencyCount.containsKey(v)) { // frequencyCount can contain new values not present in prevFrequencyCount
							observed[iteration] = prevFrequencyCount.get(v);
						} else {
							observed[iteration] = 0;
						}
						iteration++;
					}

					double testResult = 1;
					if(expected.length >= 2 && expected.length == observed.length) {
						try {
							testResult = test.chiSquareTest(expected, observed);
							System.out.println("ChiSquared: " + testResult);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (MathException e) {
							e.printStackTrace();
						}
					}
					
					if (testResult > 0.05) {//null hypothesis
						noConcedeSet.add(i);
					} else { // Null hypothesis rejected, check for concession
						int EU = estimateSetUtility(frequencyCount, i);
						int prevEU = estimateSetUtility(prevFrequencyCount, i);
						System.out.println("EU : " + EU + " prevEU : " + prevEU);
						concession = (EU < prevEU) ? true : concession; //if new estimated utility is lower then a concession has been made
					}
				}
			} 
			
			if (concession && noConcedeSet.size() != issueSet.size()) {
				for (Integer issueIndex : noConcedeSet) {
					Objective issue = opponentUtilitySpace.getDomain()
							.getObjectivesRoot().getObjective(issueIndex);
					double currentWeight = opponentUtilitySpace.getWeight(issueIndex);
					double newWeight = currentWeight + (alpha * Math.pow(time, beta));
					
					System.out.println("issue = " + issue.getName());
					System.out.println("currentWeight = " + currentWeight);
					System.out.println("newWeight = " + newWeight);
					
					opponentUtilitySpace.setWeight(issue, newWeight);
				}
			}
			
			// Then for each issue value that has been offered last time, a constant
			// value is added to its corresponding ValueDiscrete.
			// loop over all issues
			for(BidDetails bid: oppBidSet) {
				
				try {
					for (Entry<Objective, Evaluator> e : opponentUtilitySpace
							.getEvaluators()) {
						EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
						IssueDiscrete issue = ((IssueDiscrete) e.getKey());
						/*
						 * Add constant learnValueAddition to the current preference of
						 * the value to make it more important
						 */
						ValueDiscrete issuevalue = (ValueDiscrete) bid.getBid()
								.getValue(issue.getNumber());
						Integer eval = value.getEvaluationNotNormalized(issuevalue);
						value.setEvaluation(issuevalue, (learnValueAddition + eval));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			// Prepare for new set: prevBidSet contains no or an old bid set, copy bidSet to prevBidSet and empty bidSet
			prevOppBidSet = (ArrayList<BidDetails>) oppBidSet.clone();
			oppBidSet.clear();
			updateConceeded(concession);
			ConcessionHandled = false;
		}
		
		
	}
	
	public void setConcessionHandled() {
		ConcessionHandled = true;
	}
	
	public boolean getConcessionHandled() {
		return ConcessionHandled;
	}
	
	private void updateConceeded(boolean didConcede) {
		System.out.println("didConcede : " + didConcede);
		Conceeded = didConcede;
	}
	
	
	public boolean opponentConceeded() {
		return Conceeded;
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