import Dao.DaoDados;
import Modelo.*;

import java.util.Scanner;

public class AppPerformee {

    public static void main(String[] args) {

        DaoDados dao = new DaoDados();
        Scanner leitor = new Scanner(System.in);

        Integer opcao = 0;
        String ipServidor;
        Integer numTentativas = 6;


        System.out.println("""
                +-------------------------------+
                |   Bem vindo ao performee.     |""");


        do {

            System.out.print("""
                    +-------------------------------+
                    Digite o Ip do Servidor:""");
            ipServidor = leitor.nextLine();

            Boolean validacao = dao.buscarIp(ipServidor);

            if (validacao == false) {
                System.out.println("Servidor não Encontrado!");
                numTentativas--;

                if (numTentativas == 0) {
                    System.out.println("Acabou suas tentativas! Volte mais tarde");
                    String descricao = """
                    : Usuário do IP %s, hostName: %s. Esgotou o número máximo de tentativas para acessar o servidor! e o JAR foi encerrado""".formatted(dao.getIpUser(), dao.getHostNameUser(), ipServidor);
                    dao.setLog(descricao);
                    System.exit(0);
                }
                System.out.println("""
                        Você tem %d tentativas!""".formatted(numTentativas));
            } else {
                do {

                    System.out.println("""
                            +-------------------------------+
                            | 1) Cadastrar componentes      |
                            | 2) Atualizar componentes      |
                            | 3) Inserir dados de leitura   |
                            | 4) Ver Componentes            |
                            | 5) Ver Leituras               |
                            | 6) Sair                       |
                            +-------------------------------+""");

                    opcao = leitor.nextInt();


                    switch (opcao) {
                        case 1: {
                            dao.inserirComponente();
                            break;
                        }
                        case 2: {
                            Integer opcaoAtualizar;
                            do {
                                System.out.println("""
                                        +--------------------------------------+
                                        | Qual componente deseja atualizar?    |
                                        +--------------------------------------+
                                        | 1) Atualizar CPU                     |
                                        | 2) Atualizar RAM                     |
                                        | 3) Atualizar Disco                   |
                                        | 4) Atualizar Rede                    |
                                        | 5) Cancelar                          |
                                        +--------------------------------------+""");
                                opcaoAtualizar = leitor.nextInt();

                                dao.atualizarComponete(opcaoAtualizar);
                            } while (opcaoAtualizar != 5);
                            break;
                        }
                        case 3: {
                            dao.inserirLeitura();
                            break;
                        }
                        case 4: {
                            System.out.println("""
                                    +----------------------------+
                                    | Componentes:               |
                                    +----------------------------+""");
                            for (Componentes comp : dao.exibirComponentes()) {
                                System.out.println(comp);
                            }
                            break;
                        }
                        case 5: {
                            System.out.println("""
                                    +----------------------------+
                                    | Leituras:                  |
                                    +----------------------------+""");
                            for (Cpu cpu : dao.exibirCpu()) {
                                System.out.println(cpu);
                            }
                            for (Ram ram : dao.exibirRam()) {
                                System.out.println(ram);
                            }
                            for (Disk disco : dao.exibirDisco()) {
                                System.out.println(disco);
                            }
                            for (Rede rede : dao.exibirRede()) {
                                System.out.println(rede);
                            }


                            break;
                        }
                        case 6: {
                            System.out.println("""
                                    Saindo...""");
                            String descricao = """
                    : Usuário do IP %s, hostName: %s. Saiu do servidor de IP: %s""".formatted(dao.getIpUser(), dao.getHostNameUser(), ipServidor);
                            dao.setLog(descricao);
                            System.exit(0);
                        }
                        default: {
                            System.out.println("Opção inválida! digite novamente");
                        }
                    }
                } while (opcao != 3);
            }
        } while (opcao != 3);
    }
}