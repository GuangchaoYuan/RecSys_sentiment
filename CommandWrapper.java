package com.xerox.socialmedia.communityrecommendation.dbmanager;

import com.xerox.socialmedia.communityrecommendation.data.TweetContentExtractor;
import com.xerox.socialmedia.communityrecommendation.data.TweetTimeExtractor;
import com.xerox.socialmedia.communityrecommendation.data.TweetContentPreprocessor;
import com.xerox.socialmedia.communityrecommendation.data.UserPreprocessor;
import com.xerox.socialmedia.communityrecommendation.data.UserRelationProcessor;
import com.xerox.socialmedia.communityrecommendation.data.dbwriter.TweetTextDbWriter;

public class CommandWrapper {
	public static interface Command {
		public void setup();
	    public void execute();
	    public String getName();
	}
	
	/**
	 * tweet content extracting commander
	 * @author Lei
	 *
	 */
	public static class TweetContentExtractingCommand implements Command {
    	TweetContentExtractor extractor;
    	public void setup(){
    		extractor = new TweetContentExtractor();
    	}
    	public String getName(){
    		return "Tweet Content Extracting Command";
    	}
        public void execute() 
        {
            try {
				//extractor.run();
            	//extractor.preProcess();
            	extractor.writeSentiment();
            	//extractor.readFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }    
    }
	
	public static class TweetTimeExtractingCommand implements Command {
    	//TweetTimeExtractor extractor;
    	TweetTextDbWriter extractor;
    	public void setup(){
    		//extractor = new TweetTimeExtractor();
    		extractor = new TweetTextDbWriter();
    	}
    	public String getName(){
    		return "Tweet Time Extracting Command";
    	}
        public void execute() 
        {
            try {
				extractor.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }    
    }
	
	public static class TweetPreprocessingCommand implements Command {
    	TweetContentPreprocessor preprocessor;
    	public void setup(){
    		preprocessor = new TweetContentPreprocessor();
    	}
    	public String getName(){
    		return "Tweet Content Preprocessing Command";
    	}
        public void execute() 
        {
            try {
            	preprocessor.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }    
    }
	
	public static class UserPreprocessingCommand implements Command {
    	UserPreprocessor preprocessor;
    	public void setup(){
    		preprocessor = new UserPreprocessor();
    	}
    	public String getName(){
    		return "User Preprocessing Command";
    	}
        public void execute() 
        {
            try {
            	preprocessor.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }    
    }
	
	public static class UserRelationProcessingCommand implements Command {
    	UserRelationProcessor processor;
    	public void setup(){
    		processor = new UserRelationProcessor();
    	}
    	public String getName(){
    		return "User Relation Processing Command";
    	}
        public void execute() 
        {
            try {
            	processor.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }    
    }

}
