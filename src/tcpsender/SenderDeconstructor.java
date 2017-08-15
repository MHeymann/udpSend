package tcpsender;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import parameters.*;
import java.util.HashMap;
import java.util.Arrays;
import packet.Packet;

public class SenderDeconstructor implements Runnable {
	private String fileLocation = null;
	private int fileSize = -1;
	private String IP_Address = null;
	private Sender sender = null;
	private int port = -1;
	private SocketChannel socketChannel = null;
	private InetSocketAddress address = null;
	private HashMap<Integer, Packet> hMap = null;

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

	public void run() {
		go();
	}

	public void go() {
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

		/*
		this.incrementalSend(fcin);
		 */
		
		try {
			fin.close();
			this.socketChannel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.printf("Done sending file\n");
	}

	public void incrementalSend(FileChannel fcin) {
		int i, r = -1;
		ByteBuffer buffer = ByteBuffer.allocate(Parameters.BUFFER_SIZE);

		i = 0;
		while(true) {
			buffer.clear();
			System.out.printf("send %d\n", i);

			try {
				r = fcin.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (r == -1) {
				break;
			}

			buffer.flip();
			try {
				this.socketChannel.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}

			i++;
		}

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
