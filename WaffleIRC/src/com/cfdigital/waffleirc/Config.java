package com.cfdigital.waffleirc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

public class Config extends JavaPlugin {
	
	private final Logger log = Logger.getLogger("Minecraft");
	
    private FileConfiguration config;
    public static File configFile;
	static YamlConfiguration ymlConfig;
	public static boolean debugmode;
	public static String remoteHost;
	public static String linkName;
	public static String connectHash;
	public static int remotePort;
	public static int reconnectTime = 10;
	public static String serverHostName = "irc.server"; 
	public static String serverVersion = "ColdFyre's WaffleIRC v 1.7"; 
	public static String ircJoinMsg = "%username% Joined %channel%";
	public static String ircPartMsg = "%username% Parted %channel%";
	public static String ircQuitMsg = "%username% Quit IRC (%reason%)";
	public static String ircNickChange = "%username% is known as %newname%";
	public static String ircChanMsg = "[%channel%] %username%: %message%";
	public static String ircChanAction = "[%channel%] *%username% %message%";
	public static String ircPrivMsgGet = "%username% -> me: %message%";
	public static String ircPrivMsgSend = "me -> %username%: %message%";
	public static String ircPrivActionSend = "*me -> %username%: %message%";
	public static String ircPrivActionGet = "[%channel%] *%username% %message%";
	public static String ircKick = "%target% was kicked from %channel% by %username% (%reason%)";
	public static boolean burstSpam = true;
    
	public void saveLinkSettings() {
        config.set("connectPassword", "changeme");
        config.set("linkName", "irc.ponyserver");
        config.set("remotePort", 6667);
        config.set("remoteHost", "127.0.0.1");
        config.set("debugmode", "false");
        config.set("burstSpam", "true");
        config.set("ircJoinMsg", "%username% Joined %channel%");
        config.set("ircQuitMsg", "%username% Parted %channel%");
        config.set("ircPartMsg", "%username% Quit IRC (%reason%)");
        config.set("ircChanMsg", "[%channel%] %username%: %message%");
        config.set("ircChanAction", "[%channel%] *%username% %message%");
        config.set("ircPrivActionGet", "8%username% -> me: %message%");
        config.set("ircPrivActionSend", "*me -> %username%: %message%");
        config.set("ircPrivMsgSend", "me -> %username%: %message%");
        config.set("ircPrivMsgGet", "%username% -> me: %message%");
        config.set("ircNickChange", "%username% is known as %newname%");
        config.set("ircKick", "%target% was kicked from %channel% by %username% (%reason%)");

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

	}
	public void loadFilters(){
		//TODO add regex filters here
        //configFile = new File(WaffleIRC.thePlugin.getDataFolder()+"filters.yml");
        //config = YamlConfiguration.loadConfiguration(configFile);
		try {
        	BufferedReader input =  new BufferedReader(new FileReader(WaffleIRC.thePlugin.getDataFolder()+"/filters.txt"));
    		String line = null;
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.matches("^#.*") && !line.matches("")) {
    				//if (Permissions == null) {
    					if (line.startsWith("ignore group") || 
    						line.startsWith("ignore permission") || 
    						line.startsWith("require group") || 
    						line.startsWith("require permission")) {
    						continue;
    					}
    				//}
    				Filter.regexRules.add(line);
    				if (line.startsWith("match ") || line.startsWith("replace ")) {
    					String[] parts = line.split(" ", 2);
    					Filter.compilePattern(parts[1]);
    				}
    			}
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		log.warning("[WIRC] Cannot load filter rules");
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
		
	}
	


	public boolean loadLinkSettings() {
        configFile = new File(WaffleIRC.thePlugin.getDataFolder(), "config.yml");
		config = WaffleIRC.thePlugin.getConfig();


        if (!configFile.exists()) {
        	saveLinkSettings();
    		log.warning("[WIRC] No config file found. Generating new one. Please edit and reload.");
    		return false;
        }
          
		if (!WaffleIRC.thePlugin.getDataFolder().exists()) {
			WaffleIRC.thePlugin.getDataFolder().mkdirs();
        }
		
		try {
            config.load(configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        remoteHost = config.getString("remoteHost"); 
        serverHostName = config.getString("linkName"); 
        connectHash = config.getString("connectPassword"); 
        remotePort = config.getInt("remotePort");
        debugmode = config.getBoolean("debugmode");
        burstSpam = config.getBoolean("burstSpam");
        ircJoinMsg = config.getString("ircJoinMsg");
        ircQuitMsg = config.getString("ircQuitMsg");
        ircPartMsg = config.getString("ircPartMsg");
        ircChanMsg = config.getString("ircChanMsg");
        ircChanAction = config.getString("ircChanAction");
        ircPrivActionGet = config.getString("ircPrivActionGet");
        ircPrivActionSend = config.getString("ircPrivActionSend");
        ircPrivMsgSend = config.getString("ircPrivMsgSend");
        ircPrivMsgGet = config.getString("ircPrivMsgGet");
        ircNickChange = config.getString("ircNickChange");
        ircKick = config.getString("ircKick");


        return true;

	}

}