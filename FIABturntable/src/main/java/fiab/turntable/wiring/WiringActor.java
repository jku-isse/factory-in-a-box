package fiab.turntable.wiring;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fiab.core.capabilities.wiring.WiringInfo;
import fiab.handshake.client.messages.WiringRequest;
import fiab.handshake.client.messages.WiringUpdateNotification;
import fiab.turntable.message.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WiringActor extends AbstractActor {

    public static Props props(ActorRef parent, String machineName) {
        return Props.create(WiringActor.class, () -> new WiringActor(parent, machineName));
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final String wiringInfoSuffix = "wiringinfo.json";
    private final ActorRef parent;
    private final Map<String, WiringInfo> cachedWiringInfo;

    protected WiringActor(ActorRef parent, String machineName) {
        this.parent = parent;
        log.info("WiringActor started. Searching applicable wiring for machine with name " + machineName);
        this.cachedWiringInfo = readWiringInfoFromFile(machineName);
        if(!cachedWiringInfo.isEmpty()) {
            log.info("Applying wiring info from file");
            applyWiringInfo();
        }else{
            log.warning("No suitable wiring found for machine with name {}, skipping ...", machineName);
        }
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ApplyWiringFromFile.class, msg -> {
                    applyWiringInfo();
                })
                .match(SaveWiringToFile.class, msg -> {
                    saveWiringInfoToFile(msg.getFileName(), cachedWiringInfo);
                })
                .match(DeleteWiringInfoFile.class, msg -> {
                    deleteWiringInfoFile(msg.getFileName());
                })
                .match(WiringUpdateNotification.class, msg -> {
                    cachedWiringInfo.put(msg.getMachineId(), msg.getWiringInfo());
                })
                .build();
    }

    private Map<String, WiringInfo> readWiringInfoFromFile(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        //If file cannot be found check IntelliJ working directory in run configuration
        try {
            File file = new File(fileName + wiringInfoSuffix);
            log.info("Searching for wiring info file in path: " + file.getAbsolutePath());
            return objectMapper.<HashMap<String, WiringInfo>>readValue(file, new TypeReference<HashMap<String, WiringInfo>>() {});
        } catch (IOException e) {
            log.warning("Couldn't find wiring info with name " + fileName + wiringInfoSuffix + ". Continuing without wiring info...");
            return new HashMap<>();
        }
    }

    private void applyWiringInfo() {
        for (String capabilityId : cachedWiringInfo.keySet()) {
            parent.tell(new WiringRequest(self().path().name(), cachedWiringInfo.get(capabilityId)), self());
        }
    }

    private void saveWiringInfoToFile(String fileName, Map<String, WiringInfo> wiringInfoMap) {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            log.info("Persisted new wiringInfo in {}", fileName + wiringInfoSuffix);
            objectMapper.writeValue(new FileOutputStream(fileName + wiringInfoSuffix), wiringInfoMap);
            sender().tell(new WiringSavedNotification(), self());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteWiringInfoFile(String fileName) {
        String fullFileName = fileName+wiringInfoSuffix;
        try {
            Files.deleteIfExists(Path.of(fullFileName));
            log.info("Deleted WiringInfo file {}", fullFileName);
            sender().tell(new WiringDeletedNotification(), self());
        } catch (IOException e) {
            e.printStackTrace();
            sender().tell(new WiringDeletedNotification(false), self());
        }
    }
}