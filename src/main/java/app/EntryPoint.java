package app;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URLDecoder;

@Path("/api")
public class EntryPoint {

    JSONParser parser = new JSONParser();
    private File baseDir = new File(System.getProperty("user.dir"));

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String process(InputStream incommingData) {

        StringBuilder stringBuilder = new StringBuilder();
        JSONObject jsonObject;
        String cmd = "";

        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(incommingData));
            String line = null;

            while ((line = bufferedReader.readLine())!= null){
                stringBuilder.append(line);
            }

            jsonObject = (JSONObject) parser.parse(stringBuilder.toString());
            String decodedCmd = URLDecoder.decode((String) jsonObject.get("cmd"),"UTF-8");
            cmd = exectuteCmd(decodedCmd, (String) jsonObject.get("currPath"));

        }catch (Exception e){
            e.printStackTrace();
        }

        return cmd;
    }

    /**
     * This method executes the commands requested.
     * Allways test the commands inside the "testfolder" directory, so you don't erase important files by mistake.
     * @param cmd is the command to execute, passed as a string
     *            The string is then split.
     *            Following that we analyse the first word in the command, as most cases benefit from this.
     *            If the first word is recognized as an implemented function, it is run, taking in consideration
     *            the remaining parameters.
     *            If that fails, we enter a default case, where commands such as "<" and ">" can be detected,
     *            as well as bad/unimplemmented commands.
     * @return the result of that command, in String form.
     */
    private String exectuteCmd(String cmd,String currPath){
        String[] cmd_parted = cmd.split(" ");
        String res = "Could not execute the requested command!"; // default value (changes if cmd is successful)

        System.out.println("cmd is: ");
        for(String s:cmd_parted)
            System.out.println(s);
        System.out.println("path is: "+currPath);

        switch (cmd_parted[0]) {
            case "ls":
                res = ls(currPath+"/");
                break;
            case "pwd":
                res = pwd(currPath+"/");
                break;
            case "cd":
                res = cd(cmd_parted[1],currPath+"/");
                break;
            case "cat":
                res = cat(cmd_parted[1],currPath+"/");
                break;
            case "mkdir":
                System.out.println("nome: "+cmd_parted[1]+" path: "+currPath);
                res = mkdir(cmd_parted[1],currPath+"/");
                break;
            case "rmdir":
                res = rmdir(cmd_parted[1],currPath+"/");
                break;
            case "test":
                res = test(currPath+"/");
                break;
            case "echo":
                res = echo(cmd_parted[1]);
                break;
            default:
                // implementar > aqui
                if(cmd_parted.length == 3){
                    // file1's contents to file2 (create if not exists)
                    if(cmd_parted[1].equals(">"))
                        res = file2file(cmd_parted[0],cmd_parted[2],currPath+"/");
                }
                cmd = "";
                break;
        }

        return res;
    }

    /**
     * This is a test function, for debugging purposes.
     * @return
     */
    private String test(String currPath){
        return "";
    }

    /**
     * Implementation of mkdir.
     * Creates a folder in the given name.
     * @param newFoldername the name to create to the folder.
     * @return an empty String for success or an error message.
     */
    private String mkdir(String newFoldername, String currPath){
        String res = "";
        File newFolder = new File(currPath+newFoldername);
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

    /**
     * Implementation of rmdir.
     * Removes a folder if and only if it is empty.
     * @param folderName, the folder to remove
     * @return empty String for success, or an error message.
     */
    private String rmdir(String folderName, String currPath){
        String res = "";
        File currDir = new File(currPath);
        File target = new File(currPath+folderName);
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

    /**
     * Implementation of the ls command
     * @return the simplest version of ls (argumentless)
     */
    private String ls(String currPath){
        String res = "";
        File currDir = new File(currPath);
        for(File file:currDir.listFiles()){
            if(file.isDirectory()){
                res+= AppMisc.ANSI_BLUE+file.getName()+"/"+ AppMisc.ANSI_RESET +"\n";
            } else {
                res += file.getName()+" \n";
            }
        }
        return res;
    }

    /**
     * Implementation of pwd command
     * @return the present work directory
     */
    private String pwd(String currPath){
        File currDir = new File(currPath);
        return currDir.getName();
    }

    /**
     * Implementation of cat command
     * @param fileName, the path to the file we want to cat.
     * @return
     */
    private String cat (String fileName, String currDir){
        String res = "Content: \n";
        File targetFile = new File(currDir+fileName);
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

    private String cd(String folder, String currDir){
        return "";
    }

    private String echo(String phrase){
        return phrase;
    }

    private String file2file(String file1, String file2, String currPath){
        String res = "";
        File currDir = new File(currPath);
        File f2 = new File(file2);
        String contents = cat(file1,currPath);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file2, "UTF-8");
            writer.println(contents);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return res;
    }



    //https://stackoverflow.com/questions/33083397/filtering-upwards-path-traversal-in-java-or-scala
  /*  public java.nio.file.Path resolvePath(final java.nio.file.Path baseDirPath, final java.nio.file.Path userPath) {

        if (!baseDirPath.isAbsolute())
            throw new IllegalArgumentException("Base path must be absolute");

        if (userPath.isAbsolute())
            throw new IllegalArgumentException("User path must be relative");

        final java.nio.file.Path resolvedPath = baseDirPath.resolve(userPath).normalize();

        if (!resolvedPath.startsWith(baseDirPath))
            throw new IllegalArgumentException("User path escapes the base path");

        return resolvedPath;
    }*/

    /*


    @GET
    @Path(">")
    @Produces(MediaType.TEXT_PLAIN)
    public String redirectOutputTo() {
        return "";
    }*/

}