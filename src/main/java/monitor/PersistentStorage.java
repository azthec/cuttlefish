package monitor;

import commons.CrushMap;
import commons.MetadataTree;
import io.atomix.core.list.DistributedList;
import io.atomix.core.value.AtomicValue;
import io.grpc.Metadata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PersistentStorage {
    private static String storageDir = "/.cuttlefish/";

    public static void initializePersistentMaps(String path) {

    }

    public static void storeMaps(String path, DistributedList<CrushMap> distributed_crush_maps) throws IOException {
        String folder_path = path + storageDir;
        File folder = new File(folder_path);
        if (folder.exists() && !folder.isDirectory()) {
            throw new IOException("Storage directory exists, but is not a folder!");
        } else if (!folder.exists()) {
            boolean res = new File(folder_path).mkdirs();
            if (!res) {
                throw new IOException("Failed to create storage directory!");
            }
        }

        Path tmp_path = Paths.get(folder_path + "cmaps.ser.tmp");
        Path perm_path = Paths.get(folder_path + "cmaps.ser");


        FileOutputStream fout = new FileOutputStream(tmp_path.toString());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        ArrayList<CrushMap> crushMapList = new ArrayList<>(distributed_crush_maps);
        oos.writeObject(crushMapList);
        oos.close(); // closing oos internally closes fout
        Files.move(
                tmp_path,
                perm_path,
                StandardCopyOption.ATOMIC_MOVE
        ); // leverages POSIX atomic rename syscall

    }

    public static ArrayList<CrushMap> loadMaps(String path) throws IOException {
        String folder_path = path + storageDir;
        File folder = new File(folder_path);
        if (folder.exists() && !folder.isDirectory()) {
            throw new IOException("Storage directory exists, but is not a folder!");
        }

        File cmaps_file = new File(folder_path + "cmaps.ser");
        if (cmaps_file.exists() && !folder.isDirectory()) {
            throw new IOException("CrushMap storage exists, but is not a file!");
        }  else if (!cmaps_file.exists()) {
            throw new IOException("CrushMap storage does not exist!");
        }

        FileInputStream fin = new FileInputStream(folder_path + "cmaps.ser");
        ObjectInputStream ois = new ObjectInputStream(fin);
        ArrayList<CrushMap> crushMapList = null;
        try {
            crushMapList = (ArrayList<CrushMap>) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IOException("Read file does not contain a List of CrushMaps!");
        }
        ois.close();
        return crushMapList;
    }

    public static void storeMetadata(String path, AtomicValue<MetadataTree> distributed_metadata_tree) throws IOException {
        String folder_path = path + storageDir;
        File folder = new File(folder_path);
        if (folder.exists() && !folder.isDirectory()) {
            throw new IOException("Storage directory exists, but is not a folder!");
        } else if (!folder.exists()) {
            boolean res = new File(folder_path).mkdirs();
            if (!res) {
                throw new IOException("Failed to create storage directory!");
            }
        }

        Path tmp_path = Paths.get(folder_path + "mdata.ser.tmp");
        Path perm_path = Paths.get(folder_path + "mdata.ser");


        FileOutputStream fout = new FileOutputStream(tmp_path.toString());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(distributed_metadata_tree.get());
        oos.close(); // closing oos internally closes fout
        Files.move(
                tmp_path,
                perm_path,
                StandardCopyOption.ATOMIC_MOVE
        ); // leverages POSIX atomic rename syscall

    }

    public static MetadataTree loadMetadata(String path) throws IOException {
        String folder_path = path + storageDir;
        File folder = new File(folder_path);
        if (folder.exists() && !folder.isDirectory()) {
            throw new IOException("Storage directory exists, but is not a folder!");
        }

        File mdata_file = new File(folder_path + "mdata.ser");
        if (mdata_file.exists() && !folder.isDirectory()) {
            throw new IOException("Metadata storage exists, but is not a file!");
        }  else if (!mdata_file.exists()) {
            throw new IOException("Metadata storage does not exist!");
        }

        FileInputStream fin = new FileInputStream(folder_path + "mdata.ser");
        ObjectInputStream ois = new ObjectInputStream(fin);
        MetadataTree mtree = null;
        try {
            mtree = (MetadataTree) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IOException("Read file does not contain a MetadataTree!");
        }
        ois.close();
        return mtree;
    }
}
