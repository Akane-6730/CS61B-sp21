package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

/**
 * An abstract base class for all serializable objects in Gitlet (e.g., Blobs, Commits).
 * It provides a common structure and shared persistence logic, such as saving and
 * loading objects to/from their respective directories using a hierarchical file structure.
 */
public abstract class GitletObject implements Serializable {

    /**
     * The SHA-1 ID of this object. This field is inherited by all subclasses.
     */
    private String id;

    /**
     * Default constructor.
     */
    public GitletObject() {
        id = null;
    }

    /**
     * A static helper method to get the file path for any GitletObject.
     * This is the centralized implementation of the hierarchical storage strategy.
     * @param baseDir The base directory for the object type (e.g., BLOBS_DIR or COMMITS_DIR).
     * @param id The SHA-1 ID of the object.
     * @return A File object representing the full path.
     */
    protected static File getFile(File baseDir, String id) {
        if (id == null || id.length() != 40) {
            throw new IllegalArgumentException("Invalid SHA-1 ID provided.");
        }
        String dirName = id.substring(0, 2);
        String fileName = id.substring(2);
        File subDir = join(baseDir, dirName);
        return join(subDir, fileName);
    }

    /**
     * An abstract method that must be implemented by subclasses.
     * It should return the base directory where objects of this type are stored.
     * @return The File object for the base directory (e.g., Repository.BLOBS_DIR).
     */
    protected abstract File getSaveDir();

    /**
     * Saves the current object to its designated directory within .gitlet.
     * This method contains the shared logic for hierarchical file storage.
     */
    public void save() {
        if (id == null) {
            throw new IllegalStateException("Object ID has not been set before saving.");
        }

        File objectFile = getFile(getSaveDir(), id);
        if (!objectFile.exists()) {
            objectFile.getParentFile().mkdirs();
            writeObject(objectFile, this);
        }
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }
}