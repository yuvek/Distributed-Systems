import java.util.*;

public class ExclusiveService implements Runnable {

	private MultiCastService mcs;
	private String hostname;
	private Configuration config;
	private ArrayList<String> groupMembers = new ArrayList<String>();
	private static String state = "Released";
	private Queue<TimeStampedMessage> pendingBuf = new LinkedList<TimeStampedMessage>();
	private static final Object STATE_LOCK = new Object();
	private static final Object COUNT_LOCK = new Object();
	private Queue<TimeStampedMessage> recvbuf = new LinkedList<TimeStampedMessage>();
	private  int groupCount=0;
	private int ackcount=0;
	public ExclusiveService(MultiCastService mcs, String hostname) {
		super();
		this.mcs = mcs;
		this.hostname = hostname;
		config = mcs.getMsgp().getConfig();
	}

	public static void main(String[] args) {

		System.out.println(" ##### Exclusive Service #####");

		System.out.println("Please enter the configuration file name ");
		Scanner keyboard = new Scanner(System.in);
		String configName = keyboard.next();
		MultiCastService multi;
		MessagePasser msgpasser;
		System.out.println("Please Enter  the hostname");
		String hostname1 = keyboard.next();
		try {
			msgpasser = new MessagePasser(configName, hostname1);
			multi = new MultiCastService(msgpasser, hostname1);
		} catch (Exception e) {
			System.out.println("Please select correct node");
			// e.printStackTrace();
			return;
		}
		ExclusiveService exs = new ExclusiveService(multi, hostname1);
		new Thread(exs.mcs).start();
		new Thread(exs).start();
		exs.mcs.getMsgp().createClockService();
		exs.application();

	}
	public void trylock(){
		//mcs.multisend(msg, groupMembers);
		int count;
		do{
			synchronized(COUNT_LOCK){
				count=ackcount;
			}
		}while(count<groupCount-1);
		state="Held";
		System.out.println("Got the lock!");
	}
	private void update_group_yaml(){
		int pos = 0, i = 0;
		groupCount=0;
		this.mcs.getMsgp().update_yaml();
		this.groupMembers.clear();
		for (Details d : config.Configuration) {
			if (d.Name.equals(hostname))
				pos = i;
			i++;
		}
		//System.out.println("position " + pos);
		for (Iterator iterator = config.Configuration.get(pos).Group
				.iterator(); iterator.hasNext();) {
			String temp = (String) iterator.next();
			//System.out.println("host added " + temp);
			this.groupMembers.add(temp);
			groupCount++;
		}
	}
	private void application() {



		while (true) {

			update_group_yaml();
			System.out.println("Please select an Action:  ");
			System.out
			.println("1. Request Critical Section     2. Release Critical Section");
			int keyval;
			try {
				Scanner keyboard = new Scanner(System.in);
				keyval = keyboard.nextInt();

				if (keyval == 1) {

					Timestamp t = mcs.getMsgp().getClock().getTime();
					TimeStampedMessage msg = new TimeStampedMessage(hostname,
							null, "Mreq", null, t, true);
					if (state.equals("Released")) {
						synchronized (STATE_LOCK) {
							state = "Wanted";	
						}
						synchronized(COUNT_LOCK){
							ackcount++;
						}
						mcs.multisend(msg, groupMembers);
						//trylock();
					} else {
						System.out.println("Request added to pending buffer");
	                    //check duplicate here	
						pendingBuf.add(msg);
						mcs.multisend(msg, groupMembers);	
					}
				} else if (keyval == 2) {
					// receive
					if (state.equals("Held"))
					{
						TimeStampedMessage bufMSG=pendingBuf.peek();
						Timestamp t = mcs.getMsgp().getClock().getTime();
						TimeStampedMessage msg = new TimeStampedMessage(hostname,
								null, "Mrel", null, t, true);
						mcs.multisend(msg, groupMembers);
						
						if(pendingBuf.isEmpty()){
							synchronized(COUNT_LOCK){
								ackcount=0;
							}
							state="Released";
							System.out.println("Lock Released since pending buffer is Empty");
						}else{
							pendingBuf.poll();
							t = mcs.getMsgp().getClock().getTime();
							TimeStampedMessage replyMsg = new TimeStampedMessage(
									hostname, bufMSG.getSrc(), "Mack", null, t, false);
							this.mcs.unisend(replyMsg, bufMSG.getSrc());
							System.out.println("Voted for:"+bufMSG.getSrc());
							state="Voted";
						}
					}
					else
					{
						System.out.println("State is"+state+". No lock hold!");
					}


				}

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error Input");
				return;
			}

		}

	}



	public void run() {

		//System.out.println("Inside run");
		while (true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			TimeStampedMessage msg = this.mcs.receive();

			if (msg != null) {
				
				synchronized(STATE_LOCK){
					if (msg.getKind().equals("Mreq")) {
						//System.out.println("Mreq  received  " +msg);
						if (state.equals("Voted")||state.equals("Held")||state.equals("Wanted")) {
							System.out.println("Message added to pending buf "+msg.getSrc());
							Boolean dup=false;
							for (TimeStampedMessage tsm:pendingBuf){
								if (tsm.getStamp().equals(msg.getStamp()))
								{
									dup=true;
									break;
								}
							}
							if (!dup)
								pendingBuf.add(msg);
						} else if (state.equals("Released")) {
							
							Timestamp t = mcs.getMsgp().getClock().getTime();
							TimeStampedMessage replyMsg = new TimeStampedMessage(
									hostname, msg.getSrc(), "Mack", null, t, false);
							System.out.println("Send Mack to dest:"+msg.getSrc());
							this.mcs.unisend(replyMsg, msg.getSrc());
							System.out.println("Voted for:"+msg.getSrc());
							state="Voted";
						}
					}else if(msg.getKind().equals("Mrel")){
						TimeStampedMessage bufMSG=pendingBuf.peek();
						
						if(pendingBuf.isEmpty()){
							System.out.println("Lock Released since pending buffer empty");
							state="Released";
						}else{
							pendingBuf.poll();
							Timestamp t = mcs.getMsgp().getClock().getTime();
							TimeStampedMessage replyMsg = new TimeStampedMessage(
									hostname, bufMSG.getSrc(), "Mack", null, t, false);
							this.mcs.unisend(replyMsg, bufMSG.getSrc());
							System.out.println("Voted for:"+bufMSG.getSrc());
							state="Voted";
						
							
						}
					}else if (msg.getKind().equals("Mack")){
						synchronized(COUNT_LOCK){
							ackcount++;
							//System.out.println("ackcount:"+ackcount);
							if(ackcount==groupCount){
								state="Held";
								System.out.println("Got the lock!");
							}
						}
					}
				}

			}
		}

	}
}
