package app;

import org.glassfish.jersey.jaxb.internal.XmlJaxbElementProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.file.Paths;

@Path("/api")
public class EntryPoint {

    File baseDir = new File(System.getProperty("user.dir"));
    File currDir = new File(System.getProperty("user.dir"));


    @GET
    @Path("/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    public String process(@PathParam("param") String cmd) {
        return exectuteCmd(cmd);
    }

    private String exectuteCmd(String cmd){
        String[] cmd_parted = cmd.split(" ");
        String res="";

        switch (cmd_parted[0]) {
            case "ls":
                res = ls();
                break;
            case "pwd":
                res = pwd();
                break;
            case "cd":
                res = cd(cmd_parted[1]);
                break;
            case "cat":
                res = cat(cmd_parted[1]);
                break;
            case "mkdir":
                System.out.println("mkdir with foldername: " + cmd_parted[1]);
                res = mkdir(cmd_parted[1]);
                break;
            case "rmdir":
                System.out.println("rmdir on: " + cmd_parted[1]);
                res = rmdir(cmd_parted[1]);
                break;
            case "test":
                res = test();
                break;
            default:
                // implementar > aqui
                if(cmd_parted.length == 3){
                    System.out.println("Redirecting "+cmd_parted[0]+" to "+cmd_parted[2]);
                }
                res = "Could not execute the requested command!";
                break;
        }

        return res;
    }

    private String test(){
        return baseDir.getName();
    }

    private String mkdir(String newFolderPath){
        String res = "";
        File newFolder = new File(newFolderPath);
        if (!newFolder.exists()) {
            boolean result = false;
            try{
                newFolder.mkdir();
                result = true;
            }
            catch(SecurityException se){
                se.printStackTrace();
            }
            if(!result) {
                System.out.println("1: "+res);
                res = "Could not create folder, check your permissions...";
            }
        } else{
            System.out.println("2: "+res);
            res = "That folder already exists...";
        }
        System.out.println("3: "+res);
        return res;
    }

    private String rmdir(String folderPath){
        String res = "";
        File target = new File(folderPath);
        if(!target.exists() || target.exists() && !target.isDirectory())
            res = "Folder doesn't exist or isn't a directory...";
        else if(target.exists() && target.isDirectory()){
            if(target.listFiles().length == 0){
                if(!target.getAbsolutePath().equals(currDir.getPath())){
                    if(!target.delete()){
                        res="Couldn't delete the folder, check your permissions...";
                    }
                }
            } else{
                res = "The folder isn't empty...";
            }
        }
        return res;
    }

    private String ls(){
        String res = "";
        for(File file:baseDir.listFiles()){
            if(file.isDirectory()){
                res+= AppMisc.ANSI_BLUE+file.getName()+"/"+ AppMisc.ANSI_RESET +"\n";
            } else {
                res += file.getName()+" \n";
            }
        }
        return res;
    }

    private String pwd(){
        return currDir.getName();
    }

    private String cat (String filePath){
        String res = "Content: \n";
        File targetFile = new File(filePath);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile));
            String s;
            while((s=bufferedReader.readLine()) != null){
                res += s+"\n";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(res);
        return res;
    }

    private String cd(String folder){
        return "";
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