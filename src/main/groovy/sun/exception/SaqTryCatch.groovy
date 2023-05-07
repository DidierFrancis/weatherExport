package sun.exception


import org.apache.commons.lang3.StringUtils
import org.grails.datastore.gorm.GormEntity
import sun.constante.TypeException

/**
 * @author <a href="https://github.com/6ccattleya">Bamba CISSE</a>
 */
class SaqTryCatch {

/**
 * @param entity l'entité à vérifier
 * @param typeException le type d'exception à vérifier
 * @param customMessage le message personnalisé
 * @return l'objet à verifier ou L'exception
 */
    static def check(GormEntity entity, TypeException typeException, String customMessage = "") {
        if (StringUtils.isEmpty(customMessage)) {
            customMessage = typeException.defaultMessage
        }
        if (entity == null && typeException != TypeException.UNIQUE) {
            throw new SaqException(TypeException.NULL, customMessage)
        }
        switch (typeException) {
            case TypeException.SAVE:
                entity.validate()
                if (entity.hasErrors()) {
                    println("errors": ExceptionUtils.errorsAsTabList(entity.errors).first())
                    throw new SaqException(TypeException.CONTRAINTE, entity.errors, entity, customMessage)
                } else {
                    try {
                        if (entity.save(flush: true) == null) {
                            throw new SaqException(TypeException.SAVE, customMessage)
                        }
                    } catch (Exception e) {
                        e.printStackTrace()
                        throw new SaqException(TypeException.SAVE, e, entity, customMessage)
                    }
                }
                return entity
                break
            case TypeException.UPDATE:
                entity.validate()
                if (entity.hasErrors()) {
                    throw new SaqException(TypeException.CONTRAINTE, entity.errors, entity, customMessage)
                } else {
                    try {
                        if (entity.save() == null) {
                            throw new SaqException(TypeException.UPDATE, customMessage)
                        }
                    } catch (Exception e) {
                        throw new SaqException(TypeException.UPDATE, e, entity, customMessage)
                    }
                }
                return entity
                break
            case TypeException.DELETE:
                entity.validate()
                if (entity.hasErrors()) {
                    throw new SaqException(TypeException.CONTRAINTE, entity.errors, entity, customMessage)
                } else {
                    try {
                        entity.delete()
                    } catch (Exception e) {
                        throw new SaqException(TypeException.DELETE, e, entity, customMessage)
                    }
                }
                break
            case TypeException.FIND:
                entity.validate()
                if (entity.hasErrors()) {
                    throw new SaqException(TypeException.NOTFOUND, entity.errors, entity, customMessage)
                } else {
                    if (entity == null) {
                        throw new SaqException(TypeException.NOTFOUND, customMessage)
                    }
                    return entity
                }
                break

            case TypeException.UNIQUE:
                if (entity != null) {
                    throw new SaqException(TypeException.UNIQUE, customMessage)
                }
                return null

                break
        }
    }
}
