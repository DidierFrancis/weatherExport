package sun.helpers

import groovy.json.JsonOutput
import org.apache.commons.lang3.StringUtils

class Help {


    static checkJsonValid(String json) {
        try {
            JsonOutput.prettyPrint(json)
            return true
        } catch (ignored) {
            return false
        }
    }

    public static boolean isNumeric(String cs) {
        if (StringUtils.isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            char point = '.'
            char virgule = ','
            if (!cs.charAt(i).equals(point) && !cs.charAt(i).equals(virgule))
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
        }
        return true;
    }

}
