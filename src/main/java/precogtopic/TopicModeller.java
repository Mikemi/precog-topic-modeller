package precogtopic;

import static precogtopic.Utils.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

public class TopicModeller {
//	static String TESTFILE = "../tests/stest-result-mallet";
	static String TESTDIR =  "../tests/topic-test-200";
	private String OUTFILE = "../tests/out-TopicModeller-$.txt";
	private Boolean TOFILE = false;
	private Boolean VERBOSE = false;
	private String FILE_NAME_END = "-text.txt";
	
	TopicResult result = new TopicResult();
	
	private int DEFAULT_NTOPICS = 10;
	private int DEFAULT_NITERATIONS = 50;
	private String timeStamp = null;
	
	private Alphabet dataAlphabet;
	
	long startTime = System.currentTimeMillis();

	// ------------------------------------	
	static public void main(String[] args){

		TopicModeller topicModeller = new TopicModeller();
		topicModeller.getTopics(TESTDIR);
	}	
	// ------------------------------------
	
	public TopicModeller(){
		createOutFile();
		result.nIterations = DEFAULT_NITERATIONS;
		result.nTopics = DEFAULT_NTOPICS;
	}

	public TopicModeller(int nTopics, int nIterations){
		createOutFile();
		if(nTopics < 1)
			result.nTopics = DEFAULT_NTOPICS;
		else 
			result.nTopics = nTopics;
		if(nIterations < 1)
			result.nIterations = DEFAULT_NITERATIONS;
		else 
			result.nIterations = nIterations;
	}

	public TopicResult getTopics(String path){
		
		String malletFormatString = null;
		try {
			malletFormatString = createMalletFormatString(path);
		} catch (IOException e) {
			System.out.println("*** Something wrong with the input dir!");
			e.printStackTrace();
			return null;
		}
		if(malletFormatString == null) return null;
		Reader stringReader = new StringReader(malletFormatString);
		TopicResult ret = getTopics(stringReader);
		return ret;
	}
	
	public TopicResult getTopics(TopicResult input){
		
		StringBuilder sb = new StringBuilder();
		for(String docId:input.docIdDocTextMap.keySet()){
			String text = input.docIdDocTextMap.get(docId).replaceAll("\n", " ").replaceAll(" +"," ");
			sb.append(docId).append("\tTopics\t").append(text).append("\n");
		}
		Reader stringReader = new StringReader(sb.toString());
		return getTopics(stringReader);
	}
	
	public TopicResult getTopics(List<String> docIds, List<String> docTexts){
		if(docIds == null || docTexts == null || docIds.size() == 0 || docIds.size() != docTexts.size()){
			System.out.println("*** Topic modeler: invalid input args");
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0;i>docIds.size();i++){
			result.docIds.add(docIds.get(i));
			result.docIdDocTextMap.put(docIds.get(i), docTexts.get(i));
			sb.append(docIds.get(i)).append("\tTopics\t").append(docTexts.get(i).replace("\n"," ")).append("\n");
		}
		Reader stringReader = new StringReader(sb.toString());
		return getTopics(stringReader);
	}
	
	// ----------
	// THE ACTION
	public TopicResult getTopics(Reader reader){
        
		
		// Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        InstanceList instances = new InstanceList (new SerialPipes(pipeList));

        // Add the docs one by one
        
        CsvIterator csvIterator = new CsvIterator (reader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),3, 2, 1);	// data, label, name fields
        
//        try {
//			reader.reset();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        Integer i = 0;
//        while(csvIterator.hasNext()){
//        	i = i+1;
//        	Instance instance = csvIterator.next();
//        	String docId = instance.getName().toString();
//        	result.docIds.add(docId);
//        	String docText = instance.getData().toString();
//        	result.docIdDocTextMap.put(docId, docText);
//        	syso(i.toString() + ": " + docId + "\t" + docText.substring(0, (docText.length() > 120 ? 120 : docText.length())));
//        }
//        
//        try {
//			reader.reset();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//        csvIterator = new CsvIterator (reader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),3, 2, 1);	// data, label, name fields
        
        instances.addThruPipe(csvIterator); 

        // Create a model with nTopics topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.

        int numTopics = result.nTopics;
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(result.nIterations);
        try {
			model.estimate();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // ------ Model done 
        
        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
    
        for(int d = 0; d < model.getData().size(); d++){
        	String docId = result.docIds.get(d);
        	
	        FeatureSequence docTokens = (FeatureSequence) model.getData().get(d).instance.getData();
	        LabelSequence docTopics = model.getData().get(d).topicSequence;

	        // Here the word -> topic assignment is given for each doc
	        Formatter formatter = null;
	        /**	        
	        {
	        	if(VERBOSE) 
	        		syso("\n\t* Doc-" + new Integer(d).toString() + ": " + result.docIds.get(d) + ":");
		        out = new Formatter(new StringBuilder(), Locale.US);
		        StringBuilder sba = new StringBuilder("\n\t\t+Doc-a-");
		        StringBuilder sbb = new StringBuilder("\n\t\t+TopicIx-b-");
		        for (int position = 0; position < docTokens.getLength(); position++) {
		        	Object wordAtPosition = dataAlphabet.lookupObject(docTokens.getIndexAtPosition(position));
		        	Object topicIndexAtPosition = docTopics.getIndexAtPosition(position);
		        	out.format("%s-%d ", wordAtPosition, topicIndexAtPosition);        
		        }
		        if(VERBOSE)
		        	syso("\n* From Mallet formatter: " + result.docIds.get(d) + ":\n" + out.toString());
	        }
	        
	        if(VERBOSE)
	        	syso("----------------");
	        **/
	        
	        // Estimate the topic distribution of the first instance, 
	        //  given the current Gibbs state.
	        double[] docTopicDistribution = model.getTopicProbabilities(d);
		    
	        Boolean topicWordsFilled = false;
	        // Show the words in topics in order by the proportions for the document
	        for (int topic = 0; topic < numTopics; topic++) {
	            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
	            Map<String,List<String>> topicSortedWordList = new HashMap<>();
	            Map<String,List<Double>> topicSortedWeightList = new HashMap<>();
            	String topicNumAsString = new Integer(topic).toString();
                List<String> wordList = new ArrayList<>();
                List<Double> weightList = new ArrayList<>();
                Map<String,Double> topicToDistributionMap = new HashMap<>();
	            {
	            	if(VERBOSE) syso("\t* topic-" + topicNumAsString);
		            formatter = new Formatter(new StringBuilder(), Locale.US);
		            formatter.format("%d\t%.3f\t", topic, docTopicDistribution[topic]);
		            
		            topicToDistributionMap.put(topicNumAsString, docTopicDistribution[topic]);
		            
		            int rank = 0;
			        StringBuilder sba = new StringBuilder("\n\t\t+Topic-a-");
			        StringBuilder sbb = new StringBuilder("\n\t\t+TopicWeights-b-");
			        while (iterator.hasNext()){ //&& rank < 5) {
		                IDSorter idCountPair = iterator.next();
		                Object word = dataAlphabet.lookupObject(idCountPair.getID());
		                Double weight = idCountPair.getWeight();
		                formatter.format("%s (%.0f) ", word, weight);
		                if(!topicWordsFilled){
		                	Set<String> words = result.topicIdTopicWordsMap.get(topicNumAsString);
		                	if(words == null){
		                		words = new HashSet<>();
		                		result.topicIdTopicWordsMap.put(topicNumAsString, words);
		                	}
		                	words.add(word.toString());		                	
		                }
		                
		                wordList.add((String) word);
		                weightList.add(weight);		       
		                rank++;
		            }
		            if(VERBOSE) syso(formatter.toString());
	            }
	            topicWordsFilled = true;
	            topicSortedWordList.put(topicNumAsString, wordList);
	            result.docToTopicSortedWordList.put(docId, topicSortedWordList);
	            topicSortedWeightList.put(topicNumAsString, weightList);
	            result.docToTopicSortedWordWeightList.put(docId, topicSortedWeightList);
	            result.docToTopicDistr.put(docId, topicToDistributionMap);
	        }
        }
        
        /**
        // Create a new instance with high probability of topic 0
        StringBuilder topicZeroText = new StringBuilder();
        Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

        int rank = 0;
        while (iterator.hasNext() && rank < 5) {
            IDSorter idCountPair = iterator.next();
            topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
            rank++;
        }

        // Create a new instance named "test instance" with empty target and source fields.
        InstanceList testing = new InstanceList(instances.getPipe());
        testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));

        TopicInferencer inferencer = model.getInferencer();
        double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
        System.out.println("0\t" + testProbabilities[0]);
		**/
        
        Long processingTime = System.currentTimeMillis() - startTime;
        if(VERBOSE){
        	syso("\n --------------");
        	syso("* Processed " + result.docIds.size() + " documents, for " + result.nTopics + "topics (iterations: " + result.nIterations + ")");
        	syso("* Processing time was " + processingTime + " ms");
        }
        else {
        	System.out.println("\n --------------");
        	System.out.println("* Processed " + result.docIds.size() + " documents, for " + result.nTopics + "topics (iterations: " + result.nIterations + ")");
        	System.out.println("* Processing time was " + processingTime + " ms");
        }
        return result;
	}
		
	private void createOutFile(){
		// out file
		
		if(this.TOFILE){
			timeStamp = Utils.timeStampFormatted();
			String outFileName = OUTFILE.replace("$", timeStamp.toString());
			//outFile = Utils.createFileIfNotExists(outFileName);
			BufferedWriter out = null;
			try {
				out = new BufferedWriter(new OutputStreamWriter (
						new FileOutputStream(outFileName),"UTF-8"));
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				System.out.println("*** Can not create file: " + outFileName + " -- stop.");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		Utils.out = out;
		Utils.outCount = 0;
		Utils.VERBOSE = this.VERBOSE;
		Utils.outFlushLimit = -1;
		Utils.TOFILE = this.TOFILE;		
	}
	
	public String createMalletFormatString(String dirName) throws IOException{

		StringBuilder sb = new StringBuilder();

		File dir = new File(dirName);
		if(dir == null || !dir.exists() || !dir.isDirectory()){
			System.out.println("*** Something wrong with the dir " + dirName + " -- stop.");
			return null;
		}
		
		File[] files = dir.listFiles(); 
		for(File f: files){
			String docId = f.getName();
			if(!docId.endsWith(FILE_NAME_END)) continue;
			String text = readFileToString(f.getAbsolutePath());
			result.docIds.add(docId);
			result.docIdDocTextMap.put(docId, text);
			sb.append(f.getName()).append("\tTopics\t").append(text.replaceAll("\n", " ").replaceAll(" +"," ")).append("\n");
		}
			
		return sb.toString();
	}


}
