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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import eus.ixa.ixa.pipe.ml.StatisticalSequenceLabeler;
import eus.ixa.ixa.pipe.ml.sequence.SequenceLabel;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

/**
 * Annotation class for Aspect Based Sentiment Analysis (ABSA).
 * 
 * @author ragerri
 * @version 2018-04-24
 * 
 */
public class AnnotateAbsa implements Annotate {

  /**
   * The NameFinder to do the opinion target extraction.
   */
  private StatisticalSequenceLabeler oteExtractor;
  /**
   * The Document classifier to annotate polarity.
   */
  private StatisticalDocumentClassifier polTagger;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  
  public AnnotateAbsa(final Properties oteProperties, Properties polProperties) throws IOException {

    this.clearFeatures = oteProperties.getProperty("clearFeatures");
    oteExtractor = new StatisticalSequenceLabeler(oteProperties);
    polTagger = new StatisticalDocumentClassifier(polProperties);
  }
  
  /**
   * Annotate aspects, their targets and polarities.
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
        polTagger.clearFeatureData();
      }
      //target-aspects
      //TODO include aspects via document classification
      List<SequenceLabel> names = oteExtractor.getSequences(tokens);
      for (SequenceLabel name : names) {
        Integer startIndex = name.getSpan().getStart();
        Integer endIndex = name.getSpan().getEnd();
        List<Term> nameTerms = kaf.getTermsFromWFs(Arrays.asList(Arrays
            .copyOfRange(tokenIds, startIndex, endIndex)));
        ixa.kaflib.Span<Term> oteSpan = KAFDocument.newTermSpan(nameTerms);
        //Polarity Classification
        //String[] tokensAroundTarget = getTokensFromTerms(nameTerms);
        String polarity = polTagger.classify(tokens);
        List<Term> polarityTerms = kaf.getTermsFromWFs(Arrays.asList(Arrays
            .copyOfRange(tokenIds, 0, tokens.length)));
        //TODO expression span, perhaps heuristic around ote?
        ixa.kaflib.Span<Term> polaritySpan = KAFDocument.newTermSpan(polarityTerms);
        //create Opinion layer
        Opinion opinion = kaf.newOpinion();
        opinion.createOpinionTarget(oteSpan);
        OpinionExpression opExpression = opinion.createOpinionExpression(polaritySpan);
        //add aspect
        opExpression.setSentimentProductFeature(name.getType());
        //add polarity
        opExpression.setPolarity(polarity);
      }
      if (clearFeatures.equalsIgnoreCase("yes")) {
        oteExtractor.clearAdaptiveData();
        polTagger.clearFeatureData();
      }
    }
    oteExtractor.clearAdaptiveData();
    polTagger.clearFeatureData();
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
  
  private String[] getTokensFromTerms(List<Term> terms) {
    List<String> tokensList = new ArrayList<>();
    for (Term term : terms) {
      tokensList.add(term.getForm());
    }
    return tokensList.toArray(new String[tokensList.size()]);
  }

}
