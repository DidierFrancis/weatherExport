package sun.constante

import org.springframework.lang.Nullable

enum TypeException {
    SAVE("1000", "Erreur lors de l'enregistrement de l'objet"),
    CONTRAINTE("4000", "Erreur de validation"),
    UPDATE("1010", "Erreur lors de la modification"),
    DELETE("1020", "Erreur lors de la suppression"),
    FIND("1030", "Erreur lors de la recupération de l'objet"),
    NOTFOUND("1100", "object introuvable"),
    NULL("4400", "L'entité à tester est nulle"),
    VALIDATE("4500", "L'entité n'est pas validé"),
    UNIQUE("4599", "L'entité doit etre unique!")

    String code
    String defaultMessage

    TypeException(String code, String defaultMessage) {
        this.code = code
        this.defaultMessage = defaultMessage
    }

    @Nullable
    static TypeException resolve(String code) {
        for (TypeException status : values()) {
            if (status.code == code) {
                return status
            }
        }
        return null
    }
}
