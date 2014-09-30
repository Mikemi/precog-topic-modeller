package precogtopic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrecogTopicResult {
	
	public PrecogTopicResult(){
		
	}
	
	public PrecogTopicResult(Integer nTops, Integer nIter, List<String> tDocIds, Map<String,String> tDocIdDocTextMap){
		if(nTops > 0)
			 nTopics = nTops;
		if(nIter > 0)
			nIterations = nIter;
		docIds = tDocIds;
		docIdDocTextMap = tDocIdDocTextMap;
	}
	
	// The default topic and iteration numbers
	public static final int DEFAULT_NTOPICS = 50;
	public static final int DEFAULT_NITERATIONS = 100;
	
	// number of topics (if not given or <=0 is given, it is the default - see above) 
	// and iterations (recommended 1000, if not too slow. If not given or <=0 is given, it is the default - see above)	
	public Integer nTopics = DEFAULT_NTOPICS;
	public Integer nIterations = DEFAULT_NITERATIONS;
	
	// docId list (to identify)
	List<String> docIds = new ArrayList<>();
	
	// docId -> docText Map
	public Map<String,String> docIdDocTextMap = new HashMap<>();

	// size = nTopics
	// Map: topic (id) -> sorted words and their counts (sorted by the count decreasing)
	Map<String,List<StringIntegerPair>> topicsToSortedWordsAndCountsMap = new HashMap<>();
	
	// Map: sorted List of topics, sorted by combine word count
	List<StringIntegerPair> sortedTopicList = new ArrayList<>();
	
	// Map: doc (id) -> sorted list of topics (ids)
	// 		sorting is done by relevance decreasing
	Map<String,List<StringDoublePair>> docToSortedTopicsListMap = new HashMap<>();
	
//	// 	Map: doc (id) -> Map: topic (String id) -> doc related sorted list of topic words
//	// 		the words are sorted by the counts (decreasing) also included 
//	public Map<String,Map<String,List<StringIntegerPair>>> docToTopicToSortedWordCountList = new HashMap<>();
	
	public String toString(int l){
		StringBuilder sb = new StringBuilder("\n");
		sb.append("\n------------------");
		sb.append("\nPrecogTopicResult:");
		
		sb.append("\n* #Topics: ").append(nTopics).append("\n* #Iterations: ").append(nIterations);
		
		sb.append("\n* Input: ");
		for(String docId:docIds){
			String text = docIdDocTextMap.get(docId);
			sb.append("\n\t+ ").append(docId).append(":\t\t")
				.append(text.substring(0, text.length() < 100 ? text.length() : 100));
		}
		
		sb.append("\n* topicsToSortedWordsAndCountsMap (topics sorted by cumulative count): ");
		for(StringIntegerPair sip: sortedTopicList){
			String topicId = sip.s;
			Integer topicCount = sip.i;
			sb.append("\n\t+ Topic-").append(topicId).append("[").append(topicCount).append("]:\t\t");
			List<StringIntegerPair> topicWords = topicsToSortedWordsAndCountsMap.get(topicId);
			int ll = 0;
			for(StringIntegerPair sip1: topicWords){
				if(l>0 && ll >= l) break;
				sb.append(sip1.s).append("(").append(sip1.i).append(") ");
				ll++;
			}
		}
		
		sb.append("\n* docToTopicDistr: ");
		for(String docId: docIds){
			sb.append("\n\t+ ").append(docId).append(":\t\t");
			List<StringDoublePair> topicDistrList = docToSortedTopicsListMap.get(docId);
			if(topicDistrList == null){
				sb.append("topicDistrList == null");
				continue;
			}
			int ll = 0;
			for(StringDoublePair sdp:topicDistrList){
				if(l > 0 && ll >= l)
					break;
				Double d = sdp.d;
				String topicId = sdp.s;
				if(d != null)
				sb.append("Topic-").append(topicId).append("(").append(Utils.round2(d)).append(") ");
				ll++;
			}
		}
		
//		sb.append("\n* docToTopicToSortedWordCountList: ");
//		for(String docId: docIds){
//			sb.append("\n\t+ ").append(docId).append(":\t\t");
//			Map<String,List<StringIntegerPair>> TopicToSortedWordCountList = docToTopicToSortedWordCountList.get(docId);
//			if(TopicToSortedWordCountList == null){
//				sb.append("no topics -> word-list");
//				break;
//			}
//			for(Integer topic = 0; topic < nTopics; topic = topic+1){
//				String topicAsString = topic.toString();				
//				sb.append("\n\t\tTopic-").append(topicAsString).append(": ");
//				List<StringIntegerPair> list = TopicToSortedWordCountList.get(topicAsString);
//				int ll = 0;
//				if(list != null){
//					for(StringIntegerPair sip: list){
//						if(l > 0 && ll >= l)
//							break;
//						sb.append(sip.s).append("(").append(sip.i).append(") ");
//						ll++;
//					}
//				}
//			}
//			sb.append("\n");
//		}
		return sb.toString();
	}	
}
