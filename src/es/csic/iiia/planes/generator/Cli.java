/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2012, IIIA-CSIC, Artificial Intelligence Research Institute
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 *   Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 *   Neither the name of IIIA-CSIC, Artificial Intelligence Research Institute
 *   nor the names of its contributors may be used to
 *   endorse or promote products derived from this
 *   software without specific prior written permission of
 *   IIIA-CSIC, Artificial Intelligence Research Institute
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package es.csic.iiia.planes.generator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.commons.cli.*;

/**
 * Main class for the CLI interface.
 *
 * @author Marc Pujol <mpujol at iiia.csic.es>
 */
public class Cli {
    private static final String SETTINGS_FILE = "/es/csic/iiia/planes/generator/settings.properties";

    private static final Logger LOG = Logger.getLogger(Cli.class.getName());

    /**
     * List of available cli options.
     */
    private static Options options = new Options();

    /**
     * Generator's cli entry point.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        initializeLogging();

        options.addOption("d", "dump-settings", false, "dump the default settings to standard output. This can be used to prepare a settings file.");
        options.addOption("h", "help", false, "show this help message.");
        options.addOption(OptionBuilder.withArgName("setting=value")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription("override \"setting\" with \"value\".")
                //.withLongOpt("override")
                .create('o'));
        options.addOption("q", "quiet", false, "disable all output except for results and errors.");
        options.addOption(OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("Load settings from <file>.")
                .withLongOpt("settings")
                .create('s'));
        options.addOption(OptionBuilder.withLongOpt("dry-run")
                .withDescription("Output only the resolved settings, but do not run the simulation.")
                .create('t'));

        Configuration config = parseOptions(args);
        Generator g = new Generator(config);
        g.run();
    }

    private static void showHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("planes [options] <problem>", options);
        System.exit(1);
    }

    /**
     * Parse the provided list of arguments according to the program's options.
     *
     * @param in_args list of input arguments.
     * @return a configuration object set according to the input options.
     */
    private static Configuration parseOptions(String[] in_args) {
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        Properties settings = loadDefaultSettings();

        try {
            line = parser.parse(options, in_args);
        } catch (ParseException ex) {
            Logger.getLogger(Cli.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            showHelp();
        }

        if (line.hasOption('h')) {
            showHelp();
        }

        if (line.hasOption('d')) {
            dumpSettings();
        }

        if (line.hasOption('s')) {
            String fname = line.getOptionValue('s');
            try {
                settings.load(new FileReader(fname));
            }  catch (IOException ex) {
                throw new IllegalArgumentException("Unable to load the settings file \"" + fname + "\"");
            }
        }

        // Apply overrides
        settings.setProperty("quiet", String.valueOf(line.hasOption('q')));
        Properties overrides = line.getOptionProperties("o");
        settings.putAll(overrides);

        String[] args = line.getArgs();
        if (args.length < 1) {
            showHelp();
        }
        settings.setProperty("problem", args[0]);

        Configuration c = new Configuration(settings);

        if (line.hasOption('t')) {
            System.exit(0);
        }
        return c;
    }

    /**
     * Initializes the logging system.
     */
    private static void initializeLogging() {
        try {
            // Load logging configuration
            LogManager.getLogManager().readConfiguration(
                    Cli.class.getResourceAsStream("/logging.properties"));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Loads the default settings.
     */
    private static Properties loadDefaultSettings() {
        Properties settings = new Properties();
        try {
            InputStream is = Cli.class.getResourceAsStream(SETTINGS_FILE);
            if (is == null) {
                throw new RuntimeException("Unable to locate default settings file.");
            }
            settings.load(is);
        } catch (IOException ex) {
            Logger.getLogger(Cli.class.getName()).log(Level.SEVERE, null, ex);
        }
        return settings;
    }

    /**
     * Dumps the default settings to standard output and exits.
     */
    private static void dumpSettings() {
        BufferedReader is = new BufferedReader(new InputStreamReader(
            Cli.class.getResourceAsStream(SETTINGS_FILE)
        ));
        try {
            for (String line=is.readLine(); line != null; line=is.readLine()) {
                System.out.println(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(Cli.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.exit(0);
    }
}