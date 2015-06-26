package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

//The dictionary is built from Akshat's work

public class Emoticon {
	private String pathToSWN = "emoticonsWithPolarity.txt";
	private HashMap<String, Integer> dict;
	
	
	public void buildEmoticonDict() throws IOException{
		// This is our main dictionary representation
		dict = new HashMap<String, Integer>();
		BufferedReader csv = null;
		
		try {
			csv = new BufferedReader(new FileReader(pathToSWN));
			int lineNumber = 0;

			String line;
			while ((line = csv.readLine()) != null) {
				lineNumber++;
				
				// We use space or tab separation
				String[] data = line.split("\\s+");
				if(data.length==0)
					continue;
				
				int size = data.length;
				String type = data[size-1];
				int label = type2Label(type);
				
				for(int i = 0; i < (size-1); i++){
					dict.put(data[i], label);
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (csv != null) {
				csv.close();
			}
		}	
	}
	
	private int type2Label(String s){
		int label = 0;
		
		if(s.equals("Extremely-Positive"))
			label = 1;
		else if(s.equals("Positive"))
			label = 2;
		else if(s.equals("Neutral"))
			label = 3;
		else if(s.equals("Negative"))
			label = 4;
		else if(s.endsWith("Extremely-Negative"))
			label = 5;
		else 
			label = 6;  //the emoticon does not exist in the dictionary
		
		return label;
	}
	
	public int extractLabel(String s){
		int label = 6;
		if(dict.containsKey(s))
			label = dict.get(s);
		return label;
	}
	
	public double extractPos(int label){
		double pos = 0;
		if(label == 1)
			pos = 1.0;
		else if(label == 2)
			pos = 0.75;
		
		return pos;
	}
	
	public double extractNeg(int label){
		double neg = 0;
		if(label == 5)
			neg = 1.0;
		else if(label == 4)
			neg = 0.75;
		
		return neg;
	}
	
	public double extractObj(int label){
		double obj = 0;
		if(label == 3 || label == 6)
			obj = 1.0;
		
		return obj;
	}
	
	public static void main(String[] args){
		Emoticon en = new Emoticon();
		try {
			en.buildEmoticonDict();
			int label = en.extractLabel("^.~");
			
			System.out.println("pos " + en.extractPos(label));
			System.out.println("neg " + en.extractNeg(label));
			System.out.println("obj " + en.extractObj(label));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
