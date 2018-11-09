package app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.nio.file.Paths;

@Path("/api")
public class EntryPoint {

    File baseDir = new File(System.getProperty("user.dir"));

    @GET
    @Path("/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    public String process(@PathParam("param") String cmd) {
        return exectuteCmd(cmd);
    }

    private String exectuteCmd(String cmd){
        String[] cmd_parted = cmd.split(" ");
        String res="";
        switch (cmd_parted[0]){
            case "ls":
                res += ls();
                break;
            case "pwd":
                res += pwd();
                break;
            case "test":
                res += test();
                break;
        }
        return res;
    }

    private String test(){
        return baseDir.getName();
    }

    private String ls(){
        String res = "";
        for(File file:baseDir.listFiles()){
            if(file.isDirectory()){
                res+= file.getName()+"/ ";
            } else {
               res += file.getName()+" ";
            }
        }
        return res;
    }

    private String pwd(){
          return null;
    }

    //https://stackoverflow.com/questions/33083397/filtering-upwards-path-traversal-in-java-or-scala
    public java.nio.file.Path resolvePath(final java.nio.file.Path baseDirPath, final java.nio.file.Path userPath) {

        if (!baseDirPath.isAbsolute())
            throw new IllegalArgumentException("Base path must be absolute");

        if (userPath.isAbsolute())
            throw new IllegalArgumentException("User path must be relative");

        final java.nio.file.Path resolvedPath = baseDirPath.resolve(userPath).normalize();

        if (!resolvedPath.startsWith(baseDirPath))
            throw new IllegalArgumentException("User path escapes the base path");

        return resolvedPath;
    }

    /*
    @GET
    @Path("cd")
    @Produces(MediaType.TEXT_PLAIN)
    public String cd() {
        return "";
    }

    @GET
    @Path("ls")
    @Produces(MediaType.TEXT_PLAIN)
    public String ls() {
        return "";
    }

    @GET
    @Path("pwd")
    @Produces(MediaType.TEXT_PLAIN)
    public String pwd() {
        return "";
    }

    @GET
    @Path("cat")
    @Produces(MediaType.TEXT_PLAIN)
    public String cat() {
        return "";
    }

    @GET
    @Path("mkdir")
    @Produces(MediaType.TEXT_PLAIN)
    public String mkdir() {
        return "";
    }

    @GET
    @Path("rmdir")
    @Produces(MediaType.TEXT_PLAIN)
    public String rmdir() {
        return "";
    }

    @GET
    @Path("echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echo() {
        return "";
    }

    @GET
    @Path(">")
    @Produces(MediaType.TEXT_PLAIN)
    public String redirectOutputTo() {
        return "";
    }*/

}