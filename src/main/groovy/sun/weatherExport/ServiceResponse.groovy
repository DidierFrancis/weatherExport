/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sun.weatherExport

import sun.exception.SaqException

class ServiceResponse {

    Boolean serviceOk = false

    String message // message for the application
    String debugMessage // message for debug purposes
    LinkedHashMap linkedHashMapMessage
    String codeMessage
    def objetInstance
    def tabInstance = []
    Integer valeurNumerique1
    Integer valeurNumerique2
    Integer valeurNumerique3

    // from Cynod
    def obj1
    def obj2
    def obj3
    def obj4
    def obj5
    def messageProp = new HashMap()
    Map<?, ?> mapInstance


    // Pour la gestion des erreurs
    SaqException saqErrors
    List saqErrorArgs = null


}


