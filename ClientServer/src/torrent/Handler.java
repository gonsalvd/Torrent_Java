package torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * This is a Thread class that is created by the serving host. The serving host can either be the original
 * host who has the original chunk set or the peer in a Peer to Peer relationship who acts as the upload peer.
 */

public class Handler extends Thread {

	private String message;    //message received from the client
	private String MESSAGE;    //uppercase message send to the client
	private Socket connection;	//a serversocket that connects two peers or a peer and host
	private ObjectInputStream in;	//stream read from the socket
	private ObjectOutputStream out;    //stream write to the socket
	private FileInputStream inputStream; //sends bytes of file 
	private int peer_num;		//The index number of the client
	private int next_peer;	//next peer, peer we are uploading to (either from Server or other Client)
	private Map<Integer,File> host_summary;	//a reference to the host of the threads summary. ever changing.
	private Map<Integer,File> peer_summary;	//a received copy of the peer who is requesting chunks
	private Map<Integer, File> summary_diff = new HashMap<Integer,File>();	//the chunks the host should send after a difference
	private String host_name;	//the name of the host that started the thread (either Host or Peer *)
	private int num_of_chunks; //number of chunks host broke file into
	private File FILENAME; //full filename of file broken by host (eg: /Users/..../Drew.pdf)
	private int host_download_peer_num; //the downloading Client of the Server

	public Handler(Socket connection, Map<Integer,File> host_summary, String host_name, int peer_number, int num_of_users, int number_of_chunks, String filename) 
	{
		this.connection = connection;
		this.host_summary = host_summary;
		this.host_name = host_name;
		this.peer_num = peer_number;
		this.num_of_chunks = number_of_chunks;
		this.FILENAME = new File(filename);
	}

	//Creates chunk sent the host of the thread should be sending to the receiver
	void compareSummary(Map<Integer, File> summary_in)
	{
		//Summary_diff is the map that has all the keys local needs from upload (summary_in) side
		this.summary_diff.clear();
		//Create COPY of host_summary 
		this.summary_diff = new HashMap<Integer,File>(host_summary);
		//Find the difference of the host copy and what the receiver already has to get what the host can send 
		this.summary_diff.keySet().removeAll(summary_in.keySet());
	}

	//Main function that starts upon thread start
	@SuppressWarnings("unchecked")
	public void run() 
	{
		try{
			//initialize Input and Output streams
			out = new ObjectOutputStream(connection.getOutputStream());
			out.flush();
			in = new ObjectInputStream(connection.getInputStream());

			//ALL HOST CONNECTION STUFF ONLY
			//Get peer number who connected to HOST 
			next_peer = (Integer) in.readObject();
			//Added this line to keep track of host stuff. Chainged in the main while loop
			host_download_peer_num = next_peer;

			//Send filename to downloader from HOST
			String filename = FILENAME.getName().toString();
			//This IF statement prevents incorrect printouts when the peer doesnt know what the filename is 
			if (host_name == "Host")
			{
				System.out.println(String.format("Sent filename %s to Peer %d...", filename,host_download_peer_num ));
			}
			out.writeObject(filename);

			//Send number of chunks from HOST to peer
			if (host_name == "Host")
			{
				System.out.println(String.format("Host has %d chunks to send to Peer %d...", host_summary.size(), host_download_peer_num));
			}
			out.writeObject(num_of_chunks);

			//Main loop 
			//HOST/CLIENT SENDING/RECEIVING
			while(true)
			{
				//READ SUMMARY LIST
				this.peer_summary = (Map<Integer, File>)in.readObject();
				System.out.println(String.format("%s received chunk request from Peer %d...", host_name, next_peer));
				System.out.println(String.format("%s received Chunk ID list from Peer %d...", host_name, next_peer));

				//SEND ALL CHUNKS from summary_diff
				compareSummary(this.peer_summary);
				sendChunk(this.summary_diff);

				//Close connection for HOST after having sent all stuff
				if (host_name == "Host")
				{
					break;
				}
			}
		}
		catch(ClassNotFoundException classnot){
			System.err.println("Data received in unknown format");
		}
		catch(IOException ioException){
			//System.out.println(String.format("Thread hosted by %s was disconnected due to all chunks being received...", host_name));
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				connection.close();
				System.out.println(String.format("Thread hosted by %s was disconnected with Peer %d due to all chunks being received...", host_name, host_download_peer_num));
			}
			catch(IOException ioException){
				System.out.println("IO Exception in Thread");
			}
		}
	}

	//Send single chunk at a time from the summary_diff list
	synchronized public void sendChunk(Map<Integer,File> summary_diff)
	{
		//If the host of the thread does not have any chunks it can send then send dummy values of -1/no bytes
		if (summary_diff.size() == 0)
		{
			try {
				out.writeObject(summary_diff);
				out.flush();
				out.writeObject(new byte[0]);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else 
		{
			try {
				//Send the summary file so the receiver knows what is coming
				out.writeObject(summary_diff);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//Send the FIRST entry in the summary_diff chunk list even IF there are more in list. Send one at a time!
			for (Map.Entry<Integer,File> entry : summary_diff.entrySet()) {
				
				Integer chunk_id = entry.getKey();
				File chunk_file = entry.getValue();

				try
				{
					//Send CHUNK ID first
					out.writeObject(chunk_id);
					out.flush();
					//We want to actually write a file to a byte[]
					//Size of file in bytes
					int fileSize = (int) chunk_file.length();
					byte[] byteChunkPart;
					inputStream = new FileInputStream(chunk_file);
					byteChunkPart = new byte[fileSize];
					//When you read from the inputstream then you are kind of decrementing it at the same time. Less is leftover. Original file remains intact.
					inputStream.read(byteChunkPart, 0, fileSize);				
					
					//Send FILE BYTES 
					out.writeObject(byteChunkPart);
					out.flush();
					System.out.println(String.format("%s sent chunk ID %d to Peer %d of size (bytes): %d",host_name, chunk_id, next_peer, fileSize));	
				}

				catch(IOException ioException)
				{
					ioException.printStackTrace();
				}
			}
		}
	}
}
