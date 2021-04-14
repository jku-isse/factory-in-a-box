package fiab.opcua.client;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.ModifiedOpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zipkin2.reporter.AsyncReporter;

public class OPCUAClientFactory {

	 private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public OpcUaClient createTracingClient(String endpointUrl,  AsyncReporter<zipkin2.Span> reporter, String serviceName) throws Exception{
	    return createClient(endpointUrl, reporter, serviceName);
	}
	
	public OpcUaClient createClient(String endpointUrl) throws Exception {
		return createClient(endpointUrl, null, null);
	}
		
	private OpcUaClient createClient(String endpointUrl, AsyncReporter<zipkin2.Span> reporter, String serviceName) throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
        LoggerFactory.getLogger(getClass())
            .info("security temp dir: {}", securityTempDir.toAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        SecurityPolicy securityPolicy = SecurityPolicy.None;

        List<EndpointDescription> endpoints;

        try {
            endpoints = DiscoveryClient.getEndpoints(endpointUrl).get();
        } catch (Throwable ex) {
            // try the explicit discovery endpoint as well
            String discoveryUrl = endpointUrl;

            if (!discoveryUrl.endsWith("/")) {
                discoveryUrl += "/";
            }
            discoveryUrl += "milo";

            logger.info("Trying explicit discovery URL: {}", discoveryUrl);
            endpoints = DiscoveryClient.getEndpoints(discoveryUrl).get();
        }

        EndpointDescription endpoint = endpoints.stream()
            .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
           // .filter(clientExample.endpointFilter())
            .findFirst()
            .orElseThrow(() -> new Exception("no desired endpoints returned"));
//
//        logger.info("Using endpoint: {} [{}/{}]",
//            endpoint.getEndpointUrl(), securityPolicy, endpoint.getSecurityMode());
        String applicationUri = serviceName == null ? "urn:eclipse:milo:examples:client" : serviceName;
        OpcUaClientConfig config = OpcUaClientConfig.builder()
            .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
            .setApplicationUri(applicationUri)
            .setCertificate(loader.getClientCertificate())
            .setKeyPair(loader.getClientKeyPair())
            .setEndpoint(endpoint)
            .setIdentityProvider(new AnonymousProvider())
            .setRequestTimeout(uint(5000))
            .build();
        if (reporter == null) {
        	return OpcUaClient.create(config);
        } else {
        	return ModifiedOpcUaClient.create(config, reporter);	
        }
    }
	
}
