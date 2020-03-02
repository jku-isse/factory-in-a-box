package fiab.mes.order.ecore;

import org.eclipse.emf.common.util.EList;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.Parameter;
import ProcessCore.Process;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.VariableMapping;
import fiab.mes.capabilities.HashMapRegistry;
import fiab.mes.general.ComparableCapability;
import fiab.mes.machine.actor.plotter.WellknownPlotterCapability;
import scala.collection.mutable.HashTable;

public class EcoreProcessUtils {
	public static AbstractCapability getColorPlotCapability(String color) {
		ComparableCapability cap = new ComparableCapability();
		cap.setDisplayName("Plot: " + color);
		cap.setID("Capability.Plotting.Color." + color);
		cap.setUri("http://factory-in-a-box.fiab/capabilities/plotter/colors/" + color);
		cap.getInputs().add(getImageParameter());
		return cap;
	}

	public static Parameter getImageParameter() {
		Parameter p = ProcessCoreFactory.eINSTANCE.createParameter();
		p.setName(WellknownPlotterCapability.PLOTTING_CAPABILITY_INPUT_IMAGE_VAR_NAME);
		p.setType("String");
		return p;
	}

//	public static AbstractCapability getColorCapability(String color) {
//		ComparableCapability ac = new ComparableCapability();
//		ac.setDisplayName(color);
//		ac.setID("Capability.Plotting.Color." + color);
//		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/colors/" + color);
//		return ac;
//	}
//
//	public static AbstractCapability getPlottingCapability() {
//		ComparableCapability ac = new ComparableCapability();
//		ac.setDisplayName("plot");
//		ac.setID("Capability.Plotting");
//		ac.setID("http://factory-in-a-box.fiab/capabilities/plotter/plotting");
//		return ac;
//	}
//
//	public static AbstractCapability composeInOne(AbstractCapability... caps) {
//		ComparableCapability ac = new ComparableCapability();
//		for (AbstractCapability cap : caps) {
//			ac.getCapabilities().add(cap);
//		}
//		return ac;
//	}

	public static Parameter getParameter(String type, String name, String value) {
		Parameter p = ProcessCoreFactory.eINSTANCE.createParameter();
		p.setType(type);
		p.setValue(value);
		p.setName(name);
		return p;
	}

	public static void addProcessvariables(Process p, String... args) {
		for (String value : args) {
			p.getVariables().add(getParameter("String", value, value));
		}
	}

	public static VariableMapping getVariableMapping(Parameter lhs) {
		VariableMapping mapping = ProcessCoreFactory.eINSTANCE.createVariableMapping();
		mapping.setLhs(lhs);
		return mapping;
	}

	public static void mapCapInputToProcessVar(EList<Parameter> variables, CapabilityInvocation... caps) {
		for (int i = 0; i < caps.length; i++) {
			caps[i].getInputMappings().get(0).setRhs(variables.get(i));
		}
	}
}