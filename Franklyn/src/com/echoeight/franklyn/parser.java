package com.echoeight.franklyn;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class parser {

	private static final Pattern CHECK_LINK = Pattern.compile("^.+?<a href=");
	private static final Pattern CHECK_LINK2 = Pattern.compile("<.+?");
	private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");
	private static final Pattern FIND_LINK = Pattern.compile("(?i).*<a");
	
	private static Pattern htmltag;
	private static Pattern link;
	
	public static BufferedReader read(String url) throws Exception{
		return new BufferedReader(
			new InputStreamReader(
				new URL(url).openStream()));}

	public static void main (String[] args) throws Exception{
		String urlinitial = "http://snippets.dzone.com/posts/show/3553";
		URL url = new URL(urlinitial);
		String domain =  url.getHost();
		domain = "http://" + domain;
		BufferedReader reader = read(urlinitial);
		String line = reader.readLine();


		htmltag = Pattern.compile("<a\\b[^>]*href=\"[^>]*>(.*?)</a>");
		link = Pattern.compile("href=\"[^>]*\">");
		
		while (line != null) {
			line = line.toLowerCase();
			//String parsed = removeTags(line);
			//if(!(line.contains("<") || line.contains(">"))){
			Matcher tagmatch = htmltag.matcher(line);
			while (tagmatch.find()) {
				Matcher matcher = link.matcher(tagmatch.group());
				matcher.find();
				String link = matcher.group().replaceFirst("href=\"", "")
						.replaceFirst("\">", "");
				if(valid(link)){
					String end = makeAbsolute(domain, link);
					end = end.replaceAll("\"", "");
					end = end.replaceAll("class.+?", "");
					end = end.replaceAll("tag.+?", "");
					System.out.println(end);
				}
			}
			//}
			line = reader.readLine();
		} 
	}

	private static boolean valid(String s) {
		if (s.matches("javascript:.*|mailto:.*")) {
			return false;
		}
		return true;
	}

	private static String makeAbsolute(String url, String link) {
		if (link.matches("http://.*")) {
			return link;
		}
		if (link.matches("/.*") && url.matches(".*$[^/]")) {
			return url + "/" + link;
		}
		if (link.matches("[^/].*") && url.matches(".*[^/]")) {
			return url + "/" + link;
		}
		if (link.matches("/.*") && url.matches(".*[/]")) {
			return url + link;
		}
		if (link.matches("/.*") && url.matches(".*[^/]")) {
			return url + link;
		}
		throw new RuntimeException("Cannot make the link absolute. Url: " + url
				+ " Link " + link);
	}
}