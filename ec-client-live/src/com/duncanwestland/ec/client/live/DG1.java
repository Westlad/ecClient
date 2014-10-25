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

import java.util.logging.Logger;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * A class representing the ICAO DG1 datagroup
 * It is intended to be created from within a BRPReader class
 */
class DG1 extends EF {
	Logger log = Logger.getLogger(DG1.class.getName());
	/**
	 * Constructor.
	 * Loads the DG1 data from a chip to which secure messaging has been established.  
	 * @param tcvr the card channel connected to the card and with secure messaging established
	 * @throws CardException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOExceptionduncanduncan
	 */
	DG1(CardChannel tcvr) throws CardException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
		super(APDU.DG1,tcvr); //select and load data into DG1 object from chip
	}
	/**
	 * Decodes the data in DG1 and populates a
	 * BioData object that has been passed in
	 * needs to cope with 2 line and 3 line MRZ
	 * @param bd The BioData object
	 */
	void parseBioData(BioData bd){
		char p = (char)efData[5]; //select the doctype byte
		bd.setDocType(String.valueOf(p));
		switch(p) {
			case 'P':
				parseTwoLineBioData(bd);
				break;
			case 'I':
				parseThreeLineBioData(bd);
			default:
				parseThreeLineBioData(bd);
				log.warning("Unknown Document Type");
		}
		Character[] state = {(char)efData[7],(char)efData[8],(char)efData[9]};
		bd.setIssuingState(state.toString());
	}
	/**
	 * Decodes the data in a 2 line MRZ and populates BioData
	 * @param dg1Bytes
	 */
	public void parseTwoLineBioData(BioData bd){
		int i;
		byte[] dg1Bytes = Arrays.copyOfRange(efData, 5, efData.length);//strip the preamble from the MRZ data
		byte[] mrzBytes = Arrays.copyOfRange(dg1Bytes, 44, 53);
		char[] c = new char[mrzBytes.length];
		for (i=0;i<9;i++) c[i] = (char)mrzBytes[i];
		bd.setPassportNumber(clean(String.copyValueOf(c)));
		//log.info("BioData.passportNumber"+bd.getPassportNumber());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 57, 63);
		bd.setDateOfBirth(byteToDate(mrzBytes));
		//log.info("BioData.dateOfBirth"+bd.getDateOfBirth());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 65,71);
		bd.setDateOfExpiry(byteToDate(mrzBytes));
		//log.info("BioData.dateOfExpiry"+bd.getDateOfExpiry());
		bd.setSex(String.valueOf((char)Arrays.copyOfRange(dg1Bytes, 64, 65)[0])); //TODO not displayed at present
		//log.info("BioData.sex"+bd.getSex());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 5, 44);
		char[] n = new char[mrzBytes.length];
		for (i=0;i<38;i++) n[i] = (char)mrzBytes[i];
		bd.setName(clean(String.copyValueOf(n)));
		//log.info("BioData.name"+bd.getName());
	}
	/**
	 * Decodes the data in a 3 line MRZ and populates BioData
	 * @param dg1Bytes
	 */
	void parseThreeLineBioData(BioData bd){
		int i;
		byte[] dg1Bytes = Arrays.copyOfRange(efData, 5, efData.length);//strip the preamble from the MRZ data
		byte[] mrzBytes = Arrays.copyOfRange(dg1Bytes, 5, 14);
		char[] c = new char[mrzBytes.length];
		for (i=0;i<9;i++) c[i] = (char)mrzBytes[i];
		bd.setPassportNumber(clean(String.copyValueOf(c)));
		//log.info("BioData.passportNumber"+bd.getPassportNumber());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 30, 36);
		bd.setDateOfBirth(byteToDate(mrzBytes));
		//log.info("BioData.dateOfBirth"+bd.getDateOfBirth());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 38,44);
		bd.setDateOfExpiry(byteToDate(mrzBytes));
		//log.info("BioData.dateOfExpiry"+bd.getDateOfExpiry());
		bd.setSex(String.valueOf((char)Arrays.copyOfRange(dg1Bytes, 37, 38)[0])); //TODO not displayed at present
		//log.info("BioData.sex"+bd.getSex());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 60, 99);
		char[] n = new char[mrzBytes.length];
		for (i=0;i<38;i++) n[i] = (char)mrzBytes[i];
		bd.setName(clean(String.copyValueOf(n)));
		//log.info("BioData.name"+bd.getName());
	}
	private String clean(String bio) {
		String out;
		out = bio.replace('<', ' ');
		bio = out.trim();
		return bio;
	}
	private String byteToDate(byte[] dateBytes){
		char[] c = new char[dateBytes.length];
		for (int i=0;i<dateBytes.length;i++) c[i] = (char)dateBytes[i];
		String date = String.copyValueOf(c);
		String year = date.substring(0, 2);
		String month = date.substring(2, 4);
		String day = date.substring(4, 6);
		return day+"-"+month+"-"+year;
	}
}
