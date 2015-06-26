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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.xerox.socialmedia.communityrecommendation.data.dbwriter.TweetRunnable;
import com.xerox.socialmedia.communityrecommendation.dbmanager.SqlControler;
import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class Feature {
	private SqlControler sqlControler;
	
	private HashMap<Integer, HashMap<String, sentimentSet>> hashtagCounter = new HashMap<Integer,HashMap<String, sentimentSet>>();
	private HashMap<Integer, HashMap<String, sentimentScore>> hashtagMap = new HashMap<Integer,HashMap<String, sentimentScore>>();
	
	public Feature(){
		sqlControler = new SqlControler();
	}
	
	
	class sentimentSet {
		int pos = 0;
		int neg = 0;
		int obj = 0;
		double svo = 0; //SVO score
		
		public sentimentSet(){
		}
		
		public sentimentSet(int label){
			if(label==1)
				pos = 1;
			else if(label == -1)
				neg = 1;
			else
				obj = 1;
		}
		
		public void increaseSentiment( int label){
			if(label==1)
				pos = pos + 1;
			else if(label==-1)
				neg = neg +1;
			else
				obj = obj + 1;
		}
		
	}
	
	class sentimentScore {
		ArrayList<Double> pos = new ArrayList<Double>();
		ArrayList<Double> neg = new ArrayList<Double>();
		ArrayList<Double> obj = new ArrayList<Double>();
		double aver_pos = 0;
		double aver_neg = 0;
		double aver_obj = 0;
		
		public sentimentScore(){
		}
		
		public sentimentScore(double p, double n, double o){
			pos.add(p);
			neg.add(n);
			obj.add(o);
		}
		
		public void addSentiment(double p, double n, double o){
			pos.add(p);
			neg.add(n);
			obj.add(o);
		}
		
		public void averageSentiment(){
			double sum_pos = 0;
			double sum_neg = 0;
			double sum_obj = 0;
			for(int i = 0; i < pos.size(); i++){
				sum_pos += pos.get(i);
			}
			aver_pos = sum_pos/pos.size();
			
			for(int i = 0; i < neg.size(); i++){
				sum_neg += neg.get(i);
			}
			aver_neg = sum_neg/neg.size();
			
			for(int i = 0; i < obj.size(); i++){
				sum_obj += obj.get(i);
			}
			aver_obj = sum_obj/obj.size();
		}
		
	}
	
	
	//update the average pos/neg/obj features from json files
	public void updateAverageFeature(){
		sqlControler.createConnection();
		PreparedStatement ps = null;
		int fetchSize = 1000;
		String selectQuery = "select unique_id, source, positivity, negativity " +
				"from tweets_sentiment_one_month limit ?,"+fetchSize;
		ResultSet rs = null;
		ps = sqlControler.GetPreparedStatement(selectQuery);
		int num = 0;
		int numHash = 0;
		int uniqueId = 0;
		String tweet = "";
		double pos = 0;
		double neg = 0;
		double obj = 0;
		
		ArrayList<String> tags = new ArrayList<String>();
		System.out.println("Start to retrieve information from database");
		
		while(num % fetchSize == 0 && num <= 3970000){
			try {
				ps.setInt(1, num);
				rs = ps.executeQuery();
				while(rs.next()){
					num++;
					//user_id = rs.getLong(1);
					uniqueId = rs.getInt(1);
					tweet = rs.getString(2);
					pos = rs.getDouble(3);
					neg = rs.getDouble(4);
					obj = 1 - pos - neg;
					
					//extract hashtags from tweets
					tags = extractHashTags(tweet);
					//skip the record which doesn't contain any hashtag
					
					if(tags.size()==0)
						continue;
					
					
					this.updateUserSentimentScoreProfile(uniqueId, pos, neg, obj, tags);
					numHash++;
					
					if(num % 10000 == 0)
						System.out.println(num + " records have been processed from DB!");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(numHash + " records that have hashtags");
		sqlControler.close();
		
		//calculate the average scores and write the result into DB
		this.updateAveargeFeatureIntoDatabase();
	}
	
	
	public void updateFeature(){
		sqlControler.createConnection();
		PreparedStatement ps = null;
		int fetchSize = 1000;
		String selectQuery = "select unique_id, source, positivity, negativity " +
				"from tweets_sentiment_one_month limit ?,"+fetchSize;
		ResultSet rs = null;
		ps = sqlControler.GetPreparedStatement(selectQuery);
		int num = 0;
		int numHash = 0;
		int uniqueId = 0;
		String tweet = "";
		double pos = 0;
		double neg = 0;
		double obj = 0;
		
		int sentimentLabel = 0; // -1 means negative; 0 measn neutral; 1 means positive
		
		ArrayList<String> tags = new ArrayList<String>();
		System.out.println("Start to retrieve information from database");
		
		while(num % fetchSize == 0 && num <= 3970000){
			try {
				ps.setInt(1, num);
				rs = ps.executeQuery();
				while(rs.next()){
					num++;
					//user_id = rs.getLong(1);
					uniqueId = rs.getInt(1);
					tweet = rs.getString(2);
					pos = rs.getDouble(3);
					neg = rs.getDouble(4);
					obj = 1 - pos - neg;
					
					//extract hashtags from tweets
					tags = extractHashTags(tweet);
					//skip the record which doesn't contain any hashtag
					
					if(tags.size()==0)
						continue;
					
					//decide the sentiment of this tweet and its associated hashtags
					if(pos > neg)
						sentimentLabel = 1;
					else if(pos < neg)
						sentimentLabel = -1;
					else
						sentimentLabel = 0;
					
					updateUserSentimentProfile(uniqueId, sentimentLabel, tags);
					numHash++;
					
					if(num % 10000 == 0)
						System.out.println(num + " records have been processed from DB!");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(numHash + " records that have hashtags");
		sqlControler.close();
		
		//compute SVO
		computeSVO();
		
		//write the result into DB
		updateDatabase();
	}
	
	private void updateDatabase(){
		System.out.println(" Start to write results into database");
		sqlControler.createConnection();
		//write information into database
		String insertQuery = "insert into tweets_hashtag_one_month " +
				"(unique_id,hashtag,num_pos,num_neg,num_obj,SVO) " +
				"values (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE unique_id = unique_id, hashtag = hashtag" ;
		PreparedStatement prepStmt = sqlControler.GetPreparedStatement(insertQuery);
		
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_hashtag_one_month.csv";
		BufferedWriter writer  = null;
		int num = 0;
		int userId = 0;
		String hashTag = "";
		Iterator it = hashtagCounter.entrySet().iterator();
		HashMap<String, sentimentSet> hashtagSentiment = new HashMap<String, sentimentSet>();
		sentimentSet sSet = new sentimentSet();
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			while(it.hasNext()){
				Map.Entry pairs = (Entry) it.next();
				userId = (Integer) pairs.getKey();
				hashtagSentiment = (HashMap<String, sentimentSet>) pairs.getValue();
				Iterator inner = hashtagSentiment.entrySet().iterator();
				
				while(inner.hasNext()){ // compute the times that a user talks about all the hashtags
					Map.Entry innerPair = (Entry) inner.next();
					hashTag = (String) innerPair.getKey();
					sSet = (sentimentSet) innerPair.getValue();
					
					prepStmt.setInt(1, userId);
					prepStmt.setString(2, hashTag);
					prepStmt.setInt(3, sSet.pos);
					prepStmt.setInt(4, sSet.neg);
					prepStmt.setInt(5, sSet.obj);
					prepStmt.setDouble(6, sSet.svo);
					sqlControler.addBatch(prepStmt); // batch insert
					
					//write into file
					writer.append(String.valueOf(userId));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(hashTag);
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.pos));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.neg));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.obj));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.svo));
					writer.append('\n');
					writer.flush();
					//System.out.println("userId: " + userId + " hashtag: " + hashTag);
					
					
					num++;
					
					if(num%10000 ==0)
						System.out.println(num+ " records have been written");
				}
			}
			sqlControler.executeBatch(prepStmt);
			writer.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sqlControler.close();
		
	}
	
	private void updateAveargeFeatureIntoDatabase(){
		System.out.println(" Start to write results into database");
		sqlControler.createConnection();
		//write information into database
		String insertQuery = "update tweets_hashtag_one_month " +
				"set aver_pos = ?, aver_neg = ?, aver_obj = ? " +
				"where unique_id = ? and hashtag = ?" ;
		PreparedStatement prepStmt = sqlControler.GetPreparedStatement(insertQuery);
		
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_hashtag_average_one_month.csv";
		BufferedWriter writer  = null;
		int num = 0;
		int userId = 0;
		String hashTag = "";
		Iterator it = hashtagMap.entrySet().iterator();
		HashMap<String, sentimentScore> hashtagSentiment = new HashMap<String, sentimentScore>();
		sentimentScore sSet = new sentimentScore();
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			while(it.hasNext()){
				Map.Entry pairs = (Entry) it.next();
				userId = (Integer) pairs.getKey();
				hashtagSentiment = (HashMap<String, sentimentScore>) pairs.getValue();
				Iterator inner = hashtagSentiment.entrySet().iterator();
				
				while(inner.hasNext()){ // compute the times that a user talks about all the hashtags
					Map.Entry innerPair = (Entry) inner.next();
					hashTag = (String) innerPair.getKey();
					sSet = (sentimentScore) innerPair.getValue();
					
					//compute the average of the three sentiment scores
					sSet.averageSentiment();
					
					prepStmt.setDouble(1, sSet.aver_pos);
					prepStmt.setDouble(2, sSet.aver_neg);
					prepStmt.setDouble(3, sSet.aver_obj);
					prepStmt.setInt(4, userId);
					prepStmt.setString(5, hashTag);
					sqlControler.addBatch(prepStmt); // batch insert
					
					//write into file
					writer.append(String.valueOf(userId));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(hashTag);
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.aver_pos));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.aver_neg));
					writer.append(Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(sSet.aver_obj));
					writer.append('\n');
					writer.flush();
					//System.out.println("userId: " + userId + " hashtag: " + hashTag);
					
					
					num++;
					
					if(num%10000 ==0)
						System.out.println(num+ " records have been written");
				}
			}
			sqlControler.executeBatch(prepStmt);
			writer.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sqlControler.close();
		
	}

	//for a given user, update the sentiment for each hashtag in a tweet
	private void updateUserSentimentProfile(int uniqueId, int sentimentLabel,
			ArrayList<String> tags) {
		sentimentSet senSet = new sentimentSet();
		HashMap<String, sentimentSet> hashtagSen = new HashMap<String, sentimentSet>();
		if(hashtagCounter.containsKey(uniqueId)){
			hashtagSen = hashtagCounter.get(uniqueId);
			for(int i = 0; i<tags.size(); i++){
				String topic = tags.get(i);
				if(hashtagSen.containsKey(topic)){
					senSet = hashtagSen.get(topic);
					senSet.increaseSentiment(sentimentLabel);
					hashtagSen.put(topic, senSet);
				}
				else{
					sentimentSet sSet = new sentimentSet(sentimentLabel);
					hashtagSen.put(topic, sSet);
				}
			}
			hashtagCounter.put(uniqueId, hashtagSen);
		}
		else{
			sentimentSet sSet = new sentimentSet(sentimentLabel);
			HashMap<String, sentimentSet> hashtagSentiment = new HashMap<String, sentimentSet>();
			for(int i = 0; i<tags.size(); i++){
				hashtagSentiment.put(tags.get(i),sSet);
			}
			hashtagCounter.put(uniqueId, hashtagSentiment);
		}
	}
	
	//for a given user, update the sentiment for each hashtag in a tweet
	private void updateUserSentimentScoreProfile(int uniqueId, double pos, double neg, double obj,
			ArrayList<String> tags) {
		sentimentScore senSet = new sentimentScore();
		HashMap<String, sentimentScore> hashtagSen = new HashMap<String, sentimentScore>();
		if(hashtagMap.containsKey(uniqueId)){
			hashtagSen = hashtagMap.get(uniqueId);
			for(int i = 0; i<tags.size(); i++){
				String topic = tags.get(i);
				if(hashtagSen.containsKey(topic)){
					senSet = hashtagSen.get(topic);
					senSet.addSentiment(pos, neg, obj);
					hashtagSen.put(topic, senSet);
				}
				else{
					sentimentScore sSet = new sentimentScore(pos, neg, obj);
					hashtagSen.put(topic, sSet);
				}
			}
			hashtagMap.put(uniqueId, hashtagSen);
		}
		else{
			sentimentScore sSet = new sentimentScore(pos, neg, obj);
			HashMap<String, sentimentScore> hashtagSentiment = new HashMap<String, sentimentScore>();
			for(int i = 0; i<tags.size(); i++){
				hashtagSentiment.put(tags.get(i),sSet);
			}
			hashtagMap.put(uniqueId, hashtagSentiment);
		}
	}
	
	private void computeSVO(){
		System.out.println(" Start to compute SVO score");
		Iterator it = hashtagCounter.entrySet().iterator();
		HashMap<String, sentimentSet> hashtagSentiment = new HashMap<String, sentimentSet>();
		sentimentSet sSet = new sentimentSet();
		int num = 0;
		
		while(it.hasNext()){
			Map.Entry pairs = (Entry) it.next();
			hashtagSentiment = (HashMap<String, sentimentSet>) pairs.getValue();
			Iterator inner = hashtagSentiment.entrySet().iterator();
			int count = 0;//number of times a user talking about all the hashtags
			while(inner.hasNext()){ // compute the times that a user talks about all the hashtags
				Map.Entry innerPair = (Entry) inner.next();
				sSet = (sentimentSet) innerPair.getValue();
				count += sSet.pos;
				count += sSet.neg;
				count += sSet.obj;
			}
			
			//compute the SVO score for each hashtag
			inner = hashtagSentiment.entrySet().iterator();
			while(inner.hasNext()){
				Map.Entry innerPair = (Entry) inner.next();
				sSet = (sentimentSet) innerPair.getValue();
				sSet.svo = SVO(sSet.pos,sSet.neg,sSet.obj,count);
			}
			num++;
			if(num%1000==0)
				System.out.println(num+ " users have been computed for their SVO score");
		}
	}
	
	private double SVO(int pos, int neg, int obj, int count){
		double result = 0;
		double alpha = 0.3;
		double beta = 0.6;
		double gama = 0.1;
		
		double s =0;
		if(pos>0 || neg>0){
			s= (double) (pos -neg)/(pos+neg);
		}
		s = Math.pow(10, -s);
		s = 1/(1+s);
		
		double v =(double) (pos+neg+obj)/count;
		
		double o = (double) obj/(pos+neg+obj);
		
		result = alpha*s + beta*v + gama*o;
		
		return result;
	}
	
	//extract hashtag list from tweet
	public ArrayList<String> extractHashTags(String s) {
		//String[] words = s.split(Constant.SEPARATOR_SPACE);
		s = s.toLowerCase();
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
	
	public static void main(String[] args) {
		Feature sa = new Feature();
		sa.updateFeature();
		//sa.updateAverageFeature();
	}

}
