package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.ParseException;

import jsc.distributions.Normal;

import com.xerox.socialmedia.communityrecommendation.utils.Constant;


public class SentimentAnalyzer {
	/**
	 * @param 
	 */
	private static SentimentAnalyzer instance = null;
	private static final String PATH = "D:\\Social_Media_Analytics\\Anew\\ANEW\\stopwords.csv";
	private HashSet<String> stopSet;
	
	class Result {
		double valence;
		double arousal;
	}
	
	class TweetProfile {
		String screenName;
		int mapId;
		String text;
		String createDate;
	}

	private SentimentAnalyzer() {
		readStoplist(PATH);
	}

	public static SentimentAnalyzer getInstance() {
		if(instance == null){
			instance = new SentimentAnalyzer();
		}
		return instance;
	}

 	 
	/**
	 * Porter Stemming Algorithm
	 * For example: "fishing", "fished", "fish", and "fisher" are reduced to the root word, "fish".
	*/	
	@SuppressWarnings("unchecked")
	private void stemmerWords(HashSet<String> set) {
		HashSet<String> tempSet = (HashSet<String>) set.clone();
		set.clear();
		Stemmer s = new Stemmer();
		Iterator<String> wordIter = tempSet.iterator();
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			char[] cWord = sWord.toCharArray();
			s.add(cWord, sWord.length());
			s.stem();
			if(s.getResultLength() > 0) {
				set.add(s.toString());
			}
		}	

	}
	
	/**
	 *	Read the stop words list into buffer
	 *  Path is defined on the top
	 *	There are 491 stop words in the list
	*/
	private void readStoplist(String sPath) {
		stopSet = new HashSet<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(sPath));
			String sLine = "";
			while((sLine = in.readLine()) != null) {
				stopSet.add(sLine);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *	Remove the stop words from word set
	*/
	private void removeStopwords(HashSet<String> set) {
		Iterator<String> wordIter = set.iterator();
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			if(stopSet.contains(sWord))
				wordIter.remove();
			}	
	}

	private boolean isString(String str) {
		Pattern patt = Pattern.compile("[a-z,A-Z]*");
		Matcher isString = patt.matcher(str);
		if (isString.matches()) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 *	Remove non-english words
	*/
	private void removeNonstr(HashSet<String> set) {
		Iterator<String> wordIter = set.iterator();
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			if(!isString(sWord))
				wordIter.remove();
			}		
	}

	/**
	 *	Remove stop words, do the stemming process, and remove all non-english words
	*/
	public HashSet<String> cleanContent(String content) {
		content = content.toLowerCase();
		
		if(content.startsWith("RT"))  //remove "RT"
				content = content.replace("RT", "");
		content = removeURL(content); //remove url
		content = removeMention(content); //remove mention
		
		HashSet<String> wordSet = new HashSet<String>();
		StringTokenizer tokens = new StringTokenizer(content, " ");
		
		//replace digit with letter
		if(tokens.countTokens() != 0) {
			while(tokens.hasMoreTokens()){
				String sWord = tokens.nextToken();
				sWord = sWord.replaceAll("[^A-Za-z']", "");
				if(sWord.length() > 0)
					wordSet.add(sWord);
			}
		}
		else {
			content = content.toLowerCase();
			content = content.replaceAll("[^A-Za-z']", "");
			if(content.length() > 0)
				wordSet.add(content);
		}
		
		removeStopwords(wordSet);
		stemmerWords(wordSet);
		removeNonstr(wordSet);
		
		return wordSet;
	}
	
	//find and remove url
	private String removeURL(String s) {
		//String[] words = s.split(Constant.SEPARATOR_SPACE);
		
		Pattern pattern = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher matcher = null;
		int start = 0;
		int end = 0;
		
		ArrayList<String> subString = new ArrayList<String>();
		
		matcher = pattern.matcher(s);
		while(matcher.find()){
			start = matcher.start();
			end = matcher.end();
			subString.add(s.substring(start, end));
		}
		
		for(int i = 0; i < subString.size(); i++){
			s = s.replace(subString.get(i), "");
		}
		
		return s;
	}
	
	//find and remove mention(@)
	private String removeMention(String s) {
		//String[] words = s.split(Constant.SEPARATOR_SPACE);
		
		ArrayList<String> names = new ArrayList<String>();
		
		Pattern pattern = Pattern.compile("@([A-Za-z0-9_]+)");
		Matcher matcher = null;
		int start = 0;
		int end = 0;
		
		matcher = pattern.matcher(s);
		while(matcher.find()){
			start = matcher.start();
			end = matcher.end();
			names.add(s.substring(start, end));
		}
		
		for(int i = 0; i < names.size(); i++){
			s = s.replace(names.get(i), "");
		}
		
		return s;
	}
	
	//whether the tweet contains hashtag #obama or #obama2012
	private boolean isContainHashTag(String s){
		boolean contain = false;
		s = s.toLowerCase();
		if(s.contains("#obama"))
			contain = true;
		return contain;
	}
		
	//get sentimental score
	public Result getArithmean(HashMap<String, ANEW.Entry> set) {
		if(set.size() == 0)
			return null;
		Result res = new Result();
		Iterator<String> wordIter = set.keySet().iterator();
		double dValence = 0;
		double dArousal = 0;
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			ANEW.Entry entry = set.get(sWord);
			dValence += entry.valenceMean;
			dArousal += entry.arousalMean;
		}
		res.valence = dValence/set.size();
		res.arousal = dArousal/set.size();
		return res;
	}
	
	public void outputDetails(HashMap<String, ANEW.Entry> set) {
		Iterator<String> wordIter = set.keySet().iterator();
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			ANEW.Entry entry = set.get(sWord);
			System.out.println(sWord + ": " + entry);
		}
	}
	
	/**
	 *	Using ANEW dictionary to get the sentimental information of the words in a sentence
	 *	Return the Result class defined on the top
	*/
	public Result sentimentANEW(String content) {
		HashMap<String, ANEW.Entry> entrySet = new HashMap<String, ANEW.Entry>();
		ANEW anew = ANEW.getInstance();
		HashSet<String> wordSet = cleanContent(content);
		
		
		Iterator<String> wordIter = wordSet.iterator();
		while(wordIter.hasNext()) {
			String sWord = wordIter.next();
			ANEW.Entry entry = anew.get(sWord);
			if(entry != null) {
				entrySet.put(sWord, entry);
			}
		}
		/*show the sentiment details*/
		//outputDetails(entrySet);
		/*get the Arithmetic Mean*/
		return getArithmean(entrySet);
		
		
	}
	
	//read and write sentiment flow graph into file
	public void sentimentFlow(){
		System.out.println("read sentiment flow file");
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_reply_graph.csv";
		BufferedReader reader = null;
		HashMap<Long, TweetProfile> tweetMap = new HashMap<Long, TweetProfile>();
		String line = "";
		int num = 0;
		long tweetId = 0L;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line=reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length!=8){
					System.out.println("There is an error in line: " + num + " content: " + line);
					continue;
				}
				
				tweetId = Long.parseLong(words[0]);
				TweetProfile tp = new TweetProfile();
				tp.screenName = words[1];
				tp.mapId = Integer.parseInt(words[2]);
				tp.text = words[3];
				tp.createDate = words[4];
				
				if(!tweetMap.containsKey(tweetId))
					tweetMap.put(tweetId, tp);
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			reader.close();
			computeSentimentFlow(tweetMap,inputFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	private void computeSentimentFlow(HashMap<Long, TweetProfile> tweetMap, String inputFile){
		System.out.println("Start to compute sentiment!");
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_reply_sentimentFlow.csv";
		BufferedReader reader = null;
		BufferedWriter writer  = null;
		String line = "";
		int num = 0;
		int sentiNum = 0;
		
		String sourceText = "";
		long sourceId = 0L;
		TweetProfile tweetP = null;
		
		Result res = null;
		Result sourceRes = null;
		SentimentAnalyzer sa = SentimentAnalyzer.getInstance();
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while ((line = reader.readLine()) != null){
				num++;
				String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length!=8){
					System.out.println("There is an error in line: " + num + " content: " + line);
					continue;
				}
				
				//ignore the records whose sourceId is not in the dataset
				sourceId = Long.parseLong(words[5]);
				if(!tweetMap.containsKey(sourceId))
					continue;
				
				tweetP = tweetMap.get(sourceId);
				sourceText = tweetP.text;
				//compute sentiment for sourceText
				sourceRes = sa.sentimentANEW(sourceText);
				
				//compute sentiment for childText
				res = sa.sentimentANEW(words[3]);
				
				//write child's information into file
				writer.append(words[0]); //child's tweetId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[1]); //child's screenName
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[2]); //childs' mapId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[3]); //child's tweet
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[4]); //child's date
				writer.append(Constant.SEPARATOR_COMMA);
				
				if(res != null) {
					writer.append(String.valueOf(res.valence)); //child's valence
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(sentiLevel(res.valence)); //child's valence level
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(res.arousal)); //child's arousal
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(sentiLevel(res.arousal)); //child's arousal level
					writer.append(Constant.SEPARATOR_COMMA);
				}
				else{
					writer.append(String.valueOf(0));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append("Z");
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(0));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append("Z");
					writer.append(Constant.SEPARATOR_COMMA);
					
				}
				
				//write source's information into file
				writer.append(words[5]); //source tweetId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(tweetP.screenName); //source screenName
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(tweetP.mapId)); //source mapId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(tweetP.text); //source tweet
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(tweetP.createDate); //source date
				writer.append(Constant.SEPARATOR_COMMA);
				
				if(sourceRes != null) {
					writer.append(String.valueOf(sourceRes.valence)); //source valence
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(sentiLevel(sourceRes.valence)); //source valence level
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sourceRes.arousal)); //source arousal 
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(sentiLevel(sourceRes.arousal)); //source arousal level
					writer.append(Constant.SEPARATOR_COMMA);
				}
				else{
					writer.append(String.valueOf(0));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append("Z");
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(0));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append("Z");	
					writer.append(Constant.SEPARATOR_COMMA);
				}
				
				//compute date difference
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
				
				Date child = formatter.parse(words[4]);
				Date parent = formatter.parse(tweetP.createDate);
				long timeDiff = child.getTime() - parent.getTime();
				if(timeDiff<0){
					System.out.println("There is a difference in time difference: " + timeDiff + " child:" + child + " parent: " 
							+ parent);
					timeDiff = 0L;
				}
				
				writer.append(String.valueOf(timeDiff));
				
				writer.append('\n');
				writer.flush();
				sentiNum++;
					
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			writer.close();
			reader.close();
			System.out.println(sentiNum + " records of sentiment flow!");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void computeSentiment(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_obama_sentiment_mapping.csv";
		BufferedReader reader = null;
		BufferedWriter writer  = null;
		String line = "";
		int num = 0;
		int numSpam = 0;
		int numNonObama = 0;
		int numSenti = 0;
		int numNonSenti = 0;
		Result res = null;
		SentimentAnalyzer sa = SentimentAnalyzer.getInstance();
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while ((line = reader.readLine()) != null){
				num++;
				String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 6){
					System.out.println("There is an error in line: " + num + " : " + line);
					for(int i = 0; i < words.length; i++)
						System.out.print(i + ":"+words[i]+" ");
					numSpam++;
					continue;
				}
				
				//ignore the tweets without #obama
				if(!isContainHashTag(words[3])){
					numNonObama++;
					continue;
				}
				
				res = sa.sentimentANEW(words[3]);
				if(res != null) {
					writer.append(words[5]);
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(res.valence));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(res.arousal));
					numSenti++;	
				}
				else{
					writer.append(words[5]);
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(0));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(0));
					numNonSenti++;
				}
				
				writer.append('\n');
				writer.flush();
					
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			writer.close();
			reader.close();
			System.out.println(num + " records in the original file");
			System.out.println(numSpam + " spam records");
			System.out.println(numNonObama + " nonObama records");
			System.out.println(numSenti + " records in the sentiment file");
			System.out.println(numNonSenti + " records in the sentiment file without sentiment");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//for each user, mapping its communityId with its sentiment
	public void mapCommunitySentiment(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_obama_sentiment_mapping.csv";
		HashMap<Integer, Result> userSenti = new HashMap<Integer, Result>();//store sentiment of each user
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int userId = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while ((line = reader.readLine()) != null){
				String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				userId = Integer.parseInt(words[0]);
				Result senti = new Result();
				senti.valence = Double.parseDouble(words[1]);
				senti.arousal = Double.parseDouble(words[2]);
				
				//put user_id and senti mapping into hashmap
				if(!userSenti.containsKey(userId))
					userSenti.put(userId, senti);
				
				num++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			reader.close();
			writeCliqueSentimentLevel(userSenti);
			
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//write community and sentiment mapping into a file: cliques
	private void writeCliqueSentiment(HashMap<Integer, Result> userSenti){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\cliques.txt";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\clique_senti.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int userId = 0;
		double val = 0;
		double aro = 0;
		
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			reader = new BufferedReader(new FileReader(inputFile));
			
			reader.readLine(); //skip first line
			while ((line = reader.readLine()) != null){
				String words[] = line.split(Constant.SEPARATOR_TAB);
				
				//process one community
				for(int i = 0; i < words.length; i++){
					userId = Integer.parseInt(words[i]);
					if(userSenti.containsKey(userId)){
						val = userSenti.get(userId).valence;
						aro = userSenti.get(userId).arousal;
					}
					else{
						val = 0;
						aro = 0;
					}
					
					//write into file
					writer.append(words[i]); //userId
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(num)); //communityId
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(val)); //valence value
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(aro)); //arousal value
					writer.append('\n');
					
				}
				
				num++;
				writer.flush();
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			reader.close();
			writer.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//write community and sentiment level mapping into a file: cliques
		private void writeCliqueSentimentLevel(HashMap<Integer, Result> userSenti){
			String inputFile = "D:\\Social_Media_Analytics\\dataset\\cliques.txt";
			String storeFile = "D:\\Social_Media_Analytics\\dataset\\clique_senti_level.csv";
			BufferedReader reader = null;
			BufferedWriter writer = null;
			String line = "";
			int num = 0;
			int userId = 0;
			String val = "";
			String aro = "";
			
			try {
				writer = new BufferedWriter(new FileWriter(storeFile));
				reader = new BufferedReader(new FileReader(inputFile));
				
				reader.readLine(); //skip first line
				while ((line = reader.readLine()) != null){
					String words[] = line.split(Constant.SEPARATOR_TAB);
					
					//process one community
					for(int i = 0; i < words.length; i++){
						userId = Integer.parseInt(words[i]);
						if(userSenti.containsKey(userId)){
							val = sentiLevel(userSenti.get(userId).valence);
							aro = sentiLevel(userSenti.get(userId).arousal);
						}
						else{
							val = "Z";
							aro = "Z";
						}
						
						//write into file
						writer.append(words[i]); //userId
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(String.valueOf(num)); //communityId
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(val); //valence value
						writer.append(Constant.SEPARATOR_COMMA);
						writer.append(aro); //arousal value
						writer.append('\n');
						
					}
					
					num++;
					writer.flush();
					
					if(num % 10000 == 0)
						System.out.println(num + " records have been processed!");
				}	
				reader.close();
				writer.close();
			}catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	//write community and sentiment mapping into a file: Clauset-Newman-Moore
	private void writeCommunitySentiment(HashMap<Integer, Result> userSenti){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\communities.txt";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\community_senti.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int userId = 0;
		double val = 0;
		double aro = 0;
		
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			reader = new BufferedReader(new FileReader(inputFile));
			
			reader.readLine(); //skip first line
			while ((line = reader.readLine()) != null){
				String words[] = line.split(Constant.SEPARATOR_TAB);
				if(words.length != 2){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				userId = Integer.parseInt(words[0]);
				if(userSenti.containsKey(userId)){
					val = userSenti.get(userId).valence;
					aro = userSenti.get(userId).arousal;
				}
				else{
					val = 0;
					aro = 0;
				}
				
				//write into file
				writer.append(words[0]); //userId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[1]); //communityId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(val)); //valence value
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(aro)); //arousal value
				writer.append('\n');
				writer.flush();
				
				
				num++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			reader.close();
			writer.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String sentiLevel(double s){
		String level = "";
		if(s==0)
			level = "Z";//without sentiment score
		else if(s>0 && s<=3)
			level = "N"; //negative
		else if(s>3 && s<=6)
			level = "M"; //neutral
		else if(s>6)
			level = "P"; //positive
		return level;
	}
	
	//write community and sentiment level mapping into a file: Clauset-Newman-Moore
	private void writeCommunitySentimentLevel(HashMap<Integer, Result> userSenti){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\communities.txt";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\community_senti_level.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int userId = 0;
		String val = "";
		String aro = "";
		
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			reader = new BufferedReader(new FileReader(inputFile));
			
			reader.readLine(); //skip first line
			while ((line = reader.readLine()) != null){
				String words[] = line.split(Constant.SEPARATOR_TAB);
				if(words.length != 2){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				userId = Integer.parseInt(words[0]);
				if(userSenti.containsKey(userId)){
					val = sentiLevel(userSenti.get(userId).valence);
					aro = sentiLevel(userSenti.get(userId).arousal);
				}
				else{
					val = "Z";
					aro = "Z";
				}
				
				//write into file
				writer.append(words[0]); //userId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(words[1]); //communityId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(val); //valence value
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(aro); //arousal value
				writer.append('\n');
				writer.flush();
				
				
				num++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			reader.close();
			writer.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//compute user-level sentiment: one user may have several tweets
	public void userLevelSentiment(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_obama_sentiment_mapping.csv";
		HashMap<Integer, Result> userSenti = new HashMap<Integer, Result>();//store sentiment of each user
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int userId = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while ((line = reader.readLine()) != null){
				String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				userId = Integer.parseInt(words[0]);
				Result senti = new Result();
				senti.valence = Double.parseDouble(words[1]);
				senti.arousal = Double.parseDouble(words[2]);
				
				//put user_id and senti mapping into hashmap
				if(!userSenti.containsKey(userId))
					userSenti.put(userId, senti);
				else{ // if there is an existing mapping in the hashmap, get their mean values
					senti.valence = (senti.valence + userSenti.get(userId).valence)/2;
					senti.arousal = (senti.arousal + userSenti.get(userId).arousal)/2;
					userSenti.put(userId, senti);
				}
				num++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			reader.close();
			writeUserLevelSentimentIntoFile(userSenti);
			
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	//write the user-level sentiment into a file
	private void writeUserLevelSentimentIntoFile(HashMap<Integer, Result> userS){
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\tweets_two_days_obama_userLevelSentiment.csv";
		BufferedWriter writer  = null;
		int userId = 0;
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator<Integer> it = userS.keySet().iterator();
			while(it.hasNext()){
				userId = it.next();
				Result entry = userS.get(userId);
				writer.append(String.valueOf(userId));
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(entry.valence));
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(entry.arousal));
				writer.append('\n');
				writer.flush();
				num++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}	
			writer.close();
			System.out.println(num + " records in the user_level sentiment file!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
		/*SentimentAnalyzer sa = SentimentAnalyzer.getInstance();

		Result res = sa.sentimentANEW("'You told me that before and I asked her about it and she said she had no idea who you were'");
		if(res != null) {
			System.out.println("Arousal: " + res.arousal);
			System.out.println("Valence: " + res.valence);
		}*/
		SentimentAnalyzer sa = SentimentAnalyzer.getInstance();
		//sa.computeSentiment();
		//sa.userLevelSentiment();
		//sa.mapCommunitySentiment();
		sa.sentimentFlow();
	}
	

}