/*
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
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
package edu.columbia.incite.uima.api.util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.api.types.Document;
import edu.columbia.incite.uima.api.types.Entity;
import edu.columbia.incite.uima.api.types.Segment;
import edu.columbia.incite.uima.api.types.Span;

/**
 *
 * @author Jose Tomas Atria <jtatria@gmail.com>
 */
public abstract class Types {
    /** Type name for document meta-data annotations. **/
    public static final String DOCUMENT_TYPE = Document.class.getName();
    /** Type name for the root type in this type system. **/
    public static final String BASE_TYPE = Span.class.getName();
    /** Type name for the segment root type. **/
    public static final String SEGMENT_TYPE = Segment.class.getName();
    /** Type name for the entity root type. **/
    public static final String ENTITY_TYPE = Entity.class.getName();
    /** Feature name for document id's. **/
    public static final String DOC_ID_FEATURE = "id";
    /** Feature name for document URI's. **/
    public static final String DOC_URI_FEATURE = "uri";
    
    private Types(){
        // No instances.
    };
    
    /**
     * Get a valid ID for the document contained in the given JCas.
     * @param jcas  A JCas.
     * @return  An id for the document contained in the JCas.
     */
    public static String getCASId( JCas jcas ) {
        return jcas.getAnnotationIndex( Document.class ).iterator().next().getId();
    }
    
    /** Get a valid ID for the document contained in the given CAS.
     * @param cas   A CAS.
     * @return  An id for the document contained in the CAS.
     */
    public static String getCASId( CAS cas ) {
        Type docType = cas.getTypeSystem().getType( DOCUMENT_TYPE );
        Feature idFeature = docType.getFeatureByBaseName( DOC_ID_FEATURE );
        
        return cas.getAnnotationIndex( docType ).iterator().next().getFeatureValueAsString( idFeature );
    }
    
    /**
     * Get a valid URI for the document contained in the given JCas.
     * @param jcas  A JCas.
     * @return  An URI for the document contained in the JCas.
     */
    public static String getCASUri( JCas jcas ) {
        return jcas.getAnnotationIndex( Document.class ).iterator().next().getUri();
    }

    /** Get a valid URI for the document contained in the given CAS.
     * @param cas   A CAS.
     * @return  An URI for the document contained in the CAS.
     */
    public static String getCASUri( CAS cas ) {
        Type docType = cas.getTypeSystem().getType( DOCUMENT_TYPE );
        Feature uriFeature = docType.getFeatureByBaseName( DOC_URI_FEATURE );
        
        return cas.getAnnotationIndex( docType ).iterator().next().getFeatureValueAsString( uriFeature );
    }
}
