package com.duncanwestland.ec.serialisation;

import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.lang3.SerializationUtils;

public class Upload implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	transient Logger log = Logger.getLogger(this.getClass().getName());
	private String name = "Not Reported";
	private String dateOfBirth = "Not Reported";
	private String passportNumber = "Not Reported";
	private boolean signatureOK;
	private boolean idConfirmed;
	private boolean expired;
	private String userName = "Not Reported";
	private String docType = "Not Reported";
	private String issuingState = "Not Reported";
	private boolean backgroundCheckRequested = false;

	public String getIssuingState() {
		return issuingState;
	}

	public void setIssuingState(String issuingState) {
		this.issuingState = issuingState;
	}

	public boolean isBackgroundCheckRequested() {
		return backgroundCheckRequested;
	}

	public void setBackgroundCheckRequested(boolean backgroundCheckRequested) {
		this.backgroundCheckRequested = backgroundCheckRequested;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public boolean isSignatureOK() {
		return signatureOK;
	}

	public void setSignatureOK(boolean signatureOK) {
		this.signatureOK = signatureOK;
	}

	public boolean isIdConfirmed() {
		return idConfirmed;
	}

	public void setIdConfirmed(boolean idConfirmed) {
		this.idConfirmed = idConfirmed;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public byte[] serialise(){
		byte[] sbpkg = SerializationUtils.serialize(this);
		return sbpkg;
	}
}
