package ir.arandy.naghshan.data.pdf;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** استخراج متن و مختصات هر قطعه متن از PDF. */
public class PdfTextExtractor {

    /** یک قطعه متن با موقعیت. */
    public static class TextElement {
        public final String text;
        public final float  x;
        public final float  y;
        public final float  width;
        public final float  height;
        public final int    pageIndex;

        public TextElement(String t, float x, float y, float w, float h, int p) {
            this.text = t; this.x = x; this.y = y;
            this.width = w; this.height = h; this.pageIndex = p;
        }
    }

    public List<TextElement> extract(File pdf) throws IOException {
        PDDocument doc = PDDocument.load(pdf);
        try {
            List<TextElement> out = new ArrayList<>();
            int pages = doc.getNumberOfPages();
            for (int p = 0; p < pages; p++) {
                PDPage page = doc.getPage(p);
                PageStripper stripper = new PageStripper(out, p);
                stripper.setStartPage(p + 1);
                stripper.setEndPage(p + 1);
                stripper.getText(doc);
            }
            return out;
        } finally {
            doc.close();
        }
    }

    private static class PageStripper extends PDFTextStripper {
        private final List<TextElement> sink;
        private final int pageIndex;
        PageStripper(List<TextElement> sink, int pageIndex) throws IOException {
            this.sink = sink; this.pageIndex = pageIndex;
        }
        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            if (text == null) return;
            text = text.trim();
            if (text.isEmpty()) return;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (TextPosition tp : positions) {
                if (tp.getXDirAdj() < minX) minX = tp.getXDirAdj();
                if (tp.getYDirAdj() < minY) minY = tp.getYDirAdj();
                if (tp.getXDirAdj() + tp.getWidthDirAdj() > maxX)
                    maxX = tp.getXDirAdj() + tp.getWidthDirAdj();
                if (tp.getYDirAdj() + tp.getHeightDir() > maxY)
                    maxY = tp.getYDirAdj() + tp.getHeightDir();
            }
            sink.add(new TextElement(text, minX, minY, maxX - minX, maxY - minY, pageIndex));
        }
    }
}