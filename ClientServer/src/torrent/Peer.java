package torrent;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Peer implements Runnable
{
	
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	
	Map<Integer, File> summary_local = new HashMap<Integer, File>();
	Map<Integer, File> summary_diff = new HashMap<Integer, File>();

	//keySet() compares keys, entrySet() both keys and values, values() only values
	//key=chunk ID, values=local file location
	void compareSummary(Map<Integer, File> summary_in)
	{
		//Summary_diff is the map that has all the keys local needs from upload (summary_in) side
		summary_diff = summary_in;
		summary_diff.keySet().removeAll(summary_local.keySet());
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
	
	//Summary_diff_received is the requested list of files the downloader needs your to upload
	void sendDownloadFiles(Set <Integer> summary_diff_received)
	{
		Set <Integer> files_to_upload = summary_diff_received;
		
		System.out.println("Chunk ID List received by uploader...");
		for (Integer chunk_id : files_to_upload) {
		  //Send file to downloader
			System.out.println(String.format("Sent chunk ID: %d to Peer ", chunk_id.toString()));  
		}
	}
	
	void receiveUploadFiles(Set <Integer> received)
	{
		int chunk_id=1;
		try {
			File received_file = File.createTempFile("chunk_id=1_peer_1",".tmp");
			summary_local.put(chunk_id, received_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	public void Peer() 
	{

	}

	public void run()
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket("localhost", 8000);
			System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			while(true)
			{
				System.out.print("Hello, please input a sentence: ");
				//read a sentence from the standard input
				message = bufferedReader.readLine();
				//Send the sentence to the server
				sendMessage(message);
				//Receive the upperCase sentence from the server
				MESSAGE = (String)in.readObject();
				//show the message to the user
				System.out.println("Receive message: " + MESSAGE);
			}
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
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

}
