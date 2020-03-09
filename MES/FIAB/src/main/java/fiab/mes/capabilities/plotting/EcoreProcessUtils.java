package fiab.mes.capabilities.plotting;

import org.eclipse.emf.common.util.EList;

import ProcessCore.AbstractCapability;
import ProcessCore.CapabilityInvocation;
import ProcessCore.LocalVariable;
import ProcessCore.Parameter;
import ProcessCore.Process;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.VariableMapping;
import fiab.mes.capabilities.ComparableCapability;
import fiab.mes.capabilities.HashMapRegistry;
import scala.collection.mutable.HashTable;

public class EcoreProcessUtils {

	public static Parameter getParameter(String type, String name, String value) {
		Parameter p = ProcessCoreFactory.eINSTANCE.createParameter();
		p.setType(type);
		p.setValue(value);
		p.setName(name);
		return p;
	}

	public static void addProcessvariables(Process p, String... args) {
		for (String value : args) {
			p.getVariables().add(createLocalStringVariable(value, value));
		}
	}
	
	public static LocalVariable createLocalStringVariable(String name, String value) {
		LocalVariable lv = ProcessCoreFactory.eINSTANCE.createLocalVariable();
		lv.setName(name);
		lv.setType("String");
		lv.setValue(value);
		return lv;
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