package com.duncanwestland.ec.client.live;

import java.awt.Image;
import java.util.logging.Logger;

/**
 * 
 * @author duncan
 * @version 2.0.1
 * This class used to hold static biodata but as of version 2 I have made this a normal
 * instantiable class, which will be created by a passport reader.  I have also removed some of the junk
 * that isn't needed in the desktop version. To decouple this class I've removed some references 
 * to the static Crypto class's concat method. 2.0.1 includes a doctype.
 */

public class BioData {
	Logger log = Logger.getLogger(this.getClass().getName());
	private String name="";
	private String placeOfIssue="";
	//dates are dd-mm-yy format
	private String dateOfExpiry;
	private String dateOfBirth;
	private String passportNumber="";
	private String sex;
	private String docType;
	private String IssuingState;
	private boolean signatureOK= false;
	private byte[] dg1Hash;
	private byte[] dg2Hash;
	private byte[] efSOD;
	private Image jpeg;
	private boolean idConfirmed;
	private boolean Expired;
	
	public String getIssuingState() {
		return IssuingState;
	}

	public void setIssuingState(String issuingState) {
		IssuingState = issuingState;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public boolean isExpired() {
		return Expired;
	}

	public void setExpired(boolean expired) {
		Expired = expired;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlaceOfIssue() {
		return placeOfIssue;
	}

	public void setPlaceOfIssue(String placeOfIssue) {
		this.placeOfIssue = placeOfIssue;
	}

	public String getDateOfExpiry() {
		return dateOfExpiry;
	}

	public void setDateOfExpiry(String dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPassportNumber() {
		return passportNumber;
	}

	public void setPassportNumber(String passportNumber) {
		this.passportNumber = passportNumber;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public boolean isSignatureOK() {
		return signatureOK;
	}

	public void setSignatureOK(boolean signatureOK) {
		this.signatureOK = signatureOK;
	}

	public byte[] getDg1Hash() {
		return dg1Hash;
	}

	public void setDg1Hash(byte[] dg1Hash) {
		this.dg1Hash = dg1Hash;
	}

	public byte[] getDg2Hash() {
		return dg2Hash;
	}

	public void setDg2Hash(byte[] dg2Hash) {
		this.dg2Hash = dg2Hash;
	}

	public byte[] getEfSOD() {
		return efSOD;
	}

	public void setEfSOD(byte[] efSOD) {
		this.efSOD = efSOD;
	}

	public Image getJpeg() {
		return jpeg;
	}

	public void setJpeg(Image jpeg) {
		this.jpeg = jpeg;
	}

	public boolean isIdConfirmed() {
		return idConfirmed;
	}

	public void setIdConfirmed(boolean idConfirmed) {
		this.idConfirmed = idConfirmed;
	}
}
