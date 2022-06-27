package fiab.core.capabilities.functionalunit;

public abstract class FURequest {

    private final String senderId;

    public FURequest(String senderId){
        this.senderId = senderId;
    }

    public String getSenderId() {
        return senderId;
    }
}
