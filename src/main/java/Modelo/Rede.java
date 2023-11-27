package Modelo;

import java.time.LocalDateTime;

public class Rede extends Leituras{
    private Double upload;
    private Double download;

    public Rede(Integer idLeitura, Integer fkComponente, String tipo, LocalDateTime dataLeitura, String tempoAtividade, Double upload, Double download) {
        super(idLeitura, fkComponente, tipo, dataLeitura, tempoAtividade);
        this.upload = upload;
        this.download = download;
    }
    public Rede() {
        super();
    }

    public Double getUpload() {
        return upload;
    }

    public void setUpload(Double upload) {
        this.upload = upload;
    }

    public Double getDownload() {
        return download;
    }

    public void setDownload(Double download) {
        this.download = download;
    }

    @Override
    public String toString() {
        return """
                %s
                Upload: %.2f
                Download: %.2f
                """.formatted(super.toString(), upload, download);
    }
}
