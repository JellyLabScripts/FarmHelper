package com.jelly.farmhelper.nickeh.thirty;


import net.minecraftforge.event.world.NoteBlockEvent;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayMedia {
    JFrame frameLoader = new JFrame();

    public void showLoader()  {

        Runnable task = () -> {
            frameLoader.dispose();

        };
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();;
        try {
            
        Icon icon = new ImageIcon(this.getClass().getResource("/gifs/nick30.gif"));
        JLabel label = new JLabel(icon);

        frameLoader.setUndecorated(true);
        frameLoader.getContentPane().add(label);
        frameLoader.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameLoader.pack();
        frameLoader.setLocationRelativeTo(null);
        frameLoader.setVisible(true);
        frameLoader.setAlwaysOnTop(true);
        scheduler.schedule(task, 28, TimeUnit.SECONDS );
        }

        catch(Exception q) {
            q.printStackTrace();
        }
        scheduler.shutdown();
    }

}
