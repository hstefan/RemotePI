/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TreeMap;
import javax.crypto.Mac;
import remotepi.Macros;

/**
 *
 * @author hstefan
 */
public class Receiver extends Agent {

    public final String folder = "receive/" + getLocalName() + "/";

    public void setup() {
	System.out.println("Starting Receiver Agent.");
	System.out.println("Agent name: " + this.getAID().getName());
        ReceivingFile rec_agent = new ReceivingFile(this, folder);
	addBehaviour(new FilePollingAgent(this, rec_agent));

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
            System.out.println(getName() + ": Impossible to register this agent in the yellow pages");
        }
    }

    public void takeDown() {
	try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            System.out.println(getName() + ": Couldn't remove this agent from the yellow pages.");
        }
    }

    /*Agente que fica fazendo polling para verificar se há alguma mensagem de notificacao*/
    class FilePollingAgent extends TickerBehaviour {
        private ReceivingFile receiver;
        boolean receiver_added;
        
        public FilePollingAgent(Agent agent, ReceivingFile rec_agent)
        {
            super(agent, 1000);
            receiver = rec_agent;
            receiver_added = false;
        }

        @Override
        public void onTick() {
            ACLMessage msg = receive();
            if(msg != null) {
                String requisition = msg.getUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
                if(requisition != null && requisition.equals(Macros.START_REQUEST)) {
                    if(!receiver_added) {
                        myAgent.addBehaviour(receiver);
                        receiver_added = true;
                    }
                    String filename = msg.getUserDefinedParameter(Macros.FILENAME_PARAM);
                    if(filename != null) {
                        receiver.onFileArrival(filename);
                    }
                }
                else if (requisition != null && requisition.equals(Macros.STOP_REQUEST) ) {
                    myAgent.removeBehaviour(receiver);
                    receiver_added = false;
                }
            }
        }

    }

    class ReceivingFile extends Behaviour {
	
        public ReceivingFile(Agent agent, String folder) {
            files = new TreeMap<String, FileOutputStream>();
            folder_name = folder;
            new File(folder_name).mkdir();
	}

	@Override
	public void action() {
            if(!files.isEmpty()) {
                Set<String>  key_set = files.keySet();
                for(String key : key_set) {
                    
                }  
            }
	}

	@Override
	public boolean done() {
	    return false;
	}

	private TreeMap<String, FileOutputStream> files;
        private String folder_name;
        private void onFileArrival(String filename) {
            FileOutputStream fos;
            try {
		fos = new FileOutputStream(folder_name + "/" + filename);
		files.put(filename, fos);
            } catch (IOException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
