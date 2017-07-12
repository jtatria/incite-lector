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
package edu.columbia.incite.uima.ae.corpus;

import edu.columbia.incite.uima.api.index.FieldFactory;
import edu.columbia.incite.uima.ae.SegmentedEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;

import edu.columbia.incite.corpus.LemmaSet;
import edu.columbia.incite.corpus.POSClass;
import edu.columbia.incite.uima.api.types.Tokens;
import edu.columbia.incite.corpus.TermNormal;
import edu.columbia.incite.uima.api.casio.FeatureBroker;
import edu.columbia.incite.uima.res.casio.InciteLuceneFB;
import edu.columbia.incite.uima.res.index.InciteFieldFactory;
import edu.columbia.incite.uima.res.index.IndexWriterProvider;

import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class CorpusIndexer extends SegmentedEngine {
    
    public static final String PARAM_DRY_RUN = "dryRun";
    @ConfigurationParameter( name = PARAM_DRY_RUN, mandatory = false, defaultValue = "false" )
    protected Boolean dryRun;
    
    public static final String PARAM_ADD_DOC_FIELDS = "addDocFields";
    @ConfigurationParameter( name = PARAM_ADD_DOC_FIELDS, mandatory = false, defaultValue = "true" )
    protected Boolean addDocFields;
    
    public static final String PARAM_ADD_TOKENSTREAMS = "addTokens";
    @ConfigurationParameter( name = PARAM_ADD_TOKENSTREAMS, mandatory = false, defaultValue = "true" )
    protected Boolean addTokens;
    
    public static final String RES_INDEX_WRITER = "indexWriter";
    @ExternalResource( key = RES_INDEX_WRITER, mandatory = false )
    private IndexWriterProvider indexWriter;
    
    public static final String RES_FIELD_NORMALS = "fieldNormals";
    @ExternalResource( key = RES_FIELD_NORMALS, mandatory = false )
    protected FieldNormals fieldNormals;
    
    public static final String RES_FIELD_TYPES = "fieldTypes";
    @ExternalResource( key = RES_FIELD_TYPES, mandatory = false )
    protected FieldTypes fieldTypes;
    
    public static final String RES_LUCENEFB = "fieldBroker";
    @ExternalResource( key = RES_LUCENEFB, mandatory = false )
    private FeatureBroker<Document> fieldBroker;
    
    private final Map<IndexableField,UIMATokenStream> tokenFields = new HashMap<>();
    private Document docInstance;
    private Long wrtrSssn;
        
    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
        
        if( fieldBroker == null ) fieldBroker = new InciteLuceneFB();
        
        if( !fieldNormals.normals.keySet().equals( fieldTypes.types.keySet() ) ) {
            throw new ResourceInitializationException( new IllegalArgumentException(
                "Inconsistent specifications in indexed field maps"
            ) );
        }
        
        this.docInstance = new Document();
        
        for( String key : fieldNormals.normals.keySet() ) {
            TermNormal tn = fieldNormals.normals.get( key );
            FieldType ft = fieldTypes.types.get( key );
            UIMATokenStream uts = new UIMATokenStream( tn );
            IndexableField field = new Field( key, uts, ft );
            tokenFields.put( field, uts );
        }
                
        getLogger().log( Level.CONFIG, fieldTypes.toString() );
        getLogger().log( Level.CONFIG, fieldNormals.toString() );
                
        if( !dryRun ) {
            if( indexWriter == null ) {
                throw new ResourceInitializationException( 
                    ResourceInitializationException.NO_RESOURCE_FOR_PARAMETERS,
                    new Object[]{ CorpusIndexer.RES_INDEX_WRITER }
                );  
            }
            this.wrtrSssn = indexWriter.openSession();
        }
    }
    
    @Override
    protected void processSegment( 
        AnnotationFS seg, List<AnnotationFS> covers, List<AnnotationFS> tokens 
    ) throws AnalysisEngineProcessException {
        try {
            // clear old values
            docInstance.getFields().clear();
            
            // update segment metadata
            if( addDocFields ) {
                fieldBroker.values( seg, docInstance );
                for( AnnotationFS ann : covers ) {
                    fieldBroker.values( ann, docInstance );
                }
            }
            
            // update tokenstreams
            if( addTokens ) {
                tokenFields.keySet().forEach( ( f ) -> {
                    tokenFields.get( f ).setInput( tokens, seg.getBegin() );
                    docInstance.add( f );
                } );
            }
            int i = 0;
            
            // write to index
            if( !dryRun ) {
                this.indexWriter.index( docInstance );
            }
            
        } catch ( IOException | CASException ex ) {
            throw new AnalysisEngineProcessException( ex );
        }
    }
    
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        if( !dryRun ) {
            indexWriter.closeSession( wrtrSssn );
        }
    }

    protected FieldFactory iniDocFieldFactory() {
        return new InciteFieldFactory();
    }
    
    protected static class UIMATokenStream extends TokenStream {
        // Input data
        private Collection<AnnotationFS> src;
        private Integer offset;

        // State data
        private Iterator<AnnotationFS> annIt;
        private AnnotationFS cur;
        private Integer last = 0;

        private final TermNormal termNormal;
        
        private final OffsetAttribute   osAttr;
        private final CharTermAttribute ctAttr;
        private final PayloadAttribute  plAttr;
        private final TypeAttribute     tyAttr;
        
        public UIMATokenStream( TermNormal.Conf conf ) {
            this( new TermNormal( conf ) );
        }
        
        public UIMATokenStream( TermNormal tn ) {
            this.termNormal = tn;
            this.osAttr = addAttribute( OffsetAttribute.class );
            this.ctAttr = addAttribute( CharTermAttribute.class );
            this.plAttr = addAttribute( PayloadAttribute.class );
            this.tyAttr = addAttribute( TypeAttribute.class );
        }

        public UIMATokenStream setInput( Collection<AnnotationFS> tokens, int offset ) {
            this.src = tokens;
            this.offset = offset;
            return this;
        }

        @Override
        public void close() throws IOException {
            super.close();
            // Clear input.
            this.src = null;
            this.offset = null;
        }

        @Override
        public void end() throws IOException {
            super.end();
            addAttribute( OffsetAttribute.class ).setOffset( last, last );
            // Clear state.
            this.annIt = null;
            this.cur = null;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            if( src == null ) {
                throw new IllegalStateException( "No input for token stream!" );
            }
            clearAttributes();
            annIt = src.iterator();
            last = 0;
        }

        @Override
        public boolean incrementToken() throws IOException {
            clearAttributes();
            
            String term = "";
            while( annIt.hasNext() && term.equals( "" ) ) {
                cur = annIt.next();
                term = termNormal.term( cur );
                if( !term.equals( "" ) ) {
                    
                     // TODO This is naive and needs refactoring
                    last = last > cur.getEnd() ? last : cur.getEnd();
                    if( last > cur.getEnd() ) {
                        addAttribute( PositionIncrementAttribute.class ).setPositionIncrement( 0 );
                    }

                    osAttr.setOffset( cur.getBegin() - offset, cur.getEnd() - offset );
                    ctAttr.append( term );
                    // TODO: reuse bytesref
                    plAttr.setPayload( new BytesRef( termNormal.data( cur ) ) );
                    tyAttr.setType( termNormal.type( cur ) );
                    
                    return true;
                }
            } return false;
        }
    }
    
    public static class FieldNormals extends Resource_ImplBase {

        final Map<String,TermNormal> normals = new HashMap<>();
        final Map<String,TermNormal.Conf> confs = new HashMap<>();
        
        @Override
        public boolean initialize( ResourceSpecifier spec, Map<String, Object> params )
        throws ResourceInitializationException {
            boolean ret = super.initialize( spec, params );
            getLogger().log( Level.CONFIG, this.toString() );
            return ret;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            List<String> keys = new ArrayList<>( normals.keySet() );
            Collections.sort( keys );
            for( String key : keys ) {
                sb.append( String.format( "\n========== %s ==========\n", key ) );
                sb.append( normals.get( key ).toString() );
            }
            sb.append( "\n" );
            return sb.toString();
        }
    
        public void add( String field, TermNormal.Conf c, boolean save ) {
            if( save ) confs.put( field, c.clone() );
            this.normals.put( field, new TermNormal( c.commit() ) );
        }
        
        public void add( 
            String field, boolean incPunc, boolean excNlex, boolean lemmatize, boolean addPos, 
            LemmaSet[] replace, LemmaSet[] delete 
        ) {
            TermNormal.Conf c = new TermNormal.Conf();

            List<POSClass> pos = new ArrayList<>();
        
            if( incPunc ) pos.add(POSClass.PUNC );
            pos.addAll( Arrays.asList( excNlex ? Tokens.LEX_CLASSES : Tokens.ALL_CLASSES ) );        
            c.setLexClasses(pos.toArray(new POSClass[pos.size()] ) );

            Tokens.LexAction action;
            if( addPos ) action = Tokens.LexAction.ADD_POS_TAG;
            else if( lemmatize ) action = Tokens.LexAction.LEMMATIZE;
            else action = Tokens.LexAction.KEEP_AS_IS;
            c.setLexicalAction( action );

            c.setNonLexicalAction( Tokens.NonLexAction.DELETE );

            if( delete != null && delete.length > 0 ) {
                c.setLemmaDeletions( delete );
            }

            if( replace != null && replace.length > 0 ) {
                c.setLemmaSubstitutions( replace );
            }
            
            this.add( field, c, true );
        }
        
        public TermNormal.Conf getConf( String field ) {
            return confs.get( field );
        }
        
    }
    
    public static class FieldTypes extends Resource_ImplBase {
        
        final Map<String,FieldType> types = new HashMap<>();
     
        @Override
        public boolean initialize( ResourceSpecifier spec, Map<String, Object> params )
        throws ResourceInitializationException {
            boolean ret = super.initialize( spec, params );
            getLogger().log( Level.CONFIG, this.toString() );
            return ret;
        }
        
        public void add( 
            String field, IndexOptions opts, boolean tvs, boolean tvOffsets, 
            boolean tvPayloads, boolean tvPositions 
        ) {
            FieldType ft = new FieldType();
            ft.setIndexOptions( opts );
            ft.setStoreTermVectors( tvs);
            ft.setStoreTermVectorOffsets( tvOffsets );
            ft.setStoreTermVectorPayloads( tvPayloads );
            ft.setStoreTermVectorPositions( tvPositions );
            ft.setTokenized( true );
            
            this.types.put( field, ft );
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            
            List<String> keys = new ArrayList<>( types.keySet() );
            Collections.sort( keys );
            
            for( String key : keys ) {
                sb.append( String.format( "\n========== %s ==========\n", key ) );
                sb.append( types.get( key ).toString() );
            }
            sb.append( "\n" );
            return sb.toString();
        }
    }
    
}
