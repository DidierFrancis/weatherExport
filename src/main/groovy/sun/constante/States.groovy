package sun.constante

import org.springframework.lang.Nullable

enum States {
    BLOCKED("bloquer"),
    UNBLOCKED("debloquer")

    String value

    States(String value) {
        this.value = value
    }

    @Nullable
    static States resolve(String value) {
        for (States status : values()) {
            if (status.value == value) {
                return status
            }
        }
        return null
    }
}
