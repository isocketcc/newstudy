package cn.mrcode.newstudy.javasetutorial.deployment;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.InvocationTargetException;

/**
 * Runs a jar application from any url. Usage is 'java JarRunner url [args..]'
 * where url is the url of the jar file and args is optional arguments to
 * be passed to the application's main method.
 * 从任何url运行一个jar应用程序。用法是“java JarRunner url args.。”
 * url是jar文件的url而args是可选的参数
 * 被传递到应用程序的主要方法。
 */
public class JarRunner {
    public static void main(String[] args) {
        args = new String[]{"http://maven.ibiblio.org/maven2/HTTPClient/HTTPClient/0.3-3/HTTPClient-0.3-3.jar"};
        if (args.length < 1) {
            usage();
        }
        URL url = null;
        try {
            url = new URL(args[0]);
        } catch (MalformedURLException e) {
            fatal("Invalid URL: " + args[0]);
        }
        // Create the class loader for the application jar file
        JarClassLoader cl = new JarClassLoader(url);
        // Get the application's main class name
        String name = null;
        try {
            name = cl.getMainClassName();
        } catch (IOException e) {
            System.err.println("I/O error while loading JAR file:");
            e.printStackTrace();
            System.exit(1);
        }
        if (name == null) {
            fatal("Specified jar file does not contain a 'Main-Class'" +
                          " manifest attribute");
        }
        // Get arguments for the application
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        // Invoke application's main class
        try {
            cl.invokeClass(name, newArgs);
        } catch (ClassNotFoundException e) {
            fatal("Class not found: " + name);
        } catch (NoSuchMethodException e) {
            fatal("Class does not define a 'main' method: " + name);
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            System.exit(1);
        }
    }

    private static void fatal(String s) {
        System.err.println(s);
        System.exit(1);
    }

    private static void usage() {
        fatal("Usage: java JarRunner url [args..]");
    }
}