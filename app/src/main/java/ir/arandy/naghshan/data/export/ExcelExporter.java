package ir.arandy.naghshan.data.export;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;

public class ExcelExporter {

    public File export(Invoice invoice, File outFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            Workbook wb = new Workbook(fos, "Naghshan", "1.0");
            Worksheet ws = wb.newWorksheet("فاکتور");

            int r = 0;
            ws.value(r, 0, "شماره طرح");    ws.value(r, 1, nz(invoice.getProjectInfo().getProjectNumber())); r++;
            ws.value(r, 0, "متقاضی");       ws.value(r, 1, nz(invoice.getProjectInfo().getApplicantName())); r++;
            ws.value(r, 0, "شماره درخواست");ws.value(r, 1, nz(invoice.getProjectInfo().getRequestNumber())); r++;
            r++;

            String[] headers = {"ردیف","کد","نام کالا","واحد","نوع","تعداد","جنس","دستمزد","کل"};
            for (int c = 0; c < headers.length; c++) ws.value(r, c, headers[c]);
            r++;

            for (Kala k : invoice.getItems()) {
                ws.value(r, 0, k.getRowNumber());
                ws.value(r, 1, nz(k.getProductCode()));
                ws.value(r, 2, nz(k.getProductName()));
                ws.value(r, 3, nz(k.getUnit()));
                ws.value(r, 4, nz(k.getType()));
                ws.value(r, 5, k.getQuantity());
                ws.value(r, 6, k.getMaterialPrice());
                ws.value(r, 7, k.getLaborPrice());
                ws.value(r, 8, k.getTotalPrice());
                r++;
            }

            r++;
            ws.value(r, 0, "جمع جنس");    ws.value(r, 1, invoice.getMaterialTotal()); r++;
            ws.value(r, 0, "جمع دستمزد"); ws.value(r, 1, invoice.getLaborTotal());    r++;
            ws.value(r, 0, "جمع کل");     ws.value(r, 1, invoice.getGrandTotal());    r++;

            wb.finish();
        }
        return outFile;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}