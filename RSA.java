import java.lang.StringBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
import java.util.Base64.Encoder;
import java.awt.*;
import java.io.*;

public class RSA {
	private static final BigInteger e = new BigInteger("65537");		


	public static void main(String []args) {
		int i;
		if(args.length < 1)
			exit();
		else if(args.length == 1 && args[0].equals("-h"))
		{
			System.out.println("Usage:\njava RSA -h\njava RSA -k <key-file> -b <bit-size>");
			System.out.println("java RSA -e <key-file>.public -i <input-file> -o <output-file>");
			System.out.println("java RSA -d <key-file>.private -i <input-file> -o <output-file>");
			System.exit(0);
		}

		else if(args.length == 4 || args.length == 2)
		{
			String[] arguments = {null, "1024"};
			for(i = 0; i < args.length; i += 2)
			{
				if(args[i].equals("-k"))
					arguments[0] = args[i+1];
				else if(args[i].equals("-b"))
					arguments[1] = args[i+1];
			}
			
			if(arguments[0] == null)
				exit();
			else generateKey(arguments);

		} else if(args.length == 6)
		{
			boolean encrypt = false;
			String[] arguments = {null, null, null};
			for(i = 0; i < 6; i += 2)
			{
				if(args[i].equals("-e"))
				{
					encrypt = true;
					arguments[0] = args[i+1];
				} else if(args[i].equals("-d"))
					arguments[0] = args[i+1];
				 else if(args[i].equals("-i"))
					arguments[1] = args[i+1];
				else if(args[i].equals("-o"))
					arguments[2] = args[i+1];
			}

			for(String el : arguments)
				if(el == null)
					exit();

			if(encrypt)
				encrypt(arguments);
			else decrypt(arguments);
		}

		else exit();
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	This is used whenever a bad argument is encountered
	 |
	 |	Pre-Cond:	n/a
	 |
	 |	Post-Cond:	The program is exited succesfully
	 |
	 |	Parameters:	n/a
	 |
	 |	Returns:	void
	 |
	 `-------------------------------------------------------------------------------------*/
	static void exit() {
		System.out.println("Type 'java RSA -h' for help\n");
		System.exit(1);
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	this handles encoding/decoding a String with RSA
	 |
	 |	Pre-Cond:	msg is a proper size
	 |
	 |	Post-Cond:	No variables have been changed
	 |
	 |	Parameters:	msg:		This is the message that's being encoded, padded if need be
	 |				modulus:	This is the n in phi(n), or p * q (which are trivial large primes)
	 |				exp:		encrypt:
	 |								e, which is a global variable
	 |							decrypt:
	 |								d, which is the private key
	 |
	 |	Returns:	String:		the encrypted utf-8 string
	 |
	 `-------------------------------------------------------------------------------------*/
	static String cipher(String msg, BigInteger modulus, BigInteger exp) {
		BigInteger convertedMsg;
		// handling decryption differences
		convertedMsg = new BigInteger(msg.getBytes());

		//System.out.println(convertedMsg);
		String ret = convertedMsg.modPow(exp, modulus).toString();
		//System.out.println(ret);
		return ret;
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	this iterates through the textfile, getting all the proper sized
	 |				payloads. 
	 |
	 |	Pre-Cond:	the file exists, is a valid format, and that a command line file was 
	 |				provided
	 |
	 |	Post-Cond:	The message is put into properly sized blocks, with consideration for
	 |				the last one, padding being placed at the front of the block
	 |
	 |	Parameters:	fs:			the FileInputStream that is being read from, byte by byte
	 |				keyLength:	how many hex values are in the key
	 |
	 |	Returns:	ArrayList:	the ascii Strings that are read in
	 |
	 `-------------------------------------------------------------------------------------*/
	static ArrayList<String> getPayloads(FileInputStream fs, int keyLength) {
		ArrayList<String> payload = new ArrayList<String>();
		int listIndex = -1, strIndex = 0;
		char next;

		// this adds the proper payloads to the arraylist
		try 
		{
			while((next = (char)fs.read()) != 0xffff)
			{
				// keyLength accounts for hex(4 bits), but you are now reading chars(8 bits)
				if(strIndex % ((keyLength / 2) - 1) == 0)
				{
					listIndex += 1;
					payload.add("");
					strIndex = 0;
				}
				payload.set(listIndex, payload.get(listIndex) + next);
				strIndex ++;
			}
		} catch(IOException e) {};

		// padding on the last payload
		if(strIndex != 0)
			while(strIndex < (keyLength / 2))
			{
				payload.set(listIndex, '0' + payload.get(listIndex));
				strIndex ++;
			}

		return payload;
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	read the key from file
	 |
	 |	Pre-Cond:	the file is valid
	 |
	 |	Post-Cond:	the file has been read in and converted from hex to the String
	 |				representation
	 |
	 |	Parameters:	fs:	the FileInputStream for the key
	 |
	 |	Returns:	String:	the String representation of the hex value read in
	 |
	 `-------------------------------------------------------------------------------------*/
	static String readKey(FileInputStream fs) {
		StringBuilder keyBuilder = new StringBuilder();
		char next;

		try 
		{
			while((next = (char) fs.read()) != 0xffff)
				keyBuilder.append(next);
			
		} catch(IOException e) {};

		return keyBuilder.toString();
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	Reads in nibblets and converts them to ascii
	 |
	 |	Pre-Cond:	str has values 0-9, and an even number
	 |
	 |	Post-Cond:	the String has been converted to the ASCII equivalent
	 |
	 |	Parameters:	str:	The string containing nibblets
	 |
	 |	Returns:	String:	the String of ASCII equivalents to the bytes
	 |
	 `-------------------------------------------------------------------------------------*/
	static String convertToAscii(String str) {
		char[] conversion = new char[str.length() / 2];
		for(int i = 0; i < str.length() - 2; i += 2)
		{
			conversion[i / 2] = (char) Integer.parseInt(str.substring(i, i + 2));
		}

		return String.copyValueOf(conversion);
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	This is used to encrypt a message
	 |
	 |	Pre-Cond:	all values in args[] are proper
	 |
	 |	Post-Cond:	the files have been read, and the encryption has been written to file
	 |
	 |	Parameters:	args[]:	0:	The name of the key, without the .public
	 |						1:	The name of the msg file
	 |						2:	The name of the outputfile
	 |
	 |	Returns:	void
	 |
	 `-------------------------------------------------------------------------------------*/
	static void encrypt(String[] args) {
		// gets the key into a BigInteger
		FileInputStream pubKey_fs = openInputFile(args[0].concat(".public"));
		String key = readKey(pubKey_fs);
		BigInteger bigKey = new BigInteger(key, 16);

		// gets all the payloads into an ArrayList
		FileInputStream input_fs = openInputFile(args[1]);
		ArrayList<String> payload = getPayloads(input_fs, key.length());

		// writes the encoded text into the output file
		PrintWriter output_fw = openOutputFile(args[2]);
		for(String msg : payload)
		{
			String cipher = cipher(msg, bigKey, e);
			String utfCipher = convertToAscii(cipher);
			output_fw.write(utfCipher);
		}

		output_fw.close();
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	This is used to decrypt a cipher text
	 |
	 |	Pre-Cond:	all values in args[] are value
	 |
	 |	Post-Cond:	The cipher text has been converted to plaintext and written to file
	 |
	 |	Parameters:	args[]:	0:	the name of the users public and private key
	 |						1:	the name of the input file
	 |						2:	the name of the output file
	 |
	 |	Returns:	void
	 |
	 `-------------------------------------------------------------------------------------*/
	static void decrypt(String[] args) {
		// get the d from private key 
		FileInputStream privKey_fs = openInputFile(args[0].concat(".private"));
		String key = readKey(privKey_fs);
		BigInteger bigKey = new BigInteger(key, 16);

		// get the n from public key
		FileInputStream pubKey_fs = openInputFile(args[0].concat(".public"));
		String pubKey = readKey(pubKey_fs);
		BigInteger n = new BigInteger(pubKey, 16);

		// gets all the payloads into an ArrayList
		FileInputStream input_fs = openInputFile(args[1]);
		ArrayList<String> payload = getPayloads(input_fs, key.length());

		// writes the encoded text into the output file
		PrintWriter output_fw = openOutputFile(args[2]);
		for(String msg : payload)
		{
			String cipher = cipher(msg, n, bigKey);
			String utfCipher = convertToAscii(cipher);
			output_fw.write(utfCipher);
		}

		try{
			output_fw.close();
			input_fs.close();
		}
		catch(Exception e){}
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	generate a random long value
	 |
	 |	Pre-Cond:	MouseInfo is imported
	 |
	 |	Post-Cond:	nothing is changed
	 |
	 |	Parameters:	n/a
	 |
	 |	Returns:	long:	a random long
	 |
	 `-------------------------------------------------------------------------------------*/
	public static long generateLong() {
		long time = System.currentTimeMillis(), point, time2;
		Point pt = MouseInfo.getPointerInfo().getLocation();
		point = (long) (pt.getY() * (1 + pt.getY()));
		time *= time ^ point;

		if(time < Math.pow(3, 23))
			time += Math.pow(7, pt.getY());
	   	else if (time < Math.pow(3, 31))
			time += Math.pow(11, pt.getX());

		return time ^ System.currentTimeMillis();
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	opens a FileInputStream
	 |
	 |	Pre-Cond:	filename is valid
	 |
	 |	Post-Cond:	the file is opened
	 |
	 |	Parameters:	filename:	the string name to be opened
	 |
	 |	Returns:	FileInputStream:	a stream to read from
	 |
	 `-------------------------------------------------------------------------------------*/
	static FileInputStream openInputFile(String filename) {
		FileInputStream fs;

		try 
		{
			fs = new FileInputStream(new File(filename));
			return fs;
		} catch (IOException e)
		{
			System.out.println("Failed to open input file");
			exit();
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	opens an output stream
	 |
	 |	Pre-Cond:	filename is valid
	 |
	 |	Post-Cond:	the file is opened
	 |
	 |	Parameters:	filename:	the file to be opened
	 |
	 |	Returns:	PrintWriter:	a stream to write to
	 |
	 `-------------------------------------------------------------------------------------*/
	static PrintWriter openOutputFile(String filename) {
		PrintWriter fw;

		try 
		{
			fw = new PrintWriter(filename, "UTF-8");
			return fw;

		} catch (Exception e)
		{
			System.out.println("Failed to open output file");
			exit();
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------
	 |	Purpose:	generates a private and public key
	 |
	 |	Pre-Cond:	the args[] are valid
	 |
	 |	Post-Cond:	a .public and .private key have been written to
	 |
	 |	Parameters:	args[]:	0:	the name of the keys to be generated
	 |						1:	the number of bits the key has to be
	 |
	 |	Returns:	void
	 |
	 `-------------------------------------------------------------------------------------*/
	static void generateKey(String[] args) {
		Random rnd = new Random(generateLong());
		BigInteger public_key, totient, private_key,
		possiblePrime_q = BigInteger.probablePrime(Integer.parseInt(args[1]) / 2, rnd),
		possiblePrime_p = BigInteger.probablePrime(Integer.parseInt(args[1]) / 2, rnd);

		System.out.println("P: " + possiblePrime_p + "\nQ: " + possiblePrime_q);
		// file set up
		String pubf = args[0].concat(".public");
		String privf = args[0].concat(".private");
		PrintWriter pub_fw = openOutputFile(pubf);
		PrintWriter priv_fw = openOutputFile(privf);

		// calculate public key
		public_key = possiblePrime_q.multiply(possiblePrime_p);

		// calculate the private key
		totient = ((possiblePrime_p).subtract(BigInteger.ONE))
			.multiply((possiblePrime_q).subtract(BigInteger.ONE));
		private_key = public_key.modInverse(totient);
		System.out.println(private_key);

		// write them both to file and close
		pub_fw.write(public_key.toString(16));
		priv_fw.write(private_key.toString(16));
		pub_fw.close();
		priv_fw.close();
	}
}