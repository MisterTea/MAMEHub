package com.mamehub.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import org.apache.commons.io.IOUtils;

public class SoundEngine implements LineListener {
  public static SoundEngine instance = null;

  Map<String, byte[]> soundFileMap = new HashMap<String, byte[]>();
  private long timeSinceLastBeep = 0L;

  MameHubEngine mameHubEngine = null;

  public SoundEngine() {
    List<String> soundResources = Utils.getResourcesWithPrefix("/sounds");
    for (String soundFile : soundResources) {
      if (soundFile.substring("/sounds/".length()).contains("/")) {
        continue;
      }
      if (soundFile.substring("/sounds/".length()).isEmpty()) {
        continue;
      }
      try {
        String name = soundFile;
        name = name.substring("/sounds/".length(), name.length() - 4).toLowerCase();
        System.out.println("SOUND FILE: " + soundFile);
        soundFileMap.put(name,
            IOUtils.toByteArray(Utils.getResourceInputStream(soundFile)));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    instance = this;
  }

  public void playSound(String filename) {
    if (Utils.isUnix()) {
      // java sound has issues on linux
      return;
    }
    try {
      AudioInputStream audioInputStream = AudioSystem
          .getAudioInputStream(new ByteArrayInputStream(soundFileMap
              .get(filename)));
      AudioFormat format = audioInputStream.getFormat();
      DataLine.Info info = new DataLine.Info(Clip.class, format);
      Clip clip = (Clip) AudioSystem.getLine(info);
      clip.open(audioInputStream);
      clip.start();
      clip.addLineListener(this);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void playSoundIfNotActive(String filename) {
    if (Utils.isUnix()) {
      // java sound has issues on linux
      return;
    }
    if (timeSinceLastBeep + 1000 * 60 * 15 <= System.currentTimeMillis() && // Only
                                                                            // play
                                                                            // non-active
                                                                            // sounds
                                                                            // every
                                                                            // 15
                                                                            // minutes
                                                                            // minimum.
        Utils.windowIsInactive(mameHubEngine)) {
      playSound(filename);
      timeSinceLastBeep = System.currentTimeMillis();
    }
  }

  @Override
  public void update(LineEvent event) {
    LineEvent.Type eventType = event.getType();
    if (eventType == LineEvent.Type.STOP || eventType == LineEvent.Type.CLOSE) {
      event.getLine().close();
    }
  }
}
