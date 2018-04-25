
ixa-pipe-opinion
================

[![Build Status](https://travis-ci.org/ixa-ehu/ixa-pipe-opinion.svg?branch=master)](https://travis-ci.org/ixa-ehu/ixa-pipe-opinion)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/apache/opennlp/master/LICENSE)

ixa-pipe-opinion is a multilingual Aspect Based Opinion tagger consisting of Opinion Target Extraction (OTE), Aspect detection and polarity tagging.

ixa-pipe-opinion is part of IXA pipes, a multilingual set of NLP tools developed
by the IXA NLP Group [http://ixa2.si.ehu.es/ixa-pipes].

Please go to [http://ixa2.si.ehu.es/ixa-pipes] for general information about the IXA
pipes tools but also for **official releases, including source code and binary
packages for all the tools in the IXA pipes toolkit**.

This document is intended to be the **usage guide of ixa-pipe-opinion**. If you really need to clone
and install this repository instead of using the releases provided in
[http://ixa2.si.ehu.es/ixa-pipes], please scroll down to the end of the document for
the [installation instructions](#installation).

**NOTICE!!**: ixa-pipe-opinion is in [Maven Central](http://search.maven.org/).

## TABLE OF CONTENTS

1. [Overview](#overview)
  + [Available features](#features)
  + [ABSA distributed models](#absa-models)
  + [OTE distributed models](#ote-models)
2. [Usage](#usage)
  + [Aspect Based Sentiment Analysis(ABSA)](#absa)
  + [Opinion Target Extraction (OTE)](#ote)
  + [Aspect detection](#aspects)
  + [Polarity tagging](#polarity)
  + [Server mode](#server)
  + [Training your own models](#training)
  + [Evaluation](#evaluation)
3. [API via Maven Dependency](#api)
4. [Git installation](#installation)

## OVERVIEW

ixa-pipe-opinion provides Aspect Based Sentiment Analysis (ABSA) using sequence labeling and document classification trained on SemEval ABSA 2014-2016 datasets.

+ **Opinion Target Extraction (OTE)**: Sequence labeler to detect the opinion targets.
+ **Aspect detection**: Sequence labeler and Document Classification to detect aspects of opinions.
+ **Polarity tagging**: For a document and/or sentence the polarity is tagged.

We provide competitive models based on robust local features and exploiting unlabeled data
via clustering features. The clustering features are based on Brown, Clark (2003)
and Word2Vec clustering. To avoid duplication of efforts, we use and contribute to the API provided by the
[Apache OpenNLP project](http://opennlp.apache.org) with our own custom developed features for each of the three tasks.

### Features

**A description of every feature is provided in the sequenceTrainer.properties and docTrainer.properties
file** distributed with [ixa-pipe-ml](https://github.com/ixa-ehu/ixa-pipe-ml). As the training functionality is configured in
properties files, please do check this document.

### ABSA-Models

We distribute Opinion Target Extraction models for the restaurant domain trained with the SemEval ABSA 2014-2016 datasets for four languages: English, Spanish, French and Dutch. We obtain state-of-the-art results for every evaluation setting and language.

+ [English 2016 ABSA Models](http://ixa2.si.ehu.es/ixa-pipes/models/en-absa-models-1.0.0.tar.gz) [119MB]
+ [Spanish ABSA Models](http://ixa2.si.ehu.es/ixa-pipes/models/es-absa-models-1.0.0.tar.gz) [29MB]
+ [French ABSA Models](http://ixa2.si.ehu.es/ixa-pipes/models/fr-absa-models-1.0.0.tar.gz) [34MB]
+ [Dutch ABSA Models](http://ixa2.si.ehu.es/ixa-pipes/models/nl-absa-models-1.0.0.tar.gz) [27MB]

### OTE-Models

We distribute Opinion Target Extraction models for the restaurant domain trained with the SemEval ABSA 2014-2016 datasets for six languages: English, Spanish, French, Dutch, Turkish and Russian. The system was the task winner of the ote substask in the ABSA 2015 edition. We also obtain state-of-the-art results for the rest of evaluation settings and languages.

+ [English 2014-2016 ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/en-ote-models-1.0.0.tar.gz) [119MB]
+ [Spanish ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/es-ote-models-1.0.0.tar.gz) [29MB]
+ [French ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/fr-ote-models-1.0.0.tar.gz) [34MB]
+ [Dutch ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/nl-ote-models-1.0.0.tar.gz) [27MB]
+ [Turkish ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/tr-ote-models-1.0.0.tar.gz) [12MB]
+ [Russian ABSA OTE Models](http://ixa2.si.ehu.es/ixa-pipes/models/ru-ote-models-1.0.0.tar.gz) [61MB]

We also distribute a set of scripts to [reproduce the results using the ABSA test set and evaluation scripts](http://ixa2.si.ehu.es/ixa-pipes/models/absa-ote-evaluation-scripts.tar.gz).

## Usage

ixa-pipe-opinion provides a runable jar with the following command-line basic functionalities:

1. **absa**: reads a NAF document containing *wf* and *term* elements and performs
   Aspect Based Sentiment Analysis. 
2. **ote**: reads a NAF document containing *wf* and *term* elements and performs
   opinion target extraction (OTE).
3. **aspect**: reads a NAF document containing *wf* and *term* elements and detects aspects.
4. **pol**: reads a NAF document containing *wf* and *term* elements and tags polarity.
5. **server**: starts a TCP service loading the model and required resources.
6. **client**: sends a NAF document to a running TCP server.

Each of these functionalities are accessible by adding (absa|ote|aspect|pol|server|client) as a
subcommand to ixa-pipe-opinion-${version}-exec.jar. Please read below and check the -help
parameter:

````shell
java -jar target/ixa-pipe-opinion-${version}-exec.jar (absa|ote|aspect|pol|server|client) -help
````
### ABSA

Aspect Based Sentiment Analysis requires an input NAF with *wf* and *term* elements:

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l $lang | java -jar ixa-pipe-pos-$version-exec.jar tag -m posmodel.bin -lm lemma-model.bin | java -jar ixa-pipe-opinion-${version}-exec.jar absa -t targetModel.bin -p polarityModel.bin
````

ixa-pipe-opinion reads NAF documents (with *wf* and *term* elements) via standard input and outputs opinion elements containing targets, aspects and their polarities in NAF through standard output. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-nerc by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) and
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) as shown in the
example.

There are several options to tag with ixa-pipe-opinion (check the -help parameter for more info).

+ **targetModel**: model for target and aspect detection.
+ **polarityModel**: model for polarity classification
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: available OpenNLP native format and NAF. It defaults to NAF.

### OTE

Opinion Target Extraction requires an input NAF with *wf* and *term* elements:

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l $lang | java -jar ixa-pipe-pos-$version-exec.jar tag -m posmodel.bin -lm lemma-model.bin | java -jar ixa-pipe-opinion-${version}-exec.jar ote -m model.bin
````

ixa-pipe-opinion reads NAF documents (with *wf* and *term* elements) via standard input and outputs opinion targets in NAF
through standard output. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-nerc by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) and
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) as shown in the
example.

There are several options to tag with ixa-pipe-opinion (check the -help parameter for more info).

+ **model**: pass the model as a parameter.
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: available OpenNLP native format and NAF. It defaults to NAF.

### Aspects

Aspect detection requires an input NAF with *wf* and *term* elements. It is also required to specify the tagger type: Document Classification (doc) or Sequence labeling (seq).

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l $lang | java -jar ixa-pipe-pos-$version-exec.jar tag -m posmodel.bin -lm lemma-model.bin | java -jar ixa-pipe-opinion-${version}-exec.jar aspect -t seq -m model.bin
````
ixa-pipe-opinion reads NAF documents (with *wf* and *term* elements) via standard input and outputs opinion expressions containing the aspects for each sentence. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-nerc by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) and
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) as shown in the
example.

There are several other options to tag with ixa-pipe-opinion (check the -help parameter for more info).

+ **model**: pass the model as a parameter.
+ **tagger**: choose between doc (document classification) or seq (Sequence labeling).
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: available OpenNLP native format and NAF. It defaults to NAF.

### Polarity

Polarity tagging requires an input NAF with *wf* and *term* elements.

````shell
cat file.txt | java -jar ixa-pipe-tok-$version-exec.jar tok -l $lang | java -jar ixa-pipe-pos-$version-exec.jar tag -m posmodel.bin -lm lemma-model.bin | java -jar ixa-pipe-opinion-${version}-exec.jar pol -m model.bin
````
The polarity parameter of ixa-pipe-opinion reads NAF documents (with *wf* and *term* elements) via standard input and outputs opinion expressions containing the polarity for each sentence. The NAF format specification is here:

(http://wordpress.let.vupr.nl/naf/)

You can get the necessary input for ixa-pipe-nerc by piping
[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) and
[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos) as shown in the
example.

There are several other options to tag with ixa-pipe-opinion (check the -help parameter for more info).

+ **model**: pass the model as a parameter.
+ **language**: pass the language as a parameter.
+ **outputFormat**: Output annotation in a format: available OpenNLP native format and NAF. It defaults to NAF.
+ **dict**: Tag tokens with a polarity lexicon.

### Server

We can start the TCP server as follows:

````shell
java -jar target/ixa-pipe-opinion-${version}-exec.jar server -l en --port 2030 -t aspect -c seq -m model.bin
````
Once the server is running we can send NAF documents containing (at least) the term layer like this:

````shell
 cat file.pos.naf | java -jar target/ixa-pipe-opinion-${version}-exec.jar client -p 2060
````

## API

The easiest way to use ixa-pipe-opinion programatically is via Apache Maven. Add
this dependency to your pom.xml:

````shell
<dependency>
    <groupId>eus.ixa</groupId>
    <artifactId>ixa-pipe-opinion</artifactId>
    <version>1.0.0</version>
</dependency>
````

## JAVADOC

The javadoc of the module is located here:

````shell
ixa-pipe-opinion/target/ixa-pipe-opinion-$version-javadoc.jar
````

## Module contents

The contents of the module are the following:

    + formatter.xml           Apache OpenNLP code formatter for Eclipse SDK
    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module and required resources
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories
    + trainParams.properties      A template properties file containing documention
    for every available option


## INSTALLATION

Installing the ixa-pipe-nerc requires the following steps:

If you already have installed in your machine the Java 1.8+ and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

### 1. Install JDK 1.8

If you do not install JDK 1.8+ in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java8
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java8
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your JDK is 1.8.

### 2. Install MAVEN 3

Download MAVEN from

````shell
https://maven.apache.org/download.cgi
````
Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/ragerri/local/apache-maven-3.3.9
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.3.5
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````
You should see reference to the MAVEN version you have just installed plus the JDK that is using.

### 3. Get module source code

If you must get the module source code from here do this:

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-opinion
````

### 4. Compile

Execute this command to compile ixa-pipe-opinion:

````shell
cd ixa-pipe-opinion
mvn clean package
````
This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

ixa-pipe-opinion-${version}-exec.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.8 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## Contact information

````shell
Rodrigo Agerri
IXA NLP Group
University of the Basque Country (UPV/EHU)
E-20018 Donostia-San Sebastián
rodrigo.agerri@ehu.eus
````
