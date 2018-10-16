package ai2018.group15;

import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

/**
 * This is your negotiation party.
 */
public class Group15 extends AbstractNegotiationParty {

	private Bid lastReceivedBid = null;
	private float acceptableMargin = 0.05f;

	@Override
	public void init(NegotiationInfo info) {

		super.init(info);

		System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
		System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());

		// if you need to initialize some variables, please initialize them
		// below
		

	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {

		if(validActions.contains(Accept.class) && lastReceivedBid != null && isAcceptable(lastReceivedBid)) {
			return new Accept(getPartyId(), lastReceivedBid);
		} else {
			return new Offer(getPartyId(), generateBestBid());
		}
	}
	
	public boolean isAcceptable(Bid bid) {
		//ACCEPT if bid utility is equal to or larger than this party's last bid
		
		//ACCEPT if bid utility is within acceptable margin of party's last bid
		
		//ACCEPT if bid utility is larger than reservation value and it is the last round 
		
		//REJECT if bid utility is lower or equal to reservation value
		return true;
	}
	
	public Bid generateBestBid() {
		
		return generateRandomBid();
	}

	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		if (action instanceof Offer) {
			lastReceivedBid = ((Offer) action).getBid();
		}
	}

	@Override
	public String getDescription() {
		return "Test negotiation party (template)";
	}

}
