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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

// *********************************
// * Implementation of TOP *
// *********************************
/**
 * The Java Class model corresponding to the Cas TOP type. This type is the super type of all
 * feature structures. This type implements FeatureStructure since subclasses of it are the FSs
 * generated by this system.
 */
public class TOP extends FeatureStructureImpl {

  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(TOP.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the TOP_Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /*
   * Note this class doesn't extend FeatureStructureImpl because that would add one more slot (the
   * casImpl ref) to every instance.
   */
  /** used to reference the corresponding TOP_Type instance */
  public final TOP_Type jcasType;

  /** used to reference the corresponding Cas instance */
  protected final int addr;

  // This constructor is never called .
  // Have to set "final" values to avoid compile errors
  protected TOP() {
    jcasType = null;
    addr = 0;
    throw new RuntimeException("Internal Error: TOP() constructor should never be called.");
  }

  /**
   * (Internal) make a new instance of TOP, linking it with a pre-existing Cas FeatureStructure
   * object. Note: this function invoked via the generator in TOP_Type whenever the CAS needs to
   * make a java instance
   */
  public TOP(int addr, TOP_Type jcasType) {
    this.addr = addr;
    this.jcasType = jcasType;
  }

  /**
   * (Internal) create a new instance of TOP (or subclass of TOP) in Java and Cas, and make them
   * correspond.
   */
  public TOP(JCas jcas) {
    this.jcasType = jcas.getType(getTypeIndexID());
    if (null == jcasType) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS);
      e.addArgument(this.getClass().getName());
      throw e;
    }
    this.addr = jcasType.ll_cas.ll_createFS(jcasType.casTypeCode);
    jcas.putJfsFromCaddr(addr, this);
    CAS cas = jcas.getCas();
    if (((CASImpl) cas).isAnnotationType(jcasType.casTypeCode)) {
      ((CASImpl) cas).setSofaFeat(addr, ((CASImpl) cas).getSofaRef());
    }
  }

  /** add the corresponding FeatureStructure to all Cas indexes */
  public void addToIndexes() {
    addToIndexes(jcasType.jcas);
  }

  public void addToIndexes(JCas jcas) {
    jcas.getCas().addFsToIndexes(this);
  }

  /** remove the corresponding FeatureStructure from all Cas indexes */
  public void removeFromIndexes() {
    removeFromIndexes(jcasType.jcas);
  }

  public void removeFromIndexes(JCas jcas) {
    jcas.getLowLevelIndexRepository().ll_removeFS(this.addr);
  }

  /*
   * functions needed to to implement the FeatureStructure interface Note we don't simply implement
   * this class as a subclass of FeatureStructureImpl because that would add one more slot to every
   * instance
   */

  public int getAddress() {
    return this.addr;
  }

  public CASImpl getCASImpl() {
    return jcasType.casImpl;
  }

  public CAS getCAS() {
    return jcasType.casImpl;
  }

  public LowLevelCAS getLowLevelCas() {
    return jcasType.casImpl;
  }

  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof TOP))
      return false;
    TOP fs = (TOP) o;
    if ((this.addr == fs.addr) && (this.jcasType.casImpl == fs.jcasType.casImpl)) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return this.addr;
  }

}
