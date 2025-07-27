package gitlet;

import java.io.File;
import java.util.TreeMap;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository. This class serves as the main entry point for all commands.
 * It encapsulates the logic for gitlet operations like init, add, commit, etc.
 * It manages the state of the repository by interacting with the .gitlet directory
 * and its internal structure.
 *
 * @author Akane
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The directory to store all serialized commit objects. */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** The directory to store all serialized blob objects. */
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    /** The directory to store references, such as branch heads. */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    /** The directory to store pointers to the tip of each branch. */
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    /** The file that stores a reference to the current head (e.g., "ref: refs/heads/master"). */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");


    private static final String INITIAL_COMMIT_MESSAGE = "initial commit";
    private static final String MASTER = "master";

    // --- COMMAND IMPLEMENTATIONS ---

    /** init */
    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // Create the directory structure.
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();

        Commit init = new Commit(INITIAL_COMMIT_MESSAGE, null, null, new TreeMap<String, String>());
        System.exit(0);
        init.save();

        // record the branch
        File masterBranchFile = join(HEADS_DIR, MASTER);
        writeContents(masterBranchFile, init.getId());

        // Update the HEAD
        updateHead(masterBranchFile);
    }

    /**
     * A robust helper to update the HEAD file to point to a new reference.
     * It takes a File object representing the reference, making it independent of ref type.
     * @param refFile The File object for the reference (e.g., a file in the heads or remotes directory).
     */
    private void updateHead(File refFile) {
        // Get the path relative to the .gitlet directory for consistency.
        String relativePath = GITLET_DIR.toURI().relativize(refFile.toURI()).getPath();

        // The content is always in the format "ref: [path_to_ref_file]"
        writeContents(HEAD_FILE, "ref: " + relativePath);
    }

    /**
     * Reads the HEAD file to find the name of the currently active branch.
     * @return The name of the current branch (e.g., "master"), or null if not on a branch.
     */
    private String getCurrentBranchName() {
        String headContent = readContentsAsString(HEAD_FILE);
        if (!headContent.startsWith("ref: ")) {
            // This would be a detached HEAD state, which we don't support now
            return null;
        }

        String refPath = headContent.substring("ref: ".length());
        return new File(refPath).getName();
    }

    // In Repository.java

    /**
     * Gets the SHA-1 ID of the commit that HEAD currently points to.
     * It follows the symbolic reference in the HEAD file to the actual branch file.
     * @return The 40-character SHA-1 ID of the current head commit.
     */
    private String getHeadCommitId() {
        String headContent = readContentsAsString(HEAD_FILE);
        if (!headContent.startsWith("ref: ")) {
            // This is for a detached HEAD, which is not required by the spec.
            // For our project, HEAD should always be a symbolic ref.
            return headContent;
        }

        String relativeRefPath = headContent.substring("ref: ".length());
        File branchFile = join(GITLET_DIR, relativeRefPath);

        if (!branchFile.exists()) {
            throw new GitletException("HEAD points to a non-existent branch file.");
        }

        // Read the content of the branch file, which is the commit ID.
        return readContentsAsString(branchFile);
    }
}
