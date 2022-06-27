package fiab.opcua.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OPCUAClientFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Deprecated
    public OpcUaClient createClient(String endpointUrl) throws Exception {
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

        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                .setApplicationUri("urn:eclipse:milo:examples:client")
                .setCertificate(loader.getClientCertificate())
                .setKeyPair(loader.getClientKeyPair())
                .setEndpoint(endpoint)
                .setIdentityProvider(new AnonymousProvider())
                .setRequestTimeout(uint(5000))
                .build();

        return OpcUaClient.create(config);
    }

    /**
     * Async implementation of the createFIABClient method
     * Creates an OpcUa Client with many utility methods not included in the milo sdk
     * This client has been designed with the FIAB in mind and extends OpcUaClient
     *
     * @param endpointUrl endpoint to connect to
     * @return client
     * @throws Exception could not be created e.g. invalid endpoint
     */
    public static CompletableFuture<FiabOpcUaClient> createFIABClientAsync(String endpointUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return FiabOpcUaClient.createFIABClient(createClientConfig(endpointUrl));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.toString());
            }
        }).thenCompose(c -> c.connectFIABClient());
    }

    /**
     * Creates an OpcUa Client with many utility methods not included in the milo sdk
     * This client has been designed with the FIAB in mind and extends OpcUaClient
     *
     * @param endpointUrl endpoint to connect to
     * @return client
     * @throws Exception could not be created e.g. invalid endpoint
     */
    public static FiabOpcUaClient createFIABClient(String endpointUrl) throws Exception {
        OpcUaClientConfig config = createClientConfig(endpointUrl);
        return FiabOpcUaClient.createFIABClient(config);
    }

    private static OpcUaClientConfig createClientConfig(String endpointUrl) throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        SecurityPolicy securityPolicy = SecurityPolicy.None;

        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endpointUrl).get();

        EndpointDescription endpoint = endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
                .findFirst()
                .orElseThrow(() -> new Exception("no desired endpoints returned"));

        return OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("fiab opc-ua client"))
                .setApplicationUri("urn:eclipse:milo:examples:client")
                .setCertificate(loader.getClientCertificate())
                .setKeyPair(loader.getClientKeyPair())
                .setEndpoint(endpoint)
                .setIdentityProvider(new AnonymousProvider())
                .setRequestTimeout(uint(5000))
                .build();
    }

}
