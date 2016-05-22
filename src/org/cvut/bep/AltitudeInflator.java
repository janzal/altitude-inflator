package org.cvut.bep;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class AltitudeInflator {
    public static final String APP_NAME = "altitude-inflator";
    public static final String VERSION = "v0.1.1-beta";

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
                .desc("just downloads tiles from Google Maps required by input file")
                .build());
        mOptions.addOption(Option.builder("p")
                .longOpt("tiles-folder")
                .hasArg()
                .argName("PATH")
                .desc("folder where are tiles are supposed to be stored")
                .build());
        mOptions.addOption(Option.builder()
                .longOpt("tiles-depth")
                .argName("BYTE")
                .hasArg()
                .desc("depth of quadtree (default value is " + InflatorTilesDownloader.DEFAULT_DEPTH + ")")
                .build());
        mOptions.addOption(Option.builder()
                .longOpt("tiles-samples-count")
                .argName("INT")
                .hasArg()
                .desc("samples count per tile (default value is " + InflatorTilesDownloader.DEFAULT_SAMPLES_COUNT + ")")
                .build());
        mOptions.addOption(Option.builder("e")
                .longOpt("elevation-provider")
                .argName("PROVIDER")
                .hasArg()
                .desc("elevation provider <gmaps|tiles>\ngmaps - provider uses direct calls to Google Maps API\ntiles - loads tiles from folder")
                .build());
        mOptions.addOption(Option.builder("l")
                .longOpt("tiles-temp")
                .argName("PATH")
                .hasArg()
                .desc("when tiles elevation provider is used, it is necessary to provide path to resource temp folder")
                .build());
        mOptions.addOption(Option.builder("k")
                .argName("KEY")
                .longOpt("api-key")
                .hasArgs()
                .desc("Google Apps API key (multiple keys can be used at once)")
                .build());
        mOptions.addOption("h", "help", false, "shows help");
        mOptions.addOption("v", "version", false, "shows version");

        return mOptions;
    }

    private static void printHelp() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(APP_NAME, options);
    }

    public static void main(String[] args) {
        String[] myArgs = new String[]{"--elevation-provider", "tiles", "--offset", "1000",
                "--api-key", "AIzaSyBxeAGp5yIfJQtigk3e7D0veyUpZgd8tBQ",
//                "--api-key", "TENHLE_NE_BIzaSyBxeAGp5yIfJQtigk3e7D0veyUpZgd8tBQ",
                "-f", "data/zidlov.waypoints",
//                "--download-tiles",
                "--tiles-folder", "./tiles/",
                "--tiles-samples-count", "20"
        };

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(getOptions(), myArgs);

            if (line.hasOption('v')) {
                System.out.println(APP_NAME + " " + VERSION);
                return;
            }

            if (line.hasOption('h')) {
                printHelp();
                return;
            }

            if (line.hasOption("download-tiles")) {
                InflatorTilesDownloader downloader = InflatorTilesDownloader.createFromArgs(line);
                downloader.run();
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
