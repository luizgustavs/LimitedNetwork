# LimitedNetwork

Um bloqueador de dominios de anuncios para Android que funciona por meio de
uma VPN local. O aplicativo intercepta consultas DNS e as bloqueia localmente 
quando encontra pertence a uma rede de ads.

<p align="center">
  <a href="https://github.com/luizgustavs/LimitedNetwork/releases/latest/download/LimitedNetwork-1.0.1.apk">
    <img alt="Baixar APK" src="https://img.shields.io/badge/Baixar_APK-ultima_versao-2563EB?style=for-the-badge&logo=android&logoColor=white">
  </a>
</p>

## Recursos

- Bloqueio local de dominios de anuncios e rastreamento.
- Nao exige acesso root.
- Usa a API `VpnService` do Android somente para filtrar consultas DNS.
- Exibe o estado da VPN e as consultas DNS recentes.
- Encaminha consultas permitidas aos servidores DNS da rede, com fallback para
  Cloudflare (`1.1.1.1`) e Google (`8.8.8.8`).

## Requisitos

- Android 11 ou mais recente (API 30+).
- Permissao do Android para criar uma conexao VPN local.

## Instalacao

1. Baixe o APK pelo botao acima.
2. Autorize a instalação de aplicativos dessa fonte, caso o Android solicite.
3. Instale e abra o LimitedNetwork.
4. Ative o filtro e aceite a solicitacao de VPN do Android.

O Android exibe o indicador de VPN enquanto o filtro esta ativo. O trafego nao
é enviado para nenhum servidor ou VPN externo; a interface VPN é criada no proprio
dispositivo para processar DNS.

## Compilando

Clone o repositorio e execute:

```powershell
.\gradlew.bat assembleDebug
```

O APK de desenvolvimento sera gerado em
`app/build/outputs/apk/debug/app-debug.apk`.

Para gerar a variante de release:

```powershell
.\gradlew.bat assembleRelease
```

Uma distribuicao publica deve ser assinada com uma chave propria. Nunca envie
o arquivo de keystore ou suas senhas ao repositorio.

## Lista de bloqueio

A lista em `app/src/main/assets/hosts.txt` e derivada da lista padrao do
[AdAway](https://github.com/AdAway/adaway.github.io), licenciada sob
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/). Consulte
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) para os creditos.

## Licenca

O codigo e o conteudo original do LimitedNetwork sao disponibilizados sob a
[Creative Commons Attribution 3.0 Unported](LICENSE). Ao redistribuir ou adaptar
o projeto, preserve a atribuicao ao autor e um link para este repositorio.
Materiais de terceiros continuam sujeitos as respectivas licencas descritas em
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Copyright (c) 2026 luizgustavs.
