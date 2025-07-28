package gitlet;

import java.io.File;
import static gitlet.Utils.readContents;

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
}
