package sun.weatherExport.plugins.excelimport

import org.apache.poi.ss.usermodel.*
import sun.imexporter.AbstractImexporter

/**
 * Created by IntelliJ IDEA.
 * User: Didier Francis S Tendeng
 */
public abstract class AbstractExcelImporter extends AbstractImexporter {
    @Deprecated
    InputStream inStr = null

    Workbook workbook = null
    FormulaEvaluator evaluator = null;

    Sheet sheet = null

    @Deprecated
    public AbstractExcelImporter(String fileName) {
        this.read(fileName)
    }

    @Deprecated
    def close() {
        inStr.close()
    }

    public AbstractExcelImporter() {
    }

    protected def read(String fileName) {
        inStr = new FileInputStream(fileName)
        this.read(inStr)
    }

    @Override
    protected def read(InputStream inp) {
        workbook = WorkbookFactory.create(inp)
        evaluator = workbook.creationHelper.createFormulaEvaluator()
    }

    @Override
    protected def write(OutputStream out) {
        workbook.write(out)
    }

    def createEmpty() {
        workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook()
        evaluator = workbook.creationHelper.createFormulaEvaluator()
        workbook.createSheet('Sheet1')
        return this
    }

    def evaluateAllFormulaCells() {
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            def sheet = workbook.getSheetAt(sheetNum);
            for (def r : sheet) {
                for (def c : r) {
                    if (c.getCellType() == Cell.CELL_TYPE_FORMULA) {
//						if(c.getCellValue()==null || c.getCellValue() == '' || c.getCellValue() == 0){
                        evaluator.evaluateFormulaCell(c);
//						}
                    }
                }
            }
        }
    }
}
























