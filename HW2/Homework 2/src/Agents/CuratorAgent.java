package Agents;

import Helpers.*;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;

import java.io.IOException;
import java.util.ArrayList;

public class CuratorAgent extends Agent {

    private ArrayList<Artifact> gallery;

    // Define the strategy of the curator
    private double increasingPriceFactor; //= (double) (Math.random()*0.2);
    // Initialization of the bid done afterwards
    private double curatorsBid = 0.0;

    @Override
    protected void setup(){
        super.setup();


        registerAsBidder();

        addBehaviour(new ParticipatingInAuction (this));


        /*
        showCuratorInfo();

        register();

        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        MessageTemplate respondToTourGuideAgent = MessageTemplate.MatchOntology(Ontology.REQUEST_MAKE_TOUR);
        MessageTemplate respondToProfilerAgent = MessageTemplate.MatchOntology(Ontology.REQUEST_ARTIFACT_INFO);

        parallelBehaviour.addSubBehaviour(new RespondToTourGuideAgent(this, respondToTourGuideAgent));
        parallelBehaviour.addSubBehaviour(new RespondToProfilerAgent(this, respondToProfilerAgent));

        addBehaviour(parallelBehaviour);

        */


    }





    /**
     * This function is used to make a gallery and show the current Curator Agent
     */
    private void showCuratorInfo() {

        gallery = new ArrayList<>();
        for (int i = 1; i < 1000; i++) {
            Artifact artifact = new Artifact();
            gallery.add(artifact);
            //System.out.println(artifact.getId() + " " + artifact.getGenre() + " created by "
             //       + artifact.getCreator() + " in " + artifact.getCreationYear());
        }
    }

    /**
     * This function is used to register in the Curator Service
     */
    private void register(){
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Curator");
        serviceDescription.setName(getName());
        description.addServices(serviceDescription);
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get TourGuideAgent to MAKE TOUR request and reply with Artifact IDS
     */
    class RespondToTourGuideAgent extends SimpleAchieveREResponder {

        public RespondToTourGuideAgent(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request){
            return null;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request,
                                                       ACLMessage response) throws FailureException {
            System.out.println("CuratorAgent received request to make a tour from Profiler Agent");

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.INFORM);

            try {
                User user = (User) request.getContentObject();

                ArrayList<Long> idsToSend = new ArrayList<>();

                for (Artifact artifact : gallery) {
                    if(artifact.getGenre().equals(user.getInterestGenre()) &&
                            ((artifact.getCreationYear() - (artifact.getCreationYear() % 100) ) == user.getInterestCentury()) &&
                            (artifact.getCreator().equals(user.getInterestCreator())
                            )){
                        idsToSend.add(artifact.getId());
                    }
                }

                reply.setContentObject(idsToSend);
            }
            catch (IOException | UnreadableException e) {
                e.printStackTrace();
            }

            return reply;
        }
    }

    /**
     * Get request from Profiler Agent and Build Artifact list based on Artifact IDS
     */
    class RespondToProfilerAgent extends SimpleAchieveREResponder {

        public RespondToProfilerAgent(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request){
            return null;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request,
                                                       ACLMessage response) throws FailureException {
            System.out.println("CuratorAgent received tour details request from ProfilerAgent");

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.INFORM);

            try {
                ArrayList<Long> ids = (ArrayList<Long>) request.getContentObject();

                ArrayList<Artifact> artifactsToSend = new ArrayList<>();
                for (Long id : ids) {
                    for (Artifact artifact : gallery) {
                        if(id==artifact.getId()) artifactsToSend.add(artifact);
                    }
                }

                reply.setContentObject(artifactsToSend);
            }
            catch (IOException | UnreadableException e) {
                e.printStackTrace();
            }

            return reply;
        }
    }

    /**
     * When ending the CuratorAgent
     */
    @Override
    protected void takeDown() {
        System.out.println("CuratorAgent: " + getAID().getLocalName() + " shutdown.");
    }



    // Homework2

    /**
     * To register on the DF
     */
    private void registerAsBidder() {
        // The following code is to set the increasingPriceFactor
        double foo = 0;
        String name= getAID().getLocalName();
        name= name.replaceAll("[^0-9]", "");

        try {
            foo = Integer.parseInt(name);
        }catch (Exception e){
            e.printStackTrace();
        }

        increasingPriceFactor = foo/10;

        System.out.println("CuratorAgent: " + getAID().getLocalName() + " increases the bid by a factor " + increasingPriceFactor);

        DFAgentDescription description = new DFAgentDescription();
        description.setName(this.getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("bidder");
        serviceDescription.setName(this.getName());
        description.addServices(serviceDescription);
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cyclic behavior to receive and send ACL messages
     */
    public class ParticipatingInAuction extends CyclicBehaviour {

        public ParticipatingInAuction(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {
            //double curatorsBid;

            MessageTemplate mt = MessageTemplate.MatchOntology(Ontology.AUCTION);
            ACLMessage message = receive(mt);

            if (message != null) {

                //Get the content
                Auction auction = null;
                try {
                    auction = (Auction) message.getContentObject();
                } catch (UnreadableException e1) {
                    e1.printStackTrace();
                }

                //Here we set the bid of the curator
                if (curatorsBid == 0.0){
                    // As first proposal, the curator tries to buy the artifact at its actual price
                    setCuratorsBid(auction.getRealPrice());
                }
                else{
                    // During the auction, the curator computes bids using its strategy
                    setCuratorsBid(curatorsBid);
                }

                System.out.println("Curator " + getLocalName() + " current bid: " + curatorsBid);

                if (auction != null) {
                    switch (message.getPerformative()) {
                        //If it is an inform message then curator is informed about the status of the auction.
                        case ACLMessage.INFORM: {
                            if (auction.getStatus().equals(AuctionStatus.ACTIVE)) {
                                System.out.println(getLocalName() + ": Auction started with auctioneer's bid: " + auction.getActualBid());
                            } else if (auction.getStatus().equals(AuctionStatus.FINISHED_ARTIFACT_SOLD)) {
                                System.out.println(getLocalName() + ": Auction ended, artifact sold for: " + auction.getActualBid());
                            } else if (auction.getStatus().equals(AuctionStatus.FINISHED_ARTIFACT_NOT_SOLD)) {
                                System.out.println(getLocalName() + ": Auction ended, artifact not sold. Last auctioneer's bid: " + auction.getActualBid());
                            } else if (auction.getStatus().equals(AuctionStatus.ERROR)) {
                                System.out.println("Error, ending auction");
                            }

                            break;
                        }

                        //If it is a call for proposal, curator make a proposal only if the auctioneer's bid is not too high
                        case ACLMessage.CFP: {
                            if (auction.getActualBid() <= curatorsBid) {
                                System.out.println(getAID().getLocalName() + ": Making a proposal to buy artifact for " + auction.getActualBid());

                                ACLMessage reply = message.createReply();
                                reply.setPerformative(ACLMessage.PROPOSE);
                                // auction.setProposal(curatorsBid);
                                auction.setProposal(auction.getActualBid());

                                try {
                                    reply.setContentObject(auction);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                reply.setOntology(Ontology.AUCTION);
                                send(reply);
                            }
                            // Auctioneer's bid too high. Do nothing and wait for next bid
                            else {
                                System.out.println(getAID().getLocalName() +" is not bidding");
                            }
                            break;
                        }
                        //If it is a message about winning the auction
                        case ACLMessage.ACCEPT_PROPOSAL: {
                            // The bid the buyer proposed was accepted.
                            System.out.println(getAID().getLocalName() + ": won the auction for price " + auction.getActualBid());
                            break;
                        }

                        //If it is a message about losing the auction
                        case ACLMessage.REJECT_PROPOSAL: {
                            // The bid the buyer proposed was rejected.
                            System.out.println(getAID().getLocalName()+ ": lost the auction.");
                            break;
                        }

                        default: {
                            break;
                        }
                    }

                }
            } else {
                block();
            }
        }
    }


    private void setCuratorsBid(double previousBid){

        // If the curator hasn't bit before
        if (curatorsBid == 0.0) {
            int random = (int) (Math.random()*5);
            curatorsBid = previousBid / 2 + random * 100;
        }
        // If there was a bid before
        else{
            curatorsBid = previousBid * (1.0 + increasingPriceFactor);
        }
    }
}
