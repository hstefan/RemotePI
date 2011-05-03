import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import bhft.FileDrop;
import bhft.ImagePanel;
import bhft.Macros;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Location;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.mobility.CloneAction;
import jade.domain.mobility.MobilityOntology;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Sender extends Agent {
    
    private ArrayList<byte[]> file_buffer = new ArrayList<byte[]>();
    private transient byte[] filecontent = new byte[4096];
    String filename;
    
    @Override
    public void setup()
    {
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());

	JFrame frame = new JFrame("Black Hole File Transferer - " + getName());
	JPanel panel = new JPanel();
	ImagePanel img_pan = new ImagePanel(new ImageIcon("images/background.png").getImage());

	frame.getContentPane().add(img_pan);
        frame.getContentPane().add(new JScrollPane( panel ),
            java.awt.BorderLayout.CENTER );
        FileDrop fileDrop = new FileDrop(System.out, img_pan, new FileDrop.Listener() {
            public void filesDropped(File[] files) {
                DFAgentDescription[] result = getAgents();
                for (File f : files) {
                    try {
                        filename = f.getName();
                        sendFile(f, result);
                    } catch (IOException ex) {
                        Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
	
	frame.pack();
	frame.setBounds(100, 100, (int)img_pan.getDimension().getWidth(),
		(int)img_pan.getDimension().getHeight());
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        
        addBehaviour(new TickerBehaviour(this, 20) {
            @Override
            public void onTick() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("Mensagem nao nula");
                    if(msg.getPerformative() != ACLMessage.REQUEST) {
                        String f = msg.getUserDefinedParameter("teste-dbg");
                        if(f != null)
                            System.out.println(f);
                        send(msg);
                    }
                    else {
                        System.out.println("Mensagem para (Sender) ser clonado recebida!");
                        try {
                                System.out.println("token1");
                            ContentElement content = getContentManager().extractContent(msg);
                                System.out.println("token2");
                            Concept concept = ((Action)content).getAction();
                            if (concept instanceof CloneAction) {
                                CloneAction ca = (CloneAction)concept;
                                String new_name = ca.getNewName();
                                Location l = ca.getMobileAgentDescription().getDestination();
                                if(l != null)
                                    myAgent.doClone(l, new_name);
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Macros.SENDER_AGENT);
        sd.setName("dark-hole-file-transfer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch(FIPAException fe) {
            System.out.println(getName() + ": Impossible to register this agent in the yellow pages");
        }
    } 

    public DFAgentDescription[] getAgents() {
	DFAgentDescription template = new DFAgentDescription();
	ServiceDescription sd = new ServiceDescription();
	sd.setType(Macros.RECEIVER_TYPE);
	template.addServices(sd);
	try {
	    DFAgentDescription[] result = DFService.search(this, template);
	    return result;
	} catch (FIPAException ex) {
	    Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
	}
	return null;
    }

    @Override
    public void beforeClone() {
        System.out.println("Cloning Sender...");
    }

    @Override
    public void afterClone() {
        System.out.println("Sender Cloned!");
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            System.out.println("Escrevendo " + file_buffer.size() + " blocos");
            for(int i = 0; i < file_buffer.size(); ++i) {
                try {
                    fos.write(file_buffer.get(i));
                } catch (IOException ex) {
                    Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
        doDelete();
    }
    
    public void sendFile(File file, DFAgentDescription[] agts) throws IOException {
	ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

	msg.addUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME, Macros.START_REQUEST);
	msg.addUserDefinedParameter(Macros.FILENAME_PARAM, file.getName());
      
	for(DFAgentDescription ag : agts) {
	    msg.addReceiver(ag.getName());
	}
        
	send(msg);
	try {
            if(file_buffer.size() != 0)
                file_buffer.clear();
            FileInputStream fis = new FileInputStream(file);
	    int bytes = -1;
	    try {
		msg.removeUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
		while(true) {
		   bytes = fis.read(filecontent);
		   if(bytes <= 0) {
		       msg.addUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME, Macros.STOP_REQUEST);
		       System.out.println(getName() + ": Transmission concluded.");
		       send(msg);
                       fis.close();
                       msg.removeUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
                       msg.removeUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME);
                       msg.addUserDefinedParameter(Macros.T_REQUEST_PARAM_NAME, Macros.ACCEPT_FILE);
                       send(msg);
                       
		       break;
		   } else {
                       file_buffer.add(filecontent.clone());
		   }
		}
	    } catch (IOException ex) {
		Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
	    }
	} catch (FileNotFoundException ex) {
	    Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
}