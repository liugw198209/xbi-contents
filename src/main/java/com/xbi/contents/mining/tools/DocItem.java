package com.xbi.contents.mining.tools;

/**
 * Created by usr0101862 on 2016/06/05.
 */
public class DocItem {
    private String docId;
    private String docContent;
    private String docTitle;
    private String docCategory;

    public DocItem(String docId, String docContent) {
        this.docId = docId;
        this.docContent = docContent;
    }

    public DocItem(String docId, String docContent, String docTitle) {
        this.docId = docId;
        this.docContent = docContent;
        this.setDocTitle(docTitle);
    }

    public DocItem(String docId, String docContent, String docTitle, String docCategory) {
        this.docId = docId;
        this.docContent = docContent;
        this.setDocTitle(docTitle);
        this.setDocCategory(docCategory);
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getDocContent() {
        return docContent;
    }

    public void setDocContent(String docContent) {
        this.docContent = docContent;
    }


    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getDocCategory() {
        return docCategory;
    }

    public void setDocCategory(String docCategory) {
        this.docCategory = docCategory;
    }
}
