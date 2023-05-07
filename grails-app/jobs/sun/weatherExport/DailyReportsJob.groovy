package sun.weatherExport

import sun.helpers.DateUtilExtensions
import sun.wheatherExport.vdlFiles.BruteData

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DailyReportsJob {
    static triggers = {
    }
    def vdlFileService


    def execute() {
        try {
            vdlFileService.generateMatrix()
            vdlFileService.generateAllBruteData()
        }catch(Exception ex) {
            println(ex.message)
            System.exit(0)
        }

    }
}
