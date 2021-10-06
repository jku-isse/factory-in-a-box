package fiab.handshake.opcua;

public class HandshakeServerClientApplication {

    public static void main(String[] args) {
        try {
            System.out.println("Starting Standalone Server Application");
            HandshakeStartupUtil.startServerHandshakeApplication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
