package gitlet;

import java.io.File;
import static gitlet.Utils.readContents;
import static gitlet.Utils.readObject;

public class Blob extends GitObject {
    private final byte[] content;

    public Blob(File file) {
        // 1. Call the parent constructor, explicitly setting this object's type.
        super("blob");
        // 2. Read and store the file's content. This is the blob's unique data.
        content = readContents(file);
    }

    @Override
    public byte[] getSerializableContent() {
        return content;
    }

    /**
     * A static factory method to load a Blob from disk by its ID.
     * @param id The SHA-1 ID of the blob to load.
     * @return The deserialized Blob object, or null if it doesn't exist.
     */
    public static Blob load(String id) {
        // We reuse the static helper from the parent class to find the file.
        File blobFile = getFile(id);
        if (!blobFile.exists()) {
            return null;
        }
        // readObject will deserialize the file specifically into a Blob object.
        return readObject(blobFile, Blob.class);
    }
}
