/*
 *  Assignment: Distributed System Lab0
 *  Author : Tao Lin (taolin@andrew.cmu.edu)
 *           Yuvek Lokesh Mehta (ymehta@andrew.cmu.edu)
 *  Description: This class supports the main thread to send the object in the Distributed Network,
 *               and also we create another thread to listen port, and use multi-thread to receive the
 *               message in the buffer once message is delivered.
 *               
 * */
import java.io.*;
import java.util.*;
import java.net.*;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class MessagePasser implements Runnable {
	private FileInputStream fis = null;
	private Configuration config;

	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

	private String configname = null;
	private long yamlmodifiedtime = 0;
	private String hostname = null;
	private int hostindex;
	private HashMap<String, Details> namemap = new HashMap<String, Details>();
	private HashMap<String, Details> ipmap = new HashMap<String, Details>();
	private static ClockService clock;
	private ArrayList<String> hostlist = new ArrayList<String>();
	private boolean isLoggingEnabled;
	private ArrayList<Identity> timelist = new ArrayList<Identity>();
	private Queue<TimeStampedMessage> recvbuf = new LinkedList<TimeStampedMessage>();

	private Queue<TimeStampedMessage> senddelayqueue = new LinkedList<TimeStampedMessage>();
	private static int id;

	private Queue<TimeStampedMessage> rcvdelayqueue = new LinkedList<TimeStampedMessage>();

	public String getHostname() {
		return hostname;
	}

	// this function used to update yaml file at runtime, and should be
	// synchronized with
	// checkrule function
	public synchronized void update_yaml() {
		File file = new File(configname);
		if (file.lastModified() == yamlmodifiedtime) {
			return;
		}

		try {
			System.out.println("configuration file being parsed");
			yamlmodifiedtime = file.lastModified();
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.println("File not open");
		}
		Yaml yaml = new Yaml(new Constructor(Configuration.class));

		config = (Configuration) yaml.load(fis);

		try {
			fis.close();
		} catch (IOException e) {
			System.out.println("Configure file close failed");
		}
	}

	public MessagePasser(String configuration_filename, String local_name)
	throws Exception {
		this.configname = configuration_filename;
		// import the yaml file into the data structure
		update_yaml();
		hostname = local_name;
		// initialize the contact information of network node and
		namemap.clear();
		ipmap.clear();
		System.out.println("The nodes in the network are:");
		for (Details contact : config.Configuration) {
			hostlist.add(contact.Name);
			System.out.println(contact.Name);
			namemap.put(contact.Name, contact);
			ipmap.put(contact.IP, contact);

		}
		// sort hostlist and record hostindex for hostname
		Collections.sort(hostlist);
		hostindex = hostlist.indexOf(hostname);
		if (hostindex < 0)
			throw new Exception();
		// run the listen port thread for receiving messages
		new Thread(this).start();

	}

	public Queue<TimeStampedMessage> getSenddelayqueue() {
		return senddelayqueue;
	}

	// This function validates the message checks if it matches any rule
	// and send the message according to the rule
	public void send(TimeStampedMessage message) {

		if (!checkMSGValid(message)) {
			System.out.println("Invalid Message to send");
			return;
		}

		message.set_id(id);
		id++;

		String action;

		if ((action = checkSendingRules(message)) != null) {
			// action
			Action performAction = new Action(this);
			if (action.equals("drop")) {
				performAction.drop(true);
			} else if (action.equals("duplicate")) {
				performAction.duplicate(true, message);
				chcksendDelayQueue();
			} else if (action.equals("delay")) {
				performAction.delay(true, message);
			}

		} else {
			Identity temp= new Identity(message.getMulticastId(),message.getSrc());

			if (!message.isMulticast() || !timelist.contains(temp)) {
				
				this.clock.increaseTime(message.getStamp());
				timelist.add(temp);
			}
            //System.out.println("Before send at MessagePasser"+message);
			sendMSG(message);
			if (isLoggingEnabled) {
				message.setDest("Logger");
				sendMSG(message);
			}
			chcksendDelayQueue();
		}
		return;
	}



	public boolean isLoggingEnabled() {
		return isLoggingEnabled;
	}

	public static ClockService getClock() {
		return clock;
	}

	// checks if delay queue has any delayed messages buffered, send them all if
	// they exist
	public void chcksendDelayQueue() {
		while (!senddelayqueue.isEmpty()) {
			TimeStampedMessage msg = senddelayqueue.poll();
			 System.out.println("message from delay queue"+msg);
			sendMSG(msg);
			if (isLoggingEnabled()) {
				msg.setDest("Logger");
				sendMSG(msg);
			}
		}
	}

	// send the message through socket
	public void sendMSG(TimeStampedMessage message) {
		Socket socket;
		Details dest = namemap.get(message.getDest());
		try {

			socket = new Socket(dest.IP, dest.Port);

			ObjectOutputStream oos;

			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(message);
			oos.flush();
			oos.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Sending from " + hostname + " to " + dest.Name
					+ " failed due to I/O error");
		}
	}

	// creates object of class to receive messages and makes a call to its
	// function
	public TimeStampedMessage receive() {

		return (new MessageManage(recvbuf, config)).getRCVMsg();
	}

	@Override
	// Accept socket connections and spawn a new thread to listen at the port
	public void run() {

		ServerSocket ss;
		Socket socket;
		try {

			ss = new ServerSocket(namemap.get(hostname).Port);
			while (true) {
				socket = ss.accept();
				// identify which IP comes from
				String srcIP = socket.getInetAddress().toString();
				srcIP = srcIP.substring(1);
				if (ipmap.containsKey(srcIP)) {
					Details src = ipmap.get(srcIP);
					MessageManage msgmg = new MessageManage(recvbuf, src.Name,
							hostname, src, socket, rcvdelayqueue, config, this);
					// create a new thread to process the transmission of that
					new Thread(msgmg).start();
				}

			}

		} catch (IOException e) {
			System.out.println("The listen socket of " + hostname
					+ " is not connected!");
		}

	}

	public Queue<TimeStampedMessage> getRcvdelayqueue() {
		return rcvdelayqueue;
	}

	public Queue<TimeStampedMessage> getRecvbuf() {
		return recvbuf;
	}

	public HashMap<String, Details> getIpmap() {
		return ipmap;
	}

	// check the message hostname is valid
	public boolean checkMSGValid(TimeStampedMessage message) {
		if (message.getSrc().equals(hostname)
				&& namemap.containsKey(message.getDest()))
			return true;
		return false;
	}

	// function of checking the sending rules returns the action of the rule
	// which matched
	public synchronized String checkSendingRules(TimeStampedMessage message) {

		for (Iterator<Rules> iterator = this.config.SendRules.iterator(); iterator
		.hasNext();) {
			Rules type = (Rules) iterator.next();

			if (type.Src == null || message.getSrc().equals(type.Src)) {

				if (type.Dest == null || message.getDest().equals(type.Dest)) {
					if (type.Kind == null
							|| message.getKind().equals(type.Kind)) {
						if (type.ID == -1 || message.getId() == type.ID) {

							if ((message.getId() + 1) % type.Nth == 0)

								return type.Action;

						}
					}
				}
			}
		}

		return null;
	}

	// function of checking receiving rules returns the action of the rule that
	// matched
	public synchronized String checkReceivingRules(TimeStampedMessage message) {

		for (Iterator<Rules> iterator = this.config.ReceiveRules.iterator(); iterator
		.hasNext();) {
			Rules type = (Rules) iterator.next();

			if (type.Src == null || message.getSrc().equals(type.Src)) {

				if (type.Dest == null || message.getDest().equals(type.Dest)) {
					if (type.Kind == null
							|| message.getKind().equals(type.Kind)) {
						if (type.ID == -1 || message.getId() == type.ID) {
							if (type.MultiID==-1 || message.getMulticastId()==type.MultiID){
								if ((message.getId() + 1) % type.Nth == 0)

									return type.Action;
							}
						}
					}
				}
			}
		}

		return null;
	}

	public void createClockService() {
		String clockName = config.ClockType;
		if (clockName.equals("Logical")) {
			clock = new Logical();
		} else if (clockName.equals("Vector")) {
			clock = new Vector(hostindex, hostlist.size());

		}
	}

	public static void main(String args[]) {
		System.out.println("Please enter the configuration file name ");
		Scanner keyboard = new Scanner(System.in);
		String configName = keyboard.next();
		MessagePasser msgpasser;
		String hostname;
		// Boolean flag = true;

		System.out.println("Please Enter  the hostname");
		hostname = keyboard.next();
		try {
			msgpasser = new MessagePasser(configName, hostname);
		} catch (Exception e) {
			// flag = false;
			System.out.println("Please select correct node");
			e.printStackTrace();
			return;
		}

		ArrayList<Details> receivers = new ArrayList<Details>();
		for (Map.Entry<String, Details> entry : msgpasser.getNamemap()
				.entrySet()) {
			receivers.add(entry.getValue());
		}
		msgpasser.createClockService();
		msgpasser.application();

	}

	public boolean application() {

		while (true) {
			// update the yaml file before sending or receiving action
			this.update_yaml();
			System.out.println("Please select an Action: 1. Send 2. Receive");
			int keyval;
			try {
				Scanner keyboard = new Scanner(System.in);
				keyval = keyboard.nextInt();

				if (keyval == 1) {
					// send

					System.out.println("Please choose a node:");
					int i = 1;
					for (String s : this.hostlist) {

						System.out.print(i + ". " + s + " ");
						i++;
					}
					int sendindex = keyboard.nextInt();
					Details receiver = this.namemap.get(this.hostlist
							.get(sendindex - 1));
					System.out.println("Do you want to log the message ? Y/N");
					String isLogger = keyboard.next();
					if (isLogger.equals("Y")) {
						this.isLoggingEnabled = true;
					} else {
						this.isLoggingEnabled = false;
					}
					System.out
					.println("Please enter a message to be sent(Format: kind Data)");
					String kind = keyboard.next();
					String data = keyboard.next();
					Timestamp t = clock.getTime();
					TimeStampedMessage msg = new TimeStampedMessage(
							this.getHostname(), receiver.Name, kind, data, t,
							false);

					this.send(msg);
				}

				if (keyval == 2) {
					// receive

					TimeStampedMessage msg = this.receive();
					if (msg != null) {
						System.out.println("Message id = " + msg.getId());
						System.out.println(msg.getDest()
								+ ": receive message from " + msg.getSrc()
								+ ":" + (String) msg.getData());
						System.out.println("kind:" + msg.getKind());
						System.out.println("TimeStamp:" + msg.getStamp());
					} else
						System.out.println("Receive buffer is empty!");
				}
			} catch (Exception e) {
				System.out.println("Error Input");
				return false;
			}
		}
	}

	public HashMap<String, Details> getNamemap() {
		return namemap;
	}

}
