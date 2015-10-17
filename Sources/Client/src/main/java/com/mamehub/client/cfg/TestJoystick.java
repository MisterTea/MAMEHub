package com.mamehub.client.cfg;

import java.util.ArrayList;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

public class TestJoystick {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String path = System.getProperty("java.library.path");
    System.out.println(path);

    Controller[] ca = ControllerEnvironment.getDefaultEnvironment()
        .getControllers();
    List<Controller> joysticks = new ArrayList<Controller>();
    Controller keyboard = null;

    for (int i = 0; i < ca.length; i++) {
      Controller c = ca[i];
      if (c.getType() == Controller.Type.GAMEPAD
          || c.getType() == Controller.Type.STICK) {
        System.out.println("GOT A JOYSTICK: " + c.getName());
        joysticks.add(c);
      } else if (c.getType() == Controller.Type.KEYBOARD) {
        keyboard = c;
      }
    }

    if (joysticks.size() > 1) {
      // warn about 2+ joysticks
    }

    while (keyboard != null) {
      keyboard.poll();
      EventQueue queue = keyboard.getEventQueue();
      Event event = new Event();
      while (queue.getNextEvent(event)) {
        System.out.println(event.getComponent().getName() + " "
            + event.getValue());
      }
    }

    while (true) {
      for (Controller c : joysticks) {
        if (!c.poll()) {
          break;
        }
        Component[] components = c.getComponents();
        for (Component component : components) {
          System.out.println(component.getName() + " "
              + component.getIdentifier().getName() + " "
              + component.getPollData());
          if (component.isAnalog()) {
          } else {
          }
        }
        System.out.println("***");
      }
    }
  }

}
