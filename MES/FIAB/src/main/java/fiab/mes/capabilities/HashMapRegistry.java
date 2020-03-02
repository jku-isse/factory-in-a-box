package fiab.mes.capabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessCorePackage;
import ProcessCore.XmlRoot;
import actorprocess.ActorprocessPackage;
import fiab.mes.general.ComparableCapability;

public class HashMapRegistry implements CapabilityRegistry {
	private static final String CAPABILITY_FILE_URL = "files\\Capabilities.xmi";

	private final Map<String, ComparableCapability> registry;

	public HashMapRegistry() {
		super();
		registry = new HashMap<>();
		fillRegistryFromFile();
	}

// we are not using abstract capabilities, but copies of them as comparable capabilities
	@Override
	public boolean register(AbstractCapability cap) {
		ComparableCapability new_ = copy(cap);
		if (registry.containsKey(new_.getID()))
			return false;
		registry.put(new_.getID(), new_);
		return true;
	}

	// same as for register, we are only working with copies, as the equals method
	// gets true for equal IDs and URIs
	@Override
	public ComparableCapability get(String id) {
		return copy(registry.get(id));
	}

	private void fillRegistryFromFile() {
		XmlRoot root = loadRoot();
		for (AbstractCapability cap : root.getCapabilities()) {
			register(cap);
		}
	}

	private XmlRoot loadRoot() {
		ResourceSet rs = createRS();
		Resource r = rs.getResource(URI.createFileURI(CAPABILITY_FILE_URL), true);
		XmlRoot root = (XmlRoot) r.getContents().get(0);
		if (root == null)
			throw new RuntimeException("Resource does not contain any roots");

		return root;
	}

	private ComparableCapability copy(AbstractCapability cap) {
		if (cap == null)
			return null;
		ComparableCapability copy = new ComparableCapability();
		copy.setID(cap.getID());
		copy.setDisplayName(cap.getDisplayName());
		copy.setUri(cap.getUri());
		copy.getVariables().addAll(cap.getVariables());
		copy.getInputs().addAll(copyParameters(cap.getInputs()));
		return copy;
	}

	private Collection<? extends Parameter> copyParameters(EList<Parameter> inputs) {
		ArrayList<Parameter> copiedPars = new ArrayList<>();
		for (Parameter p : inputs) {
			copiedPars.add(copyParameter(p));
		}
		return copiedPars;
	}

	private Parameter copyParameter(Parameter p) {
		Parameter copy = ProcessCoreFactory.eINSTANCE.createParameter();
		copy.setName(p.getName());
		copy.setType(p.getType());
		copy.setValue(p.getValue());
		return copy;
	}

	public ResourceSet createRS() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getPackageRegistry().put(ExtensionsCoreModelPackage.eNS_URI, ExtensionsCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ProcessCorePackage.eNS_URI, ProcessCorePackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorCoreModelPackage.eNS_URI, ActorCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorprocessPackage.eNS_URI, ActorprocessPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		return rs;
	}

}
