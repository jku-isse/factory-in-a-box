package fiab.capabilityTool.opcua;

public class PlotCapability {

    public String plotCapability;

    public PlotCapability(){
        this.plotCapability = "";
    }

    public String getPlotCapability() {
        return plotCapability;
    }

    public void setPlotCapability(String plotCapability) {
        this.plotCapability = plotCapability;
    }

    @Override
    public String toString() {
        return "{\n" +
                "\"plotCapability=\"" +":\"" +plotCapability + "\"\n" +
                '}';
    }
}
