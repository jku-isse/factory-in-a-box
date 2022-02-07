package fiab.opcua.server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;

public class FastPublicNonEncryptionBaseOpcUaServer {
    private final int TCP_BIND_PORT;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final OpcUaServer server;

    public FastPublicNonEncryptionBaseOpcUaServer(int number, String serverName) throws Exception {
        TCP_BIND_PORT = 4840+number;
        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                true,
                authChallenge -> {
                    String username = authChallenge.getUsername();
                    String password = authChallenge.getPassword();

                    boolean userOk = "user".equals(username) && "password1".equals(password);
                    boolean adminOk = "admin".equals(username) && "password2".equals(password);

                    return userOk || adminOk;
                }
        );

        X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

        Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations();

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setApplicationUri("opc.tcp://127.0.0.1")
                .setApplicationName(LocalizedText.english(serverName))
                .setEndpoints(endpointConfigurations)
                .setBuildInfo(
                        new BuildInfo(
                                "urn:jku:fiab",
                                "JKU",
                                serverName,
                                OpcUaServer.SDK_VERSION,
                                "", DateTime.now()))
                .setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
                .setProductUri("urn:eclipse:milo:"+serverName)
                .build();
        server = new OpcUaServer(serverConfig); // <- This is the bottleneck. It takes about 2 seconds which is roughly half of the startup time on my machine
    }

    private Set<EndpointConfiguration> createEndpointConfigurations() {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");

        Set<String> hostnames = new LinkedHashSet<>();
        String sHostname = HostnameUtil.getHostname();
        hostnames.add(sHostname);
        //Set<String> hNames = HostnameUtil.getHostnames("0.0.0.0"); // ATTENTION: resolving hostnames takes long when there are many interfaces (e.g., docker, VPN, etc)
        //hostnames.addAll(hNames);
        //hostnames.add("127.0.0.1");
        String ipAddr = getIp();
        System.out.println("Using ip address " + ipAddr);
        hostnames.add(ipAddr);
        //FIXME: add others here for external reachability

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                        .setBindAddress(bindAddress)
                        .setHostname(hostname)
                        .setPath("/milo")       //This can be deleted in the future
                        .addTokenPolicies(
                                USER_TOKEN_POLICY_ANONYMOUS,
                                USER_TOKEN_POLICY_USERNAME
                        );


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));

                EndpointConfiguration.Builder discoveryBuilder = builder.copy()
                        .setPath("/milo/discovery")     //We can change this to /discovery as well
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
            }
        }

        return endpointConfigurations;
    }

    private EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindPort(TCP_BIND_PORT)
                .build();
    }

    private String getIp() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
                    .filter(ip -> ip instanceof Inet4Address)
                    //We know the assigned ip addresses from the router start with 192.168.0. for all machines
                    .filter(ip -> ip.getHostAddress().startsWith("192.168.0."))
                    .findFirst().orElseThrow(RuntimeException::new)
                    .getHostAddress();
        } catch (SocketException | RuntimeException e) {
            e.printStackTrace();
            System.out.println("Could not resolve IP Address, defaulting to localhost (127.0.0.1)");
        }
        return "127.0.0.1";
    }

    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() {
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        return server.shutdown();
    }
}
