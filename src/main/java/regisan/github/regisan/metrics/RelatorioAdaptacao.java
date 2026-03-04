package regisan.github.regisan.metrics;

import regisan.github.regisan.dto.PedidoLegadoDTO;

public record RelatorioAdaptacao(
        PedidoLegadoDTO dadosNormalizados, // O resultado útil
        long tempoProcessamentoMs,         // Latência (Métrica H2)
        String modeloUtilizado,            // Ex: gpt-3.5-turbo
        int totalTokens                    // Custo
) {}
