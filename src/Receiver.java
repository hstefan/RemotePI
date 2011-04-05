/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeMap;

/**
 *
 * @author hstefan
 */
public class Receiver extends Agent {

    /**
     * @param args the command line arguments
     */
    public void setup() {
	System.out.println("Starting Receiver Agent.");
	System.out.println("Agent name: " + this.getAID().getName());
	addBehaviour(new WaitingMessage(this));

	DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("receiver");
        sd.setName("dark-hole-file-transfer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            System.out.println("Impossible to register this agent in the yellow pages");
        }
    }

    public void takeDown() {
	try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            System.out.println("Couldn't remove this agent from the yellow pages.");
        }
    }

    class WaitingMessage extends Behaviour {

	public WaitingMessage(Agent agent) {
	    super(agent);
	    don = false;
	}

	@Override
	public void action() {
	    ACLMessage msg = receive();
	    if (msg != null) {
		String param = msg.getUserDefinedParameter("start");
		if (param != null && param.equals("true")) {
		    String filepath = msg.getUserDefinedParameter("filepath");
		    myAgent.addBehaviour(new ReceivingMessage(myAgent, filepath));
		    myAgent.removeBehaviour(this);
		    don = true;
		}
	    }
	}

	@Override
	public boolean done() {
	    return don;
	}

	private boolean don;
    }

    class ReceivingMessage extends Behaviour {
	public ReceivingMessage(Agent agent, String filepath) {
            files = new TreeMap<String, ObjectOutputStream>();
            ObjectOutputStream fos;
            try {
                fos = new ObjectOutputStream(new FileOutputStream(filepath));
                files.put(filepath, fos);
            } catch (IOException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
	}

	@Override
	public void action() {
	   ACLMessage msg = receive();
	   if (msg != null) {
	       String op = msg.getUserDefinedParameter("stop");
	       String filename = msg.getUserDefinedParameter("filepath");
	       if(op != null && op.equals("true")) {
		   System.out.println("Finished writting!");
		   ObjectOutputStream fos = files.get(filename);
		   if(fos != null) {
			try {
			    fos.close();
			} catch (IOException ex) {
			    Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
			}
			files.remove(filename);
			myAgent.removeBehaviour(this);
			myAgent.addBehaviour(new WaitingMessage(myAgent));
			return;
		   }
	       }
	       else if(filename != null) {
		   ObjectOutputStream fos = files.get(filename);
		   if (fos != null) {
		       try {
			   fos.write(msg.getContent().getBytes());
			   System.out.println("Writting to");
		       } catch (IOException ex) {
			   System.err.println("Unable to write file.");
			   Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
		       }
		   }
	       }
	   }
	}

	@Override
	public boolean done() {
	    return files.isEmpty();
	}

	private TreeMap<String, ObjectOutputStream> files;
    }
}
