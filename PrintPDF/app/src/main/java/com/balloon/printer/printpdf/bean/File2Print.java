package com.balloon.printer.printpdf.bean;

/**
 * Created by whenb on 4/3/2016.
 */
public class File2Print {
    private String driveId;
    private String filename;
    private double fileSize;

    public File2Print() {
    }

    public File2Print(String driveId, String filename, double fileSize, String mimeType) {
        this.driveId = driveId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    private String mimeType;

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }
}
