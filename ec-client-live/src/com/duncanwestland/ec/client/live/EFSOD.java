package com.duncanwestland.ec.client.live;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.*;
/**
 * @author Duncan Westland
 * @version 2.0.0
 * A class representing the ICAO ef.sod elementary file
 * It is intended to be created from within a BRPReader class
 */
class EFSOD extends EF {
	/**
	 * Constructor.
	 * Loads the ef.sod data from a chip to which secure messaging has been established.  
	 * @param tcvr the card channel connected to the card and with secure messaging established
	 * @throws CardException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public EFSOD(CardChannel tcvr) throws CardException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		super(APDU.EFSOD,tcvr);
	}
	/**
	 * returns the bytes representing ef.sod with the
	 * initial tag and length bytes stripped off
	 */
	@Override
	protected byte[] getEncoded() {
		if (this.efData[0]!=0x77) return null;
		int l = efData.length;
		byte[] encoded;
		switch (efData[1]) {
			case (byte) 0x82:
				encoded = Arrays.copyOfRange(efData, 4, l);
				break;
			case (byte) 0x81:
				encoded = Arrays.copyOfRange(efData, 3, l);
				break;				
			default:
				encoded = Arrays.copyOfRange(efData, 2, l);
			}
		return encoded;
	}
	

}
