package Modelo;

import java.time.LocalDateTime;

public class Cpu extends Leituras{
    private Double emUso;
    private Double temperatura;
    private Double frequencia;

    public Cpu(Integer idLeitura, Integer fkComponente, String tipo, LocalDateTime dataLeitura, String tempoAtividade, Double emUso, Double temperatura, Double frequencia) {
        super(idLeitura, fkComponente, tipo, dataLeitura, tempoAtividade);
        this.emUso = emUso;
        this.temperatura = temperatura;
        this.frequencia = frequencia;
    }
    public Cpu() {
        super();
    }

    public Double getEmUso() {
        return emUso;
    }

    public void setEmUso(Double emUso) {
        this.emUso = emUso;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Double getFrequencia() {
        return frequencia;
    }

    public void setFrequencia(Double frequencia) {
        this.frequencia = frequencia;
    }

    @Override
    public String toString() {
        return """
                %s
                emUso: %.2f%%
                temperatura: %.2f°C
                frequencia: %.2fGHz
                """.formatted(super.toString(), emUso, temperatura, frequencia);
    }
}




