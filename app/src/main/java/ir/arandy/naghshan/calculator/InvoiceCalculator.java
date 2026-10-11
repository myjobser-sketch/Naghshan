package ir.arandy.naghshan.calculator;

import java.util.ArrayList;
import java.util.List;

import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.model.KalaTable;

/** مسئول تمام محاسبات مالی. */
public class InvoiceCalculator {

    public Invoice applyAdjustment(Invoice src, double percent,
                                   boolean toMaterial, boolean toLabor) {
        double factor = 1.0 + (percent / 100.0);

        Invoice out = new Invoice();
        out.setProjectInfo(src.getProjectInfo());
        out.setAppliedPercent(percent);
        out.setApplyToMaterial(toMaterial);
        out.setApplyToLabor(toLabor);

        List<KalaTable> newTables = new ArrayList<>();
        for (KalaTable oldTable : src.getTables()) {
            KalaTable newTable = new KalaTable(oldTable.getName());
            List<Kala> newItems = new ArrayList<>();
            for (Kala k : oldTable.getItems()) {
                Kala n = new Kala();
                n.setTableName(k.getTableName());
                n.setRowNumber(k.getRowNumber());
                n.setProductCode(k.getProductCode());
                n.setProductName(k.getProductName());
                n.setUnit(k.getUnit());
                n.setType(k.getType());
                n.setQuantity(k.getQuantity());

                // قیمت واحد جنس/دستمزد
                long mat = k.getMaterialPrice();
                long lab = k.getLaborPrice();
                if (toMaterial) mat = Math.round(mat * factor);
                if (toLabor)    lab = Math.round(lab * factor);

                n.setMaterialPrice(mat);
                n.setLaborPrice(lab);
                // مبلغ کل ردیف = تعداد × (قیمت واحد جنس + قیمت واحد دستمزد)
                n.setTotalPrice(Math.round(k.getQuantity() * (mat + lab)));
                newItems.add(n);
            }
            newTable.setItems(newItems);
            newTables.add(newTable);
        }
        out.setTables(newTables);
        return out;
    }
}