package ir.arandy.naghshan.data.export;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.MoneyFormatter;

/**
 * ساخت PDF نهایی. برای پشتیبانی فارسی از فونت Vazirmatn در assets استفاده می‌کنیم.
 * نکته: PDFBox و RTL همیشه ساده نیست؛ این نسخه سادهٔ کاربردی است.
 */
public class PdfExporter {

    public File export(Invoice invoice, File outFile, File persianFontFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font font = PDType0Font.load(doc, persianFontFile);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 40f;
            float y = page.getMediaBox().getHeight() - margin;
            float lineH = 16f;

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                y = writeLine(cs, font, "فاکتور طرح", y, margin, 16);
                y -= 6;
                y = writeLine(cs, font,
                        "شماره طرح: " + safe(invoice.getProjectInfo().getProjectNumber()),
                        y, margin, 11);
                y = writeLine(cs, font,
                        "متقاضی: " + safe(invoice.getProjectInfo().getApplicantName()),
                        y, margin, 11);
                y = writeLine(cs, font,
                        "شماره درخواست: " + safe(invoice.getProjectInfo().getRequestNumber()),
                        y, margin, 11);
                y -= 8;

                y = writeLine(cs, font,
                        "ردیف | نام کالا | واحد | تعداد | جنس | دستمزد | کل",
                        y, margin, 11);
                y -= 4;

                for (Kala k : invoice.getItems()) {
                    if (y < margin + 3 * lineH) {
                        // صفحه جدید
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    String row = k.getRowNumber() + " | " + safe(k.getProductName())
                            + " | " + safe(k.getUnit())
                            + " | " + k.getQuantity()
                            + " | " + MoneyFormatter.format(k.getMaterialPrice())
                            + " | " + MoneyFormatter.format(k.getLaborPrice())
                            + " | " + MoneyFormatter.format(k.getTotalPrice());
                    y = writeLine(cs, font, row, y, margin, 10);
                }

                y -= 10;
                y = writeLine(cs, font,
                        "جمع جنس: " + MoneyFormatter.format(invoice.getMaterialTotal()),
                        y, margin, 11);
                y = writeLine(cs, font,
                        "جمع دستمزد: " + MoneyFormatter.format(invoice.getLaborTotal()),
                        y, margin, 11);
                y = writeLine(cs, font,
                        "جمع کل: " + MoneyFormatter.format(invoice.getGrandTotal()),
                        y, margin, 12);
            } finally {
                cs.close();
            }

            doc.save(outFile);
        }
        return outFile;
    }

    private static float writeLine(PDPageContentStream cs, PDType0Font font,
                                   String text, float y, float x, int size) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 6);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}