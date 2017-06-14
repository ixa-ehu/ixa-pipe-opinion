/*
 *  Copyright 2017 Rodrigo Agerri

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

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

/**
 * Annotation class for Aspect extraction using document classification.
 * 
 * @author ragerri
 * @version 2017-06-09
 * 
 */
public class DocAnnotateAspects implements Annotate {

  /**
   * The Document classifier to extract the aspects.
   */
  private StatisticalDocumentClassifier aspectExtractor;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  
  public DocAnnotateAspects(final Properties properties) throws IOException {

    this.clearFeatures = properties.getProperty("clearFeatures");
    aspectExtractor = new StatisticalDocumentClassifier(properties);
  }
  
  /**
   * Extract aspects using a document classifier.
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
        aspectExtractor.clearFeatureData();
      }
      String aspect = aspectExtractor.classify(tokens);
      List<Term> aspectTerms = kaf.getTermsFromWFs(Arrays.asList(Arrays
            .copyOfRange(tokenIds, 0, tokens.length)));
      ixa.kaflib.Span<Term> aspectSpan = KAFDocument.newTermSpan(aspectTerms);
      Opinion opinion = kaf.newOpinion();
      //TODO expression span, perhaps heuristic around ote?
      OpinionExpression opExpression = opinion.createOpinionExpression(aspectSpan);
      opExpression.setSentimentProductFeature(aspect);
      if (clearFeatures.equalsIgnoreCase("yes")) {
        aspectExtractor.clearFeatureData();
      }
    }
    aspectExtractor.clearFeatureData();
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
  
  public final String annotateAspectsToTabulated(KAFDocument kaf) {
    return kaf.toString();
  }

}
