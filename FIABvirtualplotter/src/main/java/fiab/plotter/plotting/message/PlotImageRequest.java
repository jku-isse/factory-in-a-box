package fiab.plotter.plotting.message;

import fiab.core.capabilities.functionalunit.FURequest;

public class PlotImageRequest extends FURequest {

    private final String imageId;
    private final String orderId;

    //This message is used to draw an image by using an id
    public PlotImageRequest(String senderId, String imageId, String orderId) {
        super(senderId);
        this.imageId = imageId;
        this.orderId = orderId;
    }

    public String getImageId() {
        return imageId;
    }
}
