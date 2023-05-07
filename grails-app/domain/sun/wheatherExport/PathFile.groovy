package sun.wheatherExport

class PathFile {
    String name
    String path
    String type
    static constraints = {
        type inList: ['FILE', 'DIR']
    }
}
