/*
 *  Copyright 2015 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.opinion;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabelFactory;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

/**
 * Annotation class for Opinion Target Extraction (OTE).
 * 
 * @author ragerri
 * @version 2015-04-29
 * 
 */
public class AnnotateTargets implements Annotate {

  /**
   * The factory to construct Name objects.
   */
  private SequenceLabelFactory nameFactory;
  /**
   * The NameFinder to do the opinion target extraction.
   */
  private StatisticalSequenceLabeler oteExtractor;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  
  public AnnotateTargets(final Properties properties) throws IOException {

    this.clearFeatures = properties.getProperty("clearFeatures");
    nameFactory = new SequenceLabelFactory();
    oteExtractor = new StatisticalSequenceLabeler(properties);
  }
  
  /**
   * Extract Opinion Targets.
   * @param kaf the KAFDocument
   */
  public final void annotate(final KAFDocument kaf) {

    List<List<WF>> sentences = kaf.getSentences();
    for (List<WF> sentence : sentences) {
      //process each sentence
      String[] tokens = new String[sentence.size()];
      String[] tokenIds = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
        tokenIds[i] = sentence.get(i).getId();
      }
      if (clearFeatures.equalsIgnoreCase("docstart") && tokens[0].startsWith("-DOCSTART-")) {
        oteExtractor.clearAdaptiveData();
      }
      List<SequenceLabel> names = oteExtractor.getSequences(tokens);
      for (SequenceLabel name : names) {
        Integer startIndex = name.getSpan().getStart();
        Integer endIndex = name.getSpan().getEnd();
        List<Term> nameTerms = kaf.getTermsFromWFs(Arrays.asList(Arrays
            .copyOfRange(tokenIds, startIndex, endIndex)));
        ixa.kaflib.Span<Term> oteSpan = KAFDocument.newTermSpan(nameTerms);
        Opinion opinion = kaf.newOpinion();
        opinion.createOpinionTarget(oteSpan);
        //TODO expression span, perhaps heuristic around ote?
        OpinionExpression opExpression = opinion.createOpinionExpression(oteSpan);
        opExpression.setSentimentProductFeature(name.getType());
      }
      if (clearFeatures.equalsIgnoreCase("yes")) {
        oteExtractor.clearAdaptiveData();
      }
    }
    oteExtractor.clearAdaptiveData();
  }

  /**
   * Output annotation as NAF.
   * 
   * @param kaf
   *          the naf document
   * @return the string containing the naf document
   */
  public final String annotateToNAF(KAFDocument kaf) {
    return kaf.toString();
  }

}
