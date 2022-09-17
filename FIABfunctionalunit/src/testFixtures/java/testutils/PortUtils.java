package testutils;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class PortUtils {

    private static int currentPortNumber = 4840;

    /**
     * This method looks for a free opcUa port starting at 4840 (opcua default)
     * Used in unit testing where a port may be closed, but still listening (TCP standard)
     * If 4840 is occupied, it will try 4841. This will be repeated until 65535 is reached (highest tcp port)
     *
     * @return next free port
     */
    public static int findNextFreePort() {
        while (!isPortAvailable(currentPortNumber)) {    //If port is occupied increase portNumber
            currentPortNumber++;
        }
        int freePort = currentPortNumber;
        currentPortNumber++;    //Increase, so that next time we can skip this port
        return freePort;
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) //is required only on macOS,
            // otherwise the code will not work correctly on that platform
            //serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
