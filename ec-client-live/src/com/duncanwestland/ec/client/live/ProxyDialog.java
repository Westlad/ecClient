/**
 * 
 */
package com.duncanwestland.ec.client.live;

/**
 * @author duncan
 *
 */
	import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

import javax.swing.*;
	 
	@SuppressWarnings("serial")
	public class ProxyDialog extends JDialog implements ActionListener {
		Logger log = Logger.getLogger(ProxyDialog.class.getName());
	    protected JTextField tfProxyHost = new JTextField(20);
	    protected JTextField tfProxyPort = new JTextField(20);
	    protected JLabel lbProxyHost = new JLabel("Proxy Host",JLabel.RIGHT);
	    protected JLabel lbProxyPort = new JLabel("Proxy Port",JLabel.RIGHT);
	    protected JButton btnOK = new JButton("OK");
	    protected JButton btnCancel = new JButton("Cancel");
	    protected String proxyHost;
	    protected String proxyPort;
	    private boolean isCancelled = false;
	 
	    public ProxyDialog(JFrame parent,String title) {
	        super(parent, title, true);

	        this.setVisible(false);
	        //
	        Container contentPane = this.getContentPane();
	        BoxLayout box = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
	        FlowLayout flow = new FlowLayout(FlowLayout.RIGHT);
	        
	        JPanel proxyHostPanel = new JPanel(flow);
	        JPanel proxyPortPanel = new JPanel(flow);
	       
	        proxyHostPanel.add(lbProxyHost);
	        proxyHostPanel.add(tfProxyHost);
	        proxyPortPanel.add(lbProxyPort);
	        proxyPortPanel.add(tfProxyPort);
	        
	        //loginPanel.setBorder(new LineBorder(Color.GRAY));
	 
	        btnOK.addActionListener(this);
	        btnCancel.addActionListener(this);
	        JPanel buttonPanel = new JPanel(flow);
	        buttonPanel.add(btnCancel);
	        buttonPanel.add(btnOK);
	 
	        setLayout(box);
	        contentPane.add(proxyHostPanel);
	        contentPane.add(proxyPortPanel);
	 	   	contentPane.add(buttonPanel);

	        pack();
	        setResizable(false);
	        setLocationRelativeTo(parent);
	    }
	 
	    public String getProxyHost() {
	        return proxyHost;
	    }
	 
	    public String getProxyPort() {
	        return proxyPort;
	    }
	    
	    public void setProxyHost(String proxyHost) {
	    	this.tfProxyHost.setText(proxyHost);
			this.proxyHost = proxyHost;
		}

		public void setProxyPort(String proxyPort) {
			this.tfProxyPort.setText(proxyPort);
			this.proxyPort = proxyPort;
		}

		public boolean isCancelled() {
	    	boolean cancelled = isCancelled;
	        return cancelled;
	    }

	    @Override
        public void actionPerformed(ActionEvent e) {
	    	if (e.getSource() == btnOK) {
	    		log.info("OK button pressed");
	    		proxyPort = tfProxyPort.getText().trim();
            	proxyHost = tfProxyHost.getText().trim();
	    	}
	    	if (e.getSource() == btnCancel) {
	    		isCancelled = true;
	    		log.info("cancel button pressed");
	    	}
	        tfProxyHost.setText("");
            tfProxyPort.setText("");
	    	this.setVisible(false);
	    }
	}

