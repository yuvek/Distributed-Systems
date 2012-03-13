
public abstract class ClockService {
static protected int time=0;

abstract public Timestamp getTime();

abstract public void increaseTime(Timestamp ts);

abstract public void updateReceiveTime(Timestamp stamp);

}
