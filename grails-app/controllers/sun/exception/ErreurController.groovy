package sun.exception

import grails.converters.JSON


class ErreurController {
    def index() {
        if (request.exception == null) {
            render([
                    responseCode: 500,
                    cause       : [
                            code   : 5000,
                            message: "Une erreur inconnue est survenue lors du traitement"
                    ]
            ] as JSON)
            return
        }
        def cause = request.exception.cause
        if (cause == null) {
            render([
                    responseCode: 500,
                    cause       : [
                            code   : 5000,
                            message: "Une erreur inconnue est survenue lors du traitement"
                    ]
            ] as JSON)
            return
        }
        def exception = cause.cause
        if (cause instanceof SaqException || cause.getClass().isAssignableFrom(SaqException)) {
            render model: [saqException: cause], view: 'index'
        } else {
            if (exception instanceof SaqException || exception.getClass().isAssignableFrom(SaqException)) {
                render model: [saqException: exception], view: 'index'
            } else {
                render([
                        responseCode: 500,
                        cause       : [
                                code   : 5000,
                                errors : exception?.message,
                                message: "Une erreur inconnue est survenue lors du traitement, veuillez verifier les donnees saisies",
                        ]
                ] as JSON)
            }
        }
    }
}