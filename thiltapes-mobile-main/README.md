# Thiltapes

Projeto desenvolvido para a disciplina de **Programação de Dispositivos Móveis** no semestre **2026A** da **Univates**.

## Visão geral

Jogo em que o jogador caça **thiltapes** (criatura cultural alemã) colocados no mapa por um administrador. O admin configura o jogo, posiciona thiltapes (latitude, longitude e foto) e os jogadores tentam encontrá-los. Ao se aproximar da localização, a foto é desbloqueada e fica no dispositivo.

O sistema é composto por **backend** e **aplicativo Android**. Há dois papéis:

- **Jogador:** identifica-se com **ANDROID_ID** (extraído do dispositivo) e um nome de usuário.
- **Administrador:** acesso com senha definida previamente no banco de dados.

Os dados de localização relevantes ficam no **servidor**; o app envia a posição e o servidor devolve o que o jogador pode ver, evitando que a lista de coordenadas seja obtida só inspecionando o cliente.
