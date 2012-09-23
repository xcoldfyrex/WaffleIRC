package com.cfdigital.waffleirc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Channel {
	
	private String channelName;
	private HashMap<String, GamePlayer> memberList = new HashMap<String, GamePlayer>();
	
	public Channel (String name) {
		name = name.toLowerCase();
		setChannelName(name);
	}

	private void setChannelName(String channelName) {
		channelName = channelName.replaceAll("#", "");            			
		this.channelName = "#" + channelName;
	}

	public String getChannelName() {
		return channelName;
	}
	
	public void joinChannel (GamePlayer player) {
		String userName = player.nick;
		if (!this.memberList.containsKey(userName)) {
			this.memberList.put(userName, player);
			SyrupLink.writeSocket(SyrupLink.pre+"FJOIN " + this.channelName + " 0 +nt :," + userName);
			sendChannelMessage("§c" + userName + "§6 joined §c" + this.channelName);
		}
		player.activeChannel = this.channelName;
		player.getPlayer().sendMessage("§6You are now talking in §c" + this.channelName + "§b");
		WaffleIRC.thePlugin.showAllMembers(player.player);
	}
	
	public void partChannel(GamePlayer player) {
		String userName = player.getName();
		if (this.memberList.containsKey(userName)) {
			this.memberList.remove(userName);
			SyrupLink.writeSocket(SyrupLink.pre+"PART " + this.channelName + " " + userName);
       		WaffleIRC.thePlugin.getServer().broadcastMessage("§b has joined the lobby");
       		player.getPlayer().sendMessage("§6You have left §c" + this.channelName);
       		player.getPlayer().sendMessage("§6You are now being returned to the lobby");
       		sendChannelMessage("§6" + userName + " left §c" + this.channelName);
       		player.activeChannel = "";
		} 
		else {
       		player.getPlayer().sendMessage("§7You are not on §c" + this.channelName);
		}
	}
	
	public void sendChannelMessage(String message) {
		for (String member : memberList.keySet()) {
			memberList.get(member).getPlayer().sendMessage(message);
		}
		
	}
	public boolean isNameMember(String player) {
		return this.memberList.containsKey(player);
	}
	
	public String[] getMemberList() {
		List<String> users = new ArrayList<String>();
        for (String user : memberList.keySet()) {
       		users.add(user);
        	
        }
        String userArray[] = new String[0];
        userArray = users.toArray(userArray);
        Arrays.sort(userArray);
        return userArray;
	}
	
}