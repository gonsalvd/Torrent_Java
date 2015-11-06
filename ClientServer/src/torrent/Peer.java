package torrent;

import java.net.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/* The Peer class is a class of users that receive chunks and then trade those amongst themselves creating a Peer to Peer
 * network. Each Peer is originally started by the main program TorrentProgram as a thread.
 */

public class Peer implements Runnable
{

	private File chunk_folder;		//folder to save peer's chunks in
	private Socket requestSocket;           //socket connect to the server
	private Socket downloadSocket;		//the socket this peer will use to download on and connect with other peer
	private ServerSocket uploadSocket; 	//the socket this peer will use to upload on and allow other peer to connect to

	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	ObjectOutputStream out2;         //stream write to the socket
	ObjectInputStream in2;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server

	Map<Integer, File> summary_local = new HashMap<Integer, File>();	//the chunk id summary list for this peer
	int chunk_id;	//the chunk_id received from host
	int chunk_id2; //the chunk_id received from peer
	int num_of_chunks;
	int upload_port;
	int next_peer;
	private int peer_number;	//this peers number
	private int num_of_users;	//number of users in network (except host)
	private int prev_peer;		//the previous peers number in the circle (used to connect with)
	boolean notConnected = true;	//used in a loop to allow other peer to connect as download peer on serversocket
	private String programFilename;
	Timer timer;
	Thread peer_thread;
	int timeout_delay;

	public static void main(String[] args)
	{
		Scanner in = new Scanner(System.in);
		System.out.println("Enter Peer Number (0..N-1): ");
		int peer_number = in.nextInt();
		System.out.println("Enter number of peers in network: ");
		int number_of_users = in.nextInt();
//		System.out.println(String.format("Enter program folder to save Peer %s files:",peer_number));
//		String folder = in.next();
//		System.out.println(String.format("Enter number of chunks expecting to receive:"));
//		int number_of_chunks = in.nextInt();

		try {
			Peer peer = new Peer(peer_number, number_of_users);
			Thread p = new Thread(peer);
			p.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Peer(int peer_number, int number_of_users)
	{
		System.out.println(String.format("Peer %d was created...", peer_number));
		this.peer_number = peer_number;
		this.num_of_users = number_of_users;
		//this.programFilename = programfilename;
		this.num_of_chunks = 1;
		//this.peer_number = peer_number;
		makeFolder();
		
		timeout_delay = 3000;
		timer = new Timer();
		timer.scheduleAtFixedRate(new GetSocketTask(),1000,timeout_delay);
	}

	//Method to combine chunks into one file
	private void recreateFile()
	{
		String fileOutputName = "completefile";
		File full_file = new File(chunk_folder.toString()+"/"+fileOutputName);

		FileInputStream inputStream;
		FileOutputStream fileFull;

		String outputFolderName = chunk_folder.toString();
		File directory = new File(outputFolderName);
		File[] directoryListing = directory.listFiles();
		//Must sort as listFiles() does not do so. Must pad with 0s as well.
		Arrays.sort(directoryListing);
		//Loop through files writing to file
		for (File chunk_file : directoryListing)
		{
			try {
				//Size of file in bytes
				int fileSize = (int) chunk_file.length();
				byte[] byteChunkPart;
				byteChunkPart = new byte[fileSize];
				inputStream = new FileInputStream(chunk_file);
				inputStream.read(byteChunkPart, 0, fileSize);
				fileFull = new FileOutputStream(full_file,true);
				fileFull.write(byteChunkPart);
				fileFull.flush();
				inputStream.close();
				fileFull.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		System.out.println(String.format("Combined chunks in Peer %d to form file %s in %s", peer_number,fileOutputName, chunk_folder.toString()));
	}

	//Method to make a folder for peer to store chunks in
	private void makeFolder()
	{
		try
		{
			//chunk_folder = new File(programFilename+String.format("/peer%d/",peer_number));
			chunk_folder = new File("/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp/"+String.format("/peer%d/",peer_number));
			System.out.println(String.format("Directory location for Peer %d chunks at: %s",peer_number, chunk_folder.toString()));
			if (!chunk_folder.exists()) {
				if (chunk_folder.mkdir()) {
					System.out.println(String.format("Directory created for peer %d: %s ", peer_number, chunk_folder.toString()));
				} else {
					System.out.println("Failed to create directory!");
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Invalid file");
		}
	}

	//Boolean to keep Peer looking for chunks until all chunks are received
	private boolean doNotHaveChunks()
	{
		return summary_local.size() < this.num_of_chunks;
	}

	class GetSocketTask extends TimerTask
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (notConnected)
			{
				try{
					//This peer attempts to open a thread on the previous peer port as the DOWNLOADER
					prev_peer = Math.floorMod((peer_number - 1),num_of_users);
					int prev_peer_port = 20000 + prev_peer;
					//Ex: Peer 2 downloads from Peer 1 (thus, Peer 1 uploads to Peer 2)
					downloadSocket = new Socket("localhost", prev_peer_port);
					System.out.println(String.format("Peer %d is the downloader to Peer %d on port %d", peer_number, prev_peer, prev_peer_port));

					//Start Thread of Peer to Peer
					peer_thread = new Handler(uploadSocket.accept(), summary_local,String.format("Peer %d",peer_number),peer_number,num_of_users,-1);
					System.out.println(String.format("Peer %d succesfully started its own UPLOAD thread on port %d with Peer %d", peer_number, upload_port, next_peer));
					peer_thread.start();

					//This MUST come after the "Start Thread of Peer to Peer" or else the objectinputstream() hangs
					//initialize inputStream and outputStream
					out2 = new ObjectOutputStream(downloadSocket.getOutputStream());
					out2.flush();
					in2 = new ObjectInputStream(downloadSocket.getInputStream());

					//If we get a connection then break out of loop
					notConnected = !downloadSocket.isConnected();
				}
				catch (ConnectException f) {
					System.err.println(String.format("WAIT %d seconds. Connection timeout. You need to initiate another Peer server first.",timeout_delay/1000));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				//System.out.println(String.format("STATUS: Peer %d is still connected to Peer %d", peer_number, prev_peer));
				//System.out.println("Closing time for Peer "+peer_number);
				timer.cancel();
			}
		}
	}

	//Run runs from runnable when thread started
	public void run()
	{
		try
		{
			//SETUP PEER TO HOST SERVER CONNECTION
			requestSocket = new Socket("localhost", 8000);
			System.out.println(String.format("Peer %d connected to Host on port 8000", peer_number));
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			//SETUP PEER TO PEER CONNECTIONS
			//EXAMPLE: Peer 2 uploads to Peer 3 on port 20002, Peer 3 downloads from Peer 2 on port 20002

			//Create connection as UPLOADER to other peer
			//Next peer is found as mod of value. Peers range from 0...N
			next_peer = Math.floorMod((this.peer_number + 1),this.num_of_users);
			//20000 is an arbitrary (probably) unused port. Ex: Peer 0 will open up port 20000 for listening to incoming connections
			upload_port = 20000 + peer_number;
			System.out.println(String.format("Peer %d is the uploader to Peer %d on port %d", peer_number, next_peer, upload_port));
			//Must create ServerSocket on the peer that will become the upload peer
			uploadSocket = new ServerSocket(upload_port);
			while (notConnected)
			{
				//System.out.println("here1");
				//sit and wait for peer to connect
				//System.out.println("Value of notConnected: "+notConnected);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			//System.out.println("here2");


//			//Create connection as DOWNLOADER to other peer
//			while (notConnected)
//			{
//				try{
//					//This peer attempts to open a thread on the previous peer port as the DOWNLOADER
//					prev_peer = Math.floorMod((peer_number - 1),this.num_of_users);
//					int prev_peer_port = 20000 + prev_peer;
//					//Ex: Peer 2 downloads from Peer 1 (thus, Peer 1 uploads to Peer 2)
//					downloadSocket = new Socket("localhost", prev_peer_port);
//					System.out.println(String.format("Peer %d is the downloader to Peer %d on port %d", peer_number, prev_peer, prev_peer_port));
//
//					//Start Thread of Peer to Peer
//					Thread peer_thread = new Handler(uploadSocket.accept(), summary_local,String.format("Peer %d",peer_number),peer_number);
//					System.out.println(String.format("Peer %d succesfully started its own UPLOAD thread on port %d with Peer %d", peer_number, upload_port, next_peer));
//					peer_thread.start();
//
//					//This MUST come after the "Start Thread of Peer to Peer" or else the objectinputstream() hangs
//					//initialize inputStream and outputStream
//					out2 = new ObjectOutputStream(downloadSocket.getOutputStream());
//					out2.flush();
//					in2 = new ObjectInputStream(downloadSocket.getInputStream());
//
//					//If we get a connection then break out of loop
//					notConnected = !downloadSocket.isConnected();
//				}
//				catch (ConnectException e) {
//					System.err.println("Connection refused. You need to initiate another Peer server first.");
//				} 
//			}

			//Find out number of chunks we will receive from Host/ create dummy for Peer
			num_of_chunks=(Integer)in.readObject();
			int dummy_num_chunks=(Integer)in2.readObject();
			
			//Core loop of sending/receiving
			while(doNotHaveChunks())
			{
				//SEND SUMMARY LIST to HOST and PEER
				System.out.println(String.format("Peer %d requested chunks from Host...", peer_number));
				System.out.println(String.format("Peer %d sent Chunk ID list to Host...", peer_number));
				getChunks(out, summary_local);
				System.out.println(String.format("Peer %d requested chunks from %s...", peer_number,String.format("Peer %d",prev_peer)));
				System.out.println(String.format("Peer %d sent Chunk ID list to %s...", peer_number,String.format("Peer %d",prev_peer)));
				getChunks(out2, summary_local);

				//READ INTEGER from HOST and PEER
				chunk_id=(Integer)in.readObject();
				chunk_id2=(Integer)in2.readObject();

				//READ CHUNK BYTES from HOST
				//Takes care of receiving the bytes and writes them to file instead of just renaming stuff
				//A chunk_id of -1 signifies that host has no new chunks to send
				if (chunk_id == -1)
				{
					byte[] received_bytes = (byte[]) in.readObject();
				}
				else
				{
					byte[] received_bytes = (byte[]) in.readObject();
					FileOutputStream filePart;
					File local = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%s_host_%s",String.format("%03d",chunk_id) ,".chunk"));
					filePart = new FileOutputStream(local);
					filePart.write(received_bytes);
					filePart.flush();
					filePart.close();
					summary_local.put(chunk_id, local);
					System.out.println(String.format("Peer %d received Chunk ID %d to give Chunk ID list: %s", peer_number, chunk_id, summary_local.keySet().toString()));
				}

				//READ CHUNK BYTES from PEER
				//Takes care of receiving the bytes and writes them to file 
				if (chunk_id2 == -1)
				{
					byte[] received_bytes2 = (byte[]) in2.readObject();
				}
				else
				{
					byte[] received_bytes2 = (byte[]) in2.readObject();
					FileOutputStream filePart2;
					File local2 = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%s_host_%s",String.format("%03d",chunk_id2) ,".chunk"));
					filePart2 = new FileOutputStream(local2);
					filePart2.write(received_bytes2);
					filePart2.flush();
					filePart2.close();
					summary_local.put(chunk_id2, local2);
					System.out.println(String.format("Peer %d received Chunk ID %d to give Chunk ID list: %s", peer_number, chunk_id2, summary_local.keySet().toString()));
				}
			}
			//After all chunks are received, merge chunks into single file
			recreateFile();
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
			System.err.println("Class not found");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
				out.close();
				//downloadSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

	//Send a summary list for the Peer to the Host or other Peer with the chunks he/she has
	synchronized void getChunks(ObjectOutputStream out, Map<Integer, File> summary_local)
	{
		//MUST MUST MUST pass a copy and not a reference or else you will have update/reading issues! Use 'new'!
		Map <Integer, File> summary_sent=new HashMap<Integer,File>(summary_local);
		try{
			//stream write the message
			out.writeObject(summary_sent);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}


}
