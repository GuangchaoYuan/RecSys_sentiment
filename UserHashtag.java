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
import java.util.Map.Entry;
import java.util.Set;

import com.xerox.socialmedia.communityrecommendation.utils.Constant;

public class UserHashtag {

	HashMap<Integer, hashtagSet> userHashtagMap = new HashMap<Integer,hashtagSet>();
	HashMap<String, hashtagSet> userIdHashtagMap = new HashMap<String,hashtagSet>();//a map from user's userId to his hashtagSet
	
	HashMap<String, hashtagUserSize> userSizeMap = new HashMap<String,hashtagUserSize>();//for each hashtag, how many 
	                                                                                     //users have positive/negative/objective opinions
	HashMap<Integer, String> uniqueIdUserIdMap = new HashMap<Integer, String>();//a map from a user's unique_id to his userId
	HashMap<Integer, traidPair> socialTheoryMap = new HashMap<Integer, traidPair>();
	HashMap<Integer, ArrayList<traidPair>> socialTheoryHashtag = new HashMap<Integer, ArrayList<traidPair>>();
	HashMap<Integer, pairStructure> socialLabelHashtag = new HashMap<Integer, pairStructure>();
	HashMap<String, Integer> positiveMap = new HashMap<String, Integer>();
	HashMap<String, Integer> negativeMap = new HashMap<String, Integer>();
	HashSet<String> followMap = new HashSet<String>();
	HashMap<Integer, ArrayList<Double>> socialNeighbor = new HashMap<Integer, ArrayList<Double>>();
	HashMap<String, Integer> tieMap = new HashMap<String, Integer>();//define whether the tie is strong or not
	class hashtagSet {
		HashSet<String> pos = new HashSet(); // a set of positive hashtags
		HashSet<String> neg = new HashSet(); // a set of negative hashtags
		HashSet<String> obj = new HashSet(); // a set of objective hashtags
		//HashSet<String> all = new HashSet(); // a set of all sentiments
		HashMap<String, Double> svo = new HashMap<String, Double>(); // a hashmap of hashtags and their svo scores
		
		public void mergeSet(hashtagSet ht){
			Iterator<String> it = ht.pos.iterator();
			while(it.hasNext())
				this.pos.add(it.next());
			
			it = ht.neg.iterator();
			while(it.hasNext())
				this.neg.add(it.next());
			
			it = ht.obj.iterator();
			while(it.hasNext())
				this.obj.add(it.next());
		}
		
		//get the sentiment label for one hashtag
		public int getHashtagSentiment(String tag){
			int label = 0; // 1 means positive, 2 means negative, 3 means neutral
			if(pos.contains(tag))
				label = 1;
			else if(neg.contains(tag))
				label = 2;
			else if(obj.contains(tag))
				label = 3;
			return label;
		}
		
		//merge the hastags of all sentiments
		/*public HashSet<String> allHashtags(){
			all = new HashSet(this.pos);
			all.addAll(this.neg);
			all.addAll(this.obj);
			return all;
		}*/
	}
	
	class hashtagUserSize{
		int posSize = 0;
		int negSize = 0;
		int objSize = 0;
		
		public hashtagUserSize(){
			this.posSize = 0;
			this.negSize = 0;
			this.objSize = 0;
		}
		
	}
	
	class traidPair{
		int numTraid = 0; //number of sentiment traids
		int numPair = 0; //number of sentiment traids while the third pair is connected
		
		public traidPair(){
			this.numTraid = 0;
			this.numPair = 0;
		}
		public void addPair(boolean connected){
			this.numTraid++;
			if(connected)
				this.numPair++;
		}
	}
	
	class pairStructure{
		ArrayList<Double> friendValue = new ArrayList<Double>();
		ArrayList<Double> nonFriendValue = new ArrayList<Double>();
	}
	
	//for mutual-follow graph
	public void readFollowMapping(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\hashtag_table.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int userId = 0;
		double pos = 0;
		double neg = 0;
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
				hashtagSet uHash = new hashtagSet();
				if(userIdHashtagMap.containsKey(originalId))
					uHash = userIdHashtagMap.get(originalId);
				
				//process userSize map
				hashtagUserSize uSize = new hashtagUserSize();
				if(userSizeMap.containsKey(tag))
					uSize = userSizeMap.get(tag);
				
				pos = Double.parseDouble(words[3]);
				neg = Double.parseDouble(words[4]);
				
				if(pos>neg){
					uHash.pos.add(tag);
					uSize.posSize++;
				}
				else if(pos<neg){
					uHash.neg.add(tag);
					uSize.negSize++;
				}
				else{
					uHash.obj.add(tag);
					uSize.objSize++;
				}
				
				uHash.svo.put(tag, Double.parseDouble(words[2]));
				
				userIdHashtagMap.put(originalId, uHash);
				userSizeMap.put(tag, uSize);
				
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
	
	//parse tie strength based on a criterion (index) and a threshold
	public void tieStrength(int index, double threshold){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\tie_strength.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		String source = "";
		String target = "";
		String key = "";
		double adar = 0;
		int strength = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				//skip the headline
				if(num==1)
					continue;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 5){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				source = words[0];
				target = words[1];
				key = source + Constant.SEPARATOR_HYPHEN + target;
				adar = Double.parseDouble(words[index]);
				if(adar>=threshold)
					strength = 1;
				else strength = 0;
				
				if(!tieMap.containsKey(key))
					tieMap.put(key, strength);
				
				if(num % 10000 == 0)
					System.out.println(num + " tie strength records have been read!");
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//parse tie strength based on a criterion (index) and a threshold for mention graph
	public void tieStrengthMention(String tieFile, int index, double threshold){
		String inputFile = tieFile;
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		String source = "";
		String target = "";
		String key = "";
		double adar = 0;
		int strength = 0;
		int strongCount = 0;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				//skip the headline
				if(num==1)
					continue;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 5){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				source = words[0];
				target = words[1];
				key = source + Constant.SEPARATOR_HYPHEN + target;
				adar = Double.parseDouble(words[index]);
				if(adar>=threshold){
					strength = 1;
					strongCount++;
				}
				else strength = 0;
				
				if(!tieMap.containsKey(key))
					tieMap.put(key, strength);
				
				if(num % 10000 == 0)
					System.out.println(num + " tie strength records have been read!");
			}
			
			reader.close();
			System.out.println("There are overall " + strongCount + " strong ties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readMapping(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\hashtag_table.csv";
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int userId = 0;
		double pos = 0;
		double neg = 0;
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
				//process userHashtagMap
				hashtagSet uHash = new hashtagSet();
				if(userHashtagMap.containsKey(userId))
					uHash = userHashtagMap.get(userId);
				
				//process userSize map
				hashtagUserSize uSize = new hashtagUserSize();
				if(userSizeMap.containsKey(tag))
					uSize = userSizeMap.get(tag);
				
				pos = Double.parseDouble(words[3]);
				neg = Double.parseDouble(words[4]);
				
				if(pos>neg){
					uHash.pos.add(tag);
					uSize.posSize++;
				}
				else if(pos<neg){
					uHash.neg.add(tag);
					uSize.negSize++;
				}
				else{
					uHash.obj.add(tag);
					uSize.objSize++;
				}
				
				uHash.svo.put(tag, Double.parseDouble(words[2]));
				
				userHashtagMap.put(userId, uHash);
				userSizeMap.put(tag, uSize);
				
			
				
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
	
	//get the size of the intersection
	public static HashSet<String> getIntersection(Set<String> s1, Set<String> s2){
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
	
	//get the intersection of three hashsets
	public static HashSet<String> getTriadIntersection(Set<String> s1, Set<String> s2, Set<String> s3){
		HashSet<String> clone = new HashSet<String>();
		//get the intersection of s1 and s2 first
		clone = getIntersection(s1, s2);
		
		//get the intersection of clone and s3
		if(clone.size()>0){
			clone = getIntersection(clone, s3);
		}
		
		return clone;
	}
	
	//get the intersection neighbor set
	public HashSet<Integer> getIntersectionNeighbors(Set<Integer> s1, Set<Integer> s2){
		HashSet<Integer> clone = new HashSet<Integer>();
		if(s1.size()>0 && s2.size()>0){
			if(s1.size()>=s2.size()){
				clone = new HashSet<Integer>(s1);
				clone.retainAll(s2);
			}
			else{
				clone = new HashSet<Integer>(s2);
				clone.retainAll(s1);
			}
		}
		return clone;
	}
	
	//get the union neighbor set
	public HashSet<Integer> getUnionNeighbors(Set<Integer> s1, Set<Integer> s2){
		HashSet<Integer> clone;
		if(s1.size()==0)
			clone = new HashSet<Integer>(s2);
		else if(s2.size()==0)
			clone = new HashSet<Integer>(s1);
		else{
			clone = new HashSet<Integer>(s1);
			clone.addAll(s2);
		}
		return clone;
	}
	
	public static HashSet<String> getUnion(Set<String> s1, Set<String> s2){
		HashSet<String> clone;
		if(s1.size()==0)
			clone = new HashSet<String>(s2);
		else if(s2.size()==0)
			clone = new HashSet<String>(s1);
		else{
			clone = new HashSet<String>(s1);
			clone.addAll(s2);
		}
		return clone;
	}
	
	public void splitFile(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\training_all_feature_1_subfeature.csv";
		String storeFile1 = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\subfeature_train.csv";
		String storeFile2 = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\subfeature_test.csv";
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		String key = "";
		String reverseKey = "";
		HashSet<String> edge = new HashSet<String>();
		int num = 0;
		int numWritten = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile1));
			while((line = reader.readLine())!=null){
				if(num==5818)
					writer = new BufferedWriter(new FileWriter(storeFile2));
				
				num++;
				writer.append(line);
				writer.append("\n");
				writer.flush();	
			}
			
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		
	}
	
	public void extractSocialFeature(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\training_feature_1.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\training_all_feature_1.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int label = 0;
		int numWritten = 0;
		ArrayList<Integer> sNeighbor = new ArrayList<Integer>();
		ArrayList<Integer> tNeighbor = new ArrayList<Integer>();
		HashSet<Integer> commonNeighbors = new HashSet<Integer>();
		HashSet<Integer> unionNeighbors = new HashSet<Integer>();
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 46){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				//get neighbor nodes
				sNeighbor = mentionGraph.getNeighbors(source);
				tNeighbor = mentionGraph.getNeighbors(target);
				commonNeighbors = getIntersectionNeighbors(new HashSet(sNeighbor), new HashSet(tNeighbor));
				int CN = commonNeighbors.size();
				
				//get adar distance
				double AA = getAdarDistance(commonNeighbors, mentionGraph);
				
				//get Jaccard
				unionNeighbors = this.getUnionNeighbors(new HashSet(sNeighbor), new HashSet(tNeighbor));
				double JA = (double) CN/unionNeighbors.size();
				
				int PA = mentionGraph.getDegree(source)*mentionGraph.getDegree(target);
				numWritten++;
				this.writeSocialFeatures(writer, line, CN, AA, JA, PA, numWritten);
			
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			
			System.out.println(numWritten + " records have been written overall!");
			
			reader.close();
			writer.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeSocialFeatures(BufferedWriter writer, String line, int CN, double AA, double JA, int PA, int numWritten){
		try {
			//writer.append(String.valueOf(source)+Constant.SEPARATOR_COMMA);
			//writer.append(String.valueOf(target)+Constant.SEPARATOR_COMMA);
			//writer.append(String.valueOf(label)+Constant.SEPARATOR_COMMA);
			writer.append(line+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(CN)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(AA)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(JA)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(PA));
			
			writer.append("\n"); 
			writer.flush();
			if(numWritten % 1000 == 0)
				System.out.println(numWritten + " records have been written!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private double getAdarDistance(HashSet<Integer> cn, Graph mentionGraph){
		double dist = 0;
		Iterator<Integer> it = cn.iterator();
		int node = 0;
		double degree = 0;
		while(it.hasNext()){
			node = it.next();
			degree = (double) mentionGraph.getDegree(node);
			dist+= 1/Math.log(degree);
		}
		return dist;
	}
	
	//write attribute factor into file
	private boolean writeAttributeFactor(BufferedWriter writer, String[] words, ArrayList<String> attributeName, int numWritten){
		boolean nonEmpty = false;//to check whether all the attributes value are zero
		try {
			//write the label
			String name = "";
			String source = words[0];
			int label = Integer.parseInt(words[2]);
			int count = 0;
			int negCount = 0;
			String mark = "";
			boolean leg = true;
			
			mark = "+"+words[2];
			/*if(label==1){
				mark = "+"+words[2];
				if(positiveMap.containsKey(source))
					count = positiveMap.get(source);
				else
					count = 0;
				count++;
				positiveMap.put(source, count);
			}
			else{
				if(positiveMap.containsKey(source)){
					count = positiveMap.get(source);
					if(negativeMap.containsKey(source))
						negCount = negativeMap.get(source);
					else
						negCount = 0;
					negCount++;
					if(negCount<=count){
						mark = "+"+words[2];
						negativeMap.put(source, negCount);
					}
					else
						mark = "?"+words[2];
				}
				else{
					System.out.println("There is an error for negative instance: " + source);
					leg = false;
				}
				
			}*/
			//if(leg){
				
			for(int i = 3; i < words.length; i++){
				if(Double.parseDouble(words[i])!=0){
					if(Double.parseDouble(words[i])>1)
						System.out.println("Error in line: " + numWritten + " source: " + source);
					if(!nonEmpty){
						writer.append(mark);
						nonEmpty = true;
					}
					name = attributeName.get(i);
					writer.append(Constant.SEPARATOR_SPACE + name + Constant.SEPARATOR_COLON + words[i]);
				}
			}
			
			if(nonEmpty){
				writer.newLine();
				writer.flush();
			}
			
			if(numWritten % 1000 == 0)
				System.out.println(numWritten + " attributes factor have been written!");
			
			//}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nonEmpty;
	}
	
	//write edge factor into file
	private void writeEdgeFactor(BufferedWriter writer, HashSet<String> edgeFactorP){
		try {
			int numWritten = 0;
			int numWrittenS = 0;
			String edge = "";

			
			Iterator<String> it = edgeFactorP.iterator();
			while(it.hasNext()){
				numWritten++;
				edge = it.next();
				String[] nodes = edge.split(Constant.SEPARATOR_HYPHEN);
				writer.append("#edge"+Constant.SEPARATOR_SPACE);
				writer.append(nodes[0]+Constant.SEPARATOR_SPACE);
				writer.append(nodes[1]+Constant.SEPARATOR_SPACE);
				writer.append("hashtag-sentiment");
				writer.newLine();
				writer.flush();
				
				if(numWritten % 1000 == 0)
					System.out.println(numWritten + " sentiment edge factor have been written!");
			}
			
			/*it = edgeFactorS.iterator();
			while(it.hasNext()){
				numWrittenS++;
				edge = it.next();
				String[] nodes = edge.split(Constant.SEPARATOR_HYPHEN);
				writer.append("#edge"+Constant.SEPARATOR_SPACE);
				writer.append(nodes[0]+Constant.SEPARATOR_SPACE);
				writer.append(nodes[1]+Constant.SEPARATOR_SPACE);
				writer.append("balance");
				writer.newLine();
				writer.flush();
				
				if(numWrittenS % 1000 == 0)
					System.out.println(numWrittenS + " structure edge factor have been written!");
			}
			/*it = edgeFactorN.iterator();
			while(it.hasNext()){
				numWritten++;
				edge = it.next();
				String[] nodes = edge.split(Constant.SEPARATOR_HYPHEN);
				writer.append("#edge"+Constant.SEPARATOR_SPACE);
				writer.append(nodes[0]+Constant.SEPARATOR_SPACE);
				writer.append(nodes[1]+Constant.SEPARATOR_SPACE);
				writer.append("hashtag-negative");
				writer.newLine();
				writer.flush();
				
				if(numWritten % 1000 == 0)
					System.out.println(numWritten + " edge factor have been written!");
			}
			
			it = edgeFactorO.iterator();
			while(it.hasNext()){
				numWritten++;
				edge = it.next();
				String[] nodes = edge.split(Constant.SEPARATOR_HYPHEN);
				writer.append("#edge"+Constant.SEPARATOR_SPACE);
				writer.append(nodes[0]+Constant.SEPARATOR_SPACE);
				writer.append(nodes[1]+Constant.SEPARATOR_SPACE);
				writer.append("hashtag-objective");
				writer.newLine();
				writer.flush();
				
				if(numWritten % 1000 == 0)
					System.out.println(numWritten + " edge factor have been written!");
			}*/
			int num = numWritten + numWrittenS;
			System.out.println("There are "+num + " edge factor in total!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//generate training file for probabilistic graphical model
	public void generateGraphTrainingFile(int index, double threshold){
		readMapping();
		
		String tieFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\tie_strength_3.csv";
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\normalize_graph_feature_3_test.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\normalize_graph_feature_3_test.txt";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		
		this.tieStrengthMention(tieFile, index, threshold);
		
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
	
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int label = 0;
		int sourceOriginal = 0;
		int target1 = 0;
		int target2 = 0;
		int label1 = 0;
		int label2 = 0;
		int label3 = 0;
		
		int numWritten = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		ArrayList<Integer> nodeLineNumber = new ArrayList<Integer>();
		ArrayList<String> attributeName = new ArrayList<String>();//attribute name list
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> commonP = new HashSet<String>();
		HashSet<String> commonN = new HashSet<String>();
		HashSet<String> commonO = new HashSet<String>();
		HashSet<String> edgeFactorP = new HashSet<String>();
		HashSet<String> edgeFactorN = new HashSet<String>();
		HashSet<String> edgeFactorO = new HashSet<String>();
		HashSet<String> edgeFactorS = new HashSet<String>();//edgefactor to store structure factor
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 21){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				//process the headline
				if(num==1){
					for(int i = 0; i < words.length; i++)
						attributeName.add(words[i]);
					continue;
				}
				
				//write each attribute factor
				numWritten++;
				boolean nonEmpty = this.writeAttributeFactor(writer, words, attributeName, numWritten);
				
				if(!nonEmpty)
					num--;
				//process edge factor
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				if(source!=sourceOriginal){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							
							String key1 = String.valueOf(sourceOriginal) + Constant.SEPARATOR_HYPHEN + String.valueOf(target1);
							int tie1 = 0;
							if(tieMap.containsKey(key1))
								tie1 = tieMap.get(key1);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								//if(userHashtagMap.containsKey(sourceOriginal)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
									//sHash = userHashtagMap.get(sourceOriginal);
									//tHash1 = userHashtagMap.get(target1);
									//tHash2 = userHashtagMap.get(target2);
									
									//commonP = getTriadIntersection(sHash.pos,tHash1.pos,tHash2.pos);
									//commonP = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
									//double valueP = (double) commonP.size()/sHash.pos.size();
									
									//commonN = getTriadIntersection(sHash.neg,tHash1.neg,tHash2.neg);
									//double valueN = (double) commonN.size()/sHash.neg.size();
									
									//commonO = getTriadIntersection(sHash.obj,tHash1.obj,tHash2.obj);
									//double valueO = (double) commonO.size()/sHash.obj.size();
									//double value = (double)(commonP.size()+commonN.size()+commonO.size());
									//double value = (double) commonP.size();
									//value = value/sHash.svo.keySet().size();
								
								String key2 = String.valueOf(sourceOriginal) + Constant.SEPARATOR_HYPHEN + String.valueOf(target2);
								int tie2 = 0;
								if(tieMap.containsKey(key2))
									tie2 = tieMap.get(key2);
								//System.out.println("tie1: " + key1 + " tie2: " + key2);
								
								if(mentionGraph.connected(target1, target2))
									label3=1;
								else
									label3=0;
								
								int numLabel = label1+label2+label3;
								
								if(numLabel==0 || numLabel==2){
									//add edge to edgefactor
									if(tie1==1 && tie2 ==1){		
										//System.out.println("Strong tie: tie1: " + key1 + " tie2: " + key2);
										int index1 = nodeLineNumber.get(i);
										int index2 = nodeLineNumber.get(j);
										String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
										String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);
										
										/*System.out.println("key:" + key + " source: " + sourceOriginal+
												" target1: " + target1 + " target2: " + target2);*/
										
										if(!edgeFactorP.contains(key)&&!edgeFactorP.contains(reverseKey))
											edgeFactorP.add(key);
									}
								}
									
									
									/*if(valueN>threshold){		
										if(!edgeFactorN.contains(key)&&!edgeFactorN.contains(reverseKey))
											edgeFactorN.add(key);
									}
									if(valueO>threshold){		
										if(!edgeFactorO.contains(key)&&!edgeFactorO.contains(reverseKey))
											edgeFactorO.add(key);
									}*/
									
									
								//}
							}
						}
					}
						
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					nodeLineNumber.clear();
					sourceOriginal = source;
				}
				
				if(nonEmpty){ //only add nonEmpty instances
					list.add(target); //adjacency list for source node
					labelList.add(label);
					nodeLineNumber.add(num-1);	
				}
			}
			
			//process traid information for the last source node
			if(list.size()>1){
				for(int i = 0; i<(list.size()-1); i++){
					target1= list.get(i);
					label1 = labelList.get(i);
					String key1 = String.valueOf(sourceOriginal) + Constant.SEPARATOR_HYPHEN + String.valueOf(target1);
					int tie1 = 0;
					if(tieMap.containsKey(key1))
						tie1 = tieMap.get(key1);
					for(int j = i+1; j < list.size();j++){	
						target2 = list.get(j);	
						label2 = labelList.get(j);
						String key2 = String.valueOf(sourceOriginal) + Constant.SEPARATOR_HYPHEN + String.valueOf(target2);
						
						int tie2 = 0;
						if(tieMap.containsKey(key2))
							tie2 = tieMap.get(key2);
						//System.out.println("tie1: " + key1 + " tie2: " + key2);
						
						if(mentionGraph.connected(target1, target2))
							label3=1;
						else
							label3=0;
						
						int numLabel = label1+label2+label3;
						
						if(numLabel==0 || numLabel==2){
							//add edge to edgefactor
							if(tie1==1 && tie2 ==1){		
								//System.out.println("Strong tie1: " + key1 + " tie2: " + key2);
								int index1 = nodeLineNumber.get(i);
								int index2 = nodeLineNumber.get(j);
								String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
								String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);
								
								
								if(!edgeFactorP.contains(key)&&!edgeFactorP.contains(reverseKey))
									edgeFactorP.add(key);
							}
						}
						
						
						
						
					}
				}
			}
			
			reader.close();
			
			System.out.println("There are "+numWritten + " attributes factor in total!");
			
			//write edge factor
			this.writeEdgeFactor(writer, edgeFactorP);
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	//generate training file for probabilistic graphical model for mutual-follow graph
	public void generateFollowTrainingFile(int index, double threshold){
		readUserIdUniqueIdMapping();
		readFollowMapping();
		this.tieStrength(index, threshold);
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample4_follower_feature_175_test.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\normalized_sample4_follower_feature_175_test.txt";
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String line = "";
		int num = 0;
		String source = "";
		String target = "";
		int label = 0;
		String sourceOriginal = "";
		String target1 = "";
		String target2 = "";
		int label1 = 0;
		int label2 = 0;
		int numWritten = 0;
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		ArrayList<Integer> nodeLineNumber = new ArrayList<Integer>();
		ArrayList<String> attributeName = new ArrayList<String>();//attribute name list
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> commonP = new HashSet<String>();
		HashSet<String> commonN = new HashSet<String>();
		HashSet<String> commonO = new HashSet<String>();
		HashSet<String> edgeFactorP = new HashSet<String>();
		HashSet<String> edgeFactorN = new HashSet<String>();
		HashSet<String> edgeFactorO = new HashSet<String>();
		HashSet<String> edgeFactorS = new HashSet<String>();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 21){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				//process the headline
				if(num==1){
					for(int i = 0; i < words.length; i++)
						attributeName.add(words[i]);
					continue;
				}
				
				//write each attribute factor
				numWritten++;
				boolean nonEmpty = this.writeAttributeFactor(writer, words, attributeName, numWritten);
				if(!nonEmpty)
					num--;
				
				//process edge factor
				source = words[0];
				target = words[1];
				label = Integer.parseInt(words[2]);
				
				if(!source.equals(sourceOriginal)){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							String key1 = sourceOriginal + Constant.SEPARATOR_HYPHEN + target1;
							int tie1 = 0;
							if(tieMap.containsKey(key1))
								tie1 = tieMap.get(key1);
							
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								//if(userIdHashtagMap.containsKey(sourceOriginal)&&userIdHashtagMap.containsKey(target1)&&userIdHashtagMap.containsKey(target2)){
									//sHash = userIdHashtagMap.get(sourceOriginal);
									//tHash1 = userIdHashtagMap.get(target1);
									//tHash2 = userIdHashtagMap.get(target2);
									
									//commonP = getTriadIntersection(sHash.pos,tHash1.pos,tHash2.pos);
									//commonP = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
									//double valueP = (double) commonP.size()/sHash.pos.size();
									
									//commonN = getTriadIntersection(sHash.neg,tHash1.neg,tHash2.neg);
									//double valueN = (double) commonN.size()/sHash.neg.size();
									
									//commonO = getTriadIntersection(sHash.obj,tHash1.obj,tHash2.obj);
									//double valueO = (double) commonO.size()/sHash.obj.size();
									//double value = (double)(commonP.size()+commonN.size()+commonO.size());
									//double value = (double) commonP.size();
									//value = value/sHash.svo.keySet().size();
									
									
									String key2= sourceOriginal + Constant.SEPARATOR_HYPHEN + target1;
									int tie2 = 0;
									if(tieMap.containsKey(key2))
										tie2 = tieMap.get(key2);
									
									//add edge to edgefactor
									if(tie1==1 && tie2==1){
										int index1 = nodeLineNumber.get(i);
										int index2 = nodeLineNumber.get(j);
										String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
										String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);

										if(!edgeFactorP.contains(key)&&!edgeFactorP.contains(reverseKey))
											edgeFactorP.add(key);
									}
									
									if(label1==1 && label2==1){
										int index1 = nodeLineNumber.get(i);
										int index2 = nodeLineNumber.get(j);
										String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
										String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);

										if(!edgeFactorS.contains(key)&&!edgeFactorS.contains(reverseKey))
											edgeFactorS.add(key);
									}
									/*if(valueN>threshold){
										if(!edgeFactorN.contains(key)&&!edgeFactorN.contains(reverseKey))
											edgeFactorN.add(key);
									}
									if(valueO>threshold){
										if(!edgeFactorO.contains(key)&&!edgeFactorO.contains(reverseKey))
											edgeFactorO.add(key);
									}*/
								//}
							}
						}
					}
						
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					nodeLineNumber.clear();
					sourceOriginal = source;
				}
				
				if(nonEmpty){ //only add positive labels
					list.add(target); //adjacency list for source node
					labelList.add(label);
					nodeLineNumber.add(num-1);	
				}
			}
			
			//process triad for the last source node
			if(list.size()>1){
				for(int i = 0; i<(list.size()-1); i++){
					target1= list.get(i);
					label1 = labelList.get(i);
					String key1 = sourceOriginal + Constant.SEPARATOR_HYPHEN + target1;
					int tie1 = 0;
					if(tieMap.containsKey(key1))
						tie1 = tieMap.get(key1);
					
					for(int j = i+1; j < list.size();j++){
						target2 = list.get(j);
						label2 = labelList.get(j);
						String key2= sourceOriginal + Constant.SEPARATOR_HYPHEN + target1;
						int tie2 = 0;
						if(tieMap.containsKey(key2))
							tie2 = tieMap.get(key2);
						
						//add edge to edgefactor
						if(tie1==1 && tie2==1){
							int index1 = nodeLineNumber.get(i);
							int index2 = nodeLineNumber.get(j);
							String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
							String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);

							if(!edgeFactorP.contains(key)&&!edgeFactorP.contains(reverseKey))
								edgeFactorP.add(key);
						}
						if(label1==1 && label2==1){
							int index1 = nodeLineNumber.get(i);
							int index2 = nodeLineNumber.get(j);
							String key = String.valueOf(index1)+Constant.SEPARATOR_HYPHEN+String.valueOf(index2);
							String reverseKey = String.valueOf(index2)+Constant.SEPARATOR_HYPHEN+String.valueOf(index1);

							if(!edgeFactorS.contains(key)&&!edgeFactorS.contains(reverseKey))
								edgeFactorS.add(key);
						}
					}
				}
			}
			reader.close();
			
			System.out.println("There are "+numWritten + " attributes factor in total!");
			
			//write edge factor
			//this.writeEdgeFactor(writer, edgeFactorP, edgeFactorS);
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	//iterate a sample of nodes to get the statistics
	public void socialTheory(){
		readMapping();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map1.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int source1 = 0;
		int source2 = 0;
		int target1 = 0;
		int target2 = 0;
		int label = 0;
		int mark = 0; //mark to see whether it is the odd instance or the even instance
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> common = new HashSet();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				if(label==0)
					continue;
				if(mentionGraph.getDegree(source)<=1)
					continue;
				
				mark++;
				if(mark%2==1){
					source1 = source;
					target1 = target;
				}
				else{
					source2 = source;
					target2 = target;
				}
				
				//a triad has been detected
				if(source1==source2){
					//need to check whether the hashset exist
					if(userHashtagMap.containsKey(source1)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
						sHash = userHashtagMap.get(source1);
						tHash1 = userHashtagMap.get(target1);
						tHash2 = userHashtagMap.get(target2);
						
						common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
						boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
						int key = common.size();
						
						traidPair triInstance = new traidPair();
						if(socialTheoryMap.containsKey(key)){
							triInstance = socialTheoryMap.get(key);	
						}
						triInstance.addPair(connected);
						socialTheoryMap.put(key, triInstance);
						
						/*if(common.size()>0){
							boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
							Iterator<String> it = common.iterator();
							while(it.hasNext()){
								String tag = it.next();
								int sLabel = sHash.getHashtagSentiment(tag);
								int tLabel1 = tHash1.getHashtagSentiment(tag);
								int tLabel2 = tHash2.getHashtagSentiment(tag);
								int key = this.getSocialTheoryMapKey(sLabel, tLabel1, tLabel2);
								
								traidPair triInstance = new traidPair();
								if(socialTheoryMap.containsKey(key)){
									triInstance = socialTheoryMap.get(key);	
								}
								triInstance.addPair(connected);
								socialTheoryMap.put(key, triInstance);	
							}
						}*/
					}
				}//end of traid processing
				
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			System.out.println(mark + " records have been processed for social theory");
			
			//print map
			this.printSocialTheoryMap(socialTheoryMap, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	//iterate a sample of nodes to study the theory two
	public void socialTheoryTwo(){
		readMapping();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map2_unbalanced.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int sourceOriginal = 0;
		int target1 = 0;
		int target2 = 0;
		int label = 0;
		int label1 = 0;
		int label2 = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> common = new HashSet();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				if(mentionGraph.getDegree(source)<=1)
					continue;
				
				//a triad has been detected
				if(source!=sourceOriginal){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								if((label1==1 && label2==0)||(label1==0 && label2==1)){
									if(userHashtagMap.containsKey(sourceOriginal)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
										sHash = userHashtagMap.get(sourceOriginal);
										tHash1 = userHashtagMap.get(target1);
										tHash2 = userHashtagMap.get(target2);
										
										common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
										int key = common.size();
										boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
										
					
										traidPair triInstance = new traidPair();
										if(socialTheoryMap.containsKey(key)){
											triInstance = socialTheoryMap.get(key);	
										}
										triInstance.addPair(connected);
										socialTheoryMap.put(key, triInstance);
									}
								}
									
							
							}
						}
					}
					
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					sourceOriginal = source;	
				}//end of traid processing
				
				list.add(target);
				labelList.add(label);
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialTheoryMap(socialTheoryMap, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
	}
	
	//iterate a sample of nodes to study the theory two
	public void socialTheoryThree(){
		readMapping();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map3_unbalanced.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int sourceOriginal = 0;
		int target1 = 0;
		int target2 = 0;
		int label = 0;
		int label1 = 0;
		int label2 = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> common = new HashSet();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				if(mentionGraph.getDegree(source)<=1)
					continue;
				
				//a triad has been detected
				if(source!=sourceOriginal){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								if(label1==0 && label2==0){
									if(userHashtagMap.containsKey(sourceOriginal)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
										sHash = userHashtagMap.get(sourceOriginal);
										tHash1 = userHashtagMap.get(target1);
										tHash2 = userHashtagMap.get(target2);
										
										common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
										int key = common.size();
										boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
										
					
										traidPair triInstance = new traidPair();
										if(socialTheoryMap.containsKey(key)){
											triInstance = socialTheoryMap.get(key);	
										}
										triInstance.addPair(!connected);
										socialTheoryMap.put(key, triInstance);
									}
								}
									
							
							}
						}
					}
					
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					sourceOriginal = source;	
				}//end of traid processing
				
				list.add(target);
				labelList.add(label);
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialTheoryMap(socialTheoryMap, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	//iterate a sample of nodes to study the theory about common neighbors divided by the outdegree of the source node
	public void socialTheoryNeighbor(){
		readMapping();
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map_neighbor.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int sourceOriginal = 0;
		int target1 = 0;
		int target2 = 0;
		int label = 0;
		int label1 = 0;
		int label2 = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		
		ArrayList<Double> sHash = new ArrayList<Double>();//neighbor list for source
		ArrayList<Double> tHash1 = new ArrayList<Double>(); //neighbor list for target one
		ArrayList<Double> tHash2 = new ArrayList<Double>(); //neighbor list for target two
		ArrayList<Double> common = new ArrayList<Double>();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				if(mentionGraph.getDegree(source)<=1)
					continue;
				
				int key = 0;
				//a triad has been detected
				if(source!=sourceOriginal){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								if(label1==1 && label2==1)
									key = 1;
								else if(label1==0 && label2==0)
									key=3;
								else 
									key = 2;
								
								if(userHashtagMap.containsKey(sourceOriginal)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
									//sHash = userHashtagMap.get(sourceOriginal);
									//tHash1 = userHashtagMap.get(target1);
									//tHash2 = userHashtagMap.get(target2);
									
									//common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
									
									boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
									
				
									traidPair triInstance = new traidPair();
									if(socialTheoryMap.containsKey(key)){
										triInstance = socialTheoryMap.get(key);	
									}
									triInstance.addPair(!connected);
									socialTheoryMap.put(key, triInstance);
								}
									
							
							}
						}
					}
					
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					sourceOriginal = source;	
				}//end of traid processing
				
				list.add(target);
				labelList.add(label);
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialTheoryMap(socialTheoryMap, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	//iterate a sample of nodes to study the theory about common hashtags divided by the outdegree of the hashtag
	public void socialTheoryHashtag(){
		readMapping();

		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\mutual_graph_feature_1_all.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_1.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map_hashtag_m1.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		int sourceOriginal = 0;
		int target1 = 0;
		int target2 = 0;
		int label = 0;
		int label1 = 0;
		int label2 = 0;
		
		int key = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		
		
		traidPair instance = new traidPair();
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target
		hashtagSet tHash2 = new hashtagSet();//hashtag set for source
		HashSet<String> commonP = new HashSet<String>(); //hashtag set for target
		HashSet<String> commonN = new HashSet<String>(); //hashtag set for target
		HashSet<String> commonO = new HashSet<String>(); //hashtag set for target
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				//skip the first line
				if(num==1)
					continue;
				
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 51){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				
				//a triad has been detected
				if(source!=sourceOriginal){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								if(label1==1 && label2==1)
									key = 1;
								else if(label1==0 && label2==0)
									key = 3;
								else
									key = 2;
								
								sHash = userHashtagMap.get(sourceOriginal);
								tHash1 = userHashtagMap.get(target1);
								tHash2 = userHashtagMap.get(target2);
								//common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
								
								//positive
								commonP = getTriadIntersection(sHash.pos,tHash1.pos,tHash2.pos);
								
								//negative
								commonN = getTriadIntersection(sHash.neg,tHash1.neg,tHash2.neg);
								
								//objective
								commonO = getTriadIntersection(sHash.obj,tHash1.obj,tHash2.obj);
								
								//double value = (double) common.size()/sHash.svo.keySet().size();
								
								//positive
								//double value = (double) common.size()/sHash.pos.size();
								
								//negative
								double value = (double) (commonP.size()+commonN.size()+commonO.size());
								value = value/sHash.svo.keySet().size();
								
								//objective
								//double value = (double) common.size()/sHash.obj.size();
								int index = this.getIndex(value);
								
								ArrayList<traidPair> traidStat = new ArrayList<traidPair>();
								
								if(socialTheoryHashtag.containsKey(key))
									traidStat = socialTheoryHashtag.get(key);
								else{
									for(int k = 0; k < 7; k++)
										traidStat.add(new traidPair());
								}
								
								instance = traidStat.get(index);
								instance.addPair(mentionGraph.connected(target1, target2));
								traidStat.set(index, instance);
								
								socialTheoryHashtag.put(key, traidStat);							
							}
						}
					}
					
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					sourceOriginal = source;	
				}//end of traid processing
				
				list.add(target);
				labelList.add(label);
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialTheoryHashtag(socialTheoryHashtag, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	//investigate the relationship between number of common hashtags and label
	public void labelHashtag(){
		readMapping();

		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\mutual_graph_feature_1_all.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_label_hashtag_m1.csv";
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		int source = 0;
		int target = 0;
		
		int label = 0;
		int key = 0;
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target
		HashSet<String> common = new HashSet<String>(); //hashtag set for target
		pairStructure ps = new pairStructure();
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				//skip the first line
				if(num==1)
					continue;
				
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 51){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
							
				sHash = userHashtagMap.get(source);
				tHash1 = userHashtagMap.get(target);
				common = getIntersection(sHash.svo.keySet(),tHash1.svo.keySet());
				
				double value = (double) common.size()/sHash.svo.keySet().size();
				
				int index = this.getIndex(value);
				
				if(socialLabelHashtag.containsKey(index))
					ps = socialLabelHashtag.get(index);
				else{
					ps = new pairStructure();
				}
				
				if(label==1)
					ps.friendValue.add(value);
				else if(label==0)
					ps.nonFriendValue.add(value);
				
				socialLabelHashtag.put(index, ps);							
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialLabelHashtag(socialLabelHashtag, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private int getIndex(double value){
		int index = 0;
		if(value==0)
			index = 0;
		else if(value>0 && value <=0.1)
			index = 1;
		else if(value>0.1 && value<=0.2)
			index = 2;
		else if(value>0.2 && value<=0.3)
			index = 3;
		else if(value>0.3 && value<=0.4)
			index = 4;
		else if(value>0.4 && value<=0.5)
			index = 5;
		else if(value>0.5 && value <=1)
			index = 6;
		return index;
	}
	
	//read a part of follow graph
	private void readFollowMap(String inputFile){
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		String key = "";
		int value = 0;
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}
				
				key = words[0] + Constant.SEPARATOR_HYPHEN + words[1];
				value = Integer.parseInt(words[2]);
				
				if(value==1)
					followMap.add(key);
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
			
			System.out.println(num + " records in the followMap file in total!");
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void socialTheoryFollowHashtag(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\normalized_sample4_follower_feature_175_train.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map_hashtag_f_sen.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\followPairOut.csv";
		
		readUserIdUniqueIdMapping();
		readFollowMapping();
		readFollowMap(connectedFile);
		
		BufferedReader reader = null;
		String line = "";
		int num = 0;
		String source = "";
		String target = "";
		String sourceOriginal = "";
		String target1 = "";
		String target2 = "";
		int label = 0;
		int label1 = 0;
		int label2 = 0;
		ArrayList<String> list = new ArrayList<String>();
		ArrayList<Integer> labelList = new ArrayList<Integer>();
		String edge = "";
		String reverseEdge = "";
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target
		hashtagSet tHash2 = new hashtagSet();//hashtag set for source
		HashSet<String> commonP = new HashSet<String>(); //hashtag set for target
		HashSet<String> commonN = new HashSet<String>(); //hashtag set for target
		HashSet<String> commonO = new HashSet<String>(); //hashtag set for target
		traidPair instance = new traidPair();
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			while((line = reader.readLine())!=null){
				num++;
				//skip the first line
				if(num==1)
					continue;
				
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 21){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = words[0];
				target = words[1];
				label = Integer.parseInt(words[2]);
				
				int key = 0;
				//a triad has been detected
				if(!source.equals(sourceOriginal)){
					//traid processing
					if(list.size()>1){
						for(int i = 0; i<(list.size()-1); i++){
							target1= list.get(i);
							label1 = labelList.get(i);
							for(int j = i+1; j < list.size();j++){
								target2 = list.get(j);
								label2 = labelList.get(j);
								
								edge = target1+Constant.SEPARATOR_HYPHEN+target2;
								reverseEdge = target2+Constant.SEPARATOR_HYPHEN+target1;
								
								boolean connected = false;
								if(followMap.contains(edge) || followMap.contains(reverseEdge))
									connected = true;
								
								if(label1==1 && label2==1)
									key = 1;
								else if(label1==0 && label2==0)
									key=3;
								else 
									key = 2;
								
								sHash = userIdHashtagMap.get(sourceOriginal);
								tHash1 = userIdHashtagMap.get(target1);
								tHash2 = userIdHashtagMap.get(target2);
								//common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
								//positive
								commonP = getTriadIntersection(sHash.pos,tHash1.pos,tHash2.pos);
								
								//negative
								commonN = getTriadIntersection(sHash.neg,tHash1.neg,tHash2.neg);
								
								//objective
								commonO = getTriadIntersection(sHash.obj,tHash1.obj,tHash2.obj);
								
								//double value = (double) common.size()/sHash.svo.keySet().size();
								
								//positive
								//double value = (double) common.size()/sHash.pos.size();
								
								//negative
								//double value = (double) common.size()/sHash.neg.size();
								
								//objective
								double value = (double) (commonP.size()+commonN.size()+commonO.size());
								value = value/sHash.svo.keySet().size();
								int index = this.getIndex(value);
								
								ArrayList<traidPair> traidStat = new ArrayList<traidPair>();
								
								if(socialTheoryHashtag.containsKey(key))
									traidStat = socialTheoryHashtag.get(key);
								else{
									for(int k = 0; k < 7; k++)
										traidStat.add(new traidPair());
								}
								
								instance = traidStat.get(index);
								instance.addPair(connected);
								traidStat.set(index, instance);
								
								socialTheoryHashtag.put(key, traidStat);		
							}
						}
					}
					
					//create new source node and its adjacency list
					list.clear();
					labelList.clear();
					sourceOriginal = source;	
				}//end of traid processing
				
				list.add(target);
				labelList.add(label);
				
			}
			reader.close();
			System.out.println(num + " records have been read!");
			
			//print map
			this.printSocialTheoryHashtag(socialTheoryHashtag, storeFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}


	//iterate the entire mention graph to get the statistics
	public void socialTheoryEntireGraph(){
		readMapping();
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_3.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\graph\\social_theory_map_entire_graph_unbalanced.csv";
		//create the graph
		Graph mentionGraph = new Graph();
		mentionGraph.createGraph(connectedFile);
	
		int source1 = 0;
		int target1 = 0;
		int target2 = 0;
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash1 = new hashtagSet(); //hashtag set for target one
		hashtagSet tHash2 = new hashtagSet(); //hashtag set for target two
		HashSet<String> common = new HashSet();
					
		//iterate the mention graph		
		Iterator it = mentionGraph.adj.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, ArrayList<Integer>> pairs = (Map.Entry<Integer, ArrayList<Integer>>) it.next();
			source1 = pairs.getKey();
			
			//skip the nodes whose degree is less than 2
			if(mentionGraph.getDegree(source1)<=1)
				continue;
			
			list = pairs.getValue();
			for(int i = 0; i<(list.size()-1); i++){
				for(int j = i+1; j < list.size();j++){
					target1= list.get(i);
					target2 = list.get(j);
					if(userHashtagMap.containsKey(source1)&&userHashtagMap.containsKey(target1)&&userHashtagMap.containsKey(target2)){
						sHash = userHashtagMap.get(source1);
						tHash1 = userHashtagMap.get(target1);
						tHash2 = userHashtagMap.get(target2);
						
						common = getTriadIntersection(sHash.svo.keySet(),tHash1.svo.keySet(),tHash2.svo.keySet());
						boolean connected = mentionGraph.connected(target1, target2);//test whether the two targets nodes are connected
						int key = common.size();
						
						traidPair triInstance = new traidPair();
						if(socialTheoryMap.containsKey(key)){
							triInstance = socialTheoryMap.get(key);	
						}
						triInstance.addPair(!connected);
						socialTheoryMap.put(key, triInstance);
					}
				}
				
			}
		}
		
		//print map
		this.printSocialTheoryMap(socialTheoryMap, storeFile);
	}
	
	private void printSocialTheoryMap(HashMap<Integer, traidPair> map, String storeFile){
		BufferedWriter writer  = null;
		int key = 0;
		traidPair value = new traidPair();
		int num = 0;
		int numTraid = 0;
		int numPair = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = map.entrySet().iterator();
			while(it.hasNext()){
				num++;
				Map.Entry<Integer, traidPair> pairs = (Map.Entry<Integer, traidPair>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
				numTraid = value.numTraid;
				numPair = value.numPair;
		
				writer.append(String.valueOf(key) + Constant.SEPARATOR_COMMA);//key
				writer.append(String.valueOf(numTraid)+Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(numPair));
				
				writer.append("\n");
				writer.flush();
				
				if(num % 10000 == 0)
					System.out.println(num + " records have been written!");
			}
			writer.close();
			
			System.out.println("There are " + num + " user records have been written overall");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void printSocialLabelHashtag(HashMap<Integer, pairStructure> socialLabelHashtag, String storeFile){
		BufferedWriter writer  = null;
		double key = 0;
		pairStructure ps = new pairStructure();
	
		double averFriend = 0;
		double averNonFriend = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = socialLabelHashtag.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, pairStructure> pairs = (Map.Entry<Integer, pairStructure>)it.next();
				key = pairs.getKey();
				ps = pairs.getValue();
				averFriend = this.meanArrayList(ps.friendValue);
				averNonFriend = this.meanArrayList(ps.nonFriendValue);
				
				writer.append(String.valueOf(key) + Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(averFriend) + Constant.SEPARATOR_COMMA);
				writer.append(String.valueOf(averNonFriend));
				
				writer.append("\n");
				writer.flush();
			}
			
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//calculate the mean value for an arraylist
	private double meanArrayList(ArrayList<Double> list){
		double mean = 0;
		for(int i = 0; i < list.size(); i++){
			mean+= list.get(i);
		}
		mean = mean/list.size();
		return mean;
	}
	private void printSocialTheoryHashtag(HashMap<Integer, ArrayList<traidPair>> socialTheoryHashtag, String storeFile){
		BufferedWriter writer  = null;
		double key = 0;
		ArrayList<traidPair> value = new ArrayList<traidPair>();
		
		traidPair tri = new traidPair();
		int num = 0;
		int numTraid = 0;
		int numPair = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = socialTheoryHashtag.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, ArrayList<traidPair>> pairs = (Map.Entry<Integer, ArrayList<traidPair>>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
				for(int i = 0; i < value.size(); i++){
					tri = value.get(i);
					writer.append(String.valueOf(key)+Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(i) + Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(tri.numTraid)+Constant.SEPARATOR_COMMA);
					writer.append(String.valueOf(tri.numPair));
					writer.append('\n');
					writer.flush();
				}
			}
			
			writer.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private int getSocialTheoryMapKey(int sLabel, int tLabel1, int tLabel2){
		int key = 0;
		int offset = 0;
		if(tLabel1==1 && tLabel2==1)
			offset=0;
		else if((tLabel1==1 && tLabel2==2) || (tLabel1==2 && tLabel2==1))
			offset = 1;
		else if((tLabel1==1 && tLabel2==3) || (tLabel1==3 && tLabel2==1))
			offset = 2;
		else if((tLabel1==2 && tLabel2==3) || (tLabel1==3 && tLabel2==2))
			offset = 3;
		else if(tLabel1==2 && tLabel2==2)
			offset = 4;
		else if(tLabel1==3 && tLabel2==3)
			offset = 5;
		
		if(sLabel==1)
			key = 0 + offset;
		else if(sLabel==2)
			key = 6 + offset;
		else if(sLabel==3)
			key = 12 + offset;
		
		return key;
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
	
	//write statistics about source node id and the number of hashtags it has
	public void sourceUserID(){
		readUserIdUniqueIdMapping();
		readFollowMapping();
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\sourceNodeHashtagMap.csv";
		BufferedWriter writer  = null;
		String key = "";
		hashtagSet value = new hashtagSet();
		int size = 0;
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = userIdHashtagMap.entrySet().iterator();
			while(it.hasNext()){
				num++;
				Map.Entry<String, hashtagSet> pairs = (Map.Entry<String, hashtagSet>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
				size = value.svo.keySet().size();
		
				writer.append(key);//userId
				writer.append(Constant.SEPARATOR_COMMA);
				
				//write number of hashtags
				
				writer.append(String.valueOf(size));
				
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
	//extract features to users from the follow/followee graph
	public void extractFollowFeature(){
		readUserIdUniqueIdMapping();
		readFollowMapping();
		
		System.out.println("Start to extract features");
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\candidates_175.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\follower_feature_175.csv";
		String storeFile1 = "D:\\Social_Media_Analytics\\dataset\\sentiment\\follower\\follower_no_sentiment_175.csv";
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		BufferedWriter writer1 = null;
		
		String line = "";
		int num = 0;
		int numWritten = 0;
		String source = "";
		String target = "";
		int label = 0;
		
		int interPos = 0;
		int interNeg = 0;
		int interObj = 0;
		int interAll = 0;
		int interPN = 0;
		int interNP = 0;
		
		int unPos = 0;
		int unNeg = 0;
		int unObj = 0;
		int unAll = 0;
		
		int uListSize = 0;//the size of users who have talked about a particular hashtag
		
		//HashSet<String> posSet = new HashSet<String>(); //intersection of positive hashtag set
		//HashSet<String> negSet = new HashSet<String>(); //intersection of negative hashtag set
		//HashSet<String> objSet = new HashSet<String>(); //intersection of objective hashtag set
		
		//ArrayList<Integer> uList = new ArrayList<Integer>();
		ArrayList<Integer> userListPosSize = new ArrayList<Integer>();
		ArrayList<Integer> userListNegSize = new ArrayList<Integer>();
		ArrayList<Integer> userListObjSize = new ArrayList<Integer>();
		ArrayList<Integer> userListAllSize = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash = new hashtagSet(); //hashtag set for target
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			writer1 = new BufferedWriter(new FileWriter(storeFile1));
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
				//skip the edge when anyone of the node doesn't have a sentimental opinion set
				if(!userIdHashtagMap.containsKey(source) || !userIdHashtagMap.containsKey(target)){
					writer1.append(line);
					writer1.newLine();
					writer1.flush();
					continue;		
				}
				
				sHash = userIdHashtagMap.get(source);
				tHash = userIdHashtagMap.get(target);
				
				//intersection of hashtag set
				ArrayList<String> posSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.pos));
				ArrayList<String> negSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.neg));
				ArrayList<String> objSet = new ArrayList<String>(getIntersection(sHash.obj,tHash.obj));
				ArrayList<String> allSet = new ArrayList<String>(getIntersection(sHash.svo.keySet(),tHash.svo.keySet()));
				ArrayList<String> PNSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.neg));
				ArrayList<String> NPSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.pos));
				
				interPos = posSet.size();
				interNeg = negSet.size();
				interObj = objSet.size();
				interAll = allSet.size();
				interPN = PNSet.size();
				interNP = NPSet.size();
				
				//union of hashtag set
				unPos = getUnion(sHash.pos,tHash.pos).size();
				unNeg = getUnion(sHash.neg,tHash.neg).size();
				unObj = getUnion(sHash.obj,tHash.obj).size();
				unAll = getUnion(sHash.svo.keySet(),tHash.svo.keySet()).size();
				
				//get the user lists who talked about a particular hashtag
				for(int i = 0; i < posSet.size(); i++){
					uListSize = findUserSize(posSet.get(i),1);
					if(uListSize>1)
						userListPosSize.add(uListSize);
				}
				for(int i = 0; i < negSet.size(); i++){
					uListSize = findUserSize(negSet.get(i),2);
					if(uListSize>1)
						userListNegSize.add(uListSize);
				}
				for(int i = 0; i < objSet.size(); i++){
					uListSize = findUserSize(objSet.get(i),3);
					if(uListSize>1)
						userListObjSize.add(uListSize);
				}
				for(int i = 0; i < allSet.size(); i++){
					uListSize = findUserSize(allSet.get(i),4);
					if(uListSize>1)
						userListAllSize.add(uListSize);
				}
				
				//compute the similarity of SVO score
				double eclidean = this.euclideanDist(allSet, sHash.svo, tHash.svo);
				double cosine = this.cosineDist(allSet, sHash.svo, tHash.svo);
				
				
				//get neighbor nodes
				int CN = Integer.parseInt(words[5]);
				
				//get adar distance
				double AA = Double.parseDouble(words[6]);
				
				//get Jaccard
				int sDegree = Integer.parseInt(words[1]);
				int tDegree = Integer.parseInt(words[3]);
				int union = sDegree + tDegree - CN;
				if(union<=0){
					System.out.println("There is an error in computing union neighbors");
					continue;
				}
				double JA = (double) CN/union;
				
				int PA = sDegree*tDegree;
				
				numWritten++;
				//write the features into a file
				writeFollowFeatures(writer, source, target, label, interPos, interNeg, interObj, interAll, interPN, interNP,
						unPos, unNeg, unObj, unAll, userListPosSize, userListNegSize, 
						userListObjSize, userListAllSize, eclidean, cosine, CN, AA, JA, PA, numWritten);			
				
				
				userListPosSize.clear();
				userListNegSize.clear();
				userListObjSize.clear();
				userListAllSize.clear();
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed for features!");
			}
				
			System.out.println(numWritten + " feature records have been written overall!");
			
			reader.close();
			writer.close();
			writer1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//extract features to users from the graph
	public void extractFeature(){
		readMapping();
		
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\2_hop_candiates_20n.csv";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\mutual_graph_feature_20n_rec.csv";
		String connectedFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_DB_20.csv";
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		
		String line = "";
		int num = 0;
		int numWritten = 0;
		int source = 0;
		int target = 0;
		int label = 0;
		
		int interPos = 0;
		int interNeg = 0;
		int interObj = 0;
		int interAll = 0;
		int interPN = 0;
		int interNP = 0;
		
		int unPos = 0;
		int unNeg = 0;
		int unObj = 0;
		int unAll = 0;
		
		int uListSize = 0;//the size of users who have talked about a particular hashtag
		
		//HashSet<String> posSet = new HashSet<String>(); //intersection of positive hashtag set
		//HashSet<String> negSet = new HashSet<String>(); //intersection of negative hashtag set
		//HashSet<String> objSet = new HashSet<String>(); //intersection of objective hashtag set
		
		//ArrayList<Integer> uList = new ArrayList<Integer>();
		ArrayList<Integer> userListPosSize = new ArrayList<Integer>();
		ArrayList<Integer> userListNegSize = new ArrayList<Integer>();
		ArrayList<Integer> userListObjSize = new ArrayList<Integer>();
		ArrayList<Integer> userListAllSize = new ArrayList<Integer>();
		
		hashtagSet sHash = new hashtagSet();//hashtag set for source
		hashtagSet tHash = new hashtagSet(); //hashtag set for target
		
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(storeFile));
			while((line = reader.readLine())!=null){
				num++;
				String[] words = line.split(Constant.SEPARATOR_COMMA);
				if(words.length != 3){
					System.out.println("There is an error in line: " + num + " : " + line);
					continue;
				}			
				
				source = Integer.parseInt(words[0]);
				target = Integer.parseInt(words[1]);
				label = Integer.parseInt(words[2]);
				//skip the edge when anyone of the node doesn't have a sentimental opinion set
				if(!userHashtagMap.containsKey(source) || !userHashtagMap.containsKey(target))
					continue;		
				
				sHash = userHashtagMap.get(source);
				tHash = userHashtagMap.get(target);
				
				/*System.out.println(source + " pos size: " + sHash.pos.size() + " pos: " + sHash.pos);
				System.out.println(source + " neg size: " + sHash.neg.size() + " neg: " + sHash.neg);
				System.out.println(source + " obj size: " + sHash.obj.size() + " obj: " + sHash.obj);
				System.out.println(target + " pos size: " + tHash.pos.size() + " pos: " + tHash.pos);
				System.out.println(target + " neg size: " + tHash.neg.size() + " neg: " + tHash.neg);
				System.out.println(target + " obj size: " + tHash.obj.size() + " obj: " + tHash.obj);*/
				
				//intersection of hashtag set
				ArrayList<String> posSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.pos));
				ArrayList<String> negSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.neg));
				ArrayList<String> objSet = new ArrayList<String>(getIntersection(sHash.obj,tHash.obj));
				ArrayList<String> allSet = new ArrayList<String>(getIntersection(sHash.svo.keySet(),tHash.svo.keySet()));
				ArrayList<String> PNSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.neg));
				ArrayList<String> NPSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.pos));
				
				interPos = posSet.size();
				interNeg = negSet.size();
				interObj = objSet.size();
				interAll = allSet.size();
				interPN = PNSet.size();
				interNP = NPSet.size();
				
				//union of hashtag set
				unPos = getUnion(sHash.pos,tHash.pos).size();
				unNeg = getUnion(sHash.neg,tHash.neg).size();
				unObj = getUnion(sHash.obj,tHash.obj).size();
				unAll = getUnion(sHash.svo.keySet(),tHash.svo.keySet()).size();
				
				//get the user lists who talked about a particular hashtag
				for(int i = 0; i < posSet.size(); i++){
					uListSize = findUserSize(posSet.get(i),1);
					if(uListSize>0)
						userListPosSize.add(uListSize);
				}
				for(int i = 0; i < negSet.size(); i++){
					uListSize = findUserSize(negSet.get(i),2);
					if(uListSize>0)
						userListNegSize.add(uListSize);
				}
				for(int i = 0; i < objSet.size(); i++){
					uListSize = findUserSize(objSet.get(i),3);
					if(uListSize>0)
						userListObjSize.add(uListSize);
				}
				for(int i = 0; i < allSet.size(); i++){
					uListSize = findUserSize(allSet.get(i),4);
					if(uListSize>0)
						userListAllSize.add(uListSize);
				}
				
				//compute the similarity of SVO score
				double eclidean = this.euclideanDist(allSet, sHash.svo, tHash.svo);
				double cosine = this.cosineDist(allSet, sHash.svo, tHash.svo);
				
				//create the graph
				Graph mentionGraph = new Graph();
				mentionGraph.createGraph(connectedFile);
				
				ArrayList<Integer> sNeighbor = new ArrayList<Integer>();
				ArrayList<Integer> tNeighbor = new ArrayList<Integer>();
				HashSet<Integer> commonNeighbors = new HashSet<Integer>();
				HashSet<Integer> unionNeighbors = new HashSet<Integer>();
				
				//get neighbor nodes
				sNeighbor = mentionGraph.getNeighbors(source);
				tNeighbor = mentionGraph.getNeighbors(target);
				commonNeighbors = getIntersectionNeighbors(new HashSet(sNeighbor), new HashSet(tNeighbor));
				int CN = commonNeighbors.size();
				
				//get adar distance
				double AA = getAdarDistance(commonNeighbors, mentionGraph);
				
				//get Jaccard
				unionNeighbors = this.getUnionNeighbors(new HashSet(sNeighbor), new HashSet(tNeighbor));
				double JA = (double) CN/unionNeighbors.size();
				
				int PA = mentionGraph.getDegree(source)*mentionGraph.getDegree(target);
				
				numWritten++;
				//write the features into a file
				writeFeatures(writer, source, target, label, interPos, interNeg, interObj, interAll, interPN, interNP,
						unPos, unNeg, unObj, unAll, userListPosSize, userListNegSize, 
						userListObjSize, userListAllSize, eclidean, cosine, CN, AA, JA, PA, numWritten);			
				
				
				userListPosSize.clear();
				userListNegSize.clear();
				userListObjSize.clear();
				userListAllSize.clear();
				if(num % 10000 == 0)
					System.out.println(num + " records have been processed!");
			}
				
			System.out.println(numWritten + " records have been written overall!");
			
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//compute the eclidean distance of svo score
	private double euclideanDist(ArrayList<String> allSet, HashMap<String, Double> source, HashMap<String, Double> target){
		double dist = 0;
		double sum = 0;
		
		for(int i = 0; i < allSet.size(); i++){
			String tag = allSet.get(i);
			double s1 = source.get(tag);
			double s2 = target.get(tag);
			sum += (s1-s2)*(s1-s2);
		}
		dist = Math.sqrt(sum);
		return dist;
	}
	
	//compute the cosine distance of svo score
	private double cosineDist(ArrayList<String> allSet, HashMap<String, Double> source, HashMap<String, Double> target){
		double dist = 0;
		double sum = 0;
		
		for(int i = 0; i < allSet.size(); i++){
			String tag = allSet.get(i);
			double s1 = source.get(tag);
			double s2 = target.get(tag);
			sum += s1*s2;
		}
		dist = sum/(double)(source.size()*target.size());
		return dist;
	}
	//extract features to users from the graph
		public void extractFeature1(){
			readMapping();
			
			String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\mutual_mention_graph_3.csv";
			String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\unsupervised\\feature_3_updated.csv";
		
			BufferedReader reader = null;
			BufferedWriter writer = null;
			
			String line = "";
			int num = 0;
			int numWritten = 0;
			int source = 0;
			int target = 0;
			
			
			int interPos = 0;
			int interNeg = 0;
			int interDiff = 0;
			int interAll = 0;
			
			int interPN = 0;
			int interNP = 0;
			
			//HashSet<String> posSet = new HashSet<String>(); //intersection of positive hashtag set
			//HashSet<String> negSet = new HashSet<String>(); //intersection of negative hashtag set
			//HashSet<String> objSet = new HashSet<String>(); //intersection of objective hashtag set
			
			ArrayList<Integer> uList = new ArrayList<Integer>();
			ArrayList<Integer> userListPosSize = new ArrayList<Integer>();
			ArrayList<Integer> userListNegSize = new ArrayList<Integer>();
			ArrayList<Integer> userListObjSize = new ArrayList<Integer>();
			ArrayList<Integer> userListAllSize = new ArrayList<Integer>();
			ArrayList<Integer> userListSimilarSize = new ArrayList<Integer>();
			ArrayList<Integer> userListDiffSize = new ArrayList<Integer>();
			
			hashtagSet sHash = new hashtagSet();//hashtag set for source
			hashtagSet tHash = new hashtagSet(); //hashtag set for target
			
			HashSet<String> sHashSet = new HashSet<String>();
			HashSet<String> tHashSet = new HashSet<String>();
			try {
				reader = new BufferedReader(new FileReader(inputFile));
				writer = new BufferedWriter(new FileWriter(storeFile));
				while((line = reader.readLine())!=null && num<=2){
					num++;
					String[] words = line.split(Constant.SEPARATOR_COMMA);
					if(words.length != 4){
						System.out.println("There is an error in line: " + num + " : " + line);
						continue;
					}			
					
					source = Integer.parseInt(words[0]);
					target = Integer.parseInt(words[1]);
					
					//skip the edge when anyone of the node doesn't have a sentimental opinion set
					if(!userHashtagMap.containsKey(source) || !userHashtagMap.containsKey(target))
						continue;		
					
					sHash = userHashtagMap.get(source);
					tHash = userHashtagMap.get(target);
					
					//get the hashtag of all sentiment
					//sHashSet = sHash.allHashtags();
					//tHashSet = tHash.allHashtags();
					
					System.out.println(source + " pos size: " + sHash.pos.size() + " pos: " + sHash.pos);
					System.out.println(source + " neg size: " + sHash.neg.size() + " neg: " + sHash.neg);
					System.out.println(source + " obj size: " + sHash.obj.size() + " obj: " + sHash.obj);
					System.out.println(source + " all size: " + sHashSet.size() + " all: " + sHashSet);
					System.out.println(target + " pos size: " + tHash.pos.size() + " pos: " + tHash.pos);
					System.out.println(target + " neg size: " + tHash.neg.size() + " neg: " + tHash.neg);
					System.out.println(target + " all size: " + tHashSet.size() + " all: " + tHashSet);
					
					//intersection of hashtag set
					ArrayList<String> posSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.pos));
					ArrayList<String> negSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.neg));
					ArrayList<String> allSet = new ArrayList<String>(getIntersection(sHashSet,tHashSet));
					ArrayList<String> PNSet = new ArrayList<String>(getIntersection(sHash.pos,tHash.neg));
					ArrayList<String> NPSet = new ArrayList<String>(getIntersection(sHash.neg,tHash.pos));
					
					interPos = posSet.size();
					interNeg = negSet.size();
					interAll = allSet.size();
					interPN = PNSet.size();
					interNP = NPSet.size();
					interDiff = interPN + interNP;
					
					System.out.println(source + " pos size: " + sHash.pos.size() + " pos: " + sHash.pos);
					System.out.println(source + " neg size: " + sHash.neg.size() + " neg: " + sHash.neg);
					System.out.println(source + " obj size: " + sHash.obj.size() + " obj: " + sHash.obj);
					System.out.println(source + " all size: " + sHashSet.size() + " all: " + sHashSet);
					System.out.println(target + " pos size: " + tHash.pos.size() + " pos: " + tHash.pos);
					System.out.println(target + " neg size: " + tHash.neg.size() + " neg: " + tHash.neg);
					System.out.println(target + " all size: " + tHashSet.size() + " all: " + tHashSet);
					
					//get the user lists who talked about a particular hashtag
					for(int i = 0; i < allSet.size(); i++){
						uList = findUserList(allSet.get(i),4);
						if(uList.size()>0)
							userListAllSize.add(uList.size());
					}
					for(int i = 0; i < posSet.size(); i++){
						uList = findUserList(posSet.get(i),2);
						if(uList.size()>0)
							userListDiffSize.add(uList.size());
					}
					for(int i = 0; i < negSet.size(); i++){
						uList = findUserList(negSet.get(i),1);
						if(uList.size()>0)
							userListDiffSize.add(uList.size());
					}
					
					numWritten++;
					//write the features into a file
					writeFeatures1(writer, source, target, interAll, interDiff, interPos, interNeg, 
							userListAllSize, userListDiffSize, numWritten);		
					
					
					userListAllSize.clear();
					userListDiffSize.clear();
					if(num % 10000 == 0)
						System.out.println(num + " records have been processed!");
				}
					
				System.out.println(numWritten + " records have been written overall!");
				
				reader.close();
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	private void writeFeatures(BufferedWriter writer, int source, int target, int label, int interPos, int interNeg, int interObj, int interAll, int interPN, int interNP,
			int unPos, int unNeg, int unObj,  int unAll,  ArrayList<Integer>userListPosSize, ArrayList<Integer>userListNegSize, 
			ArrayList<Integer>userListObjSize, ArrayList<Integer> userListAllSize, double eclidean, double cosine, 
			int CN, double AA, double JA, int PA, int numWritten)
	{
		try {
			writer.append(String.valueOf(source)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(target)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(label)+Constant.SEPARATOR_COMMA);
			
			//features related to intersection
			writer.append(String.valueOf(interPos)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interNeg)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interObj)+ Constant.SEPARATOR_COMMA);
			double f4 = interPos + interNeg;
			writer.append(String.valueOf(f4)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interAll)+Constant.SEPARATOR_COMMA);
			double f5 = interPos + interNeg + interObj;
			writer.append(String.valueOf(f5)+Constant.SEPARATOR_COMMA);
			
			
			//features related to Jaccard
			double f7 = 0; 
			if(unPos>0)
				f7 = (double) interPos/unPos;
			writer.append(String.valueOf(f7) + Constant.SEPARATOR_COMMA);
			double f8 = 0;
			if(unNeg>0)
				f8 = (double) interNeg/unNeg;
			writer.append(String.valueOf(f8)+Constant.SEPARATOR_COMMA);
			double f9 = 0;
			if(unObj>0)
				f9 = (double) interObj/unObj;
			writer.append(String.valueOf(f9) + Constant.SEPARATOR_COMMA);
			double f43 = 0;
			if(unAll>0)
				f43=(double) interAll/unAll;
			writer.append(String.valueOf(f43) + Constant.SEPARATOR_COMMA);
			
			//features related to sentiment homophily coefficient
			double f10 = 0;
			if(interAll>0)
				f10 = (double) (interPos+interNeg)/interAll;
			writer.append(String.valueOf(f10) + Constant.SEPARATOR_COMMA);
			
			double f11 = 0;
			if(interAll>0)
				f11 = (double) (interPN + interNP)/interAll;
			writer.append(String.valueOf(f11) + Constant.SEPARATOR_COMMA);
			
			
			double[] fPos = new double[5];
			fPos = getFeatures(userListPosSize);
			double[] fNeg = new double[5];
			fNeg = getFeatures(userListNegSize);
			double[] fObj = new double[5];
			fObj = getFeatures(userListObjSize);
			double[] fAll = new double[5];
			fAll = getFeatures(userListAllSize);
			
			//features related to Adar
			writer.append(String.valueOf(fPos[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[0]) + Constant.SEPARATOR_COMMA);
			
			double f16 = fPos[0]+fNeg[0];
			writer.append(String.valueOf(f16) + Constant.SEPARATOR_COMMA);
			double f17 = fPos[0]+fNeg[0]+fObj[0];
			writer.append(String.valueOf(f17) + Constant.SEPARATOR_COMMA);
			
			
			//features related to smallest common hashtags
			writer.append(String.valueOf(fPos[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[1]) + Constant.SEPARATOR_COMMA);
			
			double f22 = fPos[1];
			if(f22>fNeg[1])
				f22=fNeg[1];
			writer.append(String.valueOf(f22) + Constant.SEPARATOR_COMMA);
			
			double f23 = f22;
			if(f23>fObj[1])
				f23=fObj[1];
			writer.append(String.valueOf(f23) + Constant.SEPARATOR_COMMA);
			
			//features related to largest common hashtags
			writer.append(String.valueOf(fPos[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[2]) + Constant.SEPARATOR_COMMA);
			
			double f28 = fPos[2];
			if(f28<fNeg[2])
				f28 = fNeg[2];
			writer.append(String.valueOf(f28) + Constant.SEPARATOR_COMMA);
			
			double f29 = f28;
			if(f29<fObj[2])
				f29=fObj[2];
			writer.append(String.valueOf(f29) + Constant.SEPARATOR_COMMA);
			
			//features related to average size of common hashtags
			double f30 = 0;
			if(interPos>0)
				f30 = fPos[3]/(double)interPos;
			writer.append(String.valueOf(f30) + Constant.SEPARATOR_COMMA);
			double f31 = 0;
			if(interNeg>0)
				f31 = fNeg[3]/(double)interNeg;
			writer.append(String.valueOf(f31) + Constant.SEPARATOR_COMMA);
			double f32 = 0;
			if(interObj>0)
				f32 = fObj[3]/(double)interObj;
			writer.append(String.valueOf(f32) + Constant.SEPARATOR_COMMA);
			double f33 = 0;
			if(interAll>0)
				f33 = fAll[3]/(double)interAll;
			writer.append(String.valueOf(f33) + Constant.SEPARATOR_COMMA);
			double f34 = 0;
			if((interPos+interNeg)>0)
				f34 = (fPos[3]+fNeg[3])/(double)(interPos+interNeg);
			writer.append(String.valueOf(f34) + Constant.SEPARATOR_COMMA);
			double f41 = 0;
			if((interPos+interNeg+interObj)>0)
				f41 = (fPos[3]+fNeg[3]+fObj[3])/(double)(interPos+interNeg+interObj);
			writer.append(String.valueOf(f41) + Constant.SEPARATOR_COMMA);
			
			//features related to inverse
			writer.append(String.valueOf(fPos[4]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[4]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[4])+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[4])+ Constant.SEPARATOR_COMMA);
			double f39 = fPos[4]+fNeg[4];
			writer.append(String.valueOf(f39)+ Constant.SEPARATOR_COMMA);
			double f40 = f39 + fObj[4];
			writer.append(String.valueOf(f40)+ Constant.SEPARATOR_COMMA);
			
			//features related to svo
			writer.append(String.valueOf(eclidean)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(cosine)+ Constant.SEPARATOR_COMMA);
			
			//features related to social structure
			writer.append(String.valueOf(CN)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(AA)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(JA)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(PA));
			
			writer.append("\n"); 
			writer.flush();
			
			if(numWritten % 100 == 0)
				System.out.println(numWritten + " records have been written!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
	}
	
	//write features for mutual-follow graph
	private void writeFollowFeatures(BufferedWriter writer, String source, String target, int label, int interPos, int interNeg, int interObj, int interAll, int interPN, int interNP,
			int unPos, int unNeg, int unObj,  int unAll,  ArrayList<Integer>userListPosSize, ArrayList<Integer>userListNegSize, 
			ArrayList<Integer>userListObjSize, ArrayList<Integer> userListAllSize, double eclidean, double cosine, 
			int CN, double AA, double JA, int PA, int numWritten)
	{
		try {
			writer.append(source+Constant.SEPARATOR_COMMA);
			writer.append(target+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(label)+Constant.SEPARATOR_COMMA);
			
			//features related to intersection
			writer.append(String.valueOf(interPos)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interNeg)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interObj)+ Constant.SEPARATOR_COMMA);
			double f4 = interPos + interNeg;
			writer.append(String.valueOf(f4)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interAll)+Constant.SEPARATOR_COMMA);
			double f5 = interPos + interNeg + interObj;
			writer.append(String.valueOf(f5)+Constant.SEPARATOR_COMMA);
			
			
			//features related to Jaccard
			double f7 = 0; 
			if(unPos>0)
				f7 = (double) interPos/unPos;
			writer.append(String.valueOf(f7) + Constant.SEPARATOR_COMMA);
			double f8 = 0;
			if(unNeg>0)
				f8 = (double) interNeg/unNeg;
			writer.append(String.valueOf(f8)+Constant.SEPARATOR_COMMA);
			double f9 = 0;
			if(unObj>0)
				f9 = (double) interObj/unObj;
			writer.append(String.valueOf(f9) + Constant.SEPARATOR_COMMA);
			double f43 = 0;
			if(unAll>0)
				f43=(double) interAll/unAll;
			writer.append(String.valueOf(f43) + Constant.SEPARATOR_COMMA);
			
			//features related to sentiment homophily coefficient
			double f10 = 0;
			if(interAll>0)
				f10 = (double) (interPos+interNeg)/interAll;
			writer.append(String.valueOf(f10) + Constant.SEPARATOR_COMMA);
			
			double f11 = 0;
			if(interAll>0)
				f11 = (double) (interPN + interNP)/interAll;
			writer.append(String.valueOf(f11) + Constant.SEPARATOR_COMMA);
			
			
			double[] fPos = new double[5];
			fPos = getFeatures(userListPosSize);
			double[] fNeg = new double[5];
			fNeg = getFeatures(userListNegSize);
			double[] fObj = new double[5];
			fObj = getFeatures(userListObjSize);
			double[] fAll = new double[5];
			fAll = getFeatures(userListAllSize);
			
			//features related to Adar
			writer.append(String.valueOf(fPos[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[0]) + Constant.SEPARATOR_COMMA);
			
			double f16 = fPos[0]+fNeg[0];
			writer.append(String.valueOf(f16) + Constant.SEPARATOR_COMMA);
			double f17 = fPos[0]+fNeg[0]+fObj[0];
			writer.append(String.valueOf(f17) + Constant.SEPARATOR_COMMA);
			
			
			//features related to smallest common hashtags
			writer.append(String.valueOf(fPos[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[1]) + Constant.SEPARATOR_COMMA);
			
			double f22 = fPos[1];
			if(f22>fNeg[1])
				f22=fNeg[1];
			writer.append(String.valueOf(f22) + Constant.SEPARATOR_COMMA);
			
			double f23 = f22;
			if(f23>fObj[1])
				f23=fObj[1];
			writer.append(String.valueOf(f23) + Constant.SEPARATOR_COMMA);
			
			//features related to largest common hashtags
			writer.append(String.valueOf(fPos[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[2]) + Constant.SEPARATOR_COMMA);
			
			double f28 = fPos[2];
			if(f28<fNeg[2])
				f28 = fNeg[2];
			writer.append(String.valueOf(f28) + Constant.SEPARATOR_COMMA);
			
			double f29 = f28;
			if(f29<fObj[2])
				f29=fObj[2];
			writer.append(String.valueOf(f29) + Constant.SEPARATOR_COMMA);
			
			//features related to average size of common hashtags
			double f30 = 0;
			if(interPos>0)
				f30 = fPos[3]/(double)interPos;
			writer.append(String.valueOf(f30) + Constant.SEPARATOR_COMMA);
			double f31 = 0;
			if(interNeg>0)
				f31 = fNeg[3]/(double)interNeg;
			writer.append(String.valueOf(f31) + Constant.SEPARATOR_COMMA);
			double f32 = 0;
			if(interObj>0)
				f32 = fObj[3]/(double)interObj;
			writer.append(String.valueOf(f32) + Constant.SEPARATOR_COMMA);
			double f33 = 0;
			if(interAll>0)
				f33 = fAll[3]/(double)interAll;
			writer.append(String.valueOf(f33) + Constant.SEPARATOR_COMMA);
			double f34 = 0;
			if((interPos+interNeg)>0)
				f34 = (fPos[3]+fNeg[3])/(double)(interPos+interNeg);
			writer.append(String.valueOf(f34) + Constant.SEPARATOR_COMMA);
			double f41 = 0;
			if((interPos+interNeg+interObj)>0)
				f41 = (fPos[3]+fNeg[3]+fObj[3])/(double)(interPos+interNeg+interObj);
			writer.append(String.valueOf(f41) + Constant.SEPARATOR_COMMA);
			
			//features related to inverse
			writer.append(String.valueOf(fPos[4]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fNeg[4]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fObj[4])+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fAll[4])+ Constant.SEPARATOR_COMMA);
			double f39 = fPos[4]+fNeg[4];
			writer.append(String.valueOf(f39)+ Constant.SEPARATOR_COMMA);
			double f40 = f39 + fObj[4];
			writer.append(String.valueOf(f40)+ Constant.SEPARATOR_COMMA);
			
			//features related to svo
			writer.append(String.valueOf(eclidean)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(cosine)+ Constant.SEPARATOR_COMMA);
			
			//features related to social structure
			writer.append(String.valueOf(CN)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(AA)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(JA)+ Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(PA));
			
			writer.append("\n"); 
			writer.flush();
			
			if(numWritten % 100 == 0)
				System.out.println(numWritten + " records have been written!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
	}
	
	private void writeFeatures1(BufferedWriter writer, int source, int target, int interAll, int interDiff, int interPos, int interNeg, 
			ArrayList<Integer>userListAllSize, ArrayList<Integer>userListDiffSize, int numWritten)
	{
		try {
			writer.append(String.valueOf(source)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(target)+Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(interAll)+ Constant.SEPARATOR_COMMA);
			//writer.append(String.valueOf(interNeg)+ Constant.SEPARATOR_COMMA);
			//writer.append(String.valueOf(interObj)+ Constant.SEPARATOR_COMMA);
			double f4 = 0; 
			if(interAll>0)
				f4 = (double) interDiff/interAll;
			writer.append(String.valueOf(f4) + Constant.SEPARATOR_COMMA);
			/*double f5 = 0;
			if(unNeg>0)
				f5 = (double) interNeg/unNeg;
			writer.append(String.valueOf(f5)+Constant.SEPARATOR_COMMA);
			double f6 = 0;
			if(unObj>0)
				f6 = (double) interObj/unObj;
			writer.append(String.valueOf(f6) + Constant.SEPARATOR_COMMA);*/
			
			double[] fAll = new double[5];
			fAll = getFeatures(userListAllSize);
			double[] fDiff = new double[5];
			fDiff = getFeatures(userListDiffSize);
			
			writer.append(String.valueOf(fAll[0]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fDiff[0]) + Constant.SEPARATOR_COMMA);
			
			writer.append(String.valueOf(fAll[1]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fDiff[1]) + Constant.SEPARATOR_COMMA);
			
			writer.append(String.valueOf(fAll[2]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fDiff[2]) + Constant.SEPARATOR_COMMA);
			
			double f16 = 0;
			if(interAll>0)
				f16 = fAll[3]/(double)interAll;
			writer.append(String.valueOf(f16) + Constant.SEPARATOR_COMMA);
			double f17 = 0;
			if((interPos+interNeg)>0)
				f17 = fDiff[3]/(double)(interPos+interNeg);
			writer.append(String.valueOf(f17) + Constant.SEPARATOR_COMMA);
			
			writer.append(String.valueOf(fAll[4]) + Constant.SEPARATOR_COMMA);
			writer.append(String.valueOf(fDiff[4]) + Constant.SEPARATOR_COMMA);
			
			writer.append("\n"); 
			writer.flush();
			
			if(numWritten % 1000 == 0)
				System.out.println(numWritten + " records have been written!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
	}
	
	private double[] getFeatures(ArrayList<Integer> list){
		double[] dist = new double[5];
		double value = 0;
		double adar = 0;
		double min = 0;
		double max = 0;
		double sum = 0;
		double inverse = 0;
		
		for(int i = 0; i < list.size(); i++){
			value = (double) list.get(i);
			adar+=1/Math.log(value);
			sum+= value;
			inverse+=1/value;
			
			if(i==0){
				min = value;
				max = value;
			}
			else{
				if(value < min)
					min = value;
				if(value > max)
					max = value;
			}
		}
		dist[0] = adar;
		dist[1] = min;
		dist[2] = max;
		dist[3] = sum;
		dist[4] = inverse;
		return dist;
	}
	
	//find out the user list who has talked about the hashtag with a particular sentiment
	private ArrayList<Integer> findUserList(String hashtag, int label){
		ArrayList<Integer> uList = new ArrayList<Integer>();
		Iterator it = userHashtagMap.entrySet().iterator();
		hashtagSet value = new hashtagSet();
		HashSet<String> tags = new HashSet<String>();
		while(it.hasNext()){
			Map.Entry<Integer, hashtagSet> pairs = (Entry<Integer, hashtagSet>) it.next();
			value = pairs.getValue();
			if(label==1){//positive and then negative
				tags = value.pos;
			}
			else if(label==2){//negative and then positive
				tags = value.neg;
			}
			else if(label==3){//objective
				tags = value.obj;
			}
			else{// all hashtags
				tags = new HashSet<String>(value.svo.keySet());
			}
			
			if(tags.contains(hashtag))
				uList.add(pairs.getKey());
		}
		return uList;
	}
	
	//find out the user list size who has talked about the hashtag with a particular sentiment
	private int findUserSize(String hashtag, int label){
		int size = 0;
		hashtagUserSize uSize = new hashtagUserSize();
		
		uSize = userSizeMap.get(hashtag);
		if(label==1){//positive and then negative
			size = uSize.posSize;
		}
		else if(label==2){//negative and then positive
			size = uSize.negSize;
		}
		else if(label==3){//objective
			size = uSize.objSize;
		}
		else{// all hashtags
			size = uSize.posSize + uSize.negSize + uSize.objSize;
		}
		
		return size;
	}
	
	public void createUserHashtagMap(){
		String inputFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\tweets_file_";
		String storeFile = "D:\\Social_Media_Analytics\\dataset\\sentiment\\userHashtag.csv";
		
		BufferedReader reader = null;
	
		int lineNumber = 0; //line number
		String line = "";
		int userId = 0;
		double score = 0;
		String sentiment = "";
		int error = 0;
		try {
			for(int fileName = 0; fileName < 6; fileName++)
			{
				System.out.println("reading file: " + fileName);
				reader = new BufferedReader(new FileReader(inputFile+String.valueOf(fileName)+"_sentiment.csv"));			
				while ((line = reader.readLine()) != null){
					lineNumber++;
					hashtagSet uHash = new hashtagSet();
					
					String words[] = line.split(Constant.SEPARATOR_COMMA);
					if(words.length != 11){
						//System.out.println("There is an error in line: " + lineNumber + " : " + line);
						error++;
						continue;
					}
					
					//skip the record that doesn't have a hashtag
					try{
						if(Integer.parseInt(words[6])==0)
							continue;
					} catch(Exception e){
						continue;
					}
					
					
					//process the record that has at least one hashtag
					userId = Integer.parseInt(words[5]);
					String[] topic = words[7].split(Constant.SEPARATOR_SEMICOLON);
					
					score = Double.parseDouble(words[8]) - Double.parseDouble(words[9]);
					
					//System.out.println("userId: " + userId + " pos: " + words[8] + " neg: " + words[9] + " score: " + score);
					//process uHash
					if(score>0){ //postive
						for(int i = 0; i<topic.length; i++){
							uHash.pos.add(topic[i].substring(1));
						}
					}
					else if(score<0){ //negative
						for(int i = 0; i<topic.length; i++){
							uHash.neg.add(topic[i].substring(1));
						}
					}
					else if(score==0){//objective
						for(int i = 0; i<topic.length; i++){
							uHash.obj.add(topic[i].substring(1));
						}
					}
					
					insertHashtagMap(userId, uHash);
					if(lineNumber % 10000 == 0)
						System.out.println(lineNumber + " records have been processed!");
				}
			}
			
			reader.close();
			System.out.println("lineNumber overall: " + lineNumber);
			System.out.println("error overall: " + error);
			printMap(userHashtagMap,storeFile);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//print a HashSet<String> into a file
	private void printSet(HashSet<String> edgeSet, String storeFile){
		BufferedWriter writer  = null;
		String edge = "";
	
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator<String> it = edgeSet.iterator();
			while(it.hasNext()){
				edge = it.next();
				String[] words = edge.split(Constant.SEPARATOR_HYPHEN);
				writer.append(words[0]+Constant.SEPARATOR_COMMA+words[1]);
				writer.append("\n");
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//print the hashmap to a file
	private void printMap(HashMap<Integer, hashtagSet> map, String storeFile){
		BufferedWriter writer  = null;
		int key = 0;
		hashtagSet value = new hashtagSet();
		int num = 0;
		try {
			writer = new BufferedWriter(new FileWriter(storeFile));
			Iterator it = map.entrySet().iterator();
			while(it.hasNext()){
				num++;
				Map.Entry<Integer, hashtagSet> pairs = (Map.Entry<Integer, hashtagSet>)it.next();
				key = pairs.getKey();
				value = pairs.getValue();
		
				writer.append(String.valueOf(key));//userId
				writer.append(Constant.SEPARATOR_COMMA);
				
				//write postive hashtags
				if(value.pos.size()>0){
					String[] pos = value.pos.toArray(new String[value.pos.size()]);
					writer.append(pos[0]);
					if(pos.length>1){
						for(int i = 1; i < pos.length; i++)
							writer.append(Constant.SEPARATOR_SEMICOLON+pos[i]);
					}
				}
				else{
					writer.append(" ");	
				}
				writer.append(Constant.SEPARATOR_COMMA);
				
				//write negative hashtags
				if(value.neg.size()>0){
					String[] neg = value.neg.toArray(new String[value.neg.size()]);
					
					writer.append(neg[0]);
					if(neg.length>1){
						for(int i = 1; i < neg.length; i++)
							writer.append(Constant.SEPARATOR_SEMICOLON+neg[i]);
					}
				}
				else{
					writer.append(" ");				
				}
				writer.append(Constant.SEPARATOR_COMMA);
				
				//write objective hashtags
				if(value.obj.size()>0){
					String[] obj = value.obj.toArray(new String[value.obj.size()]);
					
					writer.append(obj[0]);
					if(obj.length>1){
						for(int i = 1; i < obj.length; i++)
							writer.append(Constant.SEPARATOR_SEMICOLON+obj[i]);
					}
				}
				else{
					writer.append(" ");
				}
				
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

	private void insertHashtagMap(int userId, hashtagSet uHash) {
		if(!userHashtagMap.containsKey(userId))
			userHashtagMap.put(userId, uHash);
		else{
			hashtagSet original = userHashtagMap.get(userId);
			original.mergeSet(uHash);
			userHashtagMap.put(userId, original);
		}
	}

	public static void main(String[] args){
		UserHashtag uht = new UserHashtag();
		//uht.createUserHashtagMap();
		//uht.extractFeature();
		//uht.extractFollowFeature();
		//uht.sourceUserID();
		//uht.extractSocialFeature();
		//uht.splitFile();
		/*HashSet<String> s1 = new HashSet<String>();
		s1.add("apple");
		s1.add("banana");
		s1.add("bear");
		HashSet<String> s2 = new HashSet<String>();
		s2.add("apple");
		s2.add("orange");
		HashSet<String> s3 = new HashSet<String>();
		s3 = s1;
		s3.addAll(s2);
		System.out.println(s3);
		System.out.println(s1);*/
		//uht.socialTheory();
		//uht.socialTheoryTwo();
		//uht.socialTheoryThree();
		//uht.socialTheoryHashtag();
		//uht.labelHashtag();
		//uht.socialTheoryFollowHashtag();
		//uht.socialTheoryEntireGraph();
		uht.generateGraphTrainingFile(3, 17);
		//uht.generateFollowTrainingFile(3,7);
	}
	

}
