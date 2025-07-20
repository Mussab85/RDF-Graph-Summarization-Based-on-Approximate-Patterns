package www.etis_midi.fr.panda.mapper;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import www.etis_midi.fr.panda.model.Pattern;

import java.util.*;

public class SummaryGraphBuilder {
    private List<Pattern> patterns;
    private Map<Integer, String> predicateMap; // e.g. {0: "paints", 1: "R paints", 2: "rdf:type::Artist"}

    private Map<Integer, Resource> patternURIs;

    public SummaryGraphBuilder(List<Pattern> patterns, Map<Integer, String> predicateMap) {
        this.patterns = patterns;
        this.predicateMap = predicateMap;
        this.patternURIs = new HashMap<>();
    }

    public Model buildSummaryGraph() {
        Model model = ModelFactory.createDefaultModel();
        Property extentProp = model.createProperty("http://example.org/vocab#extent");

        // Generate nodes for patterns
        for (int i = 0; i < patterns.size(); i++) {
            Resource node = model.createResource("http://example.org/pattern/" + i);
            node.addLiteral(extentProp, patterns.get(i).getTransactions().size());
            patternURIs.put(i, node);
        }

        // Process items in each pattern
        for (int i = 0; i < patterns.size(); i++) {
            Resource patternNode = patternURIs.get(i);
            Pattern pattern = patterns.get(i);

            for (int item : pattern.getItems()) {
                String predicateStr = predicateMap.get(item);
                if (predicateStr == null) continue;

                if (predicateStr.endsWith("::R")) {
                    String directPred = predicateStr.replace("::R", "");
                    int targetIndex = findTargetPattern(item); // Heuristic
                    if (targetIndex >= 0) {
                        Property p = model.createProperty(directPred);
                        patternNode.addProperty(p, patternURIs.get(targetIndex));
                    }
                } else if (predicateStr.contains("::C")) {
                    // Type
                    String typeURI = predicateStr.replace("::C", "");
                    patternNode.addProperty(RDF.type, model.createResource(typeURI));
                } else {
                    // Attribute or direct property → literal node
                    Property p = model.createProperty(predicateStr);
                    patternNode.addProperty(p, model.createResource("http://example.org/literal/" + Math.abs(predicateStr.hashCode())));
                }
            }
        }

        return model;
    }

    private int findTargetPattern(int reverseItem) {
        // Simple match: look for a pattern containing the matching non-R item
        String reversePredicate = predicateMap.get(reverseItem);
        if (reversePredicate == null || !reversePredicate.endsWith("::R")) return -1;

        String normalPredicate = reversePredicate.replace("::R", "");

        for (Map.Entry<Integer, String> entry : predicateMap.entrySet()) {
            if (entry.getValue().equals(normalPredicate)) {
                int directItem = entry.getKey();
                for (int i = 0; i < patterns.size(); i++) {
                    if (patterns.get(i).getItems().contains(directItem)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }
}
