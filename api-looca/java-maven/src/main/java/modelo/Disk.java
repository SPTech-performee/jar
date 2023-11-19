package modelo;

import java.time.LocalDateTime;

public class Disk extends Leituras{
    private Double velocidadeLeitura;
    private Double velocidadeEscrita;

    public Disk(Integer idLeitura, Integer fkComponente, String tipo, LocalDateTime dataLeitura, String tempoAtividade, Double velocidadeLeitura, Double velocidadeEscrita) {
        super(idLeitura, fkComponente, tipo, dataLeitura, tempoAtividade);
        this.velocidadeLeitura = velocidadeLeitura;
        this.velocidadeEscrita = velocidadeEscrita;
    }

    public Disk() {
        super();
    }

    public Double getVelocidadeLeitura() {
        return velocidadeLeitura;
    }

    public void setVelocidadeLeitura(Double velocidadeLeitura) {
        this.velocidadeLeitura = velocidadeLeitura;
    }

    public Double getVelocidadeEscrita() {
        return velocidadeEscrita;
    }

    public void setVelocidadeEscrita(Double velocidadeEscrita) {
        this.velocidadeEscrita = velocidadeEscrita;
    }

    @Override
    public String toString() {
        return """
                %s
                velocidade Leitura: %.2fMBs
                velocidade Escrita: %.2fMBs
                """.formatted(super.toString(), velocidadeLeitura, velocidadeEscrita);
    }
}
