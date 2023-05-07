package sun.exception

import grails.converters.JSON
import grails.util.Holders
import org.springframework.context.MessageSource
import org.springframework.validation.Errors

class ExceptionUtils {

    static MessageSource messageSource = (MessageSource) Holders.grailsApplication.mainContext.getBean("messageSource")

    static List<String> errorsAsTabList(Errors errors) {
        if (errors == null) return []
        List<String> errorsString = new ArrayList<>()
        errors.allErrors.each {
            errorsString.add(messageSource.getMessage(it, Locale.FRANCE))
        }
        return errorsString
    }


    static def asJson(SaqException saqException) {
        [
                code        : saqException.code,
                message     : saqException?.message,
                debugMessage: saqException.debugMessage,
                erreurs     : errorsAsTabList(saqException.errors)
        ] as JSON
    }
}
