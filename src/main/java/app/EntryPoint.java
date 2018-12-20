package app;

import commons.FileChunkUtils;
import commons.MetadataNode;
import commons.ObjectStorageNode;
import exceptions.InvalidNodeException;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static app.ApplicationServer.*;


@Path("/api")
public class EntryPoint {

    JSONParser parser = new JSONParser();

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
     * @param cmd is the command to execute, passed as a String (which is then split & processed).
     * @param currPath is the current path of the client.
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
     * @return the test functions output
     */
    private String test(String currPath){
        return "";
    }

    /**
     * TODO use metadata tree asap
     * @param newFoldername the name to create to the folder.
     * @return an empty String for success or an error message.
     */
    private String mkdir(String newFoldername, String currPath){
        String res = "";
        MetadataNode currNode = distributed_metadata_tree.goToNode(currPath);
        MetadataNode newNode = distributed_metadata_tree.goToNode(currNode,newFoldername);
        if(newNode != null){
            if(newNode.isFolder())
                res = "That folder already exists...";
            else if (newNode.isFile())
                res = "That's an already existing file...";
        } else if (newNode == null){
            // TODO insert the comms with the OSD's to create a folder here.
            //currNode.addFolder(newFoldername);
            byte[] newDirBytes = FileChunkUtils.fileToByteArray(new File(newFoldername));

            // any write operations require communication with the primary OSD
            if(FileChunkUtils.post_object("oid",newDirBytes,loader.sample_crush_map()))
                currNode.addFolder(newFoldername);
            else
                res = "Couldn't create the folder...";
        }
        return res;
    }

    /**
     * TODO the remove part has to be implemented yet! also review
     * Removes a folder if and only if it is empty.
     * @param folderName, the folder to remove
     * @return empty String for success, or an error message.
     */
    private String rmdir(String folderName, String currPath){
        String res = "";

        List<String> pathParted = new ArrayList<>(Arrays.asList(folderName.split("/")));

        if(folderName.charAt(0)=='/'){
            String fname = distributed_metadata_tree.goToNode(folderName).getName();

            MetadataNode currNode = distributed_metadata_tree.goToNode(currPath);
            MetadataNode parentOfFolder = distributed_metadata_tree.goToNode(folderName).getParent();
            MetadataNode nodeToDelete = parentOfFolder.get(fname);

            if(nodeToDelete.isFile())
                return "That's a file...";
            else if(nodeToDelete.isFolder() && !nodeToDelete.isLeaf())
                return "That folder is not empty...";
            else if(nodeToDelete == currNode)
                return "Can't delete the folder you are in...";
            else{
                parentOfFolder.remove(fname);
                if(nodeToDelete == null)
                    return "";
                else
                    return "Couldn't delete the folder...";
            }
        }
        // case when a relative path is given
        else{
            MetadataNode currNode = distributed_metadata_tree.goToNode(currPath);
            MetadataNode targetNode = distributed_metadata_tree.goToNode(currNode,folderName);
            MetadataNode parentOfTarget = distributed_metadata_tree.goToNode(currNode,folderName).getParent();
            if(targetNode == null)
                return  "That doens't exist";
            else if(targetNode.isFile())
                return "That's a file...";
            else if(targetNode.isFolder() && !targetNode.isLeaf())
                return  "Folder's not empty...";
            else if(targetNode.isFolder() && targetNode.isLeaf()){
                parentOfTarget.remove(targetNode.getName());
                if(targetNode == null)
                    return "";
                else
                    return "Couldn't delete the folder";
            }
        }
        return res;
    }

    /**
     * TODO review correctness fo method
     * @param currPath is the current absolutePath that the client encounters itself in.
     * @return the simplest version of ls (argumentless)
     */
    private String ls(String currPath){
        String res = "";
        MetadataNode node = distributed_metadata_tree.goToNode(currPath);
        List<MetadataNode> children = node.getChildren();
        for(MetadataNode child: children){
            if(child.isFolder()){
                res += AppMisc.ANSI_BLUE + child.getName() + "/ " + AppMisc.ANSI_RESET ;
            } else{
                res += child.getName()+" ";
            }
        }

        return res;
    }

    /**
     * @return the present work directory
     */
    private String pwd(String currPath){
        MetadataNode currNode  = distributed_metadata_tree.goToNode(currPath);
        return currNode.getPath();
    }

    /**
     * TODO check correctness
     * Implementation of cat command
     * @param fileName, the path to the file we want to cat.
     * @param currDir, the current directory (file is inside currdir)
     * @return
     */
    private String cat (String fileName, String currDir){
        String res = "";
        MetadataNode currNode = distributed_metadata_tree.goToNode(currDir);
        MetadataNode childNode = currNode.get(fileName);
        if(childNode != null && childNode.isFile()){
            System.out.println("cat function is currently broken!");
            // TODO update fileBytes and getFileContent to use get_file and byteArraysToFile from FileChunkUtils
//            byte[] fileBytes = FileChunkUtils.get_file(childNode.getPath(),crushMap,distributed_metadata_tree);
//            res = getFileContent(fileBytes);
        }
        else{
            if(childNode == null)
                res = "The file doesn't exist.";
            else if (childNode != null && childNode.isFolder())
                res = "That's a folder...";
        }
        return res;
    }

    /**
     * TODO check correctness
     * Helper method to cat, gets file content by "translating"
     * bytes to a file, opening the file, getting the content, deleting the file and
     * returning the content.
     * @param fileBytes
     * @return
     */
    private String getFileContent(byte[] fileBytes){
        String res = "";
        try {
            File f1 = new File("tmpFile");
            FileUtils.writeByteArrayToFile(new File("tmpFile"), fileBytes);
            BufferedReader br = new BufferedReader(new FileReader(f1));
            String st;
            while ((st = br.readLine()) != null)
               res += st +"\n";
            br.close();
            f1.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     *
     * TODO test correctness
     * Implementation of cd command
     * returns new directory in case of success, error message else.
     *
     * @param folder the path to the folder
     * @param currDir the current directory
     * @return
     */
    private String cd(String folder, String currDir){
        MetadataNode currNode = distributed_metadata_tree.goToNode(currDir);
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
     * TODO use metadata tree as soon as chunk stuff is working
     * implementation of the ">" command.
     * Assuming that both file1 and file2 are under the currdir
     * @param file1 source
     * @param file2 destination
     * @param currPath the client's current path
     * @return
     */
    private String file2file(String file1, String file2, String currPath){

        String res = "";
        Boolean operation = false;
        try {
            operation = FileChunkUtils.copyChunks(file1, file2, crushMap, distributed_metadata_tree);
        } catch (InvalidNodeException e) {
            System.out.println("Exception:");
            e.printStackTrace();
        }

        if(!operation)
            res = "Check for any InvalidNodeException(s), redirect failed";

        return res;
    }
}