package sun.wheatherExport.vdlFiles

class BruteData {
    Long id
    String aiValues
    Date dateTimeTrend
    Integer voyageId
    Integer dockingId
    Float dockingHeadingDeviation
    Double rool
    Integer ossId
    Integer assetId
    Integer onSiteId
    Integer dockingStable
    Double fenderPushForce = 0
    Double cppPsPitch = 0
    Double cppSbPitch
    String cppMode
    Double mePsUseHr
    Double meStbUseHr
    Double geRunningHr
    Double geStbRunningHr
    Double mePsSpeedRpm
    Double meStbSpeedRpm
    Double elecMotorPs
    Double elecMotorStb
    Double batteriePsState
    Double batterieSbState
    Double batterieSbStateHeath
    Double batteriePsStateHeath
    Double batterieSbPowerOutput
    Double batteriePsPowerOutput
    Double speedOverGround
    Float gpsLat = 0
    Float gpsLon = 0

    VDLFile file

    static constraints = {
    }
    static mapping = {
        aiValues sqlType: 'text'
    }
}
