package playground;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPlay {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int debug = 0;
		switch (debug)
		{

		case 0: 
			//Test Mapping
			Map<Integer, String> map1 = new HashMap<Integer, String>();
			Map<Integer, String> map2 = new HashMap<Integer, String>();

			map1.put(1, "drew");
			map1.put(2, "drew");
			map1.put(3, "cathy");
			map1.put(4, "drew");

			map2.put(5, "steve");

			Map<Integer, String> mapdiff = new HashMap<Integer, String>();

			map1.keySet().removeAll(map2.keySet());

			System.out.println(map1);
			
			map2.put(1, "drew");
			
			map1.keySet().removeAll(map2.keySet());

			System.out.println(map1);
			
			map2.put(2, "drew");
			
			map1.keySet().removeAll(map2.keySet());
			
			map2.put(3, "drew");
			
			map1.keySet().removeAll(map2.keySet());
			
			map2.put(4, "drew");
			
			map1.keySet().removeAll(map2.keySet());
			
			System.out.println("Size of map1: "+map1.size());
			System.out.println("Size of map2: "+map2.size());
			
			HashMap<Integer, String> map3 = new HashMap<Integer, String>();

			map3.put(1, "drew");
			map3.put(2, "drew");
			map3.put(3, "cathy");
			map3.put(4, "drew");

			HashMap<Integer, String> map4 = new HashMap<Integer, String>(map3);
			
			System.out.println("Map4 shallow copy "+map4.entrySet().toString());
			
			map3.remove(2);

			System.out.println(map4.entrySet().toString());

			map4 = map3;
			System.out.println(map4.entrySet().toString());
			map3.remove(4);
			System.out.println(map4.entrySet().toString());
			
			Map<Integer, String> map5 = (HashMap) map4.clone();
			
			System.out.println(map5.entrySet().toString());
			
			map4.remove(1);
			
			System.out.println(map5.entrySet().toString());


			

			
			
			break;

		case 1:
			//Test break file
			String filename = "/Users/gonsalves-admin/Documents/School/CNT5106C-Network/Proj1/Non_Blondes_SONG.mp3";
			File inputFile = new File(filename);
			FileInputStream inputStream;

			String newFileName;
			FileOutputStream filePart;
			int fileSize = (int) inputFile.length();
			System.out.println(String.format("Size of file in bytes: %d", fileSize));

			//readlength = 500000 = 0.5MB = 500kB
			int nChunks = 0;
			int read = 0;
			int readLength = 500000;
			
			byte[] byteChunkPart;
			try 
			{
				inputStream = new FileInputStream(inputFile);
				while (fileSize > 0) 
				{
					byteChunkPart = new byte[readLength];
					//When you read from the inputstream then you are kind of decrementing it at the same time. Less is leftover. Original file remains intact.
					read = inputStream.read(byteChunkPart, 0, readLength);
					fileSize = fileSize - read;
					System.out.println(String.format("Filesize: %d", fileSize));
					assert (read == byteChunkPart.length);
					nChunks++;
					File received_file = File.createTempFile(String.format("chunk_id=%d_peer1_", nChunks-1),".chunk");
					System.out.println(String.format("Location of chunk file: %s", received_file.toString()));
					//					newFileName = filename + ".part" + Integer.toString(nChunks - 1);
					filePart = new FileOutputStream(received_file);
					filePart.write(byteChunkPart);
					filePart.flush();
					filePart.close();
					byteChunkPart = null;
					filePart = null;
				}
				inputStream.close();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
			break;
		case 2:
			boolean connected = true;
			System.out.println(!connected);
		case 3:
			System.out.println(Math.floorMod(-1, 5));

		}
	}
}
