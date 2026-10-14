package ir.arandy.naghshan.model;

import java.io.Serializable;

/** اطلاعات کلی طرح استخراج‌شده از PDF. */
public class ProjectInfo implements Serializable {

    private String projectNumber;    // شماره طرح  (S05472262)
    private String applicantName;    // متقاضی     (عبدالله شاهزاده)
    private String requestNumber;    // شماره درخواست (472415617)

    public ProjectInfo() { }

    public ProjectInfo(String projectNumber, String applicantName, String requestNumber) {
        this.projectNumber = projectNumber;
        this.applicantName = applicantName;
        this.requestNumber = requestNumber;
    }

    public String getProjectNumber() { return projectNumber; }
    public void   setProjectNumber(String v) { this.projectNumber = v; }

    public String getApplicantName() { return applicantName; }
    public void   setApplicantName(String v) { this.applicantName = v; }

    public String getRequestNumber() { return requestNumber; }
    public void   setRequestNumber(String v) { this.requestNumber = v; }

    /**
     * نام طرح را از ترکیب نام متقاضی و شماره درخواست می‌سازد.
     * نمونه: "عبدالله پاشازاده، احمد رحیمی - 472415617، 472401005"
     */
    public String getProjectName() {
        String name = applicantName == null ? "" : applicantName.trim();
        String req  = requestNumber == null ? "" : requestNumber.trim();
        if (name.isEmpty() && req.isEmpty()) return "-";
        if (req.isEmpty())  return name;
        if (name.isEmpty()) return req;
        return name + " - " + req;
    }

    public boolean isValid() {
        return projectNumber != null && !projectNumber.isEmpty();
    }
}