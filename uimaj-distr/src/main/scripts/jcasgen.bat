@echo off

REM   Licensed to the Apache Software Foundation (ASF) under one
REM   or more contributor license agreements.  See the NOTICE file
REM   distributed with this work for additional information
REM   regarding copyright ownership.  The ASF licenses this file
REM   to you under the Apache License, Version 2.0 (the
REM   "License"); you may not use this file except in compliance
REM   with the License.  You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM   Unless required by applicable law or agreed to in writing,
REM   software distributed under the License is distributed on an
REM   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM   KIND, either express or implied.  See the License for the
REM   specific language governing permissions and limitations
REM   under the License.

@echo on

@setlocal
@if "%~1"=="" goto next1 
@set firstarg=-jcasgeninput "%~1"
@if "%~2"=="" goto next1
@set secondarg=-jcasgenoutput "%~2"
@:next1
@call "%UIMA_HOME%\bin\setUimaClasspath"
@set LOGGER=-Djava.util.logging.config.file=%UIMA_HOME%\FileConsoleLogger.properties
@echo Running JCasGen with no Java CAS Model merging.  To run with merging, use jcasgen_merge (requires Eclipse, plus UIMA and EMF plugins).
@if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre
"%JAVA_HOME%\bin\java" "%LOGGER%" -cp "%UIMA_CLASSPATH%" org.apache.uima.tools.jcasgen.Jg %firstarg% %secondarg%
