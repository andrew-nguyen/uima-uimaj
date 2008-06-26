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

package org.apache.uima.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngineManagement;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.util.Level;

/**
 * 
 */
public abstract class UimaContext_ImplBase implements UimaContextAdmin {
  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  private ComponentInfo mComponentInfo = new ComponentInfoImpl();

  /**
   * Fully-qualified name of this context.
   */
  protected String mQualifiedContextName;

  /**
   * Mapping between sofa names assigned by an aggregate engine to sofa names assigned by the
   * component engines. The key is the component sofa name and the value is the absolute sofa name
   * assigned by a top level aggregate in this process.
   */
  protected Map mSofaMappings;

  /**
   * Size of the CAS pool used to support the {@link #getEmptyCas(Class)} method.
   */
  protected int mCasPoolSize = 0;

  /**
   * Performance tuning settings. Needed to specify CAS heap size for {@link #getEmptyCas(Class)}
   * method.
   */
  private Properties mPerformanceTuningSettings;

  /**
   * Whether the component that accesses the CAS pool is sofa-aware. Needed to determine which view
   * is returned by the {@link #getEmptyCas(Class)} method.
   */
  private boolean mSofaAware;

  /**
   * Keeps track of whether we've created a CAS pool yet, which happens on the first call to
   * {@link #getEmptyCas(Class)}.
   */
  private boolean mCasPoolCreated = false;

  /**
   * Number of CASes that have been requested via {@link #getEmptyCas(Class)} minus the number calls
   * the framework has made to {@link #returnedCAS()} (which indicate that the AnalysisComponent has
   * returned a CAS from its next() method. If this number exceeeds mCasPoolSize, the
   * AnalysisComponent has requested more CASes than it is allocated and we throw an exception.
   */
  protected int mOutstandingCasRequests = 0;

  /**
   * Object that implements management interface to the AE.
   */
  protected AnalysisEngineManagementImpl mMBean = new AnalysisEngineManagementImpl();

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#createChild(java.lang.String)
   */
  public UimaContextAdmin createChild(String aContextName, Map aSofaMappings) {
    // The aSofaMappings parameter, if present, defines the mapping between the child
    // context's sofa names and this context's sofa names. This context's sofa names
    // may again be remapped (according to the mSofaMappings field). We need to
    // produce the absolute mapping and pass that into the child context's constructor.

    // child context's mappings are originally equivalent to this context's mappings
    Map childSofaMap = new TreeMap();
    childSofaMap.putAll(mSofaMappings);
    if (aSofaMappings != null) {
      // iterate through remappings list (aSofaMappings) and apply them
      Iterator it = aSofaMappings.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry) it.next();
        String childSofaName = (String) entry.getKey();
        String thisContextSofaName = (String) entry.getValue();
        String absoluteSofaName = (String) mSofaMappings.get(thisContextSofaName);
        if (absoluteSofaName == null) {
          absoluteSofaName = thisContextSofaName;
        }
        childSofaMap.put(childSofaName, absoluteSofaName);
      }
    }

    // create child context with the absolute mappings
    ChildUimaContext_impl child = new ChildUimaContext_impl(this, aContextName, childSofaMap);

    // build a tree of MBeans that parallels the tree of UimaContexts
    mMBean.addComponent(aContextName, child.mMBean);

    return child;
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigParameterValue(String)
   */
  public Object getConfigParameterValue(String aName) {
    return getConfigurationManager().getConfigParameterValue(makeQualifiedName(aName));
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getConfigParameterValue(java.lang.String,
   *      java.lang.String)
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return getConfigurationManager().getConfigParameterValue(makeQualifiedName(aParamName),
            aGroupName);
  }

  /**
   * Locates Resource URL's using the ResourceManager.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURL(String)
   */
  public URL getResourceURL(String aKey) throws ResourceAccessException {
    URL result = getResourceManager().getResourceURL(makeQualifiedName(aKey));
    if (result != null) {
      return result;
    } else {
      // try as an unmanaged resource (deprecated)
      URL unmanagedResourceUrl = null;
      try {
        unmanagedResourceUrl = getResourceManager().resolveRelativePath(aKey);
      } catch (MalformedURLException e) {
        // if key is not a valid path then it cannot be resolved to an unmanged resource
      }
      if (unmanagedResourceUrl != null) {
        UIMAFramework.getLogger().logrb(Level.WARNING, this.getClass().getName(), "getResourceURL",
                LOG_RESOURCE_BUNDLE, "UIMA_unmanaged_resource__WARNING", new Object[] { aKey });
        return unmanagedResourceUrl;
      }
      return null;
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaContext#getResourceURI(java.lang.String)
   */
  public URI getResourceURI(String aKey) throws ResourceAccessException {
    URL resourceUrl = getResourceURL(aKey);
    if (resourceUrl != null) {
      try {
        return new URI(resourceUrl.toString().replaceAll(" ", "%20"));
      } catch (URISyntaxException e) {
        throw new ResourceAccessException(e);
      }
    }
    else {
      return null;
    }
  }
 

  /* (non-Javadoc)
   * @see org.apache.uima.UimaContext#getResourceFilePath(java.lang.String)
   */
  public String getResourceFilePath(String aKey) throws ResourceAccessException {
    URI resourceUri = getResourceURI(aKey);
    if (resourceUri != null) {
      if ("file".equals(resourceUri.getScheme())) {
        return resourceUri.getPath();
      } 
      else {
        throw new ResourceAccessException(); //TODO: error message
      }
    }
    else {
      return null;
    }
  }


  /**
   * Acquires Resource InputStreams using the ResourceManager.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceAsStream(String)
   */
  public InputStream getResourceAsStream(String aKey) throws ResourceAccessException {
    InputStream result = getResourceManager().getResourceAsStream(makeQualifiedName(aKey));
    if (result != null) {
      return result;
    } else {
      // try as an unmanaged resource (deprecated)
      URL unmanagedResourceUrl = null;
      try {
        unmanagedResourceUrl = getResourceManager().resolveRelativePath(aKey);
      } catch (MalformedURLException e) {
        // if key is not a valid path then it cannot be resolved to an unmanged resource
      }
      if (unmanagedResourceUrl != null) {
        UIMAFramework.getLogger().logrb(Level.WARNING, this.getClass().getName(),
                "getResourceAsStream", LOG_RESOURCE_BUNDLE, "UIMA_unmanaged_resource__WARNING",
                new Object[] { aKey });
        try {
          return unmanagedResourceUrl.openStream();
        } catch (IOException e) {
          throw new ResourceAccessException(e);
        }
      }
      return null;
    }
  }

  /**
   * Acquires a Resource object using the ResourceManager.
   * 
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceObject(String)
   */
  public Object getResourceObject(String aKey) throws ResourceAccessException {
    return getResourceManager().getResource(makeQualifiedName(aKey));
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceAsStream(java.lang.String,
   *      java.lang.String[])
   */
  public InputStream getResourceAsStream(String aKey, String[] aParams)
          throws ResourceAccessException {
    InputStream result = getResourceManager().getResourceAsStream(makeQualifiedName(aKey), aParams);
    if (result != null) {
      return result;
    } else {
      // try as an unmanaged resource (deprecated)
      URL unmanagedResourceUrl = null;
      try {
        unmanagedResourceUrl = getResourceManager().resolveRelativePath(aKey);
      } catch (MalformedURLException e) {
        // if key is not a valid path then it cannot be resolved to an unmanged resource
      }
      if (unmanagedResourceUrl != null) {
        UIMAFramework.getLogger().logrb(Level.WARNING, this.getClass().getName(),
                "getResourceAsStream", LOG_RESOURCE_BUNDLE, "UIMA_unmanaged_resource__WARNING",
                new Object[] { aKey });
        try {
          return unmanagedResourceUrl.openStream();
        } catch (IOException e) {
          throw new ResourceAccessException(e);
        }
      }
      return null;
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceObject(java.lang.String,
   *      java.lang.String[])
   */
  public Object getResourceObject(String aKey, String[] aParams) throws ResourceAccessException {
    return getResourceManager().getResource(makeQualifiedName(aKey), aParams);
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getResourceURL(java.lang.String,
   *      java.lang.String[])
   */
  public URL getResourceURL(String aKey, String[] aParams) throws ResourceAccessException {
    URL result = getResourceManager().getResourceURL(makeQualifiedName(aKey), aParams);
    if (result != null) {
      return result;
    } else {
      // try as an unmanaged resource (deprecated)
      URL unmanagedResourceUrl = null;
      try {
        unmanagedResourceUrl = getResourceManager().resolveRelativePath(aKey);
      } catch (MalformedURLException e) {
        // if key is not a valid path then it cannot be resolved to an unmanged resource
      }
      if (unmanagedResourceUrl != null) {
        UIMAFramework.getLogger().logrb(Level.WARNING, this.getClass().getName(), "getResourceURL",
                LOG_RESOURCE_BUNDLE, "UIMA_unmanaged_resource__WARNING", new Object[] { aKey });
        return unmanagedResourceUrl;
      }
      return null;
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaContext#getResourceURI(String, String[])
   */
  public URI getResourceURI(String aKey, String[] aParams) throws ResourceAccessException {
    URL resourceUrl = getResourceURL(aKey, aParams);
    if (resourceUrl != null) {
      try {
        return new URI(resourceUrl.toString().replaceAll(" ", "%20"));
      } catch (URISyntaxException e) {
        throw new ResourceAccessException(e);
      }
    }
    else {
      return null;
    }
  }
 

  /* (non-Javadoc)
   * @see org.apache.uima.UimaContext#getResourceFilePath(String, String[])
   */
  public String getResourceFilePath(String aKey, String[] aParams) throws ResourceAccessException {
    URI resourceUri = getResourceURI(aKey, aParams);
    if (resourceUri != null) {
      if ("file".equals(resourceUri.getScheme())) {
        return resourceUri.getPath();
      } 
      else {
        throw new ResourceAccessException(); //TODO: error message
      }
    }
    else {
      return null;
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.annotator.AnnotatorContext#getDataPath()
   */
  public String getDataPath() {
    return getResourceManager().getDataPath();
  }

  protected String makeQualifiedName(String name) {
    return mQualifiedContextName + name;
  }

  public String getQualifiedContextName() {
    return mQualifiedContextName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getConfigurationGroupNames()
   */
  public String[] getConfigurationGroupNames() {
    ConfigurationGroup[] groups = getConfigurationManager().getConfigParameterDeclarations(
            getQualifiedContextName()).getConfigurationGroups();
    if (groups == null) {
      return new String[0];
    } else {
      Set names = new TreeSet();
      for (int i = 0; i < groups.length; i++) {
        names.addAll(Arrays.asList(groups[i].getNames()));
      }
      String[] nameArray = new String[names.size()];
      names.toArray(nameArray);
      return nameArray;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getConfigurationParameterNames()
   */
  public String[] getConfigParameterNames() {
    ConfigurationParameter[] params = getConfigurationManager().getConfigParameterDeclarations(
            getQualifiedContextName()).getConfigurationParameters();
    if (params == null) {
      return new String[0];
    } else {
      String[] names = new String[params.length];
      for (int i = 0; i < params.length; i++) {
        names[i] = params[i].getName();
      }
      return names;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getConfigurationParameterNames(java.lang.String)
   */
  public String[] getConfigParameterNames(String aGroup) {
    ConfigurationGroup[] groups = getConfigurationManager().getConfigParameterDeclarations(
            getQualifiedContextName()).getConfigurationGroupDeclarations(aGroup);
    if (groups.length == 0) {
      return new String[0];
    } else {
      ArrayList names = new ArrayList();
      ConfigurationParameter[] commonParams = getConfigurationManager()
              .getConfigParameterDeclarations(getQualifiedContextName()).getCommonParameters();
      if (commonParams != null) {
        for (int i = 0; i < commonParams.length; i++) {
          names.add(commonParams[i].getName());
        }
      }
      for (int i = 0; i < groups.length; i++) {
        ConfigurationParameter[] groupParams = groups[i].getConfigurationParameters();
        for (int j = 0; j < groupParams.length; j++) {
          names.add(groupParams[j].getName());
        }
      }
      String[] nameArray = new String[names.size()];
      names.toArray(nameArray);
      return nameArray;
    }
  }

  /**
   * Changes here should also be made in UimaContext_ImplBase.mapToSofaID (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#mapToSofaID(java.lang.String)
   */
  public SofaID mapToSofaID(String aSofaName) {

    int index = aSofaName.indexOf(".");
    String nameToMap = aSofaName;
    String absoluteSofaName = null;
    if (index < 0) {
      absoluteSofaName = (String) mSofaMappings.get(nameToMap);
      if (absoluteSofaName == null)
        absoluteSofaName = nameToMap;

    } else {
      nameToMap = aSofaName.substring(0, index);
      String rest = aSofaName.substring(index);
      String absoluteRoot = (String) mSofaMappings.get(nameToMap);
      if (absoluteRoot == null)
        absoluteRoot = nameToMap;
      absoluteSofaName = absoluteRoot + rest;
    }
    SofaID sofaid = new SofaID_impl();
    sofaid.setSofaID(absoluteSofaName);
    return sofaid;
  }

  /**
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#mapSofaIDToComponentSofaName(java.lang.String)
   */
  public String mapSofaIDToComponentSofaName(String aSofaID) {
    String componentSofaName = aSofaID;
    SofaID[] sofaArr = getSofaMappings();
    for (int i = 0; i < sofaArr.length; i++) {
      if (aSofaID.equals(sofaArr[i].getSofaID()))
        return sofaArr[i].getComponentSofaName();
    }
    return componentSofaName;
  }

  /**
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getSofaMappings()
   */
  public SofaID[] getSofaMappings() {
    Set sofamap = mSofaMappings.entrySet();
    Iterator iter = sofamap.iterator();
    SofaID[] sofaArr = new SofaID_impl[sofamap.size()];
    int i = 0;
    while (iter.hasNext()) {
      Map.Entry elem = (Map.Entry) iter.next();
      SofaID sofaid = new SofaID_impl();
      sofaid.setComponentSofaName((String) elem.getKey());
      sofaid.setSofaID((String) elem.getValue());
      sofaArr[i] = sofaid;
      i++;
    }
    return sofaArr;
  }  

  /* (non-Javadoc)
   * @see org.apache.uima.UimaContextAdmin#getSofaMap()
   */
  public Map getSofaMap() {
    return Collections.unmodifiableMap(mSofaMappings);
  }

  public void defineCasPool(int aSize, Properties aPerformanceTuningSettings, boolean aSofaAware)
          throws ResourceInitializationException {
    mCasPoolSize = aSize;
    mPerformanceTuningSettings = aPerformanceTuningSettings;
    mSofaAware = aSofaAware;
    // cannot actually define the CAS Pool in the CasManager yet, because this happens
    // in the middle of initialization when the entire merged type system is not yet known.
  }

  /**
   * Not part of the public API. Called directly from the AnalysisEngine whenever the
   * AnalysisComponent returns a CAS from its next() method. Used to monitor the number of CASes
   * that the AnalysisComponent is using at any one time.
   */
  public void returnedCAS() {
    mOutstandingCasRequests--;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContext#getEmptyCas(java.lang.Class)
   */
  public AbstractCas getEmptyCas(Class aCasInterface) {
    if (!mCasPoolCreated) {
      // define CAS Pool in the CasManager
      try {
        getResourceManager().getCasManager().defineCasPool(getQualifiedContextName(), mCasPoolSize,
                mPerformanceTuningSettings);
      } catch (ResourceInitializationException e) {
        throw new UIMARuntimeException(e);
      }
      mCasPoolCreated = true;
    }

    mOutstandingCasRequests++;
    if (mOutstandingCasRequests > mCasPoolSize) {
      throw new UIMARuntimeException(UIMARuntimeException.REQUESTED_TOO_MANY_CAS_INSTANCES,
              new Object[] { getQualifiedContextName(), Integer.toString(mOutstandingCasRequests),
                  Integer.toString(mCasPoolSize) });
    }
    CasManager casManager = getResourceManager().getCasManager();
    CAS cas = casManager.getCas(getQualifiedContextName());
    // set the component info, so the CAS knows the proper sofa mappings
    cas.setCurrentComponentInfo(getComponentInfo());
    // if this is a sofa-aware component, give it the Base CAS
    // if it is a sofa-unaware component, give it whatever view maps to the _InitialVie
    if (mSofaAware) {
      cas = ((CASImpl) cas).getBaseCAS();
    } else {
      cas = cas.getView(CAS.NAME_DEFAULT_SOFA);
    }
    // convert CAS to requested interface
    return casManager.getCasInterface(cas, aCasInterface);
  }

  /**
   * @return
   */
  public ComponentInfo getComponentInfo() {
    return mComponentInfo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UimaContextAdmin#getManagementInterface()
   */
  public AnalysisEngineManagement getManagementInterface() {
    return mMBean;
  }

  /**
   * Implementation of the ComponentInfo interface that allows the CAS to access information from
   * this context- currently just the Sofa mappings.
   * 
   */
  class ComponentInfoImpl implements ComponentInfo {
    /*
     * Changes here should also be made in UimaContext_ImplBase.mapToSofaID
     * 
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.ComponentInfo#mapToSofaID(java.lang.String)
     * 
     */
    public String mapToSofaID(String aSofaName) {
      int index = aSofaName.indexOf(".");
      String nameToMap = aSofaName;
      String absoluteSofaName = null;
      if (index < 0) {
        absoluteSofaName = (String) mSofaMappings.get(nameToMap);
        if (absoluteSofaName == null)
          absoluteSofaName = nameToMap;
      } else {
        nameToMap = aSofaName.substring(0, index);
        String rest = aSofaName.substring(index);
        String absoluteRoot = (String) mSofaMappings.get(nameToMap);
        if (absoluteRoot == null)
          absoluteRoot = nameToMap;
        absoluteSofaName = absoluteRoot + rest;
      }
      return absoluteSofaName;
    }

  }
}