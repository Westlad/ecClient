package com.duncanwestland.ec.client.live;

import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * @author Duncan Westland
 * @version 2.0.1
 * Abstract class from which Data Group and elementary file classes are created
 * modified at 2.0.1 so that it does not rely on a LE=0 'read 256 bytes' command because 
 * some Javacard implementations don't work with that.
 */
public abstract class EF {
	private final byte START_BYTES = 8;
	private final int CHUNK_SIZE = 0xDF;
	protected byte[] efData;
	private CardChannel tcvr;
	private String SMFail="Secure Messaging MAC authentication failed";
	private byte[] select = {0x00,0x00}; 
	private SecureMessageAPDU SMapdu = new SecureMessageAPDU();
	Logger log = Logger.getLogger(EF.class.getName());
	
	protected void setTranceiver(CardChannel tcvr) {
		this.tcvr = tcvr;
	}
	
	/**
	 * Constructor
	 * @param select the Data Group to be selected - use the APDU class variables e.g. ADPU.DG1
	 * @param tcvr A cardchannel that is connect to the chip and has already established secure messaging
	 * @throws CardException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	protected EF(byte[] select, CardChannel tcvr) throws CardException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		this.select = select;
		this.tcvr = tcvr;
		load();
	}
	
	/**
	 * Selects a Data Group.  
	 * The select class variable must be set by the subclass's constructor so
	 * that this method selects the correct Data Group
	 * @throws CardException 
	 * @throws IOException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private void select() throws CardException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		//EFResponse resp = new EFResponse();
		ResponseAPDU resp;
		//calculate secure message APDU to select EF
		CommandAPDU protectedAPDU = SMapdu.assemble(APDU.select(select),APDU.Mode.SELECT);
		Crypto.logger("select DG", protectedAPDU.getBytes());
		//transmit and check response
		resp = tcvr.transmit(protectedAPDU);
		if (!SMapdu.check(resp.getData())) throw new IOException(SMFail);
		else log.info("EF select check OK");
	}
	/**
	 * Selects and reads a Data Group
	 * @param data
	 * @throws IOException
	 * @throws CardException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	protected void load() throws IOException, CardException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		//TODO I wrote a better version of this that copes with cards that don't seem to 
		// understand LE=0.  Need to replace this routine with that one
		select(); //select the ef
		//read first bytes of EF
		ResponseAPDU resp;
		CommandAPDU protectedAPDU = SMapdu.assemble(APDU.read((byte)0x00,(byte)0x00,START_BYTES),APDU.Mode.READ);
		resp = tcvr.transmit(protectedAPDU);
		if (!SMapdu.check(resp.getData())) throw new IOException(SMFail);
		byte[] data = SMapdu.decryptDO87();
		Crypto.logger("EF Response data", data);
		
		//Read the rest of the EF
		int numBit = data[1] & 0x80;
		int len;
		if (numBit==0x80) len = ((((int)data[2]&0xFF)<<8) | ((int)data[3]&0xFF)) + 4;
		else len = ((int)data[1]&0xFF) + 2;

		int lenLeft = len - START_BYTES; //remaining length to read
		int chunks = lenLeft/CHUNK_SIZE; //number of chunks to read it in (could be 0)
		int finalChunk = lenLeft - chunks*CHUNK_SIZE;
		byte[] msbLsb;
		int i;
		for (i=0;i<chunks;i++){
			msbLsb = offset(i,CHUNK_SIZE); //calculate the offset to apply
			protectedAPDU = SMapdu.assemble(APDU.read(msbLsb[0],msbLsb[1],(byte)CHUNK_SIZE),APDU.Mode.READ);
			resp = tcvr.transmit(protectedAPDU);
			if (!SMapdu.check(resp.getData())) throw new IOException(SMFail);
			data = Crypto.concat(data, SMapdu.decryptDO87());
			//Log.d(DEBUG, "chunk " + i);
		}
		//Log.d(DEBUG, "chunks read; reading final");
		msbLsb = offset(i,CHUNK_SIZE);
		protectedAPDU = SMapdu.assemble(APDU.read(msbLsb[0],msbLsb[1],(byte) finalChunk),APDU.Mode.READ);
		resp = tcvr.transmit(protectedAPDU);
		if (!SMapdu.check(resp.getData()))  throw new IOException(SMFail);
		data = Crypto.concat(data, SMapdu.decryptDO87());
		//Log.d(DEBUG, "final length "+ i + data.length);
		efData = data;
	}

	private byte[] offset(int j, int size) {
		long off = j*size+START_BYTES;
		byte msb = (byte)((off >>> 8) & 0xFF);
		byte lsb = (byte)(off & 0xFF);
		return new byte[] {msb,lsb};
	}
	/**
	 * @throws NoSuchAlgorithmException 
	 * Generates a sha256 hash of the datagroup bytes
	 * @return
	 * @throws  
	 */
	protected byte[] getHash() throws NoSuchAlgorithmException {
		MessageDigest hash = null;
		hash = MessageDigest.getInstance("SHA256");
		hash.update(efData);
		byte[] H = hash.digest();
		return H;
	}
	protected byte[] getEncoded() {
		return efData;
	}
}
