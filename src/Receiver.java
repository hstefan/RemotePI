/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import bhft.FileAlreadyExistsException;
import bhft.FileIsNotOnTreeException;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import bhft.Macros;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author hstefan
 */
public class Receiver extends Agent {

    public final String folder = "receive/" + getLocalName() + "/";
    public TreeMap<String, FileOutputStream> file_tree;

    public void setup() {
	System.out.println("Starting Receiver Agent.");
	System.out.println("Agent name: " + this.getAID().getName());
	addBehaviour(new FilePollingBehaviour(this));
        file_tree = new TreeMap<String, FileOutputStream>();

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

    private void addFile(String filename) throws FileAlreadyExistsException {
        if(file_tree.get(filename) != null) {
            throw new FileAlreadyExistsException(filename);
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(folder + filename);
                file_tree.put(filename, fos);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void removeFile(String filename) throws FileIsNotOnTreeException {
        if(file_tree.get(filename) == null)
            throw new FileIsNotOnTreeException(filename);
        else {
            file_tree.remove(filename);
        }
    }
    
    public void writeToFile(String filename, byte[] buffer, int nbytes) throws FileIsNotOnTreeException {
        FileOutputStream fos = file_tree.get(filename);
        if(fos != null) {
            try {
                fos.write(buffer, 0, nbytes);
            } catch (IOException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw new FileIsNotOnTreeException(filename);
        }
    }

    /*Agente que fica fazendo polling para verificar se há alguma mensagem de notificacao*/
    class FilePollingBehaviour extends TickerBehaviour {
        public FilePollingBehaviour(Agent agent)
        {
            super(agent, 1000);
        }

        @Override
        public void onTick() {
            ACLMessage msg = receive();
            if(msg != null) {
                String requisition = msg.getUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
                if(requisition != null && requisition.equals(Macros.START_REQUEST)) {
                    String filename = msg.getUserDefinedParameter(Macros.FILENAME_PARAM);
                    if(filename != null) {
                        Receiver agnt = (Receiver)myAgent;
                        if (agnt != null) { 
                            try {
                                agnt.addFile(filename);
                            } catch (FileAlreadyExistsException ex) {
                                System.out.println(ex);
                            }
                        }
                    }
                }
                else if (requisition != null && requisition.equals(Macros.STOP_REQUEST) ) {
                    String filename = msg.getUserDefinedParameter(Macros.FILENAME_PARAM);
                    if(filename != null) {
                        Receiver agnt = (Receiver)myAgent;
                        if (agnt != null) {
                            try {
                                agnt.removeFile(filename);
                            } catch (FileIsNotOnTreeException ex) {
                                System.out.println(ex);
                            }
                        }
                    }
                }
            }
        }

    }
}
