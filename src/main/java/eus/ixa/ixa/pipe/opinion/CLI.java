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

import ixa.kaflib.KAFDocument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import opennlp.tools.cmdline.CmdLineUtil;

import org.jdom2.JDOMException;

import com.google.common.io.Files;

import eus.ixa.ixa.pipe.ml.utils.Flags;


/**
 * Main class of ixa-pipe-nerc, the ixa pipes (ixa2.si.ehu.es/ixa-pipes) sequence
 * labeler.
 * 
 * @author ragerri
 * @version 2015-02-26
 * 
 */
public class CLI {

  /**
   * Get dynamically the version of ixa-pipe-nerc by looking at the MANIFEST
   * file.
   */
  private final String version = CLI.class.getPackage()
      .getImplementationVersion();
  /**
   * Get the git commit of the ixa-pipe-nerc compiled by looking at the MANIFEST
   * file.
   */
  private final String commit = CLI.class.getPackage().getSpecificationVersion();
  /**
   * Name space of the arguments provided at the CLI.
   */
  private Namespace parsedArguments = null;
  /**
   * Argument parser instance.
   */
  private ArgumentParser argParser = ArgumentParsers.newArgumentParser(
      "ixa-pipe-nerc-" + version + ".jar").description(
      "ixa-pipe-nerc-" + version
          + " is a multilingual sequence labeler module developed by IXA NLP Group.\n");
  /**
   * Sub parser instance.
   */
  private Subparsers subParsers = argParser.addSubparsers().help(
      "sub-command help");
  /**
   * Parser to manage the Opinion Target Extraction sub-command.
   */
  private Subparser oteParser;
  /**
   * Parser to start TCP socket for server-client functionality.
   */
  private Subparser serverParser;
  /**
   * Sends queries to the serverParser for annotation.
   */
  private Subparser clientParser;
  
  /**
   * Construct a CLI object with the sub-parsers to manage the command
   * line parameters.
   */
  public CLI() {
    subParsers.addParser("tag").help("NER Tagging CLI");
    loadOteParameters();
    serverParser = subParsers.addParser("server").help("Start TCP socket server");
    loadServerParameters();
    clientParser = subParsers.addParser("client").help("Send queries to the TCP socket server");
    loadClientParameters();
    }

  /**
   * Main entry point of ixa-pipe-nerc.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if input data not available
   * @throws JDOMException
   *           if problems with the xml formatting of NAF
   */
  public static void main(final String[] args) throws IOException,
      JDOMException {

    CLI cmdLine = new CLI();
    cmdLine.parseCLI(args);
  }

  /**
   * Parse the command interface parameters with the argParser.
   * 
   * @param args
   *          the arguments passed through the CLI
   * @throws IOException
   *           exception if problems with the incoming data
   * @throws JDOMException if xml format problems
   */
  public final void parseCLI(final String[] args) throws IOException, JDOMException {
    try {
      parsedArguments = argParser.parseArgs(args);
      System.err.println("CLI options: " + parsedArguments);
      if (args[0].equals("tag")) {
    	  extractOte(System.in, System.out);
      } else if (args[0].equals("server")) {
        server();
      } else if (args[0].equals("client")) {
        client(System.in, System.out);
      }
    } catch (ArgumentParserException e) {
      argParser.handleError(e);
      System.out.println("Run java -jar target/ixa-pipe-nerc-" + version
          + ".jar (tag|server|client) -help for details");
      System.exit(1);
    }
  }  
  /**
   * Main method to do Opinion Target Extraction (OTE).
   * 
   * @param inputStream
   *          the input stream containing the content to tag
   * @param outputStream
   *          the output stream providing the opinion targets
   * @throws IOException
   *           exception if problems in input or output streams
   * @throws JDOMException if xml formatting problems
   */
  public final void extractOte(final InputStream inputStream,
      final OutputStream outputStream) throws IOException, JDOMException {

    BufferedReader breader = new BufferedReader(new InputStreamReader(
        inputStream, "UTF-8"));
    BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
        outputStream, "UTF-8"));
    // read KAF document from inputstream
    KAFDocument kaf = KAFDocument.createFromStream(breader);
    // load parameters into a properties
    String model = parsedArguments.getString("model");
    String outputFormat = parsedArguments.getString("outputFormat");
    String clearFeatures = parsedArguments.getString("clearFeatures");
    // language parameter
    String lang = null;
    if (parsedArguments.getString("language") != null) {
      lang = parsedArguments.getString("language");
      if (!kaf.getLang().equalsIgnoreCase(lang)) {
        System.err
            .println("Language parameter in NAF and CLI do not match!!");
        System.exit(1);
      }
    } else {
      lang = kaf.getLang();
    }
    Properties properties = setOteProperties(model, lang, clearFeatures);
    KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor(
        "opinions", "ixa-pipe-nerc-" + Files.getNameWithoutExtension(model), version + "-" + commit);
    newLp.setBeginTimestamp();
    Annotate oteExtractor = new Annotate(properties);
    oteExtractor.annotateOTE(kaf);
    newLp.setEndTimestamp();
    String kafToString = null;
    if (outputFormat.equalsIgnoreCase("opennlp")) {
      kafToString = oteExtractor.annotateOTEsToOpenNLP(kaf);
    } else {
      kafToString = oteExtractor.annotateOTEsToKAF(kaf);
    }
    bwriter.write(kafToString);
    bwriter.close();
    breader.close();
  }

  
  /**
   * Set up the TCP socket for annotation.
   */
  public final void server() {

    // load parameters into a properties
    String port = parsedArguments.getString("port");
    String model = parsedArguments.getString("model");
    String lexer = parsedArguments.getString("lexer");
    String dictTag = parsedArguments.getString("dictTag");
    String dictPath = parsedArguments.getString("dictPath");
    String clearFeatures = parsedArguments.getString("clearFeatures");
    String outputFormat = parsedArguments.getString("outputFormat");
    // language parameter
    String lang = parsedArguments.getString("language");
    Properties serverproperties = setNameServerProperties(port, model, lang, lexer, dictTag, dictPath, clearFeatures, outputFormat);
    new OpinionTaggerServer(serverproperties);
  }
  
  /**
   * The client to query the TCP server for annotation.
   * 
   * @param inputStream
   *          the stdin
   * @param outputStream
   *          stdout
   */
  public final void client(final InputStream inputStream,
      final OutputStream outputStream) {

    String host = parsedArguments.getString("host");
    String port = parsedArguments.getString("port");
    try (Socket socketClient = new Socket(host, Integer.parseInt(port));
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
            System.in, "UTF-8"));
        BufferedWriter outToUser = new BufferedWriter(new OutputStreamWriter(
            System.out, "UTF-8"));
        BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(
            socketClient.getOutputStream(), "UTF-8"));
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
            socketClient.getInputStream(), "UTF-8"));) {

      // send data to server socket
      StringBuilder inText = new StringBuilder();
      String line;
      while ((line = inFromUser.readLine()) != null) {
        inText.append(line).append("\n");
      }
      inText.append("<ENDOFDOCUMENT>").append("\n");
      outToServer.write(inText.toString());
      outToServer.flush();
      
      // get data from server
      StringBuilder sb = new StringBuilder();
      String kafString;
      while ((kafString = inFromServer.readLine()) != null) {
        sb.append(kafString).append("\n");
      }
      outToUser.write(sb.toString());
    } catch (UnsupportedEncodingException e) {
      //this cannot happen but...
      throw new AssertionError("UTF-8 not supported");
    } catch (UnknownHostException e) {
      System.err.println("ERROR: Unknown hostname or IP address!");
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Port number not correct!");
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

 
  
  /**
   * Create the available parameters for Opinion Target Extraction.
   */
  private void loadOteParameters() {
    
    oteParser.addArgument("-m", "--model")
        .required(true)
        .help("Pass the model to do the tagging as a parameter.\n");
    oteParser.addArgument("--clearFeatures")
        .required(false)
        .choices("yes", "no", "docstart")
        .setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help("Reset the adaptive features every sentence; defaults to 'no'; if -DOCSTART- marks" +
                " are present, choose 'docstart'.\n");
    oteParser.addArgument("-l","--language")
        .required(false)
        .choices("en")
        .help("Choose language; it defaults to the language value in incoming NAF file.\n");
    oteParser.addArgument("-o","--outputFormat")
        .required(false)
        .choices("naf", "opennlp")
        .setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .help("Choose output format; it defaults to NAF.\n");
  }

  /**
   * Create the available parameters for NER tagging.
   */
  private void loadServerParameters() {
    
    serverParser.addArgument("-t","--task")
         .required(false)
         .choices("ner", "ote", "sst")
         .setDefault(Flags.DEFAULT_TASK)
         .help("Choose the type of sequence labeling task.\n");
    serverParser.addArgument("-p", "--port")
        .required(true)
        .help("Port to be assigned to the server.\n");
    serverParser.addArgument("-m", "--model")
        .required(true)
        .help("Pass the model to do the tagging as a parameter.\n");
    serverParser.addArgument("--clearFeatures")
        .required(false)
        .choices("yes", "no", "docstart")
        .setDefault(Flags.DEFAULT_FEATURE_FLAG)
        .help("Reset the adaptive features every sentence; defaults to 'no'; if -DOCSTART- marks" +
                " are present, choose 'docstart'.\n");
    serverParser.addArgument("-l","--language")
        .required(true)
        .choices("de", "en", "es", "eu", "it", "nl")
        .help("Choose language.\n");
    serverParser.addArgument("-o","--outputFormat")
        .required(false)
        .choices("conll03", "conll02", "naf", "opennlp")
        .setDefault(Flags.DEFAULT_OUTPUT_FORMAT)
        .help("Choose output format; it defaults to NAF.\n");
    serverParser.addArgument("--lexer")
        .choices("numeric")
        .setDefault(Flags.DEFAULT_LEXER)
        .required(false)
        .help("Use lexer rules for NERC tagging; it defaults to false.\n");
    serverParser.addArgument("--dictTag")
        .required(false)
        .choices("tag", "post")
        .setDefault(Flags.DEFAULT_DICT_OPTION)
        .help("Choose to directly tag entities by dictionary look-up; if the 'tag' option is chosen, " +
                "only tags entities found in the dictionary; if 'post' option is chosen, it will " +
                "post-process the results of the statistical model.\n");
    serverParser.addArgument("--dictPath")
        .required(false)
        .setDefault(Flags.DEFAULT_DICT_PATH)
        .help("Provide the path to the dictionaries for direct dictionary tagging; it ONLY WORKS if --dictTag " +
                "option is activated.\n");
  }
  
  private void loadClientParameters() {
    
    clientParser.addArgument("-p", "--port")
        .required(true)
        .help("Port of the TCP server.\n");
    clientParser.addArgument("--host")
        .required(false)
        .setDefault(Flags.DEFAULT_HOSTNAME)
        .help("Hostname or IP where the TCP server is running.\n");
  }  
  /**
   * Set a Properties object with the CLI parameters for Opinion Target Extraction.
   * @param model the model parameter
   * @param language language parameter
   * @param lexer rule based parameter
   * @param dictTag directly tag from a dictionary
   * @param dictPath directory to the dictionaries
   * @return the properties object
   */
  private Properties setOteProperties(String model, String language, String clearFeatures) {
    Properties oteProperties = new Properties();
    oteProperties.setProperty("model", model);
    oteProperties.setProperty("language", language);
    oteProperties.setProperty("clearFeatures", clearFeatures);
    return oteProperties;
  }
  
  
  private Properties setNameServerProperties(String port, String model, String language, String lexer, String dictTag, String dictPath, String clearFeatures, String outputFormat) {
    Properties serverProperties = new Properties();
    serverProperties.setProperty("port", port);
    serverProperties.setProperty("model", model);
    serverProperties.setProperty("language", language);
    serverProperties.setProperty("ruleBasedOption", lexer);
    serverProperties.setProperty("dictTag", dictTag);
    serverProperties.setProperty("dictPath", dictPath);
    serverProperties.setProperty("clearFeatures", clearFeatures);
    serverProperties.setProperty("outputFormat", outputFormat);
    return serverProperties;
  }

}
