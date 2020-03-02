package fiab.mes.restendpoint.xmltransformer;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
import ProcessCore.ProcessCoreFactory;
import ProcessCore.ProcessCorePackage;
import ProcessCore.XmlRoot;
import actorprocess.ActorprocessPackage;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.unmarshalling.Unmarshaller;

public class EcoreStringUnmarshaller {
	
	protected static final Logger logger = LoggerFactory.getLogger(EcoreStringUnmarshaller.class);
	
	private final static ResourceSet rs;
	private final static String CAPABILITY_URL = "Capabilities.xmi",
			TARGET_URL = "Target.xmi";
	// private final static String CAPABILITY_URL = "C:\\Users\\JanHolzweber\\eclipse_workspaces\\workshop in a box\\Try\\Capabilities.xmi",
	//		TARGET_URL = "Target.xmi";

	static {
		rs = createResourceSet();
		rs.getResource(URI.createFileURI(CAPABILITY_URL), true);
	}

	public static Unmarshaller<HttpEntity, ProcessCore.Process> unmarshaller() {
		return Unmarshaller.forMediaType(MediaTypes.APPLICATION_XML, Unmarshaller.entityToString())
				.thenApply(s -> fromJSON(s));
	}

	private static ProcessCore.Process fromJSON(String xml) {
		// System.out.println(xml);
		XMLResource target = (XMLResource) rs.createResource(URI.createFileURI(TARGET_URL));
		try {
			target.load(new InputSource(new StringReader(xml)), null);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			e.printStackTrace();
			return ProcessCoreFactory.eINSTANCE.createProcess();
		}

		XmlRoot root = (XmlRoot) target.getContents().get(0);
		if (root.getProcesses().isEmpty()) {
			logger.warn("No process found in XML request payload, returning empty process");
			return ProcessCoreFactory.eINSTANCE.createProcess();
			//throw new RuntimeException("No Process in Serialized Xml");
		}
		ProcessCore.Process p = root.getProcesses().get(0);
		return p;		
	}

// Adding all ecore packages to the resource set
	private static ResourceSet createResourceSet() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getPackageRegistry().put(ExtensionsCoreModelPackage.eNS_URI, ExtensionsCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ProcessCorePackage.eNS_URI, ProcessCorePackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorCoreModelPackage.eNS_URI, ActorCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorprocessPackage.eNS_URI, ActorprocessPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		return rs;
	}

}
