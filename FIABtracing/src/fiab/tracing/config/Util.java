package fiab.tracing.config;

public class Util {
	
	private static final FactoryConfig config;

	static {
		config = new FactoryConfig();
	}

	public static FactoryConfig getConfig() {
		return config;
	}
}
