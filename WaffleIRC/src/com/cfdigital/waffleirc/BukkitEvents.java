package com.cfdigital.waffleirc;

import java.util.HashMap;

import org.bukkit.entity.Player;
import com.cfdigital.waffleirc.SyrupLink.CriticalSection;

public class BukkitEvents {
	
	public static CriticalSection csPlayers = new CriticalSection();
	public static HashMap<String, GamePlayer> bukkitPlayers = new HashMap<String, GamePlayer>();
	public static int channelTS = 0;

	
	//tell IRC they left
	public static boolean removeBukkitUser(String nick, String reason) {
		synchronized(csPlayers) {
			SyrupLink.writeSocket(":" + nick + " QUIT :" + Format.stripFormatting(reason));
			SyrupLink.writeSocket(":000 PRIVMSG " + SyrupLink.consoleChannelName+ " :QUIT: "+ nick + " " + Format.stripFormatting(reason));
			bukkitPlayers.remove(nick);
			return true;
			
		}
	}
	
	
	public static boolean addBukkitUser(String modes,Player player) {
		String nick = player.getName();
		String world = player.getWorld().getName();
		String rank = WaffleIRC.perms.getPrimaryGroup(player);
		String host = player.getAddress().getAddress().getHostName();
		String ip = player.getAddress().getAddress().getHostAddress();

		synchronized(csPlayers) {
			GamePlayer bp = new GamePlayer(nick, modes, host, ip, System.currentTimeMillis() / 1000L, System.currentTimeMillis(), "Left the game.", rank, world, player);
			bukkitPlayers.put(nick, bp);
			if (SyrupLink.burstSent) {
				synchronized(csPlayers) {
					SyrupLink.writeSocket(":OOO UID "+(bp.idleTime / 1000L)+" "+bp.nick+" "+bp.host+" "+bp.nick+" "+bp.ip+" "+bp.signedOn+" +r :Minecraft Player");
					SyrupLink.writeSocket(":OOO FJOIN %lobby% 0 +nt :"+modes+","+bp.nick);
				}
			}			
			return true;
		}
	}
	
	public static void sendGlobalMsg(String message, String source) {
		synchronized(csPlayers) {
			for (String key : bukkitPlayers.keySet()) {
				GamePlayer player = bukkitPlayers.get(key);
				if (player != null) {
					if (!player.hideIRC) {
						source = source.replace("/mc", "");
						if (player.activeChannel.equalsIgnoreCase(source) || source.equalsIgnoreCase(SyrupLink.channelName) || (WaffleIRC.channelList.get(source) != null && WaffleIRC.channelList.get(source).isNameMember(player.nick))) {
							player.player.sendMessage(message);
						}
					}
				}
				player = null;
			}
		}
	}
	
		
	//get object info based on nick

	public static GamePlayer getBukkitUserByUID(String UID) {
		synchronized(csPlayers) {
			int i = 0;
			GamePlayer bp;
			while (i < bukkitPlayers.size()) {
				bp = bukkitPlayers.get(i);
				if (bp.getUID().equalsIgnoreCase(UID)) { return bp; }
				else i++;
			}
			return null;
		}
	}
}