package fiab.capabilityManager.opcua;

import java.util.Objects;

public class PlotCapability {

    public String endpointUrl;
    public String plotCapability;

    public PlotCapability() {
        this.endpointUrl = "";
        this.plotCapability = "";
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getPlotCapability() {
        return plotCapability;
    }


    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public void setPlotCapability(String plotCapability) {
        this.plotCapability = plotCapability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlotCapability that = (PlotCapability) o;
        return Objects.equals(endpointUrl, that.endpointUrl) &&
                Objects.equals(plotCapability, that.plotCapability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointUrl, plotCapability);
    }

    @Override
    public String toString() {
        return "PlotCapability{" +
                "url='" + endpointUrl + '\'' +
                ", plotCapability='" + plotCapability + '\'' +
                '}';
    }
}
