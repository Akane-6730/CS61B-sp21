package gitlet;

import java.io.File;
import java.util.Map;

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

    // =================================================================
    // Section 2: Public API
    // =================================================================

    /**
     * Initializes a new repository.
     */
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

        // initialize the tree
        Tree initTree = new Tree();
        initTree.save();

        // create and save initial commit
        Commit init = new Commit(INITIAL_COMMIT_MESSAGE, null, null, initTree.getId());
        init.save();

        // update the branch
        updateBranchHead(MASTER, init.getId());

        // Update the HEAD
        updateHead(MASTER);
    }

    /**
     * Adds a file to the staging area.
     * @param fileNames the files to add to the staging area.
     */
    public void add(String[] fileNames) {
        Index index = Index.load();

        // Get the current commit and its tree for comparison.
        Commit currentCommit = getCurrentCommit();
        Tree tree = currentCommit.getTree();

        for (String fileName : fileNames) {
            // Check if the file exists.
            File fileToStage = join(CWD, fileName);
            if (!fileToStage.exists()) {
                System.out.println("File does not exist.");
                return;
            }

            // Add the file or directory to the staging area.
            if (fileToStage.isFile()) {
                addFile(fileName, tree, index);
            } else if (fileToStage.isDirectory()) {
                addDirectory(fileName, tree, index);
            }
        }

        index.save();
    }

    /**
     * Commits the changes in the staging area.
     * @param message the commit message.
     */
    public void commit(String message) {
        Index index = Index.load();
        // Pre-check
        if (index.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (message == null || message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        String headCommitId = getHeadCommitId();
        Commit headCommit = Commit.load(headCommitId);
        Map<String, String> filesToCommit = headCommit.getAllFiles();

        // Update the map
        // add / modify files
        filesToCommit.putAll(index.getStagedAdditions());
        for (String filePath : index.getStagedRemovals().keySet()) {
            filesToCommit.remove(filePath);
        }

        Tree newTree = Tree.buildTreeFromMap(filesToCommit);
        newTree.save();
        String newTreeId = newTree.getId();

        // TODO: What if it's the merge case?

        Commit newCommit = new Commit(message, headCommitId, null, newTreeId);
        newCommit.save();

        // --- Update the repository state ---
        // Update the head of the branch
        String currentBranch = getCurrentBranch();
        updateBranchHead(currentBranch, newCommit.getId());
        // Update the HEAD
        updateHead(currentBranch);

        // Clear the staging area
        index.clear();
        index.save();
    }

    // =================================================================
    // Section 3: Private Helper Methods - Grouped by Feature
    // =================================================================

    /**
     * Gets the SHA-1 ID of the current head commit.
     * @return The SHA-1 ID of the head commit.
     */
    private String getHeadCommitId() {
        // 1. Read the HEAD file to find out the current branch reference.
        // e.g., "ref: refs/heads/master"
        String headRef = readContentsAsString(HEAD_FILE);
        String[] parts = headRef.split(" ");
        if (parts.length < 2) {
            throw new GitletException("HEAD file is corrupted.");
        }

        // 2. Construct the path to the actual branch file.
        // e.g., .gitlet/refs/heads/master
        File branchFile = join(GITLET_DIR, parts[1]);

        // 3. Read the commit ID from the branch file.
        return readContentsAsString(branchFile);
    }

    /**
     * Gets the current head Commit object.
     * @return The Commit object pointed to by HEAD.
     */
    private Commit getCurrentCommit() {
        String headCommitId = getHeadCommitId();
        return Commit.load(headCommitId);
    }

    /**
     * Commits the changes in the staging area.
     * @param branchName the name of the branch to commit to.
     */
    private void updateHead(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        // Get the path relative to the .gitlet directory for consistency.
        String relativePath = GITLET_DIR.toURI().relativize(branchFile.toURI()).getPath();
        // The content is always in the format "ref: [path_to_ref_file]"
        writeContents(HEAD_FILE, "ref: " + relativePath);
    }

    private void updateBranchHead(String branchName, String commitId) {
        File branchFile = join(HEADS_DIR, branchName);
        writeContents(branchFile, commitId);
    }

    private String getCurrentBranch() {
        String headRef = readContentsAsString(HEAD_FILE);
        String[] parts = headRef.split(" ");
        if (parts.length < 2) {
            throw new GitletException("HEAD file is corrupted.");
        }
        String branchName = parts[1].substring(parts[1].lastIndexOf("/") + 1);
        return branchName;
    }

    // --- Commit Command Helpers ---



    // --- Add Command Helpers ---

    /**
     * Adds a file to the staging area.
     * @param filePath the path of the file (relative to CWD).
     * @param currentTree the current tree of the commit.
     * @param index the index to add the file to.
     */
    private void addFile(String filePath, Tree currentTree, Index index) {
        File file = join(CWD, filePath);

        // --- Get all the status ---
        // 1. Get workspace status
        // if the file does not exist, it is a removed file
        String newBlobId = file.exists() ? new Blob(file).getId() : null;

        // 2. Get head commit status
        String headBlobId = currentTree.findBlobId(filePath);

        // 3. Get index status
        String stagedBlobId = index.getStagedAdditionId(filePath);

        // --- Decision logic ---

        // 1. If the file does not exist, it is a removed file
        if (newBlobId == null) {
            // 1.1 If the file is in the index, remove it
            if (stagedBlobId != null) {
                index.unstage(filePath);
            }
            // 1.2 If the file is tracked, but not in the index, do nothing
            // It is rm command's duty!
            return;
        }

        // 2. If the file is identical to the head commit
        if (newBlobId.equals(headBlobId)) {
            // If the file is in the index, remove it
            index.unstage(filePath);
            return;
        }

        // 3. Else, the file is new or modified
        // Just update the index
        if (!newBlobId.equals(stagedBlobId)) {
            index.add(filePath, newBlobId);
            Blob newBlob = new Blob(file);
            newBlob.save();
        }
    }

    /**
     * Recursively processes all files within a given directory in the working directory.
     * @param dirPath The standardized relative path of the directory to process.
     * @param headTree The root tree of the current commit.
     * @param index The staging area.
     */
    private void addDirectory(String dirPath, Tree headTree, Index index) {
        File dir = join(CWD, dirPath);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            // Construct the standardized relative path for the child file/dir
            String childPath = join(dirPath, file.getName()).getPath();
            if (file.isDirectory()) {
                // If it's a directory, recurse
                addDirectory(childPath, headTree, index);
            } else if (file.isFile()) {
                // If it's a file, process it
                addFile(childPath, headTree, index);
            }
        }
    }

}
