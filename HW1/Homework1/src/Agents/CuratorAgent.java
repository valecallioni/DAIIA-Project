package Agents;

import Helpers.Artifact;
import Helpers.Ontology;
import Helpers.User;
import jade.core.Agent;
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

    @Override
    protected void setup(){
        super.setup();

        showCuratorInfo();

        register();

        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        MessageTemplate respondToTourGuideAgent = MessageTemplate.MatchOntology(Ontology.REQUEST_MAKE_TOUR);
        MessageTemplate respondToProfilerAgent = MessageTemplate.MatchOntology(Ontology.REQUEST_ARTIFACT_INFO);

        parallelBehaviour.addSubBehaviour(new RespondToTourGuideAgent(this, respondToTourGuideAgent));
        parallelBehaviour.addSubBehaviour(new RespondToProfilerAgent(this, respondToProfilerAgent));

        addBehaviour(parallelBehaviour);

    }

    /**
     * This function is used to make a gallery and show the current Curator Agent
     */
    private void showCuratorInfo() {
        System.out.println("CuratorAgent: " + getAID().getLocalName());

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

}
