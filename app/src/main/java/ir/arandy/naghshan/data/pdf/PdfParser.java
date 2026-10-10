package ir.arandy.naghshan.data.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.arandy.naghshan.data.pdf.PdfTextExtractor.TextElement;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.ProjectInfo;

/** Parser اصلی PDF. */
public class PdfParser {

    private static final Pattern PROJECT_NUMBER =
            Pattern.compile("(?:شماره\\s*طرح|طرح)\\s*[:\\-]?\\s*([A-Za-z]?\\d{4,})");
    private static final Pattern REQUEST_NUMBER =
            Pattern.compile("(?:شماره\\s*درخواست|درخواست)\\s*[:\\-]?\\s*(\\d{4,})");
    private static final Pattern APPLICANT =
            Pattern.compile("(?:متقاضی)\\s*[:\\-]?\\s*(.+)");

    public Invoice parse(File pdf) throws IOException {
        PdfTextExtractor extractor = new PdfTextExtractor();
        List<TextElement> elements = extractor.extract(pdf);

        Invoice invoice = new Invoice();
        invoice.setProjectInfo(extractProjectInfo(elements));

        TableDetector detector = new TableDetector();
        TableDetector.TableRegion region = detector.detect(elements);
        if (region != null) {
            TableRowAssembler assembler = new TableRowAssembler(region);
            List<Kala> items = assembler.assemble();
            invoice.setItems(items);
        }
        return invoice;
    }

    private ProjectInfo extractProjectInfo(List<TextElement> elements) {
        ProjectInfo pi = new ProjectInfo();
        // فقط دو صفحه اول را برای اطلاعات طرح اسکن می‌کنیم.
        for (TextElement e : elements) {
            if (e.pageIndex > 2) break;
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
                if (m.find()) pi.setApplicantName(m.group(1).trim());
            }
        }
        return pi;
    }
}