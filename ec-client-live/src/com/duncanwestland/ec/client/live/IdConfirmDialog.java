package com.duncanwestland.ec.client.live;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class IdConfirmDialog extends JDialog implements ActionListener {
	JButton yesButton = new JButton("Yes");
	JButton noButton = new JButton("No");
	private Boolean isConfirmed = null;

	public Boolean isConfirmed() {
		return isConfirmed;
	}

	public void setIsConfirmed(Boolean isConfirmed) {
		this.isConfirmed = isConfirmed;
	}

	public IdConfirmDialog(JFrame parent,Component locator, String title) {
		super(parent, title, true);
		setVisible(false);
		this.setPreferredSize(new Dimension(320,100));
		JPanel buttonPanel = new JPanel();
		//buttonPanel.setPreferredSize(new Dimension(200,100));
		yesButton.setBackground(Color.GREEN);
		yesButton.addActionListener(this);
		noButton.setBackground(Color.RED);
		noButton.addActionListener(this);
		Dimension dim = new Dimension(140,60);
		yesButton.setPreferredSize(dim);
		noButton.setPreferredSize(dim);
		buttonPanel.add(noButton);
		buttonPanel.add(yesButton);
		getContentPane().add(buttonPanel);

		pack();
		setResizable(false);
        setLocationRelativeTo(locator);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==noButton)	{
			this.setIsConfirmed(false);
		}		
		if(e.getSource()==yesButton)	{
			this.setIsConfirmed(true);
		}
		this.setVisible(false);		
	}
}
