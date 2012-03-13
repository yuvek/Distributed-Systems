import java.io.Serializable;


public class Message implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String src;
	protected String dest;
	protected String kind;
	protected int id=0;
	protected Object data;

	public Message(String src, String dest, String kind, Object data){
		this.src=src;
		this.dest=dest;
		this.kind=kind;
		this.data=data;
		
	}
	
	public void setDest(String dest) {
		this.dest = dest;
	}
    public void setSrc(String src){
    	this.src = src;
    }
	public String getSrc() {
		return src;
	}
	public void set_id(int id){
		this.id=id;
	}
	public String getDest() {
		return dest;
	}	

	public String getKind() {
		return kind;
	}

	public  int getId() {
		return id;
	}

	public Object getData() {
		return data;
	}

	public static void main(String args[]){
		
	}
}
