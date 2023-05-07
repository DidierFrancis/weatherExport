import ch.qos.logback.classic.boolex.GEventEvaluator
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.util.FileSize
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import java.nio.charset.StandardCharsets
import ch.qos.logback.core.filter.EvaluatorFilter
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.core.spi.FilterReply.DENY
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

def HOME_DIR = "/MARINELEC/logs/"
// See http://logback.qos.ch/manual/groovy.html for details on configuration
//appender('STDOUT', ConsoleAppender) {
//    encoder(PatternLayoutEncoder) {
//        charset = StandardCharsets.UTF_8
//
//        pattern =
//                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
//                        '%clr(%5p) ' + // Log level
//                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
//                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
//                        '%m%n%wex' // Message
//    }
//}
//
appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss}  %level %logger - %msg%n"
        charset = StandardCharsets.UTF_8
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "${HOME_DIR}/log-%d{yyyy-MM-dd_HH}.log"
        maxHistory = 30
        totalSizeCap = FileSize.valueOf("2GB")
    }
    filter(EvaluatorFilter) {
        evaluator(GEventEvaluator) {
            expression = "!(e.getLoggerName().contains('org.hibernate.orm.deprecation'))"
        }
        onMismatch = DENY
        onMatch = NEUTRAL
    }
    logger 'sun.*', INFO, ['ROLLING'], false
    logger 'sun.*', ERROR, ['ROLLING'], false

}
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = StandardCharsets.UTF_8

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
        filter(EvaluatorFilter) {
        evaluator(GEventEvaluator) {
            expression = "!(e.getLoggerName().contains('org.hibernate.orm.deprecation'))"
        }
        onMismatch = DENY
        onMatch = NEUTRAL
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            charset = StandardCharsets.UTF_8
            pattern = "%level %logger - %msg%n"
        }
    }

    logger '*', DEBUG, ['STDOUT'], false
    logger '*', ERROR, ['STDOUT'], false
    logger '*', INFO, ['STDOUT'], false

}
//
root(ERROR, ['ROLLING'])
root(INFO, ['ROLLING'])

root(ERROR, [ 'STDOUT'])
root(INFO, ['STDOUT'])