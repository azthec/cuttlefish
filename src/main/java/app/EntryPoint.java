package app;

import commons.FileChunkUtils;
import commons.FileMetadataUtils;
import commons.MetadataNode;
import commons.MetadataTree;
import io.atomix.core.lock.DistributedLock;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static app.ApplicationServer.*;


@Path("/api")
public class EntryPoint {

    JSONParser parser = new JSONParser();
    DistributedLock lock = atomix.getLock("metaLock");

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String process(InputStream incommingData) {
        System.out.println("Incomming data...");
        StringBuilder stringBuilder = new StringBuilder();
        JSONObject jsonObject;
        String cmd = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(incommingData));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            jsonObject = (JSONObject) parser.parse(stringBuilder.toString());
            String decodedCmd = URLDecoder.decode((String) jsonObject.get("cmd"), "UTF-8");
            System.out.println("Command received: " + decodedCmd);
            byte[] bytes = null;

            if(jsonObject.get("bytes") != null){
                bytes = jsonObject.get("bytes").toString().getBytes();
                System.out.println("bytes::: "+ new String(bytes, StandardCharsets.UTF_8));
            }


            cmd = exectuteCmd(decodedCmd, (String) jsonObject.get("currPath"), bytes);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cmd;
    }

    /**
     * @param cmd      is the command to execute, passed as a String (which is then split & processed).
     * @param currPath is the current path of the client.
     * @return the result of that command, in String form.
     */
    private String exectuteCmd(String cmd, String currPath,byte[] bytes) {
        String[] cmd_parted = cmd.split(" ");
        String res = "Could not execute the requested command!"; // default value

        switch (cmd_parted[0]) {
            case "cplr":
                res = copyfileLR(cmd_parted[2], bytes, currPath);
                break;
            case "cprr":
                res = file2file(absolutify(cmd_parted[1], currPath), cmd_parted[2], currPath, lock);
                break;
            case "infofile":
                res = infofile(currPath,cmd_parted[1]);
                break;
            case "ls":
                res = ls(currPath);
                break;
            case "pwd":
                res = pwd(currPath);
                break;
            case "cd":
                res = cd(cmd_parted[1], currPath);
                break;
            case "cat":
                res = cat(cmd_parted[1], currPath); // not adding lock for now
                break;
            case "mkdir":
                res = mkdir(cmd_parted[1], currPath, lock); // changed to have lock
                break;
            case "rmdir":
                res = rmdir(absolutify(cmd_parted[1], currPath), currPath, lock); // changed to have lock
                break;
            case "test":
                res = test(currPath, lock); // does not have lock (add if need be)
                break;
            case "echo":
                res = echo(cmd_parted);
                break;
            default:
                // implementar > aqui
                if (cmd_parted.length == 3) {
                    if (cmd_parted[1].equals(">"))
                        res = file2file(absolutify(cmd_parted[0], currPath), cmd_parted[2], currPath, lock);
                }
                else
                    res = "That command does not exist...";
                break;
        }

        return res;
    }

    private String absolutify(String fileName, String currPath) {
        MetadataTree tree = distributed_metadata_tree.get();
        MetadataNode currNode = tree.goToNode(currPath);
        MetadataNode childNode;

        if(fileName.startsWith("/"))
            childNode = tree.goToNodeIfNotDeleted(fileName);
        else
            childNode = currNode.get(fileName);

        if(childNode == null)
            return "Node " + fileName + " does not exist!";
        System.out.println("absolutify: " + childNode.getPath());
        return childNode.getPath();
    }


    private String copyfileLR(String remotePath, byte[] bytes, String currPath){
        if (!remotePath.startsWith("/")) {
            remotePath = currPath + remotePath;
        }

        String res = "failed";
        try {
            if(FileChunkUtils.post_bytes(bytes,
                    remotePath,
                    distributed_crush_maps.get(distributed_crush_maps.size()-1),
                    distributed_metadata_tree,lock))
                res = "success";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * This is a test function, for debugging purposes.
     *
     * @return the test functions output
     */
    private String test(String currPath, DistributedLock lock) {
        return "";
    }

    /**
     * Gets information about a file:
     *  -if completely downloaded,
     *  -full path,
     *  -size;
     *  -if being downloaded: file size and neighbor list
     * @param currPath the current path the client is in (this implementation assumes the file is also under this path)
     * @param filename the name of the file
     * @return a string as a response, in both positive and negative cases
     */
    private String infofile(String currPath, String filename){
        MetadataTree tree = distributed_metadata_tree.get();
        System.out.println("CURRPATH IS: "+currPath);
        MetadataNode node = tree.goToNode(currPath+filename);
        String res = "";
        if (node.isFile()){
            if(node != null){
                res += "\t full path: "+node.getPath()+"\n";
                res += "\t file size: "+(node.getNumberOfChunks())+"MB \n";
                res += "\t file is completely downloaded";
            } else{
                res = "There is no such file";
            }
        } else{
            res = "Can't do infofile on a folder...";
        }
        return res;
    }


    /**
     * @param newFoldername the name to create to the folder.
     * @return an empty String for success or an error message.
     */
    private String mkdir(String newFoldername, String currPath, DistributedLock lock) {
        if (newFoldername.contains("/"))
            return "Name must not contain /";
        return FileMetadataUtils.createRemoteDirectory(newFoldername, currPath,
                distributed_metadata_tree, lock);
    }

    /**
     * TODO the remove part has to be implemented yet! also review
     * Removes a folder if and only if it is empty.
     *
     * @param folderName, the folder to remove
     * @return empty String for success, or an error message.
     */
    private String rmdir(String folderName, String currPath, DistributedLock lock) {
        return FileMetadataUtils.deleteRemote(folderName, currPath, MetadataNode.FOLDER,
                distributed_crush_maps, distributed_metadata_tree, lock);
    }

    /**
     * TODO review correctness fo method
     *
     * @param currPath is the current absolutePath that the client encounters itself in.
     * @return the simplest version of ls (argumentless)
     */
    private String ls(String currPath) {
        String res = "";
        MetadataNode node = distributed_metadata_tree.get().goToNode(currPath);
        List<MetadataNode> children = node.getChildren();
        for (MetadataNode child : children) {
            if(!child.isDeleted()){
                if (child.isFolder()) {
                    res += AppMisc.ANSI_BLUE + child.getName() + "/ " + AppMisc.ANSI_RESET;
                } else {
                    res += child.getName() + " ";
                }
            }
        }
        return res;
    }

    /**
     * @return the present work directory
     */
    private String pwd(String currPath) {
        MetadataNode currNode = distributed_metadata_tree.get().goToNode(currPath);
        return currNode.getPath();
    }

    /**
     * TODO check correctness
     * Implementation of cat command
     *
     * @param fileName, the path to the file we want to cat.
     * @param currDir,  the current directory (file is inside currdir)
     * @return
     */
    private String cat(String fileName, String currDir) {
        String res = "";
        MetadataTree tree = distributed_metadata_tree.get();
        MetadataNode currNode = tree.goToNode(currDir);
        MetadataNode childNode;

        if(fileName.startsWith("/"))
            childNode = tree.goToNodeIfNotDeleted(fileName);
        else
            childNode = currNode.get(fileName);

        if(childNode == null)
            return "Node " + fileName + " does not exist!";


        if (childNode != null && childNode.isFile()) {
            System.out.println("cat function is currently broken!");
            byte[][] fileBytes =  FileChunkUtils.get_file(childNode.getPath(),distributed_crush_maps.get(distributed_crush_maps.size()-1),tree);
            File target = new File("tmpFile");
            boolean success = FileChunkUtils.byteArraysToFile(fileBytes,target);
            if(success){
                try {
                    BufferedReader br = null;
                    br = new BufferedReader(new FileReader(target));
                    String st;
                    while ((st = br.readLine()) != null)
                        res += st+"\n";
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                res = "Couldn't read the file";
                target.delete();
                return res;
            }
        } else {
            if (childNode == null)
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
     *
     * @param fileBytes
     * @return
     */
    private String getFileContent(byte[] fileBytes) {
        String res = "";
        try {
            File f1 = new File("tmpFile");
            FileUtils.writeByteArrayToFile(new File("tmpFile"), fileBytes);
            BufferedReader br = new BufferedReader(new FileReader(f1));
            String st;
            while ((st = br.readLine()) != null)
                res += st + "\n";
            br.close();
            f1.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * TODO test correctness
     * Implementation of cd command
     * returns new directory in case of success, error message else.
     *
     * @param folder  the path to the folder
     * @param currDir the current directory
     * @return
     */
    private String cd(String folder, String currDir) {
        MetadataNode currNode = distributed_metadata_tree.get().goToNode(currDir);
        System.out.println("curr node has path: " + currNode.getPath());
        System.out.println("Current node is: " + currNode.getPath());
        MetadataNode nextNode;

        if (folder.charAt(0) == '/') {
            System.out.println("jumping to root");
            currNode = distributed_metadata_tree.get().get_root();
        }


        List<String> folderPathParted = new LinkedList<>(Arrays.asList(folder.split("/")));
        while (!folderPathParted.isEmpty()) {
            String pathPiece = folderPathParted.remove(0);
            // cant go up the root
            if (pathPiece.equals("..") && currNode != distributed_metadata_tree.get().get_root()) {
                System.out.println("jumping to parent: " + currNode.getParent().getPath());
                currNode = currNode.getParent();
            }
            // check the currNode for the next step
            else {
                nextNode = currNode.get(pathPiece);
                if (nextNode.isFile())
                    return "That's a file...";
                if (nextNode == null || nextNode.isFile())
                    return "There is no folder named " + pathPiece + " under " + currNode.getPath() + ".";
                else
                    currNode = nextNode;
            }
        }

        System.out.println("Client is now at " + currNode.getPath());
        return currNode.getPath();
    }

    /**
     * See "Serra de seteais for more details"
     *
     * @param phrase
     * @return
     */
    private String echo(String[] phrase) {
        String res = "";
        if (phrase.length > 1) {
            for (int i = 1; i < phrase.length; i++)
                res += phrase[i] + " ";
        }
        return res;
    }

    /**
     * TODO use metadata tree as soon as chunk stuff is working
     * implementation of the ">" command.
     * Assuming that both file1 and file2 are under the currdir
     *
     * @param file1    source
     * @param file2    destination
     * @param currPath the client's current path
     * @return
     */
    private String file2file(String file1, String file2, String currPath, DistributedLock lock) {

        String res;

        MetadataTree t1 = distributed_metadata_tree.get();
        MetadataNode currNode = t1.goToNode(currPath);
        MetadataNode n1;
        String sn2;

        if(file1.startsWith("/"))
            n1 = t1.goToNodeIfNotDeleted(file1);
        else
            n1 = currNode.get(file1);

        if(file2.startsWith("/"))
            sn2 = file2;
        else
            sn2 = currNode.getPath() + file2;

        if(n1 == null)
            return "Node " + file1 + " does not exist!";



        if(FileChunkUtils.copyFile(n1.getPath(), sn2, file2, distributed_crush_maps.get(distributed_crush_maps.size()-1), distributed_metadata_tree, lock))
            res = "success";
        else
            res = "failed";
        return res;
    }
}