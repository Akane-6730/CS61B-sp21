package gitlet;

import java.io.File;
import static gitlet.Utils.sha1;

/**
 * Represents a file's content.
 * Each blob is identified by the SHA-1 hash of its byte content.
 * This class is a simple data container and is serializable to be stored on disk.
 *
 * @author Akane
 */
public class Blob extends GitletObject {
    /** The byte content of the file. */
    private final byte[] content;

    /**
     * Creates a new Blob object from the content of a file.
     * @param file The file to be read.
     */
    public Blob(File file) {
        super();
        content = Utils.readContents(file);
        // Calculate and set the ID inherited from GitletObject
        setId(sha1(content));
    }

    @Override
    protected File getSaveDir() {
        return Repository.BLOBS_DIR;
    }

    /** Get the content of a blob */
    public byte[] getContent() {
        return content;
    }

    // TODO: do we need load?
//    /**
//     * A static method to load a Blob from disk.
//     * It uses the static helper from the parent class to find the file.
//     * @param id The SHA-1 ID of the blob to load.
//     * @return The Blob object, or null if it doesn't exist.
//     */
//    public static Blob load(String id) {
//        File blobFile = getFile(Repository.BLOBS_DIR, id); // Use the static getFile helper
//        if (!blobFile.exists()) {
//            return null;
//        }
//        return readObject(blobFile, Blob.class);
//    }
}
