package com.mamehub.client;

import java.awt.KeyboardFocusManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import org.apache.commons.io.FileUtils;

public class SoundEngine implements LineListener {
	public static SoundEngine instance = null;

	Map<String, byte[]> soundFileMap = new HashMap<String,byte[]>();
	private long timeSinceLastBeep = 0L;

	MameHubEngine mameHubEngine = null;

	public SoundEngine() {
		File soundDir = new File("sounds");
		for(File soundFile : soundDir.listFiles()) {
			if(soundFile.isFile()==false) {
				continue;
			}
			try {
				String name = soundFile.getName();
				name = name.substring(0, name.length()-4).toLowerCase();
				soundFileMap.put(name, FileUtils.readFileToByteArray(soundFile));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		instance = this;
	}

	public void playSound(String filename) {
		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(soundFileMap.get(filename)));
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			clip.start();
			clip.addLineListener(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void playSoundIfNotActive(String filename) {
		if(timeSinceLastBeep+1000*60*15 <= System.currentTimeMillis() && // Only play non-active sounds every 15 minutes minimum.
			KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null && !mameHubEngine.isGameRunning()) {
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
