package gitlet;

import java.util.Arrays;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Akane
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // Args is empty
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        int length = args.length;

        Repository repo = new Repository();

        switch (firstArg) {
            case "init":
                if (length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                repo.init();
                return;
            default:
                break;
        }

        // Check if in an initialized Gitlet directory
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        switch (firstArg) {
            case "add":
                checkNumberOfOperands(length, 2);
                repo.add(Arrays.copyOfRange(args, 1, length));
                break;
            case "commit":
                if (length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                repo.commit(args[1]);
                break;
            case "rm":
                checkNumberOfOperands(length, 2);
                repo.rm(Arrays.copyOfRange(args, 1, length));
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    /** Helper method to check the number of operands. */
    private static void checkNumberOfOperands(int actualLength, int atLeastLength) {
        if (actualLength < atLeastLength) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
