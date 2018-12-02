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
public class Group15_HHOM extends OpponentModel {
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
		int numberOfUnchanged = 0;
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 2);
		HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid,
				oppBid);

		// count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0)
				numberOfUnchanged++;
		}

		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain()
					.getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

			if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
				newWeight = (weight + goldenValue) / totalSum;
			} else {
				newWeight = weight / totalSum;
			}
			opponentUtilitySpace.setWeight(issue, newWeight);
		}

		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
		try {
			for (Entry<Objective, Evaluator> e : opponentUtilitySpace
					.getEvaluators()) {
				EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
				IssueDiscrete issue = ((IssueDiscrete) e.getKey());
				/*
				 * add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
						.getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
			
			double myUtil = opponentUtilitySpace.getUtility(oppBid.getBid());
			double myUtilPrev = opponentUtilitySpace.getUtility(prevOppBid.getBid());
			if(myUtil > myUtilPrev) {
				updateConceeded(true);
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
		return "Adapted HH Frequency Model";
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
	
	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 * 
	 * @param a
	 *            bid of the opponent
	 * @param another
	 *            bid
	 * @return
	 */
	private HashMap<Integer, Integer> determineDifference(BidDetails first,
			BidDetails second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}
	
	
}