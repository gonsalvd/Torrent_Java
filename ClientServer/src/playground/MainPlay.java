package playground;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPlay {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int debug = 1;
		switch (debug)
		{

		case 0: 
			//Test Mapping
			Map<Integer, String> map1 = new HashMap<Integer, String>();
			Map<Integer, String> map2 = new HashMap<Integer, String>();

			map1.put(1, "drew");
			map1.put(3, "cathy");

			map2.put(2, "steve");
			map2.put(3, "steve2");

			Map<Integer, String> mapdiff = new HashMap<Integer, String>();

			map2.keySet().removeAll(map1.keySet());

			System.out.println(map2);
			System.out.println(map2.size());
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


		}
	}
}
