package fiab.mes.opcua;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSeverMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @UaMethod
    public void invoke(
        InvocationContext context,

        @UaInputArgument(
            name = "world",
            description = "A string.")
            String world,

        @UaOutputArgument(
            name = "helloworld",
            description = "Appends 'Hello' to the String.")
            Out<String> helloworld) {

        System.out.println("HelloWorld(" + world + ")");
        logger.debug("Invoking sqrt() method of Object '{}'", context.getObjectNode().getBrowseName().getName());

        helloworld.set("Hello, " + world);
    }

}
