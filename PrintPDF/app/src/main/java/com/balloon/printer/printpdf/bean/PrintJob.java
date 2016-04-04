package com.balloon.printer.printpdf.bean;

/**
 * Created by whenb on 4/3/2016.
 */
public class PrintJob  {

    private String printerAddress;
    private String deviceAddress;
    private File2Print file2Print;

    public PrintJob() {}

    public String getPrinterAddress() {
        return printerAddress;
    }

    public void setPrinterAddress(String printerAddress) {
        this.printerAddress = printerAddress;
    }


    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public File2Print getFile2Print() {
        return file2Print;
    }

    public void setFile2Print(File2Print file2Print) {
        this.file2Print = file2Print;
    }
}
