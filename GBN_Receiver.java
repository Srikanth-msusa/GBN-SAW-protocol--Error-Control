import java.io.*;
import java.net.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.nio.ByteBuffer;

public class GBN_Receiver{
	InetAddress IPAddress1;
	int network_port;
	int receiverPort;
	String destination_file;
	DatagramSocket receiver_socket;
	int expected_Seqnum;
	int char_received = -1;
	Writer arrival_Writer;
	Writer file_writer;
	Queue<packet> received_packets = new LinkedList<packet>();
	
	
	GBN_Receiver(String ipaddress, int network_port, int receiverPort, String destination_file){
		try{
		this.IPAddress1 = InetAddress.getByName(ipaddress);
		this.receiver_socket = new DatagramSocket(receiverPort);
		//System.out.println(receiver_socket.isConnected());
		} catch (Exception e){
			System.err.println(e);
		}
		this.network_port = network_port;
		this.receiverPort = receiverPort;
		this.destination_file = destination_file;
				
	}

	public void receive(String destination_file) throws Exception {
		
		byte[] ack_data = new byte[packet.create_Ack(0).retrieve_udp_data().length];
		DatagramPacket receiver_packet = new DatagramPacket(ack_data, packet.create_Ack(0).retrieve_udp_data().length);
		while(true){
			//System.out.println(receiver_socket.isConnected());
			receiver_socket.receive(receiver_packet);
			packet received_Packet = packet.parse_udp_data(receiver_packet.getData());
			System.out.println("Received packet with seq_num: " + received_Packet.getSeqNum());
			System.out.println("Sending acknowledgement: " + received_Packet.getSeqNum());
			arrival_Writer.write(received_Packet.getSeqNum() + "\n");
			
			if(received_Packet.getSeqNum() == expected_Seqnum){
				char_received = received_Packet.getSeqNum();
				packet acknowledgement = packet.create_Ack(received_Packet.getSeqNum());
				byte[] sendData = acknowledgement.retrieve_udp_data();
				DatagramPacket send_Packet = new DatagramPacket(sendData, sendData.length, IPAddress1, network_port);
				receiver_socket.send(send_Packet);
				received_packets.add(received_Packet);
//				System.out.println("Sending acknowledgement: " + received_Packet.getSeqNum());
				expected_Seqnum = (expected_Seqnum + 1)% 16;
			}
			else if((received_Packet.getType() == 2)){
			                    byte[] sendData = received_Packet.retrieve_udp_data();
                                DatagramPacket send_Packet = new DatagramPacket(sendData, sendData.length, IPAddress1, network_port);
				receiver_socket.send(send_Packet);
				return;
			}
			else{
								packet acknowledgement = packet.create_Ack(char_received);
                                byte[] sendData = acknowledgement.retrieve_udp_data();
                                DatagramPacket send_Packet = new DatagramPacket(sendData, sendData.length, IPAddress1, network_port);
                                receiver_socket.send(send_Packet);
                                
			}
			
			//System.out.print("end of while loop");
	}

	}
	
	void generate_file(String destination_file) throws Exception{
		for (packet receivedPacket : received_packets){
			String s = new String(receivedPacket.getData());
			file_writer.write(s);
		}
	}
	public static void main(String[] args){
		if(args.length != 4){
			System.err.println("User has not passed required number of run time arguments.Pls try again");
			System.exit(1);
		}
		GBN_Receiver r = new GBN_Receiver(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
		try{
			r.arrival_Writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("arrival.log"), "utf-8"));
			r.file_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[3]), "utf-8"));
			r.receive(args[3]);
			r.generate_file(args[3]);
			
		}catch (Exception e){
			System.err.print(e.getMessage());
	
		}finally {
   		try {r.arrival_Writer.close(); r.file_writer.close();} catch (Exception ex) {}
		}
	}

}

class packet {
	
	
	private final int maxDataLength = 500;
	private final int Seq_Num_Mod = 16;
	
	
	private int type;
	private int seqnum;
	private String data;
	
	
	private packet(int Type, int SeqNum, String strData) throws Exception {
		
		if (strData.length() > maxDataLength)
			throw new Exception("data too large (max 500 chars)");
			
		type = Type;
		seqnum = SeqNum % Seq_Num_Mod;
		data = strData;
	}
	
	
	public static packet create_Ack(int SeqNum) throws Exception {
		return new packet(0, SeqNum, new String());
	}
	
	public static packet create_Packet(int SeqNum, String data) throws Exception {
		return new packet(1, SeqNum, data);
	}
	
	public static packet create_EOT(int SeqNum) throws Exception {
		return new packet(2, SeqNum, new String());
	}
	
		
	public int getType() {
		return type;
	}
	
	public int getSeqNum() {
		return seqnum;
	}
	
	public int getLength() {
		return data.length();
	}
	
	public byte[] getData() {
		return data.getBytes();
	}
	
		
	public byte[] retrieve_udp_data() {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.putInt(type);
        buffer.putInt(seqnum);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
		return buffer.array();
	}
	
	public static packet parse_udp_data(byte[] UDPdata) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
		int type = buffer.getInt();
		int seqnum = buffer.getInt();
		int length = buffer.getInt();
		byte data[] = new byte[length];
		buffer.get(data, 0, length);
		return new packet(type, seqnum, new String(data));
	}
}
