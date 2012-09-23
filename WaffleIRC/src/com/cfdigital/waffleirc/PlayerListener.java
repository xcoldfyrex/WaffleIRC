package com.cfdigital.waffleirc;
     
import java.net.InetAddress;
import java.util.logging.Logger;
     
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;

import com.cfdigital.waffleirc.GamePlayer;
import com.cfdigital.waffleirc.SyrupLink;
     
public class PlayerListener implements Listener{

	private final WaffleIRC plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
     
	public PlayerListener(WaffleIRC instance) {
		plugin = instance;
	}
	
	@EventHandler(priority = EventPriority.HIGH)             
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event){
		final InetAddress ipAddress = event.getAddress();
		String message = event.getKickMessage();
		final String player = event.getName();
		final Result result = event.getResult();
		SyrupLink.writeSocket(":000 PRIVMSG " + SyrupLink.consoleChannelName+ " :CONNECT: "+ player + " ["+ipAddress+"]");
		if (result == Result.ALLOWED) {
			SyrupLink.writeSocket(":000 PRIVMSG " + SyrupLink.consoleChannelName+ " :JOIN: "+ player);
		} else
		{
			SyrupLink.writeSocket(":000 PRIVMSG " + SyrupLink.consoleChannelName+ " :DENY: "+ player + " " + message);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		final Player player = event.getPlayer();
		if (plugin.hasPermissions(player, "wirc.mode.aop")) {
			BukkitEvents.addBukkitUser("a", player);
		} else if (plugin.hasPermissions(player, "wirc.mode.op")) {
			BukkitEvents.addBukkitUser("o", player);
		} else if (plugin.hasPermissions(player, "wirc.mode.hop")) {
			BukkitEvents.addBukkitUser("h", player);
		} else if (plugin.hasPermissions(player, "wirc.mode.voice")) {
			BukkitEvents.addBukkitUser("v", player);
		}  else {
			BukkitEvents.addBukkitUser("", player);
		}

	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		String name = event.getPlayer().getName();
		String reason = event.getQuitMessage();
		BukkitEvents.removeBukkitUser(name, reason);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerKick(PlayerKickEvent event)
	{
		if (event.isCancelled()) return;
		final Player player = event.getPlayer();
		final String kickReason = event.getReason();
		GamePlayer bp = BukkitEvents.bukkitPlayers.get(player.getName());
		bp.quitReason = "Kicked: "+kickReason;

		IRCDEvents.SendIRCDPRIVMSG(SyrupLink.channelName, "Player " + player.getName() + " was kicked: " + kickReason);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		if (event.isCancelled()) return;
		Boolean cancel = false;
    	Boolean kick = false;
    	String reason = "chat filter";
    	Boolean command = false;
    	Boolean matched = false;
    	String regex = "";
    	String matched_msg = "";
    	Boolean log = false;
    	String warning = "";
    	Boolean aborted = false;
    	Boolean valid;
    	final Player player = event.getPlayer();
		String message = event.getMessage();
    	for (String line : Filter.regexRules) {
    		if (aborted) { break; } 
    		valid = false;
    		if (line.startsWith("match ")) {
    			regex = line.substring(6);
    			matched = Filter.matchPattern(message, regex);
    			if (matched) {
    				matched_msg = message;
    			}
    			valid = true;
    		}
    		if (matched) {

        		
        		if (line.startsWith("require permission ")) {
        			String check = line.substring(19);
    				valid = true;
        			if (WaffleIRC.thePlugin.hasPermissions(player, check) == false) {
        				matched = false;
        			}
        		}
				if (line.startsWith("then replace ") || line.startsWith("then rewrite ")) {
					message = Filter.replacePattern(message, regex, line.substring(13));
	    			valid = true;
				}
				if (line.matches("then replace")) {
					message = Filter.replacePattern(message, regex, "");
	    			valid = true;
				}
				if (line.startsWith("then warn ")) {
					warning = line.substring(10);
	    			valid = true;
				}
				if (line.matches("then warn")) {
					warning = event.getMessage();
	    			valid = true;
				}
				if (line.matches("then log")) {
					log = true;
	    			valid = true;
				}
				if (line.startsWith("then command ")) {
					message = line.substring(13).concat(" " + message);
					command = true;
	    			valid = true;
				}
				if (line.matches("then command")) {
					command = true;
	    			valid = true;
				}
				if (line.matches("then debug")) {
					System.out.println("[Filter] Debug match: " + regex);
					System.out.println("[Filter] Debug original: " + event.getMessage());
					System.out.println("[Filter] Debug matched: " + matched_msg);
					System.out.println("[Filter] Debug current: " + message);
					System.out.println("[Filter] Debug warning: " + (warning.equals("")?"(none)":warning));
					System.out.println("[Filter] Debug log: " + (log?"yes":"no"));
					System.out.println("[Filter] Debug deny: " + (cancel?"yes":"no"));
	    			valid = true;
				}
				if (line.startsWith("then deny")) {
					cancel = true;
	    			valid = true;
				}
				if (line.startsWith("then kick ")) {
					reason = line.substring(10);
	    			valid = true;
				}
				if (line.startsWith("then kick")) {
					kick = true;
	    			valid = true;
				}
				if (line.startsWith("then abort")) {
					aborted = true;
	    			valid = true;
				}
	    		if (valid == false) {
	    			logger.warning("[Filter] Ignored syntax error in rules.txt: " + line);    			
	    		}
    		}
    	}
    	
    	// Perform flagged actions
    	if (log) {
    		logger.info("[Filter] " +  player.getName() + "> " + event.getMessage());
    	}
    	if (!warning.matches("")) {
    		player.sendMessage("§4[Filter] " + warning);
    	}

    	if (cancel == true) {
    		event.setCancelled(true);
    	}

    	if (command == true) {
			// Convert chat message to command
			event.setCancelled(true);
			logger.info("[Filter] Helped " + player.getName() + " execute command: " + message);
			player.chat("/" + message);
		} else {
			event.setMessage(message);
		}
    	
    	if (kick) {
    		player.kickPlayer(reason);
    		logger.info("[Filter] Kicked " + player.getName() + ": " + reason);
    		IRCDEvents.SendIRCDPRIVMSG(SyrupLink.channelName, "Player " + player.getName() + " was kicked: " + reason);
    		return;
    	}
		if (event.isCancelled()) return;
		SyrupLink.IRCDPlayerChat(player, Format.convertColors(message, false));
		event.setCancelled(true);
		String prefix = "";
		String sender = event.getPlayer().getName();
		String targetChan = BukkitEvents.bukkitPlayers.get(sender).activeChannel;
		if (!targetChan.equals("")) {
			prefix = "§c[§4" + targetChan + "§c] ";
		}
		WaffleIRC.thePlugin.getServer().getConsoleSender().sendMessage(prefix + event.getFormat());
		for (Player searchPlayer : plugin.getServer().getOnlinePlayers()) {
			if (BukkitEvents.bukkitPlayers.get(searchPlayer.getName()).activeChannel.equalsIgnoreCase(targetChan)) {
				if (targetChan.equals("")) {
					searchPlayer.sendMessage(event.getFormat());
				}
				else {
					searchPlayer.sendMessage("§c[§4" + targetChan + "§c] " + event.getFormat());
				}
			}
		} 
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent event) {
		final String message = event.getDeathMessage();
		SyrupLink.IRCDSendGameEvent(SyrupLink.channelName,message);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
	{
		if (event.isCancelled()) return;

		String[] split = event.getMessage().split(" ");
		if (split[0].equalsIgnoreCase("/me")) {
			SyrupLink.writeSocket(":"+event.getPlayer().getName()+" PRIVMSG "+SyrupLink.channelName+" :"+(char)1+"ACTION "+Format.convertColors(Format.join(event.getMessage().split(" ")," ",1), false)+(char)1);
		}
		SyrupLink.writeSocket(":000 PRIVMSG " + SyrupLink.consoleChannelName+ " :COMMAND: "+event.getPlayer().getName() + ": "+event.getMessage()) ;
		event.setMessage(Format.stripFormatting(event.getMessage()));
	}
             
}
