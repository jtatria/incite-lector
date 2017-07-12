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
package edu.columbia.incite.uima.run.debug;

import edu.columbia.incite.uima.ae.SegmentedEngine;
import edu.columbia.incite.util.data.DataField;
import edu.columbia.incite.util.data.Datum;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;

/**
 *
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class SegmentMarker extends SegmentedEngine {

    Set<DataField> dfs = new HashSet<>();
    
    @Override
    protected void processSegment( AnnotationFS segment, List<AnnotationFS> covers, List<AnnotationFS> members ) throws AnalysisEngineProcessException {
        Datum md = getMetadata();
        for( DataField df : md.fields() ) {
            if( !dfs.contains( df ) ) {
                dfs.add( df );
            }
        }
    }
    
    @Override
    public void collectionProcessComplete() {
        for( DataField df : dfs ) {
            System.out.printf( "%s:%s\n", df.name, df.type.toString() );
        }
    }
}
