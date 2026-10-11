package ir.arandy.naghshan.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** یک جدول از «لیست کالا» با نام و ردیف‌های خودش. */
public class KalaTable implements Serializable {

    private String name;
    private List<Kala> items = new ArrayList<>();

    public KalaTable() { }
    public KalaTable(String name) { this.name = name; }

    public String getName() { return name; }
    public void   setName(String v) { this.name = v; }

    public List<Kala> getItems() { return items; }
    public void setItems(List<Kala> v) { this.items = v; }

    /**
     * جمع جنس کل این جدول.
     * نکته: materialPrice در Kala «قیمت واحد» است، پس در تعداد ضرب می‌شود.
     */
    public long getMaterialTotal() {
        long s = 0;
        for (Kala k : items) s += Math.round(k.getMaterialPrice() * k.getQuantity());
        return s;
    }

    /** جمع دستمزد کل این جدول (laborPrice قیمت واحد است). */
    public long getLaborTotal() {
        long s = 0;
        for (Kala k : items) s += Math.round(k.getLaborPrice() * k.getQuantity());
        return s;
    }

    /** جمع کل این جدول (totalPrice از PDF، جمع ردیف است). */
    public long getGrandTotal() {
        long s = 0; for (Kala k : items) s += k.getTotalPrice(); return s;
    }
}