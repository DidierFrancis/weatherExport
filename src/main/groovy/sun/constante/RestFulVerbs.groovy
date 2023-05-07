package sun.constante

import org.springframework.lang.Nullable

enum RestFulVerbs {
    SAVE("save","creation"),
    UPDATE("update", "modification"),
    DELETE("delete", "suppression"),
    SHOW("show", "afficher"),
    LIST("list", "Lister")

    String value
    String description

    RestFulVerbs(String value, String description) {
        this.value = value
        this.description = description
    }

    @Nullable
    static RestFulVerbs resolve(String value) {
        for (RestFulVerbs status : values()) {
            if (status.value == value) {
                return status
            }
        }
        return null
    }
}
