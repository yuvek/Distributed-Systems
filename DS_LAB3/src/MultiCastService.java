import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MultiCastService implements Runnable {
	private ArrayList<TimeStampedMessage> rcvbuf = new ArrayList<TimeStampedMessage>();
	// private HashMap<Timestamp, MsgAck> rcvholdbuf = new HashMap<Timestamp,
	// MsgAck>();
	private LinkedHashMap<Identity, TimeStampedMessage> logBuf = new LinkedHashMap<Identity, TimeStampedMessage>();
	private HashMap<Integer, TimeStampedMessage> sendholdbuf = new HashMap<Integer, TimeStampedMessage>();
	private HashMap<String, Integer> expectedBuf = new HashMap<String, Integer>();
	private ArrayList<String> hosts = new ArrayList<String>();
	private ArrayList<TimeStampedMessage> tempBuffer = new ArrayList<TimeStampedMessage>();
	private MessagePasser msgp;
	private Configuration config;
	private String hostname;
	private static final Object LIST_LOCK = new Object();
	private static int multiSequence = 1;
	private HashMap<String, Integer> recCheckMap = new HashMap<String, Integer>();

	public MultiCastService(MessagePasser msgp, String hostname) {
		this.hostname = hostname;
		this.msgp = msgp;
		config = msgp.getConfig();
		initialseExpectBuf();
	}

	public void run() {
		// new thread to receive the message
		// set new timer for each receive packet(except ack/nack);
		// timeout will execute the run()function of MsgAck
		// if ack packet, cache in the MsgAck
		// if nack, drop it
		//System.out.println("Inside run");
		while (true) {
			TimeStampedMessage msg = msgp.receive();
			if (msg != null) {

				msg = this.checkRcvMSG(msg);
			}
			if (msg != null) {
				synchronized (LIST_LOCK) {
					//System.out.println("In mcs,msg from:"+msg.getSrc());
					rcvbuf.add(msg);
				}
			}
		}

	}
	public void unisend(TimeStampedMessage msg, String dest) {
		System.out.println("Sending unicast");
		msg.setSrc(hostname);
		msg.setDest(dest);
		msgp.send(msg);
		return;
	}

	public void multisend(TimeStampedMessage msg, ArrayList<String> host) {
		System.out.println("Sending multicast");

		for (Iterator iterator = host.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();

			msg.setSrc(hostname);
			msg.setDest(string);
			msg.setMulticastId(multiSequence);
			if(!string.equals(hostname))
			{
				System.out.println("Sending to " + string);
				msgp.send(msg);
			}

		}
		if (!msg.getKind().endsWith("NACK")) {
			Identity temp = new Identity(msg.getMulticastId(), msg.getSrc());
			//System.out.println(msg + " added to logbuf");
			if (!logBuf.containsKey(temp))
				logBuf.put(temp, msg.copy());
			// sendholdbuf.put(multiSequence, msg);
			multiSequence++;
		}
	}

	public void application() {
		this.initialseExpectBuf();
		while (true) {
			this.msgp.update_yaml();
			hosts.clear();
			for (Iterator iterator = config.MultiCast.iterator(); iterator
			.hasNext();) {
				hosts.add((String) iterator.next());
			}
			System.out.println("Please select an Action: 1. Send 2. Receive");
			int keyval;
			try {
				Scanner keyboard = new Scanner(System.in);
				keyval = keyboard.nextInt();

				if (keyval == 1) {


					System.out
					.println("Please enter a message to be sent(Format: kind Data)");
					String kind = keyboard.next();
					String data = keyboard.next();
					Timestamp t = msgp.getClock().getTime();
					TimeStampedMessage msg = new TimeStampedMessage(
							this.msgp.getHostname(), null, kind, data, t, true);
					multisend(msg, hosts);

				} else if (keyval == 2) {
					// receive

					TimeStampedMessage msg = this.receive();
					if (msg != null) {
						System.out.println("Multicast id = "
								+ msg.getMulticastId());
						System.out.println(msg.getDest()
								+ ": receive message from " + msg.getSrc()
								+ ":" + (String) msg.getData());
						System.out.println("kind:" + msg.getKind());
						System.out.println("TimeStamp:" + msg.getStamp());
						if (msg.isMulticast()) {
							System.out.println("CastType:  Multicast message");
						} else {
							System.out.println("CastType: Unicast message");
						}

					} else
						System.out.println("Receive buffer is empty!");

				}
				// choose a group
				// multisend a packet to the group,similar to the MessagePasser

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error Input");
				return;
			}

		}

	}

	public TimeStampedMessage receive() {
		TimeStampedMessage msg = null;
		// synchronized(MessageManage.class){
		synchronized (LIST_LOCK) {
			if (rcvbuf.size()>0)
			{
				for (int i = 0; i < rcvbuf.size(); i++) {
					TimeStampedMessage t = rcvbuf.get(i);
					for (int j = i + 1; j < rcvbuf.size(); j++) {
						TimeStampedMessage k = rcvbuf.get(j);
						if (t.getMulticastId() == k.getMulticastId()
								&& t.getSrc().equals(k.getSrc())) {
							rcvbuf.remove(j);
						}
					}

				}
				if (rcvbuf.size() != 0)
				{
					Collections.sort(rcvbuf);
//					for (Iterator iterator = rcvbuf.iterator(); iterator
//							.hasNext();) {
//						TimeStampedMessage type = (TimeStampedMessage) iterator.next();
//						System.out.println(" Recieve buffer element"+ type);	
//					}
					
					msg = rcvbuf.remove(0);
				}
			}

		}

		return msg;
	}

	public MessagePasser getMsgp() {
		return msgp;
	}

	public void initialseExpectBuf() {
		int pos=0,i=0;
		for (Details d : config.Configuration) {
			if (d.Name.equals(hostname))
				pos = i;
			i++;
		}
		System.out.println("position " + pos);
		for (Iterator iterator = config.MultiCast
				.iterator(); iterator.hasNext();) {
			String temp = (String) iterator.next();
			expectedBuf.put(temp, 1);

		}

		/*for (Iterator iterator = config.MultiCast.iterator(); iterator
				.hasNext();) {
			expectedBuf.put((String) iterator.next(), 1);
		}*/
	}
	public TimeStampedMessage checkRcvMSG(TimeStampedMessage msg) {

		// synchronized(MessageManage.class){


		if (msg.getKind().equals("NACK")) {
			System.out.println("Node received a NACK");
			Identity find = new Identity(msg.getMulticastId(),
					(String) msg.getData());
			TimeStampedMessage failedMsg = logBuf.get(find).copy();
			System.out.println("FailedMSG:" + failedMsg);
			if (failedMsg != null) {

				unisend(failedMsg, msg.getSrc());
			}

		} else {

			int expectedSequence = expectedBuf.get(msg.getSrc());
			Identity temp1 = new Identity(msg.getMulticastId(), msg.getSrc());

			if (msg.getMulticastId() <= expectedSequence) {
				//System.out.println(" message should be added to receivebuf");
				Identity temp = new Identity(msg.getMulticastId(), msg.getSrc());
				// logBuf.put(temp, msg);
				expectedBuf.put(msg.getSrc(),
						Math.max(msg.getMulticastId() + 1, expectedSequence));
				return msg;
			} else {
				System.out.println(" Node missed a message,expected:"
						+ expectedSequence + "now:" + msg.getMulticastId());
				for (int i = expectedSequence; i < msg.getMulticastId() + 1; i++) {

					String kind = "NACK";
					String data = msg.getSrc();
					Timestamp t = msgp.getClock().getTime();
					TimeStampedMessage nackMsg = new TimeStampedMessage(
							hostname, data, kind, data, t, true);
					nackMsg.setMulticastId(i);
					System.out.println("NACK ith:" + i);
					msgp.send(nackMsg);
				}
			}
		}
		return null;
	}

	public static void main(String[] args) {

		System.out.println(" ##### MultiCast Service #####");

		System.out.println("Please enter the configuration file name ");
		Scanner keyboard = new Scanner(System.in);
		String configName = keyboard.next();
		MessagePasser msgpasser;
		System.out.println("Please Enter  the hostname");
		String hostname1 = keyboard.next();
		try {
			msgpasser = new MessagePasser(configName, hostname1);
		} catch (Exception e) {
			System.out.println("Please select correct node");
			e.printStackTrace();
			return;
		}
		MultiCastService multicast = new MultiCastService(msgpasser, hostname1);
		new Thread(multicast).start();
		multicast.msgp.createClockService();
		multicast.application();

	}
}
