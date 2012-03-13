
public class LogicalTimeStamp extends Timestamp{
	private int time;

	public int getTime() {
		return time;
	}
  
	public void setTime(int time) {
		this.time = time;
	}
    public String toString(){
    	String out="";
    	out+=time;
    	return out;
    }
   
	public LogicalTimeStamp(int time) {
		this.time = time;
	}

	@Override
	public int compareTo(Object o) {
		LogicalTimeStamp ts=(LogicalTimeStamp)o;
		if (this.time<ts.time) return -1;
		else if (ts.time>this.time) return 1;
		else
			return 0;
	}

	@Override
	public Timestamp copy() {
		return null;
	}
}
