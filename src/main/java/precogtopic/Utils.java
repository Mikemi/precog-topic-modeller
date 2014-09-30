package precogtopic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {
	static public boolean VERBOSE = false;
	static public boolean TOFILE = false;
	static public BufferedWriter out = null;
	static public int outCount = 0;
	static public int outFlushLimit = 1;					// how frequently flush the buffer content
	static public boolean LOG = false;	
	
	/**
	 * Text preparation for classification/training
	 * Common between the pipeline modules and the apcategory-cli
	 */
	static public String prepareText(String docText, Set<String> stopwordSet, Map<String, String> brownWordClusterMap) {
		/**
		 * Compile the text for the classifiers (see comment at the call of 
		 * this method in APCategoryExtractorPR):
		 *  - tokenize
		 *  - omit some tokens, including stopwords
		 *  - use brown clusters (converted to decimal) instead of the words
		 * TODO: if whether Brown cluster or the word itself is used 
		 * 		 depends on the label, make it label dependent!
		 */
    	String[] tokens = docText.toLowerCase().split("[\\s]+|[\\p{P}]+");
    	List<String> words = Arrays.asList(tokens);

    	StringBuilder sb = new StringBuilder();
    	for(String word:words){
    		if(stopwordSet.contains(word)) continue;
    		if(word.length() < 3) continue;
    		if(startsWithNumeric(word)) continue;
			String cluster = brownWordClusterMap.get(word); 
			if(cluster != null){
    			Integer iCluster = Integer.parseInt(cluster, 2);
    			sb.append(iCluster.toString()).append(" ");
			}
		}
		
		String text = sb.toString();
		return text;
	}
    	
	static public boolean startsWithNumeric(String str) {
		return str.matches("^\\d+\\w*");  //match a number with optional '-' and decimal.
	}

	/**
	 * Utilities
	 * =========
	 */
	
    static public void syso(String s){

    	if(VERBOSE){
			if(!LOG) System.out.println(s);
			if(TOFILE){
				if(out != null){ //("*** no out-file is opened!");
					try {
						out.write(s + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(outFlushLimit < 0  || ++outCount >= outFlushLimit){
						outCount = 0;
						outFlush();
					}
				}
			}				
    	}
	}	
	
	static public void syso(){
		syso("");
	}
	
	static public void syso(Boolean forceVerbose,String s){
		if(forceVerbose){
			boolean log0 = LOG;
			LOG = true;
			syso(s);
			LOG = log0;
		}
	}

	private static void outFlush(){
		if(out != null){
			try {
				out.flush();
			} catch (IOException e) {
				System.out.println("*** AP: Could not flush 'out'");
				e.printStackTrace();
			}
		}
	}

    /**
     * Just round to 2 digits
     */
    static public Double round2(double d){
    	return (double)Math.round(d * 100) / 100;
    }    

    /**
     * Just round to 4 digits
     */
    static public Double round3(double d){
    	return (double)Math.round(d * 1000) / 1000;
    }    

    /**
     * Just round to 2 digits, convert to string and replace '.' with ','
     */
    static public String round2Comma(double d){
    	return round2(d).toString().replace('.', ',');
    }    

	static public File createDirIfNotExists(String dirName){
		File theDir = new File(dirName);

		  // if the directory does not exist, create it
		  if (!theDir.exists()) {
		    System.out.println("* Creating directory: " + dirName);

		    try{
		        theDir.mkdir();
		    } 
		    catch(SecurityException se){
		    	System.out.println("*** can not create dir, stop!");
		    	System.exit(-1);
		     }
		  }
		  return theDir;
	}
	
	static public File createFileIfNotExists(String fileName){
		File theFile = new File(fileName);

		  // if the directory does not exist, create it
		  if (!theFile.exists()) {
		    System.out.println("* Creating file: " + fileName);

		    try{
		        theFile.createNewFile();
		    } 
		    catch(IOException se){
		    	System.out.println("*** can not create dir, stop!");
		    	System.exit(-1);
		     }
		  }
		  return theFile;
	}

	static public String timeStampFormatted(){
		Long msecs = System.currentTimeMillis();
		
//		Timestamp stamp = new Timestamp(System.currentTimeMillis());
//		Date date = new Date(stamp.getTime());;
//		System.out.println("Date Object:" + date);
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm_a");
//		System.out.println("Formatted Date:" + sdf2.format(date));
		
		String timeStamp = msecs.toString();
		return timeStamp;
	}
	
	static public Boolean dirExists(String dirName){
		File d = new File(dirName);
		if(d.exists() && d.isDirectory()) return true;
		else return false;
	}
	
	static public String readFileToString(String path) {
		FileInputStream in;
		StringBuilder sb = new StringBuilder();
		BufferedReader r = null;
		try {
			in = new FileInputStream(new File(path));
			InputStreamReader isr = new InputStreamReader(in, "UTF-8");
			r = new BufferedReader(isr);
			
			String line = null;
			while ((line = r.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} 
		catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
		}
		finally {
			if (r != null){
				try {
					r.close();
				}
				catch (IOException e) {
					System.err.println("Caught IOException: " + e.getMessage());
				}
			}
		}
		return sb.toString();
	}	
	
	// (From an xml string) return all non empty segments between consecutive start and end pairs
	
	public static List<String> getBetween(String text, String start, String end) {
		List<String> returnList = new ArrayList<String>();
		int startPos = 0;
		int endPos = 0;
		while(true){
			startPos = text.indexOf(start,startPos);
			if (startPos == -1) break;
			endPos = text.indexOf(end, startPos+start.length());
			if (endPos == -1) break;
			String s = text.substring(startPos + start.length(), endPos).trim();
			if(s.length() > 0) 
				returnList.add(s);
			startPos = endPos + end.length();
		}
		return returnList;
	}
	


}
