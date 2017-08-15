package tcpsender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.FileChannel;

import java.util.HashMap;
import java.util.Arrays;

import java.net.InetSocketAddress;
import parameters.Parameters;
import packet.Packet;

/**
 * A class to govern the thread that breaks down a file being sent over
 * tcp.
 */
public class SenderDeconstructor implements Runnable {
	private String fileLocation = null;
	private int fileSize = -1;
	private String IP_Address = null;
	private Sender sender = null;
	private int port = -1;
	private SocketChannel socketChannel = null;
	private InetSocketAddress address = null;
	private HashMap<Integer, Packet> hMap = null;

	/**
	 * Constructor for the class;
	 */
	public SenderDeconstructor(String fileLocation, int fileSize, String IP_Address, int port, Sender sender) 
	{
		this.fileLocation = fileLocation;
		this.fileSize = fileSize;
		this.IP_Address = IP_Address;
		this.sender = sender;
		this.port = port;
		this.socketChannel = null;
		this.hMap = null;;
	}

	/**
	 * The method that gets called by the thread responsible for this
	 * class.
	 */
	public void run() {
		FileInputStream fin;
		FileChannel fcin;
		int r = 0, r2 = 0;
		int i;
		
		try {
			fin = new FileInputStream(this.fileLocation);	
			fcin = fin.getChannel();
		}
		catch (FileNotFoundException e){
			return;
		}

		try {
			fcin.transferTo(0, this.fileSize, this.socketChannel);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			fin.close();
			this.socketChannel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.printf("Done sending file\n");
	}

	public boolean connect() {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		try {
			this.socketChannel = SocketChannel.open();
			this.address = new InetSocketAddress(this.IP_Address, this.port);
			if (!this.socketChannel.connect(this.address)) {
				while (!this.socketChannel.finishConnect());
			}
			this.sender.appendTCP("Set up TCP connection\n");
		} catch (IOException e) {
			sender.appendTCP("IOException: failed to create SocketChannel\n");
			return false;
		}
		return true;
	}


}
