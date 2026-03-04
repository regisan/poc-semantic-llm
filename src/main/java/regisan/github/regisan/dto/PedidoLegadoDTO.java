package regisan.github.regisan.dto;

// O contrato imutável do sistema de destino
public record PedidoLegadoDTO(
        String idCliente,
        Double valorTotal,
        String statusPagamento,
        String observacoesLogistica
) {}