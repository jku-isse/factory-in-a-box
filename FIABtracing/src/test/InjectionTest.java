package test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fiab.tracing.config.Util;
import fiab.tracing.factory.TracingFactory;

public class InjectionTest {
	public static void main(String[] args) {
		Injector injector = Guice.createInjector(Util.getConfig());
		
		TracingFactory fac = injector.getInstance(TracingFactory.class);
		System.out.println(fac.toString());
	}
}
