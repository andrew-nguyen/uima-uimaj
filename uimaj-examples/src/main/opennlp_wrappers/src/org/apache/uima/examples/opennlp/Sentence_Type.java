/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.examples.opennlp;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * Updated by JCasGen Fri Dec 02 14:22:23 EST 2005
 * 
 * @generated
 */
public class Sentence_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (Sentence_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = Sentence_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Sentence(addr, Sentence_Type.this);
          Sentence_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Sentence(addr, Sentence_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = Sentence.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.examples.opennlp.Sentence");

  /** @generated */
  final Feature casFeat_componentId;

  /** @generated */
  final int casFeatCode_componentId;

  /** @generated */
  public String getComponentId(int addr) {
    if (featOkTst && casFeat_componentId == null)
      this.jcas.throwFeatMissing("componentId", "org.apache.uima.examples.opennlp.Sentence");
    return ll_cas.ll_getStringValue(addr, casFeatCode_componentId);
  }

  /** @generated */
  public void setComponentId(int addr, String v) {
    if (featOkTst && casFeat_componentId == null)
      this.jcas.throwFeatMissing("componentId", "org.apache.uima.examples.opennlp.Sentence");
    ll_cas.ll_setStringValue(addr, casFeatCode_componentId, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public Sentence_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_componentId = jcas.getRequiredFeatureDE(casType, "componentId", "uima.cas.String",
            featOkTst);
    casFeatCode_componentId = (null == casFeat_componentId) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_componentId).getCode();

  }
}