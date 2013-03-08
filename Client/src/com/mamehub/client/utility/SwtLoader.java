package com.mamehub.client.utility;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import java.lang.reflect.Method;

public class SwtLoader {
	public static String getArchFilename(String prefix) 
	{ 
		return prefix + "_" + getOSName() + "_" + getArchName() + ".jar"; 
	} 

	private static String getOSName() 
	{ 
		String osNameProperty = System.getProperty("os.name"); 

		if (osNameProperty == null) 
		{ 
			throw new RuntimeException("os.name property is not set"); 
		} 
		else 
		{ 
			osNameProperty = osNameProperty.toLowerCase(); 
		} 

		if (osNameProperty.contains("win")) 
		{ 
			return "win"; 
		} 
		else if (osNameProperty.contains("mac")) 
		{ 
			return "osx"; 
		} 
		else if (osNameProperty.contains("linux") || osNameProperty.contains("nix")) 
		{ 
			return "linux"; 
		} 
		else 
		{ 
			throw new RuntimeException("Unknown OS name: " + osNameProperty); 
		} 
	} 

	private static String getArchName() 
	{ 
		String osArch = System.getProperty("os.arch"); 

		if (osArch != null && osArch.contains("64")) 
		{ 
			return "64"; 
		} 
		else 
		{ 
			return "32"; 
		} 
	}

	public static void addJarToClasspath(File jarFile) 
	{ 
		System.out.println("Loading " + jarFile.getAbsolutePath());
		try 
		{ 
			URL url = jarFile.toURI().toURL(); 
			URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader(); 
			Class<?> urlClass = URLClassLoader.class; 
			Method method = urlClass.getDeclaredMethod("addURL", new Class<?>[] { URL.class }); 
			method.setAccessible(true);         
			method.invoke(urlClassLoader, new Object[] { url });             
		} 
		catch (Throwable t) 
		{ 
			throw new RuntimeException(t);
		} 
	}

	public SwtLoader() {
		File swtJar = new File(getArchFilename("dynlib/swt")); 
		addJarToClasspath(swtJar);
	}
}
