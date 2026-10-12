package ir.arandy.naghshan.ui.invoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.arandy.naghshan.R;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.MoneyFormatter;

public class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TABLE_HEADER = 0;
    private static final int TYPE_KALA = 1;

    private final List<Object> data = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(Kala k, int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void submit(Invoice inv) {
        data.clear();
        if (inv != null && inv.getTables() != null) {
            for (ir.arandy.naghshan.model.KalaTable t : inv.getTables()) {
                data.add(t.getName()); // header
                data.addAll(t.getItems());
            }
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return (data.get(position) instanceof String) ? TYPE_TABLE_HEADER : TYPE_KALA;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        if (t == TYPE_TABLE_HEADER) {
            TextView tv = new TextView(p.getContext());
            tv.setPadding(24, 24, 24, 12);
            tv.setTextSize(16);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(0xFF1E3A8A);
            return new HeaderVH(tv);
        }
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_kala, p, false);
        return new KalaVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        if (h instanceof HeaderVH) {
            ((HeaderVH) h).tv.setText((String) data.get(pos));
        } else {
            KalaVH kh = (KalaVH) h;
            Kala k = (Kala) data.get(pos);
            kh.row.setText(String.valueOf(k.getRowNumber()));
            kh.code.setText("کد: " + (k.getProductCode() == null ? "-" : k.getProductCode()));
            kh.name.setText(k.getProductName());
            kh.unit.setText("واحد: " + (k.getUnit() == null ? "-" : k.getUnit()));
            kh.qty.setText("تعداد: " + formatQty(k.getQuantity()));
            kh.total.setText("مبلغ کل: " + MoneyFormatter.formatWithRial(k.getTotalPrice()));

            // ⬇ جدید: کلیک برای ویرایش قیمت
            final int fPos = pos;
            kh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(k, fPos);
            });
        }
    }

    @Override public int getItemCount() { return data.size(); }

    private static String formatQty(double q) {
        if (q == Math.floor(q)) return String.valueOf((long) q);
        return String.valueOf(q);
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tv;
        HeaderVH(TextView v) { super(v); tv = v; }
    }

    static class KalaVH extends RecyclerView.ViewHolder {
        final TextView row, code, name, unit, qty, total;
        KalaVH(View v) {
            super(v);
            row   = v.findViewById(R.id.txtRow);
            code  = v.findViewById(R.id.txtCode);
            name  = v.findViewById(R.id.txtName);
            unit  = v.findViewById(R.id.txtUnit);
            qty   = v.findViewById(R.id.txtQty);
            total = v.findViewById(R.id.txtTotal);
        }
    }
}