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
package edu.columbia.incite.uima.io.resources;

import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import edu.columbia.incite.uima.api.ConfigurableResource;

/**
 * Resource implementing a mapping between string keys and UIMA annotation types and features.
 * This resource is designed to be used in order to implement a mapping between data keys in a 
 * source document and members of a type system, e.g. between XML element and attribute names and 
 * UIMA types and features.
 * 
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public interface MappingProvider extends ConfigurableResource<CAS> {

    /**
     * Return a UIMA type for the given key and attribute map.
     * 
     * @param key     A string key, typically an XML element name
     * @param data    A possibly null map with additional processing data, typically an XML 
     *                attribute map.
     * @return        A UIMA Type.
     */
    Type getType( String key, Map<String,String> data );
    
    /**
     * Return a UIMA feature for the given key within the given type.
     * 
     * @param type  A UIMA type
     * @param key   A string key, typically an XML attribute name
     * @return      A UIMA Feature
     */
    Feature getFeature( Type type, String key );

    /**
     * Return true if this key corresponds to non-annotation data.
     * 
     * @param key   A string key, typically an XML element qName
     * @return      {@code true} if this key corresponds to non-annotation data that should be processed 
     *              further.
     */
    boolean isData( String key );

    /**
     * Return true if this key corresponds to UIMA Annotation type.
     * 
     * By contract, keys that return {@code false} from this method should return {@code null} from 
     * {@link #getType(String,Map)}
     * @param key   A string key, typically an XML element qName
     * @return      {@code true} if this key maps to an annotation type.
     */
    boolean isAnnotation( String key );    

    /**
     * Return true if this key corresponds to a line-break to be inserted in the CAS document text.
     * 
     * @param key A string key, typically an XML element qName
     * @return      {@code true} if this key is indicates a line-break.
     */
    boolean isLineBreak( String key ); // TODO: get rid of this abomination.
    
    /**
     * Return true if this key corresponds to a paragraph break in the CAS document text.
     * @param key A string key, typically an element qName
     * @return      {@code true} if this key indicates a paragraph break.
     */
    boolean isParaBreak( String key );

}
