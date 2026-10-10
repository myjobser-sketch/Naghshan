package ir.arandy.naghshan.model;

import java.io.Serializable;

/** یک ردیف کالا در جدول فاکتور. */
public class Kala implements Serializable {

    private int    rowNumber;         // ردیف
    private String productCode;       // کد کالا
    private String productName;       // نام کالا (ممکن است چند خطی باشد)
    private String unit;              // واحد  (متر، عدد، ...)
    private String type;              // نوع / اصل
    private long   quantity;          // تعداد
    private long   materialPrice;     // مبلغ جنس
    private long   laborPrice;        // مبلغ دستمزد

    public Kala() { }

    public long getTotalPrice() { return materialPrice + laborPrice; }

    // getters / setters
    public int    getRowNumber()      { return rowNumber; }
    public void   setRowNumber(int v) { rowNumber = v; }

    public String getProductCode()      { return productCode; }
    public void   setProductCode(String v) { productCode = v; }

    public String getProductName()      { return productName; }
    public void   setProductName(String v) { productName = v; }

    public String getUnit()      { return unit; }
    public void   setUnit(String v) { unit = v; }

    public String getType()      { return type; }
    public void   setType(String v) { type = v; }

    public long   getQuantity()      { return quantity; }
    public void   setQuantity(long v) { quantity = v; }

    public long   getMaterialPrice()      { return materialPrice; }
    public void   setMaterialPrice(long v) { materialPrice = v; }

    public long   getLaborPrice()      { return laborPrice; }
    public void   setLaborPrice(long v) { laborPrice = v; }
}