package commons;

import exceptions.InvalidNodeException;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.value.AtomicValue;
import io.grpc.Metadata;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

        List<MetadataChunk> file_OIDs = file_node.getChunks();
        byte[] get_result = new byte[0];
        for (int i = 0; i < file_node.getNumberOfChunks(); i++) {
            get_result = get_object(file_OIDs.get(i), crushMap); //.getChunkOidVersion()
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

    public static byte[] get_object(MetadataChunk metadataChunk, CrushMap crushMap) {
        System.out.println("Getting object with ID: " + metadataChunk.getChunkOidVersion());

//        ObjectStorageNode node = get_object_primary(metadataChunk.getChunkOid(), crushMap);
        ObjectStorageNode node = get_random_object_osd(metadataChunk.getChunkOid(), crushMap);
        if (node == null) {
            System.out.println("Failed to find node for object: " + metadataChunk.getChunkOidVersion());
            return new byte[0];
        }
        System.out.println("Getting from node: " + node.id);
        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        byte [] get_result = client.getChunk(metadataChunk.getChunkOidVersion());
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

    public static byte[] get_object(String oid, CrushMap crushMap) {
        String oidWitouthVersion = oid.substring(0, oid.lastIndexOf("_"));
        System.out.println("Getting object with ID: " + oid);

//        ObjectStorageNode node = get_object_primary(metadataChunk.getChunkOid(), crushMap);
        ObjectStorageNode node = get_random_object_osd(oidWitouthVersion, crushMap);
        if (node == null) {
            System.out.println("Failed to find node for object: " + oidWitouthVersion);
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

    public static byte[] getObjectFromAnyNodeInList(String oid, List<CrushNode> nodeList) {
        List<ObjectStorageNode> objectStorageNodes = getOSNodesFromCrushNodes(nodeList);
        ObjectStorageNode node = objectStorageNodes.get(new Random().nextInt(objectStorageNodes.size()));
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
     * @param , the metadata tree
     * @return
     */
    public static boolean post_file(String local_file_path, String remote_file_path,
                                    CrushMap crushMap,
                                    AtomicValue<MetadataTree> distributedMetadataTree,
                                    DistributedLock metaLock)
            throws IOException {
        // this function MUST ONLY be called after getting the FILE write lock!

        System.out.println("Attempting to post file " + remote_file_path);

        MetadataTree metadataTree = distributedMetadataTree.get();
        MetadataNode parent_node = metadataTree.goToParentFolder(remote_file_path);
        // TODO eventually throw folder does not exist error
        if (parent_node == null) {
            System.out.println("Failed to post file, parent folder does not exist!");
            return false;
        }

        byte[][] data = fileToByteArrays(new File(local_file_path));

        MetadataNode oldNode = metadataTree.goToNode(remote_file_path);
        MetadataNode newNode = new MetadataNode(
                remote_file_path.substring(remote_file_path.lastIndexOf("/") + 1),
                MetadataNode.FILE, null
        );
        if (oldNode == null) {
            newNode.setVersion(0);
        } else {
            newNode.setVersion(oldNode.getVersion() + 1);
        }
        newNode.setNumberOfChunks(data.length);
        newNode.setHash(DigestUtils.sha256Hex(new FileInputStream(local_file_path)));

        List<String> file_OIDs = newNode.getChunksOidList();
        newNode.setChunks(new ArrayList<>());
        for (int i = 0; i < data.length; i++) {
            MetadataChunk metadataChunk = new MetadataChunk(
                    i, newNode.getVersion(),
                    DigestUtils.sha256Hex(data[i]),
                    remote_file_path
            );
            boolean post_result = post_object(metadataChunk, data[i], crushMap);
            newNode.addChunk(metadataChunk);
            // TODO
            // int pg_id = Crush.get_pg_id(oid, crushMap.total_pgs);
            // PG-objmapper.add(metadataChunk, pg_id);
            if (!post_result) {
                System.out.println("Failed to post file part = " + i + " !");
                return false;
            }
        }
        System.out.println("Posted complete file successfully!");

        try {
            if (metaLock.tryLock(10, TimeUnit.SECONDS)) {
                metadataTree = distributedMetadataTree.get();
                MetadataNode parent = metadataTree.goToParentFolder(remote_file_path);
                if(parent!=null && parent.isFolder()) {
                    try {
                        parent.addChild(newNode);
                        distributedMetadataTree.set(metadataTree);
                        metaLock.unlock();
                        return true;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        metaLock.unlock();
                        System.out.println("Destination file already exists!");
                        return false;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            metaLock.unlock();
            System.out.println("Failed to acquire metaLock!");
            return false;
        }
        // reached if parent is null or failed do acquire lock
        metaLock.unlock();
        System.out.println("Failed to acquire metaLock or parent folder has been deleted!");
        return false;
    }

    public static boolean post_object(MetadataChunk metadataChunk, byte[] data, CrushMap crushMap) {
        System.out.println("Posting object with ID: " + metadataChunk.getChunkOidVersion());

        ObjectStorageNode primary = get_object_primary(metadataChunk.getChunkOid(), crushMap);
        if (primary == null) {
            System.out.println("Failed to find primary for object: " + metadataChunk.getChunkOidVersion());
            return false;
        }

        FileChunkClient client = new FileChunkClient(primary.ip, primary.port);
        boolean post_result = client.postChunk(metadataChunk.getChunkOidVersion(), data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }

    public static boolean post_object(String oid, byte[] data, CrushMap crushMap, int replicaNumber) {
        String oidWitouthVersion = oid.substring(0, oid.lastIndexOf("_"));
        System.out.println("Posting object with ID: " + oid + " to replica #" + replicaNumber);
        ObjectStorageNode node = null;
        try {
            node = get_object_osds(oidWitouthVersion, crushMap).get(replicaNumber);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Failed to find replica for object: " + oid);
            return false;
        }

        if (node == null) {
            System.out.println("Failed to find replica for object (node == null): " + oid);
            return false;
        }

        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        boolean post_result = client.postChunk(oid, data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }

    public static boolean post_object(MetadataChunk metadataChunk, byte[] data, CrushMap crushMap, int replicaNumber) {
        System.out.println("Posting object with ID: " + metadataChunk.getChunkOidVersion() + " to replica #" + replicaNumber);
        ObjectStorageNode node = null;
        try {
            node = get_object_osds(metadataChunk.getChunkOid(), crushMap).get(replicaNumber);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Failed to find replica for object: " + metadataChunk.getChunkOidVersion());
            return false;
        }

        if (node == null) {
            System.out.println("Failed to find replica for object (node == null): " + metadataChunk.getChunkOidVersion());
            return false;
        }

        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        boolean post_result = client.postChunk(metadataChunk.getChunkOidVersion(), data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }

    //OSD function
    public static boolean postByteArrayToNode(String oid, byte[] data, CrushNode crushNode) {
        ObjectStorageNode node = Loader.get_osd_map().get(crushNode.nodeID + "");
        if (node == null) {
            System.out.println("Failed to find node info for object (node == null): " + oid);
            return false;
        }
        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        boolean post_result = client.postChunkOSD(oid, data);
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
    public static boolean byteArraysToFile(byte[][] bytes, File into) {
        try {
            FileOutputStream fos = new FileOutputStream(into, false);
            for (byte[] buffer : bytes) {
                fos.write(buffer);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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

    public static List<ObjectStorageNode> getOSNodesFromCrushNodes(List<CrushNode> crushNodes) {
        Loader loader = new Loader();
        HashMap<String, ObjectStorageNode> hashMap = loader.get_osd_map();

        List<ObjectStorageNode> result = new ArrayList<>();
        for (CrushNode node : crushNodes) {
            result.add(hashMap.get("" + node.nodeID));
        }
        return result;
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
    public static boolean copyChunks(String f1, String f2, CrushMap crushMap,
                                     AtomicValue<MetadataTree> distributedMetadataTree,
                                     DistributedLock metaLock) throws InvalidNodeException {
        // this function MUST ONLY be called after getting the FILE write lock! (file lock != metaLock)
        // TODO test function in depth

        MetadataTree metadataTree = distributedMetadataTree.get();
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
                // parent is null until its set during the file lock write at the end
                n2 = new MetadataNode(n1.getName(), MetadataNode.FILE, null);
                n2.setVersion(n1.getVersion());
                n2.setNumberOfChunks(n1.getNumberOfChunks());
                n2.setHash(n1.getHash());
                n2.setChunks(n1.getChunks());
            }
        }


        System.out.println("Everything is in order, starting copy");
        //--------------------------------------------------------
        if(n1 != null && n1.isFile() && n2 != null && n2.isFile()){
            byte[][] source = FileChunkUtils.get_file(n1.getPath(),crushMap,metadataTree);
            // MetadataNode node = metadataTree.goToNode(f2); node is n2
            File into = new File(f2);
            if (!FileChunkUtils.byteArraysToFile(source, into))
                return false;
            // chunk replacing (post from f1 to f2)
            for (int i=0; i<source.length; i++){
                MetadataChunk metadataChunk = new MetadataChunk(i, n2.getVersion(), DigestUtils.sha256Hex(source[i]), f2);
                boolean postResult = post_object(metadataChunk,source[i],crushMap);
                if(!postResult){
                    System.out.println("Failed to post file part = " + i + " !");
                    return false;
                }
            }
            n2.setVersion(n2.getVersion() + 1);
        }

        // TODO add new node to metaTree
        try {
            if (metaLock.tryLock(10, TimeUnit.SECONDS)) {
                metadataTree = distributedMetadataTree.get();
                MetadataNode parent = metadataTree.goToParentFolder(f2);
                if(parent!=null && parent.isFolder()) {
                    try {
                        parent.addChild(n2);
                        distributedMetadataTree.set(metadataTree);
                        metaLock.unlock();
                        return true;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        metaLock.unlock();
                        System.out.println("Destination file already exists!");
                        return false;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            metaLock.unlock();
            System.out.println("Failed to acquire metaLock!");
            return false;
        }
        // reached if parent is null or failed do acquire lock
        metaLock.unlock();
        System.out.println("Failed to acquire metaLock or parent folder has been deleted!");
        return false;
    }

}
