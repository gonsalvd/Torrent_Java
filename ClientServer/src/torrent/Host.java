package torrent;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Host implements Runnable {

	private static final int sPort = 8000;   //The server will be listening on this port number
	private File input_file;
	private File chunk_folder;
	private static int size_of_chunks;
	private static int num_of_chunks;
	private int num_of_users;
	private String filename;
	private String BYTE_TYPE = "kB";
	private Map<Integer, String> summary_local = new HashMap<Integer, String>();

	public Host(String filename, int size_of_chunks, String byte_type, int num_of_users)
	{
		this.filename=filename;
		this.size_of_chunks=size_of_chunks;
		this.num_of_users = num_of_users;
		this.BYTE_TYPE = byte_type;

		makeFolder();
	}

	private void makeFolder()
	{
		try
		{
			chunk_folder = new File(TorrentProgram.FILE_FOLDER+"/torrent_tmp/host/");
			if (!chunk_folder.exists()) {
				if (chunk_folder.mkdir()) {
					System.out.println(String.format("Directory created for Host: %s ", chunk_folder.toString()));
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

	public void loadFile()
	{
		System.out.println("Loading file at Host...");
		input_file = new File(this.filename);
		breakFile();
	}

	private void breakFile()
	{
		File inputFile = new File(this.filename);
		String outputFileName = chunk_folder.toString();

		FileInputStream inputStream;
		FileOutputStream filePart;

		//Size of file in bytes
		int fileSize = (int) inputFile.length();
		System.out.println(String.format("Size of file in bytes: %d", fileSize));

		num_of_chunks = 0;
		int read = 0;
		int readLength=0;
		byte[] byteChunkPart;


		//Determine how to break file based on input
		if( BYTE_TYPE == "kB" )
		{
			readLength = size_of_chunks * 1000;
		} 
		else if ( BYTE_TYPE == "MB" )
		{
			readLength = size_of_chunks * 1000000;
		}

		try 
		{
			//The fileinputstream becomes this buffer that gets smaller as your .read() out of it
			inputStream = new FileInputStream(inputFile);

			while (fileSize > 0) 
			{
				byteChunkPart = new byte[readLength];
				//When you read from the inputstream then you are kind of decrementing it at the same time. Less is leftover. Original file remains intact.
				read = inputStream.read(byteChunkPart, 0, readLength);
				fileSize = fileSize - read;
				File chunk_file = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%d_host_%s", num_of_chunks+1,".chunk"));
				summary_local.put(new Integer(num_of_chunks+1), chunk_file.toString());
				filePart = new FileOutputStream(chunk_file);
				filePart.write(byteChunkPart);
				filePart.flush();
				filePart.close();
				byteChunkPart = null;
				filePart = null;
				num_of_chunks++;

				//				System.out.println(String.format("Filesize: %d", fileSize));
				System.out.println(String.format("Host has chunk ID %d chunk file: %s", num_of_chunks, chunk_file.toString()));
			}
			inputStream.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		System.out.println(String.format("File broken at Host into %d chunks...", num_of_chunks));
	}

	public void distributeFile()
	{
		//Distribute file based on peer number


	}


	public void run() {
		// TODO Auto-generated method stub
		System.out.println("The Host is running..."); 
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(sPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int clientNum = 1;
		try {
			while(true) {
				try {
					Thread peer_thread = new Handler(listener.accept(),clientNum, num_of_users, summary_local);
					peer_thread.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				clientNum++;
			}
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
	}

	/**
	 * A handler thread class.  Handlers are spawned from the listening
	 * loop and are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {

		private String message;    //message received from the client
		private String MESSAGE;    //uppercase message send to the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int peer_num;		//The index number of the client
		private Map<Integer,String> host_summary;
		private Set<Integer> peer_summary;
		private int num_peers;

		public Handler(Socket connection, int peer_num, int num_peers, Map<Integer,String> host_summary) {
			this.connection = connection;
			this.peer_num = peer_num;
			this.host_summary = host_summary;
			this.num_peers = num_peers;
		}

		//send a message to the output stream
		public void sendMessage(String msg)
		{
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + peer_num);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

		//send a message to the output stream
		public void sendChunk()
		{
			//PROBLEM: We do not know how many total users there will be if we start sending chunks just as a connection is established in real life!
			//SOLUTION: Send total number of users initially
			//Mod function
			//ex 19 chunks, 6 users, 19 mod 1
			//user 1 should get chunk 1, 7, 13, 19
			//user 2 should get chunk 2, 8, 14
			//user 3 should get chunk 3, 9, 15
			File chunk_file;
			int chunk_id;
			for (int a=1; a<=num_of_chunks; a++)
			{
				chunk_id=a;
				if (peer_num == (a % num_peers))
				{
					chunk_file = new File(host_summary.get(chunk_id));
					try{
						//Send a value of the chunk id first
						out.writeObject(new Integer(chunk_id));
						out.writeObject(chunk_file);
						out.flush();
						System.out.println(String.format("Sent chunk ID %d to Peer %d",chunk_id, peer_num));
						//Delete file from Host

					}
					catch(IOException ioException){
						ioException.printStackTrace();
					}
				}
			}
//			int chunk_id=1;
//			File chunk_file = new File(host_summary.get(chunk_id));
//			try{
//				out.writeObject(chunk_file);
//				out.flush();
//				System.out.println(String.format("Sent chunk ID %d to Peer %d",chunk_id, peer_num));
//				//Delete file from Host
//
//			}
//			catch(IOException ioException){
//				ioException.printStackTrace();
//			}
		}

		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				//sendChunk();
				in = new ObjectInputStream(connection.getInputStream());
//				if(peer_num==1)
//				{
//					sendChunk();
//				}
				try{
					while(true)
					{
						
						//receive the message sent from the client
						peer_summary = (Set<Integer>)in.readObject();
						//show the message to the user
						System.out.println("Receive message: " + peer_summary + " from client " + peer_num);
						
						sendChunk();
					}
				}
				catch(ClassNotFoundException classnot){
					System.err.println("Data received in unknown format");
				}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Peer " + peer_num);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Peer " + peer_num);
				}
			}
		}
	}


}
