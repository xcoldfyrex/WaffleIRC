package com.cfdigital.waffleirc;

import java.util.HashMap;
import java.util.Map;

public class IRCUser {

	public IRCUser(String nick,String ident, String hostmask, String modes) {
		this.nick = nick;
		this.ident = ident;
		this.hostmask = hostmask;
		this.modes = modes;
	}
	
	
	public String getModes() {
		return this.modes;
	}
	
	public String getTextModes() {
		return this.textModes;
	}
		
	public void setChannelModes(String channel, String mode) {
		this.channelMembership.remove(channel);
		this.channelMembership.put(channel, mode);
	}

	public String getChannelModes(String channel) {
		String textModes = this.channelMembership.get(channel);
		this.textModes = textModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
		return this.textModes;
	}
	
	public boolean isInChannel(String channel) {
		return channelMembership.containsKey(channel);
	}
	
	public void removeChannel(String channel) {
		if (channelMembership.containsKey(channel)) channelMembership.remove(channel);
	}
	
	public void addChannel(String channel) {
		if (!channelMembership.containsKey(channel)) channelMembership.put(channel, "");
	}
	

	private Map<String, String> channelMembership = new HashMap<String, String>();

	public String nick,ident,hostmask;
	private String modes="",textModes="";
	
}