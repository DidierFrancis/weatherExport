package sun.wheatherExport.vdlFiles

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class VDLFileServiceSpec extends Specification {
/*

    VDLFileService VDLFileService
    @Autowired Datastore datastore

    private Long setupData() {
        // TODO: Populate valid domain instances and return a valid ID
        //new VDLFile(...).save(flush: true, failOnError: true)
        //new VDLFile(...).save(flush: true, failOnError: true)
        //VDLFile VDLFile = new VDLFile(...).save(flush: true, failOnError: true)
        //new VDLFile(...).save(flush: true, failOnError: true)
        //new VDLFile(...).save(flush: true, failOnError: true)
        assert false, "TODO: Provide a setupData() implementation for this generated test suite"
        //VDLFile.id
    }

    void cleanup() {
        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
    }

    void "test get"() {
        setupData()

        expect:
        VDLFileService.get(1) != null
    }

    void "test list"() {
        setupData()

        when:
        List<VDLFile> VDLFileList = VDLFileService.list(max: 2, offset: 2)

        then:
        VDLFileList.size() == 2
        assert false, "TODO: Verify the correct instances are returned"
    }

    void "test count"() {
        setupData()

        expect:
        VDLFileService.count() == 5
    }

    void "test delete"() {
        Long VDLFileId = setupData()

        expect:
        VDLFileService.count() == 5

        when:
        VDLFileService.delete(VDLFileId)
        datastore.currentSession.flush()

        then:
        VDLFileService.count() == 4
    }

    void "test save"() {
        when:
        assert false, "TODO: Provide a valid instance to save"
        VDLFile VDLFile = new VDLFile()
        VDLFileService.save(VDLFile)

        then:
        VDLFile.id != null
    }
 */
}
