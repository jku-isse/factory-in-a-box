/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package fiab.opcua.server;

import java.security.Security;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;

public class NonEncryptionBaseOpcUaServer {

    private final int TCP_BIND_PORT;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    
    private final OpcUaServer server;

    public NonEncryptionBaseOpcUaServer(int number, String serverName) throws Exception {
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
            .setApplicationUri("opc.tcp://localhost")
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

        server = new OpcUaServer(serverConfig);
    }

    private Set<EndpointConfiguration> createEndpointConfigurations() {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");

        Set<String> hostnames = new LinkedHashSet<>();
        //String sHostname = HostnameUtil.getHostname();
        //hostnames.add(sHostname);
        //Set<String> hNames = HostnameUtil.getHostnames("0.0.0.0"); // ATTENTION: resolving hostnames takes long when there are many interfaces (e.g., docker, VPN, etc)
        //hostnames.addAll(hNames);
        hostnames.add("127.0.0.1");
        //FIXME: add others here for external reachability
        
        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                    .setBindAddress(bindAddress)
                    .setHostname(hostname)
                    .setPath("/milo")
                    .addTokenPolicies(
                        USER_TOKEN_POLICY_ANONYMOUS,
                        USER_TOKEN_POLICY_USERNAME 
                        );


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));



                EndpointConfiguration.Builder discoveryBuilder = builder.copy()
                    .setPath("/milo/discovery")
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