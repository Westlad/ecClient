package com.duncanwestland.ec.client.live;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * A collection of static convenience functions that help with the cryptography
 * and associated chip-related functions
 */
public class Crypto {

	  public static final byte[] KEY_TYPE_ENC = {0x00,0x00,0x00,0x01};
	  public static final byte[] KEY_TYPE_MAC = {0x00,0x00,0x00,0x02};
	  private static final String DES3_ENCRYPTION_KEY_TYPE = "DESede";
	  private static final String DES_ENCRYPTION_KEY_TYPE = "DES";
	  private static final String DES3_CBC = "DESede/CBC/NoPadding";
	  private static final String DES_CBC = "DES/CBC/NoPadding";
	  private static final int DES_KEY_LENGTH = 24;
	  private static Logger log = Logger.getLogger(Crypto.class.getName());
	  static{
		  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		  log.info("security provider BC set");
	  }
	  
	 /** 
	 * concatenates two byte arrays.  Amazingly this isn't a standard Java function
	 * @param A
	 * @param B
	 * @return concatenation of A and B
	 */
	public static byte[] concat(byte[] A, byte[] B) {
		if (A==null & B==null) return null;
		else if (A==null) return B;
		else if (B==null) return A;
		else{
			byte[] C= new byte[A.length+B.length];
			System.arraycopy(A, 0, C, 0, A.length);
			System.arraycopy(B, 0, C, A.length, B.length);
			return C;
		}
	}		

	 /**
	 * Makes ICAO BAC keys from a  key seed
	 * @param Kseed the key seed
	 * @param c parameter that determines if the key returned is a mac or enc key
	 * (see ICAO 9303 for details) it can be one of Crypto.KEY_TYPE_ENC or Crypto.KEY_TYPE_MAC
	 * @return the BAC key (kenc or kmac)
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] makeKeys(byte[] Kseed,byte[] c) throws NoSuchAlgorithmException {
		byte [] D = concat(Kseed,c);
		byte [] H = hash(D);
		byte[] Ka = Arrays.copyOf(H,8);
		byte[] Kb = Arrays.copyOfRange(H, 8, 16);
		Ka = desParity(Ka);
		Kb = desParity(Kb);
		byte[] Knew = concat(Ka,Kb);
		return Knew;
	}
	/** 
	 * Adjusts the parity of a byte array to form a DES key
	 * @param k
	 * @return
	 */
	private static byte[] desParity(byte[] k) {
		byte[] Knew=null;
		for (byte b: k) {
		    byte mask = 0x01;
		    boolean parity = true;
		    for (byte bit=0; bit<8; bit ++) {
		    	if ((byte)(mask & b) != 0) parity = ! parity;
		    	mask = (byte)(mask << 1); //the 0xFF just make sure the shift works correctly as the int is representing an unsigned byte
		    }		
		    if (parity) b = (byte)(b ^ 0x01);
		    Knew = Crypto.concat(Knew, new byte[]{b}); 
		}
		return Knew;
	}
	/**
	 * Computes the sha 1 hash of a byte array; provides a simple interface
	 * @param data
	 * @return the sha1 hash
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] hash(byte[] data) throws NoSuchAlgorithmException{
		byte[] H = null;
		MessageDigest hash = MessageDigest.getInstance("SHA");
		hash.update(data);
		H = hash.digest();		
		return H;
	}

	/**
	 * Perform DES3 encryption or decryption
	 * @param original
	 * @param mode
	 * @param key
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] DES3(byte[] original, int mode, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		//ICAO assumes 2key 3DES  (key1 =key3) but Java needs 3 key 3DES
		//very explicit error catching for ease of debugging
		byte[] threeKey = new byte[DES_KEY_LENGTH];
		int j,k;
		for (j=0;j<16;j++) threeKey[j]= key[j];
		for (j=0, k=16; j<8;){
			threeKey[k++] = threeKey[j++];
		}
	    SecretKeySpec keySpec = new SecretKeySpec(threeKey, DES3_ENCRYPTION_KEY_TYPE);
		Cipher cipher = null;
		cipher = Cipher.getInstance(DES3_CBC);
		IvParameterSpec iv = new IvParameterSpec(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
		cipher.init(mode, keySpec, iv);
		return cipher.doFinal(original);
	}
	/**
	 * Performs DES encryption or decryption (depending on the mode)
	 * It's not intended to be called directly, but is used by other methods in this class
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * Perform DES encryption or decryption
	 * @param original
	 * @param mode (cipher.ENCRYPT_MODE), cipher.DECRYPT_MODE
	 * @return
	 * @throws GeneralSecurityException 
	 * @throws  
	 */
	private static byte[] DES(byte[] original, int mode,String method, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
	    SecretKeySpec keySpec = new SecretKeySpec(key, DES_ENCRYPTION_KEY_TYPE);
		Cipher cipher = null;
		cipher = Cipher.getInstance(method);
		IvParameterSpec iv = new IvParameterSpec(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
		cipher.init(mode, keySpec, iv);
		return cipher.doFinal(original);
	}
	/**
	 * calculates an ISO retail MAC
	 * @param k the MAC key
	 * @param message the message over which to calculate the MAC
	 * @return the MAC
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public static byte[] MAC(byte[] k,byte[] message) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		
		message = pad(message); 
		byte[] ka = Arrays.copyOfRange(k, 0, 8);
		byte[] kb = Arrays.copyOfRange(k, 8, k.length);
		byte[] cipher = null;
		cipher = DES(message, Cipher.ENCRYPT_MODE,Crypto.DES_CBC,ka);
		cipher = Arrays.copyOfRange(cipher, cipher.length-8, cipher.length);
		//Log.v(DEBUG,"cipher "+ hex(cipher));
		byte[] code = DES(cipher,Cipher.DECRYPT_MODE,Crypto.DES_CBC,kb);
		//Log.v(DEBUG,"code "+ hex(code));
		cipher = DES(code,Cipher.ENCRYPT_MODE,Crypto.DES_CBC,ka);
		//Log.v(DEBUG,"cipher "+ hex(cipher));
		return cipher;
	}
	
	/**
	 * pads message according to ISO/IEC9797-1 method 2
	 * @param message the message to be padded
	 * @return the padded message
	 */
	public static byte[] pad(byte[] message){
		byte[] pad = {-0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		//if (message.length %8 == 0) return message; //no padding needed!
		byte[] paddedMessage = Crypto.concat(message,pad);
		//TODO this is an inefficient way to cut down the message
		while (paddedMessage.length  %8 != 0) paddedMessage = Arrays.copyOfRange(paddedMessage, 0, paddedMessage.length-1);
		return paddedMessage;
	}
	
	/**
	 * returns a hexadecimal string representation of a byte array.
	 * @param b the byte array
	 * @return the hex string representation
	 */
	public static String hex(byte[] b){
		final int MAX_LENGTH = 100;
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
			if (result.length()>MAX_LENGTH) break;
		}
		return result;
	} 
	
	/**
	 * Convenience function to log a byte array as a hex string
	 * @param var A description string that is put in the log in front of the hex
	 * @param ret The byte array to be logged
	 */
	public static void logger(String var, byte[]ret) {
		log.info(var +" = " + Crypto.hex(ret));
	}

	/**
	 * This works out the ICAO check digit
	 * @param digits a byte array containing the ascii values of the digits for which the
	 * check digit is to be calculated.
	 * @return the check digit (as an ascii value)
	 */
	static byte chk(byte[] digits) {
		HashMap<Character,Integer> l = new HashMap<Character,Integer>();
		Integer checkDigit = 0;
		String checkStr;
		byte check;
		l.put('0',0);
		l.put('1',1);
		l.put('2',2);
		l.put('3',3);
		l.put('3',3);
		l.put('4',4);
		l.put('5',5);
		l.put('6',6);
		l.put('7',7);
		l.put('8',8);
		l.put('9',9);
		l.put('A',10);
		l.put('B',11);
		l.put('C',12);
		l.put('D',13);
		l.put('E',14);
		l.put('F',15);
		l.put('G',16);
		l.put('H',17);
		l.put('I',18);
		l.put('J',19);
		l.put('K',20);
		l.put('L',21);
		l.put('M',22);
		l.put('N',23);
		l.put('O',24);
		l.put('P',25);
		l.put('Q',26);
		l.put('R',27);
		l.put('S',28);
		l.put('T',29);
		l.put('U',30);
		l.put('V',31);
		l.put('W',32);
		l.put('X',33);
		l.put('Y',34);
		l.put('Z',35);
		l.put('<',0);
		int i = 0;
		final int[] m = {7,3,1};
		int n;
		for (byte b: digits) {
			n = l.get((char)b);
			n*=m[i];
			i++;
			if (i==3) i=0;
			checkDigit +=n;
		}
		checkDigit = checkDigit %10;
		log.info("Check digit is ="+checkDigit);
		checkStr = checkDigit.toString();
		check = checkStr.getBytes()[0];
		return check;
	}

	/**
	 * format a date string which is of the form dd-mm-yyyy and put it in a byte array
	 * containing the ascii values of the date in ICAO format (yymmdd)
	 * @param date The date string to be formatted
	 * @return
	 */
	private static byte[] dateToBytes(String dateIn) {
		byte[] dateBytes;
		String date;
		date = dateIn.substring(8, 10) + dateIn.substring(3, 5) + dateIn.substring(0, 2);
		dateBytes = date.getBytes();
		return dateBytes;
	}
	/** 
	 * Calculates a byte array which contains the MRZ information for generating the BAC keys
	 * @return MrzInformation
	 */
	public static byte[] getMrzInfo(String passportNumber, 
			String dateOfBirth, String dateOfExpiry) {
		byte[] MrzInformation;
		byte[] doe;
		byte[] dob;
		byte[] doc;
		dob = Crypto.dateToBytes(dateOfBirth);
		dob = concat(dob,new byte[] {Crypto.chk(dob)});
		doe = Crypto.dateToBytes(dateOfExpiry);
		doe = concat(doe,new byte[] {Crypto.chk(doe)});
		doc = passportNumber.getBytes();
		doc = concat(doc,new byte[] {Crypto.chk(doc)});
		MrzInformation = Crypto.concat(doc,dob);
		MrzInformation = Crypto.concat(MrzInformation,doe);
		return MrzInformation;
	}
}
