package ir.arandy.naghshan.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** مدل اصلی خروجی Parser: اطلاعات طرح + لیست کالاها. */
public class Invoice implements Serializable {

    private ProjectInfo projectInfo = new ProjectInfo();
    private List<Kala> items = new ArrayList<>();

    /** درصد آخرین تغییرات اعمال‌شده (مثلاً 15 یعنی 15%). منفی = کاهش. */
    private double appliedPercent = 0.0;
    private boolean applyToMaterial = true;
    private boolean applyToLabor    = true;

    public ProjectInfo getProjectInfo() { return projectInfo; }
    public void setProjectInfo(ProjectInfo p) { this.projectInfo = p; }

    public List<Kala> getItems() { return items; }
    public void setItems(List<Kala> v) { this.items = v; }

    public double getAppliedPercent() { return appliedPercent; }
    public void setAppliedPercent(double v) { appliedPercent = v; }

    public boolean isApplyToMaterial() { return applyToMaterial; }
    public void setApplyToMaterial(boolean v) { applyToMaterial = v; }

    public boolean isApplyToLabor() { return applyToLabor; }
    public void setApplyToLabor(boolean v) { applyToLabor = v; }

    public long getMaterialTotal() {
        long s = 0; for (Kala k : items) s += k.getMaterialPrice(); return s;
    }
    public long getLaborTotal() {
        long s = 0; for (Kala k : items) s += k.getLaborPrice(); return s;
    }
    public long getGrandTotal() {
        long s = 0; for (Kala k : items) s += k.getTotalPrice(); return s;
    }
}