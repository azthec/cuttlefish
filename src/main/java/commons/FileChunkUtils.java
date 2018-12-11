package commons;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileChunkUtils {
    /**
     *
     * @param file_path, the absolute path to the file
     * @param crushMap, the crush map
     * @param metadataTree, the metadata tree
     * @return
     */
    public static byte[] get_file(String file_path, CrushMap crushMap, MetadataTree metadataTree) {
        MetadataNode file_node = metadataTree.goToNode(file_path);
        int max_file_size = 1024*1024*2* file_node.getObjects();
        byte[] result = new byte[max_file_size];
        List<String> file_OIDs = file_node.getObjectsAsOIDsString();
        for (int i = 0; i < file_node.getObjects(); i++) {
            byte[] get_result = get_object(file_OIDs.get(i), crushMap);
            if (get_result == null || get_result.length <= 0) {
                System.out.println("File getting failed");
                return new byte[0];
            }
            result = ArrayUtils.addAll(result, get_result);
        }
        System.out.println("Got complete file successfully, with size: " + result.length + "bytes.");
        return result;
    }

    public static byte[] get_object(String oid, CrushMap crushMap) {
        System.out.println("Getting object with ID: " + oid);

        // TODO change this to randomly use one of the OSD's in get_object_osds
        ObjectStorageNode primary = get_object_primary(oid, crushMap);
        if (primary == null) {
            System.out.println("Failed to find primary for object: " + oid);
            return new byte[0];
        }

        FileChunkClient client = new FileChunkClient(primary.ip, primary.port);
        byte [] get_result = client.getChunk(oid);
        if (get_result == null || get_result.length <= 0) {
            System.out.println("File getting failed");
            return new byte[0];
        }
        System.out.println("File getting returned: " + new String(get_result, StandardCharsets.UTF_8) + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return get_result;
    }
//I like to name parts from 000, 001, 002
//        String filePartName = String.format("%s_%03d", fileName, partCounter++);

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

        // TODO eventually handle adding to the metadata tree

        byte[][] data = fileToByteArrays(new File(local_file_path));
        List<String> file_OIDs = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            file_OIDs.add(remote_file_path + "_" + i);
        }
        for (int i = 0; i < data.length; i++) {
            boolean post_result = post_object(file_OIDs.get(i), data[i], crushMap);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
            FileOutputStream fos = new FileOutputStream(into, true);
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

    public static ObjectStorageNode get_object_primary(String oid, CrushMap crushMap) {
        List<ObjectStorageNode> result_list = get_object_osds(oid, crushMap);
        if (result_list.size() > 0) {
            return result_list.get(0);
        } else {
            return null;
        }
    }


}
