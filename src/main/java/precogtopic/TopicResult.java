package precogtopic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopicResult {
	// number of topics (if not given or <=0 is given, the default is 100) 
	// and iterations (recommended 1000, if not too slow. If not given or <=0 is given, the default is 50)	
	public Integer nTopics = 0;
	public Integer nIterations = 0;
	
	// docId list (to identify)
	List<String> docIds = new ArrayList<>();
	
	// docId -> docText Map
	public Map<String,String> docIdDocTextMap = new HashMap<>();

	// topicId (Integer) -> set of topicWords (unsorted)
	// size = nTopics
	public Map<String,Set<String>> topicIdTopicWordsMap = new HashMap<>();
	
	// For each doc (id) the map assigns a  probability (weight) to all topics (id)
	// The higher the better
	public Map<String,Map<String,Double>> docToTopicDistr = new HashMap<>();
	
	// For each doc (id) and topic (id) the list contains the words of the topic sorted by relevance re the document 
	public Map<String,Map<String,List<String>>> docToTopicSortedWordList = new HashMap<>();
	
	// For the above sorted topic word list (relative to a document) this gives the weights the words were sorted upon
	public Map<String,Map<String,List<Double>>> docToTopicSortedWordWeightList = new HashMap<>();
}
