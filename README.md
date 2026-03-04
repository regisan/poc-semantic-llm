# How to test

## OpenAI API key
Configure your API key in `application.properties` file

## Endpoint
URL: http://localhost:8080/api/adapter/extrair-metadados

## Example of Request:
```
curl POST 'http://localhost:8080/api/adapter/extrair-metadados' \
  --header 'Content-Type: application/json' \
  --body '{
    "customer": "Pedro", 
    "amount": 100.0, 
    "client_category": "VIP",
    "foo": "bar"
}'
```

## Example of Response

```
{
    "metadados": {
        "metadata": [
            {
                "attribute": "customer",
                "matches": "idCliente",
                "description": "A chave 'customer' se refere ao cliente ou comprador, correspondente ao atributo 'idCliente' do contrato de destino."
            },
            {
                "attribute": "amount",
                "matches": "valorTotal",
                "description": "A chave 'amount' representa o valor total da transação, que se alinha com o atributo 'valorTotal' do contrato."
            },
            {
                "attribute": "client_category",
                "matches": null,
                "description": "A chave 'client_category' não possui um mapeamento direto; portanto, não se encaixa nos atributos do contrato."
            },
            {
                "attribute": "foo",
                "matches": null,
                "description": "A chave 'foo' não é relevante para o contrato de destino e não possui correspondência semântica."
            }
        ]
    },
    "tempoProcessamentoMs": 8905,
    "modeloUtilizado": "gpt-4o-mini",
    "totalTokens": 588
}
```