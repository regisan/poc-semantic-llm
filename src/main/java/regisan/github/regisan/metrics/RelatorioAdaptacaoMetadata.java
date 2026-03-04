package regisan.github.regisan.metrics;

import regisan.github.regisan.dto.ResultadoMapeamentoMetadata;

public record RelatorioAdaptacaoMetadata(
        ResultadoMapeamentoMetadata metadados, // O resultado útil
        long tempoProcessamentoMs,         // Latência (Métrica H2)
        String modeloUtilizado,            // Ex: gpt-3.5-turbo
        int totalTokens                    // Custo
) {}
