package fiab.mes.shopfloor;

import java.time.Duration;

public class GlobalTransitionDelaySingelton {

	public static Duration PLOTTER_EXECUTE2COMPLETING = Duration.ofSeconds(5);
	
	private static Duration _PLOTTER_EXECUTE2COMPLETING = PLOTTER_EXECUTE2COMPLETING;

	public static void reset() {
		_PLOTTER_EXECUTE2COMPLETING = PLOTTER_EXECUTE2COMPLETING;
	}
	
	public static Duration get_PLOTTER_EXECUTE2COMPLETING() {
		return _PLOTTER_EXECUTE2COMPLETING;
	}

	public static void set_PLOTTER_EXECUTE2COMPLETING(Duration dur) {
		_PLOTTER_EXECUTE2COMPLETING = dur;
	}
	
	
	
}
