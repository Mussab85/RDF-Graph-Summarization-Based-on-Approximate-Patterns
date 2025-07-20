package www.etis_midi.fr.panda.mapper;

import java.awt.Dimension;
import java.util.*;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;



/**
 * Builds a binary matrix from an RDF model.
 * - Rows represent unique RDF instances (subjects + blank nodes)
 * - Columns represent predicates, reverse predicates, and rdf:type
 */
public class MatrixBuilder {
    private final Model model;

    // Mapping of predicates (columns): key = predicate or type, value = column index
    private final Map<String, Integer> predicateMap = new LinkedHashMap<>();

    // Mapping of instances (rows): key = RDF Resource (subject or blank node), value = row index
    private final Map<Resource, Integer> instanceMap = new LinkedHashMap<>();

    private int[][] matrix;

    public MatrixBuilder(Model model) {
        this.model = model;
    }

    /**
     * Main entry to build the binary matrix and initialize mappings.
     */
    public void build() {
        List<Statement> statements = model.listStatements().toList();
        Dimension dim = calculateDimensions(statements);
        matrix = new int[dim.width][dim.height];
        populateMatrix();
    }

    /**
     * Scans RDF statements to:
     * - Identify all unique resources (subjects + blank nodes as objects)
     * - Identify all predicates, reverse properties, and rdf:types
     */
    private Dimension calculateDimensions(List<Statement> statements) {
        int instanceIndex = 0;
        int predicateIndex = 0;

        for (Statement stmt : statements) {
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();

            // Add subject to instance map
            if (!instanceMap.containsKey(subject)) {
                instanceMap.put(subject, instanceIndex++);
            }

            // Add object if it's a blank node or resource (for reverse role)
            if (object.isAnon() && object.isResource()) {
                Resource objRes = object.asResource();
                if (!instanceMap.containsKey(objRes)) {
                    instanceMap.put(objRes, instanceIndex++);
                }
            }

            // rdf:type → Class column
            if (predicate.equals(RDF.type)) {
                String typeKey = object.toString() + "::C";
                if (!predicateMap.containsKey(typeKey)) {
                    predicateMap.put(typeKey, predicateIndex++);
                }

            } else {
                String predKey = predicate.toString();         // Normal predicate
                String revKey = predicate + "::R";             // Reverse property

                if (!predicateMap.containsKey(predKey)) {
                    predicateMap.put(predKey, predicateIndex++);
                    if (object.isResource()) {
                        predicateMap.put(revKey, predicateIndex++);
                    }
                }
            }
        }

        return new Dimension(instanceIndex, predicateIndex);
    }

    /**
     * Fills the binary matrix with 1s based on:
     * - Subject has predicate → matrix[subject][predicate] = 1
     * - Object has reverse → matrix[object][reverse_predicate] = 1
     * - Subject has rdf:type → matrix[subject][class_column] = 1
     */
    private void populateMatrix() {
        for (Map.Entry<String, Integer> entry : predicateMap.entrySet()) {
            String predKey = entry.getKey();
            int col = entry.getValue();

            if (predKey.endsWith("::R")) {
                // Reverse properties (object side)
                String base = predKey.replace("::R", "");
                Property prop = model.createProperty(base);
                NodeIterator objs = model.listObjectsOfProperty(prop);

                while (objs.hasNext()) {
                    RDFNode obj = objs.next();
                    if (obj.isResource()) {
                        Resource objRes = obj.asResource();
                        if (instanceMap.containsKey(objRes)) {
                            int row = instanceMap.get(objRes);
                            matrix[row][col] = 1;
                        }
                    }
                }

            } else if (predKey.endsWith("::C")) {
                // Class membership (rdf:type)
                String classUri = predKey.replace("::C", "");
                ResIterator subjects = model.listSubjectsWithProperty(RDF.type, model.createResource(classUri));

                while (subjects.hasNext()) {
                    Resource res = subjects.next();
                    if (instanceMap.containsKey(res)) {
                        int row = instanceMap.get(res);
                        matrix[row][col] = 1;
                    }
                }

            } else {
                // Standard predicates (subject side)
                Property prop = model.createProperty(predKey);
                ResIterator subjects = model.listSubjectsWithProperty(prop);

                while (subjects.hasNext()) {
                    Resource res = subjects.next();
                    if (instanceMap.containsKey(res)) {
                        int row = instanceMap.get(res);
                        matrix[row][col] = 1;
                    }
                }
            }
        }
    }

    // Accessor methods

    public int[][] getMatrix() {
        return matrix;
    }

    public Map<String, Integer> getPredicateMap() {
        return predicateMap;
    }

    public Map<Resource, Integer> getInstanceMap() {
        return instanceMap;
    }
}
