package fiab.mes.order.ecore;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;

import ProcessCore.XmlRoot;

public interface DataPersistor {
	public void persistShopfloorData(List<XmlRoot> s) throws IOException;

	public void registerPackage(String uri, EPackage pack);

	public List<File> getResource();

	public void unload();
}
