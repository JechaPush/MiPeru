package Models;

public class Nota {
    private String id;
    private String idAlumno;
    private String idCurso;
    private double valorNumerico;
    private String valorLetra; // AD, A, B, C
    private String periodo;

    public Nota(String id, String idAlumno, String idCurso, double valorNumerico, String valorLetra, String periodo) {
        this.id = id;
        this.idAlumno = idAlumno;
        this.idCurso = idCurso;
        this.valorNumerico = valorNumerico;
        this.valorLetra = valorLetra;
        this.periodo = periodo;
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

    public String getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(String idCurso) {
        this.idCurso = idCurso;
    }

    public double getValorNumerico() {
        return valorNumerico;
    }

    public void setValorNumerico(double valorNumerico) {
        this.valorNumerico = valorNumerico;
    }

    public String getValorLetra() {
        return valorLetra;
    }

    public void setValorLetra(String valorLetra) {
        this.valorLetra = valorLetra;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }
}
