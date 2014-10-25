package com.duncanwestland.ec.client.live;

import javax.smartcardio.*;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * A class containing static convenience methods that return particular pre-formatted
 * CommandAPDUs that are useful for talking to a chipped document.
 */
/**
 * @author duncan
 *
 */
/**
 * @author duncan
 *
 */
public class APDU {
	
	/**
	 * This enum is used to indicate whether a read or a select command is being created
	 */
	public static enum Mode {SELECT,READ};
	/**
	 * byte code for selecting ef.com file
	 */
	public static byte[] EFCOM = {0x01,0x1E};
	/**
	 * byte code for selecting Data Group 1
	 */
	public static byte[] DG1 = {0x01,0x01};
	/**
	 * byte code for selecting Data Group 2
	 */
	public static byte[] DG2 = {0x01,0x02};
	/**
	 * byte code for selecting ef.sod
	 */
	public static byte[] EFSOD = {0x01,0x1D};
	
	/**
	 * Requests a random number from the chip
	 * @return
	 */
	public static CommandAPDU reqRNDICC() {
		byte[] ret = {0x00,-0x7C,0x00,0x00,0x08};
		return new CommandAPDU(ret);
	}
	
	/**
	 * Mutual Authenticate command.
	 * @param cmdData mutual authentication data see ICAO9303 for details
	 * @return
	 */
	public static CommandAPDU mutualAuthenticate(byte[] cmdData) {
		byte[] b1 = {0x00,-0x7E,0x00,0x00,0x28};
		byte[] b2 = {0x28};
		byte[] ret = new byte[b1.length+cmdData.length+b2.length];
		ret = Crypto.concat(b1, cmdData);
		ret = Crypto.concat(ret, b2);
		return new CommandAPDU(ret);
	}
	
	/**
	 * Command to select the ePassport application on the chip
	 * @return
	 */
	public static CommandAPDU selectIssuerApplication() {
		byte[] ret={0x00,-0x5C,0x04,0X0C,0x07,-0x60,0x00,0x00,0x02,0x47,0x10,0x01};
		return new CommandAPDU(ret);
	}
	
	/**
	 * Command to select a Data Group or elementary file
	 * @param fileID the identifier of the relevant datagroup (the static fields defined
	 * in this class can be used for the more popular Data Groups and elementary files
	 * @return
	 */
	public static CommandAPDU select(byte[] fileID) {
		byte[] b1 = {0x00,-0x5C,0x02,0x0C,0x02};
		byte[] ret = Crypto.concat(b1,fileID);
		return new CommandAPDU(ret);
	}
	/**
	 * Command to read a sequence of bytes from a Data Group of elementary file.
	 * @param offsetMSB offset from start of file (MSB)
	 * @param offsetLSB offset from start of file (LSB)
	 * @param bytes number of bytes to read (<=255)
	 * @return
	 */
	public static CommandAPDU read(byte offsetMSB, byte offsetLSB, byte bytes) {
		byte[] b1 = {0x00,-0x50};
		byte[] ret = Crypto.concat(b1, new byte[] {offsetMSB});
		ret = Crypto.concat(ret, new byte[] {offsetLSB});
		ret = Crypto.concat(ret, new byte[] {bytes});
		return new CommandAPDU(ret);
	}
}
