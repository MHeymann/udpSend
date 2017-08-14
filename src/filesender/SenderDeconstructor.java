package filesender;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;

import java.net.InetSocketAddress;

import parameters.Parameters;
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
	private DatagramChannel datagramChannel = null;
	private InetSocketAddress address = null;
	private Selector selector = null;
	private HashMap<Integer, Packet> hMap = null;

	/**
	 * Construct a new Deconstructor of the file to systematically break
	 * down the file into packets and send it to the receiving user.
	 */
	public SenderDeconstructor(String fileLocation, int fileSize, String IP_Address, int port, Sender sender) 
	{
		this.fileLocation = fileLocation;
		this.fileSize = fileSize;
		this.IP_Address = IP_Address;
		this.sender = sender;
		this.port = port;
		this.socketChannel = null;
		this.datagramChannel = null;
		this.selector = null;
		this.hMap = null;;
	}

	/*
	public void run() {
		go();
	}
	*/

	/**
	 * The method to run when the structure gets used to run a new thread.
	 */
	public void run() {
		ByteBuffer sendBuff = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		ByteBuffer readBuff = ByteBuffer.allocate(Parameters.DATA_BYTES);
		FileInputStream fin;
		FileChannel fcin;
		InetSocketAddress address = null;
		int r1 = 0, r2 = 0;
		int sequenceNo = 0;
		int i;
		int start, finish;
		
		try{
			fin = new FileInputStream(this.fileLocation);	
			fcin = fin.getChannel();
		} catch (FileNotFoundException e){
			return;
		}

		for (sequenceNo = 0; true; ) {
			for (i = 0, start = sequenceNo; i < Parameters.BURST_LENGTH; i++, sequenceNo++) {
				sendBuff.clear();
				readBuff.clear();
	
				try {
					r1 = fcin.read(readBuff);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(r1 == -1) {
					System.out.printf("Done reading\n");
					break;
				}
				
				readBuff.flip();
				Packet packet = new Packet(sequenceNo, r1, Arrays.copyOf(readBuff.array(), readBuff.limit()));
				hMap.put(sequenceNo, packet);
				/*
				*/
	
				int destPort = this.port + (sequenceNo % Parameters.PORTS);
				address = new InetSocketAddress(this.IP_Address, destPort);

				packet.sendPacket(this.datagramChannel, address);
				/*
				if (i % 100 == 0) {
				} else {
					packet.sendPacket(this.datagramChannel, address);
				}
				*/
			}


			finish = sequenceNo - 1;

			while (!ping(start, finish, sendBuff, readBuff));

			hMap.clear();

			if (r1 == -1) {
				System.out.printf("Done reading\n");
				break;
			}
		}

		try {
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Done sending file\n");
	}

	/**
	 * Connect to a the receiving user for relaying control tcp
	 * information.
	 */
	public boolean connect() {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		try {
			this.hMap = new HashMap<Integer, Packet>();
			this.selector = Selector.open();
			this.datagramChannel = DatagramChannel.open();
			this.socketChannel = SocketChannel.open();
			this.socketChannel.configureBlocking(false);
			/*
			this.datagramChannel.configureBlocking(false);
			*/
			this.address = new InetSocketAddress(this.IP_Address, this.port);
			this.socketChannel.connect(this.address);
			while (!this.socketChannel.finishConnect());
			this.sender.appendTCP("Set up TCP connection\n");
			/*
			this.datagramChannel.register(selector, SelectionKey.OP_READ);
			*/
			this.socketChannel.register(selector, SelectionKey.OP_READ);
			buffer.clear();
			buffer.putInt((this.fileSize / (Parameters.DATA_BYTES) + 1));
			buffer.flip();
			this.socketChannel.write(buffer);
			selector.select();
			buffer.clear();
			socketChannel.read(buffer);
		} catch (IOException e) {
			sender.appendTCP("IOException: failed to create SocketChannel\n");
			return false;
			/*
		} catch (ClosedChannelException e) {
			System.out.printf("Closed Channel Exception\n");
			e.printStackTrace();
			*/
		}
		return true;
	}

	/**
	 * Signal to the receiver that the end of file has been reached.
	 */
	public void sigTerminate(ByteBuffer sendBuff) {
		int r2;
		sendBuff.clear();
		sendBuff.putInt(-1);
		sendBuff.putInt(-1);
		sendBuff.flip();
	
		try {
			//this.sender.appendTCP("Ping\n");
			this.socketChannel.write(sendBuff);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Coordinate with the receiver to ensure all packets up to this point
	 * has been received. Resend packets if an error occured.
	 */
	public boolean ping(int start, int finish, ByteBuffer sendBuff, ByteBuffer readBuff) {
		int r2;
		sendBuff.clear();
		sendBuff.putInt(start);
		sendBuff.putInt(finish);
		sendBuff.flip();
	
		try {
			this.socketChannel.write(sendBuff);
			readBuff.clear();
			selector.select();
			r2 = socketChannel.read(readBuff);
			if (r2 == -1) {
				System.out.printf("receiver TCP disconnected\n");
				this.sender.appendTCP("receiver TCP disconnected\n");
				this.socketChannel.close();
			} else {
				readBuff.flip();
				int intCount = readBuff.getInt();
				Packet p = null;
				for (int j = 0; j < intCount; j++) {
					r2 = readBuff.getInt();
					p = hMap.get(r2);

					int destPort = this.port + (r2 % Parameters.PORTS);
					address = new InetSocketAddress(this.IP_Address, destPort);
					p.sendPacket(this.datagramChannel, address);
				}
				
				if (intCount == 0) {
					return true;
				} else {
					/* drop detected */
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

}
