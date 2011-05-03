/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import bhft.FileIsNotOnTreeException;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import bhft.Macros;
import jade.domain.mobility.MobilityOntology;
import java.util.logging.Level;
import java.util.logging.Logger;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.domain.mobility.CloneAction;
import jade.domain.mobility.MobileAgentDescription;
import jade.domain.mobility.MoveAction;


/**
 *
 * @author hstefan
 */
public class Receiver extends Agent {

    public final String folder = "receive/" + getLocalName() + "/";
    public TreeMap<String, FileOutputStream> file_tree;
    public boolean ontranstion = false;

    @Override
    public void setup() {

        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());

	System.out.println("Starting Receiver Agent.");
	System.out.println("Agent name: " + this.getAID().getName());
	addBehaviour(new FilePollingBehaviour(this));
        file_tree = new TreeMap<String, FileOutputStream>();

	DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Macros.RECEIVER_TYPE);
        sd.setName("dark-hole-file-transfer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            System.out.println(getName() + ": Impossible to register this agent in the yellow pages");
        }
    }

    @Override
    public void takeDown() {
	try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            System.out.println(getName() + ": Couldn't remove this agent from the yellow pages.");
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
            if(msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                send(msg);
                System.out.println("Chegou ao lugar errado, reenviando.");
                return;
            }
            if(msg != null) {
                String requisition = msg.getUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
               
                if (requisition != null && requisition.equals(Macros.ACCEPT_FILE)) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(Macros.SENDER_AGENT);
                    template.addServices(sd);
                    try {
                        System.out.println("Escrevendo pacotes.");
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for(DFAgentDescription ds : result) {
                            MobileAgentDescription mad = new MobileAgentDescription();
                            mad.setName(ds.getName());
                            mad.setDestination(here());

                            String new_name = "Clone-"+ds.getName().getLocalName();
                            CloneAction ca = new CloneAction();
                            ca.setNewName(new_name);
                            ca.setMobileAgentDescription(mad);

                            Action action = new Action(ds.getName(), ca);
          
                            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                            //req.addUserDefinedParameter("teste-dbg", "seria requisicao");
                            req.setLanguage(new SLCodec().getName());
                            req.setOntology(MobilityOntology.getInstance().getName());
                            
                            try {
                                System.out.println("Mandando o agente se clonar...");
                                myAgent.getContentManager().fillContent(req, action);
                                req.addReceiver(ds.getName());
                                myAgent.send(req);
                            } catch (Exception ex) {
                                ex.printStackTrace(); 
                            }
                        }
                    } catch (FIPAException ex) {
                        Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

    }
}