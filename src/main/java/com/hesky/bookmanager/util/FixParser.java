package com.hesky.bookmanager.util;

import org.slf4j.Logger;
import quickfix.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Parses fix messages from string
 */
public class FixParser {
    private static final Logger LOG = getLogger(FixParser.class);
    private static DataDictionary DICTIONARY;

    static {

        try {
            DICTIONARY = new DataDictionary(FixParser.class.getClassLoader().getResourceAsStream("FIX44.xml"));
        }
        catch (ConfigError configError) {
            LOG.error("Error has occurred when dictionary was init", configError);
        }
    }

    /**
     * @param message string representation of fix message
     * @return fix message or null if {@code message} is not in an appropriate format or not valid
     */
    public static Message parse(String message) {
        Message msg = null;
        try {
            msg = MessageUtils.parse(new DefaultMessageFactory(), DICTIONARY, message);
        }
        catch (InvalidMessage invalidMessage) {
            LOG.error("Invalid message " + message, invalidMessage);
        }
        return msg;
    }
}
