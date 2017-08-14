package filereceiver;

import javax.swing.JOptionPane;

import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

import java.util.PriorityQueue;
import java.util.Set;
import java.util.Iterator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import packet.Packet;
import parameters.Parameters;

public class ReceiverReconstructor implements Runnable {
	private Receiver receiver = null;
	private int port = -1;
	private SocketChannel sChannel = null;
	private PriorityQueue<Packet> pq = null;
	private ServerSocketChannel serverSocketChannel = null;
	private Selector selector = null;
	private int expectLow = -1;
	private int expectHigh = -1;
	private FileChannel fcout = null;
	private String filePath = "hardcodefile";
	private int expectedPackets = -1;
	private long startTime = -1;
	private long endTime = -1;

	public ReceiverReconstructor(Receiver receiver, int port) {
		InetSocketAddress address = null;
		ServerSocket serverSocket = null;
		SelectionKey key = null;
		DatagramChannel dChannel = null;
		int i;

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
	
			/* set up the udp channels */
			receiver.appendUDP("Setting up UDP channel for receiving file\n");
			for (i = 0; i < Parameters.PORTS; i++) {
				dChannel = DatagramChannel.open();
				dChannel.configureBlocking(false);
				DatagramSocket dSocket = dChannel.socket();
				dSocket.bind(new InetSocketAddress(this.port + i));
				System.out.printf("registered port %d\n", (i + this.port));
				dChannel.register(selector, SelectionKey.OP_READ);
			}
			receiver.appendUDP("Set up UDP channels for receiving file\n");
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.pq = new PriorityQueue<Packet>();
	}

	/*
	public void run() {
		try {
			go();
		} catch (Exception e) {
			this.receiver.appendTCP("Some problem in Receive Listener\n");
			e.printStackTrace();
		}
	}
	*/

	/**
	 * Method that drives thread associated with this structure.
	 */
	public void run()
	{
		SelectionKey key = null;
		Set<SelectionKey> selectedKeys = null;
		Iterator<SelectionKey> it = null;
		DatagramChannel dChannel = null;
		ByteBuffer buffer = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		int n = -1;
		boolean pingback = false;
		boolean receiving = false;

		while (true) {
			try {
				n = selector.select(4);
			} catch (IOException e) {
				System.err.printf("error selecting input from selector");
				e.printStackTrace();
			}
			if (n == 0) {
				if (pingback) {
					this.pingBack();
					pingback = false;
				}
				continue;
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

					sChannel = null;
					try {
						sChannel = serverSocketChannel.accept();
						sChannel.configureBlocking(false);
						sChannel.register(selector, SelectionKey.OP_READ);
					} catch (IOException e) {
						System.err.printf("Error accepting new channel.\n");
						e.printStackTrace();
					}
					
					this.receiver.appendTCP("New TCP connection from " + sChannel.toString() + "\n");
					
					receiving = true;

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

					//If a string was returned, say so.
					if ((this.filePath != null) && (this.filePath.length() > 0)) {
					} else {
						this.filePath = "myFile";
					}
					try {
						Thread.sleep(400);
					} catch (Exception e) {
						e.printStackTrace();
					}

					buffer.clear();
					int r = -1;
					try {
						r = this.sChannel.read(buffer);
					} catch (IOException e) {
						System.out.printf("Error reading from socketChannel\n");
						e.printStackTrace();
					}
					System.out.printf("%d bytes read\n", r);
					if (r == -1) {
						String tcpmessage = "TCP connection broke down!\n";
						this.receiver.appendTCP(tcpmessage);
						System.out.printf("%s", tcpmessage);
						try {
							this.sChannel.close();
						} catch (IOException e) {
							System.out.printf("Could not close sChannel\n");
							e.printStackTrace();
						}
						System.exit(1);
					} else {
						buffer.flip();
						this.expectedPackets = buffer.getInt();
						String tcpmessage = "" + expectedPackets + " packets expected\n";
						this.receiver.appendTCP(tcpmessage);
						System.out.printf("%s", tcpmessage);
						buffer.rewind();
						try {
							sChannel.write(buffer);
						} catch (IOException e) {
							System.out.printf("Could not write to sChannel\n");
							e.printStackTrace();
						}
						buffer.clear();
					}

					this.startTime = System.currentTimeMillis();

					FileOutputStream fout = null;
					try {
						fout = new FileOutputStream(this.filePath);
					} catch (FileNotFoundException e) {
						System.out.printf("File not found\n");
						e.printStackTrace();
					}
					this.fcout = fout.getChannel();
					it.remove();
				} else if ((key.readyOps() & SelectionKey.OP_READ)
						== SelectionKey.OP_READ) {

					if (key.channel() instanceof DatagramChannel) {
						dChannel = (DatagramChannel)key.channel();
						buffer.clear();
						try {
							dChannel.receive(buffer);
						} catch (IOException e) {
							System.err.printf("Could not receive to buffer\n");
							e.printStackTrace();
						}
						buffer.flip();
						int seqNo = buffer.getInt();
						int size = buffer.getInt();
						byte[] data = new byte[size];
						buffer.get(data, 0, size);
						Packet packet = new Packet(seqNo, size, data);
	
						this.pq.add(packet);
						if (pingback) {
							if (pq.size() == (expectHigh - expectLow + 1)) {
								this.pingBack();
								pingback = false;
							}
						}


					} else if (key.channel() == sChannel) {
						if (receiving) {
							buffer.clear();
							int r = -1;
							try {
								r = this.sChannel.read(buffer);
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (r == -1) {
								String tcpmessage = "TCP connection broke down!\n";
								this.receiver.appendTCP(tcpmessage);
								System.out.printf("%s", tcpmessage);
								try {
									this.sChannel.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
								System.exit(1);
							} else {
								buffer.flip();
								r = buffer.getInt();
								expectLow = r;
								r = buffer.getInt();
								expectHigh = r;
								if (r == -1) {
									System.exit(0);
								} 
								pingback = true;
							}
						} else {
							//xxxxxx nothing
						}
					} else {
						System.err.printf("well, this is weird\n");
					}

					it.remove();
				}
			}
		}
	}

	/**
	 * Method for responding to pings from sender.
	 */
	public void pingBack() {
		ByteBuffer intBuffer = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		ByteBuffer countBuffer = ByteBuffer.allocate(4);
		ByteBuffer buffers[] = new ByteBuffer[2];
		int i;
		int countDrops = 0;
		intBuffer.clear();
		countBuffer.clear();
		PriorityQueue<Packet> tempq = new PriorityQueue<Packet>();
	
		Packet p;
		i = expectLow;
		for (i = expectLow; i < expectHigh + 1; i++) {
			if ((p = this.pq.poll()) == null) {
				countDrops++;
				intBuffer.putInt(i);
				continue;
			}
			while (i < p.getSeqNum()) {
				countDrops++;
				intBuffer.putInt(i);
				i++;
			}
			tempq.add(p);
		}

		countBuffer.putInt(countDrops);
		if (countDrops == 0) {
			double percentage = ((((10000L * (1 + expectHigh)) / this.expectedPackets) + 0.0) / 100);
			this.receiver.appendTCP("" + percentage + "%\n");
			if (percentage >= 99.99) {
				this.endTime = System.currentTimeMillis();
				double time = ((this.endTime - this.startTime) / 100 + 0.1) / 10;
				receiver.appendTCP("Time taken in seconds: " + time + "\n");
			}
			writeToFile(tempq);
			intBuffer.putInt(-1);
		} else {
			this.pq = tempq;
		}


		countBuffer.flip();
		intBuffer.flip();
		buffers[0] = countBuffer;
		buffers[1] = intBuffer;
		try {
			this.sChannel.write(buffers);
		} catch (IOException e) {
			System.out.printf("IOException\n");
			e.printStackTrace();
		}

		//System.out.printf("pq size is %d\n", this.pq.size());
	}

	/**
	 * Write current contents of priority queue to file.
	 */
	public void writeToFile(PriorityQueue<Packet> q) {
		Packet p = null;
		ByteBuffer buffer = ByteBuffer.allocate(Parameters.DATA_BYTES);
		while ((p = q.poll()) != null) {
			buffer.clear();
			buffer.put(p.getData());
			buffer.flip();
			try {
				fcout.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
