package Agents;

import Helpers.Auction;
import Helpers.AuctionStatus;
import Helpers.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;

import java.io.IOException;
import java.util.ArrayList;

public class ArtistManagerAgent extends Agent {
    private static final double REDUCING_PRICE_FACTOR = 0.05;
    private static final int PERIOD_OF_CYCLE = 1000;
    // private static final double ACCEPTANCE_MARGIN = 0.05; It's not needed since when a curator sees that
    // the auctioneer bids less than its own proposal, then the curator wants to buy the artifact for the
    // bid suggested by the auctioneer --> see line 267 CuratorAgent

    private Auction auction;
    private ArrayList<AID> bidders;
    private boolean firstTime = true;


    @Override
    protected void setup() {
        super.setup();
        //start the ArrayList
        bidders = new ArrayList<AID>();

        showArtistManagerInfo();

        findBidders();

        //Start status as ACTIVE
        auction.setStatus(AuctionStatus.ACTIVE);
        informStatus();


        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        parallelBehaviour.addSubBehaviour(new SendNewBid(this, PERIOD_OF_CYCLE));
        parallelBehaviour.addSubBehaviour(new ReceiveProposals(this));
        addBehaviour(parallelBehaviour);

    }

    /**
     * This function is used to make the auction and Show the information
     */
    private void showArtistManagerInfo() {
        auction = new Auction();

        System.out.println("ArtistManagerAgent: " + getAID().getLocalName());
        System.out.println("The Auction is about the artifact: " + auction.getArtifact().getName() + "by the price: " + auction.getRealPrice()+ ". Details of the auction:"
                            + "actual bid: " + auction.getActualBid() + ", reserved price: " + auction.getReservedPrice());

    }

    /**
     * To look for the bidders that are registered on the DF
     */
    private void findBidders() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("bidder");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this, description);
            if (resultAgentDescriptions.length > 0) {
                for (int i = 0; i < resultAgentDescriptions.length; i++)
                    bidders.add(resultAgentDescriptions[i].getName());

            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inform about status of the auction
     */
    private void informStatus() {

        ACLMessage message = new ACLMessage(ACLMessage.INFORM);

        for (AID bidder : bidders) {
            message.addReceiver(bidder);
        }

        try {
            message.setContentObject(auction);
        } catch (IOException e) {
            e.printStackTrace();
        }

        message.setOntology(Ontology.AUCTION);
        this.send(message);
    }

    private void informNewBid() {
        ACLMessage message = new ACLMessage(ACLMessage.CFP);

        for (AID bidder : bidders) {
            message.addReceiver(bidder);
        }

        try {
            message.setContentObject(auction);
        } catch (IOException e) {
            e.printStackTrace();
        }

        message.setOntology(Ontology.AUCTION);
        this.send(message);
    }

    /**
     * This is the bechavior that happens every 10 seconds to send a new bid
     */
    public class SendNewBid extends TickerBehaviour {

        public SendNewBid(Agent agent, int Time) {
            super(agent, Time);
        }

        @Override
        protected void onTick() {

            if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
                double newBid = auction.getActualBid();
                //So the first time is full price
                if (firstTime == false) {
                    newBid = auction.getActualBid() * (1.0-REDUCING_PRICE_FACTOR);
                }
                // If the actual bid is more than the reserved price, continue bidding
                // if not end the bidding
                if (newBid > auction.getReservedPrice()) {
                    System.out.println("Sending new bid: " + newBid);
                    auction.setActualBid(newBid);
                    informNewBid();
                    firstTime = false;
                } else {
                    System.out.println("Auction ended without selling, last bid: " + auction.getActualBid());
                    auction.setStatus(AuctionStatus.FINISHED_ARTIFACT_NOT_SOLD);
                    informStatus();
                }


            } else {
                System.out.println("The Auction has ended.");
                informStatus();
                stop();

            }

        }
    }

    /**
     * This is the behavior when it receives the proposals
     */
    public class ReceiveProposals extends CyclicBehaviour {

        public ReceiveProposals(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            MessageTemplate msgTemplate = MessageTemplate.MatchOntology(Ontology.AUCTION);
            ACLMessage msg = receive(msgTemplate);

            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    Auction proposal = null;
                    try {
                        proposal = (Auction) msg.getContentObject();
                    } catch (UnreadableException e) {
                        auction.setStatus(AuctionStatus.ERROR);
                        informStatus();
                        e.printStackTrace();
                    }

                    // If everything goes well
                    if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {

                        //If the proposal is in the acceptance margin ACCEPT
                        //if ((auction.getActualBid() - auction.getActualBid() * ACCEPTANCE_MARGIN < proposal.getActualBid()) &&
                        //        (auction.getActualBid() + auction.getActualBid() * ACCEPTANCE_MARGIN > proposal.getActualBid())) {
                        if (auction.getActualBid() == proposal.getActualBid()){
                            System.out.println("Proposal from bidder" + msg.getSender().getLocalName()
                                    + " is accepted " + " artifact is sold for " + proposal.getActualBid());

                            //ACCEPT PROPOSAL
                            auction.setStatus(AuctionStatus.FINISHED_ARTIFACT_SOLD);
                            auction.setSoldInformation(msg.getSender(), proposal.getActualBid());

                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

                            try {
                                reply.setContentObject(auction);
                            } catch (IOException e) {
                                auction.setStatus(AuctionStatus.ERROR);
                                informStatus();
                                e.printStackTrace();
                            }

                            reply.setOntology(Ontology.AUCTION);
                            send(reply);
                        } else {
                            System.out.println("Proposal from bidder rejected - " + auction.getActualBid()
                                    + " from " + msg.getSender().getLocalName());
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);

                            try {
                                reply.setContentObject(auction);
                            } catch (IOException e) {
                                auction.setStatus(AuctionStatus.ERROR);
                                informStatus();
                                e.printStackTrace();
                            }

                            reply.setOntology(Ontology.AUCTION);
                            send(reply);
                        }
                    } else {
                        // Auction ended
                        System.out.println("Proposal from bidder " + msg.getSender().getName()
                                + " rejected, auction already ended...");

                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);

                        try {
                            reply.setContentObject(auction);
                        } catch (IOException e) {
                            auction.setStatus(AuctionStatus.ERROR);
                            informStatus();
                            e.printStackTrace();
                        }

                        reply.setOntology(Ontology.AUCTION);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        }
    }
}



