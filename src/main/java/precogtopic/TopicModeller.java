package precogtopic;

import static precogtopic.Utils.*;
import static precogtopic.PrecogTopicResult.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

public class TopicModeller {

	private Boolean TOFILE = true;
	private Boolean VERBOSE = true;
	private Boolean TRACE = false;
	
	private int PRINT_LIST_LIMIT = 5;

	static String STOPLISTFILE = "stoplists/en.txt";
	
//	static String TESTFILE = "../tests/stest-result-mallet";
//	static String TESTDIR =  "../tests/topic-test-200";
//	static String TESTDIR =  "../tests/topic-test-1";
//	static String TESTDIR =  "../tests/topic-test-10";
//	static String TESTDIR =  "../tests/topic-test-20";
	static String TESTDIR =  "../tests/topic-test-100";
	private String OUTFILE = "../tests/out-TopicModeller-$.txt";
	private String FILE_NAME_END = "-text.txt";
	
	private String stopListFilePath = null;
	
	private Integer nTops = 0;
	private Integer nIters = 0; 
	private List<String> tDocIds = null;
	private Map<String,String> tDocIdDocTextMap = null;
	
	private String timeStamp = null;
	
	long startTime = System.currentTimeMillis();

	// ------------------------------------	
	static public void main(String[] args){

		TopicModeller topicModeller = new TopicModeller(50,100);
//		topicModeller.getTopics(STOPLISTFILE, TESTDIR);
		topicModeller.getTopics1(STOPLISTFILE, TESTDIR);
	}	
	// ------------------------------------
	
	public TopicModeller(){
		createOutFile();
		nTops = PrecogTopicResult.DEFAULT_NTOPICS;
		nIters = PrecogTopicResult.DEFAULT_NITERATIONS;
	}

	public TopicModeller(int nTopics, int nIterations){
		createOutFile();
		
		if(nTopics > 0)
			nTops = nTopics;
		else 
			nTops = PrecogTopicResult.DEFAULT_NTOPICS;
		
		if(nIterations > 0)
			nIters = nIterations;
		else
			nIters = PrecogTopicResult.DEFAULT_NITERATIONS;

	}
	
	// ------------------------------------

	// call for a test dir
	public PrecogTopicResult getTopics(String stopListFilePath, String path){
		this.stopListFilePath = stopListFilePath;
		tDocIds = new ArrayList<>();
		tDocIdDocTextMap = new HashMap<>();
		
		String malletFormatString = null;
		try {
			Boolean fillresult = true;
			malletFormatString = createMalletFormatString(path,fillresult);
		} catch (IOException e) {
			System.out.println("*** Something wrong with the input dir!");
			e.printStackTrace();
			return null;
		}
		if(malletFormatString == null) return null;
		Reader stringReader = new StringReader(malletFormatString);
		PrecogTopicResult ret = getTopics(stringReader);
		return ret;
	}
	
	// to test the call with lists
	public PrecogTopicResult getTopics1(String stopListFilePath, String path){
		
		String malletFormatString = null;
		try {
			Boolean fillresult = false;
			malletFormatString = createMalletFormatString(path,fillresult);
		} catch (IOException e) {
			System.out.println("*** Something wrong with the input dir!");
			e.printStackTrace();
			return null;
		}
		if(malletFormatString == null) return null;
		String[] ss = malletFormatString.split("\n");
		List<String> docIds = new ArrayList<>(); 
		List<String> docTexts = new ArrayList<>();
		for(String line:ss){
			String[] parts = line.split("\t");
			String docId = parts[0];
			String docText = parts[2];
			docIds.add(docId);
			docTexts.add(docText);
		}
		
		PrecogTopicResult ret = getTopics(docIds,docTexts,stopListFilePath);
		return ret;
	}
	
	// Recommended for the topicservice
	public PrecogTopicResult getTopics(List<String> docIds, List<String> docTexts, String stopListFilePath){
		this.stopListFilePath = stopListFilePath;
		tDocIds = new ArrayList<>();
		tDocIdDocTextMap = new HashMap<>();		

		if(docIds == null || docTexts == null || docIds.size() == 0 || docIds.size() != docTexts.size()){
			System.out.println("*** Topic modeller: invalid input args");
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < docIds.size(); i++){
			tDocIds.add(docIds.get(i));
			tDocIdDocTextMap.put(docIds.get(i), docTexts.get(i));
			sb.append(docIds.get(i)).append("\tTopics\t").append(docTexts.get(i).replaceAll("\n"," ").replaceAll("\t"," ").replaceAll(" +"," ")).append("\n");
		}
		Reader stringReader = new StringReader(sb.toString());
		PrecogTopicResult result = getTopics(stringReader);
		return result;
	}
	
	// ------------------------------------
	// THE ACTION
	public PrecogTopicResult getTopics(Reader reader){
        
		startTime = System.currentTimeMillis();

		// This will be the return value
		PrecogTopicResult result = new PrecogTopicResult(nTops, nIters, tDocIds, tDocIdDocTextMap);
		
		// Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File(stopListFilePath), 
        		"UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );

        InstanceList instances = new InstanceList (new SerialPipes(pipeList));

        // Add the docs one by one        
        CsvIterator csvIterator = new CsvIterator (reader, 
        		Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),3, 2, 1);	// data, label, name fields
        
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

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();

        // ----------
        // Get an array of topics - with the sorted sets of word-count pairs
        
        // This is the Mallet version:
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        
        // convert it to PrecogTopicResult version
        for(int i=0; i<topicSortedWords.size(); i++){
        	List<StringIntegerPair> sortedWordList = new ArrayList<>();
        	for(IDSorter ids:topicSortedWords.get(i)){
        		String word = (String) dataAlphabet.lookupObject(ids.getID());
        		Integer count = new Integer((int)ids.getWeight());
        		sortedWordList.add(new StringIntegerPair(word,count));
        	}
        	result.topicsToSortedWordsAndCountsMap.put(new Integer(i).toString(), sortedWordList);
        }
        
        // Get the sorted topic list
        
        for(int i=0; i<nTops; i++){
        	String topicId = new Integer(i).toString();
        	List<StringIntegerPair> sortedWordList = result.topicsToSortedWordsAndCountsMap.get(topicId);
        	Integer cumulativeCount = 0;
        	for(StringIntegerPair sip : sortedWordList){
        		cumulativeCount = cumulativeCount + sip.i;
        	}
        	result.sortedTopicList.add(new StringIntegerPair(new Integer(i).toString(),cumulativeCount));
        }
        Collections.sort(result.sortedTopicList);
    
        // For each document
        for(int d = 0; d < model.getData().size(); d++){
        	String docId = result.docIds.get(d);
        	
        	// The mallet version ???
//	        FeatureSequence docTokens = (FeatureSequence) model.getData().get(d).instance.getData();
//	        LabelSequence docTopics = model.getData().get(d).topicSequence;

	        // -------------
        	// topic distribution
	        List<StringDoublePair> topicListWithProb = new ArrayList<>();
	        
	        // Estimate the topic distribution of the doc d, 
	        //  given the current Gibbs state.
	        double[] docTopicDistribution = model.getTopicProbabilities(d);
	        for(int i=0; i<nTops; i++){
	        	String topicId = new Integer(i).toString();
	        	Double prob = docTopicDistribution[i];
	        	topicListWithProb.add(new StringDoublePair(topicId,prob));	        	
	        }
	        // sort and add to TopicResult
	        Collections.sort(topicListWithProb);
	        result.docToSortedTopicsListMap.put(docId, topicListWithProb);
		    
	        // ------------
	        // Show the words in topics sorted by the contributions to the document
//	        for (int topic = 0; topic < numTopics; topic++) {
//            	String topicNumAsString = new Integer(topic).toString();
//
//            	Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();		// nem fugg a doc-tol?
//	            Map<String,List<StringIntegerPair>> topicSortedWordList = new HashMap<>();
//
//                if(TRACE) syso("\t* topic-" + topicNumAsString);
//            	
//                Formatter formatter = new Formatter(new StringBuilder(), Locale.US);
//	            formatter.format("%d\t%.3f\t", topic, docTopicDistribution[topic]);
//	            
//	            List<StringIntegerPair> wordList = new ArrayList<>();
//	            int rank = 0;
//		        while (iterator.hasNext()){ //&& rank < 5) {
//	                IDSorter idCountPair = iterator.next();
//	                String word = (String) dataAlphabet.lookupObject(idCountPair.getID());
//	                Integer count = new Double(idCountPair.getWeight()).intValue();
//	                formatter.format("%s (%.0f) ", word, (double) count);
//	                
//	                wordList.add(new StringIntegerPair(word,count));
//	                rank++;
//	            }
//	            if(TRACE) syso(formatter.toString());
//		            
//	            topicSortedWordList.put(topicNumAsString, wordList);
//	            result.docToTopicToSortedWordCountList.put(docId, topicSortedWordList);
//	        }
        }
        
        if(VERBOSE)
        	syso(result.toString(PRINT_LIST_LIMIT));
        
        Long processingTime = System.currentTimeMillis() - startTime;
        if(VERBOSE){
        	syso("\n --------------");
        	syso("* Processed " + result.docIds.size() + " documents, for " + result.nTopics + " topics (iterations: " 
        			+ result.nIterations + ")");
        	syso("* Processing time was " + processingTime + " ms");
        }
        else {
        	System.out.println("\n --------------");
        	System.out.println("* Processed " + result.docIds.size() + " documents, for " + result.nTopics 
        			+ " topics (iterations: " + result.nIterations + ")");
        	System.out.println("* Processing time was " + processingTime + " ms");
        }
        return result;
	}
		
	private void createOutFile(){
		// out file
		BufferedWriter out = null;
		
		if(this.TOFILE){
			timeStamp = Utils.timeStampFormatted();
			String outFileName = OUTFILE.replace("$", timeStamp.toString());
			//outFile = Utils.createFileIfNotExists(outFileName);
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
	
	public String createMalletFormatString(String dirName, Boolean fillResult) throws IOException{

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
			if(fillResult){
				tDocIds.add(docId);
				tDocIdDocTextMap.put(docId, text);
			}
			sb.append(f.getName()).append("\tTopics\t").append(text.replaceAll("\n", " ").replaceAll("\t"," ").replaceAll(" +"," "))
					.append("\n");
		}
			
		return sb.toString();
	}
}
