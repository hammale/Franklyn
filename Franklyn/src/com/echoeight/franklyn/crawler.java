package com.echoeight.franklyn;

import java.applet.Applet;
import java.text.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class crawler extends Applet implements ActionListener, Runnable {
    public static final String SEARCH = "Search";
    public static final String STOP = "Stop";
    public static final String DISALLOW = "Disallow:";
    public static final int    SEARCH_LIMIT = 50;

    Panel   panelMain;
    List    listMatches;
    Label   labelStatus;

    // URLs to be searched
    Vector vectorToSearch;
    // URLs already searched
    Vector vectorSearched;
    // URLs which match
    Vector vectorMatches;

    Thread searchThread;

    TextField textURL;
    Choice    choiceType;

    public void init() {

	// set up the main UI panel
	panelMain = new Panel();
	panelMain.setLayout(new BorderLayout(5, 5));

	// text entry components
	Panel panelEntry = new Panel();
	panelEntry.setLayout(new BorderLayout(5, 5));

	Panel panelURL = new Panel();
	panelURL.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
	Label labelURL = new Label("Starting URL: ", Label.RIGHT);
	panelURL.add(labelURL);
	textURL = new TextField("", 40);
	panelURL.add(textURL);
	panelEntry.add("North", panelURL);

	Panel panelType = new Panel();
	panelType.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
	Label labelType = new Label("Content type: ", Label.RIGHT);
	panelType.add(labelType);
	choiceType = new Choice();
	choiceType.addItem("text/html");
	choiceType.addItem("audio/basic");
	choiceType.addItem("audio/au");
	choiceType.addItem("audio/aiff");
	choiceType.addItem("audio/wav");
	choiceType.addItem("video/mpeg");
	choiceType.addItem("video/x-avi");
	panelType.add(choiceType);
	panelEntry.add("South", panelType);

	panelMain.add("North", panelEntry);

	// list of result URLs
	Panel panelListButtons = new Panel();
	panelListButtons.setLayout(new BorderLayout(5, 5));

	Panel panelList = new Panel();
	panelList.setLayout(new BorderLayout(5, 5));
	Label labelResults = new Label("Search results");
	panelList.add("North", labelResults);
	Panel panelListCurrent = new Panel();
	panelListCurrent.setLayout(new BorderLayout(5, 5));
	listMatches = new List(10);
	panelListCurrent.add("North", listMatches);
	labelStatus = new Label("");
	panelListCurrent.add("South", labelStatus);
	panelList.add("South", panelListCurrent);

	panelListButtons.add("North", panelList);

	// control buttons
	Panel panelButtons = new Panel();
	Button buttonSearch = new Button(SEARCH);
	buttonSearch.addActionListener(this);
	panelButtons.add(buttonSearch);
	Button buttonStop = new Button(STOP);
	buttonStop.addActionListener(this);
	panelButtons.add(buttonStop);

	panelListButtons.add("South", panelButtons);

	panelMain.add("South", panelListButtons);

	add(panelMain);
	setVisible(true);

	repaint(); 

	// initialize search data structures
	vectorToSearch = new Vector();
	vectorSearched = new Vector();
	vectorMatches = new Vector();

	// set default for URL access
	URLConnection.setDefaultAllowUserInteraction(false);
    }

    public void start() {
    }

    public void stop() {
	if (searchThread != null) {
	    setStatus("stopping...");
	    searchThread = null;
	}
    }

    public void destroy() {
    }

    boolean robotSafe(URL url) {
	String strHost = url.getHost();

	// form URL of the robots.txt file
	String strRobot = "http://" + strHost + "/robots.txt";
	URL urlRobot;
	try { 
	    urlRobot = new URL(strRobot);
	} catch (MalformedURLException e) {
	    // something weird is happening, so don't trust it
	    return false;
	}

	String strCommands;
	try {
	    InputStream urlRobotStream = urlRobot.openStream();

	    // read in entire file
	    byte b[] = new byte[1000];
	    int numRead = urlRobotStream.read(b);
	    strCommands = new String(b, 0, numRead);
	    while (numRead != -1) {
		if (Thread.currentThread() != searchThread)
		    break;
		numRead = urlRobotStream.read(b);
		if (numRead != -1) {
		    String newCommands = new String(b, 0, numRead);
		    strCommands += newCommands;
		}
	    }
	    urlRobotStream.close();
	} catch (IOException e) {
	    // if there is no robots.txt file, it is OK to search
	    return true;
	}

	// assume that this robots.txt refers to us and 
	// search for "Disallow:" commands.
	String strURL = url.getFile();
	int index = 0;
	while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
	    index += DISALLOW.length();
	    String strPath = strCommands.substring(index);
	    StringTokenizer st = new StringTokenizer(strPath);

	    if (!st.hasMoreTokens())
		break;
	    
	    String strBadPath = st.nextToken();

	    // if the URL starts with a disallowed path, it is not safe
	    if (strURL.indexOf(strBadPath) == 0)
		return false;
	}

	return true;
    }

    public void paint(Graphics g) {
      	//Draw a Rectangle around the applet's display area.
      	g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);

	panelMain.paint(g);
	panelMain.paintComponents(g);
	// update(g);
	// panelMain.update(g);
    }

    public void run() {
	String strURL = textURL.getText();
	String strTargetType = choiceType.getSelectedItem();
	int numberSearched = 0;
	int numberFound = 0;

	if (strURL.length() == 0) {
	    setStatus("ERROR: must enter a starting URL");
	    return;
	}

	// initialize search data structures
	vectorToSearch.removeAllElements();
	vectorSearched.removeAllElements();
	vectorMatches.removeAllElements();
	listMatches.removeAll();

	vectorToSearch.addElement(strURL);

	while ((vectorToSearch.size() > 0) 
	  && (Thread.currentThread() == searchThread)) {
	    // get the first element from the to be searched list
	    strURL = (String) vectorToSearch.elementAt(0);

	    setStatus("searching " + strURL);

	    URL url;
	    try { 
		url = new URL(strURL);
	    } catch (MalformedURLException e) {
		setStatus("ERROR: invalid URL " + strURL);
		break;
	    }

	    // mark the URL as searched (we want this one way or the other)
	    vectorToSearch.removeElementAt(0);
	    vectorSearched.addElement(strURL);

	    // can only search http: protocol URLs
	    if (url.getProtocol().compareTo("http") != 0) 
		break;

	    // test to make sure it is before searching
	    if (!robotSafe(url))
		break;

	    try {
		// try opening the URL
		URLConnection urlConnection = url.openConnection();

		urlConnection.setAllowUserInteraction(false);

		InputStream urlStream = url.openStream();
		String type 
		  = urlConnection.guessContentTypeFromStream(urlStream);
		if (type == null)
		    break;
		if (type.compareTo("text/html") != 0) 
		    break;

		// search the input stream for links
		// first, read in the entire URL
		byte b[] = new byte[1000];
		int numRead = urlStream.read(b);
		String content = new String(b, 0, numRead);
		while (numRead != -1) {
		    if (Thread.currentThread() != searchThread)
			break;
		    numRead = urlStream.read(b);
		    if (numRead != -1) {
			String newContent = new String(b, 0, numRead);
			content += newContent;
		    }
		}
		urlStream.close();

		if (Thread.currentThread() != searchThread)
		    break;

		String lowerCaseContent = content.toLowerCase();

		int index = 0;
		while ((index = lowerCaseContent.indexOf("<a", index)) != -1)
		{
		    if ((index = lowerCaseContent.indexOf("href", index)) == -1) 
			break;
		    if ((index = lowerCaseContent.indexOf("=", index)) == -1) 
			break;
		    
		    if (Thread.currentThread() != searchThread)
			break;

		    index++;
		    String remaining = content.substring(index);

		    StringTokenizer st 
		      = new StringTokenizer(remaining, "\t\n\r\">#");
		    String strLink = st.nextToken();

		    URL urlLink;
		    try {
			urlLink = new URL(url, strLink);
			strLink = urlLink.toString();
		    } catch (MalformedURLException e) {
			setStatus("ERROR: bad URL " + strLink);
			continue;
		    }

		    // only look at http links
		    if (urlLink.getProtocol().compareTo("http") != 0)
			break;

		    if (Thread.currentThread() != searchThread)
			break;

		    try {
			// try opening the URL
			URLConnection urlLinkConnection 
			  = urlLink.openConnection();
			urlLinkConnection.setAllowUserInteraction(false);
			InputStream linkStream = urlLink.openStream();
			String strType 
			  = urlLinkConnection.guessContentTypeFromStream(linkStream);
			linkStream.close();

			// if another page, add to the end of search list
			if (strType == null)
			    break;
			if (strType.compareTo("text/html") == 0) {
			    // check to see if this URL has already been 
			    // searched or is going to be searched
			    if ((!vectorSearched.contains(strLink)) 
			      && (!vectorToSearch.contains(strLink))) {

				// test to make sure it is robot-safe!
				if (robotSafe(urlLink))
				    vectorToSearch.addElement(strLink);
			    }
			}

			// if the proper type, add it to the results list
			// unless we have already seen it
			if (strType.compareTo(strTargetType) == 0) {
			    if (vectorMatches.contains(strLink) == false) {
				listMatches.add(strLink);
				vectorMatches.addElement(strLink);
				numberFound++;
				if (numberFound >= SEARCH_LIMIT)
				    break;
			    }
			}
		    } catch (IOException e) {
			setStatus("ERROR: couldn't open URL " + strLink);
			continue;
		    }
		}
	    } catch (IOException e) {
		setStatus("ERROR: couldn't open URL " + strURL);
		break;
	    }

	    numberSearched++;
	    if (numberSearched >= SEARCH_LIMIT)
		break;
	}

	if (numberSearched >= SEARCH_LIMIT || numberFound >= SEARCH_LIMIT)
	    setStatus("reached search limit of " + SEARCH_LIMIT);
	else
	    setStatus("done");
	searchThread = null;
	// searchThread.stop();
    }

    void setStatus(String status) {
	labelStatus.setText(status);
    }

    public void actionPerformed(ActionEvent event) {
	String command = event.getActionCommand();

	if (command.compareTo(SEARCH) == 0) {
	    setStatus("searching...");

	    // launch a thread to do the search
	    if (searchThread == null) {
		searchThread = new Thread(this);
	    }
	    searchThread.start();
	}
	else if (command.compareTo(STOP) == 0) {
	    stop();
	}
    }
        public static void main (String argv[])
        {
                Frame f = new Frame("WebFrame");
                crawler applet = new crawler();
		f.add("Center", applet);

/*		Behind a firewall set your proxy and port here!
*/
                Properties props= new Properties(System.getProperties());
                props.put("http.proxySet", "true");
        	props.put("http.proxyHost", "webcache-cup");
        	props.put("http.proxyPort", "8080");

                Properties newprops = new Properties(props);
                System.setProperties(newprops);
/**/

		
                applet.init();
                applet.start();
                f.pack();
                f.show();
        }

}