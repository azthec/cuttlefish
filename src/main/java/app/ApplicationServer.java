package app;

import commons.*;
import io.atomix.core.Atomix;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.List;

// after building shadowJar run with
// java -cp cuttlefish-1.0-SNAPSHOT-all.jar app.ApplicationServer

public class ApplicationServer {

    static Loader loader;
    static List<String> servers;
    static AtomixUtils atomixUtils;
    static Atomix atomix;
    static MetadataTree distributed_metadata_tree;
    static CrushMap crushMap;
    static FileChunkUtils fileChunkUtils;

    /**
     * Method that prevents vars from being null.
     * Also acts as 1st time setup script.
     */
    private static void checkVars(){
        System.out.println("------------------------------------");
        if(loader == null){
            loader = new Loader();
        }

        if (crushMap == null){
            crushMap = loader.sample_crush_map();
        }

        if(servers == null){
            System.out.println("Populating servers' name list...");
            servers = loader.loadServerNames();
            System.out.println("Populated servers' name list.");
        }

        if(atomixUtils == null){
            System.out.println("AtomixUtils is null, fixing...");
            atomixUtils = new AtomixUtils();
            System.out.println("Fixed Atomix Utils.");
        }

        if(atomix == null){
            System.out.println("Atomix is null, fixing...");
            //atomix = atomixUtils.getServer("Acuña", "88.4.20.69", 8888, servers).join();
            System.out.println("Fixed Atomix.");
        }

        if(distributed_metadata_tree == null){
            System.out.println("Fetching distributed metadata tree...");
            distributed_metadata_tree = loader.sample_metadata_tree();
            System.out.println("Got distributed metadata tree.");
        }

        System.out.println("------------------------------------");
    }

    public static void main(String[] args) throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Server jettyServer = new Server(8080);
        jettyServer.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", EntryPoint.class.getCanonicalName());
        checkVars();

        try {
            jettyServer.start();
            jettyServer.join();
        } finally {
            jettyServer.destroy();
        }
    }
}
