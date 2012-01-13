package com.echoeight.franklyn;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.*;

public class parser implements ActionListener, Runnable {

//	private static final Pattern CHECK_LINK = Pattern.compile("^.+?<a href=");
//	private static final Pattern CHECK_LINK2 = Pattern.compile("<.+?");
//	private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");
//	private static final Pattern FIND_LINK = Pattern.compile("(?i).*<a");
	
	String urlinitial = "http://google.com";
	
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
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		text = new JTextArea(40,40);
		JScrollPane scrollPane = new JScrollPane(text);
		button.addActionListener(this);
		
	    scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {  
	        public void adjustmentValueChanged(AdjustmentEvent e) {  
	        e.getAdjustable().setValue(e.getAdjustable().getMaximum());  
	        }});  
		
		text.setEditable(false);
		
		panel.add(scrollPane);
		panel.add(button);
		frame.add(BorderLayout.CENTER, panel);
		
		frame.setSize(100,100);
		frame.setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		if(clear == true){
			clear = false;
			try {
				text.setText(null);
			    Thread worker = new Thread(this);
			    worker.start();
				
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
				String link = matcher.group().replaceFirst("href=\"", "").replaceFirst("\">", "");
				if(valid(link)){
					String end = makeAbsolute(domain, link);
					end = end.replaceAll("\"", "");
					end = end.replaceAll("class.+?", "");
					end = end.replaceAll("tag.+?", "");
					end = end.replaceAll("title.+?", "");
					String daend = null;
					if(end.contains(" ")){
						daend = end.substring(0, end.indexOf(" "));
					}else{
						daend = end;
					}
					if(checkDB(daend)){
						if(isOnline(daend)){
						useLink(daend);
						urlinitial = daend;
						text.append(daend + "\n");
						daend = null;
						}
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
	        String url = "jdbc:mysql://web02:3306/franklyn";
	        String user = "root";
	        String password = "r4pt0r";

	        try {        	
	        	Class.forName("com.mysql.jdbc.Driver");
	            con = DriverManager.getConnection(url, user, password);
	            
	            if(s != null){
	            	pstmt = con.prepareStatement("INSERT INTO `used`(`id`, `url`) VALUES (NULL,'" + s + "\n" + "')");            
	            	pstmt.executeUpdate();
	            }	
	            
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
            if(s != null){
            	String select = "SELECT * FROM `used` WHERE `url`='" + s + "'";
            	pstmt = connection.prepareStatement(select);
            	rs = pstmt.executeQuery();
            }

            if(rs.next()) {
                connection.close();          
            	return false;
            }
            connection.close();           
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
    public boolean isOnline(String s) throws IOException{
    	URL u = new URL (s);
    	HttpURLConnection huc =  ( HttpURLConnection )  u.openConnection (); 
    	huc.setRequestMethod ("GET");  //OR  huc.setRequestMethod ("HEAD"); 
    	huc.connect () ; 
    	int code = huc.getResponseCode() ;
    	if(code == 200){
    		return true;
    	}else{
        	return false;	
    	}
    }
}