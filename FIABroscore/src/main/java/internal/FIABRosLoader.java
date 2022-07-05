package internal;

import akka.actor.ActorRef;
import internal.node.FIABNodeMain;
import org.ros.CommandLineVariables;
import org.ros.EnvironmentVariables;
import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FIABRosLoader {

    private final List<String> argv;
    private final List<String> nodeArguments;
    private final List<String> remappingArguments;
    private final Map<String, String> environment;
    private final Map<String, String> specialRemappings;
    private final Map<GraphName, GraphName> remappings;

    public FIABRosLoader() {
        this.argv = new ArrayList<>();
        this.environment = System.getenv();
        nodeArguments = new ArrayList<>();
        remappingArguments = new ArrayList<>();
        remappings = new HashMap<>();
        specialRemappings = new HashMap<>();
        parseArgv();
    }

    private void parseArgv() {
        /*for (String argument : argv.subList(1, argv.size())) {
            if (argument.contains(":=")) {
                remappingArguments.add(argument);
            } else {
                nodeArguments.add(argument);
            }
        }*/ //We assume no args are used
    }

    /**
     * @param clazz    nodeclass to instantiate
     * @param actorRef actorRef
     * @return an instance of {@link FIABNodeMain}
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public FIABNodeMain loadClass(Class<?> clazz, ActorRef actorRef) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        //Class<?> clazz = getClass().getClassLoader().loadClass(name);
        FIABNodeMain nodeMain = createInstanceUsingActorRefConstructor(clazz, actorRef);
        return nodeMain;
    }

    /**
     * @param clazz nodeclass to instantiate
     * @return an instance of {@link FIABNodeMain}
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public FIABNodeMain loadClass(Class<?> clazz) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        //Class<?> clazz = getClass().getClassLoader().loadClass(name);
        FIABNodeMain nodeMain = createInstanceUsingDefaultConstructor(clazz);
        return nodeMain;
    }

    /**
     * Use this class to create a node with an actorRef as a constructor parameter
     * Used for callbacks in service calls
     *
     * @param clazz
     * @param actorRef
     * @return
     */
    private FIABNodeMain createInstanceUsingActorRefConstructor(Class<?> clazz, ActorRef actorRef) {
        try {
            Constructor<?> constructor = clazz.getConstructor(ActorRef.class);
            return (FIABNodeMain) constructor.newInstance(actorRef);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a node using the default constructor
     * Make sure the node provides one if a constructor has been defined
     *
     * @param clazz
     * @return
     */
    private FIABNodeMain createInstanceUsingDefaultConstructor(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            return (FIABNodeMain) constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create NodeConfiguration according to ROS command-line and environment
     * specification.
     */
    public NodeConfiguration build() {
        parseRemappingArguments();
        // TODO(damonkohler): Add support for starting up a private node.
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getHost());
        nodeConfiguration.setParentResolver(buildParentResolver());
        nodeConfiguration.setRosRoot(getRosRoot());
        nodeConfiguration.setRosPackagePath(getRosPackagePath());
        nodeConfiguration.setMasterUri(getMasterUri());
        if (specialRemappings.containsKey(CommandLineVariables.NODE_NAME)) {
            nodeConfiguration.setNodeName(specialRemappings.get(CommandLineVariables.NODE_NAME));
        }
        return nodeConfiguration;
    }

    private void parseRemappingArguments() {
        for (String remapping : remappingArguments) {
            String[] remap = remapping.split(":=");
            if (remap.length > 2) {
                throw new IllegalArgumentException("Invalid remapping argument: " + remapping);
            }
            if (remapping.startsWith("__")) {
                specialRemappings.put(remap[0], remap[1]);
            } else {
                remappings.put(GraphName.of(remap[0]), GraphName.of(remap[1]));
            }
        }
    }

    /**
     * Precedence:
     *
     * <ol>
     * <li>The __ns:= command line argument.</li>
     * <li>The ROS_NAMESPACE environment variable.</li>
     * </ol>
     */
    private NameResolver buildParentResolver() {
        GraphName namespace = GraphName.root();
        if (specialRemappings.containsKey(CommandLineVariables.ROS_NAMESPACE)) {
            namespace =
                    GraphName.of(specialRemappings.get(CommandLineVariables.ROS_NAMESPACE)).toGlobal();
        } else if (environment.containsKey(EnvironmentVariables.ROS_NAMESPACE)) {
            namespace = GraphName.of(environment.get(EnvironmentVariables.ROS_NAMESPACE)).toGlobal();
        }
        return new NameResolver(namespace, remappings);
    }

    /**
     * Precedence (default: null):
     *
     * <ol>
     * <li>The __ip:= command line argument.</li>
     * <li>The ROS_IP environment variable.</li>
     * <li>The ROS_HOSTNAME environment variable.</li>
     * <li>The default host as specified in {@link NodeConfiguration}.</li>
     * </ol>
     */
    private String getHost() {
        String host = InetAddressFactory.newLoopback().getHostAddress();
        if (specialRemappings.containsKey(CommandLineVariables.ROS_IP)) {
            host = specialRemappings.get(CommandLineVariables.ROS_IP);
        } else if (environment.containsKey(EnvironmentVariables.ROS_IP)) {
            host = environment.get(EnvironmentVariables.ROS_IP);
        } else if (environment.containsKey(EnvironmentVariables.ROS_HOSTNAME)) {
            host = environment.get(EnvironmentVariables.ROS_HOSTNAME);
        }
        return host;
    }

    /**
     * Precedence:
     *
     * <ol>
     * <li>The __master:= command line argument. This is not required but easy to
     * support.</li>
     * <li>The ROS_MASTER_URI environment variable.</li>
     * <li>The default master URI as defined in {@link NodeConfiguration}.</li>
     * </ol>
     */
    private URI getMasterUri() {
        URI uri = NodeConfiguration.DEFAULT_MASTER_URI;
        try {
            if (specialRemappings.containsKey(CommandLineVariables.ROS_MASTER_URI)) {
                uri = new URI(specialRemappings.get(CommandLineVariables.ROS_MASTER_URI));
            } else if (environment.containsKey(EnvironmentVariables.ROS_MASTER_URI)) {
                uri = new URI(environment.get(EnvironmentVariables.ROS_MASTER_URI));
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new RosRuntimeException("Invalid master URI: " + uri);
        }
    }

    private File getRosRoot() {
        if (environment.containsKey(EnvironmentVariables.ROS_ROOT)) {
            return new File(environment.get(EnvironmentVariables.ROS_ROOT));
        } else {
            // For now, this is not required as we are not doing anything (e.g.
            // ClassLoader) that requires it. In the future, this may become required.
            return null;
        }
    }

    private List<File> getRosPackagePath() {
        if (environment.containsKey(EnvironmentVariables.ROS_PACKAGE_PATH)) {
            String rosPackagePath = environment.get(EnvironmentVariables.ROS_PACKAGE_PATH);
            List<File> paths = new ArrayList<>();
            for (String path : rosPackagePath.split(File.pathSeparator)) {
                paths.add(new File(path));
            }
            return paths;
        } else {
            return new ArrayList<>();
        }
    }

}
