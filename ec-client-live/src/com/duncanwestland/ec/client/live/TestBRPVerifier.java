/**
 * 
 */
package com.duncanwestland.ec.client.live;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import com.duncanwestland.ec.serialisation.Upload;
import com.google.gdata.util.AuthenticationException;

/**
 * @author Duncan Westland 
 * @version 2.0.0
 * This is a test version of the main controller class for the application. 
 * It instantiates the GUI but does not read a passport or make a ddf certificate.
 * It attempts to upload test data.
 */
public class TestBRPVerifier extends BRPVerifier implements ActionListener {
	Logger log = Logger.getLogger(TestBRPVerifier.class.getName());
	private final static String CONFIG_FILE = "/resources/ec-client.config";
	public static Properties prop = new Properties();
	public static Preferences pref;
	//hopefully this should load the properties from file when the class is loaded
	static {try {
		prop.load(TestBRPVerifier.class.getResourceAsStream(CONFIG_FILE));
	} catch (IOException e) {
		e.printStackTrace();
	}}

	private GUI gui;
	private Uploader uploader;
	private BioData bd;
	/*
	 * Instantiate the gui and the other main objects
	 */
	public TestBRPVerifier() {
		pref = Preferences.userRoot().node(this.getClass().getName());
		gui = new GUI(this);
		bd = new BioData();
		uploader = new Uploader();
		// create a test biodata object.  Only the uploaded parts are set
		bd = new BioData();
		bd.setDateOfBirth("10-09-1962");
		bd.setIdConfirmed(true);
		bd.setName("Duncan Westland");
		bd.setPassportNumber("123456789");
		bd.setSignatureOK(true);
		bd.setExpired(false);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource()==gui.readButton){
			// the read button has been pressed
			log.info("read button pressed");
			//grey out the last result
			gui.check.setBackground(null);
			try { 
				//display the results
				setBioDataDisplay();

				//all info is now available so set the result panel colour
				if (bd.isIdConfirmed()&&!bd.isExpired()&&bd.isSignatureOK()) {
						gui.check.setBackground(Color.GREEN);
					} else {
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
							ulData.setDateOfBirth(bd.getDateOfBirth());
							ulData.setIdConfirmed(bd.isIdConfirmed());
							ulData.setName(bd.getName());
							ulData.setPassportNumber(bd.getPassportNumber());
							ulData.setSignatureOK(bd.isSignatureOK());
							ulData.setExpired(bd.isExpired());
							ulData.setUserName(uploader.getUsername());
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
							LoginDialog login = new LoginDialog(gui,"Login to the Lalis trial");
							login.setVisible(true);
							if (login.isCancelled()) return;
							uploader.setUsername(login.getUsername());
							uploader.setPassword(login.getPassword());
							uploader.authenticate();
						}
					} 
				} catch (Exception e) {
					JOptionPane.showMessageDialog(gui, "Failed to upload");
					log.severe("Failed to upload" + e.toString());
					e.printStackTrace();
					return;
				}
			//upload now complete
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
	
	public void setBioDataDisplay() {
		//image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bd.getJpeg()));
		//ImageIcon icon = new ImageIcon(bd.getJpeg());
		//gui.portrait.setIcon(icon);
		//gui.portrait.setSize(icon.getIconWidth(),icon.getIconHeight());
		//gui.portrait.repaint();
		gui.name.setText(bd.getName());
	}
}
