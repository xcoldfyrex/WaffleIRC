package com.cfdigital.waffleirc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.cfdigital.waffleirc.PlayerListener;
import com.cfdigital.waffleirc.SyrupLink;
import com.cfdigital.waffleirc.BukkitEvents;

public class WaffleIRC extends JavaPlugin

{	static class CriticalSection extends Object {
	}
	static public CriticalSection csLastReceived = new CriticalSection();

	static SyrupLink ircd = null;
	private Thread thread = null;
	public static WaffleIRC thePlugin = null;
    public static Economy econ = null;
    public static Permission perms = null;
    public static Chat chat = null;
    public static String pluginPrefix = "§6[WaffleIRC]§f ";

	private final PlayerListener playerListener = new PlayerListener(this);
	private final Logger log = Logger.getLogger("Minecraft");
	
	//this is chat channels, NOT IRC
	public static HashMap<String, Channel> channelList = new HashMap<String, Channel>();
	
	public WaffleIRC() {
		thePlugin = this;
	}

	public void onEnable()
	{
		Config config = new Config();
		
		if (config.loadLinkSettings()) {
			
		}
		config.loadFilters();
		setupPermissions();
		setupChat();
		if (perms == null) log.severe(pluginPrefix + "CANNOT LOAD PERMISSIONS");
		ircd = new SyrupLink();
		thread = new Thread(ircd);
		thread.start();
		
        //enumerate/add online players
        String mode = "";
		for (Player player : getServer().getOnlinePlayers()) {
			if (hasPermissions(player, "wirc.mode.aop")) {
				mode = "a";
			} else if (hasPermissions(player, "wirc.mode.op")) {
				mode = "o";
			} else if (hasPermissions(player, "wirc.mode.hop")) {
				mode = "h";
			} else if (hasPermissions(player, "wirc.mode.voice")) {
				mode = "v";
			}  else {
				mode = "";
			}
			BukkitEvents.addBukkitUser(mode,player);
		}
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(playerListener, this);

	
	}
	
	public void onDisable() {
		SyrupLink.burstSent = false;
		SyrupLink.capabSent = false;
		SyrupLink.running = false;
		ircd = null;
		SyrupLink.IRCDLinkClose();
		if (thread != null) thread.interrupt();
		thread = null;
		
		log.info(pluginPrefix + "Disabled!");
	}
	
    
	
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
    
    private boolean setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }

        return (chat != null);
    }
    
    public String getPrefix(Player player) {
    	return chat.getPlayerPrefix(player);
    }
    
    public boolean hasPermissions(Player p, String s) {
        if (perms != null) {
        	return perms.has(p, s);
        } else {
        	return false;
        }
    }
    //@Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    	String commandName = command.getName().toLowerCase();
        String[] trimmedArgs = args;
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (commandName.equalsIgnoreCase("ircdebug")) {
                if (hasPermissions(player, "wirc.admin") || (player == null)) {
                    boolean state = Config.debugmode;
                    if (state) {
                    	player.sendMessage(pluginPrefix + "Debug mode OFF!");
						IRCDEvents.SendIRCDPRIVMSG(SyrupLink.consoleChannelName, "***INFO: " + player.getName() + " toggled debug to false"); 
						Config.debugmode = false;
                    }
                    else {
						IRCDEvents.SendIRCDPRIVMSG(SyrupLink.consoleChannelName, "***INFO: " + player.getName() + " toggled debug to true"); 
                    	player.sendMessage(pluginPrefix + "Debug mode ON!");
                    	Config.debugmode = true;
                    }  
                    return true;
                }
               return false;
                    
            }
            if ((commandName.equalsIgnoreCase("irc") && trimmedArgs.length == 0) || (trimmedArgs.length == 1 && trimmedArgs[0].equalsIgnoreCase("help"))) {
            	player.sendMessage("§f-----  §6Help for " + Config.serverVersion+"§f  ------");
            	player.sendMessage("§6/ircdebug§f - Toggle debugging");
            	player.sendMessage("§6/ircrelink§f - Force delink/relink");
            	player.sendMessage("§6/ircreload §f - Reload configs");
            	player.sendMessage("§6/ircreply | /ir§f - Reply to last privmsg on IRC");
            	player.sendMessage("§6/ircmsg | /im <person>§f - Send privmsg to person on IRC");
            	player.sendMessage("§6/irclist | /il§f - List users on IRC");
            	player.sendMessage("§6/ircinfo | /ii§f - Get channel info on IRC");
            	player.sendMessage("§6/irchide | /ih §f - Hide IRC events for yourself");
            	player.sendMessage("§6/irckick | /ik <person>§f - Kick person on IRC");       		
            	return true;
            }
                
            if (commandName.equalsIgnoreCase("ircrelink") && hasPermissions(player, "wirc.admin")) {
            	player.sendMessage(pluginPrefix + "Attempting relink...");
                SyrupLink.IRCDLinkClose();
            	SyrupLink.IRCDLinkStart();
                return true;
            }
                	
            if (commandName.equalsIgnoreCase("ircreload") && hasPermissions(player, "wirc.admin")) {
                Config config = new Config();
            	config.loadLinkSettings();
            	player.sendMessage(pluginPrefix + "Reloaded WaffleIRC config!");
            	return true;
            }

            if (commandName.equalsIgnoreCase("ih")) {
               	GamePlayer person = BukkitEvents.bukkitPlayers.get(player.getName());
               	if (person.hideIRC) {
               		person.hideIRC = false;
               		player.sendMessage(pluginPrefix + "IRC events are now §6ENABLED§f for you");
               		return true;
               	} 
               	else {
               		person.hideIRC = true;
               		player.sendMessage(pluginPrefix + "IRC events are now §6DISABLED§f for you");
               		return true;
               	}
            }
                
            if (commandName.equalsIgnoreCase("il")) {
            	showAllMembers(player);
            	return true;
            }
                
            if (commandName.equalsIgnoreCase("im")) {
            	if (trimmedArgs.length >= 2) {
            		if (hasPermissions(player, "wirc.message")) {
            			String ircPrivMsg = Config.ircPrivMsgSend;
                		ircPrivMsg = ircPrivMsg.replace("%username%", trimmedArgs[0]);
                		ircPrivMsg = ircPrivMsg.replace("%message%", Format.join(trimmedArgs, " ", 1));
                		player.sendMessage(ircPrivMsg);
                		SyrupLink.IRCDPlayerMSG(player, Format.join(trimmedArgs, " ", 1), trimmedArgs[0]);
                		return true;
            		}
            		else {
            			player.sendMessage(pluginPrefix + "No permissions");
                       	return false;
               		}
               	}
               	return false;
            }
        	
            if (commandName.equalsIgnoreCase("ir") && hasPermissions(player, "wirc.message")) {
               	if (BukkitEvents.bukkitPlayers.get(player.getName()).lastIRCTarget != "") {
               		String Message = "";
                   	if (commandName.equalsIgnoreCase("irc")) {  
                   		Message = Format.join(trimmedArgs, " ", 1);
                   	}
                   	else {
                   		Message = Format.join(trimmedArgs, " ", 0);
                   	}
               		String Target = BukkitEvents.bukkitPlayers.get(player.getName()).lastIRCTarget;
					String ircPrivMsg = Config.ircPrivMsgSend;
					ircPrivMsg = ircPrivMsg.replace("%username%", Target);
					ircPrivMsg = ircPrivMsg.replace("%message%", Message);
					player.sendMessage(ircPrivMsg);
            		SyrupLink.IRCDPlayerMSG(player, Message, Target);
                    return true;
               	}
               	else {
               		player.sendMessage(pluginPrefix + "Noone has message you! Maybe you mean /im");
                    return true;
               	}
            }
                
            if (commandName.equalsIgnoreCase("join")) {
            	if (trimmedArgs.length == 1) {
            		if (hasPermissions(player, "wirc.channels")) {
            			GamePlayer GP = BukkitEvents.bukkitPlayers.get(player.getName());
            			trimmedArgs[0] = trimmedArgs[0].replaceAll("#", "");    
            			if (trimmedArgs[0].equalsIgnoreCase("lobby")) {
            				if (GP.activeChannel.equals("")) return true; 
            				GP.activeChannel = "";
                   			player.sendMessage("§6You have returned to the lobby");
                   			getServer().broadcastMessage("§c" + player.getName() + "§6 has joined the lobby");
            				return true;
            			}
            			if (GP.activeChannel.equals("")) {
            				getServer().broadcastMessage("§c" + player.getName() + "§6 has left the lobby");
            			}
            			if (channelList.get("#" + trimmedArgs[0]) == null) {
            				Channel channel = new Channel(trimmedArgs[0]);
            				channelList.put("#" + trimmedArgs[0], channel);
            				channel.joinChannel(GP);
            			}
            			else {
            				channelList.get("#" + trimmedArgs[0]).joinChannel(GP);
            			}
            			return true;
            		}
            		else {
                   		player.sendMessage(pluginPrefix + "No permissions");
                   		return true;
            		}
            	}
            }
            if (commandName.equalsIgnoreCase("part")) {
            	if (trimmedArgs.length == 1) {
            		if (hasPermissions(player, "wirc.channels")) {
            			trimmedArgs[0] = trimmedArgs[0].replaceAll("#", "");
            			GamePlayer GP = BukkitEvents.bukkitPlayers.get(player.getName());
            			if (channelList.get("#" + trimmedArgs[0]) != null) {
            				channelList.get("#" + trimmedArgs[0]).partChannel(GP);
            			}
            			
               			return true;
               		}
               		else {
                    	player.sendMessage(pluginPrefix + "No permissions");
                    	return true;
               		}
               	}
            }	   
            
            if (commandName.equalsIgnoreCase("lobby")) {
            	if (hasPermissions(player, "wirc.channels")) {
            		if (!BukkitEvents.bukkitPlayers.get(player.getName()).activeChannel.equalsIgnoreCase("")) {
            			BukkitEvents.bukkitPlayers.get(player.getName()).activeChannel = "";
               			player.sendMessage("§b*You have returned to the lobby");
               			getServer().broadcastMessage("§b" + player.getName() + " has joined the lobby");
            		}
               		return true;
               		
               	}
               	else {
                   	player.sendMessage(pluginPrefix + "No permissions");
                   	return true;
               	}
            }
            
            if (commandName.equalsIgnoreCase("channel")) {
            	if (trimmedArgs.length == 0) {
                	player.sendMessage("§f-----  §6Help for " + Config.serverVersion+"§f channels ------");
                	player.sendMessage("§6/part <channel>§f - Part <channel>");
                	player.sendMessage("§6/join <channel>§f - Join <channel> or set <channel> as active");
                	player.sendMessage("§6/lobby§f - Return to lobby(but keep channels open)");
                	player.sendMessage("§6/channel list§f - Shows your open channels");
                	player.sendMessage("§6/channel who§f - Shows members of current channel");
                	player.sendMessage("§6You will only see messages from your active channel. Users active in the channel are marked with an §cA");
                	return true;
            	}
            	if (trimmedArgs.length == 1) {
            		if (trimmedArgs[0].equalsIgnoreCase("list")) {
            			if (channelList.isEmpty()) {
            				player.sendMessage("§bYou are not in any channels"); 
            				return true;
            			}
            			String chanList = "";
            			for (String channels : channelList.keySet()) {
            				if (channelList.get(channels).isNameMember(player.getName())) {
            					chanList = chanList + "§6" + channels + "§b, ";
            				}
            			}
        				player.sendMessage("§bYour channels: " + chanList); 

            			return true;
            		}
            		
            		if (trimmedArgs[0].equalsIgnoreCase("who")) {
                    	showAllMembers(player);
            			return true;
            		}
            	}
            }
            player.sendMessage(pluginPrefix + "No permissions or unhandled command!?..");
            return true;	
            
            
        }
        return true;        
    }
    
    public void showAllMembers(Player player) {
    	GamePlayer gp = BukkitEvents.bukkitPlayers.get(player.getName());
    	String activeChan = gp.activeChannel;
    	String gamePlayers[];
    	if (!gp.activeChannel.equals("")) {
        	gamePlayers = channelList.get(activeChan).getMemberList();
    		activeChan = gp.activeChannel+"/mc";
    		
    	}
    	else {
    		List<String> users = new ArrayList<String>();
            for (Player players : getServer().getOnlinePlayers()) {
           		users.add(players.getName());
            }
            String userArray[] = new String[0];
            userArray = users.toArray(userArray);
            Arrays.sort(userArray);
            gamePlayers = userArray;
            activeChan = SyrupLink.channelName;
        }

    	String myChan = BukkitEvents.bukkitPlayers.get(player.getName()).activeChannel;
    	String players[] = SyrupLink.getIRCNicks(gp);
        String allplayers = "";
        String allGamePlayers = "";
        for (String curplayer : players) allplayers += "§7"+curplayer+"§f, ";
        for (String curplayer : gamePlayers) allGamePlayers += "§7"+curplayer+"§f, ";

		if (myChan.equalsIgnoreCase("")) myChan = "[LOBBY]";
        player.sendMessage("§6There are §c"+players.length+" §6IRC users and §c" + gamePlayers.length + "§6 minecraft players in §c" + activeChan);
        if (players.length > 0) player.sendMessage("§6IRC:§7 " + allplayers.substring(0,allplayers.length()-2) + " §6Minecraft:§7 " + allGamePlayers.substring(0,allGamePlayers.length()-2));
       
    }

}