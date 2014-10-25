package com.duncanwestland.ec.client.live;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.smartcardio.*;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * class to make Secure Messaging APDUs from ordinary APDUs
 * @author duncan
 *
 */
public class SecureMessageAPDU {
	static byte[] KSmac;
	static byte[] KSenc;
	static byte[] SSC;
	byte[] DO87;
	byte[] DO97;
	byte[] DO99;
	byte[] DO8E;
	Logger log  = Logger.getLogger(SecureMessageAPDU.class.getName());
	/**
	 * input a key and create and encryption object
	 * @return
	 */
	public void setKeys(byte[] KSenc,byte[] KSmac) {
			SecureMessageAPDU.KSmac = KSmac;
			SecureMessageAPDU.KSenc = KSenc;
	}
	public void startSSC(byte[] SSC){ //initialise the send sequence counter
			SecureMessageAPDU.SSC = SSC;
	}
	
	public void incSSC() {
		int SSCInt[] = new int[SSC.length];
		int i;
		for (i=0; i<SSCInt.length; i++) {
			SSCInt[i] = 0xFF & (int)SSC[i]; //convert to unsigned byte stored in int
		}
		SSCInt[SSCInt.length-1]++; //add 1 to the send sequence counter
		for (i=SSCInt.length-1;i>0;i--){
			if (SSCInt[i] == 0x100){
				SSCInt[i] = 0;
				SSCInt[i-1]++;
			}
		}
		if (SSCInt[0] == 0x100)	SSCInt[0] = 0; //#roll the counter
		for (i=0;i<SSC.length; i++) {
			SSC[i] = (byte)SSCInt[i];
		}
	}
	/**
	 * creates the Secure Messaging APDU
	 * @param apdu
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public CommandAPDU assemble(CommandAPDU cmdAPDU,APDU.Mode mode) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		// assign convenient descriptors to the apdu bytes
		//a ComandADPU can do all of the below but seeing as I've written it
		//already and it works, I might as well reuse it
		
		byte[] apdu = cmdAPDU.getBytes();
		
		byte CLA = apdu[0];
		byte INS = apdu[1];
		byte P1 = apdu[2];
		byte P2 = apdu[3];
		byte LC;
		byte[] data = new byte[2];
		byte LE=0;
	
		if (mode == APDU.Mode.READ) LE = apdu[4];
		if (mode == APDU.Mode.SELECT) {
			LC = apdu[4];
			if (LC!=0x02) {log.severe("command length is not 2"); System.exit(LC);}
			data = Arrays.copyOfRange(apdu,5,7);
		}
		CLA = (byte) (CLA | 0x0C); //mask the class byte -does this work?? CLA doens't affect cmdheader
		byte[] cmdHeader = {CLA,INS,P1,P2};
		cmdHeader = Crypto.pad(cmdHeader);
		//Crypto.logger("cmdHeader", cmdHeader);
		if (mode==APDU.Mode.SELECT) {  //create a DO87 data object
			byte[] paddedData = Crypto.pad(data);
			byte[] encryptedData = Crypto.DES3(paddedData, Cipher.ENCRYPT_MODE, KSenc);
			byte[] d = Crypto.concat(new byte[] {0x01}, encryptedData);
			byte[] tmp = Crypto.concat(new byte[] {-0x79}, new byte[] {(byte) d.length});
			DO87 = Crypto.concat(tmp, d);
			if (d.length>128) { 
				log.severe("d is too big to fit in a byte");
				System.exit(d.length);
			}
		}
		if (mode==APDU.Mode.READ){ //create a DO97 object note - LE can be 0x00
			byte[] tmp = {-0x69,0x01};
			DO97 = Crypto.concat(tmp,new byte[] {LE});
		}
		byte[] M = cmdHeader;
		if (mode==APDU.Mode.SELECT) M = Crypto.concat(M,DO87); //if there's a DO87
		if (mode==APDU.Mode.READ)	M = Crypto.concat(M,DO97);
		//add 1 to the SSC
		incSSC();
		byte[] N = Crypto.concat(SSC, M); //padding gets added in the MAC subroutine - so this isn't quite the same as ICAO's N
		byte[] CC = Crypto.MAC(KSmac,N);
		byte[] tmp = {-0x72,0x08};
		DO8E = Crypto.concat(tmp,CC);
		byte[] cmd = {CLA,INS,P1,P2};
		byte[] message=null;
		if (mode==APDU.Mode.SELECT) message = DO87;
		if (mode==APDU.Mode.READ) message = DO97;
		message = Crypto.concat(message,DO8E);
		tmp = Crypto.concat(cmd, new byte[] {(byte) message.length});
		tmp = Crypto.concat(tmp, message);
		tmp = Crypto.concat(tmp, new byte[] {0x00});
		return new CommandAPDU(tmp);
	}
	/**
	 * checks the MAC of a response to a protectedAPDU request
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public boolean check(byte[] resp) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		int len=0;
		//byte[] DO87;
		if (resp.length==0){ //no response - this isn't good
			log.severe("response is empty");
			System.exit(1);
		}
		incSSC();
		byte[] K = SSC;
		if (resp[0] == -0x79){  //DO87 present (is always first)
			if (resp[1] == -0x7E) len = ((((int)resp[2] & 0xFF)<<8) | ((int)resp[3]&0xFF)) + 4; //indicates that the length of DO87 is in two bytes
			else if (resp[1] == -0x7F) len = ((int)resp[2] & 0xFF) + 3; //my passport returns an 81 on the final read of DG2 don't know why
			else len = ((int)resp[1] & 0xFF)+2;
			//Log.d("crypto", "len"+String.valueOf(len));
			DO87 = Arrays.copyOfRange(resp, 0, len);
			K = Crypto.concat(K, DO87);
			if (resp[len] == -0x67){
				DO99 = Arrays.copyOfRange(resp, len, len+4);
				len += 4;
				K = Crypto.concat(K, DO99);
			}
		}
		if (resp[0] == -0x67) { //DO99 present (is first if there is no DO99): -0x67 -> 0x99
			DO99 = Arrays.copyOfRange(resp, 0, 4);
			len = 4;
			K = Crypto.concat(K,DO99);
		}
		if (resp[len] == -0x72){ //DO8E present (follows 87 and/or 99): -0x72 -> 0x8E
			DO8E = Arrays.copyOfRange(resp, len+2, len+10);
		}
		byte[] CC = Crypto.MAC(KSmac,K);
		if (DO8E.length != CC.length){
			log.severe("DO8E and CC are different lengths");
			Crypto.logger("CC", CC);
			Crypto.logger("DO8E",DO8E);
			Crypto.logger("resp substring", Arrays.copyOfRange(resp, 0, len+1));
			return false;
		}
		// check of DO8E and CC are the same (the .equals method seems to compare the object ref!
		for (int i=0;i<DO8E.length;i++){
			if (DO8E[i]!=CC[i]){
				log.severe("crypto checksum failed");
				return false;
			}
		}
		return true;
	}
		/**
		 * decodes DO87
		 * @return
		 * @throws BadPaddingException 
		 * @throws IllegalBlockSizeException 
		 * @throws InvalidAlgorithmParameterException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 */
		public byte[] decryptDO87() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
			byte[] data;
			int i;
			if (DO87[1] == -0x7E) data = Arrays.copyOfRange(DO87, 5, DO87.length); //this is a string longer than 256 bytes
			else if (DO87[1] == -0x7F) data = Arrays.copyOfRange(DO87, 4, DO87.length);
			else data = Arrays.copyOfRange(DO87, 3, DO87.length);
			byte[] decrypt = Crypto.DES3(data, Cipher.DECRYPT_MODE, KSenc);
			//strip the padding
			for (i=decrypt.length-1;i>=0; i--) if (decrypt[i] !=0) break;
			if (decrypt[i]!=-0x80) return decrypt;
			return Arrays.copyOfRange(decrypt, 0, i);
		}
}
