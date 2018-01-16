package Mobility.Controller;

import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import Mobility.Agents.ArtistManagerAgent;
import Mobility.Agents.CuratorAgent;

public class Controller {

    public static void main(String[] args){

        try {
            // Get a JADE Runtime instance
            Runtime runtime = Runtime.instance();

            // Main container
            AgentContainer agentContainer = runtime.createMainContainer(new ProfileImpl());
            AgentController mainController = agentContainer.createNewAgent("rma", jade.tools.rma.rma.class.getName(), new Object[0]);
            mainController.start();

            // Heritage malta container and agent
            AgentContainer hmContainer = runtime.createAgentContainer(new ProfileImpl());
            AgentController hmAgentController = hmContainer.createNewAgent("hmCurator1", CuratorAgent.class.getName(), new Object[0]);
            hmAgentController.start();

            // Galileo container and agent
            AgentContainer galileoContainer = runtime.createAgentContainer(new ProfileImpl());
            AgentController galileoAgentController = galileoContainer.createNewAgent("galileoCurator1", CuratorAgent.class.getName(), new Object[0]);
            galileoAgentController.start();


            //Add Artist Manager Agent to the main Container
            AgentController artistManagerAgentController = agentContainer.createNewAgent("artistManager", ArtistManagerAgent.class.getName(), new Object[0]);
            artistManagerAgentController.start();

        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

}