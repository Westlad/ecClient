package com.duncanwestland.ec.client.live;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;

public class SignatureCheck {
	/**
	 * Performs a check of the signatures in EF.SOD. It checks that the
	 * certificate chain is valid, that the hash table contains the hashes of
	 * dg1 and dg2 and that the signature is valid
	 * 
	 * @param sod
	 * @param dg1
	 * @param dg2
	 * @return
	 */
	private final String KEYSTORE = "/resources/csca.jks";
	//static {
//		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//	}
	public  Logger log = Logger.getLogger(SignatureCheck.class.getName());
	@SuppressWarnings("deprecation")
	public boolean checkSignature(BioData bd) throws Exception {
		boolean signatureOK = true; // a temporary assignment
		// convert the input hash strings into byte arrays
		byte[] efSOD = bd.getEfSOD();
		byte[] dg1HashOriginal = bd.getDg1Hash();
		byte[] dg2HashOriginal = bd.getDg2Hash();
		/**
		 * the first check ensures that the hashes of the data groups and the
		 * signed hashes in the hash table within EF.SOD are the same
		 */
		log.info("creating ByteArrayInputStream");
		ByteArrayInputStream bin = new ByteArrayInputStream(efSOD);
		//bin.skip(4); // don't want the EF.SOD tag+length, just the content
		log.info("creating ANS1InputStream");
		ASN1InputStream ain = new ASN1InputStream(bin);
		// create the cms signed data object from which everything flows	
		log.info("creating CMSSignedData");
		CMSSignedData cms = new CMSSignedData(ain);
		log.info("CMSSignedDataDone");
		byte[] b = (byte[]) cms.getSignedContent().getContent();
		bin = new ByteArrayInputStream(b);
		ain = new ASN1InputStream(bin);
		DERSequence content = (DERSequence) ain.readObject();
		// read in the signed data
		DERSequence hashOfHashes = (DERSequence) content.getObjectAt(2); // extract
																			// the
																			// hash
																			// of
																			// hashes
		DERSequence dg1Seq = (DERSequence) hashOfHashes.getObjectAt(0);
		byte[] dg1Hash = ((DEROctetString) dg1Seq.getObjectAt(1)).getEncoded();
		DERSequence dg2Seq = (DERSequence) hashOfHashes.getObjectAt(1);
		byte[] dg2Hash = ((DEROctetString) dg2Seq.getObjectAt(1)).getEncoded();
		//strip the first two bytes, which must be a tag of some sort
		dg1Hash = Arrays.copyOfRange(dg1Hash, 2, dg1Hash.length);
		dg2Hash = Arrays.copyOfRange(dg2Hash, 2, dg2Hash.length);

		// hashes are now extracted from EF.SOD so compare with the originals
		log.info("comparing computed hashes with those in EFSOD");
		if (!Arrays.equals(dg1Hash, dg1HashOriginal)
				| !Arrays.equals(dg2Hash, dg2HashOriginal)) {
			signatureOK = false;
		}
		log.info(new Boolean(signatureOK).toString());
		// next we need to check the signature using the document signer
		CertStore certs = cms.getCertificatesAndCRLs("Collection", "SUN");
		SignerInformationStore signers = cms.getSignerInfos();
		Collection<?> c = signers.getSigners();
		Iterator<?> it = c.iterator();
		X509Certificate DScert = null;
		//SignerInformationVerifier siv = null;
		log.info("checking document signature against DS cert");
		while (it.hasNext()) {
			SignerInformation signer = (SignerInformation) it.next();
			Collection<?> certCollection = certs.getCertificates(signer
					.getSID());
			Iterator<?> certIt = certCollection.iterator();
			DScert = (X509Certificate) certIt.next();
			if (!signer.verify(DScert,"BC")) signatureOK = false;
		}
		log.info(new Boolean(signatureOK).toString());
		// then check the DS cert is genuine
		// load country signing certificates
		log.info("checking DS cert against CS certs");
		ArrayList<X509Certificate> csca = getCSCACerts();
		boolean certOK = false;
		for (X509Certificate cs : csca) {
			try {
				log.info("checking cert " + cs.getSerialNumber());
				DScert.verify(cs.getPublicKey());
				certOK = true;
			} catch (InvalidKeyException e) {
				certOK = false;
			} catch (CertificateException e) {
				certOK = false;
			} catch (SignatureException e) {
				certOK = false;
			} catch (NoSuchAlgorithmException e) {
				certOK = false;
			} catch (NoSuchProviderException e) {
				certOK = false;
			}
			if (certOK)
				break;
		}
		if (!certOK)
			signatureOK = false;
		log.info(new Boolean(signatureOK).toString());
		// and finally that it's valid (in date)
		log.info("finally, checking DS cert is not expired");
		try {
			DScert.checkValidity();
		} catch (CertificateExpiredException e) {
			signatureOK = false;
			log.severe("DS cert expired on "+ DScert.getNotAfter());
		} catch (CertificateNotYetValidException e) {
			signatureOK = false;
		}
		log.info(new Boolean(signatureOK).toString());
		ain.close();
		return signatureOK;
	}

	/**
	 * loads an arraylist containing the country signing certificates from a
	 * keystore
	 * 
	 * @throws KeyStoreException
	 */
	public ArrayList<X509Certificate> getCSCACerts() throws Exception { 
		//final File keyStoreFile = new File("/home/duncan/Applications/keystore/csca.jks");
		KeyStore ks = KeyStore.getInstance("JKS");
		InputStream keyStoreStream = this.getClass().getResourceAsStream(KEYSTORE);
		if (keyStoreStream == null) throw new Exception("Could not find CS key store");
		ks.load(keyStoreStream, "cscam0djw".toCharArray());
		ArrayList<X509Certificate> csca = new ArrayList<X509Certificate>();
		for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
			csca.add((X509Certificate) ks.getCertificate(e.nextElement()));
		}
		return csca;
	}

	/**
	 * returns a hexadecimal string representation of a byte array.
	 * 
	 * @param b
	 * @return
	 */
	public static String hex(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	public static byte[] unHex(String hexString) {
	     HexBinaryAdapter adapter = new HexBinaryAdapter();
	     byte[] bytes = adapter.unmarshal(hexString);
	     return bytes;
	}

}
