package ir.arandy.naghshan.data.pdf;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;
import ir.arandy.naghshan.model.ProjectInfo;

/** Parser اصلی PDF. */
public class PdfParser {

    private static final String TAG = "NAGHSHAN_PARSER";

    private static final Pattern PROJECT_NUMBER =
            Pattern.compile("(S\\d{6,})");
    private static final Pattern REQUEST_NUMBER =
            Pattern.compile("(?:شماره\\s*درخواست|درخواست)\\D{0,20}?(\\d{6,12})");
    private static final Pattern APPLICANT =
            Pattern.compile("(?:نام\\s*متقاضی|متقاضی)\\s*[:\\-]?\\s*([^\\n]{2,40})");

    public Invoice parse(File pdf) throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor();
        List<TextElement> elements = extractor.extract(pdf);

        // ---- دیباگ: تعداد عناصر و نمونه‌ها ----
        Log.i(TAG, "Total TextElements: " + elements.size());

        Invoice invoice = new Invoice();
        invoice.setProjectInfo(extractProjectInfo(elements));

        TableDetector detector = new TableDetector();
        List<TableDetector.DetectedTable> tables = detector.detectAll(elements);
        Log.i(TAG, "Detected tables: " + tables.size());

        List<KalaTable> kalaTables = new ArrayList<>();
        for (TableDetector.DetectedTable dt : tables) {
            TableRowAssembler assembler = new TableRowAssembler(dt);
            List<Kala> items = assembler.assemble();
            Log.i(TAG, "Table '" + dt.name + "' rows=" + dt.rows.size()
                    + " -> items=" + items.size());
            if (items.isEmpty()) continue;
            KalaTable kt = new KalaTable(dt.name);
            kt.setItems(items);
            kalaTables.add(kt);
        }
        invoice.setTables(kalaTables);
        Log.i(TAG, "Total tables with items: " + kalaTables.size());
        return invoice;
    }

    private ProjectInfo extractProjectInfo(List<TextElement> elements) {
        ProjectInfo pi = new ProjectInfo();
        for (TextElement e : elements) {
            if (e.pageIndex > 6) break;
            String t = e.text.trim();

            if (pi.getProjectNumber() == null) {
                Matcher m = PROJECT_NUMBER.matcher(t);
                if (m.find()) pi.setProjectNumber(m.group(1));
            }
            if (pi.getRequestNumber() == null) {
                Matcher m = REQUEST_NUMBER.matcher(t);
                if (m.find()) pi.setRequestNumber(m.group(1));
            }
            if (pi.getApplicantName() == null) {
                Matcher m = APPLICANT.matcher(t);
                if (m.find()) {
                    String s = m.group(1).trim();
                    if (s.length() > 2 && !s.equals(t)) pi.setApplicantName(s);
                }
            }
        }
        return pi;
    }
}