package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.sha1;

/**
 * Represents a gitlet commit object.
 * This class stores metadata such as a message, timestamp, and parent commits.
 * It tracks file versions through a map of filenames to blob IDs.
 * It inherits persistence logic from the GitletObject base class.
 *
 * @author Akane
 */
public class Commit extends GitletObject {
    /**
     * The message of this Commit.
     */
    private String message;
    /**
     * The timestamp of when this commit was created.
     * Stored as a String in a specific format for consistency.
     */
    private final String timestamp;

    /**
     * The SHA-1 ID of the first parent commit.
     * For the very first commit, this will be null.
     */
    private final String parent;

    /**
     * The SHA-1 ID of the second parent commit.
     * This is only used for merge commits; otherwise, it is null.
     */
    private final String secondParent;

    /**
     * A map that tracks the files in this commit's snapshot.
     * Key: The full path of the file (e.g., "src/Main.java").
     * Value: The SHA-1 ID of the corresponding Blob object.
     * We use a TreeMap to ensure the keys are sorted alphabetically.
     * This guarantees that the same set of files will always produce
     * the same hash, which is critical for a consistent commit ID.
     */
    private final TreeMap<String, String> trackedFiles;


    /**
     * The constructor for a new Commit object.
     * @param message The commit message.
     * @param parent The ID of the first parent commit.
     * @param secondParent The ID of the second parent commit (for merges).
     * @param trackedFiles A map of files (filename -> blob ID) tracked by this commit.
     */
    public Commit(String message, String parent, String secondParent, TreeMap<String,String> trackedFiles) {
        super();

        this.parent = parent;
        this.secondParent = secondParent;
        this.message = message;
        this.trackedFiles = trackedFiles;
        if (parent == null) {
            this.timestamp = formatTimestamp(new Date(0));
        } else {
            this.timestamp = formatTimestamp(new Date());
        }

        setId(sha1(calculateId()));
    }

    /**
     * Calculate SHA-1 id of the commit
     * Including the file (blob) references of its files, parent reference, log message, and commit time.
     */
    private String calculateId() {
        List<Object> list = new ArrayList<>();
        list.add(message);
        list.add(timestamp);
        if (trackedFiles != null) {
            list.add(trackedFiles.toString());
        }
        if (parent != null) {
            list.add(parent);
        }
        if (secondParent != null) {
            list.add(secondParent);
        }
        return sha1(list);
    }

    private String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return sdf.format(date);
    }

    /**
     * Getters methods
     */
    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    protected File getSaveDir() {
        return Repository.COMMITS_DIR;
    }
}
