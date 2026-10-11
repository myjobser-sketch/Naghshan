package ir.arandy.naghshan.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** مدل اصلی خروجی Parser: اطلاعات طرح + جدول‌های لیست کالا. */
public class Invoice implements Serializable {

    private ProjectInfo projectInfo = new ProjectInfo();
    private List<KalaTable> tables = new ArrayList<>();

    private double appliedPercent = 0.0;
    private boolean applyToMaterial = true;
    private boolean applyToLabor    = true;

    public ProjectInfo getProjectInfo() { return projectInfo; }
    public void setProjectInfo(ProjectInfo p) { this.projectInfo = p; }

    public List<KalaTable> getTables() { return tables; }
    public void setTables(List<KalaTable> v) { this.tables = v; }

    public double getAppliedPercent() { return appliedPercent; }
    public void setAppliedPercent(double v) { appliedPercent = v; }

    public boolean isApplyToMaterial() { return applyToMaterial; }
    public void setApplyToMaterial(boolean v) { applyToMaterial = v; }

    public boolean isApplyToLabor() { return applyToLabor; }
    public void setApplyToLabor(boolean v) { applyToLabor = v; }

    /** جمع کل همه جدول‌ها. */
    public long getMaterialTotal() {
        long s = 0; for (KalaTable t : tables) s += t.getMaterialTotal(); return s;
    }
    public long getLaborTotal() {
        long s = 0; for (KalaTable t : tables) s += t.getLaborTotal(); return s;
    }
    public long getGrandTotal() {
        long s = 0; for (KalaTable t : tables) s += t.getGrandTotal(); return s;
    }
    public int getItemsCount() {
        int n = 0; for (KalaTable t : tables) n += t.getItems().size(); return n;
    }
}