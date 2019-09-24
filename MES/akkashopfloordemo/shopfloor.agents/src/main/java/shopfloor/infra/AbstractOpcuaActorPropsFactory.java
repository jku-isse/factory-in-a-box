package shopfloor.infra;

import akka.actor.Props;

public interface AbstractOpcuaActorPropsFactory  {

	public Props getProps(OpcuaEntryNodeInfo entryPoint); 
	

	
	public static class OpcuaEntryNodeInfo {
		public String serverUrl;
		public String nodeId;
	}
	
	public static class RegisterActorDefForCapability {
		public AbstractOpcuaActorPropsFactory propsFactory;
		public String capabilityNodeName;
	}
	
}
