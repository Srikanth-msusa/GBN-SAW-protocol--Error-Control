import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;
import java.util.*;

public class GBN_Sender {
	DatagramSocket socket_connection;
	int networkDataRecievePort;
	InetAddress IPAddress1;  //Name of the machine hosting the server
	String source_file;
	int ACKSize,expected_Seqnum = 0;
	long timerBegin,wait_Time;
	boolean End_Of_File = false;
	int no_of_packets=0;
	Writer sequence_number_writer;
	Writer ack_writer;
	Writer missedPacketWriter;
	Queue<packet> window_packets = new LinkedList<packet>();
	Queue<packet> actual_packets = new LinkedList<packet>();
	
	Scanner sc=new Scanner(System.in);
				int n=sc.nextInt();
				Random rnd=new Random();
				
	int totalpackets=0;
	int missedpackets=0;
	ArrayList<Integer> rnlist=new ArrayList<Integer>();
				
	//This constructor throws exception if 4 run time arguments are not passed
	GBN_Sender(String ip_address, String networkDataRecievePort, String senderACKRecievePort, String source_file){
		
		try{
		this.socket_connection = new DatagramSocket(Integer.parseInt(senderACKRecievePort));
		//System.out.println(socket_connection.isConnected());
		this.networkDataRecievePort = Integer.parseInt(networkDataRecievePort);
		this.IPAddress1 = InetAddress.getByName(ip_address);
		this.source_file = source_file;
		this.ACKSize = packet.create_ACK(0).retrieve_udp_data().length;
		}catch(Exception e){
			//System.out.println(e.getMessage());
			
		}
	}
	
	void ack_listen() throws Exception{
		
                int packet_size = packet.create_Packet(0, new String( new char[500]) ).retrieve_udp_data().length;
               	byte[] ACKdata = new byte[packet_size];
                DatagramPacket ack_datagram = new DatagramPacket(ACKdata, packet_size);
                socket_connection.setSoTimeout(1000);
		while((System.currentTimeMillis() - timerBegin) < 1000){
			try {//
			wait_Time = System.currentTimeMillis() - timerBegin;
					socket_connection.receive(ack_datagram);//it receives acknowledgement
		            packet ack_packet = packet.parse_udp_data(ack_datagram.getData());
                	System.out.println("Received acknowledgement: " + ack_packet.getSeqNum());
			if(ack_packet.getSeqNum() > -1)
			ack_writer.write(ack_packet.getSeqNum() + "\n");
			if((window_packets.peek() == null) && (actual_packets.peek() == null)){
                                //this condition checks whether all packets in window queue are completed
                                break;
                        }
			if(get_index(window_packets.peek().getSeqNum()) < get_index(ack_packet.getSeqNum())){
			
				slide_window(ack_packet.getSeqNum());
			}
			
			if((window_packets.peek() == null) && (actual_packets.peek() == null)){
				//this condition checks whether all packets in window queue are completed
				break;
			}
			
			if((window_packets.peek().getSeqNum() == ack_packet.getSeqNum())){
			
				expected_Seqnum++;
				window_packets.poll();
				
			}
			else if(ack_packet.getType() == 2){
				System.exit(0);
			}
			
			}
			catch (Exception e) { 
			//System.out.println(e);
			
			}
		}
		
	}
	
	int get_index(int a){
		int i = 0;
		List<packet> list = new ArrayList<packet>(window_packets);
		for (i = list.size()-1; i > 0; i--) {
                
			if(list.get(i).getSeqNum() == a){
				return i;
			}
		        	}
		return i;
	}
	
		void slide_window(int acknowledge){
		
		while((window_packets.peek().getSeqNum() != acknowledge)){
			window_packets.poll();
				}	
		if(window_packets.peek() != null){
					window_packets.poll();
		}
		
	}

	public void send_single_packet(packet p) throws Exception{
		byte[] sender_data = p.retrieve_udp_data();
                DatagramPacket sendPacket = new DatagramPacket(sender_data, sender_data.length, this.IPAddress1, this.networkDataRecievePort);
        		socket_connection.send(sendPacket);
				timerBegin = System.currentTimeMillis();
	}
	
	//this method is used to data_transfer the file_content
	public void data_transfer(String source_file) throws Exception{
		prepare_window();
		
		for(packet read_packet : this.window_packets){
			
			int temp=rnd.nextInt(100);//generates random number every time when it is executed
						
			if(temp>=n)//comparing the random number with user input packet loss value
			{
			totalpackets++;	
			System.out.print("Sending packet with seqnum: " + read_packet.getSeqNum() + "\n");
			sequence_number_writer.write(read_packet.getSeqNum() + System.getProperty( "line.separator" ));
			byte[] sender_data = read_packet.retrieve_udp_data();
			DatagramPacket sendPacket = new DatagramPacket(sender_data, sender_data.length, this.IPAddress1, this.networkDataRecievePort);
			socket_connection.send(sendPacket);//sending packet to receiver
			//timer started for tracking time out
			timerBegin = System.currentTimeMillis();
			}
			else{
				rnlist.add(temp);
				missedpackets++;
				missedPacketWriter.write(read_packet.getSeqNum() + System.getProperty( "line.separator" ));
			}
			
		}
		
	}
	
	public void prepare_window() throws Exception{
		//window size is made 10 and can be changed by user as per requirements
		while(actual_packets.size() > 0 && window_packets.size() < 10){
			window_packets.add(actual_packets.poll());
			
		}
	}
	
	public void split_to_packets(String source_file) throws Exception{
			int seqnum = 0;
		int input_char;
		int no_of_chars = 0;
		String file_content = "";
		BufferedReader br = new BufferedReader(new FileReader("DataSent.txt"));
		
		while((input_char = br.read()) != -1){
			
			if(no_of_chars == 500){
							
				packet addedPacket = packet.create_Packet(seqnum, file_content);
				actual_packets.add(addedPacket);
				System.out.print(addedPacket.getSeqNum() + "\n");
				no_of_chars = 0;
				file_content = "";	
				seqnum++;
				no_of_packets++;
			}
			char c = (char)input_char;
			file_content = file_content + c;
			no_of_chars++;
		}
		//for file_content less than 500 characters
		if(file_content.length() > 0){
			actual_packets.add(packet.create_Packet(seqnum, file_content));
			no_of_packets++;
			
		}
	}
	
		
	void endTransmission() throws Exception{
		packet EOTPacket = packet.create_EOT(0);
		send_single_packet(EOTPacket);
		System.out.println("----------------------------------");
		System.out.println("File Transfer Completed ");
		System.out.println("----------------------------------");
		System.out.println("----------------------------------");
		System.out.println("Statisticks of File Transfer are :");
		System.out.println("Number of packets given file_content is split into are :"+no_of_packets);
		System.out.println("Total number of packets received at receiver end are (including resent packets ): "+totalpackets);
		
		System.out.println();
		
		int discarded_packets=totalpackets-no_of_packets;
		System.out.println("Number of out of order packets received at receiver end and subsequently discarded are : "+discarded_packets);
		
		System.out.println();
		
		System.out.println("Number of unsent packets are( i.e due to packet loss value / random number given by user) : "+ missedpackets);
		
		System.out.println();
		
		int resent_packets=discarded_packets+missedpackets;
		System.out.println("Number of resent packets including unsent by GBN_Sender and discarded by receiver are : "+resent_packets );
		System.out.println("Random numbers generated are : "+rnlist);

		return;
	}
	
	
	public static void main(String [ ] args) {
	
		if( args.length != 4) {
			System.err.println("User has not passed required number of run time arguments.Pls try again");
			System.exit(1);
		}
		
		
		System.out.println("Enter random number OR Packet loss value bettween 0 and 99 :");
		GBN_Sender s = new GBN_Sender(args[0],args[1],args[2],args[3]);//creating GBN_Sender object
		long startTime = System.currentTimeMillis();//starts timer for measuring file transfer time consumption
		
		try{
			//it generates log file with sequence numbers that are transmitted
		s.sequence_number_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("receivedseqnum.log"), "utf-8"));
		
		//it generates log file with acknowledged sequence numbers
		s.ack_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ack.log"), "utf-8"));
		
		//it generates log file with not transmitted sequence numbers
		s.missedPacketWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("missedPackets.log"), "utf-8"));
	
		
		s.split_to_packets(args[3]);
                s.prepare_window();
          
				
		while(!((s.window_packets.peek() == null) && (s.actual_packets.peek() == null))){
			
			s.data_transfer(args[3]);
			s.ack_listen();
			
		}
		s.endTransmission();
		
		long endTime   = System.currentTimeMillis();
		double totalTime = endTime - startTime;//it measures time consumed for total file transfer
		totalTime=totalTime/1000;//converting milli sconds to seconds
		
		if(totalTime<60)
				System.out.println("Total time consumed for transfer in seconds is : "+ totalTime);
		else
		{
			totalTime=totalTime/60;//converting seconds to minutes
			System.out.println("Total time consumed for transfer in minutes is : "+ totalTime);
		}

		} catch(Exception e) {
			System.err.println(e);
		}finally {
  	 	try {s.sequence_number_writer.close();s.ack_writer.close(); s.missedPacketWriter.close();} 
		catch (Exception ex) {}
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
	
	
	public static packet create_ACK(int SeqNum) throws Exception {
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
