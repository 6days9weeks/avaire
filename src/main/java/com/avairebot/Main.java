package com.avairebot;

import ch.qos.logback.classic.util.ContextInitializer;
import com.avairebot.chat.ConsoleColor;
import com.avairebot.exceptions.InvalidApplicationEnvironmentException;
import com.avairebot.shared.ExitCodes;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, SQLException, InvalidApplicationEnvironmentException {
        Options options = new Options();

        options.addOption(new Option("h", "help", false, "Displays this help menu."));
        options.addOption(new Option("v", "version", false, "Displays the current version of the application."));
        options.addOption(new Option("sc", "shard-count", true, "Sets the amount of shards the bot should start up."));
        options.addOption(new Option("nocolor", "no-colors", false, "Disables colors for commands and AI actions in the terminal."));

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("Help Menu", options);
                System.exit(ExitCodes.EXIT_CODE_NORMAL);
            } else if (cmd.hasOption("version")) {
                System.out.println(AvaIre.getVersionInfo());
                System.exit(ExitCodes.EXIT_CODE_NORMAL);
            }

            Settings settings = new Settings(cmd);

            ConsoleColor.setSettings(settings);
            if (!settings.useColors()) {
                System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback_nocolor.xml");
            }

            new AvaIre(settings);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("", options);

            System.exit(ExitCodes.EXIT_CODE_NORMAL);
        }
    }
}
