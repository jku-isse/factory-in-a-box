package fiab.mes.layoutAgent.msg;

public class ProcessNextImage {

    private final boolean repeat;

    public ProcessNextImage(final boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isRepeat() {
        return repeat;
    }
}
