package fr.zentsugo.ubot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import fr.zentsugo.ubot.events.EventsListener;
import fr.zentsugo.ubot.events.Listener;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Main {
		
	public static Frame f;
	public static JDA jda;
	private static Timer waiting;
	
	 public static void main(String[] args) {
		 waiting = new Timer(1500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (jda != null)
					f.setStatusText(jda.getStatus().toString());
				else
					f.setStatusText("Disabled");
			}
		 });
		 waiting.setRepeats(false);
		 
		 f = new Frame("UnknownBot v." + Listener.VERSION, 0, 0, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				run();
			}
		 }, new ActionListener() {
			 @Override
			 public void actionPerformed(ActionEvent e) {
				 stop();
			 }
		 });
	     
	 }
	 
	 public static void run() {
		 if (jda == null) {
			/*f.setStatusText("Checking save files...");
			waiting.start();
			f.setStatusText("Error save file : " + checkFiles());
			waiting.start();*/
			f.setStatusText("Waiting...");
			start();
			waiting.start();
		} else {
			f.setStatusText("Already started !");
			waiting.start();
		}
	 }
	 
	 public static void stop() {
		 if (jda != null) {
			/*f.setStatusText("Checking save files...");
			waiting.start();*/
			//Utils.checkSaveFile();
			f.setStatusText("Waiting...");
		 	jda.shutdown();
		 	jda = null;
		 	f.setStatusText("Disabled");
		 } else {
			f.setStatusText("Not started !");
			waiting.start();
			 //jda.shutdown();
		 }
	 }
	 
	 public static void start() {
		 try {
			 //checkSaveFile();
			 EventWaiter waiter = new EventWaiter();
			 jda = new JDABuilder(AccountType.BOT)
					 .setToken(Listener.TOKEN)
					 .setAutoReconnect(true)
					 .addEventListeners(waiter, new EventsListener(waiter))
					 .build();
		 } catch (Exception e) {
			 f.setStatusText("Error");
			 e.printStackTrace();
		 }
	 }
	 
}