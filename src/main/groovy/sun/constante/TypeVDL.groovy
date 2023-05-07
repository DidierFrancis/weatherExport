package sun.constante

import org.springframework.lang.Nullable

enum TypeVDL {
    BASE("BASE"),
    MASTER("MASTER"),
    CUSTOMER("CUSTOMER"),
    SITE("SITE"),
    VOYAGE("VOYAGE"),
    DOCKING("DOCKING"),
    TREND("TREND")

    String value

    TypeVDL(String value) {
        this.value = value
    }

    @Nullable
    static TypeVDL resolve(String value) {
        for (TypeVDL typeVDL : values()) {
            if (typeVDL.value == value) {
                return typeVDL
            }
        }
        return null
    }
}