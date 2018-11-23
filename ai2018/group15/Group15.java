package ai_negotiation.ai2018.group15;

import java.util.HashMap;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.boaframework.BoaParty;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.SessionData;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.persistent.PersistentDataType;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.utility.AbstractUtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group15 extends BoaParty {
	
	public Group15 () {
		super (new Group15_AS(), null , new Group15_BS() , null ,
		new Group15_OM() , null , new Group15_OMS(),null);
	}

	@Override
	public void init(NegotiationInfo info) {
		
		if(isUncertain()) {
			this.utilitySpace = estimateUtilitySpace();
		}
		
		SessionData sessionData = null ;
		if ( info.getPersistentData().getPersistentDataType () == PersistentDataType.SERIALIZABLE ) {
			sessionData = ( SessionData ) info . getPersistentData (). get ();
		}
		if ( sessionData == null ) {
			sessionData = new SessionData ();
		}
		
		negotiationSession = new NegotiationSession (sessionData ,
		utilitySpace , info.getTimeline(), null, info.getUserModel());
		
		opponentModel.init(negotiationSession, new HashMap <String, Double>());
		omStrategy.init(negotiationSession, opponentModel, new HashMap <String, Double>());
		HashMap <String,Double> map = new HashMap <String, Double>();
		map.put("e", 1.0);
		try {
			offeringStrategy.init(negotiationSession ,opponentModel , omStrategy, map);
			acceptConditions.init(negotiationSession, offeringStrategy, opponentModel, new HashMap <String, Double>());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public AbstractUtilitySpace estimateUtilitySpace() {
		return new AdditiveUtilitySpaceFactory (
				getDomain ()). getUtilitySpace ();
	}

	@Override
	public String getDescription() {
		return "Test negotiation party (template)";
	}

}
