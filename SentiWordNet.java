package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class SentiWordNet {
	private String pathToSWN = "SentiWordNet_3.0.0.txt";
	private HashMap<String, Score> dict;

	
	class Score {
		double pos;
		double neg;
		double obj;
		
		public Score(){
			pos = 0;
			neg = 0;
			obj = 1;
		}
		
		public Score(double p, double n, double o){
			pos = p;
			neg = n;
			obj = o;
		}
	}
	
	
	public void BuildWordNet() throws IOException{
		// This is our main dictionary representation
		dict = new HashMap<String, Score>();

		// From String to list of doubles.
		HashMap<String, HashMap<Integer, Score>> tempDictionary = new HashMap<String, HashMap<Integer, Score>>();

		BufferedReader csv = null;
		try {
			csv = new BufferedReader(new FileReader(pathToSWN));
			int lineNumber = 0;

			String line;
			while ((line = csv.readLine()) != null) {
				lineNumber++;
				
				// If it's a comment, skip this line.
				if(line.trim().startsWith("#"))
					continue;
				
				
				// We use tab separation
				String[] data = line.split("\t");
				String wordTypeMarker = data[0];
				
				//only work with adjectives
				if(!wordTypeMarker.equals("a"))
					continue;
				
				// Example line:
				// POS ID PosS NegS SynsetTerm#sensenumber Desc
				// a 00009618 0.5 0.25 spartan#4 austere#3 ascetical#2
				// ascetic#2 practicing great self-denial;...etc

				// Is it a valid line? Otherwise, through exception.
				if (data.length != 6) {
					System.out.println("Incorrect tabulation format in file, line: "
									+ lineNumber);
					continue;
				}

				Score sentiScore = new Score();
				// Calculate synset score as score = PosS - NegS
				sentiScore.pos = Double.parseDouble(data[2]);
				sentiScore.neg = Double.parseDouble(data[3]);
				sentiScore.obj = 1 - sentiScore.pos - sentiScore.neg;

				// Get all Synset terms
				String[] synTermsSplit = data[4].split(" ");

				// Go through all terms of current synset.
				for (String synTermSplit : synTermsSplit) {
					// Get synterm and synterm rank
					String[] synTermAndRank = synTermSplit.split("#");
					
					//stemmer the words
					String synTerm = stemmerWords(synTermAndRank[0]);

					int synTermRank = Integer.parseInt(synTermAndRank[1]);
					// What we get here is a map of the type:
					// term -> {score of synset#1, score of synset#2...}

					// Add map to term if it doesn't have one
					if (!tempDictionary.containsKey(synTerm)) {
						tempDictionary.put(synTerm, new HashMap<Integer, Score>());
					}

					// Add synset link to synterm
					tempDictionary.get(synTerm).put(synTermRank, sentiScore);
				}
				
			}

			// Go through all the terms.
			for (Map.Entry<String, HashMap<Integer, Score>> entry : tempDictionary.entrySet()) {
				String word = entry.getKey();
				Map<Integer, Score> synSetScoreMap = entry.getValue();

				// Calculate weighted average. Weigh the synsets according to
				// their rank.
				// Score= 1/2*first + 1/3*second + 1/4*third ..... etc.
				// Sum = 1/1 + 1/2 + 1/3 ...
				double pos = 0.0;
				double neg = 0.0;
				double obj = 0.0;
				double sumPos = 0.0;
				double sumNeg = 0.0;
				double sumObj = 0.0;
				
				for (Map.Entry<Integer, Score> setScore : synSetScoreMap.entrySet()) {
					pos += setScore.getValue().pos / (double) setScore.getKey();
					neg += setScore.getValue().neg / (double) setScore.getKey();
					obj += setScore.getValue().obj / (double) setScore.getKey();
					sumPos += 1.0 / (double) setScore.getKey();
					sumNeg += 1.0 / (double) setScore.getKey();
					sumObj += 1.0 / (double) setScore.getKey();
				}
				pos /= sumPos;
				neg /= sumNeg;
				obj /= sumObj;
				
				Score overallScore = new Score(pos, neg, obj);

				dict.put(word, overallScore);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (csv != null) {
				csv.close();
			}
		}		
	}
	
	public Score extract(String word)
	{
		Score s = new Score();
		if(dict.containsKey(word))
			s = dict.get(word);
		return s;
	}
	
	
	
	/**
	 * Porter Stemming Algorithm
	 * For example: "fishing", "fished", "fish", and "fisher" are reduced to the root word, "fish".
	*/	
	@SuppressWarnings("unchecked")
	public String stemmerWords(String word) {
		String tempWord = word;
		Stemmer s = new Stemmer();
		
		char[] cWord = word.toCharArray();
		s.add(cWord, word.length());
		s.stem();
		if(s.getResultLength() > 0) {
			tempWord = s.toString();
		}
			
		return tempWord;
	}
	
	public static void main(String[] args){
		SentiWordNet swn = new SentiWordNet();
		try {
			swn.BuildWordNet();
			String stem = swn.stemmerWords("more");
			
			Score s = swn.new Score();
			s = swn.extract(stem);
			System.out.println("depressed#a "+ "stem: " + stem);
			System.out.println("pos " +s.pos);
			System.out.println("neg " +s.neg);
			System.out.println("obj " +s.obj);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
