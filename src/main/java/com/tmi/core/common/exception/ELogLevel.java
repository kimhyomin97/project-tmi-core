package com.tmi.core.common.exception;


import org.slf4j.Logger;

public enum ELogLevel {

    INFO {
        @Override
        public void log(Logger log, String format, Object... args) {
            log.info(format, args);
        }
    },

    WARN {
        @Override
        public void log(Logger log, String format, Object... args) {
            log.warn(format, args);
        }
    },

    ERROR {
        @Override
        public void log(Logger log, String format, Object... args) {
            log.error(format, args);
        }
    };

    public abstract void log(Logger log, String format, Object... args);
}
