import java.util.Queue;

public class Action {
	private MessagePasser msgpass;
	private MessageManage msgmng;

	public Action(MessagePasser msgpass) {
		this.msgpass = msgpass;
	}

	public Action(MessageManage msgmng) {
		this.msgmng = msgmng;
	}

	public void drop(boolean isSend) {
		if (isSend) {
			System.out.println("message not sent ");
		} else {
			System.out.println("message not received ");
		}
	}

	public void duplicate(boolean isSend, TimeStampedMessage message) {
		if (isSend) {

			System.out.println("Sending message twice1");
			msgpass.getClock().increaseTime(message.getStamp());
			msgpass.sendMSG(message);
			msgpass.sendMSG(message);
			if (msgpass.isLoggingEnabled()) {
				message.setDest("Logger");
				msgpass.sendMSG(message);
			}			
		}

	}

	public void delay(boolean isSend, TimeStampedMessage message) {
		if (isSend) {
			System.out.println("message delayed");
			msgpass.getClock().increaseTime(message.getStamp());
			System.out.println("Message added to delay queue"+message);
			msgpass.getSenddelayqueue().offer(message);
		} else {
			
			msgmng.getRcvdelayqueue().offer(message);

		}

	}
}
