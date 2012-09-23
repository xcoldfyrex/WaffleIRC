package com.cfdigital.waffleirc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.logging.Logger;

import com.cfdigital.waffleirc.IRCDEvents;
import com.cfdigital.waffleirc.GamePlayer;
import com.cfdigital.waffleirc.IRCUser;
import com.cfdigital.waffleirc.Config;

import org.bukkit.entity.Player;

public class SyrupLink implements Runnable {
	
	public static HashMap<String, IRCUser> IRCUsers = new HashMap<String, IRCUser>();

	public static String playername = null;
	public static String chatmsg = "";
	public static String message = "";
		
	public static String IRCDCommand = null;

	public static String channelName; 
	public static String consoleChannelName;
	public static final long serverStartTime = System.currentTimeMillis() / 1000L;
	public static long channelTS = serverStartTime, consoleChannelTS = serverStartTime;
	public static String remoteSID = null;

	public static int linkPingInterval = 60;
	public static int linkTimeoutInterval = 180;
	public static int linkDelay = 60;

	public static boolean linkcompleted = false;
	public static boolean running = true;
	public static boolean lastconnected = false;
	public static boolean wasPing = false;
	public static Socket server = null;
	public static boolean burstSent = false, capabSent = false;
	public static String pre;
	public static String genericMsg = null;
	public static boolean burstPre = false;
	public static boolean isDebugText = false;
	
	public static BufferedReader in;
	public static PrintStream out;
	
	static class CriticalSection extends Object {
	}
	static public CriticalSection csIrcUsers = new CriticalSection();
	
	private final static Logger log = Logger.getLogger("Minecraft");

	static IRCDEvents ircdevent = null;

	public void run() {
		
		ircdevent = new IRCDEvents();
		String line = null;
		IRCDLinkStart();

		while (running) {
			while (running && (server != null) && server.isConnected() && (!server.isClosed())) {
				try {
					line = in.readLine();
					if (line == null) { 
						log.warning("[WIRC] Unable to read from stream, closing connection");
						IRCDLinkClose();
						break;
					}
					
					String[] split = line.split(" ");
					split = line.split(" ");
					if (split.length == 1) {
						IRCDCommand = split[0];
					}
					else {
						IRCDCommand = split[1];
					}
					
					if(Config.debugmode) {
						if (burstPre) {
							isDebugText = true;
							out.println(":000 PRIVMSG " + consoleChannelName+ " :[IN] "+line);
							isDebugText = false;
						}
					}
				
					if (IRCDCommand.equalsIgnoreCase("ENDBURST")) {
						if (split[0].equalsIgnoreCase(remoteSID) || split[0].equalsIgnoreCase(Config.linkName)) {
							if (capabSent){ 
								IRCDLinkBurst();
							}
						}
					}
				
				
					if ((line.startsWith(":")) &&  capabSent && burstSent && (server != null) && (!server.isClosed()) && server.isConnected() && running  ) {
						remoteSID = split[0];
						IRCDEvents.LinkCommandParse(IRCDCommand, line);
						if (wasPing) {
							wasPing = false;
						}
					} else {	
						
					}

					//send BURST
					while (capabSent && !burstSent && (server != null) && (!server.isClosed()) && server.isConnected() && running) {
						if (!running) break;
						IRCDLinkBurst();
					}
				
					// Send CAPAB
					while (!capabSent && (server != null) && (!server.isClosed()) && server.isConnected() && running) {
						if (line.startsWith("CAPAB START"))  {
							IRCDLinkCAPAB();
						}
					}
					
					if (split[0].equalsIgnoreCase("ERROR")) {
						if (split[1].startsWith(":")) split[1] = split[1].substring(1);
						try { server.close(); } catch (IOException e) { }
						log.severe("[IRCD] ERROR: " + line);
					}

				} catch (IOException e) {
					if (!running) {
						log.warning("[WIRC] Server link failed: " + e);
					}
				}
			}

			if (lastconnected) {
				log.info("[WIRC] Lost connection to " + Config.remoteHost + ":" + Config.remotePort);
				lastconnected = false;
				linkcompleted = false;
				capabSent = false;
				burstSent = false;
				IRCUsers.clear();
				remoteSID = null;
			}
		}
			
		if ((server != null) && server.isConnected()) try { server.close(); } catch (IOException e) { }

		


	}
	
	public static boolean isConnected() {
		return ((server != null) && server.isConnected() && (!server.isClosed()));
	}
	
	public static boolean writeSocket(String line) {
		if ((server == null) || (!server.isConnected()) || (server.isClosed()) || (out == null)) return false;
		if (Config.debugmode) {
			if (burstPre) {
				out.println(":000 PRIVMSG " + consoleChannelName+ " :[OUT] "+line);
			}
		}
		out.println(line);
		//WATCH IN THE EVENT THIS CAUSES PROBLEMS!! LOOK HERE FIRST!!!!
		out.flush();
		// THE LINE ABOVE!!!!
		return true;
		
	}
	
	public static boolean IRCDLinkStart() {	
		if (IRCDEvents.knownChannels.size() != 0) IRCDEvents.knownChannels.clear();
		Logger log = Logger.getLogger("Minecraft");
		log.info("[WIRC] Attempting connection to " + Config.remoteHost + ":" + Config.remotePort);
		try {
			server = new Socket(Config.remoteHost, Config.remotePort);
			
			if ((server != null) && server.isConnected()) {
				in = null;
				out = null;
				try {
					in = new BufferedReader(new InputStreamReader(server.getInputStream()));		
				} catch (IOException e1) {
					e1.printStackTrace();
				} 
				try {
					out = new PrintStream(server.getOutputStream());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				log.info("[WIRC] Connected to " + Config.remoteHost + ":" + Config.remotePort);
				pre = ":000 ";
				return true;
			}
			else log.warning("[WIRC] Failed connection to " + Config.remoteHost + ":" + Config.remotePort);
			//running = false;
		}
		catch (IOException e) { 
			log.warning("[WIRC] Failed connection to " + Config.remoteHost + ":" + Config.remotePort + " ("+e+")"); 
			//running = false;
		}
		return false;
	}
	
	public static void IRCDLinkClose() {
		Logger log = Logger.getLogger("Minecraft");
		if ((server != null) && server.isConnected()) {
			writeSocket(pre+"SQUIT 000 :Shutting down or some shit");
			if (linkcompleted) {
				linkcompleted = false;
			}

			try { server.close(); } catch (IOException e) { }
			log.warning("[WIRC] Closed connection to " + Config.remoteHost + ":" + Config.remotePort) ; 
			if (running) {
				log.info("[WIRC] Retrying connection to "+Config.remoteHost+":"+Config.remotePort + " in " + Config.reconnectTime + " seconds");
				burstPre = false;
				burstSent = false; 
				capabSent = false;
				try {
					Thread.sleep(Config.reconnectTime * 100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				IRCDLinkStart();
			}
		 }		
	}
	
	//send BURST
	public static boolean IRCDLinkBurst() {
		
		if (burstSent) return false;
		writeSocket(pre+"BURST "+(System.currentTimeMillis() / 1000L));
		writeSocket(pre+"VERSION : " + Config.serverVersion);

		burstPre = true;
		for (String key : BukkitEvents.bukkitPlayers.keySet()) {
			GamePlayer bp = BukkitEvents.bukkitPlayers.get(key);
			writeSocket(pre+"UID "+(bp.idleTime / 1000L)+" "+bp.nick+" "+bp.host+" " + bp.ip + " " +  bp.signedOn+" :Minecraft Player");
			writeSocket(pre+"FJOIN %lobby% 0 +nt :" + bp.mode + "," + bp.nick);
		}

		writeSocket(pre+"ENDBURST");
		burstSent = true;
		return true;
	}
	
	public static boolean IRCDLinkCAPAB() {
		if (capabSent) return false;
		writeSocket("CAPAB START 1201");
		writeSocket("CAPAB CAPABILITIES :PROTOCOL=9000");
		writeSocket("CAPAB END");
		writeSocket("SERVER "+Config.serverHostName+" "+Config.connectHash+" :" + Config.serverVersion);
		capabSent = true;
		return true;
	}
	
	
	//Sends in game chat to irc
	public static void IRCDPlayerChat(Player player,String Message) {
		chatmsg = Message;
		String targetChannel = channelName;
		String activeChannel = BukkitEvents.bukkitPlayers.get(player.getName()).activeChannel;	
		if (activeChannel.length() != 0) {
			targetChannel = activeChannel.replace("#", "");
			writeSocket(":"+player.getName()+" PRIVMSG " + targetChannel + " :"+chatmsg);
		}
		else {
			writeSocket(":"+player.getName()+" PRIVMSG " + channelName + " :"+chatmsg);
		}
		
	}
	
	//Sends privmsg to IRC target
	public static void IRCDPlayerMSG(Player player,String Message, String Target) {
		writeSocket(":"+player.getName()+" PRIVMSG " + Target + " :"+Message);
	}
	
	//Sends non chat related events to a channel
	public static void IRCDSendGameEvent(String targetChannel,String Message) {
		writeSocket(":000 PRIVMSG " + targetChannel+ " :" + Message);
	}
	

	public static String[] getIRCNicks(GamePlayer gp) {
        List<String> users = new ArrayList<String>();
        for (String user : IRCUsers.keySet()) {
        	String activeChan = channelName;
        	if (!gp.activeChannel.equals("")) activeChan = gp.activeChannel+"/mc";
        	if (IRCUsers.get(user).isInChannel(activeChan)) {
        		user = IRCUsers.get(user).getChannelModes(activeChan) + user;
        		users.add(user);
        	}
        }
        
        String userArray[] = new String[0];
        userArray = users.toArray(userArray);
        Arrays.sort(userArray);
        return userArray;
}

	
}