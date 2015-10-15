package torrent;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class TorrentProgram {

	private boolean DEBUG_MODE_ON = true;

	private String filename;
	private File input_file;
	private int num_of_users;
	private static int num_of_chunks;

	ArrayList<Peer> peer_list = new ArrayList<Peer>();
	Host host;
	Peer peer;

	public TorrentProgram()
	{
		if (DEBUG_MODE_ON)
		{
			this.filename = "/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/Non_Blondes_SONG.mp3";
			num_of_users = 5;
			num_of_chunks = 10;
		}
		else
		{
			Scanner in = new Scanner(System.in);
			System.out.println("Enter full path to file: ");
			filename = in.next();
			System.out.println("Enter number of peers in network: ");
			num_of_users = in.nextInt();
			System.out.println("Enter number of chunks: ");
			num_of_chunks = in.nextInt();
		}

	}

	private void loadFile()
	{
		try
		{
			input_file = new File(this.filename);
		}
		catch (Exception e)
		{
			System.out.println("Invalid file");
		}
	}

	private void createPeers()
	{

		for (int a = 1; a <= num_of_users; a++)
		{
			//Either put a timer on this or wait until server is open
			peer = new Peer();
			peer_list.add(peer);
			System.out.println(String.format("Peer %d was created", a));
		}
	}

	public static int getNumChunks()
	{
		return num_of_chunks;
	}

	private void createHost()
	{
		try {
			host = new Host();
			Thread t = new Thread(host);
			t.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void distributeFile()
	{

	}

	private void breakFile()
	{

	}


	public void start()
	{
		loadFile();
		createHost();
		createPeers();
		breakFile();
		distributeFile();
	}

}
