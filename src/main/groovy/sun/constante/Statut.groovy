package sun.constante

import org.springframework.lang.Nullable

enum Statut {
    SUCCESS("SUCCESS"),
    FAILED("FAILED")

    String value

    Statut(String value) {
        this.value = value
    }

    @Nullable
    static Statut resolve(String value) {
        for (Statut status : values()) {
            if (status.value == value) {
                return status
            }
        }
        return null
    }
}
