package Dao;

import Conexao.Conexao;
import Slack.SlackConfig;
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
import Modelo.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import java.util.List;

public class DaoDados {
    private Looca looca = new Looca();
    private Sistema sistema = looca.getSistema();
    private Processador processador = looca.getProcessador();
    private Temperatura temp = new Temperatura();
    private Memoria memoria = looca.getMemoria();
    private String hostNameUser = looca.getRede().getParametros().getNomeDeDominio();
    private String ipUser = looca.getRede().getParametros().getServidoresDns().toString();

    SlackConfig slack = new SlackConfig("xoxb-6181502641763-6281797380997-o6VZ6MYnYMTimYdSuL6he6Su", "C065CMQ4ADQ");

    private String ipServidor;
    private Integer fkEmpresa;
    private Integer fkEmpresaServer;
    private Integer fkDataCenter;
    private Integer fkDataCenterServer;
    private Integer idComponente;
    private Integer idComponenteServer;
    private Integer emitirAlerta = 10;
    private Integer tentativasAcesso = 1;

    public DaoDados(Looca looca, Sistema sistema, Processador processador, Temperatura temp, Memoria memoria, String hostNameUser, String ipUser, String ipServidor, Integer fkEmpresa, Integer fkEmpresaServer, Integer fkDataCenter, Integer fkDataCenterServer, Integer idComponente, Integer idComponenteServer, Integer emitirAlerta, Integer tentativasAcesso) {
        this.looca = looca;
        this.sistema = sistema;
        this.processador = processador;
        this.temp = temp;
        this.memoria = memoria;
        this.hostNameUser = hostNameUser;
        this.ipUser = ipUser;
        this.ipServidor = ipServidor;
        this.fkEmpresa = fkEmpresa;
        this.fkEmpresaServer = fkEmpresaServer;
        this.fkDataCenter = fkDataCenter;
        this.fkDataCenterServer = fkDataCenterServer;
        this.idComponente = idComponente;
        this.idComponenteServer = idComponenteServer;
        this.emitirAlerta = emitirAlerta;
        this.tentativasAcesso = tentativasAcesso;
    }

    public DaoDados() {
    }

    private static final String file_name = "performee_log.txt";
    // Pasta dos logs para Windows
    private static final String log_folder_windows = System.getProperty("user.home") + "\\Documentos\\Log\\";

    // Pasta dos Logs para Linux (Ubuntu)
    private static final String log_folder_unix = System.getProperty("user.home") + "/Log/";

    private static final String file_path = isWindows() ? log_folder_windows + file_name : log_folder_unix + file_name;

    // Vendo se o sistema é Windowns
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public void setLog(String descricao) {
        try {
            System.out.println(file_path);
            // Verifica se o diretório existe... se não existir, ele cria.
            if (!Files.exists(Path.of(isWindows() ? log_folder_windows : log_folder_unix))) {
                Files.createDirectories(Path.of(isWindows() ? log_folder_windows : log_folder_unix));
            }

            // Verifica se o arquivo existe... se não existir, ele cria.
            if (!Files.exists(Path.of(file_path))) {
                setCaminhoArq(file_path);
            }

            try (FileWriter arq = new FileWriter(file_path, true)) {
                LocalDateTime currentDate = LocalDateTime.now();
                String formattedDateTime = currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

                // Escreve no arquivo
                arq.write(formattedDateTime + " " + descricao + "\n" + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setCaminhoArq(String caminho) {
        System.out.println(caminho);

        try {
            // Cria o arquivo na pasta Log com permissões adequadas
            Files.createFile(Path.of(caminho));

            System.out.println("Arquivo gerado com sucesso em: " + caminho);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean buscarIp(String ipServer) {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Integer verificacaoIpServer = conServer.queryForObject("SELECT COUNT(*) FROM Servidor where IpServidor = ?", Integer.class, ipServer);

        if (verificacaoIpServer != 0) {

            ipServidor = ipServer;

            fkEmpresaServer = conServer.queryForObject("SELECT fkEmpresa FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

            fkDataCenterServer = conServer.queryForObject("SELECT fkDataCenter FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Acessou o Servidor de IP: %s com Sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);

            verificarSeTemNoBancoLocal();

            return true;
        } else {

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou acessar um servidor inexistente pela %d vez!""".formatted(ipUser, hostNameUser, tentativasAcesso);
            setLog(descricao);
            tentativasAcesso++;
            return false;
        }
    }

    public void verificarSeTemNoBancoLocal() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Integer verificacaoIp = con.queryForObject("SELECT COUNT(*) FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

        String emailEmpr = conServer.queryForObject("SELECT email FROM Empresa where idEmpresa = ?", String.class, fkEmpresaServer);

        String cnpjEmp = conServer.queryForObject("SELECT cnpj FROM Empresa where idEmpresa = ?", String.class, fkEmpresaServer);

        String nomeDc = conServer.queryForObject("SELECT nome FROM DataCenter where idDataCenter = ?", String.class, fkDataCenterServer);

        Double tamanhoDc = conServer.queryForObject("SELECT tamanho FROM DataCenter where idDataCenter = ?", Double.class, fkDataCenterServer);


        if (verificacaoIp != 0) {
            fkEmpresa = con.queryForObject("SELECT fkEmpresa FROM Servidor where IpServidor = ?", Integer.class, ipServidor);
            fkDataCenter = con.queryForObject("SELECT fkDataCenter FROM Servidor where IpServidor = ?", Integer.class, ipServidor);
        } else {

            Integer emailEmprL = con.queryForObject("SELECT COUNT(*) FROM Empresa where email = ?", Integer.class, emailEmpr);
            Integer cnpjEmprL = con.queryForObject("SELECT COUNT(*) FROM Empresa where cnpj = ?", Integer.class, cnpjEmp);

            Integer nomeDcL = con.queryForObject("SELECT COUNT(*) FROM DataCenter where nome = ?", Integer.class, nomeDc);
            Integer tamanhoDcL = con.queryForObject("SELECT COUNT(*) FROM DataCenter where tamanho = ?", Integer.class, tamanhoDc);

            if (emailEmprL == 0 && cnpjEmprL == 0) {

                String razaoSocial = conServer.queryForObject("SELECT razaoSocial FROM Empresa where idEmpresa = ?", String.class, fkEmpresaServer);
                String nomeFantasia = conServer.queryForObject("SELECT nomeFantasia FROM Empresa where idEmpresa = ?", String.class, fkEmpresaServer);
                String telefone = conServer.queryForObject("SELECT telefone FROM Empresa where idEmpresa = ?", String.class, fkEmpresaServer);

                con.update("insert into Empresa(razaoSocial, nomeFantasia, cnpj, email, telefone) values (?,?,?,?,?)", razaoSocial, nomeFantasia, cnpjEmp, emailEmpr, telefone);

                fkEmpresa = con.queryForObject("SELECT idEmpresa FROM Empresa where email = ? and cnpj = ?", Integer.class, emailEmpr, cnpjEmp);
            } else {
                fkEmpresa = con.queryForObject("SELECT idEmpresa FROM Empresa where email = ? and cnpj = ?", Integer.class, emailEmpr, cnpjEmp);
            }
            if (nomeDcL == 0 && tamanhoDcL == 0) {

                con.update("insert into DataCenter(nome, tamanho, fkEmpresa) values (?,?,?)", nomeDc, tamanhoDc, fkEmpresa);

                fkDataCenter = con.queryForObject("SELECT idDataCenter FROM DataCenter where nome = ? and tamanho = ?", Integer.class, nomeDc, tamanhoDc);

            } else {
                fkDataCenter = con.queryForObject("SELECT idDataCenter FROM DataCenter where nome = ? and tamanho = ?", Integer.class, nomeDc, tamanhoDc);
            }

            String hostnameL = conServer.queryForObject("SELECT hostname FROM Servidor where IpServidor = ?", String.class, ipServidor);
            String sisOpL = conServer.queryForObject("SELECT sisOp FROM Servidor where IpServidor = ?", String.class, ipServidor);
            Integer ativoL = conServer.queryForObject("SELECT ativo FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

            con.update("insert into Servidor(ipServidor, hostname, sisOp, ativo, fkEmpresa, fkDataCenter) values (?,?,?,?,?,?)", ipServidor, hostnameL, sisOpL, ativoL, fkEmpresa, fkDataCenter);
        }
    }

    public void inserirComponente() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Integer count = con.queryForObject("SELECT COUNT(*) FROM Componente where tipo != 'GPU' and fkServidor = ?", Integer.class, ipServidor);

        Integer countServer = conServer.queryForObject("SELECT COUNT(*) FROM Componente where tipo != 'GPU' and fkServidor = ?", Integer.class, ipServidor);

        if (countServer != 0) {
            System.out.println("""
                    \033[1;33mJá existe %d componentes cadastrado!\033[m""".formatted(countServer));

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou cadastrar os componentes do servidor de IP: %s, mas já existe componentes cadastrados""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);

        } else {
            switch (1) {
                case 1: {
                    String modelo = processador.getNome();
                    double capacidadeTotal = processador.getNumeroCpusLogicas();

                    System.out.println("Salvando dados da CPU....");
                    conServer.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "CPU", modelo, capacidadeTotal, 1, fkEmpresaServer, fkDataCenterServer, ipServidor);

                }
                case 2: {
                    String modelo = "Memoria RAM";                  //divide bytes em gb
                    Double capacidadeTotal = (memoria.getTotal() / 1073741824.0);

                    System.out.println("Salvando dados da RAM....");
                    conServer.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "RAM", modelo, capacidadeTotal, 3, fkEmpresaServer, fkDataCenterServer, ipServidor);

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

                        conServer.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "Disco", modelo, capacidadeTotal, 3, fkEmpresaServer, fkDataCenterServer, ipServidor);
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

                        conServer.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "Rede", modelo, capacidadeTotal, 4, fkEmpresaServer, fkDataCenterServer, ipServidor);
                        break;
                    }
                }
            }
            System.out.println("Dados enviado com sucesso!");
            String descricao = """
                    : Usuário do IP %s, hostName: %s. Todos componentes do servidor de IP: %s cadastrados com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);
        }
        if (count != 0) {
            System.out.println("""
                    \033[1;33mJá existe %d componentes cadastrado no banco local!\033[m""".formatted(countServer));

        } else {

            switch (1) {
                case 1: {
                    String modelo = processador.getNome();
                    double capacidadeTotal = processador.getNumeroCpusLogicas();

                    System.out.println("Salvando dados da CPU local....");
                    con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "CPU", modelo, capacidadeTotal, 1, fkEmpresa, fkDataCenter, ipServidor);

                }
                case 2: {
                    String modelo = "Memoria RAM";                  //divide bytes em gb
                    Double capacidadeTotal = (memoria.getTotal() / 1073741824.0);

                    System.out.println("Salvando dados da RAM local....");
                    con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "RAM", modelo, capacidadeTotal, 3, fkEmpresa, fkDataCenter, ipServidor);

                }
                case 3: {

                    String modelo;
                    Double capacidadeTotal;

                    //Criação do gerenciador
                    DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                    //Obtendo lista de discos a partir do getter
                    List<Disco> discos = grupoDeDiscos.getDiscos();
                    System.out.println("Salvando dados do disco local....");
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

                    System.out.println("Salvando dados da rede local....");
                    for (RedeInterface rede : redes) {
                        modelo = rede.getNomeExibicao();             //bytes em mb
                        capacidadeTotal = (rede.getBytesEnviados() / 1048576.0) + (rede.getBytesRecebidos() / 1048576.0);
                        con.update("insert into Componente(tipo, modelo, capacidadeTotal, fkMedida, fkEmpresa, fkDataCenter, fkServidor) values (?,?,ROUND(?, 2),?,?,?,?)", "Rede", modelo, capacidadeTotal, 4, fkEmpresa, fkDataCenter, ipServidor);
                        break;
                    }
                }
            }
        }
    }

    public void atualizarComponete(Integer opcao) {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Integer count = con.queryForObject("SELECT COUNT(*) FROM Componente where fkServidor = ?", Integer.class, ipServidor);
        Integer countServer = conServer.queryForObject("SELECT COUNT(*) FROM Componente where fkServidor = ?", Integer.class, ipServidor);

        if (countServer == 0) {
            System.out.println("""
                    \033[1;33mNão existe componentes cadastrados nesse servidor!\033[m
                    Cadastrando agora...""");

            inserirComponente();

            String descricao = """
                    : Usuário do IP %s, hostName: %s. Tentou atualizar componentes do servidor de IP: %s, mas não existe componentes cadastrado para atualizar!""".formatted(ipUser, hostNameUser, ipServidor);
            setLog(descricao);
        } else {
            switch (opcao) {
                case 1: {
                    String modelo = processador.getNome();
                    double capacidadeTotal = processador.getNumeroCpusLogicas();

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

                    idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

                    System.out.println("Atualizando dados da CPU....");
                    con.update("update Componente set modelo = ?, capacidadeTotal = ? where idComponente = ?", modelo, capacidadeTotal, idComponente);

                    conServer.update("update Componente set modelo = ?, capacidadeTotal = ? where idComponente = ?", modelo, capacidadeTotal, idComponenteServer);

                    String descricao = """
                            : Usuário do IP %s, hostName: %s. Atualizou a CPU do servidor de IP: %s com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
                    setLog(descricao);
                    break;
                }
                case 2: {
                    String modelo = "Memoria RAM";                  //divide bytes em gb
                    Double capacidadeTotal = (memoria.getTotal() / 1073741824.0);

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);
                    idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);

                    System.out.println("Atualizando dados da RAM....");
                    con.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponente);
                    conServer.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponenteServer);

                    String descricao = """
                            : Usuário do IP %s, hostName: %s. Atualizou a RAM do servidor de IP: %s com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
                    setLog(descricao);
                    break;
                }
                case 3: {

                    String modelo;
                    Double capacidadeTotal;

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);
                    idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);

                    DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                    System.out.println("Atualizando dados do Disco....");
                    List<Disco> discos = grupoDeDiscos.getDiscos();
                    for (Disco disco : discos) {
                        modelo = disco.getModelo();            //divide bytes em gb
                        capacidadeTotal = disco.getTamanho() / 1073741824.0;
                        con.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponente);
                        conServer.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponenteServer);
                        break;
                    }
                    String descricao = """
                            : Usuário do IP %s, hostName: %s. Atualizou o Disco do servidor de IP: %s com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
                    setLog(descricao);
                    break;
                }
                case 4: {
                    String modelo;
                    Double capacidadeTotal;

                    idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);
                    idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);

                    RedeInterfaceGroup grupoDeRedes = looca.getRede().getGrupoDeInterfaces();

                    List<RedeInterface> redes = grupoDeRedes.getInterfaces();
                    System.out.println("Atualizando dados da Rede....");
                    for (RedeInterface rede : redes) {
                        modelo = rede.getNomeExibicao();             //bytes em mb
                        capacidadeTotal = (rede.getBytesEnviados() / 1048576.0) + (rede.getBytesRecebidos() / 1048576.0);
                        con.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponente);
                        conServer.update("update Componente set modelo = ?, capacidadeTotal = ROUND(?, 2) where idComponente = ?", modelo, capacidadeTotal, idComponenteServer);
                        break;
                    }
                    String descricao = """
                            : Usuário do IP %s, hostName: %s. Atualizou a Rede do servidor de IP: %s com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
                    setLog(descricao);
                    break;
                }
                case 5: {
                    System.out.println("Voltando para o inicio...");
                    break;
                }
                default: {
                    System.out.println("Opção \033[1;31minválida!\033[m digite novamente");
                }
            }
        }
    }

    public void inserirLeitura() {
        Timer cronometro = new Timer();
        Timer cronometro2 = new Timer();
        Timer cronometro3 = new Timer();

        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();
        Integer opcao = 1;

        String descricao = """
                : Usuário do IP %s, hostName: %s. Iniciou o processo de inserção no servidor de IP: %s com sucesso!""".formatted(ipUser, hostNameUser, ipServidor);
        setLog(descricao);

            cronometro3.schedule(new TimerTask() {
                @Override
                public void run() {
                    switch (opcao) {
                        case 1: {
                            idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);
                            idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

                            Double emUso = processador.getUso();
                            String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                            Double temperatura = temp.getTemperatura();
                            Double frequencia = (double) processador.getFrequencia() / 1000000000.0;

                            con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkMedidaTemp, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,?,?,6,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresa, fkDataCenter, ipServidor, idComponente);

                            conServer.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkMedidaTemp, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (GETDATE(),ROUND(?, 2),?,?,?,6,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresaServer, fkDataCenterServer, ipServidor, idComponenteServer);

                            System.out.println("Enviando Leitura da CPU");
                        }
                        case 2: {
                            idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);

                            idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'RAM' and fkServidor = ?", Integer.class, ipServidor);

                            Double emUso = memoria.getEmUso() / 1073741824.0;
                            String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                            Double temperatura = null;
                            Double frequencia = null;

                            con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,?,?,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresa, fkDataCenter, ipServidor, idComponente);

                            conServer.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (GETDATE(),ROUND(?, 2),?,?,?,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresaServer, fkDataCenterServer, ipServidor, idComponenteServer);
                            System.out.println("Enviando Leitura da RAM");

                        }
                        case 3: {

                            //Criação do gerenciador
                            DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                            //Obtendo lista de discos a partir do getter
                            List<Disco> discos = grupoDeDiscos.getDiscos();

                            idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);
                            idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);


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
                                        conServer.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, velocidadeLeitura, velocidadeEscrita, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (GETDATE(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, velocidadeLeitura, velocidadeEscrita, fkEmpresaServer, fkDataCenterServer, ipServidor, idComponenteServer);

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
                            idComponenteServer = conServer.queryForObject("SELECT idComponente FROM Componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);


                            for (RedeInterface rede : GpRede) {
                                Double emUso = null;
                                String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                                //bytes em mb
                                Double upload = rede.getBytesEnviados() / 1e6;
                                Double download = rede.getBytesRecebidos() / 1e6;


                                con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, upload, download, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, upload, download, fkEmpresa, fkDataCenter, ipServidor, idComponente);
                                conServer.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, upload, download, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (GETDATE(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, upload, download, fkEmpresaServer, fkDataCenterServer, ipServidor, idComponenteServer);
                                break;
                            }
                        }
                        System.out.println("Enviando Leitura da Rede");
                    }
                }
            }, 1000, 15000);

            cronometro.schedule(new TimerTask() {
                @Override
                public void run() {
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
                }
            }, 60000, 60000);

            cronometro2.schedule(new TimerTask() {
                @Override
                public void run() {
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
                }
            }, 120000, 120000);
      };

    public List<Componentes> exibirComponentes() {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        return conServer.query("select * from Componente where fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Componentes.class));

    }

    public List<Cpu> exibirCpu() {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        return conServer.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'CPU' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Cpu.class));
    }

    public List<Ram> exibirRam() {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        return conServer.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'RAM' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Ram.class));
    }

    public List<Disk> exibirDisco() {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        return conServer.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'DISCO' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Disk.class));
    }

    public List<Rede> exibirRede() {
        Conexao conexao = new Conexao();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        return conServer.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'REDE' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Rede.class));
    }

    public void alertaCpu() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Double mediaUsoCpuServer = conServer.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double temperaturaServer = conServer.queryForObject("SELECT ROUND(AVG(temperatura), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 temperatura\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkCpu = con.queryForObject("select idComponente from Componente where tipo = \"CPU\" and fkServidor = ?", Integer.class, ipServidor);
        Integer fkCpuServer = conServer.queryForObject("select idComponente from componente where tipo = 'CPU' and fkServidor = ?", Integer.class, ipServidor);

        String componente = "CPU";
        String tipo;
        String descricao;
        String descricao2;
        Integer dias = 10;

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM Leitura as l\n" +
                "\tJOIN Componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = \"CPU\" and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);

        Integer fkLeituraServer = conServer.queryForObject("SELECT TOP 1 idLeitura\n" +
                "FROM leitura AS l\n" +
                "JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "WHERE c.tipo = 'CPU' AND l.fkServidor = ?\n" +
                "ORDER BY l.idLeitura DESC;\n", Integer.class, ipServidor);


        if (mediaUsoCpuServer >= 85) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização da %s esteve constantemente acima de 85%%, nas últimas %d verificações. Pode ocorrer Travamentos! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpuServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);


        } else if (mediaUsoCpuServer >= 66 && mediaUsoCpuServer <= 84) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização da %s esteve constantemente entre 66%% a 84%%, nas últimas %d verificações. Pode ocorrer lentidão! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpuServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);

        } else {
            descricao = String.format("Alerta Estável. Servidor %s: A utilização da %s está abaixo de 66%%, últimas %d verificações. A utilização está boa! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoCpuServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);
        }

        slack.enviarAlerta(descricao);

        if (temperaturaServer > 39) {
            descricao2 = String.format("Alerta de Risco. Servidor %s: A Temperatura da %s está acima de 39°C, nas últimas %d verificações! Risco de Super Aquecimento!. Média de temperatura: %.2f°C", ipServidor, componente, dias, temperaturaServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);

            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);


        } else if (temperaturaServer >= 35 && temperaturaServer <= 39) {
            descricao2 = String.format("Alerta de Cuidado. Servidor %s: A Temperatura da %s está entre 35°C a 39°C, nas últimas %d verificações. Pode ocorrer aquecimento! média de temperatura: %.2f°C", ipServidor, componente, dias, temperaturaServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);

        } else {
            descricao2 = String.format("Alerta Estável. Servidor %s: A Temperatura da %s está abaixo de 35°C, nas últimas %d verificações. Temperatura está OK! Média de temperatura: %.2f°C", ipServidor, componente, dias, temperaturaServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkCpu, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkCpuServer, fkLeituraServer);

        }

        slack.enviarAlerta(descricao2);
    }

    public void alertaRam() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Double mediaUsoRamServer = conServer.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Ram' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double capacidadeTotalRamServer = conServer.queryForObject("SELECT c.capacidadeTotal as capacidade FROM Componente as c\n" +
                "    WHERE fkServidor = ? AND c.tipo = 'RAM';", Double.class, ipServidor);

        Integer fkRam = con.queryForObject("select idComponente from Componente where tipo = \"Ram\" and fkServidor = ?", Integer.class, ipServidor);
        Integer fkRamServer = conServer.queryForObject("select idComponente from componente where tipo = 'Ram' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM Leitura as l\n" +
                "\tJOIN Componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = \"Ram\" and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);

        Integer fkLeituraServer = conServer.queryForObject("SELECT TOP 1 idLeitura\n" +
                "FROM leitura AS l\n" +
                "JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "WHERE c.tipo = 'Ram' AND l.fkServidor = ?\n" +
                "ORDER BY l.idLeitura DESC;", Integer.class, ipServidor);

        String componente = "Ram";
        String tipo;
        String descricao;
        Integer dias = 10;


        if (mediaUsoRamServer > (capacidadeTotalRamServer * .85)) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização da %s está constantemente acima de 85%% da memória total, nas ultimas %d verificações. Risco de travamento! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRamServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRam, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRamServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);


        } else if (mediaUsoRamServer <= (capacidadeTotalRamServer * .85) && mediaUsoRamServer >= (capacidadeTotalRamServer * .66)) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização da %s está ocilando entre 66%% a 85%% da memória total, nas ultimas %d verificações. Pode ocorrer lentidão! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRamServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRam, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRamServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);

        } else {
            descricao = String.format("Alerta Estável. Servidor %s: A utilização da %s está abaixo de 66%% da memória total, ultimas %d verificações. A Ram está OK! Média de utilização: %.2fGB", ipServidor, componente, dias, mediaUsoRamServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRam, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRamServer, fkLeituraServer);
        }

        slack.enviarAlerta(descricao);
    }

    public void alertaDisco() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Double mediaUsoDiskServer = conServer.queryForObject("SELECT ROUND(AVG(emUso), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 emUso\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);

        Double mediaLeituraServer = conServer.queryForObject("SELECT ROUND(AVG(velocidadeLeitura), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 velocidadeLeitura\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Double mediaEscritaServer = conServer.queryForObject("SELECT ROUND(AVG(velocidadeEscrita), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 velocidadeEscrita\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkDisco = con.queryForObject("select idComponente from Componente where tipo = \"Disco\" and fkServidor = ?", Integer.class, ipServidor);
        Integer fkDiscoServer = conServer.queryForObject("select idComponente from componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM Leitura as l\n" +
                "\tJOIN Componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = \"Disco\" and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);
        Integer fkLeituraServer = conServer.queryForObject("SELECT TOP 1 idLeitura\n" +
                "FROM leitura AS l\n" +
                "JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "WHERE c.tipo = 'Disco' AND l.fkServidor = ?\n" +
                "ORDER BY l.idLeitura DESC;", Integer.class, ipServidor);

        String componente = "Disco";
        String tipo;
        String descricao;
        String descricao2;
        String descricao3;
        Integer dias = 10;


        if (mediaUsoDiskServer > 85) {
            descricao = String.format("Alerta de Risco. Servidor %s: A utilização do %s está acima de 85%% do armazenamento total, nas ultimas %d verificações. Pouco espaço no disco! média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDiskServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);


        } else if (mediaUsoDiskServer >= 66 && mediaUsoDiskServer <= 85) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: A utilização do %s está entre de 66%% a 85%% do armazenamento total, nas ultimas %d verificações. Espaço razoável no disco! média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDiskServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);

        } else {

            descricao = String.format("Alerta Estável. Servidor %s: A utilização do %s está abaixo de 66%% do armazenamento total, ultimas %d verificações. Espaço no disco OK! Média de utilização: %.2f%%", ipServidor, componente, dias, mediaUsoDiskServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

        }

        slack.enviarAlerta(descricao);

        if (mediaLeituraServer < 80) {
            descricao2 = String.format("Alerta de Risco. Servidor %s: A velocidade de leitura do %s está abaixo de 80Mbs, nas ultimas %d verificações. Leitura do disco baixa! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);


        } else if (mediaLeituraServer < 100 && mediaLeituraServer >= 80) {
            descricao2 = String.format("Alerta de Cuidado. Servidor %s: A velocidade de leitura do %s está entre 80MBs e 100MBs, nas ultimas %d verificações. Leitura do disco preocupante! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);

        } else {

            descricao2 = String.format("Alerta Estável. Servidor %s: A velocidade de leitura do %s está acima 100MBs, nas ultimas %d verificações. Leitura do disco OK! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao2, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

        }

        slack.enviarAlerta(descricao2);

        if (mediaEscritaServer < 30) {
            descricao3 = String.format("Alerta de Risco. Servidor %s: A velocidade de escrita do %s está abaixo de 30Mbs, nas ultimas %d verificações. escrita do disco baixa! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao3);
            setLog(logAlerta);


        } else if (mediaEscritaServer < 45 && mediaEscritaServer >= 30) {
            descricao3 = String.format("Alerta de Cuidado. Servidor %s: A velocidade de leitura do %s está entre 80MBs e 100MBs, nas ultimas %d verificações. Leitura do disco preocupante! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao3);
            setLog(logAlerta);

        } else {

            descricao3 = String.format("Alerta Estável. Servidor %s: A velocidade de leitura do %s está acima 100MBs, nas ultimas %d verificações. Leitura do disco OK! média de leitura: %.2fMBs", ipServidor, componente, dias, mediaLeituraServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresa, fkDataCenter, ipServidor, fkDisco, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao3, fkEmpresaServer, fkDataCenterServer, ipServidor, fkDiscoServer, fkLeituraServer);

        }
        slack.enviarAlerta(descricao3);
    }

    public void alertaRede() throws SlackApiException, IOException, InterruptedException {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();
        JdbcTemplate conServer = conexao.getConexaoDoBancoServer();

        Double mediaUsoRedeUpServer = conServer.queryForObject("SELECT ROUND(AVG(upload), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 upload\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Double mediaUsoRedeDowServer = conServer.queryForObject("SELECT ROUND(AVG(download), 2) AS media_ultimas_10_leituras\n" +
                "FROM (\n" +
                "    SELECT TOP 10 download\n" +
                "    FROM leitura AS l\n" +
                "    JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "    WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "    ORDER BY l.idLeitura DESC\n" +
                ") AS ultimas_leituras;", Double.class, ipServidor);


        Integer fkRede = con.queryForObject("select idComponente from Componente where tipo = \"Rede\" and fkServidor = ?", Integer.class, ipServidor);
        Integer fkRedeServer = conServer.queryForObject("select idComponente from componente where tipo = 'Rede' and fkServidor = ?", Integer.class, ipServidor);

        Integer fkLeitura = con.queryForObject("SELECT idLeitura \n" +
                "FROM Leitura as l\n" +
                "\tJOIN Componente as c ON c.idComponente = l.fkComponente \n" +
                "\t\tWHERE c.tipo = \"Rede\" and l.fkServidor = ?\n" +
                "\t\t\tORDER BY l.idLeitura DESC\n" +
                "\t\t\t\tLIMIT 1;", Integer.class, ipServidor);
        Integer fkLeituraServer = conServer.queryForObject("SELECT TOP 1 idLeitura\n" +
                "FROM leitura AS l\n" +
                "JOIN componente AS c ON c.idComponente = l.fkComponente \n" +
                "WHERE c.tipo = 'Rede' AND l.fkServidor = ?\n" +
                "ORDER BY l.idLeitura DESC;", Integer.class, ipServidor);

        String componente = "Rede";
        String tipo;
        String descricao;
        String descricao2;
        Integer dias = 10;


        if (mediaUsoRedeUpServer < 20) {
            descricao = String.format("Alerta de Risco. Servidor %s: O upload da %s está abaixo de 20Mbs, nas ultimas %d verificações. A rede está lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeUpServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);


        } else if (mediaUsoRedeUpServer <= 89 && mediaUsoRedeUpServer >= 20) {
            descricao = String.format("Alerta de Cuidado. Servidor %s: O upload da %s está entre 20Mbs a 89Mbs, nas ultimas %d verificações. A rede Pode ficar Lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeUpServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao);
            setLog(logAlerta);

        } else {
            descricao = String.format("Alerta Estável. Servidor %s: O upload da %s está acima dos 89Mbs, ultimas %d verificações. A rede está boa! Média de utilização: %.2fGBs", ipServidor, componente, dias, mediaUsoRedeUpServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);
        }


        if (mediaUsoRedeDowServer < 40) {
            descricao2 = String.format("Alerta de Risco. Servidor %s: O download da %s está abaixo de 40Mbs, nas ultimas %d verificações. A rede está lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDowServer);

            tipo = "Em risco";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);


        } else if (mediaUsoRedeDowServer <= 89 && mediaUsoRedeDowServer >= 40) {
            descricao2 = String.format("Alerta de Cuidado. Servidor %s: O download da %s está entre 40Mbs a 89Mbs, nas ultimas %d verificações. A rede Pode ficar Lenta! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDowServer);

            tipo = "Cuidado";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);

            String logAlerta = """
                    : Usuário do IP %s, hostName: %s. Teve %s""".formatted(ipUser, hostNameUser, descricao2);
            setLog(logAlerta);


        } else {
            descricao2 = String.format("Alerta Estável. Servidor %s: O download da %s está acima dos 89Mbs, ultimas %d verificações. A rede está boa! Média de utilização: %.2fMbs", ipServidor, componente, dias, mediaUsoRedeDowServer);

            tipo = "Estável";

            con.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (now(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresa, fkDataCenter, ipServidor, fkRede, fkLeitura);
            conServer.update("insert into Alerta(dataAlerta, tipo, descricao, fkEmpresa, fkDataCenter, fkServidor, fkComponente, fkLeitura) values (GETDATE(),?,?,?,?,?,?,?)", tipo, descricao, fkEmpresaServer, fkDataCenterServer, ipServidor, fkRedeServer, fkLeituraServer);
        }

            slack.enviarAlerta(descricao);
            slack.enviarAlerta(descricao2);
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

    public Integer getFkEmpresaServer() {
        return fkEmpresaServer;
    }

    public void setFkEmpresaServer(Integer fkEmpresaServer) {
        this.fkEmpresaServer = fkEmpresaServer;
    }

    public Integer getFkDataCenterServer() {
        return fkDataCenterServer;
    }

    public void setFkDataCenterServer(Integer fkDataCenterServer) {
        this.fkDataCenterServer = fkDataCenterServer;
    }

    public Integer getIdComponenteServer() {
        return idComponenteServer;
    }

    public void setIdComponenteServer(Integer idComponenteServer) {
        this.idComponenteServer = idComponenteServer;
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
                ", fkEmpresaServer=" + fkEmpresaServer +
                ", fkDataCenter=" + fkDataCenter +
                ", fkDataCenterServer=" + fkDataCenterServer +
                ", idComponente=" + idComponente +
                ", idComponenteServer=" + idComponenteServer +
                ", emitirAlerta=" + emitirAlerta +
                ", tentativasAcesso=" + tentativasAcesso +
                '}';
    }
}

