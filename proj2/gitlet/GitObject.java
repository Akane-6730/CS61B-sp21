package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;

public abstract class GitObject implements Serializable {
    private String id;
    private final String type;
    private static final int VALID_ID_LENGTH = 40;

    public GitObject(String type) {
        this.type = type;
    }

    public abstract byte[] getSerializableContent();

    // Get file from an SHA-1 id
    public static File getFile(String id) {
        if (id == null || id.length() != VALID_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid SHA-1 ID provided.");
        }
        String dirName = id.substring(0, 2);
        String fileName = id.substring(2);
        return join(join(Repository.OBJECTS_DIR, dirName), fileName);
    }

    public String calculateId() {
        byte[] content = getSerializableContent();
        String header = String.format("%s %d\0", this.type, content.length);
        return sha1(header.getBytes(StandardCharsets.UTF_8), content);
    }

    public void save() {
        File objectFile = getFile(getId());
        if (objectFile.exists()) {
            return;
        }
        // mkdirs(): Creates the directory named by this abstract pathname,
        // including any necessary but nonexistent parent directories.
        objectFile.getParentFile().mkdirs();
        writeObject(objectFile, this);
    }

    public String getId() {
        if (id == null) {
            id = calculateId();
        }
        return id;
    }

    public String getType() {
        return type;
    }
}
