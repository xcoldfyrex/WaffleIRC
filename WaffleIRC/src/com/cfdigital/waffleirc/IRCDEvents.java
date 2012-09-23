/*
All non link related events
*/

package com.cfdigital.waffleirc;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import com.cfdigital.waffleirc.Format;

public class IRCDEvents {
	
	public static String PRIVMSGTarget = null;
	public static String PRIVMSGMsg = null;
	public static String GenericMsg = "";
	public static String IRCDCommand = null;
	public static String IRCDEventSrc = null;
	public static String IRCDData = null;
	public static IRCUser ircUser = null;
	public static long IRCDEventSrcTS = 0;
	public static String botName = "WaffleBot" + System.currentTimeMillis() / 1000L; 
	public static List<String> knownChannels = new ArrayList<String>();

	static Logger log = Logger.getLogger("Minecraft");

	public static void LinkCommandParse(String Command, String Data){
		
		IRCDData = Data;
		IRCDCommand = Command;
		String[] split = IRCDData.split(" ");
		split = IRCDData.split(" ");
		if (split[0].startsWith(":")) split[0] = split[0].substring(1);
		
		if (IRCDCommand.startsWith("ERROR")) {
			SyrupLink.IRCDLinkClose();
		}
	
		//Got a PING
		if (IRCDCommand.startsWith("PING")) {
			SyrupLink.writeSocket(SyrupLink.pre+"PONG 000 "+ SyrupLink.remoteSID);
			SyrupLink.wasPing = true;
		}
	
		// Someone got kicked, lol
		if (IRCDCommand.startsWith("KICK")) {
			if (split[0].startsWith(":")) split[0] = split[0].substring(1);
			String sourceChannel = split[2];
			IRCUser ircUserSender = SyrupLink.IRCUsers.get(split[0]);
			IRCUser ircUserTarget = SyrupLink.IRCUsers.get(split[3]);
			if (ircUserTarget != null) ircUserTarget.removeChannel(sourceChannel);
			String kickMsg = Config.ircKick;
			kickMsg = kickMsg.replace("%username%", ircUserSender.nick);
			kickMsg = kickMsg.replace("%target%", ircUserTarget.nick);
			kickMsg = kickMsg.replace("%channel%", split[2]);
			//fix reason
			kickMsg = kickMsg.replace("%reason%", "");
			BukkitEvents.sendGlobalMsg(kickMsg, split[2]);
		}
		
		//Special proto, CONFIG
		if (Command.startsWith("CONFIG")) {
			String temp = Format.join(split, " " , 2);
			String config[] = temp.split(" ");
			int i = 0;
			while (i < config.length) {
				String var[] = config[i].split("=");
				log.warning(var[0] + " " + var[1]);
				if (var[0].equals("BOTNAME")) botName = var[1];
				if (var[0].equals("LOBBY")) {
					SyrupLink.channelName = var[1];
					knownChannels.add(var[1]);
				}
				if (var[0].equals("CONSOLE")) SyrupLink.consoleChannelName = var[1];
				i++;
			}
		}
		
		//NICK
		if (split[1].equalsIgnoreCase("NICK")) {
			IRCUser ircuser;
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if ((ircuser = SyrupLink.IRCUsers.get(split[0])) != null) {
					if (ircuser.isInChannel(SyrupLink.channelName)) {
						String ircNickChange = Config.ircNickChange;
						ircNickChange = ircNickChange.replace("%username%", ircuser.nick);
						ircNickChange = ircNickChange.replace("%newname%", split[2]);
						BukkitEvents.sendGlobalMsg(ircNickChange, SyrupLink.channelName);			
					} 
					else {
						//we need to iterate each user here... one off ugly ass way
						//will do this later....nicks won't be sent to people not in lobby
					}
					SyrupLink.IRCUsers.remove(split[0]);
					SyrupLink.IRCUsers.put(split[2], ircuser);
					SyrupLink.IRCUsers.get(split[2]).nick = split[2];
					
			}
			else { 
				log.severe("[WIRC] UID NICK "+split[2]+" not found in list..");
				SendIRCDPRIVMSG(SyrupLink.consoleChannelName, "04SEVERE ERROR: UID NICK "+split[2]+" not found in list..");
			}

		}
		 
		//PART
		if (IRCDCommand.startsWith("PART")) {
			if (split[0].startsWith(":")) split[0] = split[0].substring(1);
			ircUser = SyrupLink.IRCUsers.get(split[0]);
			if (ircUser == null){
				SendIRCDPRIVMSG(SyrupLink.consoleChannelName, "04SEVERE ERROR: Cannot find UID from IRC PART source! - " + split[0]); 
			} 
			else {
				ircUser.removeChannel(split[2]);
				String reason = "leaving";
				reason = Format.join(split, " ", 3);
				if (reason.startsWith(":")) reason = reason.substring(1);

				String partMsg = Config.ircPartMsg;
				partMsg = partMsg.replace("%username%", ircUser.nick);
				partMsg = partMsg.replace("%ident%", ircUser.ident);
				partMsg = partMsg.replace("%hostmask%", ircUser.hostmask);
				partMsg = partMsg.replace("%channel%", split[2]);
				partMsg = partMsg.replace("%reason%", reason);
				BukkitEvents.sendGlobalMsg(partMsg, split[2]);
			}
		}
		
		//JOIN
		if (IRCDCommand.startsWith("FJOIN")) {
			if (!knownChannels.contains(split[2])) {
				knownChannels.add(split[2]);
			}
			String users[] = IRCDData.split(" ");
			for (String user : users) {
				if (!user.contains(",")) continue;
				String userSplit[] = user.split(",");
				IRCUser ircuser;
				if ((ircuser = SyrupLink.IRCUsers.get(userSplit[1])) != null) {
					//Does nothing until I do this better
					ircuser.addChannel(split[2]);
					ircuser.setChannelModes(split[2], userSplit[0]);
					if (SyrupLink.burstSent && Config.burstSpam) {
						String joinMsg = Config.ircJoinMsg;
						joinMsg = joinMsg.replace("%username%", ircuser.nick);
						joinMsg = joinMsg.replace("%ident%", ircuser.ident);
						joinMsg = joinMsg.replace("%hostmask%", ircuser.hostmask);
						joinMsg = joinMsg.replace("%channel%", split[2]);
						BukkitEvents.sendGlobalMsg(joinMsg, split[2]);
					}
				}
			}
		}
		
		// New user connected, add to IRC user list by UID);
		if (IRCDCommand.startsWith("UID")) {
			if (split.length == 5){
				IRCUser ircuser = new IRCUser(split[2], split[3], split[4], "");
				SyrupLink.IRCUsers.put(split[2], ircuser);
			}
		}

	
		//Sends message to game FROM IRC
		if (IRCDCommand.startsWith("PRIVMSG")) {
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if (split[0].startsWith(":")) split[0] = split[0].substring(1);

			IRCDEventSrc = split[2];
			GenericMsg = "";
			
			//handle ! commands, but don't filter not listed commands
			if(split[3].startsWith("!",0)) {
				if(split[3].equalsIgnoreCase("!players")) {
					for (String key : BukkitEvents.bukkitPlayers.keySet()) {
						Player player = BukkitEvents.bukkitPlayers.get(key).player;
						if (player != null) {
							//GenericMsg = GenericMsg + Format.convertColors(WaffleIRC.chat.getPlayerPrefix(player), false)  + player.getName() + ", " ;
							GenericMsg = GenericMsg + player.getName() + ", " ;						
						}
					}
					SendIRCDPRIVMSG(SyrupLink.channelName, "Players online: (" +BukkitEvents.bukkitPlayers.size() + ") "  + GenericMsg);
				} 
			
				if(split[3].equalsIgnoreCase("!player")) {
					if (split.length >= 5) {
						split[4].replaceAll("/mc", "");
						if (BukkitEvents.bukkitPlayers.get(split[4]) != null) {
							GamePlayer bp = BukkitEvents.bukkitPlayers.get(split[4]);
							long signedOn = bp.signedOn;
							SendIRCDPRIVMSG(SyrupLink.channelName, "Details for player " +split[4] );
							SendIRCDPRIVMSG(SyrupLink.channelName, "Time online: "+Format.formatIntoHHMMSS(System.currentTimeMillis() / 1000L - signedOn));
							SendIRCDPRIVMSG(SyrupLink.channelName, "Rank: " + bp.rank);
							SendIRCDPRIVMSG(SyrupLink.channelName, "World: " + bp.world);
						} else {
							SendIRCDPRIVMSG(SyrupLink.channelName, "Player is not in game..");		
						}
						
					}
				}
			} 
			else {
				IRCUser ircuser;
				ircuser = SyrupLink.IRCUsers.get(split[0]);
				if (ircuser == null){
					log.severe("[WIRC] Cannot find UID from IRC PRIVMSG source! - " + split[0] );
				} else
				{	
					if (split[3].startsWith((char)1+"ACTION")) {
						String message = Format.join(split, " ", 4);
						message = Format.convertColors(message, true);
						if ((BukkitEvents.bukkitPlayers.get(split[2])) != null) {
							log.warning(BukkitEvents.bukkitPlayers.get(split[2]) + " " + message);
							String ircPrivAction = Config.ircPrivActionGet;
							ircPrivAction = ircPrivAction.replace("%username%", ircuser.nick);
							ircPrivAction = ircPrivAction.replace("%ident%", ircuser.ident);
							ircPrivAction = ircPrivAction.replace("%hostmask%", ircuser.hostmask);
							ircPrivAction = ircPrivAction.replace("%channel%", split[2]);
							ircPrivAction = ircPrivAction.replace("%message%", message);
							BukkitEvents.bukkitPlayers.get(split[2]).player.sendMessage(ircPrivAction);
							BukkitEvents.bukkitPlayers.get(split[2]).lastIRCTarget = ircuser.nick;
						} 
						else {
							String ircChanAction = Config.ircChanAction;
							ircChanAction = ircChanAction.replace("%username%", ircuser.nick);
							ircChanAction = ircChanAction.replace("%ident%", ircuser.ident);
							ircChanAction = ircChanAction.replace("%hostmask%", ircuser.hostmask);
							ircChanAction = ircChanAction.replace("%channel%", split[2]);
							ircChanAction = ircChanAction.replace("%message%", message);
							BukkitEvents.sendGlobalMsg(ircChanAction, split[2]);
						}
					}
					else {
						String temp = Format.join(split, " ", 3);
						String message = Format.convertColors(temp, true);
						if ((BukkitEvents.bukkitPlayers.get(split[2])) != null) {
							String ircPrivMsg = Config.ircPrivMsgGet;
							ircPrivMsg = ircPrivMsg.replace("%username%", ircuser.nick);
							ircPrivMsg = ircPrivMsg.replace("%ident%", ircuser.ident);
							ircPrivMsg = ircPrivMsg.replace("%hostmask%", ircuser.hostmask);
							ircPrivMsg = ircPrivMsg.replace("%channel%", split[2]);
							ircPrivMsg = ircPrivMsg.replace("%message%", message);
							BukkitEvents.bukkitPlayers.get(split[2]).player.sendMessage(ircPrivMsg);
							BukkitEvents.bukkitPlayers.get(split[2]).lastIRCTarget = ircuser.nick;
						}
						else {
							String ircChanMsg = Config.ircChanMsg;
							ircChanMsg = ircChanMsg.replace("%username%", ircuser.nick);
							ircChanMsg = ircChanMsg.replace("%ident%", ircuser.ident);
							ircChanMsg = ircChanMsg.replace("%hostmask%", ircuser.hostmask);
							ircChanMsg = ircChanMsg.replace("%channel%", split[2]);
							ircChanMsg = ircChanMsg.replace("%message%", message);
							BukkitEvents.sendGlobalMsg(ircChanMsg, split[2]);
						}
					}
				}
			}
		}
		
		//FHOST	
		if (IRCDCommand.startsWith("FHOST")) {
			IRCUser ircuser;
			String host;
			host = split[2];
			if ((ircuser = SyrupLink.IRCUsers.get(split[0])) != null) {
				ircuser.hostmask = host;
			}
		}

		
		//FMODE
		if (IRCDCommand.startsWith("FMODE")) {
			if (split[2].equalsIgnoreCase(SyrupLink.channelName)) {
				String mode;
				String target = null;
				String finaltarget = "";
				mode = split[4];
				
				//iterate stacked modes and try to assign useful stuff
				if (split.length > 5 ) {
					IRCUser modetarget;
					if (split.length > 6) {
						for (int i = 5; split.length>i; i++ ) {
							modetarget = SyrupLink.IRCUsers.get(split[i]);
							if (modetarget == null) {
								//try if ingame user next
								//modetarget = SyrupLink.
								//someone set mode on bot
								if (split[i].equalsIgnoreCase(botName)) {
									target = botName;
								}
							} 
							else {
								target = modetarget.nick;
							}
							// was probably a ban set
							if (target == null) {
								target = split[i];
							}
							finaltarget = finaltarget + target + " ";
							target = null;
							modetarget = null;
						}
					}
					//just a single target
					else {
						modetarget = SyrupLink.IRCUsers.get(split[5]);
						if (modetarget == null) {
							//try if ingame user next
							//someone set mode on bot
							if (split[5].equalsIgnoreCase(botName)) {
								target = botName;
							}
						} 
						else {
							target = modetarget.nick;
						}
						// was probably a ban set
						if (target == null) {
							target = split[5];
						}
						finaltarget = target;
						SyrupLink.IRCUsers.get(split[5]).setChannelModes(split[2], mode);
					}
					
					/*
					TODO
					######################################################################################
					this need to be sent per user, not broadcast
					######################################################################################
					*/ 
					BukkitEvents.sendGlobalMsg("§e[IRC] " + split[3] + " set mode: " + mode + " " + finaltarget, SyrupLink.channelName );
				}
				//channel mode
				else {
					BukkitEvents.sendGlobalMsg("§e[IRC] " + split[3] + " set mode: " + mode,SyrupLink.channelName );
				}
			}
		}
		
		//QUIT
		if (IRCDCommand.startsWith("QUIT")) {
			IRCUser ircuser;
			String reason;
			if (split.length > 2) {
				reason = Format.join(split, " ", 2);
				if (reason.startsWith(":")) reason = reason.substring(1);
			}
			else reason = "";
			if ((ircuser = SyrupLink.IRCUsers.get(split[0])) != null) {			
				String quitMsg = Config.ircQuitMsg;
				quitMsg = quitMsg.replace("%username%", ircuser.nick);
				quitMsg = quitMsg.replace("%reason%", reason);
				quitMsg = quitMsg.replace("%ident%", ircuser.ident);
				quitMsg = quitMsg.replace("%hostmask%", ircuser.hostmask);
				quitMsg = quitMsg.replace("%reason%", reason);
				//this also needs to check channel mapping
				BukkitEvents.sendGlobalMsg(quitMsg, SyrupLink.channelName);		
			}
			SyrupLink.IRCUsers.remove(split[0]);
		}
	}
	
	//Sends PRIVMSG to IRC target FROM game
	public static void SendIRCDPRIVMSG(String Target, String Message) {
		PRIVMSGTarget = Target;
		GenericMsg = Message;
		SyrupLink.writeSocket(":%bot% PRIVMSG " + PRIVMSGTarget + " :" + GenericMsg);

	}
	
	
	
}