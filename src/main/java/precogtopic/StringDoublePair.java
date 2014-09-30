package precogtopic;

public class StringDoublePair implements Comparable	{
	public String s = null;
	public Double d = null;
	
	public StringDoublePair(String s,double d){
		this.s = s;
		this.d = d;
	}
	
	public StringDoublePair(){
		
	}	

	@Override
	public int compareTo(Object o) {
		if(!(o instanceof StringDoublePair)){
			System.out.println(o.getClass().getName());
			System.exit(-1);
		}
		StringDoublePair sdp = (StringDoublePair) o;
		if(this.d == sdp.d) 
			return 0;
		else if (this.d < sdp.d)
			return 1;
		else
			return -1;
	}

}
