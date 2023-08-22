package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Manu John
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        } else if (args[0].equals("init")) {
            GitletMethods.init();
            return;
        } else if (!GitletMethods.GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        } else if (args[0].equals("add")) {
            GitletMethods.add(args[1]);
            return;
        } else if (args[0].equals("commit")) {
            GitletMethods.commit(args[1], null);
            return;
        } else if (args[0].equals("log")) {
            GitletMethods.log();
            return;
        } else if (args[0].equals("checkout")) {
            if (args[1].equals("--")) {
                GitletMethods.checkout(args[2]);
            } else if (args.length == 2) {
                GitletMethods.checkoutBranch(args[1]);
            } else if (args[2].equals("--")) {
                GitletMethods.checkout(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
            }
            return;
        } else if (args[0].equals("rm")) {
            GitletMethods.rm(args[1]);
            return;
        } else if (args[0].equals("global-log")) {
            GitletMethods.globalLog();
            return;
        } else if (args[0].equals("find")) {
            GitletMethods.find(args[1]);
            return;
        } else if (args[0].equals("status")) {
            GitletMethods.status();
            return;
        } else if (args[0].equals("branch")) {
            GitletMethods.branch(args[1]);
            return;
        } else if (args[0].equals("rm-branch")) {
            GitletMethods.rmbranch(args[1]);
            return;
        } else if (args[0].equals("reset")) {
            GitletMethods.reset(args[1]);
            return;
        } else if (args[0].equals("merge")) {
            GitletMethods.merge(args[1]);
            return;
        } else {
            System.out.println("No command with that name exists.");
            return;
        }
    }

}
