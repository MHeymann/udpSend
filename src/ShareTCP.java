
import java.util.Scanner;
import java.util.Set;
import java.util.Arrays;
import tcpsender.Sender;
import tcpreceiver.Receiver;
import java.io.Console;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * Author Murray Heymann and Jolandi Lombard
 *
 * This file is starts the client.  If it is started in gui mode, the swing
 * components are created and managed from here.  Threads for incoming and
 * outgoing data are created from here.  
 *
 */
public class ShareTCP extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	/* For giving instructions on what to enter in the txt field below */
	private JLabel label = null;

	/* For entering the ip and port number */
	private JTextField tfReceiverIP = null, tfPortNo = null;
	/* Buttons for actions to be performed */
	private JButton sendfile = null, receivefile = null; 
	/* current connection status */

	/* The port to connect to */
	private int portNo = -1;

	/* The host ip address */
	private String recAddress = null;
	
	/**
	 * Construct the generic staring gui for sending or receiving
	 *
	 * @param host The host ip address to send a file to.
	 * @param port The port on which the tcp connection communicates.
	 */
	public ShareTCP(String host, int port) {
		super("TCP ##### Share File");
		this.portNo = port;
		this.recAddress = host;

		/* NorthPanel */
		JPanel northPanel = new JPanel(new GridLayout(1,1));
		/* Spacte to enter the server's name and port number */
		JPanel receiverPortPanel = new JPanel(new GridLayout(1,5, 1,3));
		/* start up the text fields for server name and port number */
		tfReceiverIP = new JTextField(this.recAddress);
		tfPortNo = new JTextField("" + this.portNo);
		tfPortNo.setHorizontalAlignment(SwingConstants.RIGHT);

		receiverPortPanel.add(new JLabel("Server Address:  "));
		receiverPortPanel.add(tfReceiverIP);
		receiverPortPanel.add(new JLabel("Port Number:  "));
		receiverPortPanel.add(tfPortNo);
		receiverPortPanel.add(new JLabel(""));
		/* put all of this in the north panel */
		northPanel.add(receiverPortPanel);

		this.add(northPanel, BorderLayout.NORTH);

		/* 
		 * The CenterPanel where chat's are displayed and online users
		 * shown
		 */
		JPanel southPanel = new JPanel();

		/* the 3 buttons */
		sendfile = new JButton("Send");
		sendfile.addActionListener(this);
		receivefile = new JButton("Receive");
		receivefile.addActionListener(this);

		southPanel.add(sendfile);
		southPanel.add(receivefile);

		this.add(southPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		/*
		EXIT_ON_CLOSE
		*/
		this.setSize(600, 100);
		this.setVisible(true);
	}
	
	/**
	 * Manage actions from the swing gui.  
	 *
	 * @param e The Event that triggered this method.
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == sendfile) {
			
			String server = tfReceiverIP.getText().trim();
			if (server.length() == 0) {
				return;
			}

			String portNumber = tfPortNo.getText().trim();
			if (portNumber.length() == 0) {
				return;
			}
			int port = -1;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception except) {
				System.err.printf("Please provide a valid port number\n");
				return;
			}
			
			new Sender(server, port);

		} else if (o == receivefile) {
			String portNumber = tfPortNo.getText().trim();
			if (portNumber.length() == 0) {
				return;
			}
			int port = -1;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception except) {
				System.err.printf("Please provide a valid port number\n");
				return;
			}
			
			new Receiver("localhost", port);
		} else {
			System.err.printf("This is weird\n");
		}
	}

	/**
	 * The start of the method from command line.
	 */
    public static void main(String[] args)  {
		ShareTCP sender = null;
		sender = new ShareTCP("localhost", 8000);
	}
}
