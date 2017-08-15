package tcpreceiver;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ClosedChannelException;

import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Set;
import java.util.Iterator;
import javax.swing.JOptionPane;

import parameters.Parameters;

/**
 * This Class drives the thread that reconstrcuts a file and saves it
 * again.
 */
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

	/**
	 * Constructor for File Reconstructor
	 */
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

	public void run()
	{
		SocketChannel sChannel = null;
		SelectionKey key = null;
		Set<SelectionKey> selectedKeys = null;
		Iterator<SelectionKey> it = null;
		ByteBuffer buffer = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		int n;

		while (true) {
			try {
				n = selector.select();
			} catch (IOException e) {
				System.err.printf("Could not select on selector.\n");
				e.printStackTrace();
			}
			
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
					try {
						sChannel = serverSocketChannel.accept();
						sChannel.configureBlocking(false);
						sChannel.register(selector, SelectionKey.OP_READ);
					} catch (IOException e) {
						System.err.printf("Could not register new sChannel\n");
						e.printStackTrace();
					}
					this.receiver.appendTCP("New TCP connection from " + sChannel.toString() + "\n");

					this.startTime = System.currentTimeMillis();

					FileOutputStream fout = null;
					try {
						fout = new FileOutputStream(this.filePath);
					} catch (FileNotFoundException e) {
						System.err.printf("Could not create FileOutputStream\n");
						e.printStackTrace();
					}
					this.fcout = fout.getChannel();
					it.remove();
				} else if ((key.readyOps() & SelectionKey.OP_READ)
						== SelectionKey.OP_READ) {

					System.out.println("mewp %ld" + fPosition);

					sChannel = (SocketChannel)key.channel();
					long readCount = 0; 
					try {
						readCount = fcout.transferFrom(sChannel, fPosition, Integer.MAX_VALUE); 
					} catch (IOException e) {
						System.err.printf("Could not transfer file from sChannel\n");
						e.printStackTrace();
					}
					if (readCount == 0) {
						this.endTime = System.currentTimeMillis();
						double time = ((this.endTime - this.startTime) / 100 + 0.1) / 10;
						receiver.appendTCP("Time taken in seconds: " + time + "\n");
						System.out.println("Time taken in seconds: " + time);
						try {
							sChannel.close();
						} catch (IOException e) {
							System.err.printf("IOException closing schannel\n");
							e.printStackTrace();
						}
					} else {
						fPosition += readCount;
					}

					currentRead++;
					it.remove();
				}
			}
		}
	}

}
