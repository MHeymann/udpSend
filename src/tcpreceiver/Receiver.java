package tcpreceiver;

import java.util.Set;
import java.util.Arrays;
import java.io.Console;
import java.io.File;

//import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

//import java.awt.*;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import layouts.RelativeLayout;

/**
 * This is a GUI for the Receiver Client.
 */
public class Receiver extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final String giveLoc = "<File Location>";

	/* For giving instructions on what to enter in the txt field below */
	private JLabel label = null;
	private JLabel lblLocation = null;

	/* For displaying the ip and port number */
	private JTextField tfServerIP = null, tfPortNo = null;
	/* Buttons for actions to be performed */
	private JButton btnDisconnect = null; 
	/* For displaying messages */
	private JTextArea taTCP = null, taUDP = null;
	/* current connection status */
	private boolean connected = false;
	/* The Receiver Listener */
	private ReceiverReconstructor recon = null;
	/* The Receiver Speaker for sending messages to the server */
	private String myName = null;

	/* The port to connect to */
	private int portNo = -1;
	/* The host ip address */
	private String hostAddress = null;

	/* Create a file chooser */
	private JFileChooser fc = null;
	
	public Receiver(String host, int port) {
		super("Receive File ##########");
		this.portNo = port;
		/* TODO:figure out here how to get my IP */
		this.hostAddress = host;
		this.fc = new JFileChooser();

		/* NorthPanel */
		JPanel northPanel = new JPanel(new GridLayout(3,1));
		/* Spacte to enter the server's name and port number */
		JPanel serverPortPanel = new JPanel(new GridLayout(1,5, 1,3));
		/* start up the text fields for server name and port number */
		tfServerIP = new JTextField(this.hostAddress);
		tfPortNo = new JTextField("" + this.portNo);
		tfPortNo.setHorizontalAlignment(SwingConstants.RIGHT);

		serverPortPanel.add(new JLabel("Server Address:  "));
		serverPortPanel.add(tfServerIP);
		serverPortPanel.add(new JLabel("Port Number:  "));
		serverPortPanel.add(tfPortNo);
		serverPortPanel.add(new JLabel(""));
		tfServerIP.setEditable(false);
		tfPortNo.setEditable(false);
		/* put all of this in the north pannel */
		northPanel.add(serverPortPanel);

		/* The label and text field for communication */
		label = new JLabel("File Location:", SwingConstants.CENTER);
		northPanel.add(label);

		lblLocation = new JLabel(giveLoc, SwingConstants.CENTER);

		northPanel.add(lblLocation);
		this.add(northPanel, BorderLayout.NORTH);

		/* The CenterPanel where info about datatransfer is displayed */
		taTCP = new JTextArea("TCP message area:\n", 80, 50);
		taUDP = new JTextArea("UDP message area:\n", 80, 50);
		JPanel centerPanel = new JPanel(new GridLayout(1,1));
		centerPanel.add(new JScrollPane(taTCP));
		centerPanel.add(new JScrollPane(taUDP));
		taTCP.setEditable(false);
		taUDP.setEditable(false);
		this.add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();

		/* the button */
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(this);

		southPanel.add(btnDisconnect);

		this.add(southPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setSize(1200, 600);
		this.setVisible(true);

		this.recon = new ReceiverReconstructor(this, port);

		(new Thread(recon)).start();
	}

	/**
	 * Return the port on which the receiver listens
	 */
	public int getPort() {
		return this.portNo;
	}

	/**
	 * Add messages to the TCP listen area.
	 *
	 * @param s The message to add.
	 */
	public void appendTCP(String s) 
	{
		taTCP.append(s);
		taTCP.setCaretPosition(taTCP.getText().length() - 1);
	}

	/**
	 * Add messages to the UDP listen area. Superfluous in the tcp
	 * receiver.
	 *
	 * @param s The message to add.
	 */
	public void appendUDP(String s) 
	{
		taUDP.append(s);
		taUDP.setCaretPosition(taUDP.getText().length() - 1);
	}

	/**
	 * Return this GUI to the disconnected state.
	 */
	public void breakConnection() {
		label.setText("Enter your Username and password below");
		tfPortNo.setText("" + this.portNo);
		tfServerIP.setText(this.hostAddress);
		taTCP.setText("TCP message area:\n");
		taUDP.setText("UDP message area:\n");
	}

	/**
	 * Manage user interactions with the GUI.
	 *
	 * @param e The ActionEvent that triggers this method.
	 */
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		int returnval;
		File file = null;

		/* Disconnect being the button */
		if (o == btnDisconnect) {
			this.breakConnection();
			return;
		}
	}

	/**
	 * Start the receiver from the command line.
	 */
    public static void main(String[] args)  {
		Receiver receiver = null;
		receiver = new Receiver("localhost", 8000);
		System.out.printf("made a receiver\n");
	}
}
