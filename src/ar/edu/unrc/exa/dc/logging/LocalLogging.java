package ar.edu.unrc.exa.dc.logging;

import ar.edu.unrc.exa.dc.icebar.properties.ICEBARProperties.IcebarLoggingLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.*;

public class LocalLogging {

    public static Logger getLogger(Class<?> forClass) {return getLogger(forClass, IcebarLoggingLevel.FINE); }

    public static Logger getLogger(Class<?> forClass, IcebarLoggingLevel loggingLevel) {
        return getLogger(forClass, loggingLevel, loggingLevel);
    }

    public static Logger getLogger(Class<?> forClass, IcebarLoggingLevel consoleLoggingLevel, IcebarLoggingLevel fileLoggingLevel) {
        Logger logger = Logger.getLogger(forClass.getName());
        final Path logsFolder = Paths.get("", "logs");
        try{
            Files.createDirectories(logsFolder);
            Handler[] handlers = logger.getHandlers();
            Arrays.stream(handlers).forEach(logger::removeHandler);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            FileHandler fileHandler = new FileHandler(Paths.get(logsFolder.toString(), forClass.getName() +  ".log").toString());
            fileHandler.setLevel(fileLoggingLevel.getLoggerLevel());
            consoleHandler.setLevel(consoleLoggingLevel.getLoggerLevel());
            logger.addHandler(consoleHandler);
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        } catch(IOException exception){
            logger.log(Level.SEVERE, "Error occur in FileHandler.", exception);
        }
        return logger;
    }

    @Deprecated
    public static class CoolConsoleHandler extends StreamHandler {

        private final ConsoleHandler stderrHandler = new ConsoleHandler();

        public CoolConsoleHandler() {
            super(System.out, new SimpleFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() == Level.SEVERE.intValue()) {
                stderrHandler.publish(record);
                stderrHandler.flush();
            }
            if (record.getLevel().intValue() == Level.OFF.intValue()) {
                return;
            }
            if (record.getLevel().intValue() <= Level.INFO.intValue()) {
                super.publish(record);
                super.flush();
            } else {
                stderrHandler.publish(record);
                stderrHandler.flush();
            }
        }

    }

}
