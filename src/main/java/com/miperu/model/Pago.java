package com.miperu.model;

public class Pago {
    private String id;
    private String idAlumno;
    private double monto;
    private String concepto;
    private String fechaVencimiento;
    private boolean pagado;
    private String fechaPago;

    public Pago(String id, String idAlumno, double monto, String concepto, String fechaVencimiento, boolean pagado, String fechaPago) {
        this.id = id;
        this.idAlumno = idAlumno;
        this.monto = monto;
        this.concepto = concepto;
        this.fechaVencimiento = fechaVencimiento;
        this.pagado = pagado;
        this.fechaPago = fechaPago;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdAlumno() {
        return idAlumno;
    }

    public void setIdAlumno(String idAlumno) {
        this.idAlumno = idAlumno;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(String fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public boolean isPagado() {
        return pagado;
    }

    public void setPagado(boolean pagado) {
        this.pagado = pagado;
    }

    public String getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(String fechaPago) {
        this.fechaPago = fechaPago;
    }
}
