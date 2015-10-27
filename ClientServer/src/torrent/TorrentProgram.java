package torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class TorrentProgram {

	private boolean DEBUG_MODE_ON = true;

	private String filename;
	public static String FILE_FOLDER;
	private int num_of_users;
	private static int num_of_chunks;
	private int size_of_chunks;
	private String BYTE_TYPE = "kB";
	ArrayList<Peer> peer_list = new ArrayList<Peer>();
	File local;
	Host host;
	Peer peer;

	public TorrentProgram()
	{
		if (DEBUG_MODE_ON)
		{
			this.filename = "/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/Non_Blondes_SONG.mp3";
			local = new File(this.filename);
			FILE_FOLDER = local.getParent();
			num_of_users = 5;
			size_of_chunks = 500; //kb
		}
		else
		{
			Scanner in = new Scanner(System.in);
			System.out.println("Enter full path to file: ");
			filename = in.next();
			local = new File(this.filename);
			FILE_FOLDER = local.getParent();
			System.out.println("Enter number of peers in network: ");
			num_of_users = in.nextInt();
			System.out.println(String.format("Enter size of chunks (%s): ",BYTE_TYPE));
			size_of_chunks = in.nextInt();
		}

	}
	
	public void start()
	{
		createHost();
		createPeers();
	}

	private void createPeers()
	{
		int peer_number = 0;
		for (int a = 0; a < num_of_users; a++)
		{
			peer_number = a;
			peer = new Peer(peer_number);
			peer.setPeerType("Download");
			Thread p = new Thread(peer);
			p.start();
			peer_list.add(peer);
			System.out.println(String.format("Peer %d was created", a));
		}
	}

	private void createHost()
	{
		try {
			host = new Host(filename, size_of_chunks, BYTE_TYPE, num_of_users);
			host.loadFile();
			Thread h = new Thread(host);
			h.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
