package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.xerox.socialmedia.communityrecommendation.sentiment.UserHashtag.hashtagSet;
import com.xerox.socialmedia.communityrecommendation.sentiment.UserHashtag.hashtagUserSize;
import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class FollowGraph {
	private HashMap<Integer, String> uniqueIdUserIdMap = new HashMap<Integer, String>();//a map from a user's unique_id to his userId
	private HashMap<String, Double> userSVOMap = new HashMap<String, Double>();
	HashMap<String, HashSet<String>> userIdHashtagMap = new HashMap<String, HashSet<String>>();//a map from user's userId to his hashtagSet
	HashSet<String> allNodes = new HashSet<String>();
	HashSet<String> trainNodes = new HashSet<String>();
	HashMap<String, pairStructure> nodeInstanceMap = new HashMap<String, pairStructure>();
	
	class pairStructure {
		int pos = 0;
		int neg = 0;
		
		public pairStructure(){
			this.pos=0;
			this.neg=0;
		}
		
		public void addPos(){
			this.pos++;
		}
		
		public void addNeg(){
			this.neg++;
		}
		
	}
	
	//read the mapping between userId and unique_id
	private void readUserIdUniqueIdMapping(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\userIdMapping.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int uniqueId = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 2){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				uniqueId = Integer.parseInt(words[1]);
				if(!uniqueIdUserIdMap.containsKey(uniqueId))
					uniqueIdUserIdMap.put(uniqueId, words[0]);
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			
			System.out.println(num + " records in the userIdMapping file in total!");
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//for mutual-follow graph
	public void readFollowMapping(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\hashtag_table.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int userId = 0;
	
		String tag = "";
		String originalId = "";
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 6){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				userId = Integer.parseInt(words[0]);
				tag = words[1];
				
				if(uniqueIdUserIdMap.containsKey(userId))
					originalId = uniqueIdUserIdMap.get(userId);
				else
					continue;
				
				//process userIDHashtagMap
				HashSet<String> uHash = new HashSet<String>();
				if(userIdHashtagMap.containsKey(originalId))
					uHash = userIdHashtagMap.get(originalId);
				
				uHash.add(tag);
				
				userIdHashtagMap.put(originalId, uHash);
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been read!");
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	//for mutual-follow graph
	private void searchFollowMapping(String hashtag){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\hashtag_table.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int numPair = 0;
		int userId = 0;
		String originalUserId = "";
		double pos = 0;
		double neg = 0;
		double sentiLabel = 0;
		String tag = "";
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 6){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				userId = Integer.parseInt(words[0]);
				tag = words[1];
				
				if(!tag.equals(hashtag))
					continue;
				
				if(uniqueIdUserIdMap.containsKey(userId)){
					originalUserId = uniqueIdUserIdMap.get(userId);
				}
				else 
					continue;
				
				pos = Double.parseDouble(words[3]);
				neg = Double.parseDouble(words[4]);

				if(pos>neg)
					sentiLabel = 1;
				else if (pos<neg)
					sentiLabel = -1;
				else 
					sentiLabel = 0;
				
				userSVOMap.put(originalUserId, sentiLabel);
				numPair++;
				if(num % 10000 == 0)
					System.out.println(num + " records have been read!");
			}
			
			System.out.println(numPair + " records in the userSVOMap");
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	//observation about sentiment homophily in mutual-follow graph
	public void sentimentObservation(String hashtag){
		this.readUserIdUniqueIdMapping();
		this.searchFollowMapping(hashtag);
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\candidates_175_copy.csv";
		BufferedReader reader = null;
		int num = 0;
		int numConnected = 0;
		int numNonConnected = 0;
		String line = "";
		String source = "";
		String target = "";
		int label = 0;
		double sourceSVO = 0;
		double targetSVO = 0;
		int numSimilar = 0;
		int numDifferent = 0;
		boolean nonConnected = false;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 7){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}	
				
				source = words[0];
				target = words[2];
				
				label = Integer.parseInt(words[4]);
				
				if(label==0)
					nonConnected = true;
				
				//statistics for connected pairs
				if(!nonConnected){
					if(userSVOMap.containsKey(source)&& userSVOMap.containsKey(target)){
						numConnected++;
						sourceSVO = userSVOMap.get(source);
						targetSVO = userSVOMap.get(target);
						
						//check whether the pair has the same sentiment
						if(sourceSVO==targetSVO)
							numSimilar++;
					}
				}
				else{//statistics for non_connected pairs
					if(userSVOMap.containsKey(source)&& userSVOMap.containsKey(target)){
						numNonConnected++;
						sourceSVO = userSVOMap.get(source);
						targetSVO = userSVOMap.get(target);
						
						//check whether the pair has the same sentiment
						if(sourceSVO==targetSVO)
							numDifferent++;
					}
				}
				
				if(num%1000==0)
					System.out.println(num + " records have been processed");
				//stop reading the input file when the numDiffernt is equal to numSimilar
				if(numDifferent>numSimilar)
					break;
			}
			
			reader.close();
			
			double probSimilar = (double) numSimilar/numConnected;
			double probDifferent = (double) numDifferent/numNonConnected;
			System.out.println("NumConnected: " + numConnected + " numSimilar: " + numSimilar );
			System.out.println("The probability of connected users having the same sentiment is: " + probSimilar);
			System.out.println("NumNonConnected: " + numNonConnected + " numDifferent: " + numDifferent );
			System.out.println("The probability of non_connected users having the same sentiment is: " + probDifferent);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	//get the size of the intersection
	private static HashSet<String> getIntersection(Set<String> s1, Set<String> s2){
		HashSet<String> clone = new HashSet<String>();
		if(s1.size()>0 && s2.size()>0){
			if(s1.size()>=s2.size()){
				clone = new HashSet<String>(s1);
				clone.retainAll(s2);
			}
			else{
				clone = new HashSet<String>(s2);
				clone.retainAll(s1);
			}
		}
		return clone;
	}
		
	public void hashtagStatistics(){
		this.readUserIdUniqueIdMapping();
		this.readFollowMapping();
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\candidates_175_copy.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\hashtagStatistics.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		String source = "";
		String target = "";
		int label = 0;
		HashSet<String> sSet = new HashSet<String>();
		HashSet<String> tSet = new HashSet<String>();
		HashSet<String> commonSet = new HashSet<String>();
		HashMap<String, Integer> tagMap = new HashMap<String, Integer>();
		try {
			
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 7){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}	
				
				source = words[0];
				target = words[2];
				
				label = Integer.parseInt(words[4]);
				if(label==0)
					break;
				
				if(userIdHashtagMap.containsKey(source)&& userIdHashtagMap.containsKey(target)){
					sSet = userIdHashtagMap.get(source);
					tSet = userIdHashtagMap.get(target);
					commonSet = this.getIntersection(sSet, tSet); //compute their common hashtags
					Iterator<String> it = commonSet.iterator();
					
					while(it.hasNext()){ //update the hastag statistics
						String topic = it.next();
						int count = 0;
						if(tagMap.containsKey(topic))
							count = tagMap.get(topic);
						count++;
						tagMap.put(topic, count);
					}
				}
			}
			reader.close();
			this.printMap(tagMap, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	//print the hashmap to a file
	private void printMap(HashMap<String, Integer> map, String storeFile){
		BufferedWriter writer  = null;
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
		
				writer.append(key);//userId
				writer.append(Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(value));
				
				writer.append("\n");
				writer.flush();
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been written!");
			}
			writer.close();
			
			System.out.println("There are " + num + " user records in total");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//filter instances based on a particular threshold
	public void filterInstance(int threshold){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\follower_feature_175.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\sample_follower_feature_175.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int label = 0;
		int CN = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				
				//skip the first line
				if(num==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
					continue;
				}
				
				label = Integer.valueOf(words[2]);
				//directly copy the positive instance
				if(label==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
				}
				else{
					CN = Integer.valueOf(words[47]);
					if(CN>=threshold){ //using the common neighbors as a filter
						writer.append(line);
						writer.append("\n");
						writer.flush();
					}
				}
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
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
	
	//undersampling instance according to a threshold (negative/positive)
	public void sampleInstnace(int threshold){
		HashMap<String, Integer> negativeMap = new HashMap<String, Integer>();
		HashMap<String, Integer> positiveMap = new HashMap<String, Integer>();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\random_all_follower_feature_175.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\sample6_random_follower_feature_175.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int label = 0;
		String source = "";
		int count = 0;
		int negCount = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				//skip the first line
				if(num==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
					continue;
				}
				
				label = Integer.valueOf(words[2]);
				source = words[0];
				//directly copy the positive instance
				if(label==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
					
					if(positiveMap.containsKey(source))
						count = positiveMap.get(source);
					else
						count = 0;
					count++;
					positiveMap.put(source, count);
				}
				else{
					if(positiveMap.containsKey(source))
						count = positiveMap.get(source);
					else
						count = 0;
					
					if(count==0)
					{
						System.out.println("There is an error for negative instances of node: " + source);
						continue;
					}
					
					if(negativeMap.containsKey(source))
						negCount = negativeMap.get(source);
					else
						negCount = 0;
					negCount++;
					//sample instance: for each source node, making the number of negative samples doesn't exceed the threshold
					if(negCount<=threshold*count)
					{
						writer.append(line);
						writer.append("\n");
						writer.flush();
						negativeMap.put(source, negCount);
					}
				}
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
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
	
	//eliminate some instances according to some criteria
	public void reduceSpace(int threshold){
		HashMap<String, Integer> negativeMap = new HashMap<String, Integer>();
		HashMap<String, Integer> positiveMap = new HashMap<String, Integer>();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\random_all_follower_feature_175.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\sample7_random_follower_feature_175.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int label = 0;
		double soAdar = 0; 
		String source = "";
		int count = 0;
		int negCount = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				//skip the first line
				if(num==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
					continue;
				}
				
				source = words[0];
				label = Integer.valueOf(words[2]);
				soAdar = Double.valueOf(words[20]);
				//directly copy the positive instance
				if(label==1){
					writer.append(line);
					writer.append("\n");
					writer.flush();
					
					/*if(positiveMap.containsKey(source))
						count = positiveMap.get(source);
					else
						count = 0;
					count++;
					positiveMap.put(source, count);*/
				}
				else{
					//remove the instances whose SOAdar value is less than 1
					if(soAdar<1)
						continue;
					
					/*if(positiveMap.containsKey(source))
						count = positiveMap.get(source);
					else
						count = 0;
					
					if(count==0)
					{
						System.out.println("There is an error for negative instances of node: " + source);
						continue;
					}
					
					
					
					if(negativeMap.containsKey(source))
						negCount = negativeMap.get(source);
					else
						negCount = 0;
					negCount++;*/
					
					//sample instance: for each source node, making the number of negative samples doesn't exceed the threshold
					//if(negCount<=threshold*count)
					//{
						writer.append(line);
						writer.append("\n");
						writer.flush();
						//negativeMap.put(source, negCount);
					//}
				}
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
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
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample7_follower_feature_175.csv";
		String trainFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample7_follower_feature_175_train.csv";
		String testFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample7_follower_feature_175_test.csv";
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
	
	//split nodes into a set of train nodes and a set of test nodes
	private void splitInstances(String inputFile){
		BufferedReader reader = null;
		String line = "";
		String source = "";
		int label = 0;
		int num = 0;
		
		pairStructure ps = new pairStructure();
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
				label = Integer.parseInt(words[2]);	
				
				if(nodeInstanceMap.containsKey(source))
					ps = nodeInstanceMap.get(source);
				else
					ps = new pairStructure();
				
				if(label==1)
					ps.addPos();
				else
					ps.addNeg();
				nodeInstanceMap.put(source, ps);
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			reader.close();
				
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//First way to split file: For each user, split his instances into 7:3 train:test
	public void splitFileWayTwo(double ratio){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample4_follower_feature_175.csv";
		String trainFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\wayTwo_sample4_follower_feature_175_train.csv";
		String testFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\wayTwo_sample4_follower_feature_175_test.csv";
		this.splitInstances(inputFile);
		
		BufferedReader reader = null;
		BufferedWriter writer1 = null;
		BufferedWriter writer2 = null;
		
		String line = "";
		int num = 0;
		String source = "";
		String sourceOriginal = "";
		pairStructure ps = new pairStructure();
		int pos = 0;
		int neg = 0;
		int label = 0;
		int numPos = 0;
		int numNeg = 0;
		
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
				label = Integer.parseInt(words[2]);
				
				if(!source.equals(sourceOriginal)){
					if(nodeInstanceMap.containsKey(source))
						ps = nodeInstanceMap.get(source);
					else
						System.out.println("source: " + source + " num: " + num);
					pos = (int) (ps.pos * ratio);
					neg = (int) (ps.neg * ratio);
					sourceOriginal = source;
					numPos = 0;
					numNeg = 0;
				}
				
				//decide whether the instance should be written into train file or test file
				if(label==1){
					if(numPos<pos){
						train = 1;
						numPos++;
					}
					else
						train = 0;
				}
				else if(label==0){
					if(numNeg<neg){
						train = 1;
						numNeg++;
					}
					else
						train = 0;
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
		FollowGraph fg = new FollowGraph();
		//fg.sentimentObservation("#santorum");
		//fg.hashtagStatistics();
		//fg.filterInstance(11);
		//fg.sampleInstnace(1);
		//fg.reduceSpace(1);
		fg.splitFileWayOne(0.7);
		//fg.splitFileWayTwo(0.7);
	}

}
