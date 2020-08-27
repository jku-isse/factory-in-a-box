package fiab.capabilityTool.gui;

import akka.actor.AbstractActor;
import fiab.capabilityTool.opcua.CapabilityManagerClient;

import javax.swing.*;
import java.awt.*;

public class Connection extends AbstractActor {

    private final String url;
    
    @Override
    public Receive createReceive() {
        return null;
    }

    public Connection(String url){
        this.url = url;
        //TODO use client to communicate and update the panel
        //This class should be used by a controller that sends the update to the view
        //Controller should be able to dynamically generate as many instances as it wants
    }
    
    private JPanel createPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1));

        JButton readButton = new JButton("Read Value");
        JButton writeButton = new JButton("Write Value");
        JButton saveToFileButton = new JButton("Save to File");
        JButton disconnectButton = new JButton("Disconnect");

        buttonPanel.add(readButton);
        buttonPanel.add(writeButton);
        buttonPanel.add(saveToFileButton);
        buttonPanel.add(disconnectButton);
        
        JTextArea textArea = new JTextArea();
        panel.add(textArea, BorderLayout.EAST);
        panel.add(buttonPanel, BorderLayout.WEST);
        return panel;
    }
    
    
}
