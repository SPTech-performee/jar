package Slack;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import java.io.IOException;

public class SlackConfig {
    private MethodsClient client;
    private String token;
    private String canal;

    public SlackConfig(String token, String canal) {
        this.client = Slack.getInstance().methods();
        this.token = token;
        this.canal = canal;
    }

    public void enviarAlerta(String descricao) {
        try {
            var retorno = client.chatPostMessage(r -> r
                    .token(token)
                    .channel(canal)
                    .text(descricao)
            );
        } catch (SlackApiException | IOException erro) {
            System.err.println("Erro ao enviar alerta para o Slack: " + erro.getMessage());
        }
    }
}
