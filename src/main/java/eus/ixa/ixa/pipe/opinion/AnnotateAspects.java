package eus.ixa.ixa.pipe.opinion;

import ixa.kaflib.KAFDocument;

public interface AnnotateAspects {
  
  /**
   * Extracts aspects from a text and creates an opinion layer in NAF.
   * @param kaf the NAF document
   */
  public void annotateAspects(KAFDocument kaf);
  
  /**
   * Serializes the NAF containing opinion layer with aspects.
   * @param kaf
   * @return
   */
  public String annotateAspectsToNAF(KAFDocument kaf);

}
