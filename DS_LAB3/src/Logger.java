import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.Scanner;

public class Logger implements Runnable {

	private ArrayList<TimeStampedMessage> list = new ArrayList<TimeStampedMessage>();
	private MessagePasser msgp;
	private static final Object LIST_LOCK = new Object();
	private HashMap<Integer, ArrayList<Integer>> printList = new HashMap<Integer, ArrayList<Integer>>();

	public static void main(String[] args) {

		Logger logger = new Logger();

		System.out.println(" ##### LOG #####");

		System.out.println("Please enter the configuration file name ");
		Scanner keyboard = new Scanner(System.in);
		String configName = keyboard.next();
		MessagePasser msgpasser;
		try {
			msgpasser = new MessagePasser(configName, "Logger");
		} catch (Exception e) {
			System.out.println("Configuration file error!");
			return;
		}
		logger.msgp = msgpasser;
		logger.msgp.createClockService();
		new Thread(logger).start();
		while (true) {
			System.out.println("Enter 1 to print the log");

			String isLog = keyboard.next();
			if (isLog.equals("1")) {
				synchronized (LIST_LOCK) {
					
				Collections.sort(logger.list);
				logger.printList();
				}
				
			}else{
				System.out.println("Incorrect input");
			}

		}
	}

	public int compare(VectorTimeStamp ts1, VectorTimeStamp ts2) {

		Boolean isLess = false;
		Boolean isGreater = false;
		for (int i = 0; i < ts1.getTime().length; i++) {
			if (ts1.getTime(i) < ts2.getTime(i))
				isLess = true;
			if (ts1.getTime(i) > ts2.getTime(i))
				isGreater = true;
		}
		if (isLess && !isGreater)
			return -1;
		if (isLess && isGreater)
			return 0;
		if (!isLess && isGreater)
			return 1;
		return -2;
	}

	public void printList() {
		// Sorting the list based on vector timestamps

		for (int i = 0; i < list.size(); i++) {
			ArrayList<Integer> tempList = new ArrayList<Integer>();
			for (int j = 0; j < list.size(); j++) {

				int t = compare((VectorTimeStamp) list.get(i).getStamp(),
						(VectorTimeStamp) list.get(j).getStamp());
				if (t == 0 && i != j) {
					tempList.add(j);
				}
			}
			System.out.print(i + "  " + list.get(i) + " is Concurrent with ");
			for (int l = 0; l < tempList.size(); l++) {
				System.out.print(tempList.get(l)+", ");
			}
			System.out.println(" ");
			
		}
				
		

	}

	public void run() {
		System.out.println("Inside run");
		while (true) {
			synchronized (LIST_LOCK) {
				TimeStampedMessage msg = msgp.receive();
				if (msg != null) {
					list.add(msg);

				}
			}
		}
	}

}
