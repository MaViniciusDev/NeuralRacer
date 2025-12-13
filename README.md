# 🏁 NeuralRacer — IA de Corrida Autônoma (Java + JavaFX)

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.x-green?style=for-the-badge&logo=apache-maven)

</div>

---

## 📌 Visão Geral

NeuralRacer é um projeto Java/JavaFX que simula um pequeno jogo de corrida 2D e inclui uma implementação de algoritmo genético para treinar carros autônomos — usando exclusivamente os sensores já implementados no jogo. O grande foco é permitir que o usuário visualize em tempo real os carros sendo treinados no canvas, sem alterar a lógica original do jogo.

## 🎯 Objetivos deste repositório

- Visualizar o treinamento em tempo real: múltiplos carros desenhados no canvas e controlados simultaneamente.
- Reaproveitar o desenho e a lógica do `Carro` já existente na aplicação.
- Usar apenas os sensores do sistema para alimentar a IA.
- Permitir configuração simples dos parâmetros do algoritmo genético.

## 📂 Estrutura importante

Principais classes (localizadas em `src/main/java`):

- `org.MaViniciusDev.main.Main` — entrypoint da aplicação.
- `org.MaViniciusDev.view.EditorMapa` — editor de mapa / ponto inicial e botões para iniciar Jogo ou Treinamento.
- `org.MaViniciusDev.view.TreinamentoIA` — tela de treinamento com visualização em tempo real.
- `org.MaViniciusDev.view.Carro` — desenho, física e integração com `SensorSystem`.
- `org.MaViniciusDev.view.SensorSystem` — fornece as leituras usadas pela IA.
- `org.MaViniciusDev.ia.CerebroGenetico` — cromossomo e operações genéticas (mutação, crossover, processamento).


## 🚀 Como executar (IDE — forma mais simples)

1. Abra o projeto na sua IDE (IntelliJ recomendada).
2. Rode a classe `org.MaViniciusDev.main.Main` como aplicação Java.
3. No `EditorMapa`, defina o ponto inicial e a direção.
4. Clique em "Iniciar Jogo" para testar um carro manualmente, ou em "Treinar IA" para abrir a tela de treinamento — você verá vários carros se movendo no canvas durante as gerações.


## 🛠️ Como executar pela linha de comando (Windows PowerShell)

```powershell
mvn -DskipTests clean package
# Se houver jar executável:
java -jar target\nome-do-jar-gerado.jar

# Ou execute a classe Main apontando o JavaFX, se necessário (ajuste PATH_TO_FX):
$PATH_TO_FX = 'C:\path\to\javafx-sdk-XX\lib'
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp target\classes org.MaViniciusDev.main.Main
```


## 🔎 Visualização do Treinamento (o que deve acontecer)

Ao clicar em `Treinar IA`:
- Abre a janela de `TreinamentoIA` com um canvas (1280×720).
- Uma população de carros é instanciada a partir do ponto inicial definido no editor.
- Cada `CarroIA` reaproveita o desenho do `Carro` (sprite) e é adicionado ao `root` para ser desenhado.
- A atualização é feita por um `AnimationTimer`: a lógica (controle + física) e o desenho ocorrem a cada frame, permitindo acompanhar em tempo real.
- Sensores relevantes podem ser desenhados para alguns carros (visual opcional para performance).

Se algo estiver parando os carros (ficam parados no canvas), verifique:
- O `AnimationTimer` está ativo (não foi parado acidentalmente).
- Os carros foram adicionados como nós no `root` (root.getChildren().add(carro)).
- As chamadas de `update()` do carro estão sendo feitas no loop de jogo.


## ⚙️ Parâmetros do Algoritmo Genético (onde ajustar)

Local: `org.MaViniciusDev.view.TreinamentoIA`

- POPULACAO_TAMANHO — número de carros treinados e desenhados.
- Taxa de mutação — variável aplicada durante `evoluirProximaGeracao()`.
- Elitismo — quantos melhores mantemos entre gerações (atualmente mantém 2).
- Critério de parada — número de voltas (ex.: 3 voltas para considerar sucesso) ou número de gerações.

Dica: aumentar muito a população pode reduzir o FPS; use um canvas compartilhado ou reduza a quantidade de detalhes desenhados para melhorar performance.


## 🧪 Debug & Troubleshooting

Problemas comuns e soluções rápidas:

- IndexOutOfBoundsException relacionado a checkpoints
  - Sintoma: Exception com stacktrace apontando para `EditorMapa.treinarIA` ou `TreinamentoIA.atualizarFitness`.
  - Causa provável: `nextCheckpointIndex` foi incrementado além da lista `checkpoints`.
  - Correção segura: antes de acessar `checkpoints.get(nextCheckpointIndex)`, verifique tamanho ou aplique modulo: `nextCheckpointIndex %= checkpoints.size();` ou checagem `if (nextCheckpointIndex >= checkpoints.size()) nextCheckpointIndex = 0;`.

- Carros não aparecem / ficam parados no mapa
  - Verifique se os `CarroIA` foram adicionados ao `root` (nó JavaFX) e se possuem `opacity`/visibilidade.
  - Verifique se o loop `AnimationTimer` está ativo e se o método `update(dt)` do carro é chamado.
  - Confirme que o sprite `car.png` está presente em `resources/images/`.

- Performance ruim com muitas entidades
  - Desenhe apenas o mínimo necessário (pinte o sprite e simplifique os sensores).
  - Considere desenhar tudo em um único `Canvas` em vez de adicionar centenas de nós JavaFX.


## ✅ Recomendações de melhoria (próximos passos)

- Salvar o melhor cromossomo em arquivo para reutilizar o agente treinado.
- UI para ajustar hiperparâmetros em tempo real (tamanho da população, mutação, seleção).
- Destaque visual do melhor carro (ou follow-camera) e gráfico de evolução do fitness.
- Separar lógica de física/IA em thread própria e manter apenas o render na UI thread (cuidado com sincronização).


## 🤝 Como Contribuir

1. Fork o repositório.
2. Crie uma branch: `git checkout -b feature/minha-melhora`.
3. Faça commits pequenos e claros.
4. Abra um Pull Request descrevendo a mudança.


## 📄 Licença

Projeto aberto — ajuste conforme necessário (sugestão: MIT).


---

<div align="center">

**NeuralRacer — Desenvolvido com ❤️**

</div>
