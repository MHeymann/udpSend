package filesender;

import java.util.Scanner;
import java.util.Set;
import java.util.Arrays;
import java.io.Console;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import layouts.RelativeLayout;

public class Sender extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	private final String giveLoc = "Please give file location";
	/* For giving instructions on what to enter in the txt field below */
	private JLabel label = null;
	/* text field for entering file location */
	private JTextField tfLocation = null;
	/* FocusListeners for "name" and "data" fields */
	private FocusListener flnLogin;
	private FocusListener fldLogin, fldConnect;
	/* For entering the ip and port number */
	private JTextField tfServerIP = null, tfPortNo = null;
	/* Buttons for actions to be performed */
	private JButton btnConnectSend = null, btnDisconnect = null; 
	private JButton btnBrowse = null; 
	/* For displaying messages */
	private JTextArea taTCP = null, taUDP = null;

	/* The datastructure that deconstructs the file into individual bytes
	 * with its own thread */
	private SenderDeconstructor deconstructor = null;

	/* The port to connect to */
	private int portNo = -1;
	/* The host ip address */
	private String hostAddress = null;

	/* Create a file chooser */
	private JFileChooser fc = null;

	private String fileLocationString = "";
	private int sendFileSize = -1;
	
	/**
	 * Sender constructure, sets up all the gui elements with host and port
	 * as defualt fields.
	 */
	public Sender(String host, int port) {
		super("Send File");
		this.portNo = port;
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
		/* put all of this in the north pannel */
		northPanel.add(serverPortPanel);

		/* The label and text field for communication */
		label = new JLabel("Enter the location of the file you want to send:", SwingConstants.CENTER);
		northPanel.add(label);

		JPanel textFieldButtonPanel = new JPanel(new RelativeLayout(RelativeLayout.X_AXIS, 3));

		tfLocation = new JTextField(giveLoc);
		flnLogin = new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (tfLocation.getText().equals(giveLoc)) {
					tfLocation.setText("");
				}
			}
			public void focusLost(FocusEvent e) {
				if (tfLocation.getText().equals("")) {
					tfLocation.setText(giveLoc);
				}
			}
		};
		tfLocation.addFocusListener(flnLogin);
		tfLocation.setBackground(Color.WHITE);
		tfLocation.setEditable(true);

		btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(this);
		btnBrowse.setEnabled(true);

		textFieldButtonPanel.add(tfLocation, new Float(5));
		textFieldButtonPanel.add(btnBrowse, new Float(1));

		/* put all of this in the north panel */
		northPanel.add(textFieldButtonPanel);
		this.add(northPanel, BorderLayout.NORTH);

		taTCP = new JTextArea("TCP message area:\n", 80, 50);
		taUDP = new JTextArea("UDP message area:\n", 80, 50);
		JPanel centerPanel = new JPanel(new GridLayout(1,1));
		centerPanel.add(new JScrollPane(taTCP));
		centerPanel.add(new JScrollPane(taUDP));
		taTCP.setEditable(false);
		taUDP.setEditable(false);
		this.add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();

		/* the 3 buttons */
		btnConnectSend = new JButton("Connect and Send");
		btnConnectSend.addActionListener(this);

		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(this);
		btnDisconnect.setEnabled(false);

		southPanel.add(btnConnectSend);
		southPanel.add(btnDisconnect);

		this.add(southPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setSize(1200, 600);
		this.setVisible(true);

		tfLocation.addActionListener(this);

		btnConnectSend.requestFocus();
	}

	/**
	 * Append info in the dedicated tcp message box.
	 *
	 * @param s The string to be appended.
	 */
	public void appendTCP(String s) 
	{
		taTCP.append(s);
		taTCP.setCaretPosition(taTCP.getText().length() - 1);
	}

	/**
	 * Append info in the dedicated UDP message box.
	 *
	 * @param s The string to be appended.
	 */
	public void appendUDP(String s) 
	{
		taUDP.append(s);
		taUDP.setCaretPosition(taUDP.getText().length() - 1);
	}

	/**
	 * Reset the gui fields after a break in connection occurs.
	 */
	public void breakConnection() {
		btnConnectSend.setEnabled(true);
		btnDisconnect.setEnabled(false);

		tfLocation.setEditable(true);
		btnBrowse.setEnabled(true);
		label.setText("Enter your Username and password below");
		tfPortNo.setText("" + this.portNo);
		tfServerIP.setText(this.hostAddress);
		tfServerIP.setEditable(true);
		tfPortNo.setEditable(true);

		taTCP.setText("TCP message area:\n");
		taUDP.setText("UDP message area:\n");
	}

	/**
	 * React to action on the gui elements.
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
		} else if (o == btnBrowse) {
			returnval = fc.showOpenDialog(this);
			file = fc.getSelectedFile();
			tfLocation.setText(file.getAbsolutePath());
			fileLocationString = file.getAbsolutePath();
			sendFileSize = (int) file.length();
		} else if (o == btnConnectSend) {
			if (fileLocationString.length() == 0) {
				return;
			}
			
			String server_ip = tfServerIP.getText().trim();
			if (server_ip.length() == 0) {
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
			
			this.deconstructor = new SenderDeconstructor(fileLocationString, sendFileSize, server_ip, port, this);
			/* open connection if possible */
			if (!this.deconstructor.connect()) {
				return;
			}
			
			(new Thread(this.deconstructor)).start();
			
			btnConnectSend.setEnabled(false);
			btnDisconnect.setEnabled(true);
			tfLocation.setEditable(false);
			btnBrowse.setEnabled(false);

			tfServerIP.setEditable(false);
			tfPortNo.setEditable(false);

			String rname = tfLocation.getText();
			this.appendTCP(rname + "\n");
		}
	}

    public static void main(String[] args)  {
		Sender sender = null;
		sender = new Sender("localhost", 8000);
		System.out.printf("made a sender\n");
	}
}
