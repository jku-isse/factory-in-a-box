package fiab.mes.order.ecore;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
//import opcuaextension.OpcuaextensionPackage;
//import ProcessCore.AbstractCapability;
//import ProcessCore.Process;
import ProcessCore.ProcessCorePackage;
import ProcessCore.XmlRoot;
import actorprocess.ActorprocessPackage;

public class FileDataPersistor implements DataPersistor{
	String folderPath;
	Resource target;
	ResourceSet rs;
	File f;
	
	private URI dataUri;
	/**
	 * 
	 * @param Filename should end in ".xmi"
	 */
	public FileDataPersistor(String fileName) {
		rs = new ResourceSetImpl();
		rs.getPackageRegistry().put(ExtensionsCoreModelPackage.eNS_URI, ExtensionsCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ProcessCorePackage.eNS_URI, ProcessCorePackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorCoreModelPackage.eNS_URI, ActorCoreModelPackage.eINSTANCE);
		rs.getPackageRegistry().put(ActorprocessPackage.eNS_URI, ActorprocessPackage.eINSTANCE);
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

		String completedName;
		if(fileName.contains(".xmi")) {
			completedName = fileName;
		} else {
			completedName = fileName + ".xmi";
		}
		f = new File(completedName);
		folderPath = f.getParent()+"\\";
		
		dataUri = URI.createFileURI(completedName);
		
		
	}
	

	public void persistShopfloorData(List<XmlRoot> roots) throws IOException {

		target = rs.createResource(dataUri);
		/*

		 * Iterator<AbstractCapability> content = s.getCapabilities().iterator();
		 * while(content.hasNext()) { EObject eo = content.next();
		 * target.getContents().add(eo); } Iterator<Process> pcontent =
		 * s.getProcesses().iterator(); while(pcontent.hasNext()) { EObject eo =
		 * pcontent.next(); target.getContents().add(eo); }
		 */
		

		Iterator<XmlRoot> econtent = roots.iterator();
		while(econtent.hasNext()) {
			EObject eo = econtent.next();
			target.getContents().add(eo);
		}
		
		target.save(Collections.EMPTY_MAP);
	}


	@Override
	public void registerPackage(String uri, EPackage pack) {
		if(!rs.getPackageRegistry().containsKey(uri))
			rs.getPackageRegistry().put(uri, pack);
		
	}
	public void unload() {
		if(target != null) {
			target.unload();
		}
			
	}


	@Override
	public List<File> getResource() {
		List<File> files = new LinkedList<>();
		files.add(f);
		return files;
	}
}