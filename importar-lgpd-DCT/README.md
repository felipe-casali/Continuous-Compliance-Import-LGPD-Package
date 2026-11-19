# Pacote LGPD essencial — plugin BR

Carrega no Delphix (Data Control Tower ou Continuous Compliance Engine) uma política de descoberta
com o essencial da LGPD: documentos, nomes, contato, endereço, CEP e data de nascimento. CPF, CNPJ e
telefone são mascarados pelos algoritmos do plugin `BR` (`BR-java17.jar`, nesta pasta).

## O que vai para o Delphix

| | |
|---|---|
| Política de descoberta | `BR - LGPD - essencial` (limiar 60) |
| Classes de dados | 14 |
| Classifiers | 30 (por nome de coluna, tipo, regex e lista) |
| Algoritmos | 13 próprios do pacote, mais `CpfCnpj` e `Telefone` do plugin `BR` |
| Arquivos de lista | 5 (nomes, sobrenomes, ruas…) |

| classe de dados | algoritmo |
|---|---|
| `BR_L1_CPF`, `BR_L1_CNPJ` | `CpfCnpj` (plugin BR) — mantém a pontuação e gera DV válido |
| `BR_L1_TELEFONE` | `Telefone` (plugin BR) |
| `BR_L1_RG` | `BR_CM_DIGITOS` |
| `BR_L1_CNH`, `BR_L1_PASSAPORTE`, `BR_L1_ENDERECO_COMPLEMENTO` | `BR_CM_ALFANUM` |
| `BR_L1_NOME`, `BR_L1_SOBRENOME`, `BR_L1_NOME_COMPLETO` | `BR_NOMES`, `BR_SOBRENOMES`, `BR_NOME_COMPLETO` |
| `BR_L1_EMAIL` | `BR_EMAIL` |
| `BR_L1_ENDERECO` | `BR_ENDERECO` |
| `BR_L2_CEP` | `BR_CEP` |
| `BR_L2_DATA_NASCIMENTO` | `BR_DATA_NASCIMENTO` |

O `CpfCnpj` só mascara CPF e CNPJ válidos; valor com dígito verificador errado passa sem alteração.

## Como rodar

Copie a pasta inteira (`importar-lgpd.ps1`, `BR-java17.jar` e `data\`) e, de dentro dela:

```powershell
powershell -ExecutionPolicy Bypass -File .\importar-lgpd.ps1
```

Funciona no Windows PowerShell 5.1 e no PowerShell 7. O script pergunta o idioma, DCT ou engine, o
endereço, a API key do DCT (ou usuário e senha do engine) e se aceita certificado autoassinado.

- **Plugin BR**: no DCT, se o plugin não estiver instalado, o script oferece instalar o
  `BR-java17.jar` (sobe o JAR por `/masking-file-uploads` e instala por `/masking-plugins`). No engine,
  o plugin precisa já estar instalado.
- **O que já existe**: antes de mudar qualquer coisa, o script lista os objetos que já existem com
  os mesmos nomes e pede confirmação; eles são **apagados** antes da importação. O que o Delphix
  recusar apagar (porque algo o usa) é atualizado. Só objetos gerenciados pelo DCT são tocados; os de
  fábrica são só referenciados.
- **Nomes repetidos**: se o DCT tiver mais de um algoritmo de fábrica com o mesmo nome, vale o do
  plugin `dlpx-core`; os do plugin BR são achados pelo plugin, não só pelo nome.
- **Log**: `import-<data>.log` nesta pasta, sem senha nem chave.
- Rodar de novo retoma de onde parou.

Para rodar sem perguntas, aponte `DLPX_IMPORT_ANSWERS` para um arquivo com uma resposta por linha.

## Origem

Recorte do preset `brazil-lgpd` (v3) do Delphix Masking Helper: o pacote essencial mais a classe de
CNPJ, com CPF, CNPJ e telefone trocados pelos algoritmos do plugin BR
(`felipe-casali/Continuous-Compliance-Import-LGPD-Package`, `BR-java17.jar`: Java 17, API 2026.5).
