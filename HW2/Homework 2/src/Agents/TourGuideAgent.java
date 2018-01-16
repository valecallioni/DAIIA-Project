package Agents;

import Helpers.Artifact;
import Helpers.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.proto.states.MsgReceiver;

import java.io.IOException;
import java.util.ArrayList;

public class TourGuideAgent extends Agent {

    private AID curator;

    @Override
    protected void setup(){
        super.setup();

        showTourGuideInfo();

        register();

        subscribe();

        receiveRequest();

    }

    /**
     * This function is used to show the current TourGuideAgent
     */
    private void showTourGuideInfo() {
        addBehaviour(new OneShotBehaviour() {
            public void action()
            {
                System.out.println("TourGuideAgent: " + getAID().getLocalName());
            }
        });
    }

    /**
     * This function is used to register the service of the TourGuideAgent on the DF
     */
    private void register(){
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("TourGuide");
        serviceDescription.setName(getName());
        description.addServices(serviceDescription);
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(){
        DFAgentDescription template = new DFAgentDescription();

        SearchConstraints constraints = new SearchConstraints();
        constraints.setMaxResults((long) 1);

        addBehaviour(new SubscriptionInitiator(this,
                DFService.createSubscriptionMessage(this, getDefaultDF(), template, constraints)) {

            @Override
            protected void handleInform(ACLMessage inform) {
                super.handleInform(inform);
                try {
                    DFAgentDescription[] resultAgentDescriptions = DFService.decodeNotification(inform.getContent());
                    if (resultAgentDescriptions.length > 0) {
                        curator = findCurator();
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * This function is used to search for a curator on the DF
     */
    private AID findCurator(){
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Curator");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this,  description);
            if (resultAgentDescriptions.length > 0) {
                return resultAgentDescriptions[0].getName();
            }
        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function is used to handle the tour request received from the ProfilerAgent
     * The TourGuideAgent must then contact the curator to get a tour
     */
    private void receiveRequest(){
        MessageTemplate template = MessageTemplate.MatchOntology(Ontology.REQUEST_TOUR);
        addBehaviour(new MsgReceiver(this, template, Long.MAX_VALUE, null, null) {

            @Override
            protected void handleMessage(ACLMessage msg) {
                super.handleMessage(msg);
                System.out.println("TourGuideAgent received tour request from ProfilerAgent.");

                // TourGuideAgent then requests the curator to build a tour
                ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                message.addReceiver(curator);
                message.setOntology(Ontology.REQUEST_MAKE_TOUR);

                try {
                    message.setContentObject(msg.getContentObject());
                }
                catch (IOException | UnreadableException e) {
                    e.printStackTrace();
                }

                addBehaviour(new MakeTourInitiator(TourGuideAgent.this, message, msg));
            }

            @Override
            public int onEnd() {
                receiveRequest();
                return super.onEnd();
            }
        });
    }

    /**
     * Make the tour asking to the CuratorAgent
     */
    class MakeTourInitiator extends SimpleAchieveREInitiator {
        ACLMessage original;

        public MakeTourInitiator(Agent a, ACLMessage msg, ACLMessage original) {
            super(a, msg);
            this.original = original;
        }

        @Override
        protected void handleInform(ACLMessage msg) {
            super.handleInform(msg);
            System.out.println("TourGuideAgent received the prepared tour from CuratorAgent.");

            ACLMessage reply = original.createReply();
            reply.setPerformative(ACLMessage.INFORM);

            try {
                reply.setContentObject(msg.getContentObject());
                //ArrayList<Long> artifactIDs = (ArrayList<Long>) msg.getContentObject();
                //for (Long artifact : artifactIDs){
                //    System.out.println(artifact);
                //}

            }
            catch (IOException | UnreadableException e) {
                e.printStackTrace();
            }

            send(reply);
        }
    }

    /**
     * When ending the TourGuideAgent
     */
    @Override
    protected void takeDown() {
        System.out.println("TourGuideAgent: " + getAID().getLocalName() + " shutdown.");
    }
}
