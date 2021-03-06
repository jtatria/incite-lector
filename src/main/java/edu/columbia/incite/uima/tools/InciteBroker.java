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
package edu.columbia.incite.uima.tools;

import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Paragraph;
import edu.columbia.incite.uima.api.types.Segment;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.uima.api.types.Tuple;

import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceConfigurationException;

import edu.columbia.incite.uima.index.Tokenizer;

/**
 * Generic feature broker for Incite's default type system.
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 * @param <D>
 */
public abstract class InciteBroker<D> extends Resource_ImplBase implements FeatureBroker<D> {
    
    public static final String SEP = Tokenizer.SEP;
    
    public static final String       DOC_PREFIX = "doc";
    public static final String       SEG_PREFIX = "seg";
    public static final String      PARA_PREFIX = "para";
    public static final String       ENT_PREFIX = "ent";
    public static final String      PROC_PREFIX = "proc";
    public static final String      ATTR_PREFIX = "attr";
            
    
    public static final String DOCID_FIELD_NAME = DOC_PREFIX  + SEP + "id";
    public static final String SEGID_FIELD_NAME = SEG_PREFIX  + SEP + "id";
    public static final String PARID_FIELD_NAME = PARA_PREFIX + SEP + "id";
    public static final String ENTID_FIELD_NAME = ENT_PREFIX  + SEP + "id";
    
    public static final String  SEGL_FIELD_NAME = SEG_PREFIX  + SEP + "level";
    
    public static final String   COL_FIELD_NAME = DOC_PREFIX  + SEP + "Collection";
    public static final String   URI_FIELD_NAME = DOC_PREFIX  + SEP + "URI";
    public static final String   XPT_FIELD_NAME = DOC_PREFIX  + SEP + "XPath";
    public static final String   IND_FIELD_NAME = DOC_PREFIX  + SEP + "Index";
    public static final String  LAST_FIELD_NAME = PROC_PREFIX + SEP + "IsLast";
    public static final String  SKIP_FIELD_NAME = PROC_PREFIX + SEP + "Skip";

    public static final String       SUB_SUFFIX = "_subject";
    public static final String       OBJ_SUFFIX = "_object";

    public static final String PARAM_ADD_PROC = "addProc";
    @ConfigurationParameter( name = PARAM_ADD_PROC, mandatory = false )
    private boolean addProc;

    private final Supplier<D> dataSupplier = supplier();
        
    @Override
    public D values( AnnotationFS ann ) throws CASException {
        D d = dataSupplier.get();
        values( ann, d );
        return d;
    }

    @Override
    public void values( AnnotationFS ann, D tgt ) throws CASException {
        if( ann instanceof Span ) {
            Span span = (Span) ann;
            
            if( span.getAttributes() != null ) {
                for( int i = 0; i < span.getAttributes().size(); i++ ) {
                    String attrK = span.getAttributes( i ).getK();
                    String attrV = span.getAttributes( i ).getV();
                    addData( ATTR_PREFIX + SEP + attrK, attrV, Types.STRING, tgt );
                }
            }

            if( span.getTuples() != null ) {
                for( int i = 0; i < span.getTuples().size(); i++ ) {
                    Tuple tuple = span.getTuples( i );

                    // TODO: this needs to be tested
                    boolean isSubject;
                    if( tuple.getSubject().equals( span ) ) isSubject = true;
                    else if( tuple.getObject().equals( span ) ) isSubject = false;
                    else continue;

                    String pred = tuple.getPredicate();
                    Span targ = isSubject ? tuple.getObject() : tuple.getSubject();
                    String value = targ.getType().getShortName()
                        + TypeSystem.FEATURE_SEPARATOR + targ.getId();
                    String predKey = isSubject ? pred + SUB_SUFFIX : pred + OBJ_SUFFIX;
                    addData( predKey, value, Types.STRING, tgt );
                }
            }
        }
        
        if( ann instanceof Document ) {
            Document doc = (Document) ann;
            addData( DOCID_FIELD_NAME, doc.getId(),         Types.STRING,  tgt );
            addData( COL_FIELD_NAME,   doc.getCollection(), Types.STRING,  tgt );
            addData( URI_FIELD_NAME,   doc.getUri(),        Types.STRING,  tgt );
            addData( XPT_FIELD_NAME,   doc.getXpath(),      Types.STRING,  tgt );
            addData( IND_FIELD_NAME,   doc.getIndex(),      Types.INTEGER, tgt );
            if( addProc ) {
                addData( LAST_FIELD_NAME, doc.getProc_isLast(), Types.BOOLEAN, tgt );
                addData( SKIP_FIELD_NAME, doc.getProc_skip(), Types.BOOLEAN, tgt );
            }
        }
        

        if( ann instanceof Segment ) {
            Segment segment = (Segment) ann;
            if( ann instanceof Paragraph ) {
                addData( PARID_FIELD_NAME, segment.getId(),   Types.STRING, tgt );
            } else {
                addData( SEGID_FIELD_NAME, segment.getId(),   Types.STRING, tgt );
            }
            addData( SEGL_FIELD_NAME, segment.getLevel(), Types.INTEGER, tgt );
        }
    }

    @Override
    public void configure( CAS conf ) throws ResourceConfigurationException {
        // TODO Should we check if CAS supports Incite's TypeSystem?
    }

    protected abstract void addData( String name, Object v, Types dType, D tgt );
    
    protected abstract Supplier<D> supplier();
    
    public enum Types {
        STRING,
        INTEGER,
        REAL,
        BOOLEAN
    }
}
