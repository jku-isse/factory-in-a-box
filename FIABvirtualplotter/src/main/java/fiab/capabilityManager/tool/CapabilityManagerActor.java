package fiab.capabilityManager.tool;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import fiab.capabilityManager.opcua.CapabilityManagerClient;
import fiab.capabilityManager.opcua.PlotCapability;
import fiab.capabilityManager.opcua.msg.ClientReadyNotification;
import fiab.capabilityManager.opcua.msg.WriteRequest;
import fiab.tracing.actor.AbstractTracingActor;

public class CapabilityManagerActor extends AbstractTracingActor {

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
		return new ReceiveBuilder().match(ClientReadyNotification.class, notification -> {
			try {
				tracer.startConsumerSpan(notification,
						"Capability Manager Actor: Client ready Notification received");

				String url = notification.getEndpointUrl();
				if (urlCapabilitiesMap.containsKey(url)) {
					String capability = urlCapabilitiesMap.get(url);

					WriteRequest req = new WriteRequest(capability, tracer.getCurrentHeader());
					tracer.injectMsg(req);

					sender().tell(req, self());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tracer.finishCurrentSpan();
			}

		}).build();
	}

	private Map<String, String> getCapabilitiesFromDir() {
		Map<String, String> endPointCapabilityMap = null;
		try {
			List<Path> filePaths = Files.list(Paths.get(dirPath)).collect(Collectors.toList());
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
			String jsonContent = new String(Files.readAllBytes(capabilityFilePath));
			plotCapability = mapper.readValue(jsonContent, PlotCapability.class);
			return new AbstractMap.SimpleEntry<>(plotCapability.endpointUrl, plotCapability.plotCapability);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
