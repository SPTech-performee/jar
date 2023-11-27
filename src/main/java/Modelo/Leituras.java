package Modelo;


import java.time.LocalDateTime;


public abstract class Leituras {

    private Integer idLeitura;
    private Integer fkComponente;
    private String tipo;
    private LocalDateTime dataLeitura;
    private String tempoAtividade;

    public Leituras(Integer idLeitura, Integer fkComponente, String tipo, LocalDateTime dataLeitura, String tempoAtividade) {
        this.idLeitura = idLeitura;
        this.fkComponente = fkComponente;
        this.tipo = tipo;
        this.dataLeitura = dataLeitura;
        this.tempoAtividade = tempoAtividade;
    }

    public Leituras() {
    }


    public Integer getIdLeitura() {
        return idLeitura;
    }

    public void setIdLeitura(Integer idLeitura) {
        this.idLeitura = idLeitura;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getDataLeitura() {
        return dataLeitura;
    }

    public void setDataLeitura(LocalDateTime dataLeitura) {
        this.dataLeitura = dataLeitura;
    }

    public String getTempoAtividade() {
        return tempoAtividade;
    }

    public void setTempoAtividade(String tempoAtividade) {
        this.tempoAtividade = tempoAtividade;
    }

    public Integer getFkComponente() {
        return fkComponente;
    }

    public void setFkComponente(Integer fkComponente) {
        this.fkComponente = fkComponente;
    }

    @Override
    public String toString() {
        return """
               Tipo: %s
               fkComponente: %d
               ID Leitura: %d
               Data Leitura: %s
               Tempo Atividade: %s""".formatted(tipo, fkComponente, idLeitura, dataLeitura, tempoAtividade);
    }
}
