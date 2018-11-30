package ai2018.group15;

import java.util.HashMap;

import genius.core.boaframework.BoaParty;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.SessionData;
import genius.core.parties.NegotiationInfo;
import genius.core.persistent.PersistentDataType;

// This class contains all the different strategies of our negotiation party.
public class Group15 extends BoaParty {

	public Group15() {
		super(null, new HashMap<String, Double>(), null, new HashMap<String, Double>(), null,
				new HashMap<String, Double>(), null, new HashMap<String, Double>());
	}

	@Override
	public void init(NegotiationInfo info) {
		SessionData sessionData = null;
		if (info.getPersistentData().getPersistentDataType() == PersistentDataType.SERIALIZABLE) {
			sessionData = (SessionData) info.getPersistentData().get();
		}
		if (sessionData == null) {
			sessionData = new SessionData();
		}

		negotiationSession = new NegotiationSession(sessionData, info.getUtilitySpace(), info.getTimeline(), null,
				null);
		opponentModel = new Group15_OM();
		opponentModel.init(negotiationSession, null);
		omStrategy = new Group15_OMS();
		omStrategy.init(negotiationSession, opponentModel, null);
		offeringStrategy = new Group15_BS();
		HashMap<String, Double> map = new HashMap<String, Double>();
		map.put("e", 1.0);
		try {
			offeringStrategy.init(negotiationSession, opponentModel, omStrategy, map);
		} catch (Exception e) {
			e.printStackTrace();
		}
		acceptConditions = new Group15_AS(negotiationSession, offeringStrategy, 1, 0, 0.99, 0.7);
	}

	@Override
	public String getDescription() {
		return "Group 15";
	}

}
