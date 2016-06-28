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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;



/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class InciteTextFilter extends Resource_ImplBase implements TextFilter {

    static final String PARAM_ALNUM_COLLISIONS = "alnumCollisions";
    @ConfigurationParameter( name = PARAM_ALNUM_COLLISIONS, mandatory = false )
    private Boolean alnumCollisions = true;
    
    static final String PARAM_WHITESPACE_COLLISIONS = "whitespace";
    @ConfigurationParameter( name = PARAM_WHITESPACE_COLLISIONS, mandatory = false )
    private Boolean whitespace = true;
    
    static final String PARAM_TRAILING_WHITESPACE = "trailing";
    @ConfigurationParameter( name = PARAM_TRAILING_WHITESPACE, mandatory = false )
    private Boolean trailing = true;
    
    static final String PARAM_FORMAT_PATTERNS = "formatStrings";
    @ConfigurationParameter( name = PARAM_FORMAT_PATTERNS, mandatory = false )
    private String[] formatStrings = new String[]{
//        "^\\s+$", "", // delete empty.
        "\\n", " ",     // remove new lines.
        "\\s+", " ",    // collapse whitespace
//        "\\s+$", "",  // trim tail
//        "^\\s+", "",  // trim head
//        "(\\p{Alnum})\\s+(\\p{Punct})", "$1$2", // remove space before punctuation
    };
    
    static final String RES_NGRAM_RESOLVER = "splitChker";
    @ExternalResource( key = RES_NGRAM_RESOLVER, api = SplitCheck.class, mandatory = false )
    private SplitCheck splitChker;
    
    private List<Pattern> rules;
    private Map<Pattern,String> subst;
    private boolean splitFlag = false;
    private String splitMark;

    @Override
    public boolean initialize( ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams )
        throws ResourceInitializationException {
        super.initialize( aSpecifier, aAdditionalParams );
                
        //configurationData != null && configurationData.length % 2 != 0
        if( formatStrings != null && formatStrings.length % 2 != 0 ) {
            throw new ResourceInitializationException( new IllegalArgumentException(
                "Arguments for configuration parameter '" + PARAM_FORMAT_PATTERNS + "' must come "
                + "in key/value pairs but found an odd number of arguments: [" 
                + formatStrings.length + "]."
            ) );
        } else {
            cookPatterns( formatStrings );
        }
        return true;
    }
    
    @Override
    public String normalize( String chunk ) {
        if( rules == null || subst == null ) cookPatterns( formatStrings );
        
        for( Pattern rule : rules ) {
            if( chunk.length() <= 0 ) break;
            chunk = rule.matcher( chunk ).replaceAll( subst.get( rule) );
        }
        
        return chunk;
    }

    @Override
    public void appendToBuffer( StringBuffer tgt, String chunk ) {
        chunk = normalize( chunk );
        if( chunk.length() <= 0 ) return;

        if( splitFlag && splitChker != null ) {
            String pre = getLastWord( tgt );
            String pos = "";
            String[] parts = chunk.split( "\\s" );
            if( parts.length > 0 ) pos = parts[0];
            if( !pre.isEmpty() && !pos.isEmpty() ) {
//                System.out.printf( "@@@\t%s\t%s\t%s\n", pre, pos, splitMark );
                boolean split = splitChker.split( pre, pos, splitMark );
                if( split ) tgt.append( " " );
            }
            splitFlag = false;
            splitMark = null;
        }
        
        String last = tgt.length() > 0 ? String.valueOf( tgt.charAt( tgt.length() - 1 ) ) : "";
        String inc  = chunk.length() > 0 ? String.valueOf( chunk.charAt( 0 ) ) : "";
        
        if( whitespace && inc.matches( "\\s" ) &&
            ( last.equals( "" ) || last.matches( "\\s" ) )
        ) {
            chunk = chunk.substring( 1 ); // TODO dubious.
        }
        
        tgt.append( chunk );
    }

    private void cookPatterns( String[] formatStrings ) {
        rules = new ArrayList<>();
        subst = new HashMap<>();
        for( int i = 0; i < formatStrings.length / 2; i++ ) {
            String in = formatStrings[ i * 2 ];
            String out = formatStrings[ i * 2 + 1 ];
            Pattern rule = Pattern.compile( in );
            rules.add( rule );
            subst.put( rule, out );
        }
    }

    @Override
    public void mark( String mark ) {
        splitMark = mark;
        splitFlag = true;
    }

    private String getLastWord( StringBuffer tgt ) {
        StringBuilder out = new StringBuilder();
        int at = tgt.length() - 1; char c;
        while( at >= 0 && !Character.isWhitespace( c = tgt.charAt( at-- ) ) ) {
            out.append( c );
        }
        return out.reverse().toString();
    }
}