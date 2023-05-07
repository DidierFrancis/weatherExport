package sun.wheatherExport

import grails.artefact.Artefact
import grails.converters.JSON
import grails.core.GrailsApplication
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.util.GrailsNameUtils
import grails.web.http.HttpHeaders
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils
import org.apache.commons.validator.GenericValidator
import org.grails.datastore.gorm.GormEntity
import org.hibernate.FetchMode
import org.hibernate.criterion.CriteriaSpecification
import sun.constante.RestFulVerbs
import sun.constante.TypeException
import sun.exception.SaqException
import sun.exception.SaqTryCatch
import sun.helpers.Help

import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit

import static org.springframework.http.HttpStatus.*

@Artefact("Controller")
@ReadOnly
class RestfulController<T> {
    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: ["PUT", "POST"], patch: "PATCH", delete: "DELETE", search: "POST"]
    def springSecurityService
    Class<T> resource
    String resourceName
    String resourceClassName
    GrailsApplication grailsApplication
    boolean readOnly
    boolean isCommand

    RestfulController(Class<T> resource) {
        this(resource, false, false)
    }

    RestfulController(Class<T> resource, boolean readOnly, boolean isCommand) {
        this.resource = resource
        this.readOnly = readOnly
        this.isCommand = isCommand
        resourceClassName = isCommand ? toEntityResource().simpleName : resource.simpleName
        resourceName = GrailsNameUtils.getPropertyName(isCommand ? toEntityResource() : resource)
    }

    RestfulController(Class<T> resource, boolean readOnly) {
        this.resource = resource
        this.readOnly = readOnly
        this.isCommand = false
        resourceClassName = isCommand ? toEntityResource().simpleName : resource.simpleName
        resourceName = GrailsNameUtils.getPropertyName(isCommand ? toEntityResource() : resource)
    }

    /**
     * Lists all resources up to the given maximum
     *
     * @param max The maximum
     * @return A list of resources
     */
    def index(Integer max, Integer offset, String sort, String order) {
        params.max = max ?: 100
        params.offset = offset ?: 0
        params.sort = sort ?: 'createdAt'
        params.order = order in ['asc', 'desc'] ? order : 'desc'
        def search = params.search
        def isGlobal = true
        def searchQuery = null
        if (!StringUtils.isEmpty(params.searchQuery)) {
            params.isGlobal = params.isGlobal.equalsIgnoreCase('true')
            isGlobal = params.isGlobal
            searchQuery = params.searchQuery
        }
        if (params.searchQuery == null) {
            def list = listAllResources(params)
            respond list, model: [
                    ("${resourceName}List".toString()): list,
                    total                             : countResources(),
                    max                               : params.max,
                    offset                            : params.offset ?: 0,
                    order                             : params.order,
                    sort                              : params.sort,
                    search                            : searchQuery]

        } else {

            try {
                def list = listSearchResources(params) ?: []
                respond list, model: [
                        ("${resourceName}List".toString()): list,
                        search                            : searchQuery,
                        total                             : list.size(),
                        max                               : params.max,
                        offset                            : params.offset ?: 0,
                        sort                              : params.sort,
                        order                             : params.order
                ]
            } catch (Exception ex) {
                render model: [errors: [ex.message]], template: '/errors/custom'
            }
        }

    }

    /**
     * Lists query resources up to the given maximum
     *
     * @param max The maximum
     * @return A list of resources
     */

    /**
     * Shows a single resource
     * @param id The id of the resource
     * @return The rendered resource or a 404 if it doesn't exist
     */
    def show() {
        respond queryForResource(params.id)
    }

    /**
     * Displays a form to create a new resource
     */
    def create() {
        if (handleReadOnly()) {
            return
        }
        respond createResource()
    }

    /**
     * Saves a resource
     */
    @Transactional
    def save() {
        if (handleReadOnly()) {
            return
        }
        def instance = createResource()

        setConnectedUser(instance)

        println("perform before save operations")
        toDoBefore(instance, RestFulVerbs.SAVE, instance)
        println("instance": instance.properties)
        instance = saveResource instance

        println("perform after save operations")
        toDoAfter instance, RestFulVerbs.SAVE

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [classMessageArg, instance.id])
                redirect instance
            }
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                        grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: CREATED, view: 'show']
            }
        }
    }

    def edit() {
        if (handleReadOnly()) {
            return
        }
        respond queryForResource(params.id)
    }

    /**
     * Updates a resource for the given id
     * @param id
     */
    @Transactional
    def patch() {
        update()
    }

    /**
     * Updates a resource for the given id
     * @param id
     */
    @Transactional
    def update() {
        if (handleReadOnly()) {
            return
        }

        T instance = queryForResource(params.id)
        T newObj = queryForResource(params.id)

        setConnectedUser(instance)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        newObj.properties = getObjectToBind()
        println("perform after update operations")
        toDoBefore(instance, RestFulVerbs.UPDATE, newObj)
        instance.properties = getObjectToBind()
        instance.validate()

        updateResource instance
        toDoAfter instance, RestFulVerbs.UPDATE
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [classMessageArg, instance.id])
                redirect instance
            }
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                        grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: OK, view: 'show', message: 'Entite modifie avec succes']

            }
        }
    }
    /**
     * Deletes a resource for the given id
     * @param id The id
     */
    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        deleteResource instance

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [classMessageArg, instance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT, message: 'Entite supprime avec succes' } // NO CONTENT STATUS CODE
        }
    }

    /**
     * handles the request for write methods (create, edit, update, save, delete) when controller is in read only mode
     *
     * @return true if controller is read only
     */
    protected boolean handleReadOnly() {
        if (readOnly) {
            render status: METHOD_NOT_ALLOWED.value()
            return true
        } else {
            return false
        }
    }


    protected getObjectToBind() {
        return request
    }


    /**
     * Queries for a resource for the given id
     *
     * @param id The id
     * @return The resource or null if it doesn't exist
     */
    protected T queryForResource(Serializable id) {
        def obj = isCommand ? toGormEntity(resource.newInstance(), id) : resource.get(id)
        if (obj == null) {
            notFoundResource()
            return
        }
        return obj
    }

    /**
     * Creates a new instance of the resource for the given parameters
     *
     * @param params The parameters
     * @return The resource instance
     */
    protected T createResource(Map params) {
        resource.newInstance(params)
    }

    /**
     * Creates a new instance of the resource.  If the request
     * contains a body the body will be parsed and used to
     * initialize the new instance, otherwise request parameters
     * will be used to initialized the new instance.
     *
     * @return The resource instance
     */
    protected T createResource() {
        T instance = resource.newInstance()
        bindData instance, getObjectToBind()
        return instance
    }

    /**
     * List all of resource based on parameters
     *
     * @return List of resources or empty if it doesn't exist
     */
    protected List<T> listAllResources(Map params) {
        params.remove('controller')
        params.remove('action')
        def list = isCommand ? toEntityResource().list(params) : resource.list(params)
        return list
    }

    protected List<T> listAllResources(Map params, List aList) {
        if (aList != null && !aList.isEmpty()) {
            if (StringUtils.isEmpty(params.offset as String)) {
                params.offset = '0'
            }
            int listSize = aList.size()
            int offset = params.offset as Integer
            int max = params.max as Integer
            int start = offset == 0 ? 0 : (max * offset) > listSize ? listSize - max : (max * offset) - 1
            int end = (start + max) > listSize ? listSize : (start + max)
            return aList.subList(start, end)
        }
        params.remove('controller')
        params.remove('action')
        return isCommand ? toEntityResource().list(params) : resource.list(params)
    }

    /**
     * List all of columns to be querying
     *
     * @return List of columns or empty if it doesn't exist
     */
    protected List<String> listIndexedColumns() {
        try {
            resource.newInstance().listIndexedColumns()
        } catch (MissingMethodException ignored) {
            return ["${params.id}"]
        }
    }

    protected void setConnectedUser(T instance) {
        try {
            if (springSecurityService) {
                println(springSecurityService.getPrincipal()?.username)
                instance.setConnectedUser(springSecurityService.getPrincipal()?.username)
            } else {
                println("nulll")
            }
        } catch (MissingMethodException ignored) {
        }
    }

    protected T toGormEntity(T resource, Serializable id = null) {
        try {
            return resource instanceof GormEntity ? resource : resource.toEntity(id)
        } catch (MissingMethodException ignored) {
            throw new SaqException("500", "un objet de type 'Command' doit avoir la methode 'toEntity',")
        }
    }

    protected Class<T> toEntityResource() {
        try {
            return resource.newInstance().toEntity().class
        } catch (MissingMethodException ignored) {
            throw new SaqException("500", "un objet de type 'Command' doit avoir la methode 'toEntity',")
        }
    }

    protected void toDoAfter(T obj, RestFulVerbs verb) {
        try {
            Closure closure = resource.newInstance().toDoAfter(obj, verb)
            if (closure != null) {
                closure.call()
            }
        } catch (MissingMethodException ignored) {

        }
    }

    protected void toDoBefore(T obj, RestFulVerbs verb, T newObj = null) {
        try {
            Closure closure = newObj ? resource.newInstance().toDoBefore(obj, verb, newObj) : resource.newInstance().toDoBefore(obj, verb)
            if (closure != null) {
                closure.call()
            }
        } catch (MissingMethodException ignored) {

        }
    }

    protected String displayField(entity) {
        try {
            entity.newInstance().displayField()
        } catch (MissingMethodException ignored) {
            return "${params.id}"
        }
    }

    protected Field getField(String name, Class<T> domain) {
        domain.declaredFields.find { it.name.equalsIgnoreCase(name) }
    }

    protected List<Field> addParents(List<Field> fields) {
        fields.each {
            if (grailsApplication.isDomainClass(it.type)) {
                if (it.type.getSuperclass() != null) {
                    fields.addAll(it.type.getSuperclass().declaredFields)
                }
            }
        }
        return fields
    }

    /**
     * List query of resource based on parameters
     *
     * @return List of resources or empty if it doesn't exist
     *
     */
    protected List<T> listSearchResources(Map params) throws Exception {
        params.remove('controller')
        params.remove('action')
        ArrayList<String> fields = (isCommand ? toEntityResource() : resource).newInstance().listIndexedColumns()
        def isGlobal = params.isGlobal
        params.searchQuery = params.searchQuery ? params.searchQuery.toString().trim() : params.searchQuery
        def searchQuery = params.searchQuery.toString().trim()
        def criteria = (isCommand ? toEntityResource() : resource).createCriteria()
        def liste = []
        try {
            if (isGlobal) {
                log.info(JsonOutput.toJson([method: "listSearchResources", message: "Global search >> ${searchQuery}"]))
                def allFields = (isCommand ? toEntityResource() : resource).declaredFields.findAll { fields.contains(it.name) }
                Collection<Field> parentFields = [];
                if ((isCommand ? toEntityResource() : resource).getSuperclass() != null) {
                    parentFields = (isCommand ? toEntityResource() : resource).getSuperclass().declaredFields
                    parentFields.each {
                        allFields.add(it)
                    }
                }
                println(allFields: allFields*.name)
                def foreignFields = allFields.findAll { grailsApplication.isDomainClass(it.type) }
                println(params: params)
                liste = criteria.list(params) {
                    println("mmmmmmmm")
                    or {
                        for (field in allFields) {
                            if (grailsApplication.isDomainClass(field.type)) {
                                def targetColumn = displayField(field.type)
                                def targetField = getField(targetColumn, field.type)
                                def targetColumChild = displayField(targetField.type)
                                def foundCorrespondingDisplay = parentFields.find { it.name.equalsIgnoreCase(targetColumn) }

                                if (!grailsApplication.isDomainClass(targetField.type)) {
                                    fetchMode("${field.name}", FetchMode.SELECT)
                                    createAlias("${field.name}", "${field.name}", CriteriaSpecification.LEFT_JOIN)
                                    or {
                                        ilike("${field.name}.${targetColumn}", "%${params.searchQuery}%")
                                    }
                                } else {
                                    "${field.name}" {
                                        "${targetColumn}" {
                                            or {
                                                ilike("${targetColumChild}", "%${params.searchQuery}%")
                                            }
                                        }
                                    }
                                }


                            } else if (field.type == String.class) {
                                or { ilike("${field.name}", "%${params.searchQuery}%") }
                            } else if (field.type.getSuperclass() == Number.class) {
                                if (Help.isNumeric(params.searchQuery as String)) {
                                    switch (field.type) {
                                        case Double:
                                            or { eq("${field.name}", Double.parseDouble(params.searchQuery as String)) }
                                            break
                                        case BigDecimal:
                                            or { eq("${field.name}", new BigDecimal(params.searchQuery as String)) }
                                            break
                                        case Long:
                                            if (StringUtils.isNumeric(params.searchQuery as String))
                                                or { eq("${field.name}", Long.parseLong(params.searchQuery as String)) }
                                            break
                                        case Integer:
                                            if (StringUtils.isNumeric(params.searchQuery as String))
                                                or { eq("${field.name}", Integer.parseInt(params.searchQuery as String)) }
                                            break
                                    }

                                }
                            } else if (field.type.equals(Date.class)) {
                                SimpleDateFormat sdf = new SimpleDateFormat('dd-MM-yyyy')
                                if (GenericValidator.isDate(params.searchQuery as String, 'dd-MM-yyyy', true)) {
                                    def from = sdf.parse(params.searchQuery as String)
                                    def to = Date.from(from.toInstant().plus(1, ChronoUnit.DAYS))
                                    or { between("${field.name}", from, to) }
                                }
                            }
                        }
                    }

                    if (params.extraFields) {
                        def extras = params.extraFields as Map
                        if (extras.get('negation') == 'oui') {
                            for (field in extras) {
                                if (field.key != 'negation')
                                    ne("${field.key}", "${field.value}".toString())
                            }
                        } else {
                            for (field in extras) {
                                if (field.key != 'negation')
                                    eq("${field.key}", "${field.value}".toString())
                            }
                        }
                    }


                }
            } else {
                log.info(JsonOutput.toJson([method: "listSearchResources", message: "Specific search >> ${searchQuery}"]))
                def search = searchQuery
                if (!(searchQuery instanceof Map))
                    search = Help.checkJsonValid(search as String) ? (new JsonSlurper().parseText(search as String) as Map) : null
                else
                    search = searchQuery
                if (search == null) throw new SaqException("6000", "un json est requis pour une recherche specifique")

                def allFields = (isCommand ? toEntityResource() : resource).declaredFields.findAll { search.keySet().contains(it.name) }
                Collection<Field> parentFields = [];
                if ((isCommand ? toEntityResource() : resource).getSuperclass() != null) {
                    parentFields = (isCommand ? toEntityResource() : resource).getSuperclass().declaredFields
                    parentFields.each {
                        allFields.add(it)
                    }
                }
                def foreignFields = allFields.findAll { grailsApplication.isDomainClass(it.type) }
                Map<Field, Object> finalMap = [:]
                println(allFields: allFields.collect { it.name })
                println(foreignFields: foreignFields.collect { it.name })
                println(search: search)
                for (entry in search) {
                    allFields.each {
                        def f = getField(entry.key, it.type)
                        // def targetColumnChild = displayField(targetField.type)
                        if (it.name == entry.key || (f != null && f.name == entry.key)) {
                            finalMap.put(it, entry.value)
                        }
                    }
                }
                liste = criteria.list(params) {
                    for (field in finalMap) {
                        or {
                            if (grailsApplication.isDomainClass(field.key.type)) {
                                def targetColumn = displayField(field.key.type)
                                def targetField = getField(targetColumn, field.key.type.newInstance().class)
                                def targetColumnChild = displayField(targetField.type)
                                def foundCorrespondingDisplay = parentFields.find { it.name.equalsIgnoreCase(targetColumn) }
                                if (!grailsApplication.isDomainClass(targetField.type)) {
                                    println('here': targetColumn)
                                    println('field.key.name': field.key.name)
                                    fetchMode("${field.key.name}", FetchMode.SELECT)
                                    createAlias("${field.key.name}", "${field.key.name}", CriteriaSpecification.LEFT_JOIN)
                                    or { ilike("${field.key.name}.${targetColumn}", "%${field.value}%") }
                                } else {
                                    println('there': targetColumn)
                                    println('field.key.name': field.key.name)
                                    "${field.key.name}" {
                                        "${targetColumn}" {
                                            or {
                                                ilike("${targetColumnChild}", "%${field.value}%")
                                            }
                                        }
                                    }
                                }
                            } else if (field.key.type == String.class) {
                                or { eq("${field.key.name}", "${field.value}") }
                            } else if (field.key.type.superclass == Number.class) {
                                if (Help.isNumeric(field.value as String)) {
                                    switch (field.key.type) {
                                        case Double:
                                            or { eq("${field.key.name}", Double.parseDouble(field.value as String)) }
                                            break
                                        case BigDecimal:
                                            or { eq("${field.key.name}", new BigDecimal(field.value as String)) }
                                            break
                                        case Long:
                                            if (StringUtils.isNumeric(field.value as String))
                                                or { eq("${field.key.name}", Long.parseLong(field.value as String)) }
                                            break
                                        case Integer:
                                            if (StringUtils.isNumeric(field.value as String))
                                                or { eq("${field.key.name}", Integer.parseInt(field.value as String)) }
                                            break
                                    }
                                }

                            } else if (field.key.type.equals(Date.class)) {
                                SimpleDateFormat sdf = new SimpleDateFormat('dd-MM-yyyy')
                                if (GenericValidator.isDate(field.value as String, 'dd-MM-yyyy', true)) {
                                    def from = sdf.parse(field.value as String)
                                    def to = Date.from(from.toInstant().plus(1, ChronoUnit.DAYS))
                                    or { between("${field.key.name}", from, to) }
                                }

                            }


                        }
                    }
                }
            }
            return liste as List<T>
        }
        catch (Exception ex) {
            ex.printStackTrace()
            log.error(grails.plugin.json.builder.JsonOutput.toJson([methode: "listSearchResources", error: ex?.message]))
            render model: [errors: [ex.message]], template: '/errors/custom'
        }
    }

    /**
     * Counts query of resources
     *
     * @return List of resources or empty if it doesn't exist
     */
    protected Integer countSearchResources(params) {
        listSearchResources(params.findAll { it.key != 'max' && it.key != 'offset' })?.size()
    }

    /**
     * Counts all of resources
     *
     * @return List of resources or empty if it doesn't exist
     */
    protected Integer countResources() {
        (isCommand ? toEntityResource() : resource).count()
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [classMessageArg, params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND, message: 'Entite introuvable' }
        }
    }

    protected void notFoundResource() {
        throw new SaqException('4000', "L'objet ${resourceName} avec l'id ${params.id} est introuvle")
    }

    /**
     * Saves a resource
     *
     * @param resource The resource to be saved
     * @return The saved resource or null if can't save it
     */
    protected T saveResource(T resource) {
        def entity = SaqTryCatch.check(isCommand ? toGormEntity(resource) as GormEntity : resource as GormEntity, TypeException.SAVE, "Erreurs survenue lors de l'enregistrement de l'entite `${resourceName}`") as T
        entity
    }


    /**
     * Updates a resource
     *
     * @param resource The resource to be updated
     * @return The updated resource or null if can't save it
     */
    protected T updateResource(T resource) {
        def entity = SaqTryCatch.check(isCommand ? toGormEntity(resource, params.id) as GormEntity : resource as GormEntity, TypeException.UPDATE, "Erreurs survenue lors de l'enregistrement de l'entite `${resourceName}`") as T
        entity
    }

    /**
     * Deletes a resource
     *
     * @param resource The resource to be deleted
     */
    protected void deleteResource(T resource) {
        resource.delete flush: true
    }

    protected String getClassMessageArg() {
        message(code: "${resourceName}.label".toString(), default: resourceClassName)
    }

    /**
     * Retourne une reponse generic sous format json
     * @param parametres
     * @param message
     * @return
     */
    def genericResponse(int httpCode, String message, def parametres) {
        String status = (httpCode == 200) ? "OK" : "NOK"

        def rep = [
                codeReponse: httpCode,
                dateServeur: new Date(),
                data       : parametres,
                message    : message,
                status     : status
        ]
        render rep as JSON, status: httpCode

    }

}