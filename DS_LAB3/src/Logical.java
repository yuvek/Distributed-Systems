public class Logical extends ClockService {
      
	public Logical(){
		System.out.println(" Clock type selected is Logical");
	}
	public void increaseTime(Timestamp ts){
		this.time=time+1;
		((LogicalTimeStamp)ts).setTime(time);
	}
	public Timestamp getTime(){
		//this.time=time+1;
		Timestamp stamp= new LogicalTimeStamp(time);
		
	      return stamp;
	}
	public void updateReceiveTime(Timestamp stamp){

		this.time=Math.max(time, ((LogicalTimeStamp)stamp).getTime())+1;
	}
	
}
