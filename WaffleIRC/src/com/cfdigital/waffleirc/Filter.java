package com.cfdigital.waffleirc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
	
    public static CopyOnWriteArrayList<String> regexRules = new CopyOnWriteArrayList<String>();
    private static ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<String, Pattern>(); 
	private final static Logger log = Logger.getLogger("Minecraft");

    
    public static void compilePattern(String re) {
    	// Do not re-compile if we already have this pattern 
    	if (patterns.get(re) == null) {
    		try {
    			Pattern pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
    			patterns.put(re, pattern);
    			log.fine("[Filter] Successfully compiled regex: " + re);
    		}
    		catch (PatternSyntaxException e) {
    			log.warning("[Filter] Failed to compile regex: " + re);
    			log.warning("[Filter] " + e.getMessage());
    		}
    		catch (Exception e) {
    			log.severe("[Filter] Unexpected error while compiling expression '" + re + "'");
    			e.printStackTrace();
    		}
    	}
    }
    
    public static Boolean matchPattern(String msg, String re_from) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
			log.info("[Filter] Ignoring invalid regex: " + re_from);
    		return false;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.find();
    }
    
    public static String replacePattern(String msg, String re_from, String to) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
    		return msg;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.replaceAll(to);
    }
    
}