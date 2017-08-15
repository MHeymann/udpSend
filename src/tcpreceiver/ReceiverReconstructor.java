package tcpreceiver;

import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.io.FileOutputStream;
import java.util.*;
import java.io.IOException;
import javax.swing.JOptionPane;
/*
import java.lang.System;
*/

import packet.*;
import parameters.*;

public class ReceiverReconstructor implements Runnable {
	private Receiver receiver = null;
	private int port = -1;
	private ServerSocketChannel serverSocketChannel = null;
	private Selector selector = null;
	private FileChannel fcout = null;
	private String filePath = "hardcodefile";
	private long startTime = -1;
	private long endTime = -1;
	private int expectedReads = -1;
	private int currentRead = 0;
	private long fPosition = -1;

	public ReceiverReconstructor(Receiver receiver, int port) {
		InetSocketAddress address = null;
		ServerSocket serverSocket = null;
		SelectionKey key = null;
		this.currentRead = 0;
		int i;

		this.fPosition = 0;
		this.receiver = receiver;
		this.port = port;
		try {
			selector = Selector.open();
			/* open a listener for the given port, register with selector */
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);

			serverSocket = serverSocketChannel.socket();
			serverSocket.bind(new InetSocketAddress(this.port));
			key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			this.receiver.appendTCP("Listening on " + this.port + "\n");
	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			go();
		} catch (Exception e) {
			this.receiver.appendTCP("Some problem in Receive Listener\n");
			e.printStackTrace();
		}
	}

	public void go() throws Exception
	{
		SocketChannel sChannel = null;
		SelectionKey key = null;
		Set<SelectionKey> selectedKeys = null;
		Iterator<SelectionKey> it = null;
		ByteBuffer buffer = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		int n;

		while (true) {
			n = selector.select();
			
			selectedKeys = null;
			selectedKeys = selector.selectedKeys();
			it = null;
			it = selectedKeys.iterator();

			while (it.hasNext()) {
				key = it.next();

				if ((key.readyOps() & SelectionKey.OP_ACCEPT) 
					== SelectionKey.OP_ACCEPT) {
					serverSocketChannel = null;
					serverSocketChannel = (ServerSocketChannel)key.channel();

					try {
						this.filePath = (String)JOptionPane.showInputDialog(
							receiver,
							"Please give the file name you prefer",
							"File Name",
							JOptionPane.PLAIN_MESSAGE
                    	);
                    } catch (Exception e) {
                    	e.printStackTrace();
                    }

					if ((this.filePath != null) && (this.filePath.length() > 0)) {
						this.receiver.appendTCP("File to be saved as " + filePath + "\n");
					} else {
						this.filePath = "myFile";
					}

					sChannel = null;
					sChannel = serverSocketChannel.accept();
					sChannel.configureBlocking(false);
					sChannel.register(selector, SelectionKey.OP_READ);
					this.receiver.appendTCP("New TCP connection from " + sChannel.toString() + "\n");

					this.startTime = System.currentTimeMillis();

					FileOutputStream fout = new FileOutputStream(this.filePath);
					this.fcout = fout.getChannel();
					it.remove();
				} else if ((key.readyOps() & SelectionKey.OP_READ)
						== SelectionKey.OP_READ) {

					System.out.println("mewp %ld" + fPosition);

					sChannel = (SocketChannel)key.channel();
					long readCount = fcout.transferFrom(sChannel, fPosition, Integer.MAX_VALUE); 
					if (readCount == 0) {
						this.endTime = System.currentTimeMillis();
						double time = ((this.endTime - this.startTime) / 100 + 0.1) / 10;
						receiver.appendTCP("Time taken in seconds: " + time + "\n");
						System.out.println("Time taken in seconds: " + time);
						sChannel.close();
					} else {
						fPosition += readCount;
					}

					/*
					buffer.clear();
					int r = sChannel.read(buffer);
					if (r == -1) {
						String tcpmessage = "TCP connection broke down!\n";
						this.receiver.appendTCP(tcpmessage);
						System.out.printf("%s", tcpmessage);
						sChannel.close();

						this.endTime = System.currentTimeMillis();
						double time = ((this.endTime - this.startTime) / 100 + 0.1) / 10;
						receiver.appendTCP("Time taken in seconds: " + time + "\n");
						System.out.println("Time taken in seconds: " + time);

					} else {
						buffer.flip();
						try {
							fcout.write(buffer);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					*/

					/*
					double percentage = ((((10000L * (1 + currentRead)) / this.expectedReads) + 0.0) / 100);

					this.receiver.appendTCP("" + percentage + "%\n");
					*/

					currentRead++;
					it.remove();
				}
			}
		}
	}

}
