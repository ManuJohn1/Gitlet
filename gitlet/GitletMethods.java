package gitlet;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

public class GitletMethods {
    /** head. */
    static final File GITLET = Utils.join(
            new File(System.getProperty("user.dir")), ".gitlet");
    /** head. */
    static final File STAGING_AREA =  Utils.join(GITLET, ".stage");
    /** head. */
    static final File STAGE_FOR_ADDITION =  Utils.join(
            STAGING_AREA, ".add_stage");
    /** head. */
    static final File STAGE_FOR_REMOVAL =  Utils.join(
            STAGING_AREA, ".remove_stage");
    /** head. */
    static final File COMMITS =  Utils.join(GITLET, ".commits");
    /** head. */
    static final File BRANCHES =  Utils.join(GITLET, ".branches");
    /** head. */
    static final File BLOBS =  Utils.join(GITLET, ".blobs");
    /** head. */
    static final File HEAD = Utils.join(BRANCHES, "head.txt");

    public static void init() throws IOException {
        if (GITLET.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        }

        GITLET.mkdir();
        STAGING_AREA.mkdir();
        STAGE_FOR_ADDITION.mkdir();
        STAGE_FOR_REMOVAL.mkdir();
        COMMITS.mkdir();
        BRANCHES.mkdir();
        BLOBS.mkdir();
        HEAD.createNewFile();

        Commit initCommit = new Commit("initial commit",
                new Date(0), null, null);
        File initCommitFile = getCommitFile(initCommit);
        initCommitFile.createNewFile();
        Utils.writeObject(initCommitFile, initCommit);

        File master = Utils.join(BRANCHES, "master.txt");
        master.createNewFile();

        Utils.writeContents(master,
                Utils.sha1(initCommit.toStringWithoutSha1()));
        Utils.writeContents(HEAD, "master");
    }

    public static void add(String s) throws IOException {
        File f = Utils.join(new File(System.getProperty("user.dir")), s);
        if (!f.exists()) {
            System.out.println("File does not exist");
            return;
        }

        File stageRemovalFile = Utils.join(STAGE_FOR_REMOVAL, f.getName());
        if (stageRemovalFile.exists()) {
            stageRemovalFile.delete();
            return;
        }

        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        if (currCommit.getFileMap() != null) {
            String blobName = currCommit.getFileMap().get(s);
            if (blobName != null) {
                File currFile = Utils.join(BLOBS, blobName);
                if (Utils.readContentsAsString(f).
                        equals(Utils.readContentsAsString(currFile))) {
                    if (currFile.exists()) {
                        currFile.delete();
                    }
                    return;
                }
            }
        }

        File stagedCopy = Utils.join(STAGE_FOR_ADDITION, f.getName());
        if (!stagedCopy.exists()) {
            stagedCopy.createNewFile();
        }
        Utils.writeContents(stagedCopy, Utils.readContentsAsString(f));
    }


    public static void commit(String message, String otherParent)
            throws IOException {
        if (STAGE_FOR_ADDITION.list().length == 0
                && STAGE_FOR_REMOVAL.list().length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        } else if (message.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }

        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);
        Commit newComm = new Commit(
                message, new Date(),
                Utils.sha1(currCommit.toStringWithoutSha1()), otherParent);
        newComm.copyFileMap(currCommit.getFileMap());

        for (String s: STAGE_FOR_ADDITION.list()) {
            File currFile = Utils.join(STAGE_FOR_ADDITION, s);
            String blobId = createBlobId(currFile);
            File blobCopy = Utils.join(BLOBS, blobId);
            blobCopy.createNewFile();
            Utils.writeContents(blobCopy, Utils.readContentsAsString(currFile));
            if (newComm.getFileMap().containsKey(s)) {
                newComm.updateFileMapVal(s, blobId);
            } else {
                newComm.addFileMapVal(s, blobId);
            }
            currFile.delete();
        }

        for (String s: STAGE_FOR_REMOVAL.list()) {
            File stageRemovalFile = Utils.join(STAGE_FOR_REMOVAL, s);
            newComm.removeFileMapVal(s);
            stageRemovalFile.delete();
        }

        File newCommFile = getCommitFile(
                Utils.sha1(newComm.toStringWithoutSha1()));
        newCommFile.createNewFile();
        Utils.writeObject(newCommFile, newComm);
        Utils.writeContents(getBranchFile(),
                Utils.sha1(newComm.toStringWithoutSha1()));
    }


    public static void log() {
        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        while (!currCommit.getMessage().equals("initial commit")) {
            System.out.println(currCommit);
            File f = getCommitFile(currCommit.getPrevPointer());
            Commit prevCommit = Utils.readObject(f, Commit.class);
            currCommit = prevCommit;
        }
        System.out.println(currCommit);
    }

    public static void globalLog() {
        for (String s: Utils.plainFilenamesIn(COMMITS)) {
            File c = Utils.join(COMMITS, s);
            Commit currCommit = Utils.readObject(c, Commit.class);
            System.out.println(currCommit);
        }
    }

    public static void find(String message) {
        ArrayList<String> arr = new ArrayList<>();
        for (String s: Utils.plainFilenamesIn(COMMITS)) {
            File c = Utils.join(COMMITS, s);
            Commit currCommit = Utils.readObject(c, Commit.class);
            if (currCommit.getMessage().equals(message)) {
                arr.add(Utils.sha1(currCommit.toStringWithoutSha1()));
            }
        }

        if (arr.size() == 0) {
            System.out.println("Found no commit with that message.");
            return;
        }

        for (String a: arr) {
            System.out.println(a);
        }
    }

    public static void checkout(String fileS) throws IOException {
        File cwdFile = Utils.join(
                new File(System.getProperty("user.dir")), fileS);
        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        if (!currCommit.getFileMap().containsKey(fileS)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (!cwdFile.exists()) {
            cwdFile.createNewFile();
        }

        String blobId = currCommit.getFileMap().get(fileS);
        File blobPointer = Utils.join(BLOBS, blobId);
        Utils.writeContents(cwdFile, Utils.readContentsAsString(blobPointer));
    }

    public static void checkout(String commitId, String fileS)
            throws IOException {
        commitId = shortenedCommitId(commitId);
        File cwdFile = Utils.join(
                new File(System.getProperty("user.dir")), fileS);
        boolean cond = false;
        for (String s: COMMITS.list()) {
            if (s.substring(0, s.length() - 4).equals(commitId)) {
                cond = true;
            }
        }

        if (!cond) {
            System.out.println("No commit with that id exists.");
            return;
        }

        File currCommitFile = getCommitFile(commitId);
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        if (!currCommit.getFileMap().containsKey(fileS)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (!cwdFile.exists()) {
            cwdFile.createNewFile();
        }

        String blobId = currCommit.getFileMap().get(fileS);
        File blobPointer = Utils.join(BLOBS, blobId);
        Utils.writeContents(cwdFile, Utils.readContentsAsString(blobPointer));
    }

    public static void checkoutBranch(String branch) throws IOException {
        File branchFile = Utils.join(BRANCHES, branch + ".txt");
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }

        String commitId = Utils.readContentsAsString(branchFile);
        File branchCommitFile = getCommitFile(commitId);
        Commit branchCommit = Utils.readObject(branchCommitFile, Commit.class);

        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        for (String file: branchCommit.getFileMap().keySet()) {
            File cwdFile = Utils.join(
                    new File(System.getProperty("user.dir")), file);
            if (cwdFile.exists()
                    && !currCommit.getFileMap().containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        } else if (branch.equals(Utils.readContentsAsString(HEAD))) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        for (String file: branchCommit.getFileMap().keySet()) {
            checkout(commitId, file);
        }

        for (String file: currCommit.getFileMap().keySet()) {
            if (!branchCommit.getFileMap().containsKey(file)) {
                File cwdFile = Utils.join(
                        new File(System.getProperty("user.dir")), file);
                Utils.restrictedDelete(cwdFile);
            }
        }

        clearStage();
        Utils.writeContents(HEAD, branch);
    }

    public static void branch(String branchName) throws IOException {
        File branch = createBranchFile(branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branch.createNewFile();
        Utils.writeContents(branch, getCurrBranchAsString());
    }

    public static void rmbranch(String branchName) throws IOException {
        File branch = createBranchFile(branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (Utils.readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch.delete();
    }

    public static void rm(String fileName) throws IOException {
        File cwdFile = Utils.join(
                new File(System.getProperty("user.dir")), fileName);
        File stageFile = Utils.join(STAGE_FOR_ADDITION, fileName);
        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        if (!stageFile.exists()
                && !currCommit.getFileMap().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (stageFile.exists()) {
            stageFile.delete();
        }

        if (currCommit.getFileMap().containsKey(fileName)) {
            File stageRemovalFile = Utils.join(STAGE_FOR_REMOVAL, fileName);
            stageRemovalFile.createNewFile();
            if (cwdFile.exists()) {
                Utils.restrictedDelete(cwdFile);
            }
        }
    }

    public static void reset(String commitId) throws IOException {
        commitId = shortenedCommitId(commitId);
        File commitFile = getCommitFile(commitId);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        branch("tempgfdgnfdtgrdgtrdr");
        String currBranch = Utils.readContentsAsString(
                Utils.join(BRANCHES, "head.txt"));
        File temp = Utils.join(BRANCHES, "tempgfdgnfdtgrdgtrdr.txt");
        Utils.writeContents(temp, commitId);
        checkoutBranch("tempgfdgnfdtgrdgtrdr");
        Utils.writeContents(HEAD, currBranch);
        rmbranch("tempgfdgnfdtgrdgtrdr");
        Utils.writeContents(
                Utils.join(BRANCHES, currBranch + ".txt"), commitId);
    }

    public static void status() {
        System.out.println("=== Branches ===");
        String[] branchArr = BRANCHES.list();
        Arrays.sort(branchArr);
        for (String branch: branchArr) {
            if (!branch.equals("head.txt")) {
                if (branch.equals(Utils.readContentsAsString(HEAD) + ".txt")) {
                    System.out.print("*");
                }
                System.out.println(branch.substring(0, branch.length() - 4));
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        String[] stageArr = STAGE_FOR_ADDITION.list();
        Arrays.sort(stageArr);
        for (String stageFile: stageArr) {
            System.out.println(stageFile);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        String[] removeArr = STAGE_FOR_REMOVAL.list();
        Arrays.sort(removeArr);
        for (String trashFile: removeArr) {
            System.out.println(trashFile);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);
        ArrayList<String> modificationsNotStagedForCommit = new ArrayList<>();

        modificationCheck(currCommit, modificationsNotStagedForCommit);

        otherCheck(modificationsNotStagedForCommit);

        Collections.sort(modificationsNotStagedForCommit);
        for (String s: modificationsNotStagedForCommit) {
            System.out.println(s);
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");
        List<String> cwdTrack = Utils.plainFilenamesIn
                (System.getProperty("user.dir"));
        Collections.sort(cwdTrack);
        for (String file: cwdTrack) {
            File stageAddFile = Utils.join(STAGE_FOR_ADDITION, file);
            File stageRemoveFile = Utils.join(STAGE_FOR_REMOVAL, file);

            if (!stageAddFile.exists() && !stageRemoveFile.exists()
                    && !currCommit.getFileMap().containsKey(file)) {
                System.out.println(file);
            }
        }
    }

    private static void otherCheck(ArrayList<String>
                                           modificationsNotStagedForCommit) {
        for (String s: STAGE_FOR_ADDITION.list()) {
            File cwdFile = Utils.join(
                    new File(System.getProperty("user.dir")), s);
            File stageAddFile = Utils.join(STAGE_FOR_ADDITION, s);
            if (!cwdFile.exists()) {
                modificationsNotStagedForCommit.add(s + " (deleted)");
            } else if (!Utils.readContentsAsString
                    (cwdFile).equals(Utils.readContentsAsString
                    (stageAddFile))) {
                modificationsNotStagedForCommit.add(s + " (modified)");
            }
        }
    }

    private static void modificationCheck(Commit currCommit,
                                          ArrayList<String>
                                                  modif) {
        for (String s: currCommit.getFileMap().keySet()) {
            File cwdFile = Utils.join(
                    new File(System.getProperty("user.dir")), s);
            File blobFile = Utils.join(BLOBS, currCommit.getFileMap().get(s));
            File stageAddFile = Utils.join(STAGE_FOR_ADDITION, s);
            File stageRmFile = Utils.join(STAGE_FOR_REMOVAL, s);
            if (!cwdFile.exists() && !stageRmFile.exists()) {
                modif.add(s + " (deleted)");
            } else if (cwdFile.exists()
                    && blobFile.exists() && !stageAddFile.exists()
                && !Utils.readContentsAsString
                    (cwdFile).equals(Utils.readContentsAsString(blobFile))) {
                modif.add(s + " (modified)");
            }
        }
    }

    public static void merge(String mergedInBranchName) throws IOException {
        File mergedInBranch = createBranchFile(mergedInBranchName);
        File currCommitFile = getCommitFile(getCurrBranchAsString());
        Commit currCommit = Utils.readObject(currCommitFile, Commit.class);

        if (checkTrackedFiles(currCommit)) {
            return;
        }

        if (mergeErrorHandling(mergedInBranchName, mergedInBranch)) {
            return;
        }

        File mergedInBranchCommitFile =
                getCommitFile(Utils.readContentsAsString(mergedInBranch));
        Commit branchCommit =
                Utils.readObject(mergedInBranchCommitFile, Commit.class);
        Commit splitPointCommit = findSplitPoint(currCommit, branchCommit);

        if (moreMergeHandling(mergedInBranchName,
                currCommit, branchCommit, splitPointCommit)) {
            return;
        }

        Set<String> currCommitSet = currCommit.getFileMap().keySet();
        Set<String> branchCommitSet = branchCommit.getFileMap().keySet();
        Set<String> splitPointCommitSet =
                splitPointCommit.getFileMap().keySet();
        ArrayList<String> arr =
                mergeArr(currCommitSet,
                        branchCommitSet, splitPointCommitSet);
        boolean cond = false;

        for (String fileName: arr) {
            File cwdFile = Utils.join
                    (new File(System.getProperty("user.dir")),
                            fileName);
            String currentCommitFileId = getBlobId(currCommit, fileName);
            String otherCommitFileId = getBlobId(branchCommit, fileName);
            String splitCommitFileId = getBlobId(splitPointCommit, fileName);

            cond = isCond(branchCommit, cond, fileName,
                    cwdFile, currentCommitFileId,
                    otherCommitFileId, splitCommitFileId);
            continue;
        }

        commit("Merged "
                + mergedInBranchName
                + " into " +  Utils.readContentsAsString(HEAD)
                + ".", Utils.readContentsAsString(mergedInBranch));

        if (cond) {
            System.out.println("Encountered a merge conflict.");
            return;
        }
    }

    private static boolean isCond(Commit branchCommit,
                                  boolean cond, String fileName, File cwdFile,
                                  String currentCommitFileId,
                                  String otherCommitFileId,
                                  String splitCommitFileId) throws IOException {
        if (splitCommitFileId != null
                && otherCommitFileId != null && currentCommitFileId != null
                && !splitCommitFileId.equals(otherCommitFileId)
            && splitCommitFileId.equals(currentCommitFileId)) {
            checkout(Utils.sha1
                    (branchCommit.toStringWithoutSha1()),
                    fileName);
            add(fileName);
        } else if (splitCommitFileId != null
                && otherCommitFileId != null && currentCommitFileId != null
                && splitCommitFileId.equals(otherCommitFileId)
                && !splitCommitFileId.equals(currentCommitFileId)) {
            return cond;
        } else if ((currentCommitFileId == null
                && otherCommitFileId == null) || (currentCommitFileId != null
                && otherCommitFileId != null && currentCommitFileId.equals
                (otherCommitFileId))) {
            return cond;
        } else if (splitCommitFileId == null && otherCommitFileId == null
            && currentCommitFileId != null) {
            return cond;
        } else if (splitCommitFileId == null && currentCommitFileId == null
                && otherCommitFileId != null) {
            checkout(Utils.sha1
                    (branchCommit.toStringWithoutSha1()),
                    fileName);
            add(fileName);
        } else if (splitCommitFileId != null && currentCommitFileId != null
                && splitCommitFileId.equals(currentCommitFileId)
            && otherCommitFileId == null) {
            rm(fileName);
        } else if (splitCommitFileId != null && otherCommitFileId != null
                && splitCommitFileId.equals(otherCommitFileId)
                && currentCommitFileId == null) {
            return cond;
        } else if ((otherCommitFileId == null
                && currentCommitFileId != null) || (currentCommitFileId == null
                && otherCommitFileId != null) || (currentCommitFileId != null
                && !currentCommitFileId.equals
                (otherCommitFileId))) {
            File currCommFile;
            File mergedInCommFile;
            String currString = "";
            String otherString = "";
            if (currentCommitFileId != null) {
                currCommFile = Utils.join(BLOBS, currentCommitFileId);
                currString = Utils.readContentsAsString(currCommFile);
            }
            if (otherCommitFileId != null) {
                mergedInCommFile = Utils.join(BLOBS, otherCommitFileId);
                otherString = Utils.readContentsAsString(mergedInCommFile);
            }
            Utils.writeContents(cwdFile, "<<<<<<< HEAD\n"
                    + currString + "=======\n" + otherString + ">>>>>>>\n");
            add(fileName);
            cond = true;
        }
        return cond;
    }

    private static boolean moreMergeHandling(String mergedInBranchName,
                                             Commit currCommit, Commit
                                                     branchCommit,
                                             Commit splitPointCommit)
            throws IOException {
        if ((splitPointCommit.toStringWithoutSha1().
                equals(branchCommit.toStringWithoutSha1()))) {
            System.out.println("Given branch is an ancestor of the "
                            + "current branch.");
            return true;
        } else if ((splitPointCommit.toStringWithoutSha1().
                equals(currCommit.toStringWithoutSha1()))) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(mergedInBranchName);
            return true;
        }
        return false;
    }

    private static boolean mergeErrorHandling(String mergedInBranchName,
                                              File mergedInBranch) {
        if (STAGE_FOR_ADDITION.list().length > 0
                || STAGE_FOR_REMOVAL.list().length > 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (!mergedInBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (Utils.readContentsAsString(HEAD).
                equals(mergedInBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }

    private static boolean checkTrackedFiles(Commit currCommit) {
        for (String cwdFilename: Utils.plainFilenamesIn(
                System.getProperty("user.dir"))) {
            File cwdFile = Utils.join(
                    new File(System.getProperty("user.dir")), cwdFilename);
            File stagingAddFile = Utils.join(STAGE_FOR_ADDITION, cwdFilename);
            File stagingRemoveFile = Utils.join(STAGE_FOR_REMOVAL, cwdFilename);
            if (cwdFile.exists() && !stagingAddFile.exists()
                    && !stagingRemoveFile.exists()
                && !currCommit.getFileMap().containsKey(cwdFilename)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    public static Commit findSplitPoint(Commit curr, Commit other) {
        Commit currPointer = curr;
        Commit otherPointer = other;
        Commit extraCurrPointer = null;
        Commit extraOtherPointer = null;

        while (!currPointer.getMessage().equals("initial commit")) {
            while (!otherPointer.getMessage().equals("initial commit")) {
                if (currPointer.toStringWithoutSha1().
                        equals(otherPointer.
                                toStringWithoutSha1())
                        || (extraOtherPointer != null
                        && currPointer.toStringWithoutSha1().
                                equals(extraOtherPointer.
                                        toStringWithoutSha1()))) {
                    return currPointer;
                } else if (extraCurrPointer != null && (
                        extraCurrPointer.toStringWithoutSha1().
                                equals(otherPointer.toStringWithoutSha1())
                                || (extraOtherPointer != null
                                && extraCurrPointer.toStringWithoutSha1().equals
                                        (extraOtherPointer.
                                                toStringWithoutSha1())))) {
                    return extraCurrPointer;
                }

                if (otherPointer.getOtherPrevPointer() != null) {
                    extraOtherPointer = Utils.readObject
                            (getCommitFile
                                    (otherPointer.
                                            getOtherPrevPointer()),
                                    Commit.class);
                }
                otherPointer = Utils.readObject
                        (getCommitFile(otherPointer.getPrevPointer()),
                                Commit.class);
            }
            otherPointer = other;
            if (currPointer.getOtherPrevPointer() != null) {
                extraCurrPointer = Utils.readObject
                        (getCommitFile
                                (currPointer.getOtherPrevPointer()),
                                Commit.class);
            }
            currPointer = Utils.readObject
                    (getCommitFile(currPointer.getPrevPointer()), Commit.class);
        }
        return currPointer;
    }

    public static String getBlobId(Commit c, String fileName) {
        return c.getFileMap().get(fileName);
    }

    public static ArrayList<String> mergeArr(Set<String> currCommitArr,
                                             Set<String> branchCommitArr,
                                             Set<String> splitPointArr) {
        ArrayList<String> arr = new ArrayList<>();
        for (String s: currCommitArr) {
            arr.add(s);
        }

        for (String s: branchCommitArr) {
            if (!arr.contains(s)) {
                arr.add(s);
            }
        }

        for (String s: splitPointArr) {
            if (!arr.contains(s)) {
                arr.add(s);
            }
        }

        return arr;
    }

    /**
     * This is a helper method that returns
     * the current active branch which is in the HEAD file.
     * @return File.
     */
    public static File getCurrBranch() {
        return Utils.join(BRANCHES, Utils.readContentsAsString(HEAD) + ".txt");
    }

    /**
     * This is a helper method that returns the most recent commit.
     * in the form of a sha1 code(of Commit.ToStringWithoutSha1).
     * @return String.
     */
    public static String getCurrBranchAsString() {
        File currBranchFile = getCurrBranch();
        return Utils.readContentsAsString(currBranchFile);
    }

    public static File getCommitFile(Commit c) {
        return Utils.join(COMMITS,
                Utils.sha1(c.toStringWithoutSha1()) + ".txt");
    }

    public static File getCommitFile(String sha1) {
        return Utils.join(COMMITS, sha1 + ".txt");
    }

    public static File getBranchFile() {
        return Utils.join(BRANCHES, Utils.readContentsAsString(HEAD) + ".txt");
    }

    public static String createBlobId(File currFile) {
        return "blob"
                + Utils.sha1(Utils.readContentsAsString(currFile))
                + ".txt";
    }

    public static File createBranchFile(String branch) {
        return Utils.join(BRANCHES, branch + ".txt");
    }

    public static void clearStage() {
        for (String stageFileName: STAGE_FOR_ADDITION.list()) {
            File stageFile = Utils.join(STAGE_FOR_ADDITION, stageFileName);
            stageFile.delete();
        }

        for (String stageFileName: STAGE_FOR_REMOVAL.list()) {
            File stageFile = Utils.join(STAGE_FOR_REMOVAL, stageFileName);
            stageFile.delete();
        }
    }

    public static String shortenedCommitId(String shortVers) {
        for (String s: COMMITS.list()) {
            String longCom = s.substring(0, s.length() - 4);
            if (shortVers.equals(longCom.substring(0, shortVers.length()))) {
                return longCom;
            }
        }
        return null;
    }



}
