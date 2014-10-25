/**
 * 
 */
package com.duncanwestland.ec.client.live;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.pdfjet.A4;
import com.pdfjet.Color;
import com.pdfjet.CoreFont;
import com.pdfjet.Font;
import com.pdfjet.Image;
import com.pdfjet.ImageType;
import com.pdfjet.PDF;
import com.pdfjet.Page;
import com.pdfjet.TextBox;

/**
 * @author duncan
 * This class creates a pdf that can be returned to the employer as a
 * read receipt.  It uses the PDFjet open source library
 *
 */
public abstract class PdfCreator {
	protected String text1;
	protected String text2;
	protected String text3;
	protected int bkgnd;
	protected String auth;
	protected String user;

	/**
	 * @throws Exception 
	 * 
	 */
	public PdfCreator() {}
	/**
	 * Override to provide variable data for the pdf.  Generally, you need
	 * 	InputStream fis = this.getClass().getResourceAsStream(fileName);
     *  String text = IOUtils.toString(fis, "UTF-8");
     *  fis.close();
	 * @throws IOException 
	 */
	protected void readResources() throws IOException{
		text1 = "text for page 1";
		text2 = "text for top of page 2";
		text3 = "text for bottom of page 3";
		bkgnd = Color.blue;
		auth = "Biometric Residence Permit - Status Unknown";
	}
	
	public final byte[] typeSet(String lastFour, String user, byte[] jpeg) throws Exception{
		final float XPOS = 60;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PDF pdf = new PDF(bos);
        Font f1 = new Font(pdf, CoreFont.HELVETICA);
        Font f2 = new Font(pdf, CoreFont.HELVETICA);
        Font f3 = new Font(pdf, CoreFont.HELVETICA);
        f2.setSize(25);
        f3.setSize(15);
        
        //UKBA logo and face image
        BufferedInputStream bis1 =
                new BufferedInputStream(
                		this.getClass().getResourceAsStream("/resources/ho_logo.png"));
        Image image1 = new Image(pdf, bis1, ImageType.PNG);
        
        bis1 = new BufferedInputStream(
        		new ByteArrayInputStream(jpeg));
        Image image2 = new Image(pdf, bis1, ImageType.JPG);
        
        Page page = new Page(pdf, A4.PORTRAIT);

        image1.setPosition(58, 40);
        image1.scaleBy(0.27);
        image1.drawOn(page);
        image2.scaleBy(0.29);
        image2.setLocation(427, 40);
        image2.drawOn(page);
        
        //title
        String text1 = auth;
        TextBox box1 = new TextBox(f2, text1,330,70);
        box1.setPosition(60, 225);
        box1.setBgColor(bkgnd);
        box1.setFgColor(Color.white);
        box1.drawOn(page);
        
        //
        String text2 = "See page 2 for enlarged image";
        TextBox box2 = new TextBox(f1, text2, 150,70);
        box2.setPosition(390, 225);
        box2.drawOn(page);
        
        //BRP info
        Date dNow = new Date( );
        SimpleDateFormat ft = 
        		new SimpleDateFormat ("E dd.MM.yyyy 'at' hh:mm:ss a zzz");
        StringBuilder sb = new StringBuilder();
        sb.append("BRP number xxxxx");
        sb.append(lastFour);
        sb.append(" read on ");
        sb.append(ft.format(dNow));
        sb.append(" by ");
        sb.append(user);
        String text3 =  sb.toString();
        TextBox box3 = new TextBox(f3,text3, 480, 40);
        box3.setPosition(XPOS, 295);
        box3.drawOn(page);
        
        //boilerplate text
        float height = addBox(this.text1, f1, page, XPOS, 335);
        
        Page page2 = new Page(pdf, A4.PORTRAIT);

        //boilerplate text
        height = addBox(this.text2, f1, page2, XPOS, 40);
  
        //face image
        height += 60;
        image2.scaleBy(2.5);
        image2.setLocation(130, height);
        image2.drawOn(page2);

        //boilerplate text
        height += 20 + image2.getHeight();
        height = addBox(this.text3,f1, page2 ,XPOS ,height);
        
       
        pdf.flush();
        byte[] pdfBytes = bos.toByteArray();
        bos.close();
        return pdfBytes;
    }
	
	private final float addBox(String text, Font f,Page page, float xPos, float yPos) throws Exception{
		final float BOX_WIDTH = 480;
        TextBox box = new TextBox(f,text,BOX_WIDTH,1);
        box.setPosition(xPos, yPos);
        box.drawOn(page);
		return box.getHeight();
	}

}
