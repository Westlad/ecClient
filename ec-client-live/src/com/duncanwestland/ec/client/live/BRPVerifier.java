/**
 * 
 */
package com.duncanwestland.ec.client.live;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.smartcardio.CardException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.io.IOUtils;

import sun.tools.jar.Main;

import com.duncanwestland.ec.serialisation.Upload;
import com.google.gdata.util.AuthenticationException;

/**
 * @author Duncan Westland 
 * @version 2.0.1
 * This is the main controller class for the application. It instantiates the GUI, the card reader
 * and classes.
 */
public class BRPVerifier implements ActionListener {
	Logger log = Logger.getLogger(BRPVerifier.class.getName());
	private final static String CONFIG_FILE = "/resources/ec-client.config";
	public static Properties prop = new Properties();
	public static Preferences pref;
	//hopefully this should load the properties from file when the class is loaded
	static {try {
		prop.load(BRPVerifier.class.getResourceAsStream(CONFIG_FILE));
	} catch (IOException e) {
		e.printStackTrace();
	}}

	private GUI gui;
	private Uploader uploader;
	private BioData bd;
	private enum validate { DATE, PASSPORT_NUMBER };

	/*
	 * Instantiate the gui and the other main objects
	 */
	public BRPVerifier() {
		pref = Preferences.userRoot().node(this.getClass().getName());
		gui = new GUI(this);
		bd = new BioData();
		uploader = new Uploader();
		//make sure that the environment is ok
		try {
			checkEnv();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(gui, "Error - " + e.getMessage());
			log.severe(e.toString());
			e.printStackTrace();
			System.exit(125);			
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource()==gui.readButton){
			// the read button has been pressed
			log.info("read button pressed");
			//grey out the last result
			gui.check.setBackground(null);
			try { //validate the input fields
				try{
					validate(gui.dobTextField, validate.DATE);
					validate(gui.doeTextField, validate.DATE);
					validate(gui.passportTextField, validate.PASSPORT_NUMBER);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(gui, e.getMessage());
					return;	
				}
				
				// attempt to read the BRP and create a biodata object
				try {
					String dob = gui.dobTextField.getText().trim();
					String doe = gui.doeTextField.getText().trim();
					String doc = gui.passportTextField.getText().trim().toUpperCase();
					//pad the document number if necessary
					while(doc.length()<9) {
						doc += "<";
					}
					bd = new BRPReader().read(doc,dob,doe);
				} catch (CardException e) {
					JOptionPane.showMessageDialog(gui, e.getMessage());
					log.severe("chip reader failure" + e.toString());
					return;
				} catch (Exception e) {
					JOptionPane.showMessageDialog(gui, "Chip read failure - check typing");
					log.severe("chip reader failure" + e.toString());
					return;
				}
				//display the results
				setBioDataDisplay();

				// check the digital signature
				boolean sigOK=false;
				try {
					sigOK = new SignatureCheck().checkSignature(bd);
				} catch(Exception e) {
					sigOK = false;
					log.warning("Error reading signature: "+e.getMessage());
				}
				bd.setSignatureOK(sigOK);
				//check for expiry (a successful read means the doe is correct)
				//an out of date BRP would probably fail the signature check so this
				//is probably superfluous
				try {
					checkExpiry();
					bd.setExpired(false);
				} catch(Exception e){
					bd.setExpired(true);
					log.warning(e.toString());
				}
				//confirm the identity
				IdConfirmDialog confirm = new IdConfirmDialog(gui,gui.name,"Does the holder match the photo?");
				if (prop.getProperty("idConfirm").equalsIgnoreCase("yes") && bd.getJpeg()!=null) {
					while (confirm.isConfirmed() == null)
						confirm.setVisible(true);
					bd.setIdConfirmed(confirm.isConfirmed()); 
				}
				//all info is now available so set the result panel colour
				if (bd.isIdConfirmed()&&!bd.isExpired()&&bd.isSignatureOK()) {
						gui.check.setBackground(Color.GREEN);
					} 
				else if (bd.isIdConfirmed()&&!bd.isExpired()&&
						!bd.isSignatureOK()&&
						!bd.getIssuingState().equalsIgnoreCase("GBR") 
						||
						!bd.getIssuingState().equalsIgnoreCase("GBR") &&
						bd.getJpeg() == null
						) {
					gui.check.setBackground(Color.YELLOW);
					//if a foreign book throws an error, we call it YELLOW
				}
				else {
						gui.check.setBackground(Color.RED);
				}
				gui.check.repaint();
				
				// the BioData is now fully populated so upload it to ec-cloud
				//we'll allow three attempts
				int attemptCount = 0;
				Upload ulData = new Upload();
				try {
					while (true){
						attemptCount++;
						if (attemptCount>4) throw new Exception("More than three authentication attempts");
						try {
							if(prop.getProperty("docType").equalsIgnoreCase("yes")) ulData.setDocType(bd.getDocType());
							if(prop.getProperty("dateOfBirth").equalsIgnoreCase("yes")) ulData.setDateOfBirth(bd.getDateOfBirth());
							if(prop.getProperty("name").equalsIgnoreCase("yes")) ulData.setName(bd.getName());
							if(prop.getProperty("passportNumber").equalsIgnoreCase("yes")) ulData.setPassportNumber(bd.getPassportNumber());
							if(prop.getProperty("userName").equalsIgnoreCase("yes")) ulData.setUserName(uploader.getUsername());
							if(prop.getProperty("requestBackgroundCheck").equalsIgnoreCase("yes")) ulData.setBackgroundCheckRequested(true);
							else ulData.setBackgroundCheckRequested(false);
							if(prop.getProperty("issuingState").equalsIgnoreCase("yes")) ulData.setIssuingState(bd.getIssuingState());
							ulData.setSignatureOK(bd.isSignatureOK());
							ulData.setExpired(bd.isExpired());
							ulData.setIdConfirmed(bd.isIdConfirmed());
							uploader.setHttpsCerts(prop.getProperty("httpsCerts"));
							uploader.setHttpsCertsPassword(prop.getProperty("httpsCertsPassword"));
							uploader.setServiceUrl(prop.getProperty("serviceUrl"));
							uploader.setProxyHost(pref.get("proxyHost",""));
							uploader.setProxyPort(pref.get("proxyPort",""));
							uploader.upload(ulData.serialise());
							break;
						} catch (AuthenticationException e){
							//authentication has failed.  This could be because we're not logged in
							log.info("login attemps = "+ attemptCount);
							LoginDialog login = new LoginDialog(gui,"Login to complete transaction");
							login.setVisible(true);
							if (login.isCancelled()) return;
							uploader.setUsername(login.getUsername());
							uploader.setPassword(login.getPassword());
							uploader.authenticate();
						}
					} 
					//if we're not writing a PDF, we need an alternative indication
					//of a successful upload
					if (!prop.getProperty("createPdf").equalsIgnoreCase("yes")){
						JOptionPane.showMessageDialog(gui, "Upload successful!");
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(gui, "Failed to upload");
					log.severe("Failed to upload" + e.toString());
					return;
				}
				//upload now complete - make a pdf
				if (prop.getProperty("createPdf").equalsIgnoreCase("yes")){
					try {
						savePdf();
					} catch (Throwable e) {
						JOptionPane.showMessageDialog(gui, "Failed to write pdf");
						log.severe("Failed to write pdf" + e.toString());
						return;
					}
				} 
			} catch (Exception e) {
				JOptionPane.showMessageDialog(gui, e.getMessage());
				log.severe("Error " + e.toString());
			}
		}
		if (event.getSource()==gui.menuItemProxy){
			//update the proxy settings
			ProxyDialog proxyDialog = new ProxyDialog(gui,"Proxy information");
			proxyDialog.setProxyHost(pref.get("proxyHost",""));
			proxyDialog.setProxyPort(pref.get("proxyPort",""));
			proxyDialog.setVisible(true);
			pref.put("proxyHost", proxyDialog.getProxyHost());
			pref.put("proxyPort", proxyDialog.getProxyPort());
		}
		if (event.getSource()==gui.menuItemAbout){
			//Display the version
			JOptionPane.showMessageDialog(gui, "Version " + prop.getProperty("version"));
		}
	}
	/**
	 * Eventually this will do some checks of the environment that the app is running in
	 * currently it does nothing
	 * @throws Exception 
	 */
	private void checkEnv() throws Exception{
		//TODO
	}
	/**
	 * Creates a pdf 'certificate'
	 * Writes a UKVI 'green' or 'red certificate to the user's home folder
	 * @throws Exception (unfortunately pdfJet throws an unqualified exception in
	 * some circumstances so this routine is forced to too).
	 */
	private void savePdf() throws Exception{
		PdfCreator pdf;
		if (gui.check.getBackground().equals(Color.GREEN)&& bd.isIdConfirmed()) {
			pdf = new GreenPdfCreator();
		} else if (gui.check.getBackground().equals(Color.RED)||!bd.isIdConfirmed()) {
			pdf = new RedPdfCreator();
		}
		else throw new Exception("Could not decide which pdf to write");
		pdf.readResources();
		String last = bd.getPassportNumber()
				.substring(bd.getPassportNumber().length()-4);
		ByteArrayOutputStream os = null;
		FileOutputStream output = null;
		try {
			os = new ByteArrayOutputStream();
			ImageIO.write((BufferedImage) bd.getJpeg(), "jpg", os);
			byte[] pdfBytes = pdf.typeSet(last,uploader.getUsername(),os.toByteArray());
			String homePath = System.getProperty("user.home");
			String dt = ((Long)new Date().getTime()).toString();
			String rnd = dt.substring(dt.length()-6);
			String fileName = "BRP_" +bd.getName()+"_"+ bd.getPassportNumber() +"v"+ rnd + ".pdf";
			File f = new File(homePath, fileName);
			output = new FileOutputStream(f);
			IOUtils.write(pdfBytes, output);
			log.info("Wrote pdf");
			JOptionPane.showMessageDialog(gui,
					"The BRP data has been uploaded and certificate\n"
							+ fileName + " written to " + homePath);
		} finally {
			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(os);
		}
	}
	/**
	 * Check if the BRP has expired.  It's best to place this after the read is 
	 * successfully completed because then the doe is confirmed
	 * @throws Exception 
	 */
	private void checkExpiry() throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
		Date doe = sdf.parse(bd.getDateOfExpiry());
		Date now = new Date();
		boolean test = now.after(doe);
		if (test) throw(new Exception("This document has expired"));
	}

	/**
	 * Validates the BAC fields that are entered in the GUI
	 * @param testField The JTextField that is being validated
	 * @param type Whether a date or document number is being validated
	 * @throws ValidationException if validation fails
	 */
	private void validate(JTextField testField, validate type) throws Exception {
		//TODO see if the Apache validation library is a better way to go
		boolean result = true;
		String test = testField.getText();
		String validateFailed = "Unknown validation error";
		switch (type) {
		case DATE:
			validateFailed = "Error in date format: " + testField.getText();
			if (test.length() != 10) {
				result = false;
				break;
			}
			int day = Integer.parseInt(test.substring(0, 2));
			int month = Integer.parseInt(test.substring(3, 5));
			int year = Integer.parseInt(test.substring(6,8));
			if (day < 1 || day > 31)
				result = false;
			if (month < 1 || month > 12)
				result = false;
			if (year!=19 && year!=20)
				result = false;
			break;
		case PASSPORT_NUMBER:
			validateFailed = "Error in document number: " + testField.getText();
			if (test.length() > 9) {
				result = false;
				break;
			}
			break;
		}
		//log.info("validation result " + result + " for " + testField.getText());
		if (!result) {
			// validation failed
			throw new ValidationException(validateFailed); //TODO create more specific exception
		}
	}

	public void setBioDataDisplay() {
		//image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bd.getJpeg()));
		if (bd.getJpeg()!=null){
			ImageIcon icon = new ImageIcon(bd.getJpeg());
			gui.portrait.setText("");
			gui.portrait.setIcon(icon);
			gui.portrait.setSize(icon.getIconWidth(),icon.getIconHeight());
		} else {
				gui.portrait.setText("No Image Found");
		}
		gui.portrait.repaint();
		gui.name.setText(bd.getName());
	}

	@SuppressWarnings("unused")
	public static void main(String[] arguments){
		Logger log = Logger.getLogger("main");
		//setup logging defaults
		InputStream logStream = Main.class.getResourceAsStream("/resources/logging.properties");
	    try {
			LogManager.getLogManager().readConfiguration(logStream);
		} catch (SecurityException | IOException e1) {
			e1.printStackTrace();
		}
		BRPVerifier pv = new BRPVerifier();
	}
}
