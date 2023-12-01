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
                |   Bem vindo ao \033[1;34mperformee.\033[m     |""");


        do {

            System.out.print("""
                    +-------------------------------+
                    Digite o Ip do Servidor:""");
            ipServidor = leitor.nextLine();

            Boolean validacao = dao.buscarIp(ipServidor);

            if (!validacao) {
                System.out.println("Servidor não Encontrado!");
                numTentativas--;

                if (numTentativas == 0) {
                    System.out.println("\033[1;31mAcabou suas tentativas! Volte mais tarde\033[m");
                    String descricao = """
                    : Usuário do IP %s, hostName: %s. Esgotou o número máximo de tentativas para acessar o servidor! e o JAR foi encerrado""".formatted(dao.getIpUser(), dao.getHostNameUser(), ipServidor);
                    dao.setLog(descricao);
                    System.exit(0);
                }
                System.out.println("""
                        Você tem \033[1;31m%d\033[m tentativas!""".formatted(numTentativas));
            } else {
                System.out.println("\033[1;32mServidor encontrado!\033[m");
                do {

                    System.out.println("""
                            +-------------------------------+
                            | \033[1;35m1)\033[m Cadastrar componentes      |
                            | \033[1;35m2)\033[m Atualizar componentes      |
                            | \033[1;35m3)\033[m Inserir dados de leitura   |
                            | \033[1;35m4)\033[m Ver Componentes            |
                            | \033[1;35m5)\033[m Ver Leituras               |
                            | \033[1;35m6)\033[m Sair                       |
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
                                        | \033[1;35m1)\033[m Atualizar CPU                     |
                                        | \033[1;35m2)\033[m Atualizar RAM                     |
                                        | \033[1;35m3)\033[m Atualizar Disco                   |
                                        | \033[1;35m4)\033[m Atualizar Rede                    |
                                        | \033[1;35m5)\033[m Cancelar                          |
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
                            System.out.println("Opção \033[1;31minválida!\033[m digite novamente");
                        }
                    }
                } while (opcao != 3);
            }
        } while (opcao != 3);
    }
}