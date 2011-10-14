package org.apache.stanbol.explanation;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.explanation.impl.TestSchemaMatchers;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;

public class MockOsgiContext {

    public static Parser parser;

    public static TcManager tcManager;

    public static ONManager onManager;

    static {
        reset();
    }

    public static void reset() {

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        // We need this to make sure the local meta.owl (which does not import codolight) is loaded.
        configuration.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/ontologies", "/schemas",
                                                                             "/schemas/registry"});
        configuration.put(
            RegistryManager.REGISTRY_LOCATIONS,
            new String[] {
                          TestSchemaMatchers.class.getResource("/schemas/registry/explanation.owl")
                                  .toString(),
                          TestSchemaMatchers.class.getResource("/schemas/registry/explanation-mappings.owl")
                                  .toString()});
        OfflineConfiguration offline = new OfflineConfigurationImpl(configuration);

        tcManager = TcManager.getInstance();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        tcManager.addWeightedTcProvider(wtcp);
        onManager = new ONManagerImpl(tcManager, wtcp, offline, configuration);
        parser = Parser.getInstance();
        parser.bindParsingProvider(new JenaParserProvider());
    }

}