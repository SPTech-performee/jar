package Dao;

import Conexao.Conexao;
import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.Disco;
import com.github.britooo.looca.api.group.discos.DiscoGrupo;
import com.github.britooo.looca.api.group.memoria.Memoria;
import com.github.britooo.looca.api.group.processador.Processador;
import com.github.britooo.looca.api.group.rede.RedeInterface;
import com.github.britooo.looca.api.group.rede.RedeInterfaceGroup;
import com.github.britooo.looca.api.group.sistema.Sistema;
import com.github.britooo.looca.api.group.temperatura.Temperatura;
import com.github.britooo.looca.api.util.Conversor;
import com.slack.api.methods.SlackApiException;
import modelo.*;
import org.json.JSONObject;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import java.util.List;
import java.util.Scanner;

import static Dao.Slack.sendMessage;

public class DaoDados {
    private Looca looca = new Looca();
    private Sistema sistema = looca.getSistema();
    private Processador processador = looca.getProcessador();
    private Temperatura temp = new Temperatura();
    private Memoria memoria = looca.getMemoria();
    private String hostNameUser = looca.getRede().getParametros().getNomeDeDominio();
    private String ipUser = looca.getRede().getParametros().getServidoresDns().toString();
    private String ipServidor;
    private Integer fkEmpresa;
    private Integer fkDataCenter;
    private Integer idComponente;
    private Integer emitirAlerta = 10;
    private Integer tentativasAcesso = 1;

    public DaoDados(Looca looca, Sistema sistema, Processador processador, Temperatura temp, Memoria memoria,String hostNameUser, String ipUser, String ipServidor, Integer fkEmpresa, Integer fkDataCenter, Integer idComponente, Integer emitirAlerta, Integer tentativasAcesso) {
        this.looca = looca;
        this.sistema = sistema;
        this.processador = processador;
        this.temp = temp;
        this.memoria = memoria;
        this.hostNameUser = hostNameUser;
        this.ipUser = ipUser;
        this.ipServidor = ipServidor;
        this.fkEmpresa = fkEmpresa;
        this.fkDataCenter = fkDataCenter;
        this.idComponente = idComponente;
        this.emitirAlerta = emitirAlerta;
        this.tentativasAcesso = tentativasAcesso;
    }

    public DaoDados() {
    }

    public void setLog(String descricao) {

        FileWriter arq = null;

        try {
            // Cria o FileWriter
            arq = new FileWriter("E:\\Meus arquivos\\GitHub\\GitHub 2 sem\\performee-jar\\performee_log.txt", true);

            LocalDateTime currentDate = LocalDateTime.now();
            String formattedDateTime = currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

            // Escreve no arquivo
            arq.write(formattedDateTime + descricao + "\n");

            arq.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Fecha o FileWriter no bloco finally
                if (arq != null) {
                    arq.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean buscarIp(String ipServer) {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer verificacaoIp = con.queryForObject("SELECT COUNT(*) FROM Servidor where IpServidor = ?", Integer.class, ipServer);

        if (verificacaoIp != 0) {

            ipServidor = ipServer;
            fkEmpresa = con.queryForObject("SELECT fkEmpresa FROM Servidor where IpServidor = ?", Integer.class, ipServidor);
            fkDataCenter = con.queryForObject("SELECT fkDataCenter FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Acessou o Servidor de IP: %s com Sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);

            return true;
        } else {

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou acessar um servidor inexistente pela %d vez!""".formatted(ipUser, hostNameUser, tentativasAcesso);
            setLog(descricao);
            tentativasAcesso++;
            return false;
        }
    }

    public void inserirComponente() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer count = con.queryForObject("SELECT COUNT(*) FROM Componente where tipo != 'GPU' and fkServidor = ?", Integer.class, ipServidor);

        if (count != 0) {
            System.out.println("""
                    Já existe %d componentes cadastrado!""".formatted(count));

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou cadastrar os componentes do servidor de IP: %s, mas já existe componentes cadastrados""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);

        } else {
            switch (1) {
                case 1: {
                    String modelo = processador.getNome();
                    double capacidadeTotal = processador.getNumeroCpusLogicas();

                    System.out.println("Salvando dados da CPU....");
                    con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "CPU", modelo, capacidadeTotal, 1, fkEmpresa, fkDataCenter, ipServidor);

                }
                case 2: {
                    String modelo = "Memoria RAM";                  //divide bytes em gb
                    Double capacidadeTotal = (memoria.getTotal() / 1073741824.0);

                    System.out.println("Salvando dados da RAM....");
                    con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "RAM", modelo, capacidadeTotal, 3, fkEmpresa, fkDataCenter, ipServidor);

                }
                case 3: {

                    String modelo;
                    Double capacidadeTotal;

                    //Criação do gerenciador
                    DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                    //Obtendo lista de discos a partir do getter
                    List<Disco> discos = grupoDeDiscos.getDiscos();
                    System.out.println("Salvando dados do disco....");
                    for (Disco disco : discos) {
                        modelo = disco.getModelo();            //divide bytes em gb
                        capacidadeTotal = disco.getTamanho() / 1073741824.0;
                        con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "Disco", modelo, capacidadeTotal, 3, fkEmpresa, fkDataCenter, ipServidor);
                        break;
                    }

                }
                case 4: {
                    String modelo;
                    Double capacidadeTotal;

                    //Criação do gerenciador
                    RedeInterfaceGroup grupoDeRedes = looca.getRede().getGrupoDeInterfaces();

                    //Obtendo lista de rede a partir do getter
                    List<RedeInterface> redes = grupoDeRedes.getInterfaces();

                    System.out.println("Salvando dados da rede....");
                    for (RedeInterface rede : redes) {
                        modelo = rede.getNomeExibicao();             //bytes em mb
                        capacidadeTotal = (rede.getBytesEnviados() / 1048576.0) + (rede.getBytesRecebidos() / 1048576.0);
                        con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "Rede", modelo, capacidadeTotal, 4, fkEmpresa, fkDataCenter, ipServidor);
                        break;
                    }
                }
            }
            System.out.println("Dados enviado com sucesso!");
            String descricao = """
                    : Usuário do IP %s, hostName: %s. Todos componentes do servidor de IP: %s cadastrados com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);
        }
    }

    public void atualizarComponete(Integer opcao) {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer count = con.queryForObject("SELECT COUNT(*) FROM Componente where fkServidor = ?", Integer.class, ipServidor);
        if (count == 0) {
            System.out.println("""
                    Não existe componentes cadastrados nesse servidor!
                    Cadastrando agora...""");

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou atualizar componentes do servidor de IP: %s, mas não existe componentes cadastrado para atualizar!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);

            inserirComponente();
        } else {
            switch (opcao) {
                case 1: {
                    String modelo = processador.getNome();
                    double capacidadeTotal = processador.getNumeroCpusLogicas();

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

                    System.out.println("Atualizando dados da CPU....");
                    con.update("update Componente set modelo = ?, capacidadeTotal = ? where idComponente = ?", modelo, capacidadeTotal, idComponente);
                    break;
                }
                case 2: {
                    String modelo = "Memoria RAM";                  //divide bytes em gb
                    Double capacidadeTotal = (memoria.getTotal() / 1073741824.0);

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);

                    System.out.println("Atualizando dados da RAM....");
                    con.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponente);
                    break;
                }
                case 3: {

                    System.out.println("Deletando os dados relacionado aos componentes, aguarde....");

                    int rowsAffectedAlerta = con.update("DELETE FROM alerta WHERE fkEmpresa = ? AND fkDataCenter = ? AND fkServidor = ?", fkEmpresa, fkDataCenter, ipServidor);
                    int rowsAffectedLeitura = con.update("DELETE FROM leitura WHERE fkEmpresa = ? AND fkDataCenter = ? AND fkServidor = ?", fkEmpresa, fkDataCenter, ipServidor);
                    int rowsAffectedComponente = con.update("DELETE FROM componente WHERE fkEmpresa = ? AND fkDataCenter = ? AND fkServidor = ?", fkEmpresa, fkDataCenter, ipServidor);

                    System.out.println("Salvando os novos Componentes");
                    inserirComponente();
                    break;
                }
                case 4: {
                    System.out.println("Voltando para o inicio...");
                    break;
                }
                default: {
                    System.out.println("Opção inválida! digite novamente");
                }
            }
        }
    }

    public void inserirLeitura() throws IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        Integer opcao = 1;

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            switch (opcao) {
                case 1: {
                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

                    Double emUso = processador.getUso();
                    String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                    Double temperatura = temp.getTemperatura();
                    Double frequencia = (double) processador.getFrequencia() / 1000000000.0;

                    con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkMedidaTemp, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,?,?,5,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresa, fkDataCenter, ipServidor, idComponente);

                    System.out.println("Enviando Leitura da CPU");
                }
                case 2: {
                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);

                    Double emUso = memoria.getEmUso() / 1073741824.0;
                    String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                    Double temperatura = null;
                    Double frequencia = null;

                    Integer tamanhoAtualRam = con.queryForObject("select capacidadeTotal from componente where tipo = 'Ram' and fkServidor = ?", Integer.class, ipServidor);

                    con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,?,?,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresa, fkDataCenter, ipServidor, idComponente);
                    System.out.println("Enviando Leitura da RAM");

                }
                case 3: {

                    //Criação do gerenciador
                    DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                    //Obtendo lista de discos a partir do getter
                    List<Disco> discos = grupoDeDiscos.getDiscos();

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);


                    for (Disco arm : discos) {
                        for (FileStore store : FileSystems.getDefault().getFileStores()) {
                            try {
                                long total = store.getTotalSpace() / 1024 / 1024 / 1024;
                                long usado = (store.getTotalSpace() - store.getUnallocatedSpace()) / 1024 / 1024 / 1024;

                                double porcUso = (double) usado / total * 100;

                                Double emUso = porcUso;
                                String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                                //bytes em mb
                                Double velocidadeLeitura = arm.getBytesDeLeitura() / 1048576.0;
                                Double velocidadeEscrita = arm.getBytesDeEscritas() / 1048576.0;

                                con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, velocidadeLeitura, velocidadeEscrita, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, velocidadeLeitura, velocidadeEscrita, fkEmpresa, fkDataCenter, ipServidor, idComponente);

                            } catch (IOException e) {
                                System.err.println(e);
                            }
                            break;
                        }
                        break;
                    }
                    System.out.println("Enviando Leitura do Disco");
                }
                case 4: {
                    //Criação do gerenciador
                    RedeInterfaceGroup grupoDeRedes = looca.getRede().getGrupoDeInterfaces();

                    //Obtendo lista de discos a partir do getter
                    List<RedeInterface> GpRede = grupoDeRedes.getInterfaces();


                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);


                    for (RedeInterface rede : GpRede) {
                        Double emUso = null;
                        String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                        //bytes em mb
                        Double upload = rede.getBytesEnviados() / 1e7;
                        Double download = rede.getBytesRecebidos() / 1e7;


                        con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, upload, download, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, upload, download, fkEmpresa, fkDataCenter, ipServidor, idComponente);
                        break;
                    }
                }
                System.out.println("Enviando Leitura da Rede");
            }
            try {
                alertaCpu();
                alertaRam();
            } catch (SlackApiException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (emitirAlerta == 0) {
                try {
                    alertaDisco();
                    alertaRede();
                } catch (SlackApiException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                emitirAlerta = 10;
            } else {
                emitirAlerta--;
            }
        };
        executor.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }


    public List<Componentes> exibirComponentes() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Componente where fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Componentes.class));

    }

    public List<Cpu> exibirCpu() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'CPU' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Cpu.class));
    }

    public List<Ram> exibirRam() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'RAM' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Ram.class));
    }

    public List<Disk> exibirDisco() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'DISCO' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Disk.class));
    }

    public List<Rede> exibirRede() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'REDE' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Rede.class));
    }

    public void alertaCpu() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JSONObject json = new JSONObject();
        JSONObject json2 = new JSONObject();

        //CPU
        Double mediaUsoCpu = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double temperatura = con.queryForObject("SELECT ROUND(AVG(temperatura), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT temperatura\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkCpu = con.queryForObject("select idComponente from componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

        String componente = "CPU";
        String tipo;
        String descricao;
        String descricao2;
        Integer dias = 10;

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'CPU' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);


        if (mediaUsoCpu >= 85) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização da %s esteve constantemente acima de 85%%, nas últimas %d verificação. Pode ocorrer Travamentos! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpu);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);


        } else if (mediaUsoCpu >= 66 && mediaUsoCpu <= 84) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização da %s esteve constantemente entre 66%% a 84%%, nas últimas %d verificação. Pode ocorrer lentidão! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpu);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

        } else {
            descricao = String.format("Alerta Estável. Servidor %s: A utilização da %s está abaixo de 66%%, últimas %d verificação. A utilização está boa! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpu);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);
        }
        json.put("text", descricao);

        Slack.sendMessage(json);

        if (temperatura > 39) {
            descricao2 = String.format("Alerta de Risco. Servidor %s: A Temperatura da %s está acima de 39°C, nas últimas %d verificação! Risco de Super Aquecimento!. Média de temperatura: %.2f°C", ipServidor, componente, dias, temperatura);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);


        } else if (temperatura >= 35 && temperatura <= 39) {
            descricao2 = String.format("Alerta de Cuidado. Servidor %s: A Temperatura da %s está entre 35°C a 39°C, nas últimas %d verificação. Pode ocorrer aquecimento! média de temperatura: %.2f°C", ipServidor, componente, dias, temperatura);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

        } else {

            descricao2 = String.format("Alerta Estável. Servidor %s: A Temperatura da %s está abaixo de 35°C, nas últimas %d verificação. Temperatura está OK! Média de temperatura: %.2f°C", ipServidor, componente, dias, temperatura);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

        }

        json2.put("text", descricao2);

        Slack.sendMessage(json2);

    }

    public void alertaRam() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JSONObject json = new JSONObject();

        //RAM
        Double mediaUsoRam = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Ram' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double capacidadeTotalRam = con.queryForObject("SELECT c.capacidadeTotal as capacidade FROM Componente as c\n" +
                "    WHERE fkServidor = ? AND c.tipo = 'RAM';", Double.class, ipServidor);

        Integer fkCpu = con.queryForObject("select idComponente from componente where tipo = 'Ram' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Ram' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);

        String componente = "Ram";
        String tipo;
        String descricao;
        Integer dias = 10;


        if (mediaUsoRam > (capacidadeTotalRam * .85)) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização da %s está constantemente acima de 85%% da memória total, nas ultimas %d verificação. Risco de travamento! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRam);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);


        } else if (mediaUsoRam <=(capacidadeTotalRam * .85) && mediaUsoRam >= (capacidadeTotalRam * .66)) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização da %s está ocilando entre 66%% a 85%% da memória total, nas ultimas %d verificação. Pode ocorrer lentidão! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRam);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

        } else {
                descricao = String.format("Alerta Estável. Servidor %s: A utilização da %s está abaixo de 66%% da memória total, ultimas %d verificação. A Ram está OK! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRam);

                tipo = "Estável";

                con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);
        }
        json.put("text", descricao);

        Slack.sendMessage(json);

    }

    public void alertaDisco() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JSONObject json = new JSONObject();

        //Disco
        Double mediaUsoDisk = con.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkDisco = con.queryForObject("select idComponente from componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Disco' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);

        String componente = "Disco";
        String tipo;
        String descricao;
        Integer dias = 10;


        if (mediaUsoDisk > 85) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização do %s está acima de 85%% do armazenamento total, nas ultimas %d verificação. Pouco espaço no disco! média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDisk);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);


        } else if (mediaUsoDisk >= 66 && mediaUsoDisk <= 85) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização do %s está entre de 66%% a 85%% do armazenamento total, nas ultimas %d verificação. Espaço razoável no disco! média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDisk);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);

        } else {

            descricao = String.format("Alerta Estável. Servidor %s: A utilização do %s está abaixo de 66%% do armazenamento total, ultimas %d verificação. Espaço no disco OK! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDisk);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);

        }
        json.put("text", descricao);

        Slack.sendMessage(json);

    }

    public void alertaRede() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JSONObject json = new JSONObject();
        JSONObject json2 = new JSONObject();

        //rede
        Double mediaUsoRedeUp = con.queryForObject("SELECT ROUND(AVG(upload), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT upload\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double mediaUsoRedeDow = con.queryForObject("SELECT ROUND(AVG(download), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT download\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                "    LIMIT 10\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkRede = con.queryForObject("select idComponente from componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM leitura as l\n" +
                "\tJOIN componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = 'Rede' and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);

        String componente = "Rede";
        String tipo;
        String descricao;
        String descricao2;
        Integer dias = 10;




        if (mediaUsoRedeUp < 20) {
            descricao = String.format("Alerta de Risco. Servidor %s: O upload da %s está abaixo de 20Mbs, nas ultimas %d verificação. A rede está lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeUp);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);


        } else if (mediaUsoRedeUp <= 89 && mediaUsoRedeUp >= 20) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: O upload da %s está entre 20Mbs a 89Mbs, nas ultimas %d verificação. A rede Pode ficar Lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeUp);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);

        } else {
            descricao = String.format("Alerta Estável. Servidor %s: O upload da %s está acima dos 89Mbs, ultimas %d verificação. A rede está boa! Média de utilização: %.2fGBs", ipServidor, componente, dias, mediaUsoRedeUp);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
        }


        if (mediaUsoRedeDow < 40) {
            descricao2 = String.format("Alerta de Risco. Servidor %s: O download da %s está abaixo de 40Mbs, nas ultimas %d verificação. A rede está lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDow);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);


        } else if (mediaUsoRedeDow <= 89 && mediaUsoRedeDow >= 40) {
            descricao2 = String.format("Alerta de Cuidado. Servidor %s: O download da %s está entre 40Mbs a 89Mbs, nas ultimas %d verificação. A rede Pode ficar Lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDow);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);

        } else {
            descricao2 = String.format("Alerta Estável. Servidor %s: O download da %s está acima dos 89Mbs, ultimas %d verificação. A rede está boa! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDow);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
        }


        json.put("text", descricao);
        json2.put("text", descricao2);

        Slack.sendMessage(json);
        Slack.sendMessage(json2);

    }

    public String getHostNameUser() {
        return hostNameUser;
    }

    public void setHostNameUser(String hostNameUser) {
        this.hostNameUser = hostNameUser;
    }

    public String getIpUser() {
        return ipUser;
    }

    public void setIpUser(String ipUser) {
        this.ipUser = ipUser;
    }

    public Looca getLooca() {
        return looca;
    }

    public void setLooca(Looca looca) {
        this.looca = looca;
    }

    public Sistema getSistema() {
        return sistema;
    }

    public void setSistema(Sistema sistema) {
        this.sistema = sistema;
    }

    public Processador getProcessador() {
        return processador;
    }

    public void setProcessador(Processador processador) {
        this.processador = processador;
    }

    public Temperatura getTemp() {
        return temp;
    }

    public void setTemp(Temperatura temp) {
        this.temp = temp;
    }

    public Memoria getMemoria() {
        return memoria;
    }

    public void setMemoria(Memoria memoria) {
        this.memoria = memoria;
    }

    public String getIpServidor() {
        return ipServidor;
    }

    public void setIpServidor(String ipServidor) {
        this.ipServidor = ipServidor;
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

    public Integer getIdComponente() {
        return idComponente;
    }

    public void setIdComponente(Integer idComponente) {
        this.idComponente = idComponente;
    }

    public Integer getEmitirAlerta() {
        return emitirAlerta;
    }

    public void setEmitirAlerta(Integer emitirAlerta) {
        this.emitirAlerta = emitirAlerta;
    }

    public Integer getTentativasAcesso() {
        return tentativasAcesso;
    }

    public void setTentativasAcesso(Integer tentativasAcesso) {
        this.tentativasAcesso = tentativasAcesso;
    }

    @Override
    public String toString() {
        return "DaoDados{" +
                "looca=" + looca +
                ", sistema=" + sistema +
                ", processador=" + processador +
                ", temp=" + temp +
                ", memoria=" + memoria +
                ", hostNameUser='" + hostNameUser + '\'' +
                ", ipUser='" + ipUser + '\'' +
                ", ipServidor='" + ipServidor + '\'' +
                ", fkEmpresa=" + fkEmpresa +
                ", fkDataCenter=" + fkDataCenter +
                ", idComponente=" + idComponente +
                ", emitirAlerta=" + emitirAlerta +
                ", tentativasAcesso=" + tentativasAcesso +
                '}';
    }
}

