# Testar a build Android ARM64

Esta build experimental usa os arquivos extraídos de uma cópia própria de *Midnight Club: Los Angeles Complete Edition* para Xbox 360 (Title ID `545407F8`). O APK contém o programa ARM64; os dados do jogo ficam fora dele.

## Instalação sem root

1. Instale o APK ARM64 criado por `scripts/build_android_apk.sh`.
2. Extraia os arquivos do disco em uma pasta acessível pelo seletor de documentos do Android, por exemplo `Download/MCLA-extraido/`. Não selecione a raiz `Download`: em algumas versões do Android ela não pode ser concedida a um app.
3. Abra **MCLA Recomp**, toque em **Select game folder** e escolha `MCLA-extraido`. Aguarde a cópia terminar; são aproximadamente 6,2 GB. Deixe espaço livre para a pasta de origem e a cópia privada.
4. Depois da importação, toque em **Play**. Nas próximas aberturas, o launcher mostra os botões de driver e **Play**.

O próprio seletor do Android concede ao app leitura da pasta escolhida; não é preciso autorizar acesso geral ao armazenamento. Confirme a seleção quando o Android pedir. Se o acesso for negado, o launcher mostrará uma mensagem e permitirá escolher a pasta novamente. Em Android 11 ou superior, escolha a subpasta `MCLA-extraido`, pois o seletor não permite conceder a raiz `Download`.

Os 13 arquivos precisam estar **diretamente** dentro da pasta selecionada, sem uma subpasta intermediária: `default.xex`, os quatro `xarchive_*.rpf`, os seis vídeos `.bik`, `nxeart` e `root_directory_padding.pad`. O launcher confere todos eles antes de iniciar o jogo, inclusive após uma cópia interrompida. Um `.zip` ou `.iso` fechado não serve no seletor.

## Cópia direta com root

O destino final é:

```text
/data/user/0/org.mcla.recomp/files/game/
```

`/data/data/org.mcla.recomp/files/game/` aponta para o mesmo local neste aparelho. Assim, o caminho de um arquivo é `/data/user/0/org.mcla.recomp/files/game/default.xex`. Instale e abra o APK uma vez para criar o diretório privado, depois copie **o conteúdo** da pasta extraída para `game/` com permissões de leitura para o UID do app. Um gerenciador de arquivos comum não acessa `/data/user/0` sem root; use o importador acima nesse caso.

Não limpe os dados do aplicativo se quiser preservar essa cópia, as configurações e os saves. Atualizar o APK com `pm install -r` preserva os dados do app. Os logs ficam em `files/logs/` e o cache de shaders em `files/mcla/cache/shaders/`.

Em Android 10 ou mais recente, o app também exporta automaticamente os logs de texto para `Documents/MCLA/logs/` pela MediaStore. A exportação ocorre ao abrir o launcher, aos 30 segundos de jogo e depois a cada 60 segundos, além de pausar ou encerrar o jogo. Os arquivos compartilhados têm extensão `.txt` (por exemplo, `mcla_017.txt`); os originais privados continuam em `files/logs/mcla_017.log`. A pasta é criada pelo Android sem permissão ampla de armazenamento. Se o jogo travar antes da próxima exportação, reabrir o launcher copia o log que ficou na pasta privada. A exportação anterior foi verificada; a mudança final de nome de `.log.txt` para `.txt` ainda não foi testada em execução. [Armazenamento compartilhado no Android](https://developer.android.com/training/data-storage/shared/media).

Neste aparelho de desenvolvimento, a pasta pronta está em `/storage/emulated/0/Download/MCLA-extraido/`: os 13 arquivos foram comparados byte a byte com a extração local. O importador foi recompilado e instalado; a detecção de uma cópia privada já completa foi testada. A seleção e cópia pelo seletor Android ainda precisam de um teste ponta a ponta em uma instalação sem dados anteriores.

## Driver Vulkan opcional

O padrão é o driver Vulkan do sistema. No launcher, toque em **Select Turnip (.zip)**, escolha um pacote para ARM64 que contenha `libvulkan_freedreno.so` e espere a mensagem de importação; depois toque em **Play**. O seletor Android concede acesso apenas ao ZIP escolhido, e o driver é copiado para `files/gpu-drivers/` antes de ser carregado. **Use system driver** desativa a seleção Turnip sem apagar o ZIP original. O launcher aparece em toda abertura, inclusive se o jogo falhar ao carregar um driver experimental.

O Turnip depende da GPU Adreno, da versão do Android, do kernel e da versão do pacote. Uma combinação incompatível pode causar tela preta, erro Vulkan ou pior desempenho. O carregador tenta voltar ao driver do sistema quando o AdrenoTools não consegue abrir o Turnip; caso a falha ocorra depois disso, reabra o app e selecione **Use system driver**. Confira em `files/logs/mcla_*.log` a linha `AdrenoTools opened Vulkan for selected Turnip` e, sobretudo, a linha `Vulkan device` com o nome da GPU/driver realmente usado. `AdrenoTools rejected selected driver` indica que houve fallback.

Para reconstruir o APK, execute `scripts/build_adrenotools_android.sh`. Ele fixa `bylaws/libadrenotools` em `8fae8ce254dfc1344527e05301e43f37dea2df80` e o submódulo `liblinkernsbypass` em `aa3975893d83ef1bc84c321ec60c65fbf1287887`, compilando para ARM64 em `out/build/adrenotools-arm64`. No SDK ReXGlue limpo, aplique nesta ordem `patches/rexglue-android-arm64.patch`, `patches/rexglue-turnip-loader.patch` e `patches/rexglue-driver-cache-path.patch`; recompile e instale o SDK, recompile `mcla` e execute `scripts/build_android_apk.sh`. O APK inclui AdrenoTools e seus hooks, mas nenhum driver Turnip fornecido por terceiros. O script também copia o APK verificado para `Download/MCLA-Recomp-arm64.apk` no Termux; `MCLA_DOWNLOADS_DIR` muda a pasta de destino.

O código do cache binário Vulkan usa um arquivo distinto para cada driver/UUID em `files/mcla/cache/shaders/local/`. Ao alternar entre Turnip e sistema, o cache anterior de cada um deve ser preservado. Na primeira abertura após esta atualização, o arquivo antigo `545407F8.vk.bin` só é aceito se o cabeçalho Vulkan corresponder ao driver ativo. O log `mcla_020.log` da comparação anterior registrou `Loaded 0 bytes` porque a troca de Turnip para sistema invalidou o cache único antigo; isso pode causar engasgos na primeira abertura. A nova separação de arquivos compilou, mas ainda não foi verificada em execução.

No RMX5070 realme usado nos testes, os faróis traseiros piscaram em branco tanto com Turnip 8G2-OneUI quanto com Turnip Standard v27.0.0-20261107. O usuário confirmou que o driver Vulkan do sistema eliminou essa piscada. Até investigar a causa no driver/SDK, selecione **USE SYSTEM DRIVER** no launcher para jogar sem o artefato; a opção Turnip continua experimental. O desempenho do sistema pode ser ligeiramente menor segundo o usuário. A GPU foi observada limitada termicamente a 295 MHz durante testes, então uma comparação formal de FPS requer temperatura inicial semelhante.
