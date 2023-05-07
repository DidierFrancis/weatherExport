package sun.weatherExport.excel

import sun.constante.Constantes
import sun.security.User

class CsvToolService {


    public static String getTempDir() {
        String tempDir = System.getProperty('java.io.tmpdir')  // Ecrire ces fchiers dans le répertoire temporaire
        if (!tempDir.endsWith(File.separator)) {
            tempDir += File.separator
        }
        return tempDir
    }

    public static String getFolderDir() {
        String brutFolder = Constantes.FILE_BRUTE_PATH  // Ecrire ces fchiers dans le répertoire brut
        if (!brutFolder.endsWith(File.separator)) {
            brutFolder += File.separator
        }
        return brutFolder
    }

    def fasterExportCsv(outs, String entete, lines, User currentUser) {

        outs << "\n"
        outs << "\n"
        outs << "${entete}\n"
        outs << "\n"
        lines.each { String line ->
            outs << "${line}\n"
        }

        outs.flush()
        outs.close()
    }

    def exportCsv(def title, def header, def listData, def date, def filtre) {
        String tempDir = getTempDir() // pour les problèmes de séparateur
        log.debug(tempDir)
        def fileName = tempDir + title
        FileWriter csvWriter = new FileWriter(fileName)
        println csvWriter.encoding

        header.each {
            csvWriter.append(it.toString())
            csvWriter.append(";")

        }
        csvWriter.append("\n")


        listData.each {
            it.each { val ->
                csvWriter.append(val.toString())
                csvWriter.append(";")
            }
            csvWriter.append("\n")
        }

        csvWriter.flush()
        csvWriter.close()
        return title
    }

    def getFileWriter(title) {
        String tempDir = getFolderDir() // pour les problèmes de séparateur
        log.debug(tempDir)
        def fileName = tempDir + title
        FileWriter csvWriter = new FileWriter(fileName, true)
        println csvWriter.encoding
        csvWriter
    }

    def writeHeader(def header, def title, def tempDir) {
        log.debug(tempDir)
        def fileName = tempDir + title
        FileWriter csvWriter = new FileWriter(fileName, false)
        header.each {
            csvWriter.append(it.toString())
            csvWriter.append(";")

        }
        csvWriter.append("\n")
        csvWriter.flush()
        csvWriter.close()
    }

    def writeLine(def line, def title, def tempDir) {
        log.debug(tempDir)
        def fileName = tempDir + title
        FileWriter csvWriter = new FileWriter(fileName, true)
        line.each {
            csvWriter.append(it.toString())
            csvWriter.append(";")

        }
        csvWriter.append("\n")
        csvWriter.flush()
        csvWriter.close()
    }


    def exporSimpleCsv(String exportFilename, def header, def listData, boolean append) {

        if (exportFilename == null) {
            throw new IllegalArgumentException("filename is null")
        }
        String tempDir = getTempDir()
        log.debug(tempDir)
        String _fileName = tempDir + exportFilename
        FileWriter csvWriter = new FileWriter(_fileName, append)
        println csvWriter.encoding
        if (header != null) {
            header.each {
                csvWriter.append(it)
                csvWriter.append(";")
            }
            csvWriter.append("\n")
        }
        if (listData != null) {
            listData.each {
                it.each { val ->
                    csvWriter.append(val.toString())
                    csvWriter.append(";")
                }
                csvWriter.append("\n")
            }
        }
        csvWriter.flush()
        csvWriter.close()
        return _fileName
    }

}
