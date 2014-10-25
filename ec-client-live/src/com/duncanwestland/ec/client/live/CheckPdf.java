package com.duncanwestland.ec.client.live;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import sun.applet.Main;

/**
 * @author Duncan Westland
 * Class for testing a PdfCreator class
 * It does not form a part of the main ec-client-live
 * application
 */
public class CheckPdf {
	/**
	 * A simple main method to test the PdfCreator class
	 * This is intended to be used only with some on-the-fly
	 * source code editing. 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		InputStream fis = Main.class.getResourceAsStream("/resources/test.jpg");
		byte[] jpeg = IOUtils.toByteArray(fis);
		PdfCreator gpdf = new RedPdfCreator();
		gpdf.readResources();
		byte[] b = gpdf.typeSet("1234","lalis.trial@gmail.com",jpeg);
		OutputStream fos = new FileOutputStream("/home/duncan/test.pdf");
		fos.write(b);
		fos.close();
	}

}
