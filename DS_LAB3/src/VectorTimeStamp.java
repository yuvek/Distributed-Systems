public class VectorTimeStamp extends Timestamp {
	private int[] time_vec;

	public int[] getTime() {
		return time_vec;
	}

	public int getTime(int index) {
		if (index >= time_vec.length)
			return -1;
		return time_vec[index];
	}

	public String toString() {
		String out = "(";
		for (int i = 0; i < time_vec.length - 1; i++)
			out += time_vec[i] + ",";
		out += time_vec[time_vec.length - 1] + ")";
		return out;
	}

	public void setTime(VectorTimeStamp ts) {
		if (time_vec.length != ts.time_vec.length)
			return;
		for (int i = 0; i < time_vec.length; i++)
		
		 time_vec[i] = Math.max(time_vec[i], ts.time_vec[i]);
	}

	public void setTime(int index, int val) {
		if (index >= time_vec.length)
			return;
		time_vec[index] = val;
	}

	public VectorTimeStamp(int length) {
		time_vec = new int[length];
	}
	public boolean equals(Object o){
		VectorTimeStamp ts=(VectorTimeStamp)o;
		boolean eq=true;
		for (int i = 0; i < time_vec.length; i++)
		{
			if (time_vec[i]!=ts.time_vec[i])
			{
				eq=false;
				break;
			}
		}
		return eq;
	}
	@Override
	public int compareTo(Object o) {
		VectorTimeStamp ts=(VectorTimeStamp)o;
		Boolean isLess = false;
		Boolean isGreater = false;
		for (int i=0;i<ts.time_vec.length;i++)
		{
			if (time_vec[i]<ts.time_vec[i]) isLess=true;
			if (time_vec[i]>ts.time_vec[i]) isGreater=true;
		}
		if (isLess && !isGreater) return -1;
		if (isLess && isGreater) return 0;
		if (!isLess && isGreater) return 1;
		return 0;
	}

	@Override
	public Timestamp copy() {
		VectorTimeStamp ts= new VectorTimeStamp(time_vec.length);
		for (int i = 0; i < time_vec.length; i++)
			
			 ts.time_vec[i] = time_vec[i];
		return ts;
	}

}
