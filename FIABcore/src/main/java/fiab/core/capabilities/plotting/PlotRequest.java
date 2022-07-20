package fiab.core.capabilities.plotting;

import fiab.core.capabilities.functionalunit.FURequest;

public class PlotRequest extends FURequest {

    private final String imageId;

    public PlotRequest(String senderId, String imageId) {
        super(senderId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }
}
