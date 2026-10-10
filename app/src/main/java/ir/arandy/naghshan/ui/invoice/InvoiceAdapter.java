package ir.arandy.naghshan.ui.invoice;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.arandy.naghshan.R;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.util.MoneyFormatter;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.VH> {

    private final List<Kala> items = new ArrayList<>();

    public void submit(Invoice inv) {
        items.clear();
        if (inv != null) items.addAll(inv.getItems());
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_kala, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Kala k = items.get(pos);
        h.row.setText(String.valueOf(k.getRowNumber()));
        h.name.setText(k.getProductName());
        h.unit.setText("واحد: " + (k.getUnit() == null ? "-" : k.getUnit()));
        h.qty.setText("تعداد: " + k.getQuantity());
        h.total.setText("مبلغ کل: " + MoneyFormatter.formatWithRial(k.getTotalPrice()));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView row, name, unit, qty, total;
        VH(View v) {
            super(v);
            row   = v.findViewById(R.id.txtRow);
            name  = v.findViewById(R.id.txtName);
            unit  = v.findViewById(R.id.txtUnit);
            qty   = v.findViewById(R.id.txtQty);
            total = v.findViewById(R.id.txtTotal);
        }
    }
}