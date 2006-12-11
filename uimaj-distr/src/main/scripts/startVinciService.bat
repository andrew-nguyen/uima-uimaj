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
@if "%~1"=="" goto usage
@call "%UIMA_HOME%\bin\setUimaClassPath"
@if "%JAVA_HOME%"=="" set JAVA_HOME=%UIMA_HOME%\java\jre

@set SERVICE=%~1
@set INSTANCEID=0

@if NOT "%~3"=="" goto execute2

@if "%~2"=="" goto execute
@set VNS_HOST=%~2
@:execute
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" org.apache.uima.adapter.vinci.VinciAnalysisEngineService_impl %SERVICE%
@goto end

@:execute2
@set VNS_HOST=%~2
@set INSTANCEID=%~3
"%JAVA_HOME%\bin\java" -cp "%UIMA_CLASSPATH%" "-Duima.datapath=%UIMA_DATAPATH%" -DVNS_HOST=%VNS_HOST% -DVNS_PORT=%VNS_PORT% "-Djava.util.logging.config.file=%UIMA_HOME%\Logger.properties" org.apache.uima.adapter.vinci.VinciAnalysisEngineService_impl %SERVICE% %INSTANCEID%
@goto end

@:usage
@  echo Usage: startVinciService.sh svcdescriptor [vns_host]
@:end

