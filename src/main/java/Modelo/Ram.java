package Modelo;

import java.time.LocalDateTime;

public class Ram extends Leituras{
    private Double emUso;

    public Ram(Integer idLeitura, Integer fkComponente, String tipo, LocalDateTime dataLeitura, String tempoAtividade, Double emUso) {
        super(idLeitura, fkComponente, tipo, dataLeitura, tempoAtividade);
        this.emUso = emUso;
    }
    public Ram() {
        super();
    }

    public Double getEmUso() {
        return emUso;
    }

    public void setEmUso(Double emUso) {
        this.emUso = emUso;
    }

    @Override
    public String toString() {
        return """
                %s
                em Uso: %.2fGBs
                """.formatted(super.toString(), emUso);
    }
}
