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

//Renamed to Client from Peer on 11/30
//Assume client = peer
public class Client implements Runnable
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
	int num_of_chunks; //number of chunks host split file into
	int upload_port; //the port the client will upload to a client on
	int prev_peer_port; //the port the client will download on
	int host_port; //the port to connect to the server on
	int next_peer; //the client number of the next peer in this circul architecture
	private int peer_number;	//this peer's/client's number
	private int num_of_users;	//number of users in network (except host)
	private int prev_peer;		//the previous peers number in the circle (used to connect with)
	boolean notConnected = true;	//used in a loop to allow other peer to connect as download peer on serversocket
	Timer timer; //timer to get a socket
	Thread peer_thread; //client to client thread
	private int TIMEOUT_DELAY = 3000; //(ms) the delay to attempt to get connection between client and server started
	Map<Integer,File> chunk_id_summary_receiving; //the received chunk id summary from host/other peer on the chunks that will be received shortly
	private String original_filename; //filename, name only, eg 'Drew.pdf'

	public static void main(String[] args)
	{
		//Type in the Client number of this client at Terminal
		Scanner in = new Scanner(System.in);
		System.out.println("Enter Peer Number (0..N-1): ");
		int peer_number = in.nextInt();
		in.close();
		//HARD SET TO KNOW THAT THERE ARE 5 USERS IN THIS SYSTEM. COULD BE MORE DYNAMIC IF NEEDED.
		int number_of_users = 5;
		//		System.out.println("Enter number of peers in network: ");
		//		int number_of_users = in.nextInt();
		//		System.out.println(String.format("Enter program folder to save Peer %s files:",peer_number));
		//		String folder = in.next();
		//		System.out.println(String.format("Enter number of chunks expecting to receive:"));
		//		int number_of_chunks = in.nextInt();

		try {
			Client peer = new Client(peer_number, number_of_users);
			Thread p = new Thread(peer);
			p.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Client(int peer_number, int number_of_users)
	{
		System.out.println(String.format("Peer %d was created...", peer_number));
		this.peer_number = peer_number;
		this.num_of_users = number_of_users;
		//this.programFilename = programfilename;
		this.num_of_chunks = 1;
		//Read the network.config file to see what ports this Client will be using/connecting with
		readConfiguration();
		//Make a folder to save chunks for this peer in /Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp
		makeFolder();

		//Setup/start timer to fire every (3) seconds to try and get a connection from Client to Server
		timer = new Timer();
		timer.scheduleAtFixedRate(new GetSocketTask(),1000,TIMEOUT_DELAY);
	}

	//Read from hardcoded network.config file
	private void readConfiguration()
	{
		File config = new File("/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp/network.config");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(config));
			String line;
			//Read in line by line
			while ((line = reader.readLine()) != null)
			{
				//Each line of file looks something like: '0 20000 1 4 20004' which has index locations 0,1,2,3,4
				//Read in with whitespace as delimiter
				String[] type = line.split(" ");
				//Look at the first value in the line. This denotes the Server or Client number. Servers use '-1', Clients use 0-4
				int val = new Integer(type[0]);

				//For Server only
				if (val == -1)
				{
					host_port = new Integer(type[1]);
				}
				//Sets up clients
				else if (val == peer_number)
				{
					upload_port = new Integer(type[1]);
					next_peer = new Integer(type[2]);
					prev_peer = new Integer(type[3]);
					prev_peer_port = new Integer(type[4]);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//Method to combine chunks into one file after all chunks are received
	private void recreateFile()
	{
		File full_file = new File(chunk_folder.toString()+"/"+original_filename);

		FileInputStream inputStream;
		FileOutputStream fileFull;

		String outputFolderName = chunk_folder.toString();
		File directory = new File(outputFolderName);
		File[] directoryListing = directory.listFiles();

		//Must sort as listFiles() for a directory does not do so automatically.
		Arrays.sort(directoryListing);
		//Loop through SORTED files writing to file
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
		System.out.println(String.format("Combined chunks in Peer %d to form file %s in %s", peer_number,original_filename, chunk_folder.toString()));
	}

	//Method to make a folder for peer to store chunks in
	private void makeFolder()
	{
		try
		{
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

	//Task used to try and get connections between two Clients
	class GetSocketTask extends TimerTask
	{
		@Override
		public void run() {

			//Checks to see if we have connected to the Server
			if (notConnected)
			{
				try{
					//This peer attempts to open a thread on the previous peer port as the DOWNLOADER
					//Ex: Peer 2 downloads from Peer 1 (thus, Peer 1 uploads to Peer 2)
					//"localhost" is using 127.0.0.1. Could be changed to IP of server in real application
					downloadSocket = new Socket("localhost", prev_peer_port);
					System.out.println(String.format("Peer %d is the downloader to Peer %d on port %d", peer_number, prev_peer, prev_peer_port));

					//Start Thread of Peer to Peer
					peer_thread = new Handler(uploadSocket.accept(), summary_local,String.format("Peer %d",peer_number),peer_number,num_of_users,-1,"blank");
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
					System.err.println(String.format("WAIT %d seconds. Connection timeout. You need to initiate another Peer first.",TIMEOUT_DELAY/1000));
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

	//Send a summary list for the Peer to the Host or other Peer with the chunks he/she has
	synchronized void getChunks(ObjectOutputStream out, Map<Integer, File> summary_local)
	{
		//MUST MUST MUST pass a copy and not a reference or else you will have update/reading issues! Use 'new'!
		Map <Integer, File> summary_sent=new HashMap<Integer,File>(summary_local);
		try{
			//stream write the message
			System.out.println(String.format("Peer %d requested chunks from %s...", peer_number,String.format("Peer %d",prev_peer)));
			System.out.println(String.format("Peer %d sent Chunk ID list to %s...", peer_number,String.format("Peer %d",prev_peer)));
			out.writeObject(summary_sent);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	//Run runs from runnable when thread started
	@SuppressWarnings("unchecked")
	public void run()
	{
		try
		{
			//SETUP PEER TO HOST SERVER CONNECTION
			requestSocket = new Socket("localhost", host_port);
			System.out.println(String.format("Peer %d connected to Host on port %d", peer_number,host_port));
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());



			System.out.println(String.format("Peer %d is the uploader to Peer %d on port %d", peer_number, next_peer, upload_port));
			//Must create ServerSocket on the peer that will become the upload peer
			uploadSocket = new ServerSocket(upload_port);

			//SETUP PEER TO PEER CONNECTIONS
			//EXAMPLE: Peer 2 uploads to Peer 3 on port 20002, Peer 3 downloads from Peer 2 on port 20002
			//This run() was run when the thread was started and gets STOPPED here until notConnected = false, which is being driven by our GetSocketTask
			while (notConnected)
			{
				//sit and wait for peer to connect
				try {
					//Try every 1 second to check whether or not notConnected=false yet. Decreased CPU use/rechecking
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			//CORE AREA FOR CONNECTIONS AND FLOW OF DATA
			//out/in = Server/Client
			//out2/in2 = Client/Client

			//Send peer number of downloader (self)
			out.writeObject(this.peer_number);
			out2.writeObject(this.peer_number);

			//Get filename name (ex: Drew.pdf) so we know what to save file as later
			original_filename = (String)in.readObject();
			//Some 'dummy' variables are used so that the Handler class works properly for BOTH Clients and Server connections
			String dummy_filename = (String)in2.readObject();

			//Find out number of chunks we will receive from Host/ create dummy for Peer
			num_of_chunks=(Integer)in.readObject();
			int dummy_num_chunks=(Integer)in2.readObject();

			//SEND SUMMARY LIST to HOST and PEER
			System.out.println(String.format("Peer %d requested chunks from Host...", peer_number));
			System.out.println(String.format("Peer %d sent Chunk ID list to Host...", peer_number));
			getChunks(out, summary_local);

			//READ SUMMARY from HOST and PEER
			chunk_id_summary_receiving=(Map<Integer,File>)in.readObject();

			//READ CHUNK BYTES from HOST
			//Takes care of receiving the bytes and writes them to file instead of just renaming stuff
			//A chunk_id of -1 signifies that host has no new chunks to send
			//This had been a problem. For loop is under assumption that if someone sends a list of length 10, then the downloader will receiver 10 objects
			//NOTE: Had to use this for loop because Maps are NOT guaranteed to be in sorted keyvalue order!!!!
			for (int a = 0; a < chunk_id_summary_receiving.size(); a++) 
			{
				Integer chunk_id = (Integer) in.readObject();
				byte[] received_bytes = (byte[]) in.readObject();

				FileOutputStream filePart;
				File local = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%s_host_%s",String.format("%03d",chunk_id) ,".chunk"));
				//A little error checking so we arent overwriting data
				if (!local.exists())
				{
					filePart = new FileOutputStream(local);
					filePart.write(received_bytes);
					filePart.flush();
					filePart.close();
					summary_local.put(chunk_id, local);
					System.out.println(String.format("Peer %d received Chunk ID %d from Host to give Chunk ID list: %s with size (bytes): %d", peer_number, chunk_id, summary_local.keySet().toString(), (int) local.length()));
				}
				else
				{
					System.out.println("I tried to overwrite data to a current location on Host receiving.");
				}
			}

			//Core loop of sending/receiving between CLIENT/CLIENT
			while(doNotHaveChunks())
			{
				//REQUEST FOR CHUNKS FROM CLIENT: Getchunks() requests chunks based on his summary chunk ID list from other Client
				getChunks(out2, summary_local);

				//Only send chunk id list every 3 seconds to minimize excessive requests
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//This is the chunk IDs the uploader will be sending shortly...
				chunk_id_summary_receiving = (Map<Integer,File>)in2.readObject();

				//READ CHUNK BYTES from PEER
				//Takes care of receiving the bytes and writes them to file 
				//If no bytes need to be received then do NOT write them to file
				if (chunk_id_summary_receiving.size() == 0)
				{
					byte[] received_bytes2 = (byte[]) in2.readObject();
				}
				else
				{
					//If we know that 10 chunks for coming, for loop 10x and record the chunk ID and save to the file folder for Client
					for (int a = 0; a< chunk_id_summary_receiving.size(); a++) 
					{
						Integer chunk_id2 = (Integer) in2.readObject();						
						byte[] received_bytes2 = (byte[]) in2.readObject();

						FileOutputStream filePart2;
						File local2 = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%s_host_%s",String.format("%03d",chunk_id2) ,".chunk"));
						//Small error checking to not overwrite
						if (!local2.exists())
						{
							filePart2 = new FileOutputStream(local2);
							filePart2.flush();
							filePart2.write(received_bytes2);
							filePart2.flush();
							filePart2.close();
							summary_local.put(chunk_id2, local2);
							System.out.println(String.format("Peer %d received Chunk ID %d from Peer %d to give Chunk ID list: %s of size (bytes): %d", peer_number, chunk_id2, prev_peer, summary_local.keySet().toString(), (int) local2.length()));
						}
						else
						{
							System.out.println("I tried to overwrite data to a current location on Peer receiving.");
						}
					}
				}
			}

			//MERGE ALL CHUNKS. We get here after we have received all chunks
			recreateFile();

			//After all chunks are received then close connections...
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
				in2.close();
				out2.close();
				requestSocket.close();
				downloadSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
}
