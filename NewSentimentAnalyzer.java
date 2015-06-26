package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.xerox.socialmedia.communityrecommendation.data.dbwriter.WorkQueue;
import com.xerox.socialmedia.communityrecommendation.dbmanager.SqlControler;
import com.xerox.socialmedia.communityrecommendation.sentiment.CMUTagger.TaggedToken;
import com.xerox.socialmedia.communityrecommendation.sentiment.SentiWordNet.Score;

public class NewSentimentAnalyzer {
	
	static class tweetScore {
		double pos;
		double neg;
		double obj;
		
		public tweetScore(){
			pos = 0;
			neg = 0;
			obj = 0;
		}
		
		//average the score of each word from the tweet
		public void averageScore(ArrayList<Score> list){
			
			for(int i = 0; i<list.size(); i++){
				//System.out.println(list.get(i).pos + " " + list.get(i).neg + " " + list.get(i).obj);
				pos += list.get(i).pos;
				neg += list.get(i).neg;
				obj += list.get(i).obj;
			}
			
			pos /= list.size();
			neg /= list.size();
			obj /= list.size();
			
			obj = 1 - pos - neg;
			if(obj<0){
				obj = 0;
				System.out.println("error in averageScore: obj is negative"); 
			}
			
			
		}
	}
	
	
	public void sentimentAnalyze(){
		String inputFile = "tweets_file_5.csv";
		String storeFile = "tweets_file_5_sentiment.csv";
		
		BufferedReader reader = null;
		BufferedWriter writer  = null;
		String line = "";
		int num = 0;
		String tweet = "";
		WorkQueue queue = new WorkQueue(6);
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			
			//build sentiwordnet dictionary
			SentiWordNet swn = new SentiWordNet();
			swn.BuildWordNet();
			
			//build emoticon dictionary
			Emoticon en = new Emoticon();
			en.buildEmoticonDict();
			
			//Load the POS tagger
			CMUTagger tagger = new CMUTagger();
            tagger.loadModel();
            
			//load the stanford parser (for negation)
			Negation ng = new Negation();
			
			//read the file line by line
			while ((line = reader.readLine()) != null){
				num++;
				queue.execute(new SentimentRunnable(num, line, swn, en, tagger, ng, writer));
				/*String words[] = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 8){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				tweet = words[3];
				//System.out.println("tweet: " + tweet);
				
				//POS tagging
	            List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(tweet);
	            
	            //identify negation set
	            HashSet<String> negSet = ng.identifyNegation(tweet);
	            
	            //a list containing the score of each effective word
	            ArrayList<Score> scoreList = new ArrayList<Score>();
	            
	            for (TaggedToken token : taggedTokens){
                    if(token.tag.equals("A")){ //adjectives
                    	//System.out.println("token: " + token.token);
                    	String stem = swn.stemmerWords(token.token);
                    	Score wScore = swn.new Score();
                    	wScore = swn.extract(stem);
                    	//if negation of word exist, its pos and neg need to be exchanged
                    	if(negSet.size() > 0 && negSet.contains(token.token)){
                    		double temp = wScore.pos;
                    		wScore.pos = wScore.neg;
                    		wScore.neg = temp;
                    	}
                    	
                    	scoreList.add(wScore);
                    }
                    else if(token.tag.equals("E")){ //emoticons 
                    	int label = en.extractLabel(token.token);
                    	//System.out.println("token: " + token.token);
                    	Score eScore = swn.new Score();
                    	eScore.pos = en.extractPos(label);
                    	eScore.neg = en.extractNeg(label);
                    	eScore.obj = en.extractObj(label);
                    	scoreList.add(eScore);
                    }
	            }
	            
	            //average the score of each effective word of the tweet
	            tweetScore tScore = new tweetScore();
	            
	         
	            if(scoreList.size()>0){
	            	tScore.averageScore(scoreList);	   
	            }
	            //write the result of each tweet into a file
	            writer.append(line);
	            writer.append(Constant.SEPARATOR_COMMA);
	            writer.append(String.valueOf(tScore.pos));
	            writer.append(Constant.SEPARATOR_COMMA);
	            writer.append(String.valueOf(tScore.neg));
	            writer.append(Constant.SEPARATOR_COMMA);
	            writer.append(String.valueOf(tScore.obj));
	            writer.append('\n');
				writer.flush();
				
				if(num % 1000 == 0)
					System.out.println(num + " records have been processed!");*/
	            
			}
			
			System.out.println("There are " + num + " records in total");
			
			Thread.sleep(Integer.MAX_VALUE);
			
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//sentiment analysis with DB
	public void sentimentAnalyzeDB(){
		SqlControler sqlControler = new SqlControler();
		SqlControler sqlControler2 = new SqlControler();
		sqlControler.createConnection();
		sqlControler2.createConnection();
		PreparedStatement ps = null;
		int fetchSize = 3451;
		
		String selectQuery = "select tweet_id, source from tweets_sentiment_one_month " +
				"where positivity = -1";
		ResultSet rs = null;
		ps = sqlControler.GetPreparedStatement(selectQuery);
		
		String line = "";
		int num = 0;
		long tweetID= 0L;
		String tweet = "";
		WorkQueue queue = new WorkQueue(2);
		
		try {
			//build sentiwordnet dictionary
			SentiWordNet swn = new SentiWordNet();
			swn.BuildWordNet();
			
			//build emoticon dictionary
			Emoticon en = new Emoticon();
			en.buildEmoticonDict();
			
			//Load the POS tagger
			CMUTagger tagger = new CMUTagger();
	        tagger.loadModel();
	        
			//load the stanford parser (for negation)
			Negation ng = new Negation();
			
			//while(num % fetchSize == 0 && num <= 13900){
				//ps.setInt(1, num);
				rs = ps.executeQuery();
				
				while(rs.next()){
					tweetID = rs.getLong(1);
					tweet = rs.getString(2);
					
					//queue.execute(new TweetRunnable(tweet, tweetID, sqlControler2, num));
					queue.execute(new SentimentDBRunnable(num, tweet, tweetID, swn, en, tagger, ng, sqlControler2));
					
					num++;
					//if(num % 100 == 0)
						//System.out.println(num + " records have been processed!");
				}
				
			//}
			
			System.out.println("There are " + num + " records in total");
			
			Thread.sleep(Integer.MAX_VALUE);
			
			sqlControler.close();
			sqlControler2.close();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		NewSentimentAnalyzer nsa = new NewSentimentAnalyzer();
		//nsa.sentimentAnalyze();
		nsa.sentimentAnalyzeDB();
	}

}
