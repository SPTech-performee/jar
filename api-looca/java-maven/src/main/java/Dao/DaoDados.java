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
import modelo.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.Scanner;

public class DaoDados {
    private Looca looca = new Looca();
    private Sistema sistema = looca.getSistema();
    private Processador processador = looca.getProcessador();
    private Temperatura temp = new Temperatura();
    private Memoria memoria = looca.getMemoria();

    private String ipServidor;
    private Integer fkEmpresa;
    private Integer fkDataCenter;
    private Integer idComponente;

    public DaoDados(Looca looca, Sistema sistema, Processador processador, Temperatura temp, Memoria memoria, String ipServidor, Integer fkEmpresa, Integer fkDataCenter, Integer idComponente) {
        this.looca = looca;
        this.sistema = sistema;
        this.processador = processador;
        this.temp = temp;
        this.memoria = memoria;
        this.ipServidor = ipServidor;
        this.fkEmpresa = fkEmpresa;
        this.fkDataCenter = fkDataCenter;
        this.idComponente = idComponente;
    }

    public DaoDados(){
    }

    public Boolean buscarIp(String ipServer) {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer verificacaoIp = con.queryForObject("SELECT COUNT(*) FROM Servidor where IpServidor = ?", Integer.class, ipServer);

        if (verificacaoIp != 0) {

            ipServidor = ipServer;
             fkEmpresa = con.queryForObject("SELECT fkEmpresa FROM Servidor where IpServidor = ?", Integer.class, ipServidor);
            fkDataCenter = con.queryForObject("SELECT fkDataCenter FROM Servidor where IpServidor = ?", Integer.class, ipServidor);

            return true;
        } else{
            return false;
        }
    }

    public void inserirComponente() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        Integer count = con.queryForObject("SELECT COUNT(*) FROM Componente where fkServidor = ?", Integer.class, ipServidor);

        if (count != 0) {
            System.out.println("""
                    Já existe %d componentes cadastrado!""".formatted(count));
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
    public void inserirLeitura () {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Agendando a execução a cada 15 segundos
        scheduler.scheduleAtFixedRate(() -> {
            try {
                while (true) {
                    Integer opcao = 1;
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

                            con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, temperatura, frequencia, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,?,?,?,?,?,?)", emUso, tempoAtivdade, temperatura, frequencia, fkEmpresa, fkDataCenter, ipServidor, idComponente);
                            System.out.println("Enviando Leitura da RAM");
                        }
                        case 3: {

                            //Criação do gerenciador
                            DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();

                            //Obtendo lista de discos a partir do getter
                            List<Disco> discos = grupoDeDiscos.getDiscos();

                            idComponente = con.queryForObject("SELECT idComponente FROM Componente where tipo = 'Disco' and fkServidor = ?", Integer.class, ipServidor);


                            for (Disco arm : discos){
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


                            for (RedeInterface rede : GpRede){
                                Double emUso = null;
                                String tempoAtivdade = Conversor.formatarSegundosDecorridos(sistema.getTempoDeAtividade());
                                //bytes em mb
                                Double upload = rede.getBytesEnviados() / 1048576.0;
                                Double download = rede.getBytesRecebidos() / 1048576.0;


                                con.update("insert into Leitura(dataLeitura, emUso, TempoAtividade, upload, download, fkEmpresa, fkDataCenter, fkServidor, fkComponente) values (now(),ROUND(?, 2),?,ROUND(?, 2),ROUND(?, 2),?,?,?,?)", emUso, tempoAtivdade, upload, download, fkEmpresa, fkDataCenter, ipServidor, idComponente);
                                break;
                            }
                        }
                        System.out.println("Enviando Leitura da Rede");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 15, TimeUnit.SECONDS);


    }
    public List <Componentes> exibirComponentes() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Componente where fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Componentes.class));

    }

    public List <Cpu> exibirCpu() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'CPU' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Cpu.class));
    }

    public List <Ram> exibirRam() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'RAM' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Ram.class));
    }

    public List <Disk> exibirDisco() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'DISCO' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Disk.class));
    }
    public List <Rede> exibirRede() {
        Conexao conexao = new Conexao();
        JdbcTemplate con = conexao.getConexaoDoBanco();

        return con.query("select * from Leitura as l join Componente on idComponente = fkComponente where tipo = 'REDE' and l.fkServidor = " + ipServidor,
                new BeanPropertyRowMapper<>(Rede.class));
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

    @Override
    public String toString() {
        return "DaoDados{" +
                "looca=" + looca +
                ", sistema=" + sistema +
                ", processador=" + processador +
                ", temp=" + temp +
                ", memoria=" + memoria +
                ", ipServidor='" + ipServidor + '\'' +
                ", fkEmpresa=" + fkEmpresa +
                ", fkDataCenter=" + fkDataCenter +
                ", idComponente=" + idComponente +
                '}';
    }
}

