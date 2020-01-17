package fiab.mes.machine.actor.iostation.wrapper;

public interface IOStationWrapperInterface {

	public void stop();
	
	public void reset();

	void subscribeToStatus();
	
	void subscribeToLoadStatus();
}