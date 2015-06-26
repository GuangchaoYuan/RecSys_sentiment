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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.xerox.socialmedia.communityrecommendation.dbmanager.SqlControler;
import com.xerox.socialmedia.communityrecommendation.utils.Constant;
import com.xerox.socialmedia.communityrecommendation.utils.IOUtil;

public class MentionGraph {
	private static String propertyFile = "config/resource.properties";
	private static Properties property = IOUtil.getProperties(propertyFile);
	private HashMap<String, Integer> screenNameIDMapping = new HashMap<String, Integer>();
	private HashMap<String, Integer> mentionMap = new HashMap<String, Integer>();
	private HashSet<String> userIdList = new HashSet<String>();
	private HashMap<Integer, Double> userSVOMap = new HashMap<Integer, Double>();
	
	HashSet<String> allNodes = new HashSet<String>();
	HashSet<String> trainNodes = new HashSet<String>();
	
	private int numTweet = 0; //total number of tweets
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
	
	//generate random edges
	public void generateRandomEdge(int numEdge){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\UserNameMapping.csv";
		ArrayList<String> uniqueIdList = new ArrayList<String>();
		HashSet<String> randomEdge = new HashSet<String>();
		String line = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 2){
					System.out.println("There is an error in reading file");
					continue;
				}
				uniqueIdList.add(words[1]);
			}
			reader.close();
			
			//generate random edge
			String source = "";
			String target = "";
			String edge = "";
			String reverse = "";
			int sIndex = 0;
			int tIndex = 0;
			int size = uniqueIdList.size();
			int num = 0;
			Random random = new Random();
			
			while(num<numEdge){
				sIndex = random.nextInt(size);
				source = uniqueIdList.get(sIndex);
				tIndex = random.nextInt(size);
				target = uniqueIdList.get(tIndex);
				
				edge = source + Constant.SEPARATOR_HYPHEN + target;
				reverse = target + Constant.SEPARATOR_HYPHEN + source;
				if(!randomEdge.contains(edge) && !randomEdge.contains(reverse)){
					randomEdge.add(edge);
					num++;
				}
			}
			
			//print hashset
			String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\random_graph.csv";
			this.printEdgeSet(randomEdge, storeFile);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	
	public void createGraph(){
		String dataFolder = property.getProperty("tweet.data.folder");
		File[] dataFiles = new File(dataFolder).listFiles();
		
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_obama_graph.csv";
		String edgeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_seven_days_obama_edgelist.csv";
		int numTweet = 0;
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		BufferedWriter bw = null;
		String screenName = "";
		int userId = 0;
		String tweetText = "";
		String RTName = "";
		ArrayList<String> mentionNames = new ArrayList<String>();
		ArrayList<Integer> retweet = new ArrayList<Integer>(); //indicate whether the mentioned screen_name is a retweet or not: 1 means retweet, 0 means mention
		
		try{
			bw = new BufferedWriter(new FileWriter(edgeFile, true));
			for(File f:dataFiles){
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				int numTweetPerFile = 0;
				int uId = 0;
				
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
						
						//create obama mention graph
						if(isContainHashTag(tweetText)){
							//get the user's screenName
							screenName = tweet.getJSONObject("user").getString("screen_name");
							userId = screenNameIDMapping.get(screenName);
							
							mentionNames = extractScreenNameFromMention(tweetText);
							
							//add retweet label to each mentioned screenName
							if(tweet.getInt("retweet_count") > 0){ //if this retweet is a retweet
								RTName = tweet.getJSONObject("retweeted_status").getJSONObject("user").getString("screen_name");
								for(int j = 0; j < mentionNames.size(); j++){
									if(mentionNames.get(j).equals(RTName))
										retweet.add(1);
									else
										retweet.add(0);
								}
							}
							else{
								for(int j = 0; j < mentionNames.size(); j++){
										retweet.add(0);
								}	
							}
							
							//write edge into file
							for(int i = 0; i < mentionNames.size(); i++){
								String name = mentionNames.get(i);
								uId = screenNameIDMapping.get(name);
								
								int label = retweet.get(i);
								
								bw.append(String.valueOf(userId));
								bw.append(Constant.SEPARATOR_COMMA);
								bw.append(String.valueOf(uId));
								bw.append(Constant.SEPARATOR_COMMA);
								bw.append(String.valueOf(label));
								bw.append('\n');
							}
							bw.flush();
						}
					}
					mentionNames.clear();
					retweet.clear();
				}
				
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " records in" + f.getName());
				numTweetPerFile = 0;
			}
			
			System.out.println("Total number of tweets: " + numTweet);
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
		}
		
	}
	
	
	//create mention table from json files
	public void run(){
		readUserIdList();
		String dataFolder = "K:\\Xerox\\gyuan\\dataset\\StreamData";
		//String dataFolder = "D:\\Social_Media_Analytics\\StreamData1";
		File[] dataFiles = new File(dataFolder).listFiles();
		
		InputStreamReader isr = null;
		BufferedReader bf = null;
		String line= "";
		JSONObject tweet = null;
		
		try{
			//bw = new BufferedWriter(new FileWriter(storeFile));
			for(File f:dataFiles) //go through all files
		    {
				isr = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
				bf = new BufferedReader(isr);
				String screenName = "";
				String tweetText = "";
				int numTweetPerFile = 0;
				String originalId = "";
				int userId = 0;
				ArrayList<String> mentionNames = new ArrayList<String>();
				int uId = 0;
				String mentionKey = "";
				int mentionWeight = 0;
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
						originalId = tweet.getJSONObject("user").getString("id");
						//remove the tweets whose author's number of tweets doesn't exceeds 5
						if(!userIdList.contains(originalId)){
							continue;
						}
						
						//get the user's screenName
						screenName = tweet.getJSONObject("user").getString("screen_name");
						userId = screenNameIDMapping.get(screenName);			
						
						mentionNames = extractScreenNameFromMention(tweetText);
						//put the mention-mapping into the hashmap
						for(int i = 0; i < mentionNames.size(); i++){
							String name = mentionNames.get(i);
							uId = screenNameIDMapping.get(name);
							
							//skip the self-loop
							if(uId == userId)
								continue;
							mentionKey = String.valueOf(userId) + Constant.SEPARATOR_HYPHEN + String.valueOf(uId);
							if(mentionMap.containsKey(mentionKey)){
								mentionWeight = mentionMap.get(mentionKey);
								mentionMap.put(mentionKey, mentionWeight+1);
							}
							else
								mentionMap.put(mentionKey, 1);
						}
						
						//remove '\r' and '\n'
						/*tweetText = tweetText.replaceAll("\r", " ");
						tweetText = tweetText.replaceAll("\n", " ");
						tweetText = tweetText.replaceAll(",", " ");
						tweetText = tweetText.replaceAll("\t", " ");
						
						bw.append(tweet.getString("id"));
						bw.append(Constant.SEPARATOR_COMMA);
						bw.append(tweet.getJSONObject("user").getString("id"));
						bw.append(Constant.SEPARATOR_COMMA);
						
						bw.append(tweetText);
						bw.append(Constant.SEPARATOR_COMMA);
						
												
						bw.append('\n');
						bw.flush();*/
						
						mentionNames.clear();	
					}
				}
				numTweet += numTweetPerFile;
				System.out.println(numTweetPerFile + " records in" + f.getName());
				numTweetPerFile = 0;
		    }
			
			//write the hashmap into a file
			printMap(mentionMap);
			//bw.close();
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
	
	
	private HashMap<String, Integer> readMentionTable(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mention_table_DB.csv";
		String line = "";
		String key = "";
		HashMap<String, Integer> mMap = new HashMap<String, Integer>();
		int num = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in reading file");
					continue;
				}
				key = words[0]+Constant.SEPARATOR_HYPHEN+words[1];
				mMap.put(key, Integer.parseInt(words[2]));
			}
			reader.close();
			System.out.println("There are " + num + " records in the mention table");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		return mMap;
	}
	
	//read the mention graph into a hashset
	private HashSet<String> readMentionGraph(String mentionFile){
		HashSet<String> graph = new HashSet<String>();
		String line = "";
		String key = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mentionFile));
			while((line = reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 4){
					System.out.println("There is an error in reading file");
					continue;
				}
				key = words[0]+Constant.SEPARATOR_HYPHEN+words[1];
				graph.add(key);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		return graph;
	}
	
	//create the list whose edges don't exist in the mention graph
	public void filterNonMentionGraph(int weight){
		String mentionFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_"+String.valueOf(weight)+".csv";
		HashSet<String> mMap = new HashSet<String>();
		mMap = readMentionGraph(mentionFile);
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mention_table_DB.csv";	
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\Non_mutual_mention_graph_DB_"+String.valueOf(weight)+".csv";
	
		HashSet<String> edge = new HashSet<String>();
		
		String line = "";
		String key = "";
		String reverseKey = "";
		int value = 0;
		int num = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in reading file");
					continue;
				}
				
				key = words[0]+Constant.SEPARATOR_HYPHEN+words[1];
				reverseKey = words[1]+Constant.SEPARATOR_HYPHEN+words[0];
				
				if(!edge.contains(key)){
					//store the record whose weight is less than the weight threshold
					value = Integer.parseInt(words[2]);
					if(value<weight){
						num++;
						writer.append(words[0] + Constant.SEPARATOR_COMMA + words[1]);
						edge.add(reverseKey);
						writer.append("\n");
						writer.flush();
						if(num % 10000 == 0)
							System.out.println(num + " mutal edges have been processed!");
					}
					//search for its reserve edge				
					else if(!mMap.contains(key)&&!mMap.contains(reverseKey)){
						num++;
						writer.append(words[0] + Constant.SEPARATOR_COMMA + words[1]);
						edge.add(reverseKey);
						writer.append("\n");
						writer.flush();
						if(num % 10000 == 0)
							System.out.println(num + " mutal edges have been processed!");
					}

				}				
			}
			reader.close();
			writer.close();
			System.out.println("There are "+num + " non mutal edges in total!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	//filter a mention graph based on the specified weight threshold
	public void filterMentionGraph(int weight){
		HashMap<String, Integer> mMap = new HashMap<String, Integer>();
		HashSet<String> edge = new HashSet<String>();
		mMap = readMentionTable();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mention_table_DB.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_"+String.valueOf(weight)+".csv";
		String line = "";
		String key = "";
		int value = 0;
		int num = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in reading file");
					continue;
				}
				
				//skip the record whose weight is less than the weight threshold
				value = Integer.parseInt(words[2]);
				if(value<weight)
					continue;
				
				//search for its reverse edge
				key = words[1]+Constant.SEPARATOR_HYPHEN+words[0];
				//avoid to process the reverse edge again: the relationship is symmetric
				if(!edge.contains(words[0]+Constant.SEPARATOR_HYPHEN+words[1])){
					if(mMap.containsKey(key) && (value=mMap.get(key))>=weight){
						num++;
						writer.append(words[0]);
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(words[1]);
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(words[2]);
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(String.valueOf(value));
						writer.append("\n");
						writer.flush();
						
						edge.add(key);
						if(num % 10000 == 0)
							System.out.println(num + " mutal edges have been processed!");
					}
				}	
			}
			reader.close();
			writer.close();
			System.out.println("There are "+num + " mutal edges in total!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		
	}
	
	//create mention table from file
	public void mentionTable(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_one_month.csv";
		
		HashMap<String, Integer> mentionMap = new HashMap<String, Integer>();
		BufferedReader reader = null;
		BufferedWriter writer  = null;
		String line = "";
		int num = 0;
		String tweet = "";
		String screenName = "";
		int userId = 0;
		int uId = 0;
		ArrayList<String> mentionNames = new ArrayList<String>();
		String mentionKey = "";
		int mentionWeight = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while ((line = reader.readLine()) != null){
				num++;
				String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 8){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				tweet = words[3];
				screenName = words[2];
				userId = Integer.parseInt(words[5]);
				
				mentionNames = extractScreenNameFromMention(tweet);
				
				//put the mention-mapping into the hashmap
				for(int i = 0; i < mentionNames.size(); i++){
					String name = mentionNames.get(i);
					if(screenNameIDMapping.containsKey(name))
						uId = screenNameIDMapping.get(name);
					else{
						System.out.println("num: " + num + " screenName: " + name + " isn't in the mapping");
						continue;
					}
					
					//skip the self-loop
					if(uId == userId)
						continue;
					mentionKey = String.valueOf(userId) + Constant.SEPARATOR_HYPHEN + String.valueOf(uId);
					if(mentionMap.containsKey(mentionKey)){
						mentionWeight = mentionMap.get(mentionKey);
						mentionMap.put(mentionKey, mentionWeight+1);
					}
					else
						mentionMap.put(mentionKey, 1);
				}
				
				mentionNames.clear();		
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			
			System.out.println("There are " + num + " records in total");
			//write the hashmap into a file
			printMap(mentionMap);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//create mention table from database
	public void mentionTablefromDB(){
		int fetchSize = 1000;
		SqlControler sqlControler;
		sqlControler = new SqlControler();
		sqlControler.createConnection();
		
		PreparedStatement ps = null;
		String selectQuery = "select unique_id, screen_name, source " +
				"from tweets_sentiment_one_month limit ?,"+fetchSize;
		ResultSet rs = null;
		ps = sqlControler.GetPreparedStatement(selectQuery);
		
		
		HashMap<String, Integer> mentionMap = new HashMap<String, Integer>();
		int num = 0;
		String tweet = "";
		String screenName = "";
		int userId = 0;
		int uId = 0;
		ArrayList<String> mentionNames = new ArrayList<String>();
		String mentionKey = "";
		int mentionWeight = 0;
		
		//read data and process mention table from DB
		while(num % fetchSize == 0 && num <= 3970000){
			try {
				ps.setInt(1, num);
				rs = ps.executeQuery();
				while(rs.next()){
					num++;
					//user_id = rs.getLong(1);
					userId = rs.getInt(1);
					screenName = rs.getString(2);
					tweet = rs.getString(3);
					
					mentionNames = extractScreenNameFromMention(tweet);
					//put the mention-mapping into the hashmap
					for(int i = 0; i < mentionNames.size(); i++){
						String name = mentionNames.get(i);
						if(screenNameIDMapping.containsKey(name))
							uId = screenNameIDMapping.get(name);
						else{
							System.out.println("num: " + num + " screenName: " + name + " isn't in the mapping");
							continue;
						}
						
						//skip the self-loop
						if(uId == userId)
							continue;
						mentionKey = String.valueOf(userId) + Constant.SEPARATOR_HYPHEN + String.valueOf(uId);
						if(mentionMap.containsKey(mentionKey)){
							mentionWeight = mentionMap.get(mentionKey);
							mentionMap.put(mentionKey, mentionWeight+1);
						}
						else
							mentionMap.put(mentionKey, 1);
					}
					
					mentionNames.clear();
					
					if(num % 10000 == 0)
						System.out.println(num + " records have been processed from DB!");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("There are " + num + " records in total");
		//write the hashmap into a file
		printMap(mentionMap);
			
	}
	
	
	//for a given hashtag, compute the average SVO similarity for every edge in a mention graph
	public void averageSVOMentionGraph(String hashtag, int weight, int size){
		readUserListFromDB(hashtag, size);
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_"+String.valueOf(weight)+".csv";
		String nonConnectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\Non_mutual_mention_graph_DB_1.csv";
		//String nonConnectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\random_graph.csv";
		int num = this.svoConnectedUsers(connectedFile);
		this.svoNonConnectedUsers(nonConnectedFile, num);
		
	}

	//compute the average svo similarity scores among connected pairs
	private int svoConnectedUsers(String inputFile) {
		ArrayList<Double> similarityList = new ArrayList<Double>();
		BufferedReader reader = null;
		String line = "";
		int source = 0;
		int target = 0;
		double sourceSVO = 0;
		double targetSVO = 0;
		double simiScore = 0;
		double averScore = 0;
		int num = 0;
		int numSimilar = 0; // number of pairs that have the same sentiment
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while ((line = reader.readLine()) != null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 4){
					System.out.println("There is an error in reading file");
					continue;
				}
				
				source = Integer.valueOf(words[0]);
				target = Integer.valueOf(words[1]);
				
				//only consider the pairs that both of nodes have the SVO scores
				if(userSVOMap.containsKey(source)&& userSVOMap.containsKey(target)){
					num++;
					sourceSVO = userSVOMap.get(source);
					targetSVO = userSVOMap.get(target);
					//compute SVO similarity score
					//simiScore = this.svoSimilarity(sourceSVO, targetSVO);
					//similarityList.add(simiScore);
					
					//check whether the pair has the same sentiment
					if(sourceSVO==targetSVO)
						numSimilar++;
				}
				
			}
			
			//compute the average similarity score of all pairs
			//averScore = this.averageSimiScore(similarityList);
			
			double prob = (double) numSimilar/num;
			//System.out.println("The average similarity score of connected users is: " + averScore);
			System.out.println("The probability of connected users having the same sentiment is: " + prob);
			System.out.println("The number of connected users that have similarity score: " + num);
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return num;
	}
	
	//compute the average svo similarity scores among non_connected pairs
	private void svoNonConnectedUsers(String inputFile, int overall) {
		ArrayList<Double> similarityList = new ArrayList<Double>();
		BufferedReader reader = null;
		String line = "";
		int source = 0;
		int target = 0;
		double sourceSVO = 0;
		double targetSVO = 0;
		double simiScore = 0;
		double averScore = 0;
		int numSimilar = 0;
		//int num = 0;
		int numTotal = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while ((line = reader.readLine()) != null && numTotal<overall){
				//num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 2){
					System.out.println("There is an error in reading file");
					continue;
				}
				
				source = Integer.valueOf(words[0]);
				target = Integer.valueOf(words[1]);
				
				//only consider the pairs that both of nodes have the SVO scores
				if(userSVOMap.containsKey(source)&& userSVOMap.containsKey(target)){
					numTotal++;
					sourceSVO = userSVOMap.get(source);
					targetSVO = userSVOMap.get(target);
					//compute SVO similarity score
					//simiScore = this.svoSimilarity(sourceSVO, targetSVO);
					//similarityList.add(simiScore);
					if(sourceSVO==targetSVO)
						numSimilar++;
				}
				
			}
			
			//compute the average similarity score of all pairs
			//averScore = this.averageSimiScore(similarityList);
			
			double prob = (double) numSimilar/numTotal;
			
			//System.out.println("The average similarity score of Non_connected users is: " + averScore);
			System.out.println("The probability of Non_connected users having the same sentiment is: " + prob);
			System.out.println("The number of Non_connected users that have similarity score: " + numTotal);
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//compute the average similarity score of all pairs
	private double averageSimiScore(ArrayList<Double> list){
		double aver = 0;
		for(int i = 0; i < list.size(); i++){
			aver += list.get(i);
		}
		aver = aver/list.size();
		return aver;
	}
	//compute SVO similarity score between two scores
	private double svoSimilarity(double s, double t){
		double score = 0;
		double dist = Math.abs(s-t);
		score = 1/(1+dist);
		return score;
	}
	//search for the user list who has talked about the hashtag from DB
	private void readUserListFromDB(String hashtag, int size){
		int fetchSize = 1000;
		SqlControler sqlControler;
		sqlControler = new SqlControler();
		sqlControler.createConnection();
		
		PreparedStatement ps = null;
		/*String selectQuery = "select unique_id, SVO from tweets_hashtag_one_month" +
				" where hashtag = '" + hashtag +  "' limit ?,"+fetchSize;*/
		
		String selectQuery = "select unique_id, aver_pos, aver_neg from tweets_hashtag_one_month" +
				" where hashtag = '" + hashtag +  "' limit ?,"+fetchSize;
		
		ResultSet rs = null;
		ps = sqlControler.GetPreparedStatement(selectQuery);
		
		int num = 0;
		int userId = 0;
		//double svo = 0;
		double pos = 0;
		double neg = 0;
		double same = 0;//1 means positive; 0 means neutral; -1 means negative
		//read data and process mention table from DB
		while(num % fetchSize == 0 && num <= size){
			try {
				ps.setInt(1, num);
				rs = ps.executeQuery();
				while(rs.next()){
					num++;
					userId = rs.getInt(1);
					//svo = rs.getDouble(2);
					pos = rs.getDouble(2);
					neg = rs.getDouble(3);
					
					if(pos>neg)
						same = 1;
					else if(pos<neg)
						same = -1;
					else
						same = 0;
					//userSVOMap.put(userId, svo); // put the pairs into hashmap
					userSVOMap.put(userId, same);
					if(num%1000==0)
						System.out.println(num + " records have been processed from DB");
				} 
			}catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		sqlControler.close();
	}
	
	//print the hashmap to a file
	private void printMap(HashMap<String, Integer> map){
		BufferedWriter writer  = null;
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mention_table_DB.csv";
		String key = "";
		int value = 0;
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = map.entrySet().iterator();
			while(it.hasNext()){
				num++;
				Map.Entry<String, Integer> pairs = (Map.Entry<String, Integer>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
				String[] nodes = key.split(Constant.SEPARATOR_HYPHEN);
				writer.append(nodes[0] + Constant.SEPARATOR_COMMA + nodes[1]);
				writer.append(Constant.SEPARATOR_COMMA + Integer.toString(value));
				writer.append("\n");
				writer.flush();
				
				if(num % 100000 == 0)
					System.out.println(num + " records have been written!");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//print the edge hashset (each element is an edge)
	private void printEdgeSet(HashSet<String> map, String output){
		BufferedWriter writer  = null;
		String storeFile = output;
		String key = "";
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = map.iterator();
			while(it.hasNext()){
				num++;
				key = (String) it.next();
				String[] nodes = key.split(Constant.SEPARATOR_HYPHEN);
				writer.append(nodes[0] + Constant.SEPARATOR_COMMA + nodes[1]);
				writer.append("\n");
				writer.flush();
			}
			writer.close();
			System.out.println(num + " edges in the random graph!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//print the node hashset (each element is a node)
	private void printNodeSet(HashSet<Integer> map, String output){
		BufferedWriter writer  = null;
		String storeFile = output;
		int key = 0;
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = map.iterator();
			while(it.hasNext()){
				num++;
				key = (Integer) it.next();
				writer.append(String.valueOf(key));
				writer.append("\n");
				writer.flush();
			}
			writer.close();
			System.out.println(num + " edges in the random graph!");
		} catch (IOException e) {
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
	
	//read the unique userId list whose number of tweets exceeds 5
	public void readUserIdList(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\userIdList_one_month";
		String line = "";
		int num = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				userIdList.add(line);
				num++;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		System.out.println("The number of unique userId is: " + num);
	}
	
	public int numNodes(int weight){
		HashSet<String> nodes = new HashSet<String>();
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_"+String.valueOf(weight)+".csv";
		nodes = readNodesFromGraph(connectedFile);
		return nodes.size();
	}

	//read nodes from a graph to construct a hashset
	private HashSet<String> readNodesFromGraph(String input) {
		String line = "";
		HashSet<String> nodes = new HashSet<String>();
		BufferedReader reader =  null;
		try {
			reader = new BufferedReader(new FileReader(input));
			while ((line = reader.readLine()) != null){
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 4){
					System.out.println("There is an error in reading file");
					continue;
				}
				nodes.add(words[0]);
				nodes.add(words[1]);
			}
			reader.close();
				
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nodes;
	}
	
	
	//generate training file for train and test file
	public void generateTrainingFile(int lower, int upper, int threshold){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\randomSourceNodeList_20n.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_20.csv";
		String output = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates_20n.csv";
		int num = 0;
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		//generate random source nodes
		//getRandomSourceNodes(num, lower, upper, inputFile, mentionGraph);
		this.getSourceNodes(lower, upper, inputFile, mentionGraph);
		
		BufferedReader reader =  null;
		BufferedWriter writer = null;
		String line = "";
		int source = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(output));
			System.out.println("Start to generate 2-hop candidates");
			while ((line = reader.readLine()) != null){
				source = Integer.parseInt(line);
				boolean write = mentionGraph.getTwoHopCandidates(source, writer, threshold);
				if(write)
					num++;
			}
			reader.close();
			writer.close();
			System.out.println("There are " + num + " source nodes");
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//for a given graph(mentionGraph), generate num (num) of random nodes whose degree 
	//exceeds a threshold and store the nodes into a file (inputFile)
	private void getRandomSourceNodes(int num, int lower, int upper, String inputFile,
			Graph mentionGraph) {
		int size = mentionGraph.size();
		//convert the hashset into an Array
		Integer[] list = mentionGraph.keySet().toArray(new Integer[size]);
		HashSet<Integer> sourceSet = new HashSet<Integer>();
		Random random = new Random();
		int index = 0;
		int nodeId = 0;
		int degree = 0;
		
		//generate the random source node
		for(int i = 0; i < num;){
			index = random.nextInt(size);
			nodeId = list[index];
			//skip the nodes whose degree is less than degree
			degree = mentionGraph.getDegree(nodeId);
			if((degree <lower) || (degree > upper))
				continue;
			if(!sourceSet.contains(nodeId)){
				sourceSet.add(nodeId);
				i++;
			}			
		}
		
		this.printNodeSet(sourceSet, inputFile);
	}
	
	//get the source nodes based on some threshold
	private void getSourceNodes(int lower, int upper, String inputFile,
			Graph mentionGraph) {
		int size = mentionGraph.size();
		//convert the hashset into an Array
		Integer[] list = mentionGraph.keySet().toArray(new Integer[size]);
		HashSet<Integer> sourceSet = new HashSet<Integer>();
		int nodeId = 0;
		int degree = 0;
		
		for(int i = 0; i < list.length; i++){
			nodeId = list[i];
			degree = mentionGraph.getDegree(nodeId);
			if((degree>=lower) && (degree <= upper)){
				if(!sourceSet.contains(nodeId)){
					sourceSet.add(nodeId);
				}
			}
		}
		
		this.printNodeSet(sourceSet, inputFile);
	}
	
	//whether the tweet contains hashtag #obama
	private boolean isContainHashTag(String s){
		boolean contain = false;
		s = s.toLowerCase();
		if(s.contains("#obama"))
			contain = true;
		return contain;
	}
	
	//split nodes into a set of train nodes and a set of test nodes
	private void splitNodes(String inputFile, double ratio){
		BufferedReader reader = null;
		String line = "";
		String source = "";
		String sourceOriginal = "";
		int num = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));		
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				//skip the first line
				if(num==1)
					continue;
				
				if(words.length!=21){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
			
				source = words[0];
				if(!source.equals(sourceOriginal)){
					allNodes.add(source);
					sourceOriginal = source;
				}
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			reader.close();
			
			//generate train nodes
			ArrayList<String> nodeList = new ArrayList<String>(allNodes);
			int allSize = allNodes.size();
			int size = (int) (allSize*ratio);
			Random seed = new Random();
			String node = "";
			for(int i = 0; i < size; ){
				node = nodeList.get(seed.nextInt(allSize));
				if(!trainNodes.contains(node)){
					trainNodes.add(node);
					i++;
				}
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	//First way to split file: split users into 7:3 train:test
	public void splitFileWayOne(double ratio){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\normalize_graph_feature_7n_rec.csv";
		String trainFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\normalize_graph_feature_7n_rec_train.csv";
		String testFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\normalize_graph_feature_7n_rec_test.csv";
		this.splitNodes(inputFile, ratio);
		
		BufferedReader reader = null;
		BufferedWriter writer1 = null;
		BufferedWriter writer2 = null;
		
		String line = "";
		int num = 0;
		String source = "";
		String sourceOriginal = "";
		int train = 1; //1 means train file, otherwise means test file
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer1 = new BufferedWriter(new FileWriter(trainFile));
			writer2 = new BufferedWriter(new FileWriter(testFile));
			
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				//skip the first line
				if(num==1){
					writer1.append(line);
					writer1.append("\n");
					writer1.flush();
					
					writer2.append(line);
					writer2.append("\n");
					writer2.flush();
					continue;
				}
				
				source = words[0];
				if(source!=sourceOriginal){
					if(trainNodes.contains(source))
						train = 1;
					else
						train = 0;
					
					sourceOriginal = source;
				}


				if(train==1){//write to train file
					writer1.append(line);
					writer1.append("\n");
					writer1.flush();
				}
				else{ //write to test file
					writer2.append(line);
					writer2.append("\n");
					writer2.flush();
				}
					
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			reader.close();
			writer1.close();
			writer2.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public static void main(String[] args){
		MentionGraph mg = new MentionGraph();
		//mg.readMapping();
		//mg.createGraph();
		//mg.mentionTable();
		//mg.run();
		//mg.filterMentionGraph(20);
		//mg.filterNonMentionGraph(1);
		//mg.mentionTablefromDB();
		//mg.averageSVOMentionGraph("#ows", 3, 5000);
		//int num = mg.numNodes(3);
		//System.out.println("The number of nodes is: " + num);
		//mg.generateRandomEdge(100000);
		//mg.generateTrainingFile(1, 50, 0);
		mg.splitFileWayOne(0.5);
	}

}
