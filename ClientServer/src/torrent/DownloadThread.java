package torrent;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class DownloadThread implements Runnable{

	Socket DownloadSocket;
	Socket UploadSocket;
	
	ObjectOutputStream download_out;
	ObjectInputStream download_in;
	ObjectOutputStream upload_out;
	ObjectInputStream upload_in;
	
	DownloadThread(Socket DownSocket, Socket UpSocket)
	{
		
	}
	
	public void run() {
		// TODO Auto-generated method stub
		try
		{
			download_out = new ObjectOutputStream(DownloadSocket.getOutputStream());
			upload_out = new ObjectOutputStream(UploadSocket.getOutputStream());
			download_in = new ObjectInputStream(DownloadSocket.getInputStream());
			upload_in = new ObjectInputStream(UploadSocket.getInputStream());
			
			//Initial download request for summary list
			
			//Initial upload peer sends summary list
			//Download side compares
			//Download side sends request to upload side
			//Upload side sends back requested chunks one by one
			//Download side receives all chunks
			
			//Download requests for summary list AGAIN
		} 		
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		}
		catch(UnknownHostException unknownHost){
		System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
		ioException.printStackTrace();
		}
		finally
		{
			
		}
	}

}
