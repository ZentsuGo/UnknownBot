package fr.zentsugo.ubot;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Frame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel lblStatut;

	public Frame(String title, int width, int height,
			ActionListener startactionlistener, final ActionListener stopactionlistener) {
		super(title);
		if (width == 0 || height == 0)
			setSize(300, 120);
		else
			setSize(width, height);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				stopactionlistener.actionPerformed(null);
				System.exit(0);
			}
		});
		getContentPane().setLayout(null);
		getContentPane().setBackground(Color.WHITE);
		
		lblStatut = new JLabel("Status :");
		lblStatut.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblStatut.setBounds(10, 11, 274, 24);
		lblStatut.setText(lblStatut.getText() + " Disabled");
		getContentPane().add(lblStatut);
		
		JButton stopButton = new JButton("Stop");
		stopButton.setBackground(Color.WHITE);
		stopButton.addActionListener(stopactionlistener);
		stopButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		stopButton.setBounds(165, 58, 100, 23);
		getContentPane().add(stopButton);
		
		JButton startButton = new JButton("Start");
		startButton.setBackground(Color.WHITE);
		startButton.addActionListener(startactionlistener);
		startButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		startButton.setBounds(20, 58, 100, 23);
		getContentPane().add(startButton);
		
		JLabel lblNewLabel = new JLabel("Note : Ne pas stop/start rapidement sous cause d'erreurs");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 10));
		lblNewLabel.setBounds(10, 33, 274, 14);
		getContentPane().add(lblNewLabel);
		
		setVisible(true);
	}
	
	public void setStatusText(String text) {
		lblStatut.setText("Status : " + text);
	}
}
