package torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/*This is the program that you could say runs centrally and coordinates the start of Hosts and Peers
 * 
 */
public class TorrentProgram {

	private boolean DEBUG_MODE_ON = false;	//debugger
	private String fullPathname;	//filename to be broken into chunks
	public static String FILE_FOLDER;	//folder where file located. also where all host/peer chunks will be stored
	public static String FILENAME;	//filename
	public static int num_of_users;	//number of users (peers) in system
	private int size_of_chunks;	//size of chunks
	private String BYTE_TYPE = "kB";	//type of size of chunks
	private static ArrayList<Client> peer_list = new ArrayList<Client>(); //creates list of all peers created
	private File local; //manages local files
	private Server host; //host who holds initial file/chunks
	private Client peer;	//peers who will receive chunks/trade

	public TorrentProgram()
	{
		if (DEBUG_MODE_ON)
		{
			this.fullPathname = "/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/chord.pdf";
			local = new File(this.fullPathname);
			FILE_FOLDER = local.getParent();
			FILENAME = local.getName().toString();
			num_of_users = 3;
			size_of_chunks = 50; //kb
		}
		else
		{
			Scanner in = new Scanner(System.in);
			System.out.println("Enter full path to file: ");
			this.fullPathname = in.next();
			local = new File(this.fullPathname);
			FILE_FOLDER = local.getParent();
			FILENAME = local.getName().toString();
			System.out.println("Enter number of peers in network: ");
			num_of_users = in.nextInt();
			System.out.println(String.format("Enter size of chunks (%s): ",BYTE_TYPE));
			size_of_chunks = in.nextInt();
		}

	}

	//Make folder to store Host chunks
	private void makeFolder()
	{
		try
		{
			File torrent_folder = new File(TorrentProgram.FILE_FOLDER+"/torrent_tmp/");
			if (!torrent_folder.exists()) {
				if (torrent_folder.mkdir()) {
					System.out.println(String.format("Directory created for P2P: %s ", torrent_folder.toString()));
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

	//Main will enter program through here
	public void start()
	{
		System.out.println("Starting PEER-TO-PEER 'Circle' architecture system...");
		makeFolder();
		createHost();
		createPeers();
	}

	//Creates peers/starts their threads
	private void createPeers()
	{
		int peer_number = 0;
		for (int a = 0; a < num_of_users; a++)
		{
			peer_number = a;
			//peer = new Peer(peer_number);
			Thread p = new Thread(peer);
			p.start();
			peer_list.add(peer);
			System.out.println(String.format("Peer %d was created...", a));
		}
	}

	//Creates host/starts thread
	private void createHost()
	{
		try {
			host = new Server(fullPathname, size_of_chunks, BYTE_TYPE, num_of_users);
			host.loadFile();
			Thread h = new Thread(host);
			h.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
