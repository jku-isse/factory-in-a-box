package fiab.capabilityTool.tool;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import fiab.capabilityTool.opcua.CapabilityManagerClient;
import fiab.capabilityTool.opcua.PlotCapability;
import fiab.capabilityTool.opcua.msg.ClientReadyNotification;
import fiab.capabilityTool.opcua.msg.WriteRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CapabilityManagerActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final URI dirPath;
    private final Map<String, String> urlCapabilitiesMap;

    public static Props props(URI dirPath) {
        return Props.create(CapabilityManagerActor.class, dirPath);
    }

    public CapabilityManagerActor(URI dirPath) {
        this.dirPath = dirPath;
        urlCapabilitiesMap = getCapabilitiesFromDir();
        urlCapabilitiesMap.forEach((url, capability) -> context().actorOf(CapabilityManagerClient.props(url)));
    }

    @Override
    public Receive createReceive() {
        return new ReceiveBuilder()
                .match(ClientReadyNotification.class, notification -> {
                    String url = notification.getEndpointUrl();
                    if (urlCapabilitiesMap.containsKey(url)) {
                        String capability = urlCapabilitiesMap.get(url);
                        sender().tell(new WriteRequest(capability), self());
                    }
                }).build();
    }

    private Map<String, String> getCapabilitiesFromDir() {
        Map<String, String> endPointCapabilityMap = null;
        try {
            List<Path> filePaths = Files.list(Path.of(dirPath)).collect(Collectors.toList());
            endPointCapabilityMap = new HashMap<>();
            for (Path path : filePaths) {
                AbstractMap.SimpleEntry<String, String> entry = parseJsonFileAsCapabilityEntry(path);
                if (entry != null) {
                    endPointCapabilityMap.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return endPointCapabilityMap;
    }

    private AbstractMap.SimpleEntry<String, String> parseJsonFileAsCapabilityEntry(Path capabilityFilePath) {
        ObjectMapper mapper = new ObjectMapper();
        PlotCapability plotCapability;
        try {
            String jsonContent = Files.readString(capabilityFilePath);
            plotCapability = mapper.readValue(jsonContent, PlotCapability.class);
            return new AbstractMap.SimpleEntry<>(plotCapability.endpointUrl, plotCapability.plotCapability);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
