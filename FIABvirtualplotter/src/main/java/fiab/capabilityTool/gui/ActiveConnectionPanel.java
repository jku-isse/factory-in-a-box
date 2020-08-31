package fiab.capabilityTool.gui;

import javax.swing.*;
import java.awt.*;

public class ActiveConnectionPanel extends JPanel {

    private JTextField url;
    private JTextField capability;
    private JButton read;
    private JButton write;
    private JButton save;   //TODO add option?

    public ActiveConnectionPanel(String machineUrl) {
        super();
        setLayout(new GridLayout(2,2));
        initComponents(machineUrl);
    }

    private void initComponents(String machineUrl){
        this.url = new JTextField(machineUrl);
        this.url.setEnabled(false);
        this.capability = new JTextField();
        this.capability.setEnabled(false);
        this.read = new JButton("Read");
        this.write = new JButton("Write");
        this.add(url);
        this.add(capability);
        this.add(read);
        this.add(write);
    }

    public void setCapability(String capability){
        this.capability.setText(capability);
    }
}
