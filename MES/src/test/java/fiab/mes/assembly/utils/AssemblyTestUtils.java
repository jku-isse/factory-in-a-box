package fiab.mes.assembly.utils;

import ActorCoreModel.ActorCoreModelPackage;
import ExtensionsCoreModel.ExtensionsCoreModelPackage;
import ExtensionsForAssemblyline.ExtensionsForAssemblylinePackage;
import InstanceExtensionModel.InstanceExtensionModelPackage;
import LinkedCoreModelActorToPart.LinkedCoreModelActorToPartPackage;
import PartCoreModel.PartCoreModelPackage;
import PriorityExtensionModel.PriorityExtensionModelPackage;
import ProcessCore.XmlRoot;
import VariabilityExtensionModel.VariabilityExtensionModelPackage;
import actorprocess.ActorprocessPackage;
import akka.actor.ActorRef;
import at.pro2future.shopfloors.interfaces.impl.FileDataSource;
import fiab.mes.assembly.order.message.ExtendedRegisterProcessRequest;
import fiab.mes.order.OrderProcess;
import partprocess.PartprocessPackage;

import java.io.IOException;

public class AssemblyTestUtils {

    public static ExtendedRegisterProcessRequest loadExampleProcessFromFile(String path, ActorRef sender) {
        String senderId = "MockOrderActor";
        XmlRoot xmlRoot = loadXmi(path);
        return new ExtendedRegisterProcessRequest(senderId, new OrderProcess(xmlRoot.getProcesses().get(0)), xmlRoot, sender);
    }

    public static XmlRoot loadXmi(String path) {
        FileDataSource src = new FileDataSource(path);
        addPackages(src);
        XmlRoot root = null;
        try {
            root = src.getShopfloorData().get(0);
            return root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    public static void addPackages(FileDataSource src) {
        src.registerPackage(ActorCoreModelPackage.eNS_URI, ActorCoreModelPackage.eINSTANCE);
        src.registerPackage(ActorprocessPackage.eNS_URI, ActorprocessPackage.eINSTANCE);
        src.registerPackage(PartCoreModelPackage.eNS_URI, PartCoreModelPackage.eINSTANCE);
        src.registerPackage(LinkedCoreModelActorToPartPackage.eNS_URI, LinkedCoreModelActorToPartPackage.eINSTANCE);
        src.registerPackage(PartprocessPackage.eNS_URI, PartprocessPackage.eINSTANCE);
        src.registerPackage(ExtensionsForAssemblylinePackage.eNS_URI, ExtensionsForAssemblylinePackage.eINSTANCE);
        src.registerPackage(InstanceExtensionModelPackage.eNS_URI, InstanceExtensionModelPackage.eINSTANCE);
        src.registerPackage(ExtensionsCoreModelPackage.eNS_URI, ExtensionsCoreModelPackage.eINSTANCE);
        src.registerPackage(VariabilityExtensionModelPackage.eNS_URI, VariabilityExtensionModelPackage.eINSTANCE);
        src.registerPackage(PriorityExtensionModelPackage.eNS_URI, PriorityExtensionModelPackage.eINSTANCE);
    }
}
