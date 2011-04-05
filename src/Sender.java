
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import remotepi.FileDrop;
import remotepi.ImagePanel;

public class Sender extends Agent {
    @Override
    public void setup()
    {        
	JFrame frame = new JFrame("Black Hole File Transferer");
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
	sd.setType("receiver");
	template.addServices(sd);
	try {
	    DFAgentDescription[] result = DFService.search(this, null);
	    return result;
	} catch (FIPAException ex) {
	    Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
	}
	return null;
    }
    
    public void sendFile(File file, DFAgentDescription[] agts) throws IOException {
	ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

	msg.addUserDefinedParameter("start", "true");
	msg.addUserDefinedParameter("filepath", file.getName());

	for(DFAgentDescription ag : agts) {
	    msg.addReceiver(ag.getName());
	}
	send(msg);
	try {
            ObjectInputStream oos = new ObjectInputStream(new FileInputStream(file));
	    byte[] filecontent = new byte[4096];
	    int bytes = -1;
	    try {
		msg.removeUserDefinedParameter("start");
		while(true) {
		   bytes = oos.read(filecontent);
		   if(bytes <= 0) {
		       msg.addUserDefinedParameter("stop", "true");
		       System.out.println("Transmission concluded.");
                       oos.close();
		       break;
		   } else {
                        String f = new String(filecontent);
			msg.setContent(f);
			send(msg);
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
