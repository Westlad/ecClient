package com.duncanwestland.ec.client.live;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.pdfjet.Color;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * Creates a 'green' pdf certificate and stores it in the user's home folder
 */
public class GreenPdfCreator extends PdfCreator {

	public GreenPdfCreator() {}
	
	/**
	 * Overridden to provide variable data for the pdf.  Generally, you need
	 * InputStream fis = this.getClass().getResourceAsStream(fileName);
     * String text = IOUtils.toString(fis, "UTF-8");
     * fis.close();
	 */
	@Override
	protected void readResources() throws IOException{
		InputStream fis = this.getClass().getResourceAsStream("/resources/green_brp_text1");
	    text1 = IOUtils.toString(fis, "UTF-8");
	    fis = this.getClass().getResourceAsStream("/resources/green_brp_text2");
	    text2 = IOUtils.toString(fis, "UTF-8");
	    fis = this.getClass().getResourceAsStream("/resources/green_brp_text3");
	    text3 = IOUtils.toString(fis, "UTF-8");
		bkgnd = Color.green;
		auth = "Biometric Residence Permit - Authenticated";
	}
}
