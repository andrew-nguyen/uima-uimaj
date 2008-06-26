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

package org.apache.uima.internal.util;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.Constants;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.impl.ResultSpecification_impl;
import org.apache.uima.analysis_engine.impl.TaeDescription_impl;
import org.apache.uima.analysis_engine.impl.TestAnnotator;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;


public class TextAnalysisEnginePoolTest extends TestCase {

  /**
   * Constructor for MultithreadableAnalysisEngine_implTest.
   * 
   * @param arg0
   */
  public TextAnalysisEnginePoolTest(String arg0) throws java.io.FileNotFoundException {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      mSimpleDesc = new TaeDescription_impl();
      mSimpleDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      mSimpleDesc.setPrimitive(true);
      mSimpleDesc.getMetaData().setName("Test Primitive TAE");
      mSimpleDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      mSimpleDesc.getMetaData().setName("Simple Test");
      Capability cap = new Capability_impl();
      cap.addOutputType("NamedEntity", true);
      cap.addOutputType("DocumentStructure", true);
      Capability[] caps = new Capability[] {cap};
      mSimpleDesc.getAnalysisEngineMetaData().setCapabilities(caps);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetAnalysisEngineMetaData() throws Exception {
    TextAnalysisEnginePool pool = null;
    try {
      // create pool
      pool = new TextAnalysisEnginePool("taePool", 3, mSimpleDesc);

      TextAnalysisEngine tae = pool.getTAE();
      AnalysisEngineMetaData md = tae.getAnalysisEngineMetaData();
      Assert.assertNotNull(md);
      Assert.assertEquals("Simple Test", md.getName());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    } finally {
      if (pool != null)
        pool.destroy();
    }
  }

  public void testProcess() throws Exception {
    try {
      // test simple primitive MultithreadableTextAnalysisEngine
      // (using TestAnnotator class)
      TextAnalysisEnginePool pool = new TextAnalysisEnginePool("taePool", 3, mSimpleDesc);
      _testProcess(pool, 0);

      // test simple aggregate MultithreadableTextAnalysisEngine
      // (again using TestAnnotator class)
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", mSimpleDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      pool = new TextAnalysisEnginePool("taePool", 3, aggDesc);
      _testProcess(pool, 0);

      // multiple threads!
      final int NUM_THREADS = 4;
      ProcessThread[] threads = new ProcessThread[NUM_THREADS];
      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i] = new ProcessThread(pool, i);
        threads[i].start();
      }

      // wait for threads to finish and check if they got exceptions
      for (int i = 0; i < NUM_THREADS; i++) {
        threads[i].join();
        Throwable failure = threads[i].getFailure();
        if (failure != null) {
          if (failure instanceof Exception) {
            throw (Exception)failure;
          } else {
            fail(failure.getMessage());
          }
        }
      }     
      
      //Check TestAnnotator fields only at the very end of processing,
      //we can't test from the threads themsleves since the state of
      //these fields is nondeterministic during the multithreaded processing.
      assertEquals("testing...", TestAnnotator.getLastDocument());
      ResultSpecification resultSpec = new ResultSpecification_impl();
      resultSpec.addResultType("NamedEntity", true);
      assertEquals(resultSpec, TestAnnotator.getLastResultSpec());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    
   
  }

  public void testReconfigure() throws Exception {
    try {
      // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(new ConfigurationParameter[] { p1 });
      primitiveDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test1") });

      // create pool
      TextAnalysisEnginePool pool = new TextAnalysisEnginePool("taePool", 3, primitiveDesc);

      TextAnalysisEngine tae = pool.getTAE();
      try {
        // check value of string param (TestAnnotator saves it in a static field)
        assertEquals("Test1", TestAnnotator.stringParamValue);

        // reconfigure
        tae.setConfigParameterValue("StringParam", "Test2");
        tae.reconfigure();

        //test again
        assertEquals("Test2", TestAnnotator.stringParamValue);

        // check pool metadata
        pool.getMetaData().setUUID(tae.getMetaData().getUUID());
        Assert.assertEquals(tae.getMetaData(), pool.getMetaData());
      } finally {
        pool.releaseTAE(tae);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Auxilliary method used by testProcess()
   * 
   * @param aTaeDesc
   *          description of TextAnalysisEngine to test
   */
  protected void _testProcess(TextAnalysisEnginePool aPool, int i)
          throws UIMAException {
    TextAnalysisEngine tae = aPool.getTAE(0);
    try {
      // Test each form of the process method. When TestAnnotator executes, it
      // stores in static fields the document text and the ResultSpecification.
      // We use thse to make sure the information propogates correctly to the annotator.

      // process(CAS)
      CAS tcas = tae.newCAS();
      tcas.setDocumentText("new test");
      tae.process(tcas);
      tcas.reset();

      // process(CAS,ResultSpecification)
      ResultSpecification resultSpec = new ResultSpecification_impl();
      resultSpec.addResultType("NamedEntity", true);

      tcas.setDocumentText("testing...");
      tae.process(tcas, resultSpec);
      tcas.reset();
    } finally {
      aPool.releaseTAE(tae);
    }
  }

  class ProcessThread extends Thread {
    ProcessThread(TextAnalysisEnginePool aPool, int aId) {
      mPool = aPool;
      mId = aId;
    }

    public void run() {
      try {
        // System.out.println("thread started");
        _testProcess(mPool, mId);
        // System.out.println("thread finished");
      } catch (Throwable t) {
        t.printStackTrace();
        //can't cause unit test to fail by throwing exception from thread.
        //record the failure and the main thread will check for it later.
        mFailure = t;
      }
    }
    
    public synchronized Throwable getFailure() {
      return mFailure;
    }

    int mId;

    TextAnalysisEnginePool mPool;

    boolean mIsAggregate;
    
    Throwable mFailure = null;
  }

  private TaeDescription mSimpleDesc;
}