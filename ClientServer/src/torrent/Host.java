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
				File chunk_file = new File(chunk_folder.toString()+"/"+String.format("chunk_id=%d_host_%s", num_of_chunks,".chunk"));
				summary_local.put(new Integer(num_of_chunks), chunk_file.toString());
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
					Thread peer_thread = new Handler(listener.accept(), num_of_users, summary_local);
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
}
