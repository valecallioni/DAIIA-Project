package Queen;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;

import java.io.IOException;
import java.util.ArrayList;

import static java.lang.StrictMath.abs;

public class Queen extends Agent {
    private boolean foundSolution = false;
    private int myRow;
    private int totRows;
    private int[] columns;
    private ArrayList<AID> queens;
    private AID prevQueen;
    private AID nextQueen;

    @Override
    protected void setup() {

        setMyRow();
        register();
        setTotRows();
        setNeighbors();

        // If it is the first row
        if (myRow == 1){
            int c = (int) (Math.random()*(totRows-1));
            columns[myRow-1] = c;
            informNext();
        }

        // This behavior was implemented so the program ends after finding a solution
        addBehaviour(new MyBehaviour());
    }

    /**
     * Set the row of the queen looking at the local name
     * In this way the rows' indexes will go from 1 to totRows
     */
    public void setMyRow() {

        String name = getAID().getLocalName();
        name = name.replaceAll("[^0-9]", "");

        try {
            myRow = Integer.parseInt(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register as Queen.Queen on the DF
     */
    public void register() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Queen");
        serviceDescription.setName(getName());
        description.addServices(serviceDescription);
        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Look for all the queens on the DF so that we know how many queens are there
     * and we can set once for all the previous and the next queen
     */
    public void setTotRows() {
        DFAgentDescription description = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Queen");
        description.addServices(serviceDescription);
        try {
            DFAgentDescription[] resultAgentDescriptions = DFService.search(this, description);
            if (resultAgentDescriptions.length > 0) {
                totRows = resultAgentDescriptions.length;
                columns = new int[totRows];
                queens = new ArrayList<AID>();
                for (int i = 0; i < resultAgentDescriptions.length; i++)
                    queens.add(resultAgentDescriptions[i].getName());
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the AIDs of the previous queen and of the next queen
     * These are the only queens we can communicate with
     */
    public void setNeighbors() {
        for (AID queen : queens){
            String name = queen.getLocalName();
            name = name.replaceAll("[^0-9]", "");
            int number = Integer.parseInt(name);
            if (number == myRow + 1)
                nextQueen = queen;
            else if (number == myRow - 1)
                prevQueen = queen;
        }
    }

    /**
     * Check the position, looking at the previous queens
     */
    public boolean checkPosition(int c) {
        for (int r = 0; r<myRow; r++){
            if (c == columns[r] || (abs((c-columns[r])) == abs((myRow-1-r)) )){
                return false;
            }
        }
        return true;
    }

    /**
     * Inform the next queen about the updated positions
     * The next queen can now place itself
     */
    public void informNext() {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(nextQueen);

        try {
            message.setContentObject(columns);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //message.setOntology(Ontology.INFORM_COLUMNS);
        this.send(message);
    }

    /**
     * Inform the previous queen that the current queen couldn't find a place
     * The previous queen have to look for another place
     */
    public void informPrev() {
        ACLMessage message = new ACLMessage(ACLMessage.FAILURE);
        message.addReceiver(prevQueen);

        try {
            message.setContentObject(columns);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //message.setOntology(Ontology.INFORM_COLUMNS);
        this.send(message);
    }

    /**
     * Print the solution found
     */
    public void printSolution() {
        for (int r=0; r<totRows; r++){
            System.out.print("\n");
            // Let's look for the column occupied by queen r+1
            for (int c=0; c<totRows; c++){
                if (columns[r] == c){
                    System.out.print(" Q" + (r+1) + " ");
                }
                else{
                    System.out.print(" ** ");
                }
            }
            System.out.print("\n");
        }
        System.out.print("-----------------------------");
    }

    /**
     * Set the column, i.e., the position of the current queen along the row
     * The columns' indexes go from 0 to totRows-1 (as in Java language)
     */
    public void setMyColumn() {
        boolean placed = false;
        for (int c=0; c<totRows; c++){
            if (checkPosition(c)){
                columns[myRow-1] = c;
                placed = true;
                if (myRow == totRows){
                    // this queen is the last one, let's print out the solution
                    printSolution();
                    foundSolution =true;
                }
                else
                    informNext();
            }
        }
        if (!placed){
            if (myRow == 1){
                columns = new int[totRows];
                int c = (int) (Math.random()*(totRows-1));
                columns[myRow-1] = c;
                informNext();
            }
            else
                informPrev();
        }

    }


    /**
     * This behavior was implemented this way so when we find a solution,
     * the program runs done, and stops the behaviour.
     */
    public class MyBehaviour extends SimpleBehaviour {

        @Override
        public void action() {
            ACLMessage message = receive();

            if (message != null) {
                try {
                    columns = (int[]) message.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                switch (message.getPerformative()) {
                    case ACLMessage.FAILURE:
                        setMyColumn();
                        break;
                    case ACLMessage.INFORM:
                        setMyColumn();
                        break;

                    default:
                        break;
                }
            } else {
                block();
            }

        }
        @Override
        public boolean done() {
            if (foundSolution) return true; else return false;
        }
    }

}


