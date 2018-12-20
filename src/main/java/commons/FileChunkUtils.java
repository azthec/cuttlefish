package commons;

import exceptions.InvalidNodeException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class FileChunkUtils {
    /**
     *
     * @param file_path, the absolute path to the file
     * @param crushMap, the crush map
     * @param metadataTree, the metadata tree
     * @return
     */
    public static byte[][] get_file(String file_path, CrushMap crushMap, MetadataTree metadataTree) {
        MetadataNode file_node = metadataTree.goToNode(file_path);
        if (file_node == null) {
            System.out.println("File does not exist!");
            return new byte[0][0];
        }
        int sizeOfFiles = 1024 * 1024; // 1MB
        byte[][] result = new byte[file_node.getNumberOfChunks()][];

        List<String> file_OIDs = file_node.getChunksOidList();
        byte[] get_result = new byte[0];
        for (int i = 0; i < file_node.getNumberOfChunks(); i++) {
            get_result = get_object(file_OIDs.get(i), crushMap);
            if (get_result == null || get_result.length <= 0) {
                System.out.println("File getting failed");
                return new byte[0][0];
            }
            result[i] = get_result;
        }
        System.out.println("Got complete file successfully, with size: "
                + ((result.length - 1) * 1024 * 1024 + get_result.length)
                + " bytes.");
        return result;
    }

    public static byte[] get_object(String oid, CrushMap crushMap) {
        System.out.println("Getting object with ID: " + oid);

//        ObjectStorageNode primary = get_object_primary(oid, crushMap);
        ObjectStorageNode node = get_random_object_osd(oid, crushMap);
        if (node == null) {
            System.out.println("Failed to find node for object: " + oid);
            return new byte[0];
        }
        System.out.println("Getting from node: " + node.id);
        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        byte [] get_result = client.getChunk(oid);
        if (get_result == null || get_result.length <= 0) {
            System.out.println("File getting failed");
            return new byte[0];
        }

        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return get_result;
    }

    /**
     *
     * @param local_file_path, the absolute path to the file
     * @param crushMap, the crush map
     * @param metadataTree, the metadata tree
     * @return
     */
    public static boolean post_file(String local_file_path, String remote_file_path,
                                    CrushMap crushMap, MetadataTree metadataTree)
            throws IOException {
        System.out.println("Attempting to post file " + remote_file_path);

        MetadataNode parent_node = metadataTree.goToParentFolder(remote_file_path);
        // TODO eventually throw folder does not exist error
        if (parent_node == null) {
            System.out.println("Failed to post file, parent folder does not exist!");
            return false;
        }

        byte[][] data = fileToByteArrays(new File(local_file_path));

        MetadataNode node = metadataTree.goToNode(remote_file_path);
        System.out.println("Updating node at MetadataTree!");
        System.out.println(node);
        if (node == null) {
            System.out.println("Node does not exist, adding new node.");
            node = metadataTree.goToParentFolder(remote_file_path)
                    .addFile(remote_file_path.substring(
                            remote_file_path.lastIndexOf("/") + 1)
                    );
            node.setVersion(0);
        } else {
            System.out.println("Node exists, updating data.");
            node.setVersion(node.getVersion() + 1);
        }
        node.setNumberOfChunks(data.length);
        node.setHash(DigestUtils.sha256Hex(new FileInputStream(local_file_path)));
        metadataTree.epoch += 1;

        List<String> file_OIDs = node.getChunksOidList();
        for (int i = 0; i < data.length; i++) {
            boolean post_result = post_object(file_OIDs.get(i), data[i], crushMap);
            node.addChunk(
                    new MetadataChunk(
                            i, node.getVersion(),
                            DigestUtils.sha256Hex(data[i]),
                            remote_file_path
                    )
            );
            if (!post_result) {
                System.out.println("Failed to post file part = " + i + " !");
                return false;
            }
        }
        System.out.println("Posted complete file successfully!");
        return true;
    }

    public static boolean post_object(String oid, byte[] data, CrushMap crushMap) {
        System.out.println("Posting object with ID: " + oid);

        ObjectStorageNode primary = get_object_primary(oid, crushMap);
        if (primary == null) {
            System.out.println("Failed to find primary for object: " + oid);
            return false;
        }

        FileChunkClient client = new FileChunkClient(primary.ip, primary.port);
        System.out.println("Posting object: " + oid + " | with data: " + data);
        boolean post_result = client.postChunk(oid, data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }

    public static boolean post_object(String oid, byte[] data, CrushMap crushMap, int replicaNumber) {
        System.out.println("Posting object with ID: " + oid + " to replica #" + replicaNumber);
        ObjectStorageNode node = null;
        try {
            node = get_object_osds(oid, crushMap).get(replicaNumber);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Failed to find replica for object: " + oid);
            return false;
        }

        if (node == null) {
            System.out.println("Failed to find replica for object (node == null): " + oid);
            return false;
        }

        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        System.out.println("Posting object: " + oid + " | with data: " + data);
        boolean post_result = client.postChunk(oid, data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }

    /**
     * Converts a local file to a byte array, that we can transmit
     * @param file
     * @return
     */
    public static byte[] fileToByteArray(File file){
        byte[] res = null;
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
            res = IOUtils.toByteArray(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Converts a local file to a byte matrix
     * @param file
     * @return
     */
    public static byte[][] fileToByteArrays(File file) throws IOException {
        int partCounter = 0;

        int sizeOfFiles = 1024 * 1024; // 1MB
        byte[] buffer = new byte[sizeOfFiles];
        int numberOfParts = (int) Math.ceil((double) file.length() / sizeOfFiles);
        byte[][] res = new byte[numberOfParts][];

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                res[partCounter] = new byte[bytesAmount];
                System.arraycopy(buffer, 0, res[partCounter], 0, bytesAmount);
                partCounter++;
            }
        }
        return res;
    }

    /**
     * Converts a byte matrix to a local file
     * @param bytes, into
     */
    public static void byteArraysToFile(byte[][] bytes, File into) {
        try {
            FileOutputStream fos = new FileOutputStream(into, false);
            for (byte[] buffer : bytes) {
                fos.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ObjectStorageNode>  get_object_osds(String oid, CrushMap crushMap) {
        System.out.println("Running OID selection for: " + oid);

        int pg = Crush.get_pg_id(oid, crushMap.total_pgs);
        System.out.println("Selected PG: " + pg);

        // this maps a placement group to OSD's
        Crush crush = new Crush();
        List<CrushNode> selected_osds = crush.select_OSDs(crushMap.get_root(), "" + pg);
        System.out.println("Selected OSD's: " + selected_osds);

        // TODO change this to eventually use discovery based shared OSD map
        Loader loader = new Loader();
        HashMap<String, ObjectStorageNode> hashMap = loader.get_osd_map();

        List<ObjectStorageNode> result = new ArrayList<>();
        for (CrushNode node : selected_osds) {
            result.add(hashMap.get("" + node.nodeID));
        }
        return result;
    }

    public static ObjectStorageNode get_random_object_osd(String oid, CrushMap crushMap) {
        Random random = new java.util.Random();
        List<ObjectStorageNode> result_list = get_object_osds(oid, crushMap);
        if (result_list.size() > 0) {
            return result_list.get(random.nextInt(result_list.size()));
        } else {
            return null;
        }
    }

    public static ObjectStorageNode get_object_primary(String oid, CrushMap crushMap) {
        List<ObjectStorageNode> result_list = get_object_osds(oid, crushMap);
        if (result_list.size() > 0) {
            return result_list.get(0);
        } else {
            return null;
        }
    }

    private static MetadataNode getParentFromAbsPath(String path, MetadataTree metadataTree){
        String[] pathParted = path.split("/");
        MetadataNode currNode = metadataTree.get_root();
        // in case someone sends the root
        if(pathParted.length > 0){
            for(int i=0; i<pathParted.length-1; i++){
                currNode = currNode.get(pathParted[i]);
            }
        }
        return currNode;
    }

    /**
     * TODO swap the error prints with exceptions
     * Copies the chunks of f1 to f2
     * Used as the implementation of the ">" command
     * @param f1 the absolute path of the source file
     * @param f2 the absolute path of the dest file
     * @return a boolean indicating the success of the operation
     */
    @SuppressWarnings("Duplicates")
    public static boolean copyChunks(String f1, String f2, CrushMap crushMap, MetadataTree metadataTree) throws InvalidNodeException {

        MetadataNode n1 = metadataTree.goToNode(f1);
        MetadataNode n2 = metadataTree.goToNode(f2);

        if (n1 == null ) {
            throw new InvalidNodeException("The source MetadataNode is null.");
        } else if (n1 != null && n1.isFolder()){
            throw new InvalidNodeException("The source MetadataNode is a folder.");
        } else if (n2 != null && n2.isFolder()){
            throw new InvalidNodeException("The destination MetadataNode is a folder.");
        } else if (n2 == null) {
            System.out.println("The destination is null, checking for parent folder");
            MetadataNode n2Parent = getParentFromAbsPath(f2, metadataTree);
            if (n2Parent == null){
                throw new InvalidNodeException("The parent of the destination MetadataNode is null");
            }
            else{
                System.out.println("The parent node exists, creating the destination node.");
                n2Parent.addFile(f2);
            }
        }

        System.out.println("Everything is in order, starting copy");
        //--------------------------------------------------------
        if(n1 != null && n1.isFile() && n2 != null && n2.isFile()){
            byte[][] source = FileChunkUtils.get_file(n1.getPath(),crushMap,metadataTree);
            // MetadataNode node = metadataTree.goToNode(f2); node is n2
            File into = new File(f2);
            FileChunkUtils.byteArraysToFile(source, into);
            // chunk replacing (post from f1 to f2)
            try {
                n2.setNumberOfChunks(source.length);
                n2.setHash(DigestUtils.sha256Hex(new FileInputStream(f2)));
                List<String> file_OIDs = n1.getChunksOidList();
                for (int i=0; i<source.length; i++){
                    boolean postResult = post_object(file_OIDs.get(i),source[i],crushMap);
                    n2.addChunk(new MetadataChunk(i, n2.getVersion(), DigestUtils.sha256Hex(source[i]), f2));
                    if(!postResult){
                        System.out.println("Failed to post file part = " + i + " !");
                        return false;
                    }
                }
                n2.setVersion(n2.getVersion() + 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        metadataTree.epoch += 1;
        return true;
    }

}
