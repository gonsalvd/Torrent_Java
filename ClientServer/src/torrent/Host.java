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

	private int sPort;   //the host server will be listening on this port number for incoming connections
	private File input_file;	//the file to be split up
	private File chunk_folder;	//the location to store the chunks
	private static int size_of_chunks;	//the size (kb, Mb, etc) of the chunks
	public static int num_of_chunks;	//number of chunks created based on the size_of_chunks
	public static int num_of_users;	//number of users in the program. num of users to receive chunks
	private String filename;	//filename to be split
	private static String BYTE_TYPE = "kB";	//part of creating chunks	
	private Map<Integer, File> summary_local = new HashMap<Integer, File>();	//chunk summary of host
	
	private static boolean DEBUG_MODE_ON = false;	//debugger
	private static boolean INPUT_MODE = false;
	private static String fullPathname;	//filename to be broken into chunks
	public static String FILE_FOLDER;	//folder where file located. also where all host/peer chunks will be stored
	public static String FILENAME;	//filename
	private static File local; //manages local files

	
	public static void main(String[] args)
	{
		System.out.println("Created by: Drew Gonsalves");
		if (DEBUG_MODE_ON)
		{
			fullPathname = "/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/chord.pdf";
			local = new File(fullPathname);
			FILE_FOLDER = local.getParent();
			FILENAME = local.getName().toString();
			num_of_users = 3;
			size_of_chunks = 50; //kb
		}
		else if (INPUT_MODE)
		{
			Scanner in = new Scanner(System.in);
			System.out.println("Enter full path to file: ");
			fullPathname = in.next();
			local = new File(fullPathname);
			FILE_FOLDER = local.getParent();
			FILENAME = local.getName().toString();
			System.out.println("Enter number of peers in network: ");
			num_of_users = in.nextInt();
			System.out.println(String.format("Enter size of chunks (%s): ",BYTE_TYPE));
			size_of_chunks = in.nextInt();
			in.close();
		}
		//Hardset mode
		else
		{
			Scanner in = new Scanner(System.in);
			System.out.println("Enter full path to file: ");
			fullPathname = in.next();
			local = new File(fullPathname);
			FILE_FOLDER = local.getParent();
			FILENAME = local.getName().toString();
			num_of_users=5;
			size_of_chunks=100;
		}
		try {
			Host host = new Host(fullPathname, size_of_chunks, BYTE_TYPE, num_of_users);
			host.loadFile();
			Thread h = new Thread(host);
			h.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Host(String filename, int size_of_chunks, String byte_type, int num_of_users)
	{
		this.filename=filename;
		//this.size_of_chunks=size_of_chunks;
		//this.num_of_users = num_of_users;
		//this.BYTE_TYPE = byte_type;
		System.out.println(String.format("Host was created..."));
		readConfiguration();
		makeFolder();
	}
	
	private void readConfiguration()
	{
		File config = new File("/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp/network.config");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(config));
			String line;
			while ((line = reader.readLine()) != null)
			{
				//String word = new Scanner(line);
				String[] type = line.split(" ");
				int val = new Integer(type[0]);
				
				if (val == -1)
				{
					sPort = new Integer(type[1]);
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
			if (clientNumber == (a % num_of_users))
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
					Thread peer_thread = new Handler(listener.accept(), createUserSpecificSummary(clientNum), "Host", -1,num_of_users, num_of_chunks,FILENAME);
					peer_thread.start();
					System.out.println("A Peer connected to host...");
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


	private void makeFolder()
	{
		//Make folder for program 
		try
		{
			//File torrent_folder = new File(FILE_FOLDER+"/torrent_tmp/");
			File torrent_folder = new File("/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp/");
			System.out.println("Directory location for P2P: "+torrent_folder.toString());
			if (!torrent_folder.exists()) {
				if (torrent_folder.mkdir()) {
					//System.out.println(String.format("Directory created for P2P: %s ", torrent_folder.toString()));
				} else {
					System.out.println("Failed to create directory!");
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Invalid file");
		}
		
		//Make Folder for host
		try
		{
			//chunk_folder = new File(FILE_FOLDER+"/torrent_tmp/host/");
			chunk_folder = new File("/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/torrent_tmp/host/");
			System.out.println("Directory location for Host: "+chunk_folder.toString());
			if (!chunk_folder.exists()) {
				if (chunk_folder.mkdir()) {
					//System.out.println(String.format("Directory created for Host: %s ", chunk_folder.toString()));
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
			System.out.println(String.format("File broken at Host into %d chunks...", num_of_chunks));
			inputStream.close();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
