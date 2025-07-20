package www.etis_midi.fr.panda.mapper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Utility class for writing matrix data and mappings to files.
 */
public class MatrixWriter {

    /**
     * Writes the binary matrix to a file.
     * Each line shows the 1-based column indices that are active for each subject.
     */
    public static void writeMatrix(int[][] matrix, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            for (int[] row : matrix) {
                for (int j = 0; j < row.length; j++) {
                    if (row[j] != 0) {
                        writer.write((j + 1) + " ");
                    }
                }
                writer.write("\n");
            }
        }
    }

    /**
     * Writes a mapping from column index to predicate/type.
     */
    public static void writePredicates(Map<String, Integer> predicateMap, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            for (Map.Entry<String, Integer> entry : predicateMap.entrySet()) {
                writer.write((entry.getValue() + 1) + " " + entry.getKey() + "\n");
            }
        }
    }

    /**
     * Writes a mapping from row index to RDF subject resource (URI or blank node ID).
     */
    public static void writeSubjects(Map<Resource, Integer> instanceMap, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            for (Map.Entry<Resource, Integer> entry : instanceMap.entrySet()) {
                Resource subject = entry.getKey();
                int index = entry.getValue();

                String subjectId = subject.isAnon() ? "_:" + subject.getId().getLabelString()
                                                    : subject.getURI();

                writer.write(index + " " + subjectId + "\n");
            }
        }
    }
}
