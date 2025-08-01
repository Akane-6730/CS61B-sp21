package gitlet;

import java.io.File;
import java.util.*;

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
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
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
        assert headCommit != null;
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
        Index.clearIndex();
    }

    /**
     * Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal and remove
     * the file from the working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     * @param filePaths the paths of the files to unstage.
     */
    public void remove(String[] filePaths) {
        Index index = Index.load();
        Commit currentCommit = getCurrentCommit();
        for (String filePath : filePaths) {
            String trackedFileId = currentCommit.getTrackedFileId(filePath);
            // If the file is tracked,
            // remove it from the index and delete it from the working directory.
            if (trackedFileId != null) {
                index.remove(filePath, trackedFileId);
                restrictedDelete(join(CWD, filePath));
            } else if (index.isStagedForAddition(filePath)) {
                // If the file is in the index, remove it
                index.unstageAddition(filePath);
            } else {
                System.out.println("No reason to remove the file.");
                return;
            }
        }
        index.save();
    }

    public void log() {
        Commit currentCommit = getCurrentCommit();
        while (currentCommit != null) {
            currentCommit.printCommit();
            String parentId = currentCommit.getParent();
            if (parentId == null) {
                break;
            }
            currentCommit = Commit.load(parentId);
        }
    }

    public void globalLog() {
        List<Commit> allCommits = getAllCommits();
        for (Commit commit : allCommits) {
            commit.printCommit();
        }
    }

    public void find(String commitMessage) {
        List<Commit> allCommits = getAllCommits();
        boolean found = false;

        for (Commit commit : allCommits) {
            if (commit.getMessage().equals(commitMessage)) {
                System.out.println(commit.getId());
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        Index index = Index.load();

        // Branches
        System.out.println("=== Branches ===");
        String currentBranch = getCurrentBranch();
        List<String> branchNames = plainFilenamesIn(HEADS_DIR);
        assert branchNames != null;
        for (String branchName : branchNames) {
            if (branchName.equals(currentBranch)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();

        // Staged Files
        System.out.println("=== Staged Files ===");
        index.getStagedAdditions().forEach((filePath, blobId) -> {
            System.out.println(filePath);
        });
        System.out.println();

        // Removed Files
        System.out.println("=== Removed Files ===");
        index.getStagedRemovals().forEach((filePath, blobId) -> {
            System.out.println(filePath);
        });
        System.out.println();

        // Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();

        // Untracked Files
        System.out.println("=== Untracked Files ===");
        printUntrackedFiles();
        System.out.println();
    }

    public void checkoutBranch(String branchName) {
        if (!join(HEADS_DIR, branchName).exists()) {
            System.out.println("No such branch exists.");
            return;
        }
        if (getCurrentBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        String targetCommitId = readContentsAsString(join(HEADS_DIR, branchName));
        Commit targetCommit = Commit.load(targetCommitId);

        // --- Untracked File Overwrite Check ---
        boolean success = resetToCommit(targetCommit);
        if (success) {
            updateHead(branchName);
        }
    }

    /**
     * Takes the version of the file as it exists in the head commit and
     * puts it in the working directory, overwriting the version of the
     * file that’s already there if there is one.
     * The new version of the file is not staged.
     * @param filePath the path of the file to checkout.
     */
    public void checkoutFileFromHead(String filePath) {
        Commit currentCommit = getCurrentCommit();
        restoreFile(currentCommit, filePath);
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     * @param commitId the id of the commit to checkout from.
     * @param filePath the path of the file to checkout.
     */
    public void checkoutFileFromCommit(String commitId, String filePath) {
        String fullCommitId = resolveFullCommitId(commitId);
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = Commit.load(fullCommitId);
        restoreFile(commit, filePath);
    }

    public void branch(String branchName) {
        if (join(HEADS_DIR, branchName).exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        // Create the branch
        updateBranchHead(branchName, getHeadCommitId());
    }

    public void removeBranch(String branchName) {
        if (!join(HEADS_DIR, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (getCurrentBranch().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        join(HEADS_DIR, branchName).delete();
    }

    public void reset(String commitId) {
        String fullCommitId = resolveFullCommitId(commitId);
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit targetCommit = Commit.load(fullCommitId);
        boolean success = resetToCommit(targetCommit);
        if (success) {
            updateBranchHead(getCurrentBranch(), fullCommitId);
        }
    }

    public void merge(String branchName) {
        if (!Index.load().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        String currentBranch = getCurrentBranch();
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        String otherBranchCommitId = getBranchCommitId(branchName);
        if (otherBranchCommitId == null) {
            return;
        }
        String currentBranchCommitId = getHeadCommitId();

        String splitPoint = getSplitPoint(currentBranchCommitId, otherBranchCommitId);
        assert splitPoint != null;
        if (splitPoint.equals(otherBranchCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        if (splitPoint.equals(currentBranchCommitId)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            return;
        }

        List<String> untrackedFiles = getUntrackedFiles();
        if (!untrackedFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
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
        return parts[1].substring(parts[1].lastIndexOf("/") + 1);
    }

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

    // --- Checkout Command Helpers ---

    /**
     * Restores the version of the file as it exists in the given commit
     * @param commit The commit to restore from.
     * @param filePath The path of the file to restore.
     */
    private void restoreFile(Commit commit, String filePath) {
        String targetBlobId = commit.getTrackedFileId(filePath);
        if (targetBlobId == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        byte[] content = Blob.load(targetBlobId).getSerializableContent();
        File targetFile = join(CWD, filePath);
        writeContents(targetFile, content);
    }

    // --- Log Command Helpers ---

    /**
     * Gets all the commits in the repository.
     * @return A list of all the commits in the repository.
     */
    private List<Commit> getAllCommits() {
        List<Commit> allCommits = new ArrayList<>();
        String[] subDirNames = OBJECTS_DIR.list();
        if (subDirNames == null) {
            return allCommits;
        }

        for (String dirName : subDirNames) {
            File subDir = join(OBJECTS_DIR, dirName);
            List<String> objectFileNames = plainFilenamesIn(subDir);
            if (objectFileNames == null) {
                continue;
            }

            for (String fileName : objectFileNames) {
                File objectFile = join(subDir, fileName);
                try {
                    Commit commit = readObject(objectFile, Commit.class);
                    allCommits.add(commit);
                } catch (IllegalArgumentException | ClassCastException e) {
                    // not a commit object, skip it
                }
            }
        }

        return allCommits;
    }


    // --- Status Command Helpers ---
    private void printUntrackedFiles() {
        List<String> untrackedFiles = getUntrackedFiles();

        Collections.sort(untrackedFiles);
        untrackedFiles.forEach(System.out::println);
    }

    private List<String> getUntrackedFiles() {
        Index index = Index.load();
        List<String> untrackedFiles = new ArrayList<>();

        Commit headCommit = getCurrentCommit();
        Set<String> trackedFiles = headCommit.getAllFiles().keySet();
        Set<String> stagedForAdditionFileNames = index.getStagedAdditions().keySet();
        Set<String> stagedForRemovalFileNames = index.getStagedRemovals().keySet();
        getUntrackedFiles("", trackedFiles,
                stagedForAdditionFileNames, stagedForRemovalFileNames, untrackedFiles);
        return untrackedFiles;
    }

    // Including files that have been staged for removal,
    // but then re-created without Gitlet’s knowledge.
    private void getUntrackedFiles(String dirPath, Set<String> trackedFiles,
                                   Set<String> stagedForAdditionFiles,
                                   Set<String> stagedForRemovalFiles,
                                   List<String> untrackedFiles) {
        String[] files = join(CWD, dirPath).list();
        if (files == null) {
            return;
        }
        for (String fileName : files) {
            if (fileName.equals(".gitlet")) {
                continue;
            }

            String filePath = join(dirPath, fileName).getPath().replace('\\', '/');
            boolean isTrackedAndNotRemoved = trackedFiles.contains(filePath)
                    && !stagedForRemovalFiles.contains(filePath);
            boolean isStagedForAddition = stagedForAdditionFiles.contains(filePath);

            if (!isTrackedAndNotRemoved && !isStagedForAddition) {
                if (new File(filePath).isDirectory()) {
                    getUntrackedFiles(filePath, trackedFiles, stagedForAdditionFiles,
                            stagedForRemovalFiles, untrackedFiles);
                } else {
                    untrackedFiles.add(filePath);
                }
            }
        }
    }

    // --- Checkout Branch & Reset Command Helpers ---
    private boolean resetToCommit(Commit targetCommit) {
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            return false;
        }

        Set<String> targetTrackedFiles = targetCommit.getAllFiles().keySet();
        List<String> untrackedFiles = getUntrackedFiles();

        for (String untrackedFile : untrackedFiles) {
            if (targetTrackedFiles.contains(untrackedFile)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return false;
            }
        }

        // restore files
        for (String filePath : targetTrackedFiles) {
            restoreFile(targetCommit, filePath);
        }

        // remove extra files
        Commit currentCommit = getCurrentCommit();
        Set<String> currentTrackedFiles = currentCommit.getAllFiles().keySet();
        for (String filePath : currentTrackedFiles) {
            if (!targetTrackedFiles.contains(filePath)) {
                restrictedDelete(join(CWD, filePath));
            }
        }

        Index.clearIndex();
        return true;
    }

    /**
     * Resolves a short or full commit ID to the full 40-character SHA-1 ID.
     * This method searches the object database for a unique commit ID that starts
     * with the given prefix.
     *
     * @param shortId The potentially abbreviated commit ID from the user.
     * @return The full 40-character SHA-1 ID if a unique match is found;
     * otherwise, returns null if the ID is not found or is ambiguous.
     */
    private String resolveFullCommitId(String shortId) {
        // First, handle the common case of a full-length ID directly.
        if (shortId != null && shortId.length() == 40) {
            return GitObject.getFile(shortId).exists() ? shortId : null;
        }

        // Default the result to null. We only update it if a unique match is found.
        String result = null;

        // Proceed only if the shortId is a valid candidate for a prefix search.
        if (shortId != null && shortId.length() >= 2) {
            String subDirName = shortId.substring(0, 2);
            File subDir = join(OBJECTS_DIR, subDirName);

            // Only search if the corresponding subdirectory exists.
            if (subDir.isDirectory()) {
                String restOfShortId = shortId.substring(2);
                String[] objectFileNames = subDir.list();
                List<String> matches = new ArrayList<>();

                if (objectFileNames != null) {
                    for (String fileName : objectFileNames) {
                        if (fileName.startsWith(restOfShortId)) {
                            matches.add(subDirName + fileName);
                        }
                    }
                }

                // If and only if we found exactly one match, we have our result.
                if (matches.size() == 1) {
                    result = matches.get(0);
                }
            }
        }

        return result;
    }

    // --- Merge Command Helpers ---
    private String getBranchCommitId(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            return null;
        }
        return readContentsAsString(branchFile);
    }

    private String getSplitPoint(String commitId1, String commitId2) {
        Set<String> ancestors1 = getAncestors(commitId1);

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(commitId2);
        visited.add(commitId2);

        while (!queue.isEmpty()) {
            String currentCommitId = queue.poll();
            if (ancestors1.contains(currentCommitId)) {
                return currentCommitId;
            }

            Commit currentCommit = Commit.load(currentCommitId);
            assert currentCommit != null;
            String parentId = currentCommit.getParent();

            if (parentId != null && visited.add(parentId)) {
                queue.offer(parentId);
            }
            String secondParentId = currentCommit.getSecondParent();
            if (secondParentId != null && visited.add(secondParentId)) {
                queue.offer(secondParentId);
            }
        }

        return null; // Should not happen
    }

    private Set<String> getAncestors(String commitId) {
        Set<String> ancestors = new HashSet<>();
        dfs(commitId, ancestors);
        return ancestors;
    }

    private void dfs(String commitId, Set<String> ancestors) {
        ancestors.add(commitId);
        Commit commit = Commit.load(commitId);
        if (commit == null) {
            return;
        }
        String parentId = commit.getParent();
        if (parentId != null) {
            if (ancestors.add(parentId)) {
                dfs(parentId, ancestors);
            }
        }
        String secondParentId = commit.getSecondParent();
        if (secondParentId != null) {
            if (ancestors.add(secondParentId)) {
                dfs(secondParentId, ancestors);
            }
        }
    }
}
