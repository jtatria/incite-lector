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
package edu.columbia.incite.uima.api.types;

import edu.columbia.incite.corpus.POSClass;
import java.util.function.Function;

import org.apache.uima.cas.text.AnnotationFS;

import static edu.columbia.incite.corpus.POSClass.*;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class Tokens {
    /** Token part separator: _ **/
    public static final String SEP = "_";
    /** Generic non-lexical mark: %%% **/
    public static final String NL_MRKR = "%%%";
    
    /** POS group index in canonical array **/
    public static final int PGI  = 0;
    /** POS tag index in canonical array **/
    public static final int PTI  = 1;
    /** Lemma index in canonical array **/
    public static final int LMI  = 2;
    /** Raw text index in canonical array **/
    public static final int RAW  = 3;
    
    public static final POSClass[] ALL_CLASSES = new POSClass[]{
        ADJ,
        ADV,
        ART,
        CARD,
        CONJ,
        NN,
        NP,
        O,
        PP,
        PR,
//            PUNC,
        V
    };
    
    public static final POSClass[] LEX_CLASSES = new POSClass[]{
        ADJ,
        ADV,
//        ART,
//        CARD,
//        CONJ,
        NN,
        NP,
//        O,
//        PP,
//        PR,
//        PUNC,
        V 
    };
    
    /**
     * Produce canonical String array for the given token.
     * This method omits raw text.
     * @param token A {@link Token}.
     * @return A String[] containing the parts of the canonical form of the given token.
     */
    public static String[] parse( AnnotationFS token ) {
        return Tokens_DKPro.parse( token );
    }
    
    public static boolean isToken( AnnotationFS token ) {
        return Tokens_DKPro.isToken( token );
    }
    
    /**
     * Get POS group and tag for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */
    public static String pos( AnnotationFS token ) {
        String[] parse = parse( token );
        return String.join( SEP, parse[PGI], parse[PTI] );
    }
    
    /**
     * Get POS group for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be used to obtain a {@link POSClass} instance with {@link POSClass#valueOf(java.lang.String) }.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */
    public static String posG( AnnotationFS token ) {
        return parse( token )[PGI];
    }
    
    /**
     * Get POS tag for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}.
     * @return The String representation of the POS tag for the given token.
     */    
    public static String posT( AnnotationFS token ) {
        return parse( token )[PTI];
    }

    /**
     * Get Lemma for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be tested agains the tests defined in a {@link LemmaSet}.
     * @param token A {@link Token}.
     * @return The String representation of the Lemma for the given token.
     */        
    public static String lemma( AnnotationFS token ) {
        return parse( token )[LMI];
    }
    
    /**
     * Get raw text for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * The returned String can be tested agains the tests defined in a {@link LemmaSet}.
     * @param token A {@link Token}.
     * @return The String representation of the POS group for the given token.
     */        
    public static String raw( AnnotationFS token ) {
        return parse( token )[RAW];
    }
    
    /** 
     * Produce canonical String representation for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * This method omits raw text.
     * @param token A {@link Token}
     * @return The canonical string representation of the given token.
     */
    public static String build( AnnotationFS token ) {
        return build( token, false );
    }

    /** 
     * Produce canonical String representation for the given token.
     * This method calls {@link Tokens#parse(de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)} internally.
     * @param token A {@link Token}
     * @param addTxt    if {@code true}, append raw text to the canonical form.
     * @return The canonical string representation of the given token.
     */    
    public static String build( AnnotationFS token, boolean addTxt ) {
        return String.join( SEP, parse( token ) );
    }

    /**
     * Actions for string serialization of lexical tokens. See {@link POSClass} for details 
     * about lexicality determination.
     */
    public enum LexAction implements Function<AnnotationFS,String> {
        /** Keep original text: covered text **/
        KEEP_AS_IS(  ( AnnotationFS token ) -> parse( token )[RAW] ),
        /** Keep lemmatized form: replace by lemma/stem value **/
        LEMMATIZE( ( AnnotationFS token ) -> parse( token )[LMI] ),
        /** Keep POS tag group and lemmatized form: replaced by [POSG]_[lemma/stem] **/
        ADD_POS_GRP(  ( AnnotationFS token ) -> {
            String[] parse = parse( token );
            return String.join( SEP, parse[PGI], parse[LMI] );
        } ),
        /** Keep POS tag group, POS tag, and lemmatized form: replaced by [POSG]_[POST]_[lemma/stem] **/
        ADD_POS_TAG(  ( AnnotationFS token ) -> build( token ) ),
        /** Keep POS tag group, POS tag, lemma, and raw text: replaced by [POSG]_[POST]_[lemma]_[covered text] **/
        BUILD_FULL(  ( AnnotationFS token ) -> build( token, true ) ),
        /** Drop lemmas, keepm POS group and tag: replace by [POSG]_[POST] **/
        POS_ONLY( ( AnnotationFS token ) -> {
            String[] parse = parse( token );
            return String.join( SEP, parse[PGI], parse[PTI] );
        })
        ;

        private final Function<AnnotationFS,String> func;
        
        private LexAction( Function<AnnotationFS,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( AnnotationFS token ) {
            return func.apply( token );
        }
    }

    /**
     * Actions for string serialization of non-lexical tokens. See {@link POSClass} for details
     * about lexicality determination.
     **/
    public enum NonLexAction implements Function<AnnotationFS,String> {
        /** Delete everyhing **/
        DELETE( ( AnnotationFS t ) -> "" ),
        /** Replace with {@link Tokens#NL_MRKR} **/
        MARK(   ( AnnotationFS t ) -> Tokens.NL_MRKR ),
        /** Replace with POS group **/
        USE_POS_GRP(   ( AnnotationFS t ) -> parse( t )[PGI] ),
        /** Replace with POS tag **/
        USE_POS_TAG(   ( AnnotationFS t ) -> parse( t )[PTI] ),
        /** Replace with lemma **/
        LEMMATIZE(  ( AnnotationFS t ) -> parse( t )[LMI] ),
        ;

        private final Function<AnnotationFS,String> func;
        
        private NonLexAction( Function<AnnotationFS,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( AnnotationFS t ) {
            return func.apply( t );
        }
    }
    
}
