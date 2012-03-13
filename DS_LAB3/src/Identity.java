public class Identity {
	public String src;
	public int msgId;

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public int getMsgId() {
		return msgId;
	}

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public Identity(int msgID, String src) {
		this.src = src;
		this.msgId = msgID;
	}

	@Override
	public String toString() {
		return "Identity [src=" + src + ", msgId=" + msgId + "]";
	}

	public Identity() {
		super();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + msgId;
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		Identity temp = (Identity) obj;
		if (this.msgId == temp.msgId && this.src.equals(temp.src))
			return true;
		return false;
	}

	

}
