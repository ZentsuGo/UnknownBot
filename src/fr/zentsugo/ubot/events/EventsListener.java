package fr.zentsugo.ubot.events;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import fr.zentsugo.ubot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventsListener extends ListenerAdapter {
	
	private EventWaiter waiter;
	public EventsListener(EventWaiter waiter) {
		this.waiter = waiter;
	}
	
	@Override
    public void onReady(ReadyEvent event) {
        System.out.println("Ready");
        Activity g = Activity.playing("!ask");
        event.getJDA().getPresence().setActivity(g);
    }
	
	@Override
	public void onShutdown(ShutdownEvent event) {
		System.out.println("Shutdown");
		//JDA getPresence() for more methods
		event.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);
		if (Main.f != null)
			Main.f.setStatusText("Disabled");
		else
			System.exit(0);
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		//welcome for a new user who has joined the server/guild
		Member member = event.getMember();
		copyChannel(member, true);
	}
	
	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		//bye for a new user who has left the server/guild
		Member member = event.getMember();
		List<Category> may = event.getGuild().getCategoriesByName(member.getUser().getId(), true);
		Category category = may.isEmpty() ? null : may.get(0);
		if (category == null) return;
		HashMap<String, Integer> names = new HashMap<>();
		for (int l = 0; l < category.getTextChannels().size(); l++) {
			names.put(category.getTextChannels().get(l).getName(), l);
		}
		for (String channel : channels) {
			TextChannel tmp = (names.containsKey(channel) ? category.getTextChannels().get(names.get(channel)) : null);
			if (tmp == null) continue;
			tmp.delete().queue();
			print(event.getGuild(), "Deleted : " + channel + " of " + member.getUser().getId() + " (" + member.getEffectiveName() + ")", true);
		}
		category.delete().queue();
		print(event.getGuild(), "Deleted : catgegory " + member.getUser().getId() + " (" + member.getEffectiveName() + ")", true);
	}
	
	static boolean mute = false;
	static final String base = "base";
	public void onMessageReceived(final MessageReceivedEvent event) {
		String message = event.getMessage().getContentRaw();
		Guild guild = event.getGuild();
		String[] args = message.split(" ");
		String cmd = args[0];
		
		if (!cmd.startsWith("ib!")) {
			List<Category> may = event.getMember().isOwner() ? guild.getCategoriesByName(base, true) : guild.getCategoriesByName(event.getMember().getUser().getId(), true);
			Category category = may.isEmpty() ? null : may.get(0);
			if (category != null) {
			HashMap<String, Integer> names = new HashMap<>();
			for (int l = 0; l < category.getTextChannels().size(); l++) {
				names.put(category.getTextChannels().get(l).getName(), l);
			}
			TextChannel tmp = (names.containsKey(channels[1]) ? category.getTextChannels().get(names.get(channels[1])) : null);
			names.clear();
			if (tmp != null) {
				if (!cmd.startsWith("!ask")) return;
					event.getChannel().sendMessage("Oui j'écoute, quelle est votre question ?").queue();
					waiter.waitForEvent(GuildMessageReceivedEvent.class,
							q -> q.getAuthor().equals(event.getAuthor()),
							q -> {
								String txt = q.getMessage().getContentRaw();
								String id = event.getChannel()
										.sendMessage("Êtes-vous sûr de poser cette question ?")
										.complete().getId();
								Message msg = event.getChannel().retrieveMessageById(id).complete();
								msg.addReaction("✅").queue();
								msg.addReaction("❌").queue();
								waiter.waitForEvent(MessageReactionAddEvent.class,
										//create new variable e handling MessageReceivedEvent (condition predicate) if it's the same channel and author
										j -> j.getMember().equals(q.getMember()) && j.getMessageId().equals(id),
										//using e again consumer so in the case the condition predicate is true do that using e and event (two different event listeners) sending message
										j -> {
											if (j.getReactionEmote().getName().equals("✅")) {
												askQuestion(event, txt, guild);
											} else {
												event.getChannel().sendMessage("Demande annulée.").queue();
												msg.delete().queue();
											}
										},
										60, TimeUnit.SECONDS, new Runnable() {
											public void run() {
												print(guild, "Demande expirée.", false);
												msg.clearReactions().queue();
											}
										});
							}, 60, TimeUnit.SECONDS, new Runnable() {
								@Override
								public void run() {
									event.getChannel().sendMessage("Demande expirée.").queue();
								}
							});
				}
			}
		}
		
		if (!event.getMember().isOwner()) return;
		
		cmd = cmd.replaceFirst("ib!", "");
		
		event.getMessage().delete().queue();
		
		switch (cmd) {
			case "update" :
				if (args.length == 2) {
					if (!args[1].equals("last") && !args[1].equals("all")) break;
					guild.getMembers().forEach(member -> {
						//causes errors if bad connection
						//better adapt to queue with consumer system to wait for completion
						//otherwise try to make something with sequential execution (maybe thread)
						copyChannel(member, args[1].equals("all") ? true : false);
					});
 				} else if (args.length == 3) {
					Member member = event.getGuild().getMemberById(args[2]);
					copyChannel(member, args[1].equals("all") ? true : false);
				} else print(guild, "Usage : ib!update last/all (member)", false);
			break;
			case "compare" :
				TextChannel first = guild.getTextChannelById(args[1]);
				TextChannel second = guild.getTextChannelById(args[2]);
				event.getTextChannel().sendMessage("compared : " + compare(guild, first, second)).queue();
				break;
			case "send" :
				if (args.length >= 3) {
					boolean big = args[1].equals("new") ? true : false;
					final String txt = event.getMessage().getContentRaw().substring(
							event.getMessage().getContentRaw().indexOf(!big ? args[1] : args[3]), event.getMessage().getContentRaw().length() - 1);
					print(guild, "And the second text/answer is ?", true);
					waiter.waitForEvent(GuildMessageReceivedEvent.class,
							m -> m.getAuthor().equals(event.getAuthor()),
							m -> {
								String txt2 = m.getMessage().getContentRaw();
								String id = guild.getTextChannelById(logs)
										.sendMessage("Are you sure to send this to everyone?")
										.complete().getId();
										Message msg = guild.getTextChannelById(logs).retrieveMessageById(id).complete();
										msg.addReaction("✅").queue();
										msg.addReaction("❌").queue();
										waiter.waitForEvent(GuildMessageReactionAddEvent.class,
												e -> e.getMember().equals(event.getMember()) && e.getMessageId().equals(id),
												e -> {
													if (e.getReactionEmote().getName().equals("✅")) {
														print(guild, "Sent.", true);
															sendForEach(guild, !big, (!big ? "Réponse" : args[2]), channels[4], (!big ? txt : txt2), (!big ? txt2 : txt), null);
													} else {
														print(guild, "Cancelled.", false);
														msg.clearReactions().queue();
													}
												}, 120, TimeUnit.SECONDS, new Runnable() {
													@Override
													public void run() {
														print(guild, "Send expired.", false);
														msg.clearReactions().queue();
													}
												});
							}, 5, TimeUnit.MINUTES, new Runnable() {
								@Override
								public void run() {
									print(guild, "Send expired.", false);
								}
							});
				} else print(guild, "Usage : ib!send (new) (<title>) <message>", false);
			break;
			case "pruneall" :
				Iterator<Message> iterator = event.getChannel().getIterableHistory().iterator();
				while (iterator.hasNext()) {
					iterator.next().delete().queue();
				}
				break;
			case "list" :
				event.getGuild().getMembers().forEach(e -> {
					print(guild, e.getEffectiveName() + " : " + e.getUser().getId(), true);
				});
				break;
			case "stop" :
				Main.stop();
				break;
		}
	}
	
	private String logs = "612071214747353120";
//	private String logs = "612289389115998218";
	
	private TextChannel getSpecificChannel(Guild guild, Member member, String channel) {
		List<Category> categories = member.isOwner() ? guild.getCategoriesByName(base, true) : guild.getCategoriesByName(member.getUser().getId(), true);
		if (categories.isEmpty()) return null;
		Category category = categories.get(0);
		for (int i = 0; i < category.getTextChannels().size(); i++) {
			if (category.getTextChannels().get(i).getName().equals(channel))
				return category.getTextChannels().get(i);
		}
		return null;
	}
	
	private void askQuestion(MessageReceivedEvent event, String txt, Guild guild) {
		event.getChannel().sendMessage("Votre question va être traitée en privé (ou public dans #questions)").queue();
		String message2 = txt;
		guild.getTextChannelById(logs).sendMessage("New question " + guild.getOwner().getAsMention()).queue();
		String id = guild.getTextChannelById(logs)
				.sendMessage(new EmbedBuilder().setTitle((!event.getMember().isOwner() ?
						channels[1] + "_" + event.getMember().getUser().getId() + " (" + event.getMember().getEffectiveName() + ")" :
							channels[1]))
						.setThumbnail("https://i.imgur.com/D9FsKgT.png")
						.setDescription(message2 + "\nLink : " + getSpecificChannel(guild, event.getMember(), channels[1]).getAsMention()).build())
			.complete().getId();
			Message msg = guild.getTextChannelById(logs).retrieveMessageById(id).complete();
			msg.addReaction("✅").queue();
			msg.addReaction("❌").queue();
			waiter.waitForEvent(MessageReactionAddEvent.class,
					//create new variable e handling MessageReceivedEvent (condition predicate) if it's the same channel and author
					e -> e.getMember().isOwner() && e.getMessageId().equals(id),
					//using e again consumer so in the case the condition predicate is true do that using e and event (two different event listeners) sending message
					e -> {
						if (e.getReactionEmote().getName().equals("✅")) {
							print(guild, "And the second text/answer is ?", true);
							askText(e, guild, message2, msg);
						} else {
							print(guild, "Cancelled.", false);
							msg.clearReactions().queue();
						}
					},
					10, TimeUnit.MINUTES, new Runnable() {
						public void run() {
							print(guild, "Expired. Use ib!send <message> to send it manually.", false);
							msg.clearReactions().queue();
						}
					});
	}
	
	private void askText(MessageReactionAddEvent e, Guild guild, String message2, Message msg) {
		waiter.waitForEvent(MessageReceivedEvent.class,
				m -> m.getAuthor().equals(e.getMember().getUser()),
				m -> {
					String txt2 = m.getMessage().getContentRaw();
					String id2 = guild.getTextChannelById(logs)
							.sendMessage("Are you sure to send this to everyone?")
							.complete().getId();
							Message msg2 = guild.getTextChannelById(logs).retrieveMessageById(id2).complete();
							msg2.addReaction("✅").queue();
							msg2.addReaction("❌").queue();
							waiter.waitForEvent(MessageReactionAddEvent.class,
									j -> j.getMember().equals(e.getMember()) && j.getMessageId().equals(id2),
									j -> {
										if (j.getReactionEmote().getName().equals("✅")) {
											print(guild, "Sent.", true);
											sendForEach(guild, true, "Question anonyme", channels[4], message2, txt2, null);
										} else {
											print(guild, "Retype new message.", true);
											askText(e, guild, message2, msg);
										}
									}, 120, TimeUnit.SECONDS, new Runnable() {
										@Override
										public void run() {
											print(guild, "Send expired.", false);
											msg2.clearReactions().queue();
										}
									});
				}, 10, TimeUnit.MINUTES, new Runnable() {
					@Override
					public void run() {
						print(guild, "Send expired.", false);
						msg.clearReactions().queue();
					}
				});
	}
	
	private void sendForEach(Guild guild, boolean question, String title, String channel, String message, String second, Member except) {
		guild.getMembers().forEach(member -> {
			if (!member.getUser().isBot() && (except == null ? true : !member.getUser().getId().equals(except.getUser().getId()))) {
				List<Category> may = member.isOwner() ? guild.getCategoriesByName(base, true) : guild.getCategoriesByName(member.getUser().getId(), true);
				Category category = may.isEmpty() ? null : may.get(0);
				if (category == null) return;
				HashMap<String, Integer> names = new HashMap<>();
				for (int l = 0; l < category.getTextChannels().size(); l++) {
					names.put(category.getTextChannels().get(l).getName(), l);
				}
				TextChannel tmp = (names.containsKey(channel) ? category.getTextChannels().get(names.get(channel)) : null);
				names.clear();
				if (tmp != null) {
					Random rand = new Random();
					float r = rand.nextFloat();
					float g = rand.nextFloat();
					float b = rand.nextFloat();
					Color color = new Color(r, g, b);
					tmp.sendMessage(new EmbedBuilder()
							.setTitle(title).setThumbnail(question ? "https://i.imgur.com/D9FsKgT.png" : "https://i.imgur.com/dUmEcYA.png")
							.setColor(color).setDescription(message)
							.setFooter(" : " + second, question ? "https://i.imgur.com/dUmEcYA.png" : "https://i.imgur.com/D9FsKgT.png").build())
					.queue();
				}
			}
		});
	}
	
	private void print(Guild guild, String message, boolean color) {
//		guild.getTextChannelById(logs).sendMessage(message).queue();
		guild.getTextChannelById(logs).sendMessage(new EmbedBuilder().setDescription(message)
				.setColor(color ? Color.ORANGE : Color.DARK_GRAY).build()).queue();
	}
	
	private String[] channels = {"informations", "général", "bases", "programmation", "questions"};
	private String[] ids = {"608678137156337674", "608677867349344257", "608679924638941210", "608679865373163532", "612410833292492860"};
//	private String[] ids = {"612305981564649474", "612288701875224604", "612289079010131968", "612289035792023566", "612375623578419206"};
	
	private boolean compare(Guild guild, TextChannel first, TextChannel second) {
		Iterator<Message> iterator = first.getIterableHistory().iterator();
		Iterator<Message> iterator2 = second.getIterableHistory().iterator();
		
		ArrayList<Message> origm = new ArrayList<>();
		ArrayList<Message> copym = new ArrayList<>();
		
		while (iterator2.hasNext()) {
			Message om = iterator2.next();
			origm.add(om);
		}
		while (iterator.hasNext()) {
			copym.add(iterator.next());
		}
		ArrayList<Message> compare = new ArrayList<>();
		boolean same = true;
		boolean reverse = false;
		Collections.reverse(origm);
		Collections.reverse(copym);
		if (origm.size() > copym.size()) {
			for (int j = copym.size(); j < origm.size(); j++) {
				compare.add(origm.get(j));
			}
//			System.out.println("considered " + origm.size() + " greater than " + );
			same = false;
		} else if (origm.size() < copym.size()) {
			for (int j = origm.size(); j < copym.size(); j++) {
				compare.add(copym.get(j));
			}
			reverse = true;
			same = false;
		} else {
			for (int j = 0; j < origm.size(); j++) {
				if (!origm.get(j).equals(copym.get(j)) || !(origm.get(j).getContentRaw().equals(copym.get(j).getContentRaw()) &&
						origm.get(j).getAttachments().equals(copym.get(j).getAttachments()) && origm.get(j).getEmbeds().equals(copym.get(j).getEmbeds()))) {
					if (origm.get(j).getAttachments().size() == copym.get(j).getAttachments().size()) {
						for (int k = 0; k < origm.get(j).getAttachments().size(); k++) {
							if (origm.get(j).getAttachments().get(k).getHeight() != copym.get(j).getAttachments().get(k).getHeight()
									|| origm.get(j).getAttachments().get(k).getWidth() != copym.get(j).getAttachments().get(k).getWidth()
									|| origm.get(j).getAttachments().get(k).getSize() != copym.get(j).getAttachments().get(k).getSize()
									|| !origm.get(j).getContentRaw().equals(copym.get(j).getContentRaw())) {
								compare.add(origm.get(j));
								same = false;
							}
						}
						if (same) {
							if (origm.get(j).getEmbeds().size() == copym.get(j).getEmbeds().size()) {
								for (int k = 0; k < origm.get(j).getEmbeds().size(); k++) {
									if (origm.get(j).getEmbeds().get(k).getTitle() != null && copym.get(j).getEmbeds().get(k).getTitle() != null) {
										if (!origm.get(j).getEmbeds().get(k).getTitle().equals(copym.get(j).getEmbeds().get(k).getTitle())) {
											compare.add(origm.get(j));
											same = false;
										}
									} else {
										if (origm.get(j).getEmbeds().get(k).getDescription() != null && copym.get(j).getEmbeds().get(k).getDescription() != null) {
											if (!origm.get(j).getEmbeds().get(k).getDescription().equals(copym.get(j).getEmbeds().get(k).getDescription())) {
												compare.add(origm.get(j));
												same = false;
											}
										}
									}
								}
							} else {
								compare.add(origm.get(j));
								same = false;
							}
						}
					} else {
						compare.add(origm.get(j));
						same = false;
					}
				}
			}
		}
		return same;
	}
	
	private void copyChannel(Member member, boolean all) {
		if (member.isOwner() || member.getUser().isBot()) return;
		Guild guild = member.getGuild();
		
		ArrayList<Permission> deny = new ArrayList<>();
		deny.add(Permission.MESSAGE_READ);
		deny.add(Permission.CREATE_INSTANT_INVITE);
		deny.add(Permission.MESSAGE_WRITE);
		ArrayList<Permission> allow = new ArrayList<>();
		allow.add(Permission.MESSAGE_READ);
		allow.add(Permission.MESSAGE_HISTORY);
		for (int i = 0; i < channels.length; i++) {
			String channel = channels[i];
			String id = ids[i];
			List<Category> may = guild.getCategoriesByName(member.getUser().getId(), true);
			if (may != null) {
			if (!may.isEmpty()) {
				Category category = may.get(0);
				HashMap<String, Integer> names = new HashMap<>();
				for (int l = 0; l < category.getTextChannels().size(); l++) {
					names.put(category.getTextChannels().get(l).getName(), l);
				}
				TextChannel tmp = (names.containsKey(channel) ? category.getTextChannels().get(names.get(channel)) : null);
				names.clear();
				if (tmp != null) {
					if (i == 1) continue;
					TextChannel copy = tmp;
//					if (copy.getIterableHistory().isEmpty() || !copy.getIterableHistory().getLast().getContentRaw().equals(guild.getTextChannelsByName(channel, true).get(0).getIterableHistory().getLast().getContentRaw())) {
//						System.out.println(copy.getName() + " and " + guild.getTextChannelsByName(channel, true).get(0).getName() + " are not the same but exist.");
//					}
					Iterator<Message> iterator = copy.getIterableHistory().iterator();
					Category cate = guild.getCategoriesByName(base, true).get(0);
					for (int l = 0; l < cate.getTextChannels().size(); l++) {
						names.put(cate.getTextChannels().get(l).getName(), l);
					}
					Iterator<Message> iterator2 = names.containsKey(channel) ? ((TextChannel) cate.getChannels().get(names.get(channel))).getIterableHistory().iterator() : null;
					names.clear();
					if (iterator2 == null) {
						print(guild, "Channel " + channel + " not found.", false);
						return;
					}
					
					ArrayList<Message> origm = new ArrayList<>();
					ArrayList<Message> copym = new ArrayList<>();
					
					while (iterator2.hasNext()) {
						Message om = iterator2.next();
						origm.add(om);
					}
					while (iterator.hasNext()) {
						copym.add(iterator.next());
					}
					ArrayList<Message> compare = new ArrayList<>();
					boolean same = true;
					boolean reverse = false;
					Collections.reverse(origm);
					Collections.reverse(copym);
					if (origm.size() > copym.size()) {
						for (int j = copym.size(); j < origm.size(); j++) {
							compare.add(origm.get(j));
						}
//						System.out.println("considered " + origm.size() + " greater than " + );
						same = false;
					} else if (origm.size() < copym.size()) {
						for (int j = origm.size(); j < copym.size(); j++) {
							compare.add(copym.get(j));
						}
						reverse = true;
						same = false;
					} else {
						for (int j = 0; j < origm.size(); j++) {
							if (!origm.get(j).equals(copym.get(j)) || !(origm.get(j).getContentRaw().equals(copym.get(j).getContentRaw()) &&
									origm.get(j).getAttachments().equals(copym.get(j).getAttachments()) && origm.get(j).getEmbeds().equals(copym.get(j).getEmbeds()))) {
								if (origm.get(j).getAttachments().size() == copym.get(j).getAttachments().size()) {
									for (int k = 0; k < origm.get(j).getAttachments().size(); k++) {
										if (origm.get(j).getAttachments().get(k).getHeight() != copym.get(j).getAttachments().get(k).getHeight()
												|| origm.get(j).getAttachments().get(k).getWidth() != copym.get(j).getAttachments().get(k).getWidth()
												|| origm.get(j).getAttachments().get(k).getSize() != copym.get(j).getAttachments().get(k).getSize()
												|| !origm.get(j).getContentRaw().equals(copym.get(j).getContentRaw())) {
											compare.add(origm.get(j));
											same = false;
										}
									}
									if (same) {
										if (origm.get(j).getEmbeds().size() == copym.get(j).getEmbeds().size()) {
											for (int k = 0; k < origm.get(j).getEmbeds().size(); k++) {
												if (origm.get(j).getEmbeds().get(k).getTitle() != null && copym.get(j).getEmbeds().get(k).getTitle() != null) {
													if (!origm.get(j).getEmbeds().get(k).getTitle().equals(copym.get(j).getEmbeds().get(k).getTitle())) {
														compare.add(origm.get(j));
														same = false;
													}
												} else {
													if (origm.get(j).getEmbeds().get(k).getDescription() != null && copym.get(j).getEmbeds().get(k).getDescription() != null) {
														if (!origm.get(j).getEmbeds().get(k).getDescription().equals(copym.get(j).getEmbeds().get(k).getDescription())) {
															compare.add(origm.get(j));
															same = false;
														}
													}
												}
											}
										} else {
											compare.add(origm.get(j));
											same = false;
										}
									}
								} else {
									compare.add(origm.get(j));
									same = false;
								}
							}
						}
					}
					
					if (!same) {
						if (all) {
							Iterator<Message> iterator3 = copy.getIterableHistory().iterator();
							while (iterator3.hasNext()) {
								Message message = iterator3.next();
								message.delete().queue();
							}
							ArrayList<Message> messages = new ArrayList<>();   
							Iterator<Message> iterator4 = guild.getTextChannelsByName(channel, true).get(0).getIterableHistory().iterator();
							while (iterator4.hasNext()) { 
								Message message = iterator4.next();
								if (message == null) continue;
								if (message.getContentRaw().isEmpty() && message.getAttachments().isEmpty() && message.getEmbeds().isEmpty()) continue;
								messages.add(message);
							}
							Collections.reverse(messages);
							messages.forEach(m -> {
								if (!m.getAttachments().isEmpty()) {
									m.getAttachments().forEach(e -> {
 										File file = new File(e.getId() + e.getFileName().substring(e.getFileName().indexOf("."), e.getFileName().length()));
 										if (!file.exists()) {
 											e.downloadToFile(file).thenAccept(f -> {
 												copy.sendMessage(m).addFile(f).complete();
 											});
 										} else {
 											copy.sendMessage(m).addFile(file).complete();
 										}
									});
								} else if (!m.getContentRaw().isEmpty() || !m.getEmbeds().isEmpty())
									copy.sendMessage(m).complete();
							});
							print(guild, "Updating all : " + copy.getAsMention() + " (" + member.getEffectiveName() + ") (" + messages.size() + " messages)", true); 
						} else {
							if (!compare.isEmpty()) {
//								Collections.reverse(compare);
								if (!reverse) {
									compare.forEach(m -> {
										if (!m.getAttachments().isEmpty()) {
											m.getAttachments().forEach(e -> {
												File file = new File(e.getId() + e.getFileName().substring(e.getFileName().indexOf("."), e.getFileName().length()));
												if (!file.exists()) {
													e.downloadToFile(file).thenAccept(f -> {
														copy.sendMessage(m).addFile(f).complete();
													});
												} else {
													copy.sendMessage(m).addFile(file).complete();
												}
											});
										} else if (!m.getContentRaw().isEmpty() || !m.getEmbeds().isEmpty()) {
											copy.sendMessage(m).complete();
										}
									});
									print(guild, "Updating last (non reverse) : " + copy.getAsMention() + " (" + member.getEffectiveName() + ")", true);
								} else {
									compare.forEach(m -> {
										m.delete().queue();
									});
									print(guild, "Updating last (reverse (delete)) : " + copy.getAsMention() + " (" + member.getEffectiveName() + ")", true);
								}
							}
						}
					} else print(guild, "Updating " + copy.getAsMention() + " (" + member.getEffectiveName() + ") not required.", false);
					continue;
				}
			}
		}
			
			if (i == 1) allow.add(Permission.MESSAGE_WRITE);
			else allow.remove(Permission.MESSAGE_WRITE);
			TextChannel temp = guild.getTextChannelById(id);
			Category category = guild.getCategoriesByName(member.getUser().getId(), true).isEmpty() ? guild.createCategory(member.getUser().getId()).complete() :
				guild.getCategoriesByName(member.getUser().getId(), true).get(0);
			TextChannel copy2 = null;
			if (!guild.getTextChannelsByName(channel, true).isEmpty()) {
				for (TextChannel c : guild.getTextChannelsByName(channel, true)) {
					if (c.getParent().getName().equals(member.getUser().getId())) {
						copy2 = c;
						break;
					}
				}
			}
			if (copy2 == null) {
				copy2 = (TextChannel) guild.createCopyOfChannel(temp)
					.setName(temp.getName())
					.setParent(category)
					.setTopic(temp.getTopic())
					.addPermissionOverride(guild.getPublicRole(), new ArrayList<Permission>(), deny)
					.addPermissionOverride(member, allow, (i == 1 ? new ArrayList<Permission>() : Arrays.asList(Permission.MESSAGE_WRITE))).complete();
			}
			TextChannel copy = copy2;
			if (i != 1) {
				TextChannel original = guild.getTextChannelsByName(channel, true).get(0);
				Iterator<Message> iterator = original.getIterableHistory().iterator();
				ArrayList<Message> messages = new ArrayList<Message>();
				while (iterator.hasNext()) {
					Message message = iterator.next();
					if (message == null) continue;
					if (message.getContentRaw().isEmpty() && message.getAttachments().isEmpty() && message.getEmbeds().isEmpty()) continue;
					messages.add(message);
				}
				Collections.reverse(messages);
				messages.forEach(m -> {
					if (!m.getAttachments().isEmpty()) {
						m.getAttachments().forEach(e -> {
							File file = new File(e.getId() + e.getFileName().substring(e.getFileName().indexOf("."), e.getFileName().length()));
							if (!file.exists()) {
								e.downloadToFile(file).thenAccept(f -> {
									copy.sendMessage(m).addFile(f).complete();
								});
							} else {
								copy.sendMessage(m).addFile(file).complete();
							}
						});
					} else if (!m.getContentRaw().isEmpty() || !m.getEmbeds().isEmpty())
						copy.sendMessage(m).complete();
				});
			} else {
				((TextChannel) copy).sendMessage("Utilisez la commande `!ask` ici pour poser une question.").queue();
			}
			print(guild, "Copied : " + channel + " for " + member.getUser().getId() + " (" + member.getEffectiveName() + ")", true);
		}
	}
}
