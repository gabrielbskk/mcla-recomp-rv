# Notas para o port nativo Android ARM64

**Estado consolidado:** [docs/android-handoff.md](docs/android-handoff.md). As observações abaixo são cronológicas; frases como “pendente” em uma seção antiga podem ter sido resolvidas em uma seção posterior. O handoff indica o estado da última build e separa testes no aparelho de verificações apenas de compilação.

Este arquivo registra a análise inicial para agentes que continuarem o port. O objetivo é executar o código recompilado diretamente em ARM64/Android, com Vulkan e APIs Android. Um wrapper que rode a build Linux dentro de proot/Box64 não atende ao objetivo final.

## Estado atual

- Repositório: `https://github.com/RenanQueiroz/mcla-recomp.git`
- Clone local inicial: `f656d24dfac102cc41942a457d885aedd5d3db1b` (2026-11-07).
- Host de desenvolvimento disponível nesta sessão: Termux em Android 16, `aarch64`, kernel Android 5.15; o pacote `ndk-sysroot` já está instalado.
- Instalados nesta sessão: CMake, Ninja, Vulkan headers, loader Android, shaderc/glslang/SPIR-V tools, FFmpeg, PipeWire/Wayland, Xss, LLDB e patchelf. Também já havia Clang/LLD, Git, Python e X11 libs.
- Os executáveis `cmake`, `ninja`, `clang`, `glslangValidator`, `glslc`, `spirv-val` e `lldb` estão no PATH.
- Build ARM64 Android e APK de desenvolvimento concluídos neste aparelho em 2026-11-08. O ZIP original fornecido pelo usuário foi extraído localmente; `game/`, ISO, ZIP, código gerado e APK ficam ignorados pelo Git. Após a preparação de shaders, o APK mostrou uma cena 3D e o menu do jogo. O usuário navegou nas opções; o log registrou mudanças de LOD pelo menu, confirmando entrada. A captura de evidência está em `out/android-arm64/screen-current.png` (ignorada pelo Git).
- O APK é empacotado diretamente no Termux por `scripts/build_android_apk.sh`, com `aapt2`, `javac`, D8, `zipalign` e `apksigner`. O script baixa apenas `android.jar` da plataforma 35 e `d8.jar` de Build Tools 35.0.1 quando ausentes. A Activity deriva de SDLActivity e copia o plugin Xenos para a pasta privada que o runtime usa como base.
- O usuário disponibilizou armazenamento remoto privado para artefatos, caso necessário. Endereço, usuário e credenciais ficam fora da documentação pública. Ele não é um ambiente de build.

## Armazenamento remoto autorizado

- Host e conta são privados; solicitar os dados ao proprietário somente se houver uma necessidade real de armazenamento remoto.
- Uso permitido: guardar arquivos do trabalho quando houver necessidade concreta de espaço/transferência, por exemplo artefatos grandes de build ou cópias de segurança de documentação.
- Não assumir que há compiladores, SDKs, rede externa ou capacidade de executar builds nesse host. Não instalar software nem executar tarefas além do armazenamento.
- Antes de transferir, definir e documentar um diretório dedicado (por exemplo `mcla-recomp-port/`) e enviar somente arquivos necessários, excluindo imagens/dados do jogo, chaves e credenciais.
- Não conectar até existir um artefato que realmente precise ser armazenado remotamente. Não registrar a senha em arquivos do projeto.

## Port nativo versus emulação

O projeto já recompila estaticamente o código PowerPC do Xbox 360 para C++ pelo ReXGlue/XenonRecomp e possui backend Vulkan. Isso evita emular a CPU PowerPC. Ainda existe uma camada de compatibilidade para serviços Xbox e uma tradução da API gráfica do console para Vulkan; “nativo” aqui significa código host ARM64 e integração Android/Vulkan, não código originalmente escrito para Android nem remoção da compatibilidade do console.

O Android não é um alvo pronto no fork: a matriz CMake lista Linux/Windows/macOS, e a aplicação Windows/launcher é C#/.NET. O caminho de Android deve partir do host/runtime portátil do ReXGlue e substituir/adaptar janela, eventos, áudio e arquivos para SDL3/Android ou APIs nativas. Reutilizar a build Linux com proot/Box64 seria só um protótipo de compatibilidade, não o produto pretendido.

## Achados no código deste repositório

- `CMakeLists.txt` define C++23 e reúne o host em `src/`; o alvo atual é `mcla` (executável), com dependências carregadas por `generated/rexglue.cmake`.
- `CMakePresets.json` já declara `linux-arm64-*`, mas Linux ARM64 não equivale ao ABI Android. Android usa NDK, sysroot/bionic, linker/SONAME e integração lifecycle diferentes.
- `docs/building.md` descreve build Linux via Clang 20, Ninja, CMake, Vulkan e pacotes de desktop. Essa receita não gera APK e inclui dependências típicas de desktop (X11/GTK/Wayland/áudio).
- O script Linux aponta para `third_party/rexglue-sdk` e para `out/build/linux-amd64-release`; não foi encontrado um script Android existente.
- Há referências SDL no código e mudanças Vulkan extensas no SDK, mas a integração atual é orientada aos hosts desktop. Precisamos identificar o ponto de entrada do `WindowedApp`, o proprietário do loop de eventos e as abstrações de áudio/input antes de desenhar o host Android.
- `generated/` contém os arquivos CMake de integração, não o código recompilado do jogo. O build precisa gerar esse código localmente a partir dos dados legítimos do usuário.

## Projetos de referência examinados

### Black-Android

Repositório: https://github.com/RanggaCasper/Black-Android (MIT). Commit analisado: `8e9c6fc7d570503ea68d039cff16c09339ee6d30`.

É a referência técnica útil. O README descreve recompilação estática Xbox x86 para C++ ARM64, Vulkan para gráficos e SDL3 para janela/input/áudio, sem emulador. A árvore tem `native/vulkan.cpp`, `native/vk_shader.cpp`, `native/game_host.cpp`, `native/input.cpp` e Gradle/CMake para APK. A implementação inclui:

- shaders HLSL convertidos a SPIR-V com glslang em runtime, com especialização de funções que recebem textura/sampler e cache de SPIR-V;
- cache persistente de pipelines Vulkan;
- três frames em voo e buffers de upload reaproveitados;
- seleção de render passes dinâmicos quando disponíveis e fallback para render passes tradicionais;
- timeline semaphores com fallback para fences;
- fallback de input para controladores que Android expõe como eventos de tecla/eixos em vez de SDL Gamepad;
- integração lifecycle Android, logs, armazenamento e dados do dispositivo via SDL/JNI.

Esses conceitos são referências para design e testes. O renderer `native/vulkan.cpp` implementa a tradução específica de Direct3D 8/Xbox desse outro jogo; não é um renderer genérico que possa substituir o renderer Xenos/ReXGlue. O compilador de shaders aceita HLSL daquele pipeline e requer validar as diferenças do bytecode/shader translator do MCLA. Não copiar arquivos antes de verificar licença de cada componente e compatibilidade com GPL-3.0-or-later deste repositório; a licença MIT do projeto não automaticamente cobre todas as dependências/arquivos de terceiros.

### Def-Jam-Android-Native-Port

Repositório: https://github.com/contatg/Def-Jam-Android-Native-Port.

O branch público consultado é um hub de documentação/releases. Ele declara que a implementação e os workflows de build não estão no branch público atual. Portanto, não foi possível auditar renderer, shaders ou código Android desse port. A documentação confirma o modelo de distribuição: cada usuário fornece os próprios dados extraídos do jogo.

## Plano técnico sugerido

1. **Estabelecer o baseline**: manter o checkout upstream/fork intacto, localizar no ReXGlue a configuração de host Android e identificar a saída do codegen ARM64. Não alterar os presets Linux para fingir que são Android.
2. **Provar um binário mínimo Android**: compilar e carregar um `libmcla.so` arm64-v8a com NDK, bionic e o runtime C++ do SDK. Registrar erros de headers, syscalls, exceções, atomics, `dlopen`, threads e filesystem.
3. **Host de app**: criar projeto Android Gradle com SDL3 ou Activity/JNI; criar/destroi surface Vulkan no lifecycle, suspender/retomar threads e áudio em background, armazenar saves/config/cache no diretório privado e importar os dados do jogo via Storage Access Framework.
4. **Vulkan**: começar usando o backend Vulkan do ReXGlue e a surface Android provida por SDL/ANativeWindow. Verificar extensões Android, formato/present mode, perda/recriação de surface, limites de memória e formatos de depth. Manter fallback para render passes/fences caso drivers reais não ofereçam dynamic rendering/timeline semaphores.
5. **Shaders e cache**: primeiro usar a tradução já existente no ReXGlue; medir custo de compilação fria. Persistir pipeline cache por GPU/driver e invalidá-lo quando versão do shader, app, dispositivo ou driver mudar. Só adotar a estratégia runtime HLSL→SPIR-V do Black se o MCLA realmente receber HLSL equivalente e a licença permitir.
6. **Input/áudio**: mapear GameController/SDL Gamepad para os quatro slots XInput do jogo, testar dois pads conectados simultaneamente, hotplug e reconexão. Implementar saída Android/SDL apropriada e validar mixagem/latência.
7. **APK e validação**: otimizar release ARM64; conferir páginas ELF de 16 KB, permissões, assinatura e instalação. Validar em aparelho com Vulkan real; `mesa-vulkan-icd-virtio` instalado no Termux não prova que o APK terá esse ICD, pois Android usa o driver Vulkan do sistema.

## Ferramentas Termux instaladas nesta sessão

Base: `clang`, `lld`, `git`, `python`, `pkg-config`, `make`, `unzip`, `tar` (preexistentes). Adicionadas via `pkg`: `cmake`, `ninja`, `vulkan-headers`, `vulkan-loader-android`, `shaderc`, `ffmpeg`, `pipewire`, `libwayland`, `libwayland-protocols`, `libxss`, `lldb`, `patchelf` e dependências transitivas. Pacotes de desktop como PipeWire/X11/Wayland são úteis para inspecionar/builds auxiliares, mas não constituem o stack do APK.

Antes de uma tentativa real de build, verificar versões (`cmake --version`, `clang --version`), confirmar acesso a Android SDK/NDK/Gradle e espaço livre. O repo pede CMake 3.25+; confirmar que a versão Termux presente satisfaz a configuração. O sistema indicado para teste é Android com driver Vulkan real, não o software ICD VirtIO.

## Segurança, dados e licenças

- Não baixar ISOs, executáveis, chaves, assets ou saves do jogo. Pedir ao usuário o caminho dos dados extraídos de sua cópia legítima, se/quando a etapa de codegen exigir.
- Não adicionar arquivos derivados do jogo, `generated` recompilado, APKs ou caches ao Git.
- Revisar `LICENSE`, `THIRD-PARTY-NOTICES.md`, licenças do ReXGlue, SDL3, glslang, ImGui e qualquer código de referência antes de portar trechos.
- Os dois clones de referência estão em `~/.cache/mcla-references/` e fora do repositório MCLA; não são dependências de build.

## Fontes consultadas

- Projeto MCLA: https://github.com/RenanQueiroz/mcla-recomp
- Build atual: `docs/building.md`, `CMakeLists.txt`, `CMakePresets.json`, `scripts/play_linux.sh`
- Black-Android: https://github.com/RanggaCasper/Black-Android
- Def Jam Android: https://github.com/contatg/Def-Jam-Android-Native-Port
- ReXGlue SDK: https://github.com/rexglue/rexglue-sdk

## Progresso Android ARM64 (2026-11-08)

- ReXGlue já tinha partes do alvo Android (SDL3 configurava Android-36, backend Vulkan reconhece `ANativeWindow`), mas a seleção final de plataforma no CMake sobrescrevia Android com Linux ARM64. Corrigido: `ANDROID` agora seleciona `android-arm64` e define `REX_PLATFORM_ANDROID`; o CMake de UI deixa de compilar `surface_gnulinux.cpp` no Android.
- Adicionada surface SDL/Android em `include/rex/ui/surface_android.h`, que obtém o `ANativeWindow` da propriedade SDL e consulta dimensões nativas. A classe não adquire/release o handle, pois o proprietário é SDL; a surface deve durar menos que a janela.
- Compatibilidade C++23: o libc++ disponível não define `std::chrono::clock_time_conversion`; ampliado o polyfill anteriormente exclusivo de Apple para libc++ sem `__cpp_lib_chrono >= 201907L`.
- Patch reproduzível: `patches/rexglue-android-arm64.patch`. SDK de trabalho: `third_party/rexglue-sdk` (ignorado pelo repositório principal); aplicar o patch sobre o commit pinado antes de configurar.
- Configuração SDK agora conclui como `android-arm64`, Vulkan ON, SDL Android com drivers Vulkan, AAudio/OpenSLES e joystick Android/HIDAPI. Build dos alvos `rexgpu-xenos` e `rexruntime` em andamento; ainda não é build completa do jogo nem APK.
- Uma busca inicial nas contas de armazenamento conectadas pelo usuário não encontrou arquivos MCLA/Midnight Club nem ZIP/ISO do jogo. Um documento de links antigos apareceu em buscas genéricas, mas não foi aberto porque não foi identificado como arquivo do jogo. Downloads locais estavam vazios naquela inspeção; depois o ZIP chegou ao aparelho.
- Novo bloqueio encontrado no build: Bionic removeu `ucontext` (`getcontext`/`swapcontext`), usado pelo runtime POSIX para fibers cooperativos. Adicionado backend Android ARM64 com troca de contexto AArch64 preservando x19–x30, d8–d15 e SP; CMake agora seleciona esse backend só em Android e mantém `ucontext` em Linux/macOS. A compilação foi retomada após essa correção.
- O ZIP do jogo começou a aparecer em `~/storage/downloads`: por enquanto só 9 volumes, numerados `.003`, `.007`, `.009`, `.015`, `.016.jpg`, `.020`, `.021`, `.025`, `.026` (250 MiB cada; 2,20 GiB presentes). Há lacunas, então é uma transferência multipart incompleta; não extrair nem tentar montar até todas as partes chegarem. Não renomeei nem alterei esses arquivos.

## Estado verificado em 2026-11-08, após o download completo

- Os 27 volumes locais foram combinados em `out/game-input/mcla-complete.zip`; o volume `.016.jpg` contém dados ZIP normais e foi usado sem renomear o original. `unzip -t` confirmou o CRC da ISO. `scripts/extract_game.py` extraiu os 13 arquivos do disco para `game/` e o codegen identificou `default.xex`, Title ID `545407F8`, Media ID `5940C9DB`. `diff -qr` entre `game/` e `/data/user/0/org.mcla.recomp/files/game` terminou sem diferenças (executado como root, pois a pasta do app é privada).
- SDK ReXGlue v0.10.0 no commit `f5337cdc947ff6d4c4196737e2c807a48f2a1fc2`; o patch completo do SDK desta árvore está em `patches/rexglue-android-arm64.patch` e inclui as correções prévias do fork mais a adaptação Android. Aplicar uma única vez sobre esse commit limpo. O arquivo antigo de 14 KB foi substituído porque omitia correções de runtime e do empacotamento.
- SDK compilado como `android-arm64`, com `libSDL3.so`, `librexruntime.so` e `librexgpu-xenos.so`. SDL precisa ser compartilhado: a Activity Java chama JNI em `libSDL3.so` e o runtime deve usar a mesma instância. O alvo principal `mcla` produz `libmain.so` e exporta `SDL_main`.
- `scripts/build_android_apk.sh` produz `out/android-arm64/MCLA-arm64.apk` assinado com chave local de depuração. O pacote usa somente `lib/arm64-v8a`. `apksigner verify` passou; `pm install -r` passou; `am start -n org.mcla.recomp/.MclaActivity` abriu a Activity. Para ensaio neste aparelho, os dados extraídos foram copiados para `/data/user/0/org.mcla.recomp/files/game`, com proprietário UID 10690. Esta cópia é privada e não faz parte do APK.
- Revisão do erro inicial: `GetExecutableFolder()` lia `/proc/self/exe` e retornava `/system/bin`, causando erro ao criar logs. No Android agora usa `SDL_GetAndroidInternalStoragePath()` para a base e pasta de usuário. Depois, o plugin Xenos estava instalado na pasta nativa do APK e não na base privada; `MclaActivity` o copia na abertura. Por fim, `CreateFileMappingHandle()` caía em `/dev/ashmem` porque `AndroidInitialize()` nunca era chamado; agora inicializa `ASharedMemory_create` antes de reservar a memória do Xbox. Essas três falhas foram identificadas pelo log do app instalado, corrigidas e o lançamento repetido.
- O log privado `files/logs/mcla_004.log` confirmou `VK_KHR_android_surface`, GPU Adreno 740, criação de swapchain, reserva da arena do guest, 30.094 funções guest mapeadas, inicialização de shader storage, entrada no código do jogo, gráficos Vulkan e áudio 48 kHz. A primeira captura ficou preta durante a preparação de shaders. No lançamento seguinte, `mcla_005.log` registrou dezenas de pipelines gráficos criados e mudanças de opções via menu (`mcla_city_lod`, `mcla_traffic_lod`, `mcla_traffic_distance`). A captura `out/android-arm64/screen-current.png` mostra carro, rua e menu renderizados. O processo continuava vivo após mais de um minuto. Esta é uma build jogável localmente, com a cópia do jogo já presente na pasta privada do app; outras fases e controles ainda merecem ensaios.
- O shader pack pré-compilado não está presente em `files/shaderpack/545407F8.shaderpack`; o runtime tentou iniciar com o storage existente. `gamecontrollerdb.txt` também não está no pacote; SDL informou que iniciou seu driver de input mesmo assim. Ainda não foi testada resposta dos controles no jogo.

### Comandos para continuar

```sh
cd ~/mcla-recomp
cmake --build third_party/rexglue-sdk/out/build/android-arm64 --target rexgpu-xenos rexruntime rexglue -j 4
cmake --install third_party/rexglue-sdk/out/build/android-arm64 --prefix "$PWD/third_party/rexglue-sdk/out/install/android-arm64"
cmake --build out/build/mcla-android-arm64 --target mcla -j 4
scripts/build_android_apk.sh
su -c 'pm install -r /data/data/com.termux/files/home/mcla-recomp/out/android-arm64/MCLA-arm64.apk'
su -c 'am start -n org.mcla.recomp/.MclaLauncherActivity'
```

O SDK foi configurado com `-DANDROID=ON -DREXGLUE_USE_VULKAN=ON`; a raiz com `-DANDROID=TRUE` e `-DCMAKE_PREFIX_PATH=$PWD/third_party/rexglue-sdk/out/install/android-arm64`. No Termux o Clang já aponta para Android ARM64; `-DANDROID` seleciona os ramos Android no CMake. A recompilação do código PowerPC para C++ usa `mcla_manifest.toml` e os dados locais em `game/`.

## Fullscreen e tela preta (2026-11-08)

- `android/MclaActivity.java` força o modo imersivo após `SDLActivity.onCreate` e quando a janela recupera o foco. O APK foi recompilado, assinado, instalado com `pm install -r` e aberto pela `MclaLauncherActivity`.
- A captura `out/android-arm64/screen-fullscreen.png` mostra os créditos em 2340×1080 sem barras do sistema; `screen-fullscreen-2.png` mostra o carregamento e `screen-fullscreen-3.png` mostra o jogo na rua com HUD. O processo continuou vivo até essa última captura. A sobreposição de gravação visível na segunda captura pertence ao sistema Android.
- O log `files/logs/mcla_008.log` registrou 676 pipelines Vulkan carregados do storage e pipelines adicionais durante a entrada no jogo. A tela preta vista no início não persistiu neste ensaio; a causa exata da ocorrência anterior ainda não foi isolada. Para capturar uma nova ocorrência, executar `scripts/collect_android_logs.sh` logo após ela e guardar a captura de tela.

## Perfil de desempenho no Android (2026-11-08)

- No aparelho RMX5070/Adreno 740, `mcla_009.log` levou 26,4 s de `Initializing shader storage` a `Created 805 graphics pipelines from Vulkan storage`. O storage de descrições (`files/mcla/cache/shaders/shareable/545407F8.fbo.vk.xpso`) já existe. O cache binário opcional `vulkan_pipeline_cache_file` estava desligado. Um ensaio com ele ligado começou com `Loaded 0 bytes of VkPipelineCache data`, mas o usuário fechou o jogo antes do fim da carga; nenhum `.vk.bin` foi gravado, portanto não há comparação válida. A opção experimental foi removida do `mcla.toml` após o ensaio.
- Com `present_log` temporariamente ativo, as últimas 10 s do CSV capturado tiveram 118 refreshes (cerca de 12/s), mediana de 35,8 ms, percentil 90 de 171,6 ms e máximo de 976 ms. O arquivo bruto está em `out/android-arm64/present-profile.csv` (ignorado pelo Git). `simpleperf stat` observou 3,29 CPUs ocupadas durante 8 s. O processo estava em torno de 1,3 GiB RSS.
- A GPU marcou 94 a 99% de uso enquanto o teto de clock em `/sys/class/kgsl/kgsl-3d0/max_gpuclk` caiu de 719 MHz disponíveis a 295 e depois 220 MHz; `thermal_pwrlevel=7`. Temperaturas reportadas por sensores CPU/GPU ficaram em torno de 60 a 70 °C. O aparelho limitou o clock durante a partida. Isso torna o cache de pipelines útil sobretudo para carga inicial; não há evidência de que ele sozinho eleve o FPS sustentado. Evitar forçar clocks com root.
- O APK agora declara `android:appCategory="game"` e a Activity ativa `Window.setSustainedPerformanceMode(true)` somente se `PowerManager.isSustainedPerformanceModeSupported()` disser que o aparelho oferece o recurso. O APK compilou, passou `apksigner verify` e foi instalado; o modo ainda precisa ser observado num lançamento e comparado com a build anterior. [API Android](https://developer.android.com/reference/android/os/PowerManager#isSustainedPerformanceModeSupported()).
- `scripts/collect_android_logs.sh` coleta também clock/uso da GPU, sensores relevantes e o CSV de apresentação quando presente. A próxima comparação deve usar a mesma cena, temperatura inicial semelhante e pelo menos 1 minuto de jogo. Não assumir ganho de FPS apenas porque o cache reduziu o tempo de carga.

## Importação de dados para teste (2026-11-08)

- Os 13 arquivos extraídos do disco próprio do usuário foram copiados para `/storage/emulated/0/Download/MCLA-extraido/` (6,2 GB) e `diff -qr` confirmou igualdade byte a byte com `game/`. A pasta de origem e a cópia privada existente foram preservadas.
- O launcher pede a pasta extraída via `ACTION_OPEN_DOCUMENT_TREE`, que concede leitura apenas à árvore escolhida. A seleção de `Download` como raiz é restrita em Android 11+; selecionar `MCLA-extraido` dentro dela. Nenhuma permissão ampla de armazenamento é necessária para este fluxo.
- O importador agora confere os 13 arquivos, copia apenas esses arquivos em ordem estável, mostra o número do arquivo e MiB copiados, mantém a tela ativa durante a transferência, remove o arquivo `.partial` quando uma cópia falha e mostra um erro específico de acesso negado. O APK foi recompilado, assinado e instalado com `pm install -r`, preservando a pasta privada do app (13 arquivos).
- A Activity abre diretamente o jogo quando os 13 arquivos privados já existem. O fluxo com o seletor e uma instalação inicialmente vazia ainda não teve teste ponta a ponta; executar em um pacote de teste isolado ou aparelho limpo antes de distribuir o APK.
- Um pacote isolado `org.mcla.recomp.importtest` e arquivos fictícios foram preparados para ensaiar o seletor, mas o aparelho estava com outro app em uso; o ensaio de interface foi interrompido sem concluir a importação. O pacote isolado foi desinstalado e a pasta fictícia removida após conferir seu conteúdo. Não automatizar toques na interface enquanto o usuário usa o aparelho. A pasta real `Download/MCLA-extraido/` e os 13 arquivos privados do app principal continuaram presentes.

## Verificação de empacotamento ARM64

- As cinco bibliotecas do APK (`libmain`, `librexgpu-xenos`, `librexruntime`, `libSDL3`, `libc++_shared`) têm todos os segmentos ELF `LOAD` com alinhamento mínimo de 0x4000. `readelf -d` mostrou dependências apenas entre bibliotecas empacotadas e bibliotecas do Android (`libc`, `libm`, `libdl`, `liblog`, `libandroid`, `libOpenSLES`). Não há dependência dinâmica de bibliotecas de desktop.
- `scripts/build_android_apk.sh` agora falha se alguma biblioteca não for ELF64 AArch64 ou se algum segmento ELF `LOAD` tiver alinhamento inferior a 16 KiB. Depois chama `zipalign -P 16`, `apksigner verify` e `zipalign -c -P 16 4` no APK final. A nova build passou nessas verificações e `pm install -r` retornou `Success`. O aparelho de teste usa páginas de 4 KiB; execução em hardware de 16 KiB ainda precisa ser testada.

## Cache Vulkan Android, nova build

- `src/mcla_app.cpp` agora define `vulkan_pipeline_cache_file=true` por padrão apenas no Android, sem sobrescrever um valor escolhido pelo jogador em `mcla.toml`. Isso mantém o cache binário do driver além das descrições de shaders já persistidas. A opção pode ser desativada com `vulkan_pipeline_cache_file = false` no arquivo de configuração se o driver responder pior.
- O patch do SDK antecipa a primeira solicitação de gravação do cache para o próximo `UpdateShaderStorage` após a criação dos pipelines da abertura. A gravação permanece na thread de storage, via arquivo temporário e rename, no caminho privado `files/mcla/cache/shaders/local/545407F8.vk.bin`.
- `rexgpu-xenos` e `libmain.so` foram recompilados para ARM64, o patch completo foi atualizado e `git apply --check --cached` passou sobre o índice limpo do SDK. O APK foi assinado, passou nas checagens de ELF64 AArch64/16 KiB e instalado com `pm install -r`; os 13 arquivos privados do jogo foram preservados.
- Para repetir a comparação, anotar `Loaded N bytes of VkPipelineCache data` e `Created N graphics pipelines` em duas aberturas consecutivas, junto com temperatura e cadência durante a partida. O cache não deve ser tratado como ganho automático de FPS.

### Ensaio de duas aberturas no RMX5070

- `mcla_013.log`: a primeira abertura registrou `Loaded 0 bytes of VkPipelineCache data` às 07:43:37.850 e `Created 822 graphics pipelines from Vulkan storage` às 07:43:53.641 (15,8 s entre as linhas). O arquivo privado `545407F8.vk.bin` foi criado com 18.276.386 bytes. O teste fechou a primeira sessão depois de confirmar o arquivo.
- `mcla_014.log`: a segunda abertura registrou `Loaded 18276386 bytes of VkPipelineCache data` às 07:44:14.751 e `Created 822 graphics pipelines from Vulkan storage` às 07:44:15.218 (0,47 s entre as linhas). O mesmo número de pipelines foi carregado, com o cache do driver. A captura `out/android-arm64/screen-cache-warm.png` mostra a cena de direção e o HUD renderizados depois da segunda abertura.
- Uma amostra de 10 s da SurfaceFlinger nessa segunda sessão indicou aproximadamente 13,1 apresentações/s, mediana de 66,7 ms, percentil 90 de 116,8 ms e máximo de 241,9 ms. No mesmo período, `gpu_busy_percentage` foi 63% e o teto de clock da GPU estava reduzido a 348 MHz. O cache melhorou claramente a preparação dos pipelines nesse par de execuções, mas o FPS sustentado segue baixo e limitado por carga/temperatura; não atribuir ganho de FPS de jogo ao cache.

## Reflexos dos carros e seleção de driver

- O usuário observou uma piscada preta breve em veículos. O arquivo privado `mcla.toml` tinha `mcla_env_map_interval = 4`; esse passe desenha o ambiente refletido na pintura dos carros a cada quatro quadros. Voltamos a `0` (todo quadro), recompilamos e instalamos o APK. O usuário confirmou que a piscada sumiu e percebeu melhora no desempenho. A captura única `out/android-arm64/screen-reflection-retest.png` mostra uma cena renderizada, mas a confirmação da piscada veio do teste visual do usuário, pois uma imagem estática não comprova a ausência de artefato transitório.
- O preset Low-end ainda definia o intervalo como 4. Agora essa alteração é aplicada só em desktop; no Android os reflexos permanecem atualizados a cada quadro. `src/perf_options.cpp` foi recompilado, o APK passou as verificações ARM64/16 KiB e `pm install -r` retornou `Success`.
- O launcher agora permanece antes do jogo, com **Selecionar Turnip (.zip)**, **Usar driver do sistema** e **Jogar**. O Turnip é opcional, importado para a pasta privada e só é solicitado pelo processo Vulkan após o botão Jogar. O SDK usa AdrenoTools para abrir o driver selecionado e mantém o Vulkan do sistema como padrão e fallback quando não consegue abrir a biblioteca customizada. A seleção por ZIP e a execução real com o pacote fornecido pelo usuário foram verificadas no teste abaixo.
- Dependência nova: `bylaws/libadrenotools` em `8fae8ce254dfc1344527e05301e43f37dea2df80` e submódulo `liblinkernsbypass` em `aa3975893d83ef1bc84c321ec60c65fbf1287887`, ambos BSD-2-Clause. O patch incremental `patches/rexglue-turnip-loader.patch` deve ser aplicado depois de `patches/rexglue-android-arm64.patch`. O APK empacota AdrenoTools e hooks, nunca um driver Turnip externo. Ver `docs/android-testing.md` e `THIRD-PARTY-NOTICES.md`.
- ZIP recebido em Downloads: `Turnip-v27.0.0-20261107-8G2-OneUI.zip` (2.564.416 bytes), com `libvulkan_freedreno.so` ELF64 AArch64 (8.362.816 bytes) e `meta.json`. O pacote declara Vulkan 1.4.367, `minApi` 28 e ajuste UBWC para Adreno 740 / Snapdragon 8 Gen 2 em firmware novo com perfil One UI. O aparelho RMX5070 é realme, SM8550, Android 16. O chipset coincide, mas o perfil One UI não é específico deste firmware; qualquer teste deve verificar visual e desempenho e manter o botão de retorno ao driver do sistema.
- `unzip -t` confirmou o CRC do ZIP; SHA-256: `9b9f1fbf88e31773badd386edeb5799aa21f57f35f11cb4855b05992c341f09c`. O app importou `libvulkan_freedreno.so` e gravou `files/gpu-drivers/selected.txt`. Em `mcla_016.log`, o AdrenoTools abriu o Vulkan customizado às 08:05:30.981, o dispositivo detectado foi `Turnip Adreno (TM) 740`, API 1.4.367, `driverID: MesaTurnip`; 829 pipelines foram criados e o jogo chegou à cena renderizada em `out/android-arm64/mcla-turnip-test2.png`. A primeira captura durante a carga estava preta, mas a seguinte mostrou a cena. Isso valida o carregamento e a apresentação inicial; desempenho sustentado e possíveis artefatos em jogo ainda exigem observação.

## Revisão de 08:12: faróis, variante Turnip e logs públicos

- As capturas `Screenshot_20261108_081226.jpg`, `081229.jpg` e `081232.jpg` mostram o carro parado a 0 mph: lanternas traseiras escuras nas duas primeiras e brancas na terceira. A gravação `out/android-arm64/mcla-rear-test.mp4` mostra a alternância durante o jogo. O usuário confirmou a piscada e esclareceu que o FPS já estava baixo antes da última alteração. `mcla_017.log` confirma Turnip 1.4.367 e tem um aviso isolado de quadro pulado por placeholder assíncrono, mas isso não prova a causa da piscada repetida.
- O autor da variante `8G2-OneUI` diz que ela ativa `enable_tp_ubwc_flag_hint` para firmwares cujo driver do sistema usa a mesma opção; em outros aparelhos com Snapdragon 8 Gen 2 ela pode causar o glitch. O aparelho é realme SM8550. Baixamos a variante Standard da mesma versão de [Banners-Turnip v27.0.0-20261107](https://github.com/The412Banner/Banners-Turnip/releases/tag/v27.0.0-20261107), validamos CRC e metadata e deixamos o ZIP em `Download/Turnip-v27.0.0-20261107-Standard.zip`; SHA-256 `a38fbce17c6e6364a9968b0782e303487e64fcd68d988a1c3c9a59d60a54ab28`. A biblioteca privada selecionada foi trocada pela Standard após o jogo fechar, preservando o ZIP OneUI original. O teste visual da Standard ainda falta; não considerar a hipótese confirmada antes de comparar as mesmas luzes e o FPS.
- O launcher está em inglês; `out/android-arm64/mcla-launcher-standard.png` mostra os botões **SELECT TURNIP (.ZIP)**, **USE SYSTEM DRIVER** e **PLAY**. As conversas com o usuário seguem em português, conforme esclarecido por ele.
- O novo `android/MclaLogExporter.java` exportou 16 logs para `Documents/MCLA/logs/` via MediaStore, sem pedir permissão ampla. No primeiro ensaio, o Android anexou `.txt` a nomes `.log`, produzindo `.log.txt`; a revisão seguinte passa a publicar `.txt` diretamente e renomeia as cópias anteriores. O código agenda exportação no launcher, durante o jogo e ao pausar/encerrar. A revisão de nomes e periodicidade ainda requer ensaio após a próxima instalação.
- O usuário testou a variante Standard em `mcla_019.log` e informou que a piscada branca continua. Nas capturas `Screenshot_20261108_082217.jpg`, `082221.jpg` e `082223.jpg`, a mesma lanterna traseira passa de escura para branca com o carro parado a 0 mph. Portanto a diferença OneUI/Standard não resolve esse bug. A comparação com o driver do sistema na mesma cena ainda está pendente; não atribuir a causa ao Turnip sem esse teste. A GPU estava limitada a 295 MHz durante parte dos ensaios, então FPS medido nesse momento não compara as variantes de modo justo.

## Fechamento da última build solicitada

- A comparação com o driver do sistema foi feita em `mcla_020.log`: `Adreno (TM) 740`, Vulkan 1.3.128. A gravação `out/android-arm64/mcla-system-rear-gameplay.mp4` mostra lanternas normais durante a direção e ao frear, sem o clarão branco visto com os dois Turnip. O usuário confirmou que a piscada sumiu com o sistema, mas percebeu desempenho um pouco inferior e stutters. O marcador Turnip foi movido para `files/gpu-drivers/selected.txt.turnip-backup`; o ZIP OneUI e o ZIP Standard continuam em Downloads. A opção Turnip pode ser reativada importando o ZIP no launcher.
- A primeira volta ao driver do sistema registrou `VulkanPipelineCache: The pipeline cache file is from another device or driver` e `Loaded 0 bytes of VkPipelineCache data`. Isso contribui para engasgos de preparação de pipelines nessa abertura. A revisão `patches/rexglue-driver-cache-path.patch` salva arquivos separados por Title ID, vendor ID, device ID, driver version e UUID do cache Vulkan no Android; valida o cabeçalho e aceita o arquivo antigo como migração apenas se combinar. Não promete ganho de FPS sustentado.
- O usuário pediu que o launcher inteiro fique em inglês e que logs sejam exportados automaticamente para `Documents/MCLA/logs/`; o código Java e `docs/android-testing.md` foram atualizados. A primeira implementação MediaStore exportou 18 logs íntegros, mas criou nomes `.log.txt`; a revisão final usa `.txt`, preservando os `.log` privados. Java compila com Android 35; resta conferir o nome final após o último APK.
- O usuário pediu uma última build, cópia do APK em Downloads e documentação para retomar builds, depois parar os testes. `scripts/build_android_apk.sh` agora copia o APK verificado para `Download/MCLA-Recomp-arm64.apk`. Não iniciar novos ensaios de gameplay após empacotar; somente verificações de build/assinatura/arquivo e documentação necessárias para entregar essa versão.
- Build final em 2026-11-08: `cmake --build out/build/mcla-android-arm64 --target mcla -j 4` terminou em 112/112, linkando `libmain.so` com exit code 0. `scripts/build_android_apk.sh` compilou o Java, verificou as dez bibliotecas ELF64 AArch64 com segmentos `LOAD` alinhados a 16 KiB, assinou e validou o APK, e o copiou para `/storage/emulated/0/Download/MCLA-Recomp-arm64.apk` (105 MiB). O APK no diretório `out/android-arm64/` e a cópia em Downloads têm SHA-256 `94b5fc1ac47556efa790b858099ed96226fff9f15c5826de44133854a519de3e`. `git diff --check` passou. Não foi feito novo teste de gameplay desta build, conforme pedido; a migração dos nomes dos logs para `.txt` e o cache por driver compilaram, mas ainda precisam de validação em execução futura.
