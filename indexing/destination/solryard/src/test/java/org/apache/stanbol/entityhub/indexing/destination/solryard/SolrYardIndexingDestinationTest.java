package org.apache.stanbol.entityhub.indexing.destination.solryard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.naming.NameParser;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * What to test:
 *  - correct initialisation
 *    - special schema initialisation
 *    - default schema initialisation
 *  - finalisation
 *    - writing of the IndexFieldConfiguration
 *    - creating of the {name}.solrindex.zip
 *    - creating of the {name}.solrindex.ref
 *    
 * Indexing needs not to be tested, because this is the responsibility of the
 * Unit Tests for the used Yard implementation.
 * 
 * @author Rupert Westenthaler
 *
 */
public class SolrYardIndexingDestinationTest {

    private static final Logger log = LoggerFactory.getLogger(SolrYardIndexingDestinationTest.class);
    private static final String CONFIG_ROOT = 
        FilenameUtils.separatorsToSystem("testConfigs/");
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes.
     * This folder is than used as classpath.<p>
     * "/target/test-files/" does not exist, but is created by the
     * {@link IndexingConfig}.
     */
    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("/target/test-files");
    private static String  userDir;
    private static String testRoot;
    /**
     * The methods resets the "user.dir" system property
     */
    @BeforeClass
    public static void initTestRootFolder(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        //store the current user.dir
        userDir = System.getProperty("user.dir");
        testRoot = baseDir+TEST_ROOT;
        log.info("ConfigTest Root : "+testRoot);
        //set the user.dir to the testRoot (needed to test loading of missing
        //configurations via classpath
        //store the current user.dir and reset it after the tests
        System.setProperty("user.dir", testRoot);
    }
    /**
     * resets the "user.dir" system property the the original value
     */
    @AfterClass
    public static void cleanup(){
        System.setProperty("user.dir", userDir);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testMissingBoostConfig(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"missingBoostConfig");
        config.getIndexingDestination();
    }
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidBoostConfig(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"invalidBoostConfig");
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required, but the name of the config
     * file is the default. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingDefaultSolrSchemaConfig(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"missingDefaultSolrConf");
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required and the name of the config
     * file is specified. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingSolrSchemaConfig(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"missingSolrConf");
        config.getIndexingDestination();
    }
    @Test
    public void testSimple() throws YardException, IOException {
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"simple");
        validateSolrDestination(config);
    }
    @Test
    public void testWithSolrConf() throws YardException, IOException {
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"withSolrConf");
        validateSolrDestination(config);
    }
    
    /**
     * Checks if the SolrYardIndexingDestination returned by the 
     * {@link IndexingConfig} is valid and functional
     * @param config the configuration
     * @throws YardException indicates problems while working with the {@link SolrYard}
     * returned by {@link IndexingDestination#getYard()}
     * @throws IOException indicates problems while validating the SolrArchives
     * created by the {@link IndexingDestination#finalise()} method
     */
    private void validateSolrDestination(IndexingConfig config) throws YardException,
                                                               IOException {
        //get the destination
        IndexingDestination destination = config.getIndexingDestination();
        assertNotNull(destination);
        assertEquals(destination.getClass(), SolrYardIndexingDestination.class);
        //initialise
        assertTrue(destination.needsInitialisation());
        destination.initialise();
        //test that the returned Yard instance is functional
        Yard yard = destination.getYard();
        assertNotNull(yard);
        assertEquals(yard.getClass(), SolrYard.class);
        Representation rep = yard.getValueFactory().createRepresentation("http://www.example.com/entity#123");
        rep.add(NamespaceEnum.rdfs+"label", "test");
        rep.add(NamespaceEnum.rdfs+"description", "Representation to test storage while indexing");
        rep.add(RdfResourceEnum.entityRank.getUri(), Float.valueOf(0.8f));
        yard.store(rep);
        //finalise
        destination.finalise();
        //test the archives
        File expectedSolrArchiveFile = 
            new File(config.getDistributionFolder(),config.getName()+".solrindex.zip");
        assertTrue(expectedSolrArchiveFile.isFile());
        //TODO: validate the archive
        
        //check for the solrArchive reference file and validate required properties
        File expectedSolrArchiveReferenceFile = 
            new File(config.getDistributionFolder(),config.getName()+".solrindex.ref");
        assertTrue(expectedSolrArchiveReferenceFile.isFile());
        Properties solrRefProperties = new Properties();
        solrRefProperties.load(new FileInputStream(expectedSolrArchiveReferenceFile));
        assertTrue(solrRefProperties.getProperty("Index-Archive").equals(expectedSolrArchiveFile.getName()));
        assertTrue(solrRefProperties.getProperty("Name") != null);
    }
    
}
