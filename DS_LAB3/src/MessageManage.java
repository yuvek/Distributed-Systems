/*
 *  Assignment: Distributed System Lab0
 *  Author : Tao Lin (taolin@andrew.cmu.edu)
 *           Yuvek Lokesh Mehta (ymehta@andrew.cmu.edu)
 *  Description: This class supports the multi-thread receiving messages. Each messages would
 *               be dealt with one thread
 *               
 * */
import java.io.*;
import java.net.Socket;
import java.util.*;

public class MessageManage implements Runnable {
	private Queue<TimeStampedMessage> rcvbuf;
	private Queue<TimeStampedMessage> rcvdelayqueue;
	private String senderhostname;
	private String receiverhostname;
	private Details sender;
	private Socket socket;
	private static final Object QUEUE_LOCK = new Object();
	private Configuration config;
	private MessagePasser msgpasser;

	// public MessageManage(Queue<Message> recvbuf){this.rcvbuf=recvbuf;}
	public MessageManage(Queue<TimeStampedMessage> recvbuf, Configuration config) {
		this.rcvbuf = recvbuf;
		this.config = config;
	}
	public MessageManage(Queue<TimeStampedMessage> recvbuf, String senderhostname,
			String receiverhostname, Details sender, Socket socket,
			Queue<TimeStampedMessage> rcvdelayqueue,Configuration config,MessagePasser msgpasser) {
		this.rcvbuf = recvbuf;
		this.receiverhostname = receiverhostname;
		this.senderhostname = senderhostname;
		this.sender = sender;
		this.socket = socket;
		this.rcvdelayqueue=rcvdelayqueue;
		this.config=config;
		this.msgpasser=msgpasser;
		
	}
	public Queue<TimeStampedMessage> getRcvdelayqueue() {
		return rcvdelayqueue;
	}

	

	
	@Override
	// Obtain the object with socket, check with the rules, perform the action
	// push Message into the queue if possible
	public void run() {

		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(socket.getInputStream());

			TimeStampedMessage recv = (TimeStampedMessage) ois.readObject();
			ois.close();
			

			// check with the rules, perform the action
			// push Message into the queue
			String action;
			if ((action=msgpasser.checkReceivingRules(recv)) != null) {
				
				Action performAction = new Action(this);
				
				if (action.equals("drop")) {
					performAction.drop(false);
				} else if (action.equals("duplicate")) {
					synchronized (QUEUE_LOCK) {
						rcvbuf.offer(recv);
						rcvbuf.offer(recv);
						MessagePasser.getClock().updateReceiveTime(recv.getStamp());
						chckdelayQueue();
					}

				} else if (action.equals("delay")) {
					performAction.delay(false, recv);
				}

			} else {

				synchronized (QUEUE_LOCK) {
					
					rcvbuf.offer(recv);
					if(MessagePasser.getClock()!=null)
					MessagePasser.getClock().updateReceiveTime(recv.getStamp());
					chckdelayQueue();
				}
			}
			
			socket.close();

		} catch (IOException e) {
			System.out.println("Connection from " + senderhostname + " to "
					+ receiverhostname + "is failed");
		} catch (ClassNotFoundException e) {
			System.out.println("Object parser encountered errors while reading object");
		}
	}

	public Queue<TimeStampedMessage> getRcvbuf() {
		return rcvbuf;
	}
    public void chckdelayQueue(){
    	while(!rcvdelayqueue.isEmpty()) {
    		TimeStampedMessage msg = rcvdelayqueue.poll();
    		MessagePasser.getClock().updateReceiveTime(msg.getStamp());
			rcvbuf.offer(msg); 
			
		}
    }
	public void setRcvbuf(Queue<TimeStampedMessage> rcvbuf) {
		this.rcvbuf = rcvbuf;
	}

	public TimeStampedMessage getRCVMsg() {
		TimeStampedMessage msg;
		// synchronized(MessageManage.class){
		synchronized (QUEUE_LOCK) {
			msg = rcvbuf.poll();
			
		}
		return msg;
	}


}
