
import java.io.Serializable;
import java.io.Serializable;
import java.util.Comparator;


public class TimeStampedMessage extends Message implements Serializable,Comparable{
	

	private Timestamp stamp;
	private boolean isMulticast;
	private int multicastId;
	
	public boolean isMulticast() {
		return isMulticast;
	}

	public Timestamp getStamp() {
		return stamp;
	}
   
	public TimeStampedMessage(String src, String dest, String kind, Object data,Timestamp stamp,boolean isMutly){
		super(src,dest,kind,data);
		this.stamp=stamp;
		this.isMulticast= isMutly;
		
	}
	public TimeStampedMessage copy(){
		TimeStampedMessage newtsm= new TimeStampedMessage(src,dest,kind,data,
				stamp.copy(),isMulticast);
		newtsm.setMulticastId(multicastId);
		return newtsm;
		
	}
	


	public int getMulticastId() {
		return multicastId;
	}

	public void setMulticastId(int multicastId) {
		this.multicastId = multicastId;
	}

	public static void main(String args[]){
		
	}
	
	public int compare(TimeStampedMessage m1, TimeStampedMessage m2) {
		
		return 0;
	}

	
	@Override
	public int compareTo(Object arg0) {
		TimeStampedMessage tsm=(TimeStampedMessage)arg0;
		return this.stamp.compareTo(tsm.stamp);
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeStampedMessage other = (TimeStampedMessage) obj;
		if (isMulticast != other.isMulticast)
			return false;
		if (multicastId != other.multicastId)
			return false;
		if (stamp == null) {
			if (other.stamp != null)
				return false;
		} else if (!stamp.equals(other.stamp))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TimeStampedMessage [src=" + src + ", dest=" + dest + ", kind="
				+ kind + ", id=" + id + ", data=" + data + ", stamp=" + stamp
				+ "multicastId= "+ multicastId+"]";
	}
}
