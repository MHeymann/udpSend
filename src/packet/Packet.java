package packet;

import java.nio.channels.DatagramChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import parameters.Parameters;
import java.io.IOException;

/**
 * @author Murray Heymann and Jolandi Lombard.
 *
 * The packet datastructure used to ferry information as a datagram.
 */

public class Packet implements Comparable<Packet> {
	private int sequenceNumber = -1;
	private int size = -1;
	private byte[] data = null;

	/**
	 * Construct a new Packet structure with the appropriate sequence
	 * number, size and data variables.
	 */
	public Packet (int sequenceNumber, int size, byte[] data) {
		this.sequenceNumber = sequenceNumber;
		this.size = size;
		this.data = data;
	}

	/**
	 * Get the sequence number that determines where in the sequence of
	 * packets this packet belongs.
	 *
	 * @return the sequence number, an integer.
	 */
	public int getSeqNum() {
		return this.sequenceNumber;
	}

	/**
	 * Get the size of this packet, the number of bytes.
	 * @return The integer number of bytes in the packet data.
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * A method to get the data bytes in this packet.
	 *
	 * @return the byte array that this packet contains.
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * A method for determining where in the sequence of datagrams this
	 * packet must lie, relative to another.
	 *
	 * @param that The packet this is being compared to.
	 */
	@Override
	public int compareTo(Packet that) {
		return this.sequenceNumber - that.sequenceNumber;
	}

	/**
	 * Send this packet to some address.
	 * @param channel	The datagram channel to which the packet must be
	 *					send.
	 * @param address	The ip address to which to send the packet. 
	 */
	public void sendPacket(DatagramChannel channel, InetSocketAddress address) {
		ByteBuffer sendBuff = ByteBuffer.allocate(Parameters.BUFFER_SIZE);
		sendBuff.clear();
		sendBuff.putInt(this.sequenceNumber);	
		sendBuff.putInt(this.size);	
		sendBuff.put(this.data, 0, size);	
		sendBuff.flip();
		try {
			channel.send(sendBuff, address);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
