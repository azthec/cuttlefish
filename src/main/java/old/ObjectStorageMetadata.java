package old;

import java.io.Serializable;

public class ObjectStorageMetadata implements Serializable {

}



//public class SyncList<T> implements Serializable {
//    private static final long serialVersionUID = -6184959782243333803L;
//
//    private List<T> list = new ArrayList<>();
//    private transient Lock readLock, writeLock;
//
//    public SyncList() {
//        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//        readLock = readWriteLock.readLock();
//        writeLock = readWriteLock.writeLock();
//    }
//
//    public void add(T element) {
//        writeLock.lock();
//        try {
//            list.add(element);
//        } finally {
//            writeLock.unlock();
//        }
//    }
//
//    public T get(int index) {
//        readLock.lock();
//        try {
//            return list.get(index);
//        } finally {
//            readLock.unlock();
//        }
//    }
//
//    public String dump() {
//        readLock.lock();
//        try {
//            return list.toString();
//        } finally {
//            readLock.unlock();
//        }
//    }
//
//    public boolean replace(T old, T newElement) {
//        writeLock.lock();
//        try {
//            int pos = list.indexOf(old);
//            if (pos < 0)
//                return false;
//            list.set(pos, newElement);
//            return true;
//        } finally {
//            writeLock.unlock();
//        }
//    }
//
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        readLock.lock();
//        try {
//            out.writeObject(list);
//        } finally {
//            readLock.unlock();
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private void readObject(ObjectInputStream in) throws IOException,
//            ClassNotFoundException {
//        list = (List<T>) in.readObject();
//        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
//        readLock = readWriteLock.readLock();
//        writeLock = readWriteLock.writeLock();
//    }
//}