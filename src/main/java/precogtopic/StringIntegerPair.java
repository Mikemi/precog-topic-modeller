package precogtopic;

public class StringIntegerPair implements Comparable {
	public String s = null;
	public Integer i = null;
	
	public StringIntegerPair(String s,Integer i){
		this.s = s;
		this.i = i;
	}
	
	public StringIntegerPair(){
		
	}

	@Override
	public int compareTo(Object o) {
		if(!(o instanceof StringIntegerPair)){
			System.out.println(o.getClass().getName());
			System.exit(-1);
		}
		StringIntegerPair sip = (StringIntegerPair) o;
		if(this.i == sip.i) 
			return 0;
		else if (this.i < sip.i)
			return 1;
		else
			return -1;
	}
}
