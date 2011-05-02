
import bhft.FileIsNotOnTreeException;
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
import jade.core.Location;
import java.util.ArrayDeque;
import java.util.Queue;

public class Sender extends Agent {
    
    private byte[] filecontent = new byte[4096];
    private Queue<Location> receivers = new ArrayDeque<Location>();
    private String file_on_transmission = "";
    private int n_bytes = 0;
    
    @Override
    public void setup()
    {        
	JFrame frame = new JFrame("Black Hole File Transferer - " + getName());
	JPanel panel = new JPanel();
	ImagePanel img_pan = new ImagePanel(new ImageIcon("images/background.png").getImage());

	frame.getContentPane().add(img_pan);
        frame.getContentPane().add(new JScrollPane( panel ),
            java.awt.BorderLayout.CENTER );
        new FileDrop( System.out, img_pan, /*dragBorder,*/ new FileDrop.Listener() {
	    public void filesDropped( File[] files ) {
		DFAgentDescription[] result = getAgents();
		for(File f : files) {
                    try {
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
    public void afterMove() {
        Queue<Agent> agents = null /* = pega agentes*/;
        for (Agent ag : agents) {
            if (ag instanceof Receiver) {
                try {
                    ((Receiver)ag).writeToFile(file_on_transmission, filecontent, n_bytes);
                } catch (FileIsNotOnTreeException ex) {
                    Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if(receivers.isEmpty()) {
            return;
        }
        else {
            Location l = receivers.element();
            receivers.remove();
            doMove(l);
        }
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
		       break;
		   } else {
                       for(DFAgentDescription ag : agts) {
                           
                       }
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
