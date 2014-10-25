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
	public class LoginDialog extends JDialog implements ActionListener {
		Logger log = Logger.getLogger(LoginDialog.class.getName());
	    protected JTextField tfUsername = new JTextField(20);
	    protected JPasswordField pfPassword = new JPasswordField(20);
	    protected JLabel lbUsername = new JLabel("Username",JLabel.RIGHT);
	    protected JLabel lbPassword = new JLabel("Password",JLabel.RIGHT);
	    protected JButton btnLogin = new JButton("Login");
	    protected JButton btnCancel = new JButton("Cancel");
	    protected String username;
	    protected String password;
	    private boolean isCancelled = false;
	 
	    public LoginDialog(JFrame parent,String title) {
	        super(parent, title, true);

	        this.setVisible(false);
	        //
	        Container contentPane = this.getContentPane();
	        BoxLayout box = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
	        FlowLayout flow = new FlowLayout(FlowLayout.RIGHT);
	        
	        JPanel usernamePanel = new JPanel(flow);
	        JPanel passwordPanel = new JPanel(flow);
	       
	        usernamePanel.add(lbUsername);
	        usernamePanel.add(tfUsername);
	        passwordPanel.add(lbPassword);
	        passwordPanel.add(pfPassword);
	        
	        //loginPanel.setBorder(new LineBorder(Color.GRAY));
	 
	        btnLogin.addActionListener(this);
	        btnCancel.addActionListener(this);
	        JPanel buttonPanel = new JPanel(flow);
	        buttonPanel.add(btnCancel);
	        buttonPanel.add(btnLogin);
	 
	        setLayout(box);
	        contentPane.add(usernamePanel);
	        contentPane.add(passwordPanel);
	 	   	contentPane.add(buttonPanel);

	        pack();
	        setResizable(false);
	        setLocationRelativeTo(parent);
	    }
	 
	    public String getUsername() {
	        return username;
	    }
	 
	    public String getPassword() {
	        return password;
	    }
	    
	    public boolean isCancelled() {
	    	boolean cancelled = isCancelled;
	        return cancelled;
	    }

	    @Override
        public void actionPerformed(ActionEvent e) {
	    	if (e.getSource() == btnLogin) {
	    		log.info("login button pressed");
	    		password = new String(pfPassword.getPassword());
            	username = tfUsername.getText().trim();
	    	}
	    	if (e.getSource() == btnCancel) {
	    		isCancelled = true;
	    		log.info("cancel button pressed");
	    	}
	        tfUsername.setText("");
            pfPassword.setText("");
	    	this.setVisible(false);
	    }
	}

