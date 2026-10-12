package ir.arandy.naghshan.ui.history;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ir.arandy.naghshan.R;
import ir.arandy.naghshan.data.history.ExportRecord;
import ir.arandy.naghshan.data.history.HistoryManager;
import ir.arandy.naghshan.util.MoneyFormatter;
import ir.arandy.naghshan.util.ShareUtil;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private static final String TAG = "NAGHSHAN_HISTORY";

    private final List<ExportRecord> data = new ArrayList<>();
    private final Context ctx;
    private final HistoryManager hm;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);

    public HistoryAdapter(Context ctx, HistoryManager hm) {
        this.ctx = ctx;
        this.hm  = hm;
    }

    public void submit(List<ExportRecord> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
        Log.v(TAG, "submit: size=" + data.size());
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_history, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ExportRecord r = data.get(pos);

        h.txtProject.setText(r.projectNumber == null || r.projectNumber.isEmpty()
                ? "بدون شماره طرح" : r.projectNumber);
        h.txtApplicant.setText(r.applicant == null || r.applicant.isEmpty()
                ? "-" : r.applicant);
        h.txtTotal.setText(MoneyFormatter.formatWithRial(r.grandTotal));

        StringBuilder meta = new StringBuilder();
        meta.append(sdf.format(new Date(r.createdAt)));
        if (r.appliedPercent != 0) {
            meta.append("  •  ");
            meta.append(r.appliedPercent > 0 ? "+" : "");
            meta.append(r.appliedPercent).append("%");
        }
        if (r.excelPath != null) meta.append("  •  Excel");
        if (r.pdfPath   != null) meta.append("  •  PDF");
        h.txtMeta.setText(meta.toString());

        h.itemView.setOnClickListener(v -> showActions(r, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return data.size(); }

    private void showActions(ExportRecord r, int pos) {
        final CharSequence[] items = new CharSequence[]{
                "اشتراک PDF",
                "اشتراک Excel",
                "حذف از تاریخچه"
        };
        new AlertDialog.Builder(ctx)
                .setTitle(r.projectNumber == null ? "پروژه" : r.projectNumber)
                .setItems(items, (DialogInterface d, int which) -> {
                    switch (which) {
                        case 0: sharePdf(r);  break;
                        case 1: shareExcel(r);break;
                        case 2: deleteRecord(r, pos); break;
                    }
                })
                .show();
    }

    private void sharePdf(ExportRecord r) {
        if (r.pdfPath == null) { toastMissing(); return; }
        ShareUtil.share(ctx, new File(r.pdfPath), "application/pdf");
    }

    private void shareExcel(ExportRecord r) {
        if (r.excelPath == null) { toastMissing(); return; }
        ShareUtil.share(ctx, new File(r.excelPath),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private void deleteRecord(ExportRecord r, int pos) {
        new AlertDialog.Builder(ctx)
                .setTitle("حذف رکورد")
                .setMessage("این رکورد از تاریخچه حذف شود؟ (فایل‌ها حذف نمی‌شوند)")
                .setPositiveButton("حذف", (d, w) -> {
                    hm.remove(r);
                    data.remove(pos);
                    notifyItemRemoved(pos);
                    Log.i(TAG, "deleted: " + r);
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void toastMissing() {
        Toast.makeText(ctx, "فایل مورد نظر حذف شده است", Toast.LENGTH_SHORT).show();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtProject, txtApplicant, txtTotal, txtMeta;
        VH(View v) {
            super(v);
            txtProject   = v.findViewById(R.id.txtProject);
            txtApplicant = v.findViewById(R.id.txtApplicant);
            txtTotal     = v.findViewById(R.id.txtTotal);
            txtMeta      = v.findViewById(R.id.txtMeta);
        }
    }
}