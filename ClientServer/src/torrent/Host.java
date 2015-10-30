package torrent;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/* Host is the user with the original file that is to be sent
 * 
 */

public class Host implements Runnable {

	private final int sPort = 8000;   //the host server will be listening on this port number for incoming connections
	private File input_file;	//the file to be split up
	private File chunk_folder;	//the location to store the chunks
	private int size_of_chunks;	//the size (kb, Mb, etc) of the chunks
	public static int num_of_chunks;	//number of chunks created based on the size_of_chunks
	private int num_of_users;	//number of users in the program. num of users to receive chunks
	private String filename;	//filename to be split
	private String BYTE_TYPE = "kB";	//part of creating chunks	
	private Map<Integer, File> summary_local = new HashMap<Integer, File>();	//chunk summary of host

	public Host(String filename, int size_of_chunks, String byte_type, int num_of_users)
	{
		this.filename=filename;
		this.size_of_chunks=size_of_chunks;
		this.num_of_users = num_of_users;
		this.BYTE_TYPE = byte_type;
		System.out.println(String.format("Host was created..."));
		makeFolder();
	}

	//Creates custom chunk ID sets divided equally amongst number of users in program
	//Ex: 5 users in program (Peers 0,1,2,3,4) . 10 chunks (0..9). A user will get chunks (0,5) or 1,6 or 4,9...
	public Map<Integer,File> createUserSpecificSummary(int clientNumber)
	{
		//Create a special summary list for each peer
		Map<Integer, File> summary_user = new HashMap<Integer,File>();
		//Ex: number of chunks = 10, a ranges from 0 to 9, num of users = 5, client numbers from 0 to 4
		//Ex: 0 mod 5 = 0, 4 mod 5 = 4, 5 mod 5 = 0, 9 mod 5 = 4, etc
		for (int a=0; a<summary_local.size(); a++)
		{
			if (clientNumber == (a % TorrentProgram.num_of_users))
			{
				summary_user.put(a, summary_local.get(a));
			}
		}
		return summary_user;
	}

	//Main function
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
		//Clients range from 0..N-1
		int clientNum = 0;
		try {
			//Keep listening for peers to connect
			while(true) {
				try {		
					//Create thread for incoming peer initiated with custom user summary of chunks available to send
					Thread peer_thread = new Handler(listener.accept(), createUserSpecificSummary(clientNum), "Host", -1);
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

	//Make folder to store Host chunks
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

	//Load the file for host that will be broken into chunks
	public void loadFile()
	{
		System.out.println("Loading file at Host...");
		input_file = new File(this.filename);
		breakFile();
	}

	//Break the file into chunks based on chunk size
	private void breakFile()
	{
		//Size of file in bytes
		File inputFile = new File(this.filename);
		int fileSize = (int) inputFile.length();
		
		System.out.println(String.format("Size of file in bytes: %d", fileSize));
		System.out.println(String.format("File broken at Host into %d chunks...", num_of_chunks));

		//Streams that read/write to Files
		FileInputStream inputStream;
		FileOutputStream filePart;

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

			//Keep breaking file while there are still chunks left. Reused code.
			while (fileSize > 0) 
			{
				//Minor tweak so that file sizes are EXACTLY the same
				if (fileSize < readLength)
				{
					readLength = fileSize;
				}
				byteChunkPart = new byte[readLength];
				//When you read from the inputstream then you are kind of decrementing it at the same time. Less is leftover. Original file remains intact.
				read = inputStream.read(byteChunkPart, 0, readLength);
				fileSize = fileSize - read;
				File chunk_file = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%s_host_%s", String.format("%03d",num_of_chunks),".chunk"));
				summary_local.put(new Integer(num_of_chunks), chunk_file);
				filePart = new FileOutputStream(chunk_file);
				filePart.write(byteChunkPart);
				filePart.flush();
				filePart.close();
				byteChunkPart = null;
				filePart = null;
				System.out.println(String.format("Host has Chunk ID %d chunk file: %s", num_of_chunks, chunk_file.toString()));
				num_of_chunks++;
			}
			inputStream.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
