package Mobility.Agents;

import Mobility.Helpers.Ontology;
import Mobility.Helpers.User;
import Mobility.Helpers.Artifact;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;

import java.io.IOException;
import java.util.ArrayList;

public class ProfilerAgent extends Agent {

    private static final int PERIOD = 30000;
    private AID curator;
    private AID tourGuide;
    private User user;
    private ArrayList<Long> artifactIDs;
    private ArrayList<Artifact> tourArtifacts;

    @Override
    protected void setup(){
        super.setup();
        System.out.println("ProfilerAgent: " + getAID().getLocalName());

        user = new User();
        showUserInfo();

        addBehaviour(new TickerBehaviour(this, PERIOD) {
            private static final long serialVersionUID = 1L;

            protected void onTick() {
                System.out.println("ProfilerAgent looks for a TourGuideAgent and a CuratorAgent.");
                curator = findCurator();
                tourGuide = findTourGuide();
                System.out.println("ProfilerAgent asks the TourGuideAgent to make a tour.");
                getTour();

            }
        });

    }

    /**
     * Just to print the Agent's Information
     */
    public void showUserInfo()
    {
        addBehaviour(new OneShotBehaviour() {
            public void action()
            {
                System.out.println("These are the basic user information:");
                System.out.println("Age: " + user.getAge());
                System.out.println("Occupation: " + user.getOccupation());
                System.out.println("Gender: " + user.getGender());
                System.out.println("Interested in century: " + user.getInterestCentury());
                System.out.println("Interested in creator: " + user.getInterestCreator());
                System.out.println("Interested in genre: " + user.getInterestGenre());
            }
        });
    }

    /**
     * This function is used to search for a CuratorAgent on the DF
     */
    private AID findCurator() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Curator");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this, description);
            if (resultAgentDescriptions.length > 0) {
                return resultAgentDescriptions[0].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function is used to search for a TourGuideAgent on the DF
     */
    private AID findTourGuide() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("TourGuide");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this, description);
            if (resultAgentDescriptions.length > 0) {
                return resultAgentDescriptions[0].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function gets the tour from the TourGuideAgent and asks to the
     * Curator Agent for more details about the artifacts that constitute the tour
     */
    private void getTour() {

        // Create request message for the tour
        ACLMessage reqMsg = new ACLMessage(ACLMessage.REQUEST);
        reqMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        reqMsg.addReceiver(tourGuide);

        try {
            reqMsg.setContentObject(user);
        } catch (IOException e) {
            e.printStackTrace();
        }

        reqMsg.setOntology(Ontology.REQUEST_TOUR);
        RequestArtifactsIDs requestArtifactsIDs = new RequestArtifactsIDs(this, reqMsg);

        // Get the tour first: store the IDs of the artifacts
        // Then ask for information about artifacts
        SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
        sequentialBehaviour.addSubBehaviour(requestArtifactsIDs);

        ACLMessage reqMsg2 = new ACLMessage(ACLMessage.REQUEST);
        reqMsg2.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        reqMsg2.addReceiver(curator);
        reqMsg2.setOntology(Ontology.REQUEST_ARTIFACT_INFO);
        RequestArtifactsDetails requestArtifactsDetails = new RequestArtifactsDetails(this, reqMsg2);
        sequentialBehaviour.addSubBehaviour(requestArtifactsDetails);
        addBehaviour(sequentialBehaviour);
    }

    /**
     * This behaviour handles the response of the TourGuideAgent
     * storing the IDs of the artifacts that constitute the tour
     */
    class RequestArtifactsIDs extends SimpleAchieveREInitiator {

        public RequestArtifactsIDs(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void handleInform(ACLMessage msg) {
            super.handleInform(msg);
            System.out.println("ProfilerAgent received a tour from TourGuideAgent. The IDs of the artifacts are:");

            try {
                artifactIDs = (ArrayList<Long>) msg.getContentObject();
                for (Long artifact : artifactIDs){
                    System.out.println(artifact);
                }
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This behaviour handles the response of the CuratorAgent
     * storing the complete artifacts that constitute the tour
     */
    class RequestArtifactsDetails extends SimpleAchieveREInitiator {

        public RequestArtifactsDetails(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected ACLMessage prepareRequest(ACLMessage msg) {
            try {
                msg.setContentObject(artifactIDs);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return super.prepareRequest(msg);
        }

        /**
         * To show the final output: ArticactsID
         * @param msg
         */
        @Override
        protected void handleInform(ACLMessage msg) {
            super.handleInform(msg);

            try {
                tourArtifacts = (ArrayList<Artifact>) msg.getContentObject();


                System.out.println("A tour with following artifacts is provided by the TourGuideAgent and CuratorAgent: ");

                for (Artifact artifact : tourArtifacts) {
                    System.out.println(artifact.getId() + ", genre: " + artifact.getGenre() + ", created by: "
                            + artifact.getCreator() + ", in: " + artifact.getCreationYear());
                }


            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * When ending the ProfilerAgent
     */
    @Override
    protected void takeDown() {
        System.out.println("ProfilerAgent: " + getAID().getLocalName() + " shutdown.");
    }


}
