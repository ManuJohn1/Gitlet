package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class Commit implements Serializable {
    /** message. */
    private String message;
    /** date. */
    private Date d;
    /** prev pointer. */
    private String prevPointer;
    /** other prev pointer. */
    private String otherPrevPointer;
    /** maps the file names to the blob ids (sha1(commit.ToStringWithoutId)). */
    private TreeMap<String, String> fileMap;


    public Commit(String mes, Date date, String prev, String other) {
        this.message = mes;
        this.d = date;
        this.prevPointer = prev;
        this.otherPrevPointer = other;
        this.fileMap = new TreeMap<>();
    }

    public String getMessage() {
        return message;
    }

    public String getPrevPointer() {
        return prevPointer;
    }

    public Date getDate() {
        return d;
    }

    public TreeMap<String, String> getFileMap() {
        return fileMap;
    }

    public String getOtherPrevPointer() {
        return otherPrevPointer;
    }

    public void setOtherPrevPointer(String sha1) {
        this.otherPrevPointer = sha1;
    }

    public String getFile(String s) {
        return fileMap.get(s);
    }

    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("EE LLL dd kk:mm:ss yyyy Z");
        if (otherPrevPointer == null) {
            return "===\n" + "commit " + Utils.sha1(this.toStringWithoutSha1())
                    + "\n" + "Date: " + df.format(d) + "\n" + message + "\n";
        } else {
            String parentCode = prevPointer.substring(0, 7)
                    + " " + otherPrevPointer.substring(0, 7);
            return "===\n" + "commit " + Utils.sha1(this.toStringWithoutSha1())
                    + "\n" + "Merge: " + parentCode
                    + "\n" + "Date: " + df.format(d) + "\n" + message + "\n";
        }
    }

    public String toStringWithoutSha1() {
        String returnVal = "===\n"
                + "Date: " + d.toString() + "\n" + message
                + "\n" + "prev_pointer" + prevPointer;

        if (fileMap != null) {
            for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                returnVal += "key: "
                        + entry.getKey() + "value " + entry.getValue();
            }
        }

        return returnVal;
    }


    public void updateFileMapVal(String fileName, String commitId) {
        for (String s: fileMap.keySet()) {
            if (fileName.equals(s)) {
                fileMap.put(s, commitId);
            }
        }
    }

    public void addFileMapVal(String fileName, String commitId) {
        fileMap.put(fileName, commitId);
    }

    public void removeFileMapVal(String fileName) {
        fileMap.remove(fileName);
    }

    public void copyFileMap(TreeMap<String, String> fm) {
        for (Map.Entry<String, String> entry: fm.entrySet()) {
            this.fileMap.put(entry.getKey(), entry.getValue());
        }
    }



}
