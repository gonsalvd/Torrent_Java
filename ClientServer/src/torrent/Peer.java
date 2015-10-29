package torrent;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Peer implements Runnable
{

	private File chunk_folder;
	private File received_file;

	Socket requestSocket;           //socket connect to the server
	Socket downloadSocket;
	ServerSocket uploadSocket;
	//	ServerSocket uploadSocket = null;
	//	private int sPort;   //The server will be listening on this port number

	ObjectOutputStream out;         //stream write to the socket
	ObjectInputStream in;          //stream read from the socket
	ObjectOutputStream out2;         //stream write to the socket
	ObjectInputStream in2;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server

	Map<Integer, File> summary_local = new HashMap<Integer, File>();
	Map<Integer, File> summary_diff = new HashMap<Integer, File>();
	Set summary_sent;
	int chunk_id;
	int chunk_id2;
	private int peer_number;
	private int prev_peer;
	boolean notConnected = true;


	public Peer(int peer_number)
	{
		this.peer_number = peer_number;
		makeFolder();
	}

	private void recreateFile()
	{
		String fileOutputName = "song.mp3";
		File full_file = new File(chunk_folder.toString()+"/"+fileOutputName);

		FileInputStream inputStream;
		FileOutputStream fileFull;
		
		String outputFolderName = chunk_folder.toString();
		File directory = new File(outputFolderName);
		File[] directoryListing = directory.listFiles();
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

	private void makeFolder()
	{
		try
		{
			chunk_folder = new File(TorrentProgram.FILE_FOLDER+String.format("/torrent_tmp/peer%d/",peer_number));
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

	Set<Integer> sendDownloadList()
	{
		System.out.println("Chunk ID List sent by downloader...");
		return summary_diff.keySet();
	}

	Set<Integer> sendUploadList()
	{
		System.out.println("Chunk ID List sent by uploader...");
		return summary_local.keySet();
	}

	private boolean doNotHaveChunks()
	{
		return summary_local.size() < Host.num_of_chunks;
	}

	public void run()
	{
		try
		{
			//create a socket to connect to the server
			requestSocket = new Socket("localhost", 8000);
			System.out.println(String.format("Peer %d connected to Host in port 8000", peer_number));
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());

			//SETUP PEER TO PEER CONNECTIONS
			//EXAMPLE: Peer 2 uploads to Peer 3 on port 20002, Peer 3 downloads from Peer 2 on port 20002

			//Create connection as UPLOADER to other peer
			int next_peer = Math.floorMod((peer_number + 1),TorrentProgram.num_of_users);
			int upload_port = 20000 + peer_number;
			System.out.println(String.format("Peer %d is the uploader to Peer %d on port %d", peer_number, next_peer, upload_port));
			uploadSocket = new ServerSocket(upload_port);

			//Create connection as DOWNLOADER to other peer
			while (notConnected)
			{
				try{
					prev_peer = Math.floorMod((peer_number - 1),TorrentProgram.num_of_users);
					int prev_peer_port = 20000 + prev_peer;

					downloadSocket = new Socket("localhost", prev_peer_port);
					System.out.println(String.format("Peer %d is the downloader to Peer %d on port %d", peer_number, prev_peer, prev_peer_port));


					//Start Thread of Peer to Peer
					Thread peer_thread = new Handler(uploadSocket.accept(), summary_local,String.format("Peer %d",peer_number));
					System.out.println(String.format("Peer %d succesfully started its own UPLOAD thread on port %d with Peer %d", peer_number, upload_port, next_peer));
					peer_thread.start();

					//initialize inputStream and outputStream
					out2 = new ObjectOutputStream(downloadSocket.getOutputStream());
					out2.flush();
					in2 = new ObjectInputStream(downloadSocket.getInputStream());

					notConnected = !downloadSocket.isConnected();
				}
				catch (ConnectException e) {
					System.err.println("Connection refused. You need to initiate a server first.");
				} 
			}

			while(doNotHaveChunks())
			{
				//SEND SUMMARY LIST
				System.out.println(String.format("Peer %d requested chunks from Host. Peer %d ID Summary: %s", peer_number,peer_number,summary_local.keySet().toString()));
				getChunks(out, summary_local);
				System.out.println(String.format("Peer %d requested chunks from %s. Peer %d ID Summary: %s", peer_number,String.format("Peer %d",prev_peer),peer_number,summary_local.keySet().toString()));
				getChunks(out2, summary_local);

				//READ INTEGER
				chunk_id=(Integer)in.readObject();
				chunk_id2=(Integer)in2.readObject();

				//DEBUG
				//System.out.println(chunk_id=(Integer)in.readObject());
				//System.out.println(chunk_id2=(Integer)in2.readObject();


				//READ BYTES
				//Takes care of receiving the bytes and writes them to file instead of just renaming stuff
				//A chunk_id of -1 signifies that host has no new chunks to send
				if (chunk_id == -1)
				{
					byte[] received_bytes = (byte[]) in.readObject();
					//System.out.println("Chunk ID is -1");
				}
				else
				{
					byte[] received_bytes = (byte[]) in.readObject();
					FileOutputStream filePart;
					File local = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%d_host_%s",chunk_id ,".chunk"));
					filePart = new FileOutputStream(local);
					filePart.write(received_bytes);
					filePart.flush();
					filePart.close();
					summary_local.put(chunk_id, local);
					System.out.println(String.format("Peer %d received chunk %d to give summary list: %s", peer_number, chunk_id, summary_local.keySet().toString()));
				}

				//READ BYTES
				//Takes care of receiving the bytes and writes them to file instead of just renaming stuff
				if (chunk_id2 == -1)
				{
					byte[] received_bytes2 = (byte[]) in2.readObject();
					//System.out.println("Chunk ID2 is 0");
				}
				else
				{
					byte[] received_bytes2 = (byte[]) in2.readObject();
					FileOutputStream filePart2;
					File local2 = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%d_host_%s",chunk_id2 ,".chunk"));
					filePart2 = new FileOutputStream(local2);
					filePart2.write(received_bytes2);
					filePart2.flush();
					filePart2.close();
					summary_local.put(chunk_id2, local2);
					System.out.println(String.format("Peer %d received chunk %d to give summary list: %s", peer_number, chunk_id2, summary_local.keySet().toString()));
				}
			}
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
				downloadSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	synchronized void getChunks(ObjectOutputStream out, Map<Integer, File> summary_local)
	{
		//NOTE: Had serialization issue. I guess you cannot send the 'raw' summary set, and neeed to make a new HashSe
		//MUST MUST MUST do a shallow copy of this!!!
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

	void initializePeerNumberWithHost()
	{
		try {
			out.writeObject(this.peer_number);
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
