package xyz.tylerwong.searchlist;
/*	Created by: Tyler Wong
 * 	Date: August 10th, 2014
 * 	Purpose: To obtain an up to date sarch list of up to date search terms that people
 * 	are actively searching for. 
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.seleniumhq.jetty7.util.ajax.JSON;
import org.json.*;

public class SearchList {
    
	// Holds the words. using a SortedSat because I like the natural order and no duplicates
	// Which happens a lot with this set of data because I'm always updating it
	SortedSet<String> wordSet = new TreeSet<String>();
	
	private String filePath = (System.getProperty("user.dir") + "\\src\\WordList.txt");		// change this if you want the wordlist somewhere else
	
    // declare variables for selenium
    private WebDriver driver;
    private String historyWikiUrl;
    private String wikiUrl;
    private String aolUrl;
    
    public SearchList() { }
  
    // execute whole script
    public void execute() {
    	try {
            setUp();
            getWords();
            tearDown();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    @Before
    public void setUp() {
        driver = new FirefoxDriver();
        historyWikiUrl = "http://tools.wmflabs.org/usersearch/usersearch.py?name=West.andrew.g&page=User%3AWest.andrew.g%2FPopular+pages&server=enwiki&max=500";
        wikiUrl = "http://en.wikipedia.org/wiki/Wikipedia:5000";		//This list updates every Sunday morning (UTC), aggregating data from the 7 days preceeding 11:59PM Saturday.
        aolUrl = "http://search.aol.com/aol/trends";
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        
        // load current word list
        try {
        	Scanner s = new Scanner(new File(filePath), "UTF-8");
        	s.useDelimiter("\r\n");
        	while(s.hasNext()) {
        		wordSet.add(s.next().trim());
        	}
        	s.close();
        }
        catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    @Test
    public void getWords() {
//    	// gets the top 5000 wiki articles from current to October 2012
//    	ArrayList<String> links = new ArrayList<String>();
//    	
//    	driver.get(historyWikiUrl);
//    	
//    	WebElement ul = driver.findElement(By.tagName("ul"));
//    	List<WebElement> li_collection_wiki = ul.findElements(By.tagName("li"));
//    	
//    	// finds which links to click
//    	for(WebElement li : li_collection_wiki) {
//    		try {
//	    		List<WebElement> a_collection = li.findElements(By.tagName("a"));
//	    		if(li.findElement(By.tagName("span")).getText().equals("Updating popular pages report")) {		// checks to see if the edit is actually an update to the report
//	    			links.add(a_collection.get(0).getAttribute("href"));
//	    		}
//    		}
//    		catch(Exception e) {}
//    	}
//    	
//    	// clicks each link and gets the resulsts
//    	for(String link : links) {
//    		try {
//	    		driver.get(link);
//	    		
//	    		WebElement tbody = driver.findElement(By.xpath("//*[@id=\"mw-content-text\"]/dl/dd/dl/dd/table/tbody"));
//	        	List<WebElement> tr_collection = tbody.findElements(By.tagName("tr"));
//	        	
//	        	for(WebElement tr : tr_collection) {
//	        		List<WebElement> td_collection = tr.findElements(By.tagName("td"));
//	        		
//	        		String word = td_collection.get(1).getText().trim();
//	        		wordSet.add(word);
//	        	}
//    		}
//    		catch(Exception e) {}
//    	}
//    	
//    	// get top 5000 top article titles on wikipedia for that week
//    	driver.get(wikiUrl);
//    	
//    	WebElement tbody = driver.findElement(By.xpath("//*[@id=\"mw-content-text\"]/dl[2]/dd/dl/dd/table/tbody"));
//    	List<WebElement> tr_collection = tbody.findElements(By.tagName("tr"));
//    	
//    	for(WebElement tr : tr_collection) {
//    		List<WebElement> td_collection = tr.findElements(By.tagName("td"));
//    		
//    		String word = td_collection.get(1).getText().trim();
//    		wordSet.add(word);
//    	}
//    	
//    	// get 50 more words from aol top daily searches
//    	driver.get(aolUrl);
//    	
//    	WebElement list = driver.findElement(By.xpath("//*[@id=\"trends\"]/div[3]"));
//    	List<WebElement> li_collection = list.findElements(By.tagName("li"));
//    	
//    	for(WebElement li : li_collection) {
//    		String word = li.getText().trim();
//    		
//    		wordSet.add(word);
//    	}
    	
    	// Gets json data from bing for related search results
		SortedSet<String> tempSet = new TreeSet<String>();		// I just want to go over the data once
		for(String entry : wordSet) {
			tempSet.add(entry);
		}
		int count = 0;
		for(String word : tempSet) {
			try {
		    	URL link = new URL("http://api.bing.net/qson.aspx?query=" + word.replaceAll(" ", "+"));
		    	BufferedReader in = new BufferedReader(new InputStreamReader(link.openStream()));

				JSONObject obj = new JSONObject(in.readLine());
				JSONArray arr = obj.getJSONObject("SearchSuggestion").getJSONArray("Section");
				
				for(int i = 0;i < arr.length(); i++) {
					wordSet.add(arr.getJSONObject(i).getString("Text"));
				}
				if(count % 1000 == 0) {
					System.out.println(word + " " + count/tempSet.size() + "%");
				}
				count++;
				in.close();
			}
			catch(Exception e) {}	// anything caught by this means that there are no related searches
		}
    }
    
    @After
    public void tearDown() {
        driver.quit();
        
        // write to text file
        try {
			FileWriter writer = new FileWriter(filePath);
			
			for(String word : wordSet) {
				if(!word.contains("(") && !word.contains(")") && !word.contains("/")) {
					writer.append(word + "\r\n");
				}
				else if(word.contains("(") || word.contains(")")){	// remove anything in parantheses
					writer.append(word.substring(0, word.lastIndexOf("(")).trim() + "\r\n");
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
