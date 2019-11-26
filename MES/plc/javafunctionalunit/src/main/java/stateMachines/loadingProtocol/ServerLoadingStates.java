package stateMachines.loadingProtocol;
/**
 [Class description.  The first sentence should be a meaningful summary of the class since it
 will be displayed as the class summary on the Javadoc package page.]

 [Other notes, including guaranteed invariants, usage instructions and/or examples, reminders
 about desired improvements, etc.]
 @author Michael Bishara
 @author <A HREF="mailto:[michaelbishara14@gmail.com]">[Michael Bishara]</A>
 @author <A HREF="https://github.com/michaelanis14">[Github]</A>
 @date 11 Sep 2019
 **/
public enum ServerLoadingStates {
    RESETTING,IDLE_EMPTY,IDLE_LOADED, STARTING, PREPARING, READY_LOADED,READY_EMPTY,EXECUTE,COMPLETING, COMPLETED, STOPPING, STOPPED

}
