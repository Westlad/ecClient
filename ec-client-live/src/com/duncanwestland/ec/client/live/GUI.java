package com.duncanwestland.ec.client.live;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * @author Duncan Westland
 * @version 2.0.0
 * Class that provides the GUI elements to the ec-client-live application.  These are
 * abstracted from the control logic which sits in the BRPVerifier class
 */
public class GUI extends JFrame {
	static Logger log=Logger.getLogger(GUI.class.getName());
	private static final long serialVersionUID = 1L;
	private static final int SEPARATOR = 10;
	private static final Dimension PORTRAIT = new Dimension(384,480);
	private static final String PORTRAIT_TEXT = "Portrait";
	JLabel portrait;
	JTextField dobTextField = new JTextField(8);
	JTextField doeTextField = new JTextField(8);
	JTextField passportTextField = new JTextField(9);
	JLabel name;
	JLabel check;
	JButton readButton = new JButton("Read");
	JButton clearButton = new JButton("Clear");
	JMenuItem menuItemProxy = new JMenuItem("Proxy settings",KeyEvent.VK_P);
	JMenuItem menuItemAbout = new JMenuItem("About",KeyEvent.VK_P);
			
	/**
	 * Instantiate the GUI
	 * The controlling BRPVerifier object is passed in so that
	 * the ActionListener in BRPVerifier can be called when
	 * the user interacts with the GUI
	 * @param pv the BRPVerifier object
	 */
	GUI(BRPVerifier pv) {
		super("Document Verifier");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		
		//set up the MRZ input GUI elements
		readButton.addActionListener(pv);
		//as the clear button doesn't access anything outside the gui class, it makes no sense to externalise it
		clearButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent event) {
				log.info("clear button pressed");
				portrait.setIcon(null);
				portrait.setText(PORTRAIT_TEXT);
				portrait.repaint();
				name.setText(null);
				dobTextField.setText(null);
				doeTextField.setText(null);
				passportTextField.setText(null);
				check.setBackground(null);
		}});
		//uncomment for testing
		//dobTextField.setText("05/01/1980");
		//doeTextField.setText("01/01/2014");
		//passportTextField.setText("zw7009102");
		
		dobTextField.setUI(new HintTextFieldUI("dd-mm-yyyy", false));
		doeTextField.setUI(new HintTextFieldUI("dd-mm-yyyy", false));
		passportTextField.setUI(new HintTextFieldUI("xxxxxxxxx", false));
				
		JLabel dobLabel = new JLabel("Date of birth", JLabel.RIGHT);
		JLabel doeLabel = new JLabel("Valid until", JLabel.RIGHT);
		JLabel passportLabel = new JLabel("Document number", JLabel.RIGHT);
		JPanel mrzInputPanel = new JPanel();
		GridLayout grid = new GridLayout(4,2,SEPARATOR,SEPARATOR);
		FlowLayout flow = new FlowLayout(FlowLayout.LEFT);

		mrzInputPanel.setLayout(grid);
		mrzInputPanel.add(passportLabel);		
		mrzInputPanel.add(passportTextField);
		mrzInputPanel.add(doeLabel);		
		mrzInputPanel.add(doeTextField);
		mrzInputPanel.add(dobLabel);	
		mrzInputPanel.add(dobTextField);
		mrzInputPanel.add(clearButton);
		mrzInputPanel.add(readButton);
		mrzInputPanel.setBorder(BorderFactory.createLineBorder(Color.black));
	    Border border = mrzInputPanel.getBorder();
	    Border margin = new EmptyBorder(10,10,10,10);
	    mrzInputPanel.setBorder(new CompoundBorder(border, margin));    
		//set up the display of passport portrait
		portrait = new JLabel(PORTRAIT_TEXT,JLabel.CENTER);
		portrait.setPreferredSize(PORTRAIT);
		portrait.setBorder(BorderFactory.createLineBorder(Color.black));
		//set up display of the biodata
		name = new JLabel("",JLabel.CENTER);
		check = new JLabel("Document Check",JLabel.CENTER);
		check.setOpaque(true);
		check.setBorder(BorderFactory.createLineBorder(Color.black));
		check.setPreferredSize(new Dimension(100,200));
		//add components to the GUI JFrame
		JPanel leftPane = new JPanel();	
		BorderLayout leftBox = new BorderLayout(SEPARATOR,SEPARATOR);
		leftPane.setLayout(leftBox);
		leftPane.add(mrzInputPanel,BorderLayout.NORTH);
		leftPane.add(name,BorderLayout.CENTER);
		leftPane.add(check, BorderLayout.SOUTH);
		leftPane.setPreferredSize(PORTRAIT);
		setLayout(flow);
		add(leftPane);
		add(portrait);
		setJMenuBar(menuBar(pv));
		
		pack();
		setVisible(true);
	}
	private JMenuBar menuBar(BRPVerifier pv) {
		//Create the menu bar.
		JMenuBar menuBar = new JMenuBar();

		//Build the first menu.
		JMenu menu = new JMenu("Config");
		menu.setMnemonic(KeyEvent.VK_C);
		menu.getAccessibleContext().setAccessibleDescription(
		        "Configuration menu");
		menuBar.add(menu);

		//add MenuItems
		menuItemProxy.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuItemProxy.getAccessibleContext().setAccessibleDescription(
		        "settings for working with a web proxy");
		menuItemProxy.addActionListener(pv);
		
		menu.add(menuItemProxy);		
		
		//Build the second menu.
		JMenu menuHelp = new JMenu("Help");
		menuHelp.setMnemonic(KeyEvent.VK_C);
		menuHelp.getAccessibleContext().setAccessibleDescription(
		        "Help menu");
		menuBar.add(menuHelp);

		//add MenuItems
		menuItemAbout.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuItemAbout.getAccessibleContext().setAccessibleDescription(
		        "settings for working with a web proxy");
		menuItemAbout.addActionListener(pv);
		
		menuHelp.add(menuItemAbout);
		return menuBar;
	}
}
