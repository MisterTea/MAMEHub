package com.mamehub.client;

import java.util.ArrayList;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class TestJoystick {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller[] ca = ControllerEnvironment.getDefaultEnvironment().getControllers();
		List<Controller> joysticks = new ArrayList<Controller>();

        for(int i =0;i<ca.length;i++){
        	Controller c = ca[i];
        	if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK) {
        		System.out.println("GOT A JOYSTICK: " + c.getName());
        		joysticks.add(c);
        	}
        }
        
        if(joysticks.size()>1) {
        	// warn about 2+ joysticks
        }
        
        
        while(true) {
        	for(Controller c : joysticks) {
        		c.poll();
        		Component[] components = c.getComponents();
        		for(Component component : components) {
        			if(component.isAnalog()) {
        			} else {
        			}
        		}
        	}
        }
	}

}
