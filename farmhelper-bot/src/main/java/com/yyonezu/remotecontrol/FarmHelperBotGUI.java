
package com.yyonezu.remotecontrol;

import com.yyonezu.remotecontrol.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

public class FarmHelperBotGUI {
    private static JPanel infoPanel;

    public FarmHelperBotGUI() {
        Toolkit.getDefaultToolkit().getImage(FarmHelperBotGUI.class.getResource("/assets/rat.png"));
        JFrame frame = new JFrame("FarmHelper Bot GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width / 3, screenSize.height / 4);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLayout(new BorderLayout());

        JPanel imagePanel = new JPanel();
        imagePanel.setBackground(Color.WHITE);
        JLabel imageLabel = new JLabel(new ImageIcon(Objects.requireNonNull(FarmHelperBotGUI.class.getResource("/assets/farmhelper.png"))));
        imagePanel.add(imageLabel);
        frame.add(imagePanel, BorderLayout.NORTH);

        infoPanel = new JPanel();
        infoPanel.setBackground(Color.white); // Set panel background color
        JLabel infoLabel = new JLabel("The bot is not running :(");
        infoPanel.add(infoLabel); // Add the info label to the info panel
        frame.add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.white); // Set panel background color
        JButton startButton = new JButton("Start");
        buttonPanel.add(startButton);
        JButton stopButton = new JButton("Stop");
        buttonPanel.add(stopButton);
        JButton minimizeButton = new JButton("Minimize");
        buttonPanel.add(minimizeButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        minimizeButton.addActionListener(e -> {
            minimizeToTray(frame);
        });
        frame.addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent we) {
                if (we.getNewState() == Frame.ICONIFIED) {
                    minimizeToTray(frame);
                }
            }
        });

        startButton.addActionListener(e -> {
            setInfoText("The bot is starting... (Don't close this window, just minimize it)");
            Main.start();
        });

        stopButton.addActionListener(e -> {
            Main.stop();
            setInfoText("The bot is not running :(");
        });

        frame.setVisible(true);
    }


    public static void setInfoText(String text) {
        ((JLabel) infoPanel.getComponent(0)).setText(text);

    }


    private static void minimizeToTray(JFrame frame) {
        Utils.showNotification("Bot has been minimized to tray.");
        // Check if system tray is supported
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(FarmHelperBotGUI.class.getResource("/assets/rat.png")));
            TrayIcon trayIcon = new TrayIcon(image, "FarmHelper Bot");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                // Restore the window when tray icon is clicked
                frame.setVisible(true);
                frame.setExtendedState(JFrame.NORMAL);
                tray.remove(trayIcon);
            });

            try {
                tray.add(trayIcon);
                frame.setVisible(false); // Hide the frame
            } catch (AWTException ex) {
                System.err.println("Failed to add tray icon.");
                ex.printStackTrace();
            }
        } else {
            System.out.println("System tray is not supported.");
        }
    }
}