package fiab.capabilityTool.gui;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import fiab.capabilityTool.gui.msg.ClientReadyNotification;
import fiab.capabilityTool.gui.msg.ReadNotification;
import fiab.capabilityTool.gui.msg.ReadRequest;
import fiab.capabilityTool.gui.msg.WriteRequest;
import fiab.capabilityTool.opcua.CapabilityManagerClient;
import fiab.capabilityTool.opcua.PlotCapability;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CapabilityManagerUI extends AbstractActor {

    private final String title;
    private JTextArea textArea;
    private JButton submitButton;
    private JButton readButton;
    private ActorRef client;
    private URL pathToJsonFile;
    private JFrame frame;
    private JPanel buttonPanel;

    private Map<String, ActorRef> activeConnections;

    public static Props props(String title) {
        return Props.create(CapabilityManagerUI.class, title);
    }

    public CapabilityManagerUI(String title) throws HeadlessException {
        this.title = title;
        client = context().actorOf(CapabilityManagerClient.props("opc.tcp://localhost:4840/milo"));
        activeConnections = new HashMap<>();
        SwingUtilities.invokeLater(() -> showGUI(title));
        try {
            pathToJsonFile = Objects.requireNonNull(getClass().getClassLoader()
                    .getResource("plotterCapabilityExample.json")).toURI().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }
        /*frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setTitle(title);
        addTextField();
        addReadButton();
        addSubmitButton();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);*/
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ReadNotification.class, notification -> {
                    //updateTextArea(notification.getValue());
                })
                .build();
    }

    private void showGUI(String title) {
        frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setTitle(title);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 0));
        frame.add(buttonPanel, BorderLayout.SOUTH);
        /*addTextField();
        addReadButton();
        addSubmitButton();
        addWriteButton();*/
        addConnectToMachineMenu();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void addConnectToMachineMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Machine");
        JMenuItem connectItem = new JMenuItem();
        connectItem.setText("Connect new Machine");
        connectItem.addActionListener(l -> {
            String url = JOptionPane.showInputDialog("Enter Machine url");
            //TODO validate
            ActorRef actorRef = getContext().actorOf(ActiveConnectionPanelActor.props(url));
            activeConnections.putIfAbsent(url, actorRef);
        });
        menu.add(connectItem);
        menuBar.add(menu);
        frame.add(menuBar, BorderLayout.NORTH);
    }

    public void addTextField() {
        textArea = new JTextArea();
        InputStream example = getClass().getClassLoader().getResourceAsStream("plotterCapabilityExample.json");
        if (example != null) {
            textArea.append(new BufferedReader(new InputStreamReader(example)).lines().collect(Collectors.joining("\n")));
        }
        textArea.setPreferredSize(new Dimension(600, 800));
        frame.add(textArea, BorderLayout.NORTH);
    }

    public void addReadButton() {
        readButton = new JButton("Read Remote Value");
        buttonPanel.add(readButton);
        readButton.addActionListener(event -> {
            client.tell(new ReadRequest(), self());
        });
    }

    public void addWriteButton() {
        JButton writeButton = new JButton("Write Remote value");
        buttonPanel.add(writeButton);
        writeButton.addActionListener(event -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                PlotCapability plotCapability = mapper.readValue(textArea.getText(), PlotCapability.class);
                client.tell(new WriteRequest(plotCapability.getPlotCapability()), self());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateTextArea(String capabilityValue) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            PlotCapability capability = mapper.readValue(pathToJsonFile, PlotCapability.class);
            capability.setPlotCapability(capabilityValue);
            //mapper.writerWithDefaultPrettyPrinter().writeValue(new File(pathToJsonFile.toURI()), new PlotCapability());
            textArea.setText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(capability));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSubmitButton() {
        submitButton = new JButton("Save File");
        buttonPanel.add(submitButton);
        submitButton.addActionListener(event -> {
            Path path = new File(getClass().getClassLoader().getResource("plotterCapabilityExample.json").getFile()).toPath();
            String text = textArea.getText();
            try {
                Files.write(path.toAbsolutePath(), text.getBytes(), StandardOpenOption.WRITE);
                System.out.println("Successfully Written file: " + path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
