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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.xerox.socialmedia.communityrecommendation.utils.Constant;
import com.xerox.socialmedia.communityrecommendation.utils.IOUtil;

public class ReadDataFromFile {
	private static String propertyFile = "config/resource.properties";
	private static Properties property = IOUtil.getProperties(propertyFile);
	private int numTweet = 0; //total number of tweets
	private HashMap<String, Integer> screenNameIDMapping = new HashMap<String, Integer>();
	private HashMap<String, Integer> userIdSet = new HashMap<String, Integer>();
	
	//read the screen_name and unique ID mapping from file
	public void readMapping() {
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\UserNameMapping.csv";
		String line = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 2){
					System.out.println("There is an error in reading file");
					continue;
				}
				screenNameIDMapping.put(words[0], Integer.parseInt(words[1]));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	
	public void splitFile(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_one_month.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_file_test.csv";
		String fileName = "";
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		int lineNumber = 0; //line number
		String line = "";
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile)); // test
			while ((line = reader.readLine()) != null && lineNumber < 100){
				/*if((lineNumber % 1000000)==0 && lineNumber<6000000){
					int num = lineNumber/1000000;
					fileName = String.valueOf(num);
					fileName += ".csv";
					writer = new BufferedWriter(new FileWriter(storeFile+fileName));
					System.out.println("lineNumber: " + lineNumber);
				}*/
				
				//write each line into another file
	            writer.append(line);
	            writer.append('\n');
				writer.flush();
				lineNumber++;
			}
			
			System.out.println("lineNumber overall: " + lineNumber);
			
			reader.close();
			writer.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	public void run(){
		String dataFolder = "K:\\Xerox\\gyuan\\dataset\\StreamData";
		//String dataFolder = "D:\\Social_Media_Analytics\\StreamData1";
		File[] dataFiles = new File(dataFolder).listFiles();
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_one_month.csv";
		
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		BufferedWriter bw = null;
		SimpleDateFormat parser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		try{
			bw = new BufferedWriter(new FileWriter(storeFile));
			for(File f:dataFiles) //go through all files
		    {
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				String screenName = "";
				String tweetText = "";
				int numTweetPerFile = 0;
				Date statusCreatedAt;
				String date = "";
				ArrayList<String> hashTags = new ArrayList<String>();
				int userId = 0;
				
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
					
					//remove '\r' and '\n'
					tweetText = tweetText.replaceAll("\r", "");
					tweetText = tweetText.replaceAll("\n", "");
					tweetText = tweetText.replaceAll(",", "");
					tweetText = tweetText.replaceAll("\t", "");
					
					numTweetPerFile++;
					
					//remove the tweets whose number of hashtags exceeds 10
					if(!isMoreHashtagsInside(tweetText, 10)){
						bw.append(tweet.getString("id"));
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(tweet.getJSONObject("user").getString("id"));
						bw.append(Constant.SEPARATOR_COMMA);
						
						screenName = tweet.getJSONObject("user").getString("screen_name");
						bw.append(screenName);
						if(screenNameIDMapping.containsKey(screenName))
							userId = screenNameIDMapping.get(screenName); //search the userId for the screenName
						else{
							userId =0;
							System.out.println("screenName: " + screenName + " in File: " + f.getName() + " does not in the mapping file");
						}
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(tweetText);
						bw.append(Constant.SEPARATOR_COMMA);
						//parse date 'created_at'
						statusCreatedAt = parser.parse(tweet.getString("created_at"));
						date = formatter.format(statusCreatedAt);
						bw.append(date);
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(String.valueOf(userId));
						bw.append(Constant.SEPARATOR_COMMA);
						//extract hashtags
						hashTags = extractHashTags(tweetText);
						//append hashtags size: 0 means there is no hashtag in the tweet
						bw.append(String.valueOf(hashTags.size()));
						bw.append(Constant.SEPARATOR_COMMA);
						//append hashtags
						if(hashTags.size()>1){
							for(int i = 0; i < hashTags.size()-1; i++){
								bw.append(hashTags.get(i));
								bw.append(Constant.SEPARATOR_SEMICOLON);
							}
							bw.append(hashTags.get(hashTags.size()-1));
						}
						else if(hashTags.size()==1)
							bw.append(hashTags.get(0));
						else
							bw.append(" ");
						
						
						bw.append('\n');
						bw.flush();
					}
				}
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " records in" + f.getName());
				numTweetPerFile = 0;
		    }
			bw.close();
			bf.close();
			
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	public void parseUserId(){
		String dataFolder = "K:\\Xerox\\gyuan\\dataset\\StreamData";
		//String dataFolder = "D:\\Social_Media_Analytics\\StreamData1";
		File[] dataFiles = new File(dataFolder).listFiles();
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\userIdList_one_month";
		
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		BufferedWriter bw = null;
		int numUserTweet = 0;
		String uId = "";
		//SimpleDateFormat parser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
		//SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		try{
			bw = new BufferedWriter(new FileWriter(storeFile));
			for(File f:dataFiles) //go through all files
		    {
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				//String screenName = "";
				String tweetText = "";
				int numTweetPerFile = 0;
				//Date statusCreatedAt;
				//String date = "";
				//ArrayList<String> hashTags = new ArrayList<String>();
				//int userId = 0;
				
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
					
					//remove '\r' and '\n'
					tweetText = tweetText.replaceAll("\r", " ");
					tweetText = tweetText.replaceAll("\n", " ");
					tweetText = tweetText.replaceAll(",", " ");
					tweetText = tweetText.replaceAll("\t", " ");
					
					numTweetPerFile++;
					
					//remove the tweets whose number of hashtags exceeds 10
					if(!isMoreHashtagsInside(tweetText, 10)){
						//bw.append(tweet.getString("id"));
						//bw.append(Constant.SEPARATOR_COMMA);
						uId = tweet.getJSONObject("user").getString("id");
						if(userIdSet.containsKey(uId)){
							numUserTweet = userIdSet.get(uId);
							numUserTweet++;
							userIdSet.put(uId, numUserTweet);
						}
						else
							userIdSet.put(uId, 1);
						
						//bw.append(Constant.SEPARATOR_COMMA);
						
						/*screenName = tweet.getJSONObject("user").getString("screen_name");
						bw.append(screenName);
						if(screenNameIDMapping.containsKey(screenName))
							userId = screenNameIDMapping.get(screenName); //search the userId for the screenName
						else{
							userId =0;
							System.out.println("screenName: " + screenName + " in File: " + f.getName() + " does not in the mapping file");
						}
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(tweetText);
						bw.append(Constant.SEPARATOR_COMMA);
						//parse date 'created_at'
						statusCreatedAt = parser.parse(tweet.getString("created_at"));
						date = formatter.format(statusCreatedAt);
						bw.append(date);
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(String.valueOf(userId));
						bw.append(Constant.SEPARATOR_COMMA);
						//extract hashtags
						hashTags = extractHashTags(tweetText);
						//append hashtags size: 0 means there is no hashtag in the tweet
						bw.append(String.valueOf(hashTags.size()));
						bw.append(Constant.SEPARATOR_COMMA);
						//append hashtags
						if(hashTags.size()>1){
							for(int i = 0; i < hashTags.size()-1; i++){
								bw.append(hashTags.get(i));
								bw.append(Constant.SEPARATOR_SEMICOLON);
							}
							bw.append(hashTags.get(hashTags.size()-1));
						}
						else if(hashTags.size()==1)
							bw.append(hashTags.get(0));
						else
							bw.append(" ");*/
					}
				}
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " records in" + f.getName());
				numTweetPerFile = 0;
		    }
			
			//write distinct ID into a file
			int numUser = 0;
			Iterator<Entry<String, Integer>> it = userIdSet.entrySet().iterator();
			while(it.hasNext()){
				Entry<String, Integer> pairs = it.next();
				if(pairs.getValue()>5){ //only select those whose tweet is more than 5
					bw.append(pairs.getKey());
					//System.out.println("key: " + pairs.getKey() + " value: " + pairs.getValue());
					numUser++;
					bw.append('\n');
					bw.flush();
				}
				
			}
			
			System.out.println("There are " + numUser + " distinct user");

			bf.close();
			
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
	
	//read tweets who replies previous tweets from JSON files
	public void readReplyGraph(){
		String dataFolder = property.getProperty("tweet.data.folder");
		File[] dataFiles = new File(dataFolder).listFiles();
		
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_reply_graph.csv";

		int numTweet = 0;
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		BufferedWriter bw = null;
		String screenName = "";
		int userId = 0;
		String tweetText = "";
		String replyName = "";
		
		int mapId = 0;
		
		SimpleDateFormat parser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		try{
			bw = new BufferedWriter(new FileWriter(storeFile));
			for(File f:dataFiles){
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				int numTweetPerFile = 0;
				int uId = 0;
				Date statusCreatedAt;
				String date = "";
				Long tweetId = 0L;
				Long replyId = 0L;
				
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
					
					//remove '\r' and '\n'
					tweetText = tweetText.replaceAll("\r", "");
					tweetText = tweetText.replaceAll("\n", "");
					tweetText = tweetText.replaceAll(",", "");
					tweetText = tweetText.replaceAll("\t", "");
					
                    
					
					//remove the tweets whose number of hashtags exceeds 10
					if(!isMoreHashtagsInside(tweetText, 10)){
					
						//read tweets who have reply information
						if(!tweet.getString("in_reply_to_status_id").equals("null")){
							numTweetPerFile++;
							
							//get the user's screenName
							screenName = tweet.getJSONObject("user").getString("screen_name");
							userId = screenNameIDMapping.get(screenName);
							
							replyName = tweet.getString("in_reply_to_screen_name");
							if(screenNameIDMapping.containsKey(replyName))
								uId = screenNameIDMapping.get(replyName);
							else if(screenNameIDMapping.containsKey(replyName.toLowerCase())){
								replyName = replyName.toLowerCase();
								uId = screenNameIDMapping.get(replyName);
							}
							else{
								uId = mapId;
								mapId++;
								System.out.println("NonMapping: " + replyName + " text: " + tweetText);
							}
							
							//parse date 'created_at'
							statusCreatedAt = parser.parse(tweet.getString("created_at"));
							date = formatter.format(statusCreatedAt);
							
							//write edge into file
							tweetId = tweet.getLong("id");
							bw.append(String.valueOf(tweetId)); //tweetId
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(screenName); //sceenName
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(String.valueOf(userId)); //MappedId
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(tweetText); //tweet
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(date); //date of the tweet
							bw.append(Constant.SEPARATOR_COMMA);
							replyId = tweet.getLong("in_reply_to_status_id");
							bw.append(String.valueOf(replyId)); //replied tweetId
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(replyName);//replied screenName
							bw.append(Constant.SEPARATOR_COMMA);
							bw.append(String.valueOf(uId));//replied MappedId
							
							bw.append('\n');
							
							bw.flush();
						}
					}
					
				}
				
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " replies in" + f.getName());
				numTweetPerFile = 0;
			}
			
			System.out.println("Total number of replies: " + numTweet);
			bf.close();
			bw.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
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
	
	//extract hashtag list from tweet
	public ArrayList<String> extractHashTags(String s) {
		//String[] words = s.split(Constant.SEPARATOR_SPACE);
		
		ArrayList<String> names = new ArrayList<String>();
		
		Pattern pattern = Pattern.compile("#([A-Za-z0-9_]+)");
		Matcher matcher = null;
		int start = 0;
		int end = 0;
			
		matcher = pattern.matcher(s);
		while(matcher.find()){
			start = matcher.start();
			end = matcher.end();
			names.add(s.substring(start, end));
		}
			
		return names;
	}
	
	public static void main(String[] args){
		ReadDataFromFile rf = new ReadDataFromFile();
		//rf.readMapping();
	    //rf.run();
		rf.parseUserId();
		
	}

}
