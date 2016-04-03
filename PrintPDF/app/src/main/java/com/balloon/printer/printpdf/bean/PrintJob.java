package com.balloon.printer.printpdf.bean;

/**
 * Created by whenb on 4/3/2016.
 */
public class PrintJob extends File2Print {

    private String printerAddress;

    public PrintJob() {}

    public PrintJob(String driveId, String filename, String mimeType, double fileSize) {
        super(driveId, filename, fileSize, mimeType);
    }

    public String getPrinterAddress() {
        return printerAddress;
    }

    public void setPrinterAddress(String printerAddress) {
        this.printerAddress = printerAddress;
    }


}
