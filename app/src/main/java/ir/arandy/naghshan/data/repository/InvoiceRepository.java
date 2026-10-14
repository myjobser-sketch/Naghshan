package ir.arandy.naghshan.data.repository;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ir.arandy.naghshan.calculator.InvoiceCalculator;
import ir.arandy.naghshan.data.export.ExcelExporter;
import ir.arandy.naghshan.data.export.ExportOptions;
import ir.arandy.naghshan.data.export.PdfExporter;
import ir.arandy.naghshan.data.pdf.PdfParser;
import ir.arandy.naghshan.data.profile.ProfileManager;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.storage.FileStorage;

/** مرکز هماهنگی عملیات. */
public class InvoiceRepository {

    private final Context ctx;
    private final PdfParser        parser     = new PdfParser();
    private final InvoiceCalculator calculator = new InvoiceCalculator();
    private final ExcelExporter    excel      = new ExcelExporter();
    private final PdfExporter      pdf        = new PdfExporter();
    private final FileStorage      storage;

    public InvoiceRepository(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.storage = new FileStorage(this.ctx);
    }

    /** فایل را از Uri به cache کپی می‌کند و Parse می‌کند. */
    public Invoice parsePdfFromUri(android.net.Uri uri) throws IOException {
        File tmp = new File(ctx.getCacheDir(), "input.pdf");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        return parser.parse(tmp);
    }

    public Invoice applyAdjustment(Invoice invoice, double percent,
                                   boolean toMaterial, boolean toLabor) {
        return calculator.applyAdjustment(invoice, percent, toMaterial, toLabor);
    }

    public File exportExcel(Invoice invoice, ExportOptions opts) throws IOException {
        File out = storage.newExcel(invoice.getProjectInfo().getProjectNumber());
        ProfileManager pm = new ProfileManager(ctx);
        return excel.export(invoice, out, pm, opts);
    }

    public File exportPdf(Invoice invoice, File persianFontFile, ExportOptions opts) throws IOException {
        File out = storage.newPdf(invoice.getProjectInfo().getProjectNumber());
        ProfileManager pm = new ProfileManager(ctx);
        return pdf.export(invoice, out, persianFontFile, pm, opts);
    }
}