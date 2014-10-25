package com.duncanwestland.ec.client.live;

import icc.ICCProfileException;

import javax.smartcardio.*;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import colorspace.ColorSpaceException;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * This class represents a card-reader
 * It handles all of the mutual authentication and it populates an object of
 * the BioData class with the data that it reads from a chip.  It requires a native
 * library for accessing the card to be installed.  For Windows this is automatic
 * For Linux libccid is required TODO - add more detail.
 */
public class BRPReader {
	private static Logger log = Logger
			.getLogger(BRPReader.class.getName());

	/**
	 * Method to read a chip.
	 * @param doc The document number (used for Basic Access Control)
	 * @param dob The date of birth in dd-mm-yyyy form (used for Basic Access Control)
	 * @param doe The date of expiry in dd-mm-yyyy form (used for Basic Access Control)
	 * @return a BioData object containing the data read from the card
	 * @throws NoSuchAlgorithmException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws IOException 
	 * @throws ICCProfileException 
	 * @throws ColorSpaceException 
	 */
	public BioData read(String doc, String dob, String doe) throws CardException, 
		NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, 
		InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, 
		IOException, ColorSpaceException, ICCProfileException {
		
		BioData bd = new BioData();
		// Read the passport's chip and output the biodata class
		final long WAIT_FOR_CHIP = 5000;
		log.info("starting passport reader");
		ResponseAPDU resp;
		// compute the hash from the MRZ information
		byte[] mrzInfoBytes = Crypto.getMrzInfo(doc, dob, doe);
		Crypto.logger("mrzInfoBytes", mrzInfoBytes);
		byte[] mrzHash = Crypto.hash(mrzInfoBytes);
		// compute the key seed then Kenc and Kmac
		byte[] kSeed = Arrays.copyOfRange(mrzHash, 0, 16); // 0,16
		byte[] Kenc = Crypto.makeKeys(kSeed, Crypto.KEY_TYPE_ENC);
		byte[] Kmac = Crypto.makeKeys(kSeed, Crypto.KEY_TYPE_MAC);
		//Crypto.logger("Kenc", Kenc);
		//Crypto.logger("Kmac", Kmac);

		// connect to the ppt
		// show the list of available terminals
		log.info("starting NFC");
		TerminalFactory factory = TerminalFactory.getDefault();
		CardTerminals terminals = factory.terminals();
		log.info("Terminals: " + terminals);
		//this is needed because ternminals.list() throws an error if there is no terminal connected
		try {
			@SuppressWarnings("unused")
			List<CardTerminal> terminalList = terminals.list();
		}
		catch (CardException e) {
			throw new CardException("no chip reader was found");
		}
		// see if one of the terminals has a card in it
		log.info("testing to see if a chip is already present");
		CardTerminal terminal = null;
		PassportFound: while (terminal == null) {
			for (CardTerminal testTerminal : terminals
					.list(CardTerminals.State.CARD_INSERTION)) {
				terminal = testTerminal; // grab a terminal with a passport in it
				break PassportFound;
			}
			//if not, wait until it does have a card in it
			log.info("no document currently, waiting for document");
			if (!terminals.waitForChange(WAIT_FOR_CHIP))
				throw new CardException("no chip was found");
		}
		// establish a connection with the card
		log.info("connecting to chip");
		Card passport = terminal.connect("*");
		log.info("chip connected: " + passport);
		CardChannel tcvr = new WrappedCardChannel(passport.getBasicChannel());

		// request RNDicc number from passport
		byte[] RNDicc;
		log.info("sending selectIssuerApplication");
		resp = tcvr.transmit(APDU.selectIssuerApplication());
		log.info("requesting reqRNDICC");
		resp = tcvr.transmit(APDU.reqRNDICC());
		RNDicc = resp.getData();

		// generate random numbers
		SecureRandom random = new SecureRandom();
		byte[] RNDifd = new byte[8];
		byte[] Kifd = new byte[16];
		random.nextBytes(RNDifd);
		random.nextBytes(Kifd);

		// Concatenate RND.IFD, RND.ICC and KIFD
		byte[] S = Crypto.concat(RNDifd, RNDicc);
		S = Crypto.concat(S, Kifd);

		// DES3 encryption of S
		byte[] Eifd = Crypto.DES3(S, Cipher.ENCRYPT_MODE, Kenc);

		// Retail MAC of EIFD
		byte[] Mifd = Crypto.MAC(Kmac, Eifd);

		// Construct command for MUTUAL AUTHENTICATE and send
		byte[] cmdData = Crypto.concat(Eifd, Mifd);
		log.info("sending mutualAuthenticate command");
		resp = tcvr.transmit(APDU.mutualAuthenticate(cmdData));

		// decrypt and verify the response
		int split = resp.getData().length - 8;
		byte[] Micc = Arrays.copyOfRange(resp.getData(), split,
				resp.getData().length);
		byte[] Eicc = Arrays.copyOfRange(resp.getData(), 0, split);
		byte[] MiccCheck = Crypto.MAC(Kmac, Eicc);
		if (!Arrays.equals(MiccCheck, Micc)) {
			Crypto.logger("Micc ", Micc);
			Crypto.logger("MiccCheck ", MiccCheck);
			throw new CardException("Chip read failure - check typing");
		}
		byte[] R = Crypto.DES3(Eicc, Cipher.DECRYPT_MODE, Kenc);
		byte[] RNDifdCheck = Arrays.copyOfRange(R, 8, 16);
		if (!Arrays.equals(RNDifdCheck, RNDifd)) {
			throw new CardException("Chip read failure - check typing");
		}

		// calculate session keys
		byte[] Kicc = Arrays.copyOfRange(R, 16, R.length);
		byte[] Kseed = new byte[Kicc.length];
		for (int i = 0; i < 16; i++)
			Kseed[i] = (byte) (Kifd[i] ^ Kicc[i]);
		byte[] KSenc = Crypto.makeKeys(Kseed, Crypto.KEY_TYPE_ENC);
		byte[] KSmac = Crypto.makeKeys(Kseed, Crypto.KEY_TYPE_MAC);

		// calculate send sequence counter
		byte[] SSC = Crypto.concat(
				Arrays.copyOfRange(RNDicc, 4, RNDicc.length),
				Arrays.copyOfRange(RNDifd, 4, RNDifd.length));

		// instantiate a secure messaging APDU object
		log.info("Instantiating secure messaging object");
		SecureMessageAPDU SMapdu = new SecureMessageAPDU();
		SMapdu.setKeys(KSenc, KSmac);
		SMapdu.startSSC(SSC);

		// at this point, secure messaging is established

		// create DG1
		log.info("creating DG1");
		DG1 dg1 = new DG1(tcvr);
		dg1.parseBioData(bd); // load relevant data into the BioData class

		// create DG2
		log.info("creating DG2");
		DG2 dg2 = new DG2(tcvr);
		dg2.parseBioData(bd);

		// create EF.SOD
		log.info("creating EF.SOD");
		EFSOD efSOD = new EFSOD(tcvr);

		// close the nfc transceiver
		passport.disconnect(false);

		// load EF data into the BioData class; this lets it be picked up
		// by the certificate check
		bd.setEfSOD(efSOD.getEncoded());
		bd.setDg1Hash(dg1.getHash());
		bd.setDg2Hash(dg2.getHash());
		return bd;
	}
}
