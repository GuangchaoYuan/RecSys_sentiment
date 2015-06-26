package com.xerox.socialmedia.communityrecommendation.sentiment;

import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.util.*;
import edu.umass.cs.mallet.grmm.inference.Inferencer;
import edu.umass.cs.mallet.grmm.inference.TRP;
import edu.umass.cs.mallet.grmm.learning.*;

import edu.umass.cs.mallet.grmm.learning.templates.SimilarTokensTemplate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CRF {
	 public static String TrainingFile = "D:\\Social_Media_Analytics\\MRF\\conll2000.train1k.txt";
	 public static String TestingFile = "D:\\Social_Media_Analytics\\MRF\\conll2000.test1k.txt";
	 public static String EvaluationResultFile = "D:\\Social_Media_Analytics\\MRF\\EVAL_RESULT.txt";
	 
	 public static void main(String[] args) throws Exception {
	        
	        /***********************************
	         * Training/Testing File Processor
	         ***********************************/
	        
	        //the token ---- must separate labels from features.
	        //for parsing ACRF Training/Testing file format
	        
	        //convert ACRF Training/Testing file format into TokenSequence Format
	        GenericAcrfData2TokenSequence basePipe = new GenericAcrfData2TokenSequence ();
	        basePipe.setFeaturesIncludeToken(false);
	        basePipe.setIncludeTokenText(false);
	        
	        /**
	        convert TokenSequence format into FeatureVectorSequence
	        FeatureVectorSequence is a collection of FeatureVector for an instance
	        for example:
	        FeatureVector 1: word.NN.2@4 , word.VB.1@-1 , ....
	        FeatureVector 2: word.JJ.2@4 , word.VB.2@-1 , ....
	        and FeatureVectorSequence = {FeatureVector 1, FeatureVector 2, ...}
	        **/ 
	        TokenSequence2FeatureVectorSequence secondPipe = new TokenSequence2FeatureVectorSequence (true, true);
	        
	        //we will use these aforementioned Pipes to process Training/Testing Text File.
	        Pipe pipe = new SerialPipes (new Pipe[] {basePipe, secondPipe});
	        
	        /**
		LineGroupIterator : for detecting line group. each line group is separated by BLANK LINE
	        example:
		aaa |
		aaa | => This part is one Line Group 
		aaa |
		
		dd |
		ff | => This part is another Line Group
		**/
	        PipeInputIterator trainSource = new LineGroupIterator (new FileReader (TrainingFile), Pattern.compile ("^\\s*$"), true);
	        PipeInputIterator testSource = new LineGroupIterator (new FileReader (TestingFile), Pattern.compile ("^\\s*$"), true);
	        
	        /**
	         Convert Training/Testing File Text Format into InstanceList representation
	         InstanceList is MALLET's standard representation for Training/Testing data
	         **/
	        InstanceList training = new InstanceList (pipe);
	        training.add (trainSource);
	        InstanceList testing = new InstanceList (pipe);
	        testing.add (testSource);
	        
	        
	        /************************************************************
	         * Defining our Dynamic CRF model ! via predefined templates
	         ************************************************************/
	        
	        ACRF.Template[] tmpls = {new ACRF.BigramTemplate (0), //factor 0: for Part-of-Speech
	                                 new ACRF.BigramTemplate (1), //factor 1: for NP Chunking (B, I, O)
	                                 new ACRF.PairwiseFactorTemplate (0,1)}; //connect factor 0 and factor 1 (2-Dimensional CRF)
	        
	        /**
	         POS : A - A - A - A   factor 0
	               |   |   |   |   connection between factor 0 and 1
	         NP  : B - B - B - B   factor 1
	               |   |   |   |
	               X1  X2  X3  X4  observation sequence
	         **/
	        
	        
	        /***************
	         * Our Inferencer 
	         ***************/
	        Inferencer inf = new TRP();
	        Inferencer maxInf = TRP.createForMaxProduct();
	        
	        
	        /******************
	         * MODEL CONTAINER
	         ******************/
	        //This is our MODEL CONTAINER, later this class will be serialized after training
	        ACRF acrf = new ACRF (pipe, tmpls);
	        acrf.setInferencer (inf);
	        acrf.setViterbiInferencer (maxInf);
	        
	        
	        /*******************
	         * Training start !
	         *******************/
	        ACRFTrainer trainer = new ACRFTrainer ();
	        trainer.train(acrf, training, 9999);
	        
	        
	        //until this line, acrf has been trained. We can serialize acrf anytime for future use so that
	        //we don't need to train the model again in future use.
	        //serialize acrf into gzip file
	        FileUtils.writeGzippedObject (new File ("acrf.ser.gz"), acrf);
	        
	        
	        /*******************************************************
	         * Evaluation start !
	         * The result will be store in the file EVAL_RESULT.txt
	         *******************************************************/
	        
	        trainer.test(acrf, testing, new ACRFTrainer.FileEvaluator(new File(EvaluationResultFile)));
	    }

}
