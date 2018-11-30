package app;

import commons.MetadataNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static app.ApplicationServer.distributed_metadata_tree;

@Path("/api")
public class EntryPoint {

    JSONParser parser = new JSONParser();

    /**
     * Method that goes down the tree given the current absolute path for a client
     * @param path the current absolute path fo the client
     * @return the node where the client is at that moment
     */
    private MetadataNode goToCurrentNode(String path){

        List<String> pathSplit = new LinkedList<>(Arrays.asList(path.split("/")));
        MetadataNode node = distributed_metadata_tree.get_root();
        while(!pathSplit.isEmpty()){
            String next = pathSplit.remove(0);
            MetadataNode nextNode = node.get(next);
            if(nextNode != null && nextNode.isFolder()){
                node = nextNode;
            }
        }
        return node;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String process(InputStream incommingData) {

        StringBuilder stringBuilder = new StringBuilder();
        JSONObject jsonObject;
        String cmd = "";

        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(incommingData));
            String line;

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
     *
     * @param cmd is the command to execute, passed as a String (which is then split & processed).
     * @param currPath is the current path of the client.
     *
     * @return the result of that command, in String form.
     */
    private String exectuteCmd(String cmd,String currPath){
        String[] cmd_parted = cmd.split(" ");
        String res = "Could not execute the requested command!"; // default value

        switch (cmd_parted[0]) {
            case "ls":
                res = ls(currPath);
                break;
            case "pwd":
                res = pwd(currPath);
                break;
            case "cd":
                res = cd(cmd_parted[1],currPath);
                break;
            case "cat":
                res = cat(cmd_parted[1],currPath);
                break;
            case "mkdir":
                res = mkdir(cmd_parted[1],currPath);
                break;
            case "rmdir":
                res = rmdir(cmd_parted[1],currPath);
                break;
            case "test":
                res = test(currPath);
                break;
            case "echo":
                res = echo(cmd_parted[1]);
                break;
            default:
                // implementar > aqui
                if(cmd_parted.length == 3){
                    // file1's contents to file2 (create if not exists)
                    if(cmd_parted[1].equals(">"))
                        res = file2file(cmd_parted[0],cmd_parted[2],currPath);
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
     * @param currPath is the current absolutePath that the client encounters itself in.
     * @return the simplest version of ls (argumentless)
     */
    private String ls(String currPath){
        String res = "";
        MetadataNode node = goToCurrentNode(currPath);
        List<MetadataNode> children = node.getChildren();
        for(MetadataNode child: children){
            if(child.isFolder()){
                res += AppMisc.ANSI_BLUE + child.getName() + "/ " + AppMisc.ANSI_RESET ;
            } else{
                res += child.getName()+" ";
            }
        }

        return "path is: "+currPath+" and content is: "+res;
    }


    /**
     * Implementation of pwd command
     * @return the present work directory
     */
    private String pwd(String currPath){
        File currDir = new File(currPath);
        return currDir.getPath();
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

    /**
     * Implementation of cd command
     * returns new directory in case of success, error message else.
     *
     * @param folder the path to the folder
     * @param currDir the current directory
     * @return
     */
    private String cd(String folder, String currDir){
        MetadataNode currNode = goToCurrentNode(currDir);
        System.out.println("curr node has path: "+currNode.getPath());
        System.out.println("Current node is: "+currNode.getPath());
        MetadataNode nextNode;

        if(folder.charAt(0) == '/'){
            System.out.println("jumping to root");
            currNode = distributed_metadata_tree.get_root();
        }


        List<String> folderPathParted = new LinkedList<>(Arrays.asList(folder.split("/")));
        while(!folderPathParted.isEmpty()){
            String pathPiece = folderPathParted.remove(0);
            // cant go up the root
            if(pathPiece.equals("..") && currNode != distributed_metadata_tree.get_root()){
                System.out.println("jumping to parent: "+currNode.getParent().getPath());
                currNode = currNode.getParent();
            }
            // check the currNode for the next step
            else{
                nextNode =  currNode.get(pathPiece);
                if (nextNode.isFile())
                    return "That's a file...";
                if(nextNode == null || nextNode.isFile())
                    return "There is no folder named " +pathPiece+" under "+currNode.getPath()+".";
                else
                    currNode = nextNode;
            }
        }

        System.out.println("Client is now at "+currNode.getPath());
        return currNode.getPath();
    }

    /**
     * See "Serra de seteais for more details"
     * @param phrase
     * @return
     */
    private String echo(String phrase){
        return phrase;
    }

    /**
     * implementation of the ">" command.
     * @param file1 source
     * @param file2 destination
     * @param currPath the client's current path
     * @return
     */
    private String file2file(String file1, String file2, String currPath){
        /*
        *
        *
        * REFAZER
        *
        *
        * */
        String res = "";
        File currDir = new File(currPath);
        File f2 = new File(file2);
        if(!f2.exists()) {
            try {
                f2.createNewFile();
                System.out.println("f2 is in: "+f2.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
}