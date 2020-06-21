package fiab.mes.planer.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ActorCoreModel.Actor;
import ProcessCore.AbstractCapability;
import akka.actor.ActorRef;
import fiab.core.capabilities.ComparableCapability;
import fiab.mes.machine.AkkaActorBackedCoreModelAbstractActor;
import fiab.mes.machine.msg.MachineConnectedEvent;

public class MachineCapabilityManager {

	protected Map<AkkaActorBackedCoreModelAbstractActor, Set<AbstractCapability>> actorProvides = new HashMap<>();
	protected Map<AbstractCapability, Set<AkkaActorBackedCoreModelAbstractActor>> capProvidedBy = new HashMap<>();
	
	
	public Optional<AkkaActorBackedCoreModelAbstractActor> resolveById(String machineId) {
		return actorProvides.keySet().stream()
				.filter(mach -> mach.getId().contentEquals(machineId))
				.findAny();
	}
	
	public void setCapabilities(MachineConnectedEvent mce) {
		assert(mce != null);
		// put in index of provided capabilities
		// first, remove any old references		
		removeActor(mce.getMachine());
		// then set capabilities
		mce.getProvidedMachineCapabilities().stream()
			.flatMap(cap -> flatHierarchyToCapsWithNonNullId(cap).stream()) 
			.forEach(cap -> capProvidedBy.computeIfAbsent(cap, k -> new HashSet<AkkaActorBackedCoreModelAbstractActor>()).add(mce.getMachine()) );
		// replace/add in actor list
		Set<AbstractCapability> caps = mce.getProvidedMachineCapabilities().stream()
			.flatMap(cap -> flatHierarchyToCapsWithNonNullId(cap).stream())
			.collect(Collectors.toSet());
		actorProvides.put(mce.getMachine(), caps);
	}
	
	// we flatten all capabilities into a list regardless of hierarchy structure, only include caps when they have an id
	public static List<AbstractCapability> flatHierarchyToCapsWithNonNullId(AbstractCapability cap) {
		List<AbstractCapability> caps = new ArrayList<>();
		if (cap.getID() != null) {
			caps.add(transformToComparableCapability(cap));
		}
		cap.getCapabilities().parallelStream().forEach(subCap -> caps.addAll(flatHierarchyToCapsWithNonNullId(subCap)));
		return caps;
	}
	
	private static ComparableCapability transformToComparableCapability(AbstractCapability cap) {
		if (cap instanceof ComparableCapability)
			return (ComparableCapability) cap;
		else {
			ComparableCapability cc = new ComparableCapability();
			cc.setID(cap.getID());
			cc.setUri(cap.getUri());
			cc.getInputs().addAll(cap.getInputs());
			cc.getOutputs().addAll(cap.getOutputs());
			cc.getVariables().addAll(cap.getVariables());
			cc.setDisplayName(cap.getDisplayName());
			return cc;
		}
	}
	
	public void removeActor(AkkaActorBackedCoreModelAbstractActor machine) {
		assert(machine != null);
		actorProvides.remove(machine);
		capProvidedBy.values().stream().forEach(capSet -> capSet.remove(machine));
	}
	
	public Optional<AkkaActorBackedCoreModelAbstractActor> resolveByAkkaActor(ActorRef actorRef) {
		return actorProvides.keySet().stream()
			.filter(a2a -> a2a.getAkkaActor().equals(actorRef))
			.findFirst();
	}
	
	public Optional<AkkaActorBackedCoreModelAbstractActor> resolveByModelActor(Actor modelActor) {
		return actorProvides.keySet().stream()
				.filter(a2a -> a2a.getModelActor().equals(modelActor))
				.findFirst();
	}
	
	
	public Set<AkkaActorBackedCoreModelAbstractActor> getMachinesProvidingCapability(AbstractCapability cap) {
		assert(cap != null);
		return getMachinesProvidingAllCapabilities(flatHierarchyToCapsWithNonNullId(cap).stream().collect(Collectors.toSet()));
	}
	
	public Set<AkkaActorBackedCoreModelAbstractActor> getMachinesProvidingAllCapabilities(Set<AbstractCapability> caps) {
		assert(caps != null);
		if (caps.isEmpty()) return Collections.emptySet();
		if (caps.size() == 1) { 
			return capProvidedBy.getOrDefault(caps.iterator().next(), Collections.emptySet());			
		} else { 
			return intersect(caps.stream()
								.map(cap -> capProvidedBy.getOrDefault(cap, Collections.emptySet())) // produces stream of sets
								.collect(Collectors.toList())
				);				
		}
	}
	
	private Set<AkkaActorBackedCoreModelAbstractActor> intersect(List<Set<AkkaActorBackedCoreModelAbstractActor>> sets) {
		assert(sets != null && !sets.isEmpty());
		Set<AkkaActorBackedCoreModelAbstractActor> intersectTo = new HashSet<AkkaActorBackedCoreModelAbstractActor>(sets.remove(0));
		sets.stream().forEach(actorSet -> intersectTo.retainAll(actorSet));		
		return intersectTo;
	}
}
