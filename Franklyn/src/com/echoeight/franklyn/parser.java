package com.echoeight.franklyn;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class parser implements ActionListener, Runnable {

//	private static final Pattern CHECK_LINK = Pattern.compile("^.+?<a href=");
//	private static final Pattern CHECK_LINK2 = Pattern.compile("<.+?");
//	private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");
//	private static final Pattern FIND_LINK = Pattern.compile("(?i).*<a");
	
	String urlinitial = "http://en.wikipedia.org/wiki/Main_Page";
	
	JTextArea text;
	JButton button;
	boolean clear = true;
	
	public static void main (String[] args){
		parser gui = new parser();
		gui.go();
	}
	
	public void go(){
		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		button = new JButton("Start");
		text = new JTextArea(40,40);
		button.addActionListener(this);
		text.setEditable(false);
		
		panel.add(text);
		panel.add(button);
		frame.add(BorderLayout.CENTER, panel);
		
		frame.setSize(40,100);
		frame.setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if(clear == true){
			clear = false;
			try {
				
			    Thread worker = new Thread(this);
			    worker.start();  // this calls the method run()
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		button.setText("Stop");
		
		}else{
			Thread.currentThread().interrupt();
			text.setText(null);
			button.setText("Start");
			clear = true;
		}

	}
	
	
	private static Pattern htmltag;
	private static Pattern link;
	
	PreparedStatement pstmt;
	
	public static BufferedReader read(String url) throws Exception{
		return new BufferedReader(
			new InputStreamReader(
				new URL(url).openStream()));
	}


	public void startCrawl() throws Exception{
		while(clear == false){
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
					end = end.replaceAll("title.+?", "");
					String daend = end.substring(0, end.indexOf(" "));
					if(checkDB(daend)){
						useLink(daend);
						urlinitial = daend;
						text.append(daend);
					}
				}
			}
			//}
			line = reader.readLine();
		} 
		}
	}
	
	private void useLink(String s) throws ClassNotFoundException {

		 Connection con = null;
	        Statement st = null;

	        String url = "jdbc:mysql://web02:3306/franklyn";
	        String user = "root";
	        String password = "r4pt0r";

	        try {        	
	        	Class.forName("com.mysql.jdbc.Driver");
	            con = DriverManager.getConnection(url, user, password);
	            
	            pstmt = con.prepareStatement("INSERT INTO `used`(`id`, `url`) VALUES (NULL,'" + s + "\n" + "')");
	            
	            //String query = pstmt;
	            pstmt.executeUpdate();
	            
	        } catch (SQLException ex) {
	        	ex.printStackTrace();

	        } finally {
	            try {
	            		pstmt.close();
	                    con.close();

	            } catch (SQLException ex) {
	            	ex.printStackTrace();
	            }
	        }
	}

	private static boolean valid(String s) {
		if (s.matches("javascript:.*|mailto:.*")) {
			return false;
		}
		if (s.contains("#")) {
			return false;
		}
		if (s.contains(":")) {
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
	
	
    public boolean checkDB(String s) throws ClassNotFoundException{

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = null;
            connection = DriverManager.getConnection("jdbc:mysql://web02:3306/franklyn", "root", "r4pt0r");
            String select = "SELECT * FROM `used` WHERE `url`=" + s;
            pstmt = connection.prepareStatement(select);
            rs = pstmt.executeQuery();
            connection.close();
            
            if (rs.next()) {
            	return false;
            }

            return true;
            
        } catch (SQLException ex) {
        	ex.printStackTrace();

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException ex) {
            	ex.printStackTrace();
            }
        }
        return true;
    }

    public void run(){
        try {
			startCrawl();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}