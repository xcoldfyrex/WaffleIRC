package com.cfdigital.waffleirc;

public class Format {
	
	public static int[] ircColors     = { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15};
	public static String[] gameColors = {"0","f","1","2","c","4","5","6","e","a","3","b","9","d","8","7"};
	public static boolean convertColorCodes = true;

	
	public static String[] split(String line) {
		String[] sp1 = line.split(" :", 2);
		String[] sp2 = sp1[0].split(" ");
		String[] res;
		if (!sp2[0].startsWith(":")) {
			res = new String[sp1.length + sp2.length];
			System.arraycopy(sp2, 0, res, 1, sp2.length);
		} else {
			res = new String[sp1.length + sp2.length - 1];
			System.arraycopy(sp2, 0, res, 0, sp2.length);
			res[0] = res[0].substring(1);
		}
		if (sp1.length == 2)
		res[res.length - 1] = sp1[1];
		return res;
	}
	
	public static String join(String[] strArray, String delimiter, int start) {
		String joined = "";
		int noOfItems = 0;
		for (String item : strArray) {
			if (noOfItems < start) { noOfItems++; continue; }
			joined += item;
			if (++noOfItems < strArray.length)
			joined += delimiter;
		}
		return joined;
	}
	public static String convertColors(String input, boolean fromIRCtoGame) {
		if (!convertColorCodes) {
			String output = input;
			int i = 16;
			if (fromIRCtoGame) {
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) {
						output = output.replace(((char)3)+"0"+Integer.toString(ircColors[i]), "");
					}
					output = output.replace(((char)3)+Integer.toString(ircColors[i]), "");
				}
				output = output.replace((char)3+"", "").replace((char)2+"", "").replace((char)29+"", "").replace((char)15+"", "").replace((char)31+"", "");
			}
			else {
				String irccolor;
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) irccolor = "0"+ircColors[i];
					else irccolor=Integer.toString(ircColors[i]);
					output = output.replace("§"+gameColors[i].toLowerCase(), ((char)3)+irccolor);
					output = output.replace("&"+gameColors[i].toLowerCase(), ((char)3)+irccolor);
					output = output.replace("§"+gameColors[i].toUpperCase(), ((char)3)+irccolor);
					output = output.replace("&"+gameColors[i].toUpperCase(), ((char)3)+irccolor);
				}
				output = output.replace("^K", (char)3+"").replace("^B", (char)2+"").replace("^I", (char)29+"").replace("^O", (char)15+"").replace("^U", (char)31+"");
			}
			return output;

		}
		else {
			String output = input;
			int i = 16;
			if (fromIRCtoGame) {
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) {
						output = output.replace(((char)3)+"0"+Integer.toString(ircColors[i]), "§"+gameColors[i]);
					}
					output = output.replace(((char)3)+Integer.toString(ircColors[i]), "§"+gameColors[i]);
				}
				output = output.replace((char)3+"", "§f").replace((char)2+"", "§l").replace((char)29+"", "").replace((char)15+"", "§r").replace((char)31+"", "§n");
			}
			//came from in game
			else {
				String irccolor;
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) irccolor = "0"+ircColors[i];
					else irccolor=Integer.toString(ircColors[i]);
					output = output.replace("§"+gameColors[i].toLowerCase(), ((char)3)+irccolor);
					output = output.replace("&"+gameColors[i].toLowerCase(), ((char)3)+irccolor);
					output = output.replace("§"+gameColors[i].toUpperCase(), ((char)3)+irccolor);
					output = output.replace("&"+gameColors[i].toUpperCase(), ((char)3)+irccolor);
				}
				output = output.replace("^K", (char)3+"").replace("&l", (char)2+"").replace("^O", (char)29+"").replace("&r", (char)15+"").replace("&n", (char)31+"");
			}
			return output;
		}
	}
	
	public static String stripFormatting(String input)
	{
		String output = input;
		int i = 16;
		while (i > 0) {
			i--;
			if (ircColors[i] < 10) output=output.replace("^K0"+i,"");
			output=output.replace("^K"+i,"");
		}
		output = output.replace("^K", "").replace("^B", "").replace("^I", "").replace("^O", "").replace("^U", "");
		return output;
	}
	
	public static String formatIntoHHMMSS(long secs)
	{
		long secsIn= secs;
		long hours = secsIn / 3600,
		remainder = secsIn % 3600,
		minutes = remainder / 60,
		seconds = remainder % 60;

		return ( (hours < 10 ? "0" : "") + hours
				+ ":" + (minutes < 10 ? "0" : "") + minutes
				+ ":" + (seconds< 10 ? "0" : "") + seconds );
	}
}