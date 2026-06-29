# Google Maps e GPS

O app ja possui:

- permissao de localizacao;
- rastreamento GPS do entregador em entregas ativas;
- mapa de acompanhamento do pedido;
- botao para abrir rota no app de mapas;
- captura opcional da localizacao atual do cliente no checkout.

Para o mapa in-app renderizar, configure uma chave da Google Maps Platform.

## Onde configurar

No arquivo `local.properties` do projeto Android, adicione:

```text
GOOGLE_MAPS_API_KEY=sua_chave_google_maps
```

Nao publique essa chave em repositorio publico.

O projeto Android ja le essa chave em `app/build.gradle.kts` e injeta no `AndroidManifest.xml`.

## APIs necessarias

No Google Cloud Console, habilite:

```text
Maps SDK for Android
```

Recomendado restringir a chave por:

```text
Android apps
Package name: com.goprex
SHA-1 do certificado de assinatura
```

Sem essa chave, o acompanhamento por Firestore/GPS continua funcionando, mas o mapa embutido pode ficar em branco.
