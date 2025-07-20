package www.etis_midi.fr.panda.mapper;

import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Utility class to load an RDF file into a Jena Model.
 */
public class RDFModelLoader {
    public static Model load(String filename) {
        return RDFDataMgr.loadModel(filename); // Auto-detects RDF format (.ttl, .rdf, .nt, etc.)
    }
}
