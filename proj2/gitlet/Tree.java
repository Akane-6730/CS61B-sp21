package gitlet;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class Tree extends GitObject {
    /**
     * The core data structure of the Tree.
     * It maps a filename or directory name (String) to its corresponding TreeEntry.
     * Using TreeMap ensures entries are always sorted by name, which is crucial for
     * generating a consistent, deterministic SHA-1 hash for the tree's content.
     */
    private final TreeMap<String, TreeEntry> entries;

    public Tree() {
        // 1. Call the parent constructor, explicitly setting this object's type.
        super("tree");
        // 2. Initialize the internal map.
        this.entries = new TreeMap<>();
    }

    public static class TreeEntry implements Serializable {
        /** The type of the object this entry points to, either "blob" or "tree". */
        public final String type;
        /** The SHA-1 hash of the object this entry points to. */
        public final String id;

        public TreeEntry(String type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    /**
     * Implements the abstract method from GitObject.
     * This method defines how a Tree object is converted into a canonical
     * byte array representation, which is then used for hashing by the parent class.
     * The stability of this representation is critical for consistent ID generation.
     *
     * @return The canonical byte array representation of this tree's content.
     */
    @Override
    public byte[] getSerializableContent() {
        // Use a StringBuilder for efficient string concatenation in a loop.
        StringBuilder contentBuilder = new StringBuilder();

        // Iterate through the TreeMap. Because it's a TreeMap, the iteration order
        // is guaranteed to be sorted alphabetically by the entry's name (the key).
        // This ensures the final string is always the same for the same set of entries.
        for (Map.Entry<String, TreeEntry> mapEntry : this.entries.entrySet()) {
            String name = mapEntry.getKey();
            TreeEntry entryData = mapEntry.getValue();

            // Append each entry's information in a fixed, standardized format.
            // A simple format like "{type} {id} {name}\n" is robust and easy to debug.
            // The newline character '\n' acts as a clear separator between entries.
            contentBuilder.append(entryData.type)
                    .append(" ")
                    .append(entryData.id)
                    .append(" ")
                    .append(name)
                    .append("\n");
        }

        // Convert the final, aggregated string into a byte array using a standard
        // character set (UTF-8) to ensure consistency across all computer systems.
        return contentBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns a read-only view of the entries in this tree.
     * @return A map of names to TreeEntry objects.
     */
    public Map<String, TreeEntry> getEntries() {
        return entries;
    }

    /**
     * Retrieves a specific TreeEntry by its name.
     * @param name The name of the file or directory.
     * @return The TreeEntry object, or null if no entry with that name exists.
     */
    public TreeEntry getEntry(String name) {
        return entries.get(name);
    }
}
