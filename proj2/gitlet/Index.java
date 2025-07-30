package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Utils.*;

/**
 * Represents the Gitlet staging area, also known as the "index".
 * It maintains a "flat" mapping of file paths to their corresponding blob SHA-1 IDs
 * for both "staged for addition" and "staged for removal" files.
 * This entire object is serialized to the .gitlet/index file.
 */
public class Index implements Serializable {
    /** Staging area index file (.gitlet/index) tracking added but uncommitted changes */
    private static final File INDEX = join(Repository.GITLET_DIR, "index");

    /**
     * Tracks files that are staged for addition.
     * Key: The relative path of the file (e.g., "src/Main.java").
     * Value: The SHA-1 ID of the blob for that file's content.
     */
    private final TreeMap<String, String> stagedForAddition;

    /**
     * Tracks files that are staged for removal.
     * Key: The relative path of the file to be removed.
     * Value: The SHA-1 ID of the blob from the *last* commit (for reference).
     */
    private final TreeMap<String, String> stagedForRemoval;



    /**
     * Constructor for a new, empty Index.
     */
    public Index() {
        this.stagedForAddition = new TreeMap<>();
        this.stagedForRemoval = new TreeMap<>();
    }

    // --- Public API for state modification ---

    /**
     * Stages a file for addition. If the file was previously staged for
     * removal, this unstages it from removal.
     *
     * @param filepath The relative path of the file.
     * @param blobId The SHA-1 ID of the file's content.
     */
    public void add(String filepath, String blobId) {
        // If the file was marked for removal, unmark it.
        stagedForRemoval.remove(filepath);
        // Stage the new version for addition. Overwrites any previous stage.
        stagedForAddition.put(filepath, blobId);
    }

    /**
     * Stages a file for removal. If the file was previously staged for
     * addition, this unstages it.
     *
     * @param filepath The relative path of the file.
     * @param blobId The blob ID of the file in the last commit.
     */
    public void remove(String filepath, String blobId) {
        // Unstage it from addition if it was there.
        stagedForAddition.remove(filepath);
        // Stage it for removal.
        stagedForRemoval.put(filepath, blobId);
    }

    /**
     * Unstages a file, removing it from both the addition and removal lists.
     * @param filepath The relative path of the file to unstage.
     */
    public void unstage(String filepath) {
        stagedForAddition.remove(filepath);
        stagedForRemoval.remove(filepath);
    }

    /**
     * Unstages a file only from addition list.
     * @param filepath The relative path of the file to unstage.
     */
    public void unstageAddition(String filepath) {
        stagedForAddition.remove(filepath);
    }

    /**
     * Clears the staging area. Called after a commit.
     */
    public void clear() {
        stagedForAddition.clear();
        stagedForRemoval.clear();
    }

    // --- Public API for state querying ---

    /**
     * Checks if a file is currently staged for addition and returns its blob ID.
     * @param filePath The standardized path of the file.
     * @return The blob ID if staged for addition, otherwise null.
     */
    public String getStagedAdditionId(String filePath) {
        return stagedForAddition.get(filePath);
    }

    public boolean isStagedForAddition(String filepath) {
        return stagedForAddition.containsKey(filepath);
    }

    /**
     * Checks if a file is currently staged for removal.
     * @param filePath The standardized path of the file.
     * @return true if the file is staged for removal, otherwise false.
     */
    public boolean isStagedForRemoval(String filePath) {
        return stagedForRemoval.containsKey(filePath);
    }

    /**
     * Checks if the staging area is completely empty.
     * @return true if no files are staged for addition or removal.
     */
    public boolean isEmpty() {
        return stagedForAddition.isEmpty() && stagedForRemoval.isEmpty();
    }

    /**
     * Returns a read-only view of the files staged for addition.
     * The returned map cannot be modified.
     * @return An unmodifiable map of file paths to blob IDs.
     */
    public Map<String, String> getStagedAdditions() {
        return Collections.unmodifiableMap(stagedForAddition);
    }

    /**
     * Returns a read-only view of the files staged for removal.
     * The returned map cannot be modified.
     * @return An unmodifiable map of file paths to blob IDs.
     */
    public Map<String, String> getStagedRemovals() {
        return Collections.unmodifiableMap(stagedForRemoval);
    }


    // ================================================================
    //  I/O
    // ================================================================

    /**
     * Loads the Index from the .gitlet/index file.
     * If the file does not exist, returns a new, empty Index object.
     * @return The Index object loaded from the file.
     */
    public static Index load() {
        if (!INDEX.exists()) {
            return new Index();  // Return a new, clean Index object
        }
        return readObject(INDEX, Index.class);
    }

    /**
     * Saves the Index to the .gitlet/index file.
     */
    public void save() {
        writeObject(INDEX, this);
    }
}