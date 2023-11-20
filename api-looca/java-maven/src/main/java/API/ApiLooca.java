package API;

import Dao.DaoDados;
import com.github.britooo.looca.api.group.discos.Disco;
import modelo.*;

import java.io.IOException;
import java.util.Scanner;

public class ApiLooca {

    public static void main(String[] args) throws IOException, InterruptedException {

        DaoDados dao = new DaoDados();
        Scanner leitor = new Scanner(System.in);

        Integer opcao;
        String ipServidor;
        Integer numTentativas = 6;


        System.out.println("""
                +-------------------------------+
                |   Bem vindo ao performee.     |""");


        while (true) {

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
                    System.exit(0);
                }
                System.out.println("""
                        Você tem %d tentativas!""".formatted(numTentativas));
            }
            else {
                while (true) {

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
                            | 3) Atualizar Tudo                    |
                            | 4) Cancelar                          |
                            +--------------------------------------+""");
                                opcaoAtualizar = leitor.nextInt();

                                dao.atualizarComponete(opcaoAtualizar);
                            } while (opcaoAtualizar != 4);
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
                            System.exit(0);
                        }
                        default: {
                            System.out.println("Opção inválida! digite novamente");
                        }
                    }
                }
            }
        }
    }
}

