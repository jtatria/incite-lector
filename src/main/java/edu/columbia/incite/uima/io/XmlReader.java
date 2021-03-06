/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.uima.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXWriter;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.columbia.incite.uima.api.types.Document;


/**
 * Collection reader that creates CASes from XML elements. This component takes an input directory
 * containing XML files and an (optional) XPath expression that produces a sequence of XML elements
 * corresponding to CAS documents and writes each of these elements to a SAX handler in order to
 * populate new CASes.
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class XmlReader extends AbstractFileReader {

    /**
     * Collection name. This value will be written to each document's metadata annotation if the
     * type system supports it.
     */
    public final static String PARAM_COLLECTION_NAME = "collection";
    @ConfigurationParameter( name = PARAM_COLLECTION_NAME, mandatory = false, defaultValue = ""
        , description = "Collection name." )
    protected String collectionName;
    
    /**
     * Optional XPath expression for XML nodes that will be CASed. Each document's root node will 
     * be used if none is given.
     */
    public final static String PARAM_CAS_XPATH = "XPath";
    @ConfigurationParameter( name = PARAM_CAS_XPATH, mandatory = false, defaultValue = ""
        , description = "Optional XPath expression for XML nodes that will be CASed." )
    private String xpathExpr;

    /**
     * Resource implementing a {@link SaxHandler} that will process XML data and populate new CASes.
     */
    public final static String RES_SAX_HANDLER = "saxHandler";
    @ExternalResource( key = RES_SAX_HANDLER, api = SaxHandler.class, mandatory = true
        , description = "SAX content handler." )
    private SaxHandler saxHandler;

    private ListIterator<Element> elements;
    private Element curElt;
    private XPath xpath;
    private int cur;

    private Map<String,Integer> docIds;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );

        if( saxHandler == null ) {
            throw new ResourceInitializationException(
                ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                new Object[] { RES_SAX_HANDLER }
            );
        }

        // XPath expression is valid...
        try {
            xpath = xpathExpr.equals( "" ) ? new Dom4jXPath( "/*" ) : new Dom4jXPath( xpathExpr );
        } catch( JaxenException ex ) {
            throw new ResourceInitializationException(
                ResourceConfigurationException.RESOURCE_DATA_NOT_VALID,
                new Object[] { xpathExpr, PARAM_CAS_XPATH }
            );
        }

        // Initialize element iterator. This is weird, but it works.
        elements = ( new ArrayList<Element>( 0 ) ).listIterator();

        // Initialize docIds map.
        docIds = new HashMap<>();

        // Announce ourselves to the world.
        if( !xpathExpr.equals( "" ) ) {
            getLogger().log( Level.INFO,
                "Xpath XML reader: collecting cases from sequence {0}", xpathExpr
            );
        }

        // Start pulling elements from files to start processing.
        getNextElement();
    }

    /**
     * Get the next XML element in the sequence produced by the configured XPath expression.
     * This will correspond to the root element of each XML file in the input director if no XPath
     * expression has been configured.
     *
     * @param jcas JCas that will be populated with data from the next XML element.
     *
     * @throws org.apache.uima.collection.CollectionException If there is any error in XML
     *                                                        processing.
     */
    @Override
    public void getNext( JCas jcas ) throws CollectionException {
        cur++;

        // Set CAS metadata and language
        jcas.setDocumentLanguage( "en" );

        // Build document id.
        String docId = FilenameUtils.removeExtension( curPath.getFileName().toString() );
        docId += xpathExpr.equals( "" ) ? ""
            : xpathExpr.replaceAll( "/+", "-" ) + "-" + Integer.toString( cur );
        if( docIds.containsKey( docId ) ) {
            int i = docIds.get( docId );
            docIds.put( docId, i++ );
        } else {
            docIds.put( docId, 0 );
        }

        Document doc = new Document( jcas );
        doc.setId( docId );
        doc.setUri( curPath.toUri().toString() );
        doc.setCollection( collectionName );
        doc.setIndex( cur );
        doc.setXpath( xpathExpr );
        doc.setProc_isLast( elements == null || !elements.hasNext() );
        jcas.addFsToIndexes( doc );

        // Populate CAS data.
        try {
            // TODO: refactor this to get rid of the dependency on the ancient Dom4J. 
            // See http://www.java2s.com/Code/Java/XML/ProduceaSAXstreamfromaDOMDocument.htm for an 
            // example using (completely moronic and cumbersome) standard libraries
            
            // Pass CAS to handler. CAS must be released when done.
            saxHandler.configure( jcas.getCas() );

            // Get a SAX writer.
            SAXWriter writer = new SAXWriter( saxHandler );

            // Start handler processing.
            // TODO: review SAX spec to determine if this is appropriate when dealing with non-root nodes.
            saxHandler.startDocument();
            // Write data to handler.
            writer.write( curElt );
            // Finish handler processing.
            // TODO: review SAX spec to determine if this is appropriate when dealing with non-root nodes.
            saxHandler.endDocument();

        } catch( SAXException | ResourceConfigurationException ex ) {
            throw new CollectionException( ex );
        } finally {
            // CAS must be released on reset.
            saxHandler.reset();
        }

        getNextElement();
    }

    /**
     * Returns true if there are remaining elements in the collection.
     *
     * @return {@code true} if there are XML files left in the input directory stream, or if the
     *         current file has elements left.
     */
    @Override
    public boolean hasNext() {
        return ( elements != null || curElt != null );
    }

    private void getNextElement() {
        if( elements == null ) {    // if elements iterator is null, we are done;
            curElt = null;          // set current element to null and
            return;                 // bail.
        }

        // No more elements, but more files.
        while( !elements.hasNext() && pathsIt.hasNext() ) {
            cur = 0; // Reset element counter.
            curPath = pathsIt.next(); // Get next file.

            try( InputStream is = Files.newInputStream( curPath ) ) {
                // This part is fugly. XML parsing in java sucks. I miss XML::Twig.
                // Eventually, this should all be moved to a resource handler,
                // ideally using BaseX (or another xquery processor) as I/O layer.
                InputSource src = new InputSource( is );        // Get input stream

                org.dom4j.Document doc = ( new SAXReader() ).read( src ); // Read it...
                List<Element> elts = xpath.selectNodes( doc );  // Get relevant nodes.

                // Announce file name, number of nodes found, expression used, files left.
                String message = String.format( "Reading file %s.", curPath.getFileName() );
                if( !xpathExpr.equals( "" ) ) {
                    message += String.format( " Found %d nodes with expression %s.",
                        new Object[] { elts.size(), xpathExpr }
                    );
                }
                message += String.format( " %d files left.", totalFiles - readFiles );
                getLogger().log( Level.FINE, message );

                // Copy new elements iterator over old iterator.
                elements = elts.listIterator();

            } catch( DocumentException | JaxenException | IOException ex ) {
                getLogger().log( Level.SEVERE, String.format( "Error processing %s: %s", curPath.toString(), ex.toString() ) );
            }
        }

        // Get next element or null if none.
        curElt = elements.hasNext() ? elements.next() : null;

        // Nullify elements iterator if there are no more elements or files.
        if( !elements.hasNext() && !pathsIt.hasNext() ) {
            elements = null;
        }
    }

    @Override
    protected void addDataFromFile( JCas jcas, FileInputStream fileInputStream ) throws CollectionException {
    }

}
