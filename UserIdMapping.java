package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.xerox.socialmedia.communityrecommendation.dbmanager.SqlControler;
import com.xerox.socialmedia.communityrecommendation.utils.Constant;
import com.xerox.socialmedia.communityrecommendation.utils.IOUtil;

public class UserIdMapping {
	private static String propertyFile = "config/resource.properties";
	private static Properties property = IOUtil.getProperties(propertyFile);
	private int numTweet = 0; //total number of tweets
	String storeFile = "D:\\Social_Media_Analytics\\dataset\\UserIdMapping.csv";
	private HashMap<String, Integer> userNameMapping = new HashMap<String, Integer>();
	private SqlControler sqlControler;
	
	public UserIdMapping(){
		sqlControler = new SqlControler();
	}
	
	public void mapping(){
		String dataFolder = property.getProperty("tweet.data.folder");
		File[] dataFiles = new File(dataFolder).listFiles();
		
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		ArrayList<String> mentionNames = new ArrayList<String>();
		String screenName = "";
		String RTName = "";
		int uniqueId = 0;
		try{
			for(File f:dataFiles) //go through all files
		    {
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				String tweetText = "";
				int numTweetPerFile = 0;
				
				while ((line = bf.readLine()) != null) { // extract tweet id, user id, screen name, text from each line 
					try{
						tweet = (JSONObject) new JSONObject(line);
					} catch(JSONException e){
						continue;
					}
					//ignore the tweets who don't have an id
					if(!tweet.has("id"))
						continue;
					
					//retrieve text from tweet
					tweetText = tweet.getString("text");
					
                    numTweetPerFile++;
					
					//remove the tweets whose number of hashtags exceeds 10
					if(!isMoreHashtagsInside(tweetText, 10)){
						mentionNames = extractScreenNameFromMention(tweetText);
						
						//get the user's screenName
						screenName = tweet.getJSONObject("user").getString("screen_name");
						//add the user's screen_name
						mentionNames.add(screenName);
						
						//if it is a retweet, add the retweeted screen_name
						if(tweet.getInt("retweet_count") > 0){
							RTName = tweet.getJSONObject("retweeted_status").getJSONObject("user").getString("screen_name");
							mentionNames.add(RTName);
						}
						
						
						for(int i = 0; i < mentionNames.size(); i++){
							String name = mentionNames.get(i);
							
							//mapping and add an unique screen_name into hashmap
							if(!userNameMapping.containsKey(name)){
								userNameMapping.put(name, uniqueId);
								uniqueId++;
							}
						}	
					}
					
					mentionNames.clear();	
				}
				
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " records in" + f.getName());
				numTweetPerFile = 0;
		    }
			bf.close();
			writeIntoDBFile();
			
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// write the mapping relationship into DB and file
	private void writeIntoDBFile(){
		sqlControler.createConnection();
		String insert = "insert into user_id_name values (?,?,?)";
		PreparedStatement ps = sqlControler.GetPreparedStatement(insert);
		String key = "";
		int value = 0;
		int index = 0;
		
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\userNameMapping.csv";
		BufferedWriter bw = null;
		
		try{
			bw = new BufferedWriter(new FileWriter(storeFile));
			for(Map.Entry<String, Integer> pairs: userNameMapping.entrySet()){
				key = (String) pairs.getKey();
				value = (Integer) pairs.getValue();
				ps.setInt(1, index);
				ps.setInt(2, value);
				ps.setString(3, key);
				sqlControler.addBatch(ps); // batch insert
				index++;
				
				bw.append(key);
				bw.append(Constant.SEPARATOR_COMMA);
				bw.append(String.valueOf(value));
				bw.append('\n');
				bw.flush();
			}
			sqlControler.executeBatch(ps);
			
			ps.close();
			sqlControler.close();
			bw.close();
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	
	private boolean isMoreHashtagsInside(String s, int count) {
		String[] words = s.split(Constant.SEPARATOR_SPACE);
		int hashtags = 0;
		for (String w : words)
			if (w.startsWith("#"))
				hashtags++;
		return hashtags > count;
	}
	
	private ArrayList<String> extractScreenNameFromMention(String s) {
		//String[] words = s.split(Constant.SEPARATOR_SPACE);
		
		ArrayList<String> names = new ArrayList<String>();
		
		Pattern pattern = Pattern.compile("@([A-Za-z0-9_]+)");
		Matcher matcher = null;
		int start = 0;
		int end = 0;
		
			/*if (w.contains("@")){
				if(w.endsWith(":")){
					names.add(w.substring(w.indexOf("@")+1, w.length()-1));
				}
				else
					names.add(w.substring(w.indexOf("@")+1));
				
			}*/
			matcher = pattern.matcher(s);
			while(matcher.find()){
				start = matcher.start()+1;
				end = matcher.end();
				names.add(s.substring(start, end));
			}
			
		
		return names;
	}
	
	public static void main(String[] args){
		UserIdMapping rf = new UserIdMapping();
		
	    rf.mapping();
		
	}

}
