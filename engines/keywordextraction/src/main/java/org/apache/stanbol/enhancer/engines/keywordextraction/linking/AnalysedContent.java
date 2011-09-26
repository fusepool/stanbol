/**
 * 
 */
package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

import java.util.Iterator;

import opennlp.tools.util.Span;

import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;

/**
 * Represents the already with NLP tools analysed content to be linked with
 * Entities of an {@link EntitySearcher}.<p>
 * Note that for the linking process it is only required that the text is
 * tokenized. All other features (sentence detection, POS tags and Chunks) are
 * optional but do improve the performance and to an smaller amount also the
 * results of the linking process. <p>
 * TODO: <ul>
 * <li> Find a better Name
 * <li> The API is not optimal. In general the {@link TextAnalyzer} and the
 * {@link AnalysedContent} interface do not play well together :(
 * </ul>
 * @author Rupert Westenthaler
 *
 */
public interface AnalysedContent {

    
    /**
     * Getter for the Iterator over the analysed sentences. This Method
     * is expected to return always the same Iterator instance.
     * @return the iterator over the analysed sentences
     */
    public Iterator<AnalysedText> getAnalysedText();
    /**
     * Called to check if a {@link Token} should be used to search for
     * Concepts within the Taxonomy based on the POS tag of the Token.
     * @param posTag the POS tag to check
     * @return <code>true</code> if Tokens with this POS tag should be
     * included in searches. Otherwise <code>false</code>.  If this information 
     * is not available (e.g. no set of Tags that need to be processed is defined) 
     * this Method MUST return <code>null</code>
     */
    public Boolean processPOS(String posTag);
    /**
     * Called to check if a chunk should be used to search for Concepts.
     * @param chunkTag the tag (type) of the chunk
     * @return <code>true</code> if chunks with this tag (type) should be
     * processed (used to search for matches of concepts) and <code>false</code>
     * if not. If this information is not available (e.g. no set of Tags that
     * need to be processed is defined) this Method MUST return <code>null</code>
     */
    public Boolean processChunk(String chunkTag);
    /**
     * Tokenizes the parsed label
     * @param label the label to tokenize
     * @return the spans of the tokens
     */
    public String[] tokenize(String label);
}