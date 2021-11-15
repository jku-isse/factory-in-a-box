package fiab.machine.foldingstation.opcua;

import fiab.core.capabilities.folding.WellknownFoldingCapability;

public class ExampleStartupFoldingStation {

    public static void main(String[] args) {
        StartupUtil.startup(0, "VirtualFoldingStation");
    }

}
