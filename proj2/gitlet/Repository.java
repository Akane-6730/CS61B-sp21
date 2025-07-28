package gitlet;

import java.io.File;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author Akane
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The object directory */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** The directory to store references, such as branch heads. */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    /** The directory to store pointers to the tip of each branch. */
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    /** The file that stores a reference to the current head (e.g., "ref: refs/heads/master"). */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");

    private static final String INITIAL_COMMIT_MESSAGE = "initial commit";
    private static final String MASTER = "master";

    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // Create the directory structure.
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();

        // create and save initial commit
        Commit init = new Commit(INITIAL_COMMIT_MESSAGE, null, null, null);
        init.save();

        // record the branch
        File masterBranchFile = join(HEADS_DIR, MASTER);
        writeContents(masterBranchFile, init.getId());

        // Update the HEAD
        updateHead(masterBranchFile);
    }

    private void updateHead(File refFile) {
        // Get the path relative to the .gitlet directory for consistency.
        String relativePath = GITLET_DIR.toURI().relativize(refFile.toURI()).getPath();

        // The content is always in the format "ref: [path_to_ref_file]"
        writeContents(HEAD_FILE, "ref: " + relativePath);
    }
}
