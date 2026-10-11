package ir.arandy.naghshan.model;

import java.io.Serializable;

/** یک ردیف کالا در جدول لیست کالا. */
public class Kala implements Serializable {

    private String tableName;      // زیرعنوان جدول (مثلاً "شبکه فشارمتوسط هوایی(پیشنهادی)")
    private int    rowNumber;      // ردیف
    private String productCode;    // کد کالا
    private String productName;    // نام کالا
    private String unit;           // واحد
    private String type;           // نوع
    private double quantity;       // تعداد (ممکن است اعشاری باشد)
    private long   materialPrice;  // مبلغ جنس
    private long   laborPrice;     // مبلغ دستمزد
    private long   totalPrice;     // مبلغ کل (از خود PDF)

    public Kala() { }

    // --- tableName ---
    public String getTableName() { return tableName; }
    public void   setTableName(String v) { this.tableName = v; }

    // --- rowNumber ---
    public int  getRowNumber()      { return rowNumber; }
    public void setRowNumber(int v) { rowNumber = v; }

    // --- productCode ---
    public String getProductCode()      { return productCode; }
    public void   setProductCode(String v) { productCode = v; }

    // --- productName ---
    public String getProductName()      { return productName; }
    public void   setProductName(String v) { productName = v; }

    // --- unit ---
    public String getUnit()      { return unit; }
    public void   setUnit(String v) { unit = v; }

    // --- type ---
    public String getType()      { return type; }
    public void   setType(String v) { type = v; }

    // --- quantity ---
    public double getQuantity()      { return quantity; }
    public void   setQuantity(double v) { quantity = v; }

    // --- materialPrice ---
    public long getMaterialPrice()      { return materialPrice; }
    public void setMaterialPrice(long v) { materialPrice = v; }

    // --- laborPrice ---
    public long getLaborPrice()      { return laborPrice; }
    public void setLaborPrice(long v) { laborPrice = v; }

    // --- totalPrice ---
    public long getTotalPrice()      { return totalPrice; }
    public void setTotalPrice(long v) { totalPrice = v; }
}