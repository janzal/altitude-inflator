package org.cvut.bep;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class AltitudeInflator {
    private static Options mOptions = null;

    private static Options getOptions() {
        if (mOptions != null) return mOptions;
        mOptions = new Options();

        mOptions.addOption(Option.builder("f")
                .argName("FILE")
                .longOpt("file")
                .required()
                .hasArg()
                .desc("input file (waypoint file in MAVLink format)")
                .build());
        mOptions.addOption(Option.builder("o")
                .argName("FILE")
                .longOpt("output")
                .hasArg()
                .desc("output file")
                .build());
        mOptions.addOption(Option.builder("t")
                .argName("NUMBER")
                .longOpt("offset")
                .hasArg()
                .desc("altitude offset")
                .type(Double.class)
                .build());

        mOptions.addOption(Option.builder("s")
                .longOpt("download-tiles")
                .argName("PATH")
                .hasArg()
                .desc("just downloads tiles from Google Maps required by input file")
                .build());
        mOptions.addOption(Option.builder("e")
                .longOpt("elevation-provider")
                .argName("PROVIDER")
                .hasArg()
                .desc("elevation provider <gmaps|tiles>\ngmaps - provider uses direct calls to Google Maps API\ntiles - loads tiles from folder")
                .build());
        mOptions.addOption(Option.builder("l")
                .longOpt("tiles")
                .argName("PATH")
                .hasArg()
                .desc("when tiles elevation provider is used, it is necessary to provide path to resource folder")
                .build());
        mOptions.addOption(Option.builder("k")
                .argName("KEY")
                .longOpt("api-key")
                .hasArg()
                .desc("Google Apps API key")
                .build());
        mOptions.addOption("h", "help", false, "shows help");

        return mOptions;
    }

    private static void printHelp() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("altitude-inflator", options);
    }

    public static void main(String[] args) {
        String[] myArgs = new String[]{"--elevation-provider", "gmaps", "--offset", "2", "--api-key", "AIzaSyBxeAGp5yIfJQtigk3e7D0veyUpZgd8tBQ", "-f", "data/sample.waypoints"};

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(getOptions(), myArgs);

            if (line.hasOption('h')) {
                printHelp();
                return;
            }

            if (line.hasOption("download-tiles")) {
                System.out.println("Downloading tiles...");
            } else {
                AltitudeInflatorApp app = AltitudeInflatorApp.createFromArgs(line);
                app.run();
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp();
        } catch (AltitudeInflatorException e) {
            System.err.println(e.getMessage());
            printHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
