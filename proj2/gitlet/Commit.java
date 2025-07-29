package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static gitlet.Utils.readObject;

/** Represents a gitlet commit object.
 *
 *  @author Akane
 */
public class Commit extends GitObject {

    /** The message of this Commit. */
    private final String message;
    /** The SHA-1 ID of the tree object representing the project's root directory. */
    private final String treeId;
    /** The SHA-1 ID of the first parent commit. Null for the initial commit. */
    private final String parent;
    /** The SHA-1 ID of the second parent commit, used only for merges. Null otherwise. */
    private final String secondParent;
    /** The timestamp of when this commit was created, stored as a formatted String. */
    private final String timestamp;

    /**
     * The primary constructor for a Commit object.
     * @param message The commit message.
     * @param parent The ID of the first parent commit.
     * @param secondParent The ID of the second parent commit (for merges).
     * @param treeId The ID of the root tree object for this commit's snapshot.
     */
    public Commit(String message, String parent, String secondParent, String treeId) {
        super("commit");

        this.message = message;
        this.parent = parent;
        this.secondParent = secondParent;
        this.treeId = treeId;

        // The first commit in Git history has a special timestamp (the Unix Epoch).
        if (parent == null) {
            this.timestamp = formatTimestamp(new Date(0));
        } else {
            this.timestamp = formatTimestamp(new Date());
        }
    }

    /**
     * A static factory method to load a Commit from disk by its ID.
     * @param id The SHA-1 ID of the commit to load.
     * @return The deserialized Commit object, or null if it doesn't exist.
     */
    public static Commit load(String id) {
        File commitFile = getFile(id);
        if (!commitFile.exists()) {
            return null;
        }
        return readObject(commitFile, Commit.class);
    }

    /**
     * Implements the abstract method from GitObject.
     * It creates a canonical string representation of the commit's metadata,
     * which is then used by the parent class to calculate the SHA-1 ID.
     * The order and format of this content must be stable.
     *
     * @return The canonical byte array representation of this commit's content.
     */
    @Override
    public byte[] getSerializableContent() {
        StringBuilder contentBuilder = new StringBuilder();

        // The order of these lines is critical for a stable hash.
        contentBuilder.append("tree ").append(this.treeId).append("\n");

        if (this.parent != null) {
            contentBuilder.append("parent ").append(this.parent).append("\n");
        }
        if (this.secondParent != null) {
            contentBuilder.append("parent ").append(this.secondParent).append("\n");
        }

        contentBuilder.append(this.message).append("\n");

        return contentBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A private helper to format a Date object into the standard Git timestamp string.
     * @param date The Date object to format.
     * @return A formatted string (e.g., "Sun Jul 27 20:10:00 2025 +0900").
     */
    private String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return sdf.format(date);
    }

    // --- Getters ---

    public String getTreeId() {
        return treeId;
    }

    public Tree getTree() {
        return readObject(getFile(treeId), Tree.class);
    }

    public String getParent() {
        return parent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
