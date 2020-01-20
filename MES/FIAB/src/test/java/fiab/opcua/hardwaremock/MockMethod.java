package fiab.opcua.hardwaremock;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fiab.opcua.hardwaremock.methods.Methods;

public class MockMethod {
	private Methods method;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public MockMethod(Methods m) {
    	method = m;
    }

    @UaMethod
    public void invoke(
        InvocationContext context//,
//
//        @UaInputArgument(
//            name = "string",
//            description = "A String.")
//            String string,
//
//        @UaOutputArgument(
//            name = "newString",
//            description = "The first letter of the original String.")
//            Out<String> firstLetter
        ) {
    		
        System.out.println("INVOKED");
        method.invoke();

//        firstLetter.set("First letter is: " + string.charAt(0));
    		
        
    }
        

}