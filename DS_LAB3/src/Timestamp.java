import java.io.Serializable;

public abstract class Timestamp implements Serializable,Comparable{
	
public abstract int compareTo(Object o);
public abstract Timestamp copy();

}
