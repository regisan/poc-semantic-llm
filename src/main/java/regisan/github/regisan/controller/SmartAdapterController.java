package regisan.github.regisan.controller;

import regisan.github.regisan.dto.PedidoLegadoDTO;
import regisan.github.regisan.dto.ResultadoMapeamentoMetadata;
import regisan.github.regisan.metrics.RelatorioAdaptacao;
import regisan.github.regisan.metrics.RelatorioAdaptacaoMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/adapter")
public class SmartAdapterController {

    private static final String MODEL = "gpt-4o-mini";

    private final ChatClient chatClient;

    public SmartAdapterController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/processar-pedido")
    public PedidoLegadoDTO processarPayloadDinamico(@RequestBody Map<String, Object> payloadSujo) {

        // 1. Definição do Prompt
        String promptSystem = """
            Você é um adaptador de integridade de dados para um sistema bancário legado.
            Sua função é analisar o JSON de entrada (que pode ter campos desconhecidos)
            e mapeá-lo para a estrutura estrita de saída.
            
            Regras:
            1. Mapeie campos óbvios (ex: 'total', 'cost', 'price' -> valorTotal).
            2. Se houver campos de contexto novos (ex: 'campanha_verao', 'delivery_express'), 
               resuma-os e concatene no campo 'observacoesLogistica'.
            3. NÃO invente dados. Se não existir, deixe null.
            4. Responda APENAS o JSON compatível com a estrutura alvo.
            """;

        // 2. Chamada à IA (Abstraída pelo Spring AI)
        // O .entity(PedidoLegadoDTO.class) faz o parse automático do JSON de volta para Java!
        return chatClient.prompt()
                .system(promptSystem)
                .user("Payload de Entrada: " + payloadSujo.toString())
                .call()
                .entity(PedidoLegadoDTO.class);
    }

    @PostMapping("/processar-pedido-com-metricas")
    public RelatorioAdaptacao processarComMetricas(@RequestBody Map<String, Object> payloadSujo) {

        long inicio = System.currentTimeMillis();

        // 1. Criar o conversor explicitamente.
        // Ele ajuda a gerar o prompt de formato e a fazer o parse depois.
        var converter = new BeanOutputConverter<>(PedidoLegadoDTO.class);

        // 2. Definir o Prompt System
        // O "converter.getFormat()" é CRUCIAL aqui. Ele injeta as instruções de JSON no prompt.
        String promptSystem = """
            Você é um adaptador de integridade de dados.
            Analise o JSON de entrada e mapeie para a estrutura de saída.
            Se houver campos novos ou desconhecidos, resuma-os no campo 'observacoesLogistica'.

            Responda APENAS o JSON, sem markdown.

            %s
            """.formatted(converter.getFormat());

        // 3. Chamada à IA recuperando o ChatResponse COMPLETO (não apenas a entidade)
        ChatResponse response = chatClient.prompt()
                .system(promptSystem)
                .user("Payload de Entrada: " + payloadSujo.toString())
                .call()
                .chatResponse();

        // 4. Extração Manual
        // Pegar o texto cru (JSON String) que a IA gerou
        String jsonString = response.getResult().getOutput().getText();
        System.out.println(jsonString);

        // Converter o texto para o Objeto Java usando o conversor do Spring AI
        PedidoLegadoDTO dto = converter.convert(jsonString);

        long fim = System.currentTimeMillis();

        // 5. Montar o Relatório com os dados reais
        // Nota: O acesso aos tokens pode variar levemente dependendo da versão (0.8.x vs 1.0.x)
        // Se der erro no getUsage(), use apenas 0 por enquanto para destravar.
        Long tokensTotais = 0L;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokensTotais = Long.valueOf(response.getMetadata().getUsage().getTotalTokens());
        }

        return new RelatorioAdaptacao(
                dto,
                (fim - inicio),
                "gpt-3.5-turbo", // ou o modelo que estiver configurado no application.properties
                tokensTotais.intValue()
        );
    }

    @PostMapping("/extrair-metadados")
    public RelatorioAdaptacaoMetadata extrairMetadados(@RequestBody Map<String, Object> payloadSujo) {

        long inicio = System.currentTimeMillis();

        // 1. Criar o conversor explicitamente.
        // Ele ajuda a gerar o prompt de formato e a fazer o parse depois.
        var converter = new BeanOutputConverter<>(ResultadoMapeamentoMetadata.class);

        // 2. Definir o Prompt System
        // O "converter.getFormat()" é CRUCIAL aqui. Ele injeta as instruções de JSON no prompt.
        String promptSystem = """
                  Você é um especialista em integração de sistemas e análise semântica de dados.
                  Sua tarefa é analisar as chaves de um JSON de entrada e mapeá-las semanticamente para os atributos de um Contrato de Destino predefinido.
                
                  CONTRATO DE DESTINO VÁLIDO:
                  - idCliente (tipo: string, descrição: identificação do cliente ou comprador)
                  - valorTotal (tipo: number, descrição: valor total da transação financeira)
                  - statusPagamento (tipo: string, descrição: situação atual do pagamento)
                  - observacoesLogistica (tipo: string, descrição: campo livre para metadados adicionais não mapeados)
                
                  REGRAS DE MAPEAMENTO:
                  1. Analise cada chave do JSON de entrada.
                  2. Infira a intenção de negócio da chave e encontre a correspondência semântica exata no CONTRATO DE DESTINO.
                  3. Preencha o campo "description" com a sua justificativa semântica para o mapeamento.
                  4. Se uma chave de entrada não tiver correspondência no contrato, defina "matches" como null, ou mapeie para "observacoesLogistica" se for um dado logístico importante, como um endereço.
                
                  FORMATO DE SAÍDA:
                  Você deve retornar ESTRITAMENTE um objeto JSON válido, sem formatação markdown (```json), seguindo exatamente esta estrutura:
                  {
                    "metadata": [
                      {
                        "attribute": "<chave_original_do_payload>",
                        "matches": "<atributo_do_contrato_destino_ou_null>",
                        "description": "<sua_justificativa_semantica>"
                      }
                    ]
                  }
            """.formatted(converter.getFormat());

        // 3. Chamada à IA recuperando o ChatResponse COMPLETO (não apenas a entidade)
        ChatResponse response = chatClient.prompt()
                .system(promptSystem)
                .user("Payload de Entrada: " + payloadSujo.toString())
                .options(OpenAiChatOptions.builder().model(MODEL).build())
                .call()
                .chatResponse();

        // 4. Extração Manual
        // Pegar o texto cru (JSON String) que a IA gerou
        String jsonString = response.getResult().getOutput().getText();
        System.out.println(jsonString);

        // Converter o texto para o Objeto Java usando o conversor do Spring AI
        ResultadoMapeamentoMetadata dto = converter.convert(jsonString);

        long fim = System.currentTimeMillis();

        // 5. Montar o Relatório com os dados reais
        // Nota: O acesso aos tokens pode variar levemente dependendo da versão (0.8.x vs 1.0.x)
        // Se der erro no getUsage(), use apenas 0 por enquanto para destravar.
        Long tokensTotais = 0L;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            tokensTotais = Long.valueOf(response.getMetadata().getUsage().getTotalTokens());
        }

        return new RelatorioAdaptacaoMetadata(
                dto,
                (fim - inicio),
                MODEL, // ou o modelo que estiver configurado no application.properties
                tokensTotais.intValue()
        );
    }
}
