package Dao;
import Conexao.Conexao;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.google.gson.JsonObject;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.net.URL;

public class Alerta {

    DaoDados dao = new DaoDados();
    private String fkServidor;
    private Integer fkEmpresa;
    private Integer fkDataCenter;
    private Integer fkComponente;

    public Alerta(String fkServidor, Integer fkEmpresa, Integer fkDataCenter, Integer fkComponente) {
        this.fkServidor = fkServidor;
        this.fkEmpresa = fkEmpresa;
        this.fkDataCenter = fkDataCenter;
        this.fkComponente = fkComponente;
    }
    public Alerta() {
    }

    public void executaAlerta(String ipServidor, Integer fkEmp, Integer fkDataC, Integer fkComp) throws IOException, SlackApiException {
        String slackToken = "xoxb-6181502641763-6204598337072-YvyYD55fyxIjrKMJAHkjSz7o";
        Slack slack = Slack.getInstance();
        String nomeDoBot = "performee.";
        String idDoCanal = "A064X25E32T";

        fkServidor = ipServidor;
        fkEmpresa = fkEmp;
        fkDataCenter = fkDataC;
        fkComponente = fkComp;

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        //CPU
        Double mediaUso = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, fkServidor);

        Double mediaTemp = con.queryForObject("SELECT ROUND(AVG(temperatura), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT temperatura\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, fkServidor);

        //RAM
        Double mediaUsoRam = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras FROM (SELECT emUso FROM leitura AS l JOIN componente AS c ON c.idComponente = l.fkComponente WHERE c.tipo = 'Ram' AND l.fkServidor = ? ORDER BY l.idLeitura DESC LIMIT 10) AS ultimas_leituras;", Double.class, fkServidor);


        //Disco
        Double mediaUsoDisk = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, fkServidor);

        //rede
        Double mediaUsoRedeUp = con.queryForObject("SELECT ROUND(AVG(upload), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT upload\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, fkServidor);


        JsonObject dadosDoServidor = new JsonObject();
        JsonObject dadosDoServidorRam = new JsonObject();
        dadosDoServidor.addProperty("utilizaçãoCpu", mediaUso);
        dadosDoServidor.addProperty("temperaturaCpu", mediaTemp);

        dadosDoServidor.addProperty("utilizaçãoDisco", mediaUsoDisk);
        dadosDoServidor.addProperty("utilizaçãoRedeUp", mediaUsoRedeUp);

        dadosDoServidorRam.addProperty("utilizacaoRam", mediaUsoRam);


        dadosDoServidor.addProperty("Verificação", 10);

        // URL para envio da mensagem (substitua pela URL real)
        URL urlDoCanal = new URL("https://hooks.slack.com/services/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");

        sendMensagem(urlDoCanal, slackToken, "#channel-performee", construirMensagemUsoCpu(dadosDoServidor));
        sendMensagem(urlDoCanal, slackToken, "#channel-performee", construirMensagemTempCpu(dadosDoServidor));
        // sendMensagem(urlDoCanal, slackToken, "#channel-performee", construirMensagemUsoRam(dadosDoServidorRam));
        sendMensagem(urlDoCanal, slackToken, "#channel-performee", construirMensagemUsoDisco(dadosDoServidor));
       // sendMensagem(urlDoCanal, slackToken, "#channel-performee", construirMensagemUsoRedeUp(dadosDoServidor));

    }

    public  void sendMensagem(URL url, String token, String channel, String mensagem) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();

        // Crie uma instância do método de envio de mensagens
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(channel)
                .text(mensagem)
                .build();

        // Envie a mensagem
        ChatPostMessageResponse response = slack.methods(token).chatPostMessage(request);

        // Verifique se a mensagem foi enviada com sucesso
        if (response.isOk()) {
            System.out.println("Mensagem enviada com sucesso!");
        } else {
            System.err.println("Erro ao enviar mensagem: " + response.getError());
        }
    }

    public  String construirMensagemUsoCpu(JsonObject dadosDoServidor) {
        String componente = "CPU";
        double utilizacao = dadosDoServidor.get("utilizaçãoCpu").getAsDouble();
        int dias = dadosDoServidor.get("Verificação").getAsInt();

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer fkCpu = con.queryForObject("select idComponente from componente where tipo = 'Disco' and fkServidor = ?", Integer.class, fkServidor);

        String tipo;
        String descricao;

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'CPU' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, fkServidor);
        
        if (utilizacao >= 85) {
            descricao = String.format("Alerta de Risco: A utilização da %s esteve constantemente acima de 85%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;

        } else if (utilizacao > 66) {
            descricao = String.format("Alerta de Cuidado: A utilização da %s esteve constantemente acima de 66%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;
        } else {

            descricao = String.format("Alerta Estável: A utilização da %s está estável ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;
        }

    }

    public  String construirMensagemTempCpu(JsonObject dadosDoServidor) {
        String componente = "CPU";
        double temperatura = dadosDoServidor.get("temperaturaCpu").getAsDouble();
        int dias = dadosDoServidor.get("Verificação").getAsInt();

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer fkCpu = con.queryForObject("select idComponente from componente where tipo = 'Disco' and fkServidor = ?", Integer.class, fkServidor);

        String tipo;
        String descricao;

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'CPU' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, fkServidor);

        if (temperatura > 39) {
            descricao = String.format("Alerta de Risco: A Temperatura da %s esteve constantemente acima de 39°C nas ultimas %d verificação! média de temperatura: %.2f°C", componente, dias, temperatura);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;

        } else if (temperatura > 35) {
            descricao = String.format("Alerta de Cuidado: A Temperatura da %s esteve constantemente acima de 35°C nas ultimas %d verificação! média de temperatura: %.2f°C", componente, dias, temperatura);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;
        } else {

            descricao = String.format("Alerta Estável: A Temperatura da %s está estável ultimas %d verificação! média de temperatura: %.2f°C", componente, dias, temperatura);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkCpu, fkLeitura);
            return descricao;
        }

    }

    /* public String construirMensagemUsoRam(JsonObject dadosDoServidorRam) {
        String componente = "Ram";
        int utilizacao = dadosDoServidorRam.get("utilizacaoRam").getAsInt();
        int dias = 10;

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        String tipo;
        String descricao;

        Integer fkRam = con.queryForObject("select idComponente from componente where tipo = 'Ram' and fkServidor = ?", Integer.class, fkServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Ram' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, fkServidor);

        System.out.println(utilizacao);

        System.out.println(fkRam);
        System.out.println(fkLeitura);

        if (utilizacao >= 85) {
            descricao = String.format("Alerta de Risco: A utilização da %s esteve constantemente acima de 85%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkRam, fkLeitura);
            return descricao;

        } else if (utilizacao > 66) {
            descricao = String.format("Alerta de Cuidado: A utilização da %s esteve constantemente acima de 66%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkRam, fkLeitura);
            return descricao;
        } else {

            descricao = String.format("Alerta Estável: A utilização da %s está estável ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkRam, fkLeitura);
            return descricao;
        }

    }

     */

    public  String construirMensagemUsoDisco(JsonObject dadosDoServidor) {
        String componente = "Disco";
        double utilizacao = dadosDoServidor.get("utilizaçãoDisco").getAsDouble();
        int dias = dadosDoServidor.get("Verificação").getAsInt();

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        String tipo;
        String descricao;
        Integer fkDisco = con.queryForObject("select idComponente from componente where tipo = 'Disco' and fkServidor = ?", Integer.class, fkServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Disco' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, fkServidor);

        if (utilizacao >= 85) {
            descricao = String.format("Alerta de Risco: A utilização da %s esteve constantemente acima de 85%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;

        } else if (utilizacao > 66) {
            descricao = String.format("Alerta de Cuidado: A utilização da %s esteve constantemente acima de 66%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;
        } else {

            descricao = String.format("Alerta Estável: A utilização da %s está estável ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;
        }

    }

  /*  public  String construirMensagemUsoRedeUp(JsonObject dadosDoServidor) {
        String componente = "Rede";
        double utilizacao = dadosDoServidor.get("utilizaçãoRedeUp").getAsDouble();
        int dias = dadosDoServidor.get("Verificação").getAsInt();

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        String tipo;
        String descricao;

        Double totalUp = con.queryForObject("select upload form Leitura as l join Componente as c on c.idComponente = l.fkComponente where c.tipo = 'Rede' and l.fkLeitura = ? order by l.idLeitura desc limit 1", Double.class, fkServidor);

        Double compa = totalUp - utilizacao;

        Integer fkDisco = con.queryForObject("select idComponente from componente where tipo = 'Rede' and fkServidor = ?", Integer.class, fkServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Rede' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, fkServidor);

        if (compa <= 20) {
            descricao = String.format("Alerta de Risco: A utilização da %s esteve constantemente acima de 85%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;

        } else if (compa < 89) {
            descricao = String.format("Alerta de Cuidado: A utilização da %s esteve constantemente acima de 66%% nas ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;
        } else {

            descricao = String.format("Alerta Estável: A utilização da %s está estável ultimas %d verificação! média de utilização: %.2f%%", componente, dias, utilizacao);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkDisco, fkLeitura);
            return descricao;
        }

    }

   */



    public String getFkServidor() {
        return fkServidor;
    }

    public void setFkServidor(String fkServidor) {
        this.fkServidor = fkServidor;
    }

    public Integer getFkEmpresa() {
        return fkEmpresa;
    }

    public void setFkEmpresa(Integer fkEmpresa) {
        this.fkEmpresa = fkEmpresa;
    }

    public Integer getFkDataCenter() {
        return fkDataCenter;
    }

    public void setFkDataCenter(Integer fkDataCenter) {
        this.fkDataCenter = fkDataCenter;
    }

    public Integer getFkComponente() {
        return fkComponente;
    }

    public void setFkComponente(Integer fkComponente) {
        this.fkComponente = fkComponente;
    }

    @Override
    public String toString() {
        return "Alerta{" +
                "fkServidor='" + fkServidor + '\'' +
                ", fkEmpresa=" + fkEmpresa +
                ", fkDataCenter=" + fkDataCenter +
                ", fkComponente=" + fkComponente +
                '}';
    }
}
