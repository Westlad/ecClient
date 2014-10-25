package com.duncanwestland.ec.client.live;


import icc.ICCProfileException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.smartcardio.*;

import colorspace.ColorSpaceException;
/**
 * @author Duncan Westland
 * @version 2.0.0
 * A class representing the ICAO DG2 datagroup
 * It is intended to be created from within a BRPReader class
 */
class DG2 extends EF{
	Logger log = Logger.getLogger(DG2.class.getName());
	/**
	 * Constructor.
	 * Loads the DG2 data from a chip to which secure messaging has been established.  
	 * @param tcvr the card channel connected to the card and with secure messaging established
	 * @throws CardException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	DG2(CardChannel tcvr) throws CardException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
		super(APDU.DG2, tcvr);
	}
	/**
	 * Decodes the data in DG1 and populates a
	 * BioData object that has been passed in
	 * @param bd The BioData object
	 */
	void parseBioData(BioData bd) throws IOException, ColorSpaceException, ICCProfileException {
		//extract biometric data
		byte[] data = efData;
		int posn = 0, len = 0;
		for (int i=0; i<data.length-1;i++){
			if (data[i]==0x5F & data[i+1]==0x2E){
				len = (((int)data[i+3]) & 0xFF)<<8 | ((int)data[i+4]&0xFF);
				posn = i+5;
				break;
			}
		}
		log.info("BDB starts at byte "+ posn + "length = "+len);
		byte[] BDB = Arrays.copyOfRange(data, posn, posn+len);
		//parse the biometric data block
		//byte[] facialRecordHeader = Arrays.copyOfRange(BDB,0,14);
		byte[] facialInformationBlock = Arrays.copyOfRange(BDB,14,34);
		int numberFeaturePointBlocks =((int)facialInformationBlock[4]&0xFF)<<8 | 
										((int)facialInformationBlock[5]&0xFF);
		int startImageInfo = numberFeaturePointBlocks*8 + 34;
		byte[] imageInfo = Arrays.copyOfRange(BDB,startImageInfo,startImageInfo+12);
		byte FaceImageType = imageInfo[0];
		byte imageDataType = imageInfo[1];
		int width = ((int)imageInfo[2]&0xFF)<<8 | ((int)imageInfo[3]&0xFF);
		int height = ((int)imageInfo[4]&0xFF)<<8 | ((int)imageInfo[5]&0xFF);
		//byte quality = imageInfo[6];
		log.info("FaceImageType = "+ FaceImageType + "imageDataType = " + imageDataType);
		log.info("width = " + width + "height = " + height);
		int startImageData = startImageInfo + 12;
		byte[] debug = Arrays.copyOfRange(BDB, startImageData, startImageData+50);
		Crypto.logger("*********************Start of Image Information", debug);
		byte[] jp = (Arrays.copyOfRange(BDB,startImageData, BDB.length));
		try {
		if (imageDataType == 1) { //a JPEG2000 image
			J2kStreamDecoder j2k = J2kStreamDecoder.getInstance();
			bd.setJpeg(j2k.decode(new ByteArrayInputStream(jp)));
		}
		if (imageDataType == 0) { //a standard jpeg
			bd.setJpeg(ImageIO.read(new ByteArrayInputStream(jp)));
		}
		} catch(IllegalArgumentException e) {
			bd.setJpeg(null);
			log.severe("Could not read image: "+e.getMessage());
		}
	}
}
