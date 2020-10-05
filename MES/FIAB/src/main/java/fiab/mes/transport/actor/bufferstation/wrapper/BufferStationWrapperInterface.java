package fiab.mes.transport.actor.bufferstation.wrapper;

public interface BufferStationWrapperInterface {

    public void load(String orderId);

    public void unload(String orderId);

    public void stop();

    public void reset();

    public void subscribeToStatus();

    public void unsubscribeFromStatus();
}
