package commons;

import io.atomix.core.list.DistributedList;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.value.AtomicValue;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class FileMetadataUtils {
    public static String createRemoteDirectory(String newFoldername, String currPath,
                                               AtomicValue<MetadataTree> distributed_metadata_tree,
                                               DistributedLock lock) {
        String res = "";
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                MetadataTree tree = distributed_metadata_tree.get();
                MetadataNode currNode = tree.goToNodeIfNotDeleted(currPath);
                MetadataNode newNode = tree.goToNodeIfNotDeleted(currPath+newFoldername+"/");
                System.out.println("I WANNA DO: "+ currPath+"/"+newFoldername+"/");
                if (newNode != null && !newNode.isDeleted()) {
                    System.out.println("node with that name already exists");
                    if (newNode.isFolder())
                        res = "That folder already exists...";
                    else if (newNode.isFile())
                        res = "That's an already existing file...";
                } else if(newNode != null && newNode.isDeleted()) {
                    if(newNode.isFolder())
                        newNode.undelete();
                    else {
                        System.out.println("TODO test, this should never occur as folders have / appended");
                    }
                } else {
                    System.out.println("node with that name doesn't exist");
                    currNode.addFolder(newFoldername);
                }
                distributed_metadata_tree.set(tree);
                res += "\n mkdir finished";
                lock.unlock();

            } else {
                res = "Couldn't obtain the lock, operation aborted";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            res = "Interrupted while acquiring lock!";
        }
        return res;
    }

    public static String deleteRemote(String folderName, String currPath, boolean type,
                                       DistributedList<CrushMap> distributed_crush_maps,
                                       AtomicValue<MetadataTree> distributed_metadata_tree,
                                       DistributedLock lock) {
        String res = "";
        try {
            // lock success
            if (lock.tryLock(10, TimeUnit.SECONDS)) {

                CrushMap crushMap = distributed_crush_maps.get(distributed_crush_maps.size()-1);
                MetadataTree tree = distributed_metadata_tree.get();
                MetadataNode currNode = tree.goToNodeIfNotDeleted(currPath);
                MetadataNode newNode;// = tree.goToNode(currNode,folderName);

                if (folderName.charAt(0) == '/'){
                    System.out.println("abs path given");
                    newNode = tree.goToNodeIfNotDeleted(folderName); // abs path
                } else{
                    System.out.println("relative path given");
                    newNode = tree.goToNodeIfNotDeleted(currPath+folderName); // relative path
                }

                if (newNode != null) {
                    if (newNode.getType() != type) {
                        return "Existing node is not of specified type!";
                    }
                    if (newNode == currNode)
                        return "Can't remove you current directory!";
                    System.out.println("node not null and not deleted");
                    if (newNode.hasUndeletedChildren())
                        return "Can't delete node containing undeleted children.";
                    if (newNode.isFolder()) {
                        System.out.println("node is folder");
                        newNode.delete();
                    } else if (newNode.isFile()) {
                        System.out.println("node is a file");
                        for (MetadataChunk chunk : newNode.getChunks()) {
                            int pg = Crush.get_pg_id(chunk.getChunkOid(), crushMap.total_pgs);
                            tree.getPgs().get(pg).getObjects().remove(chunk.getChunkOid());
                        }
                        newNode.setChunks(new ArrayList<>());
                        newNode.delete();
                    }
                } else {
                    System.out.println("no folder with that name");
                    res = "There is no folder with that name";
                }

                distributed_metadata_tree.set(tree);
                res += " rmdir concluded";
                lock.unlock();
            } else {
                res = "Couldn't obtain the lock, operation aborted";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            res = "Interrupted while acquiring lock!";
        }
        return res;
    }
}
