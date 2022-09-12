package fiab;

import akka.actor.ActorSystem;
import fiab.iostation.InputStationFactory;

import java.net.URL;

public class InputStationROSApplication {

    public static void main(String[] args) {
        String rosMasterIpAddress;
        if(args.length == 1 && isValidURI(args[0])){
            System.out.println("Using remote master node located at: " + args[0]);
            rosMasterIpAddress = args[0];
        }else {
            if(args.length > 0){
                System.out.println("Remote URI "+ args[0]+" could not be validated");
                System.out.println("Using remote master node located at: " + args[0]);
            }else{
                System.out.println("Using default ros master URI at localhost: http://127.0.0.1:11311");
            }
            rosMasterIpAddress = "http://127.0.0.1:11311";
        }
        ActorSystem system = ActorSystem.create();
        InputStationFactory.startStandaloneInputStationROS(system, 4840, "InputStation", rosMasterIpAddress);
    }

    private static boolean isValidURI(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (Exception e1) {
            return false;
        }
        return "http".equals(url.getProtocol());
    }
}
