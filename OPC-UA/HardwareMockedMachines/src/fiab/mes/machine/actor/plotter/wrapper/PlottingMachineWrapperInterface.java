package fiab.mes.machine.actor.plotter.wrapper;

public interface PlottingMachineWrapperInterface {

	public void plot(String imageId, String orderId);
	
	public void stop();
	
	public void reset();

	void subscribeToStatus();
	
}