package com.xerox.socialmedia.communityrecommendation.sentiment;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.WordStemmer;

import java.util.Properties;

public class Negation {
	
	private LexicalizedParser lp = LexicalizedParser.loadModel("englishPCFG.ser.gz");
	
	public HashSet<String> identifyNegation(String strLine){
		HashSet<String> negList = new HashSet<String>();
	    StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	    PTBTokenizer tkzr; // tokenizer object
	    WordStemmer ls = new WordStemmer(); // stemmer/lemmatizer object
	    
	    // do all the standard java over-complication to use the stanford parser tokenizer
    	sr = new StringReader(strLine);
    	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = (List) tkzr.tokenize();
    	
	    Tree parse = (Tree) lp.apply((java.util.List<? extends HasWord>) toks); // finally, we actually get to parse something
	    
		// Get dependency tree
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
 	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
 	    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
 	    Collection tdl = gs.typedDependenciesCollapsed();
 	    
 	   for(Iterator<TypedDependency> iter = tdl.iterator(); iter.hasNext();){
			TypedDependency var = iter.next();

			String reln = var.reln().getShortName();
			if(reln.equals("neg")){
				TreeGraphNode gov = var.gov();
				String[] m = gov.toString().split("-");
				negList.add(m[0]);
			}
		}
 	   
 	   return negList;
		
	}
	
	public static void main(String[] args) throws Exception {
		Negation ng = new Negation();
		HashSet<String> neg = ng.identifyNegation("Weather is not good today. I am not good.");
		if(neg.size()>0){
			for(Iterator iter = neg.iterator();iter.hasNext();)
				System.out.println(iter.next());
		}
		else
			System.out.println("There is no negation.");
	}

}
