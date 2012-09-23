package com.cfdigital.waffleirc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

public class GamePlayer {
	
	public GamePlayer(String nick, String mode, String host, String ip, long signedOn, long idleTime, String quitReason, String rank, String world, Player player) {
		this.nick = nick;
		this.mode = mode;
		if (mode != null) this.textMode = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v");
		this.host = host;
		this.ip = ip;
		this.signedOn = signedOn;
		this.idleTime = idleTime;
		this.quitReason = quitReason;
		this.rank = rank;
		this.world = world;
		this.player = player;
		this.lastIRCTarget = "";
		this.lastChatTarget = "";
		this.hideIRC = false;
		
	}
	
	public void setUID(String UID) {
		this.UID = UID;
	}
	
	public String getUID() {
		return this.nick;
	}
	
	public String getName() {
		return this.nick;
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	
	public void setMode(String mode) {
		this.mode = mode;
		this.textMode = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v");
	}
	
	public String getMode() {
		return this.mode;
	}
	
	public String getTextMode() {
		return this.textMode;
	}
	
	public String getChannel(){
		return this.channel;
	}
	
	public boolean isIRCHidden() {
		return this.hideIRC;
	}
	
	public boolean isChannelMember(String channel) {
		channel = channel.toLowerCase();
		if (this.channelList.contains(channel)) {
			return true;
		}
		return false;
	}

	
	private List<String> channelList = new ArrayList<String>();
	String nick = null, host = null, ip = null, UID = null, quitReason = "Left the game.", rank = "N/A", world = "N/A", channel = "#lobby" ,lastIRCTarget, lastChatTarget;
	String mode = null,textMode = null;
	String activeChannel = "";
	long signedOn = 0, idleTime = 0;
	boolean hideIRC = false;
	Player player;
}