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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import eus.ixa.ixa.pipe.ml.polarity.DictionaryPolarityTagger;
import eus.ixa.ixa.pipe.ml.utils.Flags;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
import ixa.kaflib.Term;
import ixa.kaflib.Term.Sentiment;
import ixa.kaflib.WF;

/**
 * Annotation class for polarity tagging using document classification.
 * 
 * @author ragerri
 * @version 2017-06-09
 * 
 */
public class AnnotatePolarity implements Annotate {

  /**
   * The Document classifier to extract the aspects.
   */
  private StatisticalDocumentClassifier polTagger;
  private DictionaryPolarityTagger dictTagger;
  private Boolean isDict = false;
  private String dictionary = null;
  /**
   * Clear features after every sentence or when a -DOCSTART- mark appears.
   */
  private String clearFeatures;

  
  public AnnotatePolarity(final Properties properties) throws IOException {

    this.clearFeatures = properties.getProperty("clearFeatures");
    dictionary = properties.getProperty("dictionary");
    if (!dictionary.equalsIgnoreCase(Flags.DEFAULT_DICT_OPTION)) {
      dictTagger = new DictionaryPolarityTagger(new FileInputStream(dictionary));
      isDict = true;
    }
    polTagger = new StatisticalDocumentClassifier(properties);
  }
  
  /**
   * Annotate polarity using a document classifier.
   * @param kaf the KAFDocument
   */
  public final void annotate(final KAFDocument kaf) {

    List<List<WF>> sentences = kaf.getSentences();
    List<Term> terms = kaf.getTerms();
    for (List<WF> sentence : sentences) {
      //process each sentence
      String[] tokens = new String[sentence.size()];
      String[] tokenIds = new String[sentence.size()];
      for (int i = 0; i < sentence.size(); i++) {
        tokens[i] = sentence.get(i).getForm();
        tokenIds[i] = sentence.get(i).getId();
      }
      if (isDict) {
        for (Term term : terms) {
          String polarity = dictTagger.tag(term.getForm());
          if (polarity.equalsIgnoreCase("O")) {
            polarity = dictTagger.tag(term.getLemma());
          }
          if (!polarity.equalsIgnoreCase("O")) {
            Sentiment sentiment = term.createSentiment();
            sentiment.setPolarity(polarity);
            sentiment.setResource(Files.getNameWithoutExtension(dictionary));
          }
        }
      }
      if (clearFeatures.equalsIgnoreCase("docstart") && tokens[0].startsWith("-DOCSTART-")) {
        polTagger.clearFeatureData();
      }
      //Document Classification
      String polarity = polTagger.classify(tokens);
      List<Term> polarityTerms = kaf.getTermsFromWFs(Arrays.asList(Arrays
            .copyOfRange(tokenIds, 0, tokens.length)));
      ixa.kaflib.Span<Term> polaritySpan = KAFDocument.newTermSpan(polarityTerms);
      Opinion opinion = kaf.newOpinion();
      //TODO expression span, perhaps heuristic around ote and/or around opinion expression?
      OpinionExpression opExpression = opinion.createOpinionExpression(polaritySpan);
      opExpression.setPolarity(polarity);
      if (clearFeatures.equalsIgnoreCase("yes")) {
        polTagger.clearFeatureData();
      }
    }
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
  
  public final String annotatePolarityToTabulated(KAFDocument kaf) {
    return kaf.toString();
  }

}
