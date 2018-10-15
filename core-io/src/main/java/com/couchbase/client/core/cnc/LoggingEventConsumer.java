/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.core.cnc;

import com.couchbase.client.core.annotation.Stability;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 * Consumes {@link Event Events} and logs them per configuration.
 *
 * <p>This consumer is intended to be attached per default and performs convenient logging
 * throughout the system. It tries to detect settings and loggers in a best-effort
 * way but can always be swapped out or changed to implement custom functionality.</p>
 *
 * <p>If SLF4J is detected on the classpath it will be used, otherwise it will fall back to
 * java.util.logging or the console depending on the configuration.</p>
 */
public class LoggingEventConsumer implements Consumer<Event> {

  /**
   * Contains true if SLF4J is on the classpath, false otherwise.
   */
  private static final boolean SLF4J_AVAILABLE = slf4JOnClasspath();

  /**
   * Contains the selected logger that should be used for logging.
   */
  private final Logger logger;

  /**
   * Returns a {@link Builder} that allows to customize a {@link LoggingEventConsumer}.
   *
   * @return the builder to customize.
   */
  public static LoggingEventConsumer.Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new {@link LoggingEventConsumer} with all defaults.
   *
   * @return a {@link LoggingEventConsumer}.
   */
  public static LoggingEventConsumer create() {
    return builder().build();
  }

  private LoggingEventConsumer(final Builder builder) {
    String name = builder.loggerName;

    if (builder.customLogger != null) {
      logger = builder.customLogger;
    } else if (SLF4J_AVAILABLE && !builder.disableSlf4J) {
      logger = new Slf4JLogger(name);
    } else if (builder.fallbackToConsole) {
      logger = new ConsoleLogger(name);
    } else {
      logger = new JdkLogger(name);
    }
  }

  @Override
  public void accept(final Event event) {
    StringBuilder logLineBuilder = new StringBuilder();

    logLineBuilder.append("[").append(event.category()).append("]");
    logLineBuilder.append("[").append(event.getClass().getSimpleName()).append("]");

    if (!event.duration().isZero()) {
      logLineBuilder
        .append("[")
        .append(TimeUnit.NANOSECONDS.toMicros(event.duration().toNanos()))
        .append("µs]");
    }

    if (event.description() != null) {
      logLineBuilder.append(" ").append(event.description());
    }

    if (event.context() != null) {
      logLineBuilder.append(" ").append(event.context().exportAsString(Context.ExportFormat.JSON));
    }

    String logLine = logLineBuilder.toString();
    switch (event.severity()) {
      case VERBOSE:
        logger.trace(logLine);
        break;
      case DEBUG:
        logger.debug(logLine);
        break;
      case INFO:
        logger.info(logLine);
        break;
      case WARN:
        logger.warn(logLine);
        break;
      case ERROR:
      default:
        logger.error(logLine);
    }
  }

  /**
   * Helper method to check if SLF4J is on the classpath.
   */
  private static boolean slf4JOnClasspath() {
    try {
      Class.forName("org.slf4j.Logger");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  static class Builder {

    private Logger customLogger;
    private boolean fallbackToConsole;
    private boolean disableSlf4J;
    private String loggerName;

    Builder() {
      fallbackToConsole = false;
      disableSlf4J = false;
      loggerName = "CouchbaseLogger";
      customLogger = null;
    }

    /**
     * Allows to specify a custom logger. This is used for testing only.
     *
     * @param customLogger the custom logger
     * @return the Builder for chaining purposes
     */
    @Stability.Internal
    Builder customLogger(Logger customLogger) {
      this.customLogger = customLogger;
      return this;
    }

    public Builder fallbackToConsole(boolean fallbackToConsole) {
      this.fallbackToConsole = fallbackToConsole;
      return this;
    }

    public Builder disableSlf4J(boolean disableSlf4J) {
      this.disableSlf4J = disableSlf4J;
      return this;
    }

    public Builder loggerName(String loggerName) {
      this.loggerName = loggerName;
      return this;
    }

    public LoggingEventConsumer build() {
      return new LoggingEventConsumer(this);
    }
  }

  /**
   * Generic logger interface.
   */
  @Stability.Internal
  interface Logger {

    /**
     * Return the name of this <code>Logger</code> instance.
     *
     * @return name of this logger instance
     */
    String getName();

    /**
     * Is the logger instance enabled for the TRACE level.
     *
     * @return True if this Logger is enabled for the TRACE level,
     *         false otherwise.
     */
    boolean isTraceEnabled();

    /**
     * Log a message at the TRACE level.
     *
     * @param msg the message string to be logged
     */
    void trace(String msg);

    /**
     * Log a message at the TRACE level according to the specified format
     * and arguments.
     *
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the TRACE level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for TRACE.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    void trace(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the TRACE level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void trace(String msg, Throwable t);

    /**
     * Is the logger instance enabled for the DEBUG level.
     *
     * @return True if this Logger is enabled for the DEBUG level,
     *         false otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    void debug(String msg);

    /**
     * Log a message at the DEBUG level according to the specified format
     * and arguments.
     *
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the DEBUG level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for DEBUG. </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    void debug(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the DEBUG level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void debug(String msg, Throwable t);

    /**
     * Is the logger instance enabled for the INFO level.
     *
     * @return True if this Logger is enabled for the INFO level,
     *         false otherwise.
     */
    boolean isInfoEnabled();

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    void info(String msg);

    /**
     * Log a message at the INFO level according to the specified format
     * and arguments.
     *
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the INFO level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for INFO. </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    void info(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the INFO level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void info(String msg, Throwable t);

    /**
     * Is the logger instance enabled for the WARN level.
     *
     * @return True if this Logger is enabled for the WARN level,
     *         false otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    void warn(String msg);

    /**
     * Log a message at the WARN level according to the specified format
     * and arguments.
     *
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the WARN level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for WARN. </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    void warn(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the WARN level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void warn(String msg, Throwable t);

    /**
     * Is the logger instance enabled for the ERROR level.
     *
     * @return True if this Logger is enabled for the ERROR level,
     *         false otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    void error(String msg);

    /**
     * Log a message at the ERROR level according to the specified format
     * and arguments.
     *
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the ERROR level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for ERROR. </p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    void error(String format, Object... arguments);

    /**
     * Log an exception (throwable) at the ERROR level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    void error(String msg, Throwable t);
  }

  static class Slf4JLogger implements Logger {

    private final org.slf4j.Logger logger;

    Slf4JLogger(final String name) {
      logger = org.slf4j.LoggerFactory.getLogger(name);
    }

    @Override
    public String getName() {
      return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
      return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
      logger.trace(msg);
    }

    @Override
    public void trace(String format, Object... arguments) {
      logger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
      logger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
      return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
      logger.debug(msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
      logger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
      logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
      return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
      logger.info(msg);
    }

    @Override
    public void info(String format, Object... arguments) {
      logger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
      logger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
      return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
      logger.warn(msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
      logger.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
      logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
      return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
      logger.error(msg);
    }

    @Override
    public void error(String format, Object... arguments) {
      logger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
      logger.error(msg, t);
    }
  }

  static class JdkLogger implements Logger {

    private final java.util.logging.Logger logger;

    JdkLogger(String name) {
      this.logger = java.util.logging.Logger.getLogger(name);
    }

    @Override
    public String getName() {
      return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
      return logger.isLoggable(Level.FINEST);
    }

    @Override
    public void trace(String msg) {
      logger.log(Level.FINEST, msg);
    }

    @Override
    public void trace(String format, Object... arguments) {
      logger.log(Level.FINEST, formatHelper(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
      logger.log(Level.FINEST, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
      return logger.isLoggable(Level.FINE);
    }

    @Override
    public void debug(String msg) {
      logger.log(Level.FINE, msg);
    }

    @Override
    public void debug(String format, Object... arguments) {
      logger.log(Level.FINE, formatHelper(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
      logger.log(Level.FINE, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
      return logger.isLoggable(Level.INFO);
    }

    @Override
    public void info(String msg) {
      logger.log(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object... arguments) {
      logger.log(Level.INFO, formatHelper(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
      logger.log(Level.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
      return logger.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String msg) {
      logger.log(Level.WARNING, msg);
    }

    @Override
    public void warn(String format, Object... arguments) {
      logger.log(Level.WARNING, formatHelper(format, arguments));
    }

    @Override
    public void warn(String msg, Throwable t) {
      logger.log(Level.WARNING, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
      return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public void error(String msg) {
      logger.log(Level.SEVERE, msg);
    }

    @Override
    public void error(String format, Object... arguments) {
      logger.log(Level.SEVERE, formatHelper(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
      logger.log(Level.SEVERE, msg, t);
    }

  }

  static class ConsoleLogger implements Logger {

    private final String name;
    private final PrintStream err;
    private final PrintStream log;

    ConsoleLogger(String name) {
      this.name = name;
      this.log = System.out;
      this.err = System.err;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public boolean isTraceEnabled() {
      return true;
    }

    @Override
    public synchronized void trace(String msg) {
      this.log.format("[TRACE] (%s) %s\n", Thread.currentThread().getName(), msg);
    }

    @Override
    public synchronized void trace(String format, Object... arguments) {
      this.log.format("[TRACE] (%s) %s\n", Thread.currentThread().getName(),
        formatHelper(format, arguments));
    }

    @Override
    public synchronized void trace(String msg, Throwable t) {
      this.log.format("[TRACE] (%s) %s - %s\n", Thread.currentThread().getName(), msg, t);
      t.printStackTrace(this.log);
    }

    @Override
    public boolean isDebugEnabled() {
      return true;
    }

    @Override
    public synchronized void debug(String msg) {
      this.log.format("[DEBUG] (%s) %s\n", Thread.currentThread().getName(), msg);
    }

    @Override
    public synchronized void debug(String format, Object... arguments) {
      this.log.format("[DEBUG] (%s) %s\n", Thread.currentThread().getName(),
        formatHelper(format, arguments));
    }

    @Override
    public synchronized void debug(String msg, Throwable t) {
      this.log.format("[DEBUG] (%s) %s - %s\n", Thread.currentThread().getName(), msg, t);
      t.printStackTrace(this.log);
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public synchronized void info(String msg) {
      this.log.format("[ INFO] (%s) %s\n", Thread.currentThread().getName(), msg);
    }

    @Override
    public synchronized void info(String format, Object... arguments) {
      this.log.format("[ INFO] (%s) %s\n", Thread.currentThread().getName(),
        formatHelper(format, arguments));
    }

    @Override
    public synchronized void info(String msg, Throwable t) {
      this.log.format("[ INFO] (%s) %s - %s\n", Thread.currentThread().getName(), msg, t);
      t.printStackTrace(this.log);
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public synchronized void warn(String msg) {
      this.err.format("[ WARN] (%s) %s\n", Thread.currentThread().getName(), msg);
    }

    @Override
    public synchronized void warn(String format, Object... arguments) {
      this.err.format("[ WARN] (%s) %s\n", Thread.currentThread().getName(),
        formatHelper(format, arguments));
    }

    @Override
    public synchronized void warn(String msg, Throwable t) {
      this.err.format("[ WARN] (%s) %s - %s\n", Thread.currentThread().getName(), msg, t);
      t.printStackTrace(this.err);
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public synchronized void error(String msg) {
      this.err.format("[ERROR] (%s) %s\n", Thread.currentThread().getName(), msg);
    }

    @Override
    public synchronized void error(String format, Object... arguments) {
      this.err.format("[ERROR] (%s) %s\n", Thread.currentThread().getName(),
        formatHelper(format, arguments));
    }

    @Override
    public synchronized void error(String msg, Throwable t) {
      this.err.format("[ERROR] (%s) %s - %s\n", Thread.currentThread().getName(), msg, t);
      t.printStackTrace(this.err);
    }
  }

  /**
   * Helper method to compute the formatted arguments.
   *
   * @param from      the original string
   * @param arguments the arguments to replace
   * @return the formatted string.
   */
  private static String formatHelper(final String from, final Object... arguments) {
    if (from != null) {
      String computed = from;
      if (arguments != null && arguments.length != 0) {
        for (Object argument : arguments) {
          computed = computed.replaceFirst(
            "\\{\\}", Matcher.quoteReplacement(argument.toString())
          );
        }
      }
      return computed;
    }
    return null;
  }

}