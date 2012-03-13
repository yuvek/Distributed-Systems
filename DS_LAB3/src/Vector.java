
public class Vector extends ClockService{
    private int index;
    private VectorTimeStamp ts;
	public Vector(int index, int length){
		this.index=index;
		this.ts=new VectorTimeStamp(length);
		System.out.println("Clock type selected is vector");
	}
	public void increaseTime(Timestamp msgts){
		time++;
		ts.setTime(index, time);
		((VectorTimeStamp)msgts).setTime(index, time);
	}
	public Timestamp getTime(){
		 //time++;
		 //ts.setTime(index, time);
	      return ts;
	}

	public void updateReceiveTime(Timestamp stamp){

		ts.setTime((VectorTimeStamp)stamp);
		time++;
		ts.setTime(index, time);
	}
	
}
