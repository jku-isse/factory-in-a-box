package fiab.mes.machine.actor.foldingstation.wrapper;

public interface FoldingStationWrapperInterface {

        public void fold(String shape, String orderId);

        public void stop();

        public void reset();

        void subscribeToStatus();


}
