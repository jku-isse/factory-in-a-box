package fiab.mes.layoutAgent.msg;

import layoutTracker.utils.ImageSequenceGenerator;

public class ChangeImageSequenceSource {

    private final ImageSequenceGenerator imageSequenceGenerator;


    public ChangeImageSequenceSource(ImageSequenceGenerator imageSequenceGenerator) {
        this.imageSequenceGenerator = imageSequenceGenerator;
    }

    public ImageSequenceGenerator getImageSequenceGenerator() {
        return imageSequenceGenerator;
    }
}
