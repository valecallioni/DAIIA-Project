package Mobility.Agents;

import Mobility.Helpers.*;
import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.NameClashException;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ArtistManagerAgent extends Agent {
    private static final double REDUCING_PRICE_FACTOR = 0.05;
    private static final int PERIOD_OF_CYCLE = 5000;
    // private static final double ACCEPTANCE_MARGIN = 0.05; It's not needed since when a curator sees that
    // the auctioneer bids less than its own proposal, then the curator wants to buy the artifact for the
    // bid suggested by the auctioneer --> see line 267 CuratorAgent

    private Artifact artifact;
    private Auction auction;
    private ArrayList<AID> bidders;
    private boolean firstTime = true;

    private HashMap<String, Location> locations;
    private Location home;
    private AID originalAgent;
    private HashMap<String, SoldInformation> sellingInformation;
    private boolean messagesReceived = false;
    private int localAuctionEnded = 1;

    int cloneNumber = 1;


    @Override
    protected void setup() {
        super.setup();

        showArtistManagerInfo();

        registerAsArtistManager();

        locations = new HashMap<String, Location>();
        sellingInformation = new HashMap<String, SoldInformation>();
        home = here();

        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());

        getLocations();

        getOriginalAgent();

        cloneArtistManager();

        addBehaviour(new ReceivingBehavior());

    }


    /**
     * This function is used to make the auction and Show the information
     */
    private void showArtistManagerInfo() {
        artifact = new Artifact();

        System.out.println("ArtistManagerAgent: " + getAID().getLocalName());
        //System.out.println("The Auction is about the artifact: " + auction.getArtifact().getName() + "by the price: " + auction.getRealPrice()+ ". Details of the auction:"
        //                    + "actual bid: " + auction.getActualBid() + ", reserved price: " + auction.getReservedPrice());

    }

    /**
     * To look for the bidders that are registered on the DF
     */
    private void findBidders() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("bidder in " + here().getName());
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

        System.out.println("Bidders in " + here().getName());
        for (AID bidder: bidders)
            System.out.println(bidder.getLocalName());

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
                    newBid = auction.getActualBid() * (1.0 - REDUCING_PRICE_FACTOR);
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
                localAuctionEnded++;
                if (localAuctionEnded ==2){
                    sendMessageAndReturn();
                }
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
                        if (auction.getActualBid() == proposal.getActualBid()) {
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


    // HOMEWORK 3

    /**
     * This function is used to register in the Artist Manager DF
     */
    private void registerAsArtistManager(){
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("ArtistManager");
        serviceDescription.setName(getName());
        description.addServices(serviceDescription);
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Store all locations
     */
    private void getLocations() {
        // Send request
        sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));
        // Receive response from AMS
        try {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchSender(getAMS()),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage resp = blockingReceive(mt);
            ContentElement ce = getContentManager().extractContent(resp);
            Result result = (Result) ce;
            jade.util.leap.Iterator it = result.getItems().iterator();
            while (it.hasNext()) {
                Location loc = (Location) it.next();
                locations.put(loc.getName(), loc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes a request to clone agent
     */
    private void sendRequest(Action action) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.setLanguage(new SLCodec().getName());
        request.setOntology(MobilityOntology.getInstance().getName());
        try {
            getContentManager().fillContent(request, action);
            request.addReceiver(action.getActor());
            send(request);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Remember the AID of the original agent
     */
    private void getOriginalAgent() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("ArtistManager");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this, description);
            if (resultAgentDescriptions.length > 0) {
                for (int i = 0; i < resultAgentDescriptions.length; i++) {
                    if (!resultAgentDescriptions[i].getName().getLocalName().equals("Clone")) {
                        originalAgent = resultAgentDescriptions[i].getName();
                    }
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }

    /**
     * Clone the Artist Manager
     */
    private void cloneArtistManager() {
        SequentialBehaviour cloningBehaviour = new SequentialBehaviour();
        System.out.println("print location values: "+ locations.values());
        for (final Location location : locations.values()) {
            if (!location.equals(home)) {
                cloningBehaviour.addSubBehaviour(new OneShotBehaviour() {
                    @Override
                    public void action() {
                        if (getLocalName().contains("Clone") == false){
                            final String cloneName = getLocalName()  + "_Clone_" + cloneNumber;
                            doClone(location, cloneName);
                            cloneNumber++;
                        }
                    }
                });

            }
        }
        addBehaviour(cloningBehaviour);
    }

    /**
     * To start auctions send messages and moves back
     */
    @Override
    protected void afterClone() {
        super.afterClone();

        doWait(5000);

        System.out.println("The Artist Manager is in: " + here().getName() + " and is starting the auction.");

        setupAuction();

    }

    /**
     * To start Auctions in the containers
     */
    private void setupAuction() {
        auction = new Auction();
        auction.setArtifact(artifact);

        System.out.println("The Auction is about the artifact: " + auction.getArtifact().getName() + "by the price: " + auction.getRealPrice() + ". Details of the auction:"
                + "actual bid: " + auction.getActualBid() + ", reserved price: " + auction.getReservedPrice());

        //start the ArrayList
        bidders = new ArrayList<AID>();

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
     * This function is called in the SendNewBid behavior: when the auction ends
     * the clone must inform the original agent about the selling price and return home
     */
    private void sendMessageAndReturn(){
        System.out.println("Sending the message");
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.setOntology(Ontology.ENDED_AUCTION);
        message.addReceiver(originalAgent);
        try {
            message.setContentObject(auction.getSoldInformation());
        } catch (IOException e) {
            e.printStackTrace();
        }
        send(message);

        doWait(5000);

        doMove(home);
    }

    /**
     * Deletes the agent after it moved back to the Main Container
     */
    @Override
    protected void afterMove() {
        super.afterMove();
        System.out.println("ArtistManager clone " + getLocalName() + " moved back");
        doWait(5000);
        doDelete();
    }

    /**
     * This behavior is so it finishes after the original agent receives the information
     * about the selling from both the clones
     */
    public class ReceivingBehavior extends SimpleBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchOntology(Ontology.ENDED_AUCTION);
            ACLMessage message = receive(mt);

            if (message != null) {
                System.out.println("Received a message");
                SoldInformation soldinformation;
                try {
                    soldinformation = (SoldInformation) message.getContentObject();
                    sellingInformation.put(message.getSender().getLocalName(), soldinformation);
                    System.out.println("putting the selling info on the hashmap");
                } catch (UnreadableException el) {
                    el.printStackTrace();
                }

                //System.out.println("The sellingInformation size: " +sellingInformation.size() + " and the locations: " + locations.size() );
                if (sellingInformation.size() == (locations.size() -1)) {
                    System.out.println("We have received all the infos");
                    double highestPrice = Double.MIN_VALUE;
                    String highestBuyer = "";
                    for (SoldInformation info : sellingInformation.values()) {
                        if (info.getSoldPrice() > highestPrice) {
                            highestPrice = info.getSoldPrice();
                            highestBuyer = info.getBuyer().getLocalName();
                        }
                    }
                    System.out.println("Highest price for auction was: " + highestPrice + " bought by: " + highestBuyer);
                    messagesReceived = true;
                }
            } else {
                block();
            }

        }

        @Override
        public boolean done() {
            if (messagesReceived) return true;
            else return false;
        }
    }



}