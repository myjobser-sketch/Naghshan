package ir.arandy.naghshan.calculator;

import java.util.ArrayList;
import java.util.List;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;

/** مسئول تمام محاسبات مالی. مستقل از Parser و UI. */
public class InvoiceCalculator {

    /**
     * یک کپی از Invoice می‌سازد که در آن درصد داده‌شده روی مبالغ اعمال شده است.
     * درصد می‌تواند منفی باشد (کاهش).
     */
    public Invoice applyAdjustment(Invoice src, double percent,
                                   boolean toMaterial, boolean toLabor) {

        double factor = 1.0 + (percent / 100.0);

        Invoice out = new Invoice();
        out.setProjectInfo(src.getProjectInfo());
        out.setAppliedPercent(percent);
        out.setApplyToMaterial(toMaterial);
        out.setApplyToLabor(toLabor);

        List<Kala> newItems = new ArrayList<>(src.getItems().size());
        for (Kala k : src.getItems()) {
            Kala n = new Kala();
            n.setRowNumber(k.getRowNumber());
            n.setProductCode(k.getProductCode());
            n.setProductName(k.getProductName());
            n.setUnit(k.getUnit());
            n.setType(k.getType());
            n.setQuantity(k.getQuantity());

            long mat = k.getMaterialPrice();
            long lab = k.getLaborPrice();

            if (toMaterial) mat = Math.round(mat * factor);
            if (toLabor)    lab = Math.round(lab * factor);

            n.setMaterialPrice(mat);
            n.setLaborPrice(lab);
            newItems.add(n);
        }
        out.setItems(newItems);
        return out;
    }
}