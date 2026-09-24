# Dieta fixa, porções, edição de lembretes e tela da dieta — Design

Data: 2026-09-23 · App: Nudge (`dev.viniciuscole.nudge`) · Base: `main` @ `de5d224`

## Objetivo

Ajustar o app ao uso real: uma dieta que se repete todo dia, digitada em medidas caseiras, com lembretes editáveis, uma meta de calorias própria e uma tela que mostra o total da dieta.

## Restrição principal — não perder dados

O app roda como APK de uso pessoal e já tem refeições e lembretes gravados no celular. Toda mudança deve:

- **Não reescrever nem apagar** dados existentes. Nenhuma migração de formato.
- Adicionar **somente campos opcionais** ao JSON gravado. Um dado antigo, sem esses campos, tem que continuar lendo exatamente como hoje.
- Manter **sem alteração** os testes `ReminderJsonTest.readsTheFormatAlreadyStoredOnDevices` e `MealJsonTest.readsTheFormatAlreadyStoredOnDevices`. Se algum precisar mudar para passar, o design está errado.
- Continuar assinando com `keystore/debug.keystore`, para a instalação atualizar por cima.

## Escopo

1. Refeição fixa por lembrete (a dieta se repete todo dia)
2. Porções caseiras por alimento
3. Editar lembrete
4. Meta de calorias configurável
5. Tela "Sua dieta" com os totais
6. Novo mapa de toques nos cards da tela inicial

Fora do escopo: metas por macro, água na tela da dieta, medidas genéricas (xícara/colher para qualquer alimento), unidades nos 15 alimentos em inglês, histórico por dia.

## 1. Refeição fixa por lembrete

Hoje o montador abre a refeição **do dia** (`forReminderOn(reminderId, LocalDate.now())`), então amanhã ele aparece vazio.

**Mudança:** o montador abre a refeição **mais recente daquele lembrete, de qualquer data**, e salvar atualiza essa mesma refeição (mesmo `id`, `date` atualizada para hoje).

- `MealRepository.latestFor(reminderId): SavedMeal?` — maior `date`; empate, maior `id`.
- `MealBuilderViewModel` passa a usar `latestFor` na carga e no salvamento.
- Refeições de dias anteriores continuam gravadas e intocadas; só deixam de ser a "mais recente". Nenhuma é apagada.
- O formato gravado não muda.

## 2. Porções caseiras

### Modelo

- Novo `data class Portion(singular: String, plural: String, grams: Double)`.
- `FoodItem` ganha `portion: Portion? = null`.
- `Ingredient` ganha três campos opcionais: `portionSingular: String?`, `portionPlural: String?`, `portionGrams: Double?`.
- **A quantidade continua gravada em gramas/ml (`qty: Int`).** A porção é só a forma de digitar e exibir. Cálculo de calorias e macros não muda.

### Persistência

- `MealJson` grava os três campos quando existem e lê com `opt*`, tratando ausência como `null`.
- Ingrediente antigo (sem os campos) → porção inferida na carga: se o nome bater exatamente com um item do catálogo que tem porção, usa a dele; senão, fica sem porção (gramas, como hoje).

### Catálogo — porções definidas

Somente nos itens em português. Os demais seguem em gramas.

| Alimento | Porção (singular / plural) | g ou ml |
|---|---|---|
| Pão francês | pão / pães | 50 |
| Pão de forma integral | fatia / fatias | 25 |
| Pão de queijo | unidade / unidades | 20 |
| Tapioca (goma) | colher de sopa / colheres de sopa | 15 |
| Aveia em flocos | colher de sopa / colheres de sopa | 15 |
| Arroz branco cozido | colher de servir / colheres de servir | 45 |
| Arroz integral cozido | colher de servir / colheres de servir | 45 |
| Feijão carioca cozido | concha / conchas | 80 |
| Feijão preto cozido | concha / conchas | 80 |
| Lentilha cozida | concha / conchas | 80 |
| Ovo cozido | ovo / ovos | 50 |
| Ovo frito | ovo / ovos | 50 |
| Leite integral | copo / copos | 200 |
| Leite desnatado | copo / copos | 200 |
| Iogurte natural | pote / potes | 170 |
| Iogurte grego | pote / potes | 100 |
| Queijo minas frescal | fatia / fatias | 30 |
| Queijo mussarela | fatia / fatias | 15 |
| Requeijão cremoso | colher de sopa / colheres de sopa | 30 |
| Manteiga | colher de chá / colheres de chá | 5 |
| Banana prata | banana / bananas | 70 |
| Maçã | maçã / maçãs | 130 |
| Laranja | laranja / laranjas | 150 |
| Azeite de oliva | colher de sopa / colheres de sopa | 13 |
| Óleo de soja | colher de sopa / colheres de sopa | 13 |
| Açúcar refinado | colher de chá / colheres de chá | 5 |
| Mel | colher de sopa / colheres de sopa | 20 |
| Castanha-do-pará | unidade / unidades | 4 |
| Whey protein (pó) | scoop / scoops | 30 |
| Café coado sem açúcar | xícara / xícaras | 50 |
| Suco de laranja natural | copo / copos | 200 |

Pesos são referência caseira comum; o usuário pode ajustar depois.

### Montador

- Adicionar alimento **com** porção → entra com **1 porção** (`qty = portion.grams`). Sem porção → 100 g / 10 ml, como hoje.
- Linha do ingrediente com porção: o campo mostra e aceita a **quantidade em porções**, com vírgula ou ponto decimal ("2", "0,5", "1.5"). Abaixo ou ao lado, o rótulo `"2 pães · 100 g"`. `qty = round(porções × grams)`, limitado a 0..2000.
- Botões +/− andam **1 porção** (com porção) ou o passo atual de 10 g / 5 ml (sem porção).
- Singular quando a quantidade é exatamente 1; plural caso contrário (inclusive 0,5 e 0).
- Exibição da quantidade em porções: até 1 casa decimal, sem zero à direita ("1", "1,5", "0,5").

## 3. Editar lembrete

- Rota `edit/{reminderId}`. Reaproveita `AddReminderScreen`/`AddReminderViewModel` com um modo edição.
- `AddReminderViewModel(app, editId: Long?)`: com `editId`, preenche o `Draft` a partir do lembrete salvo.
- Título "Editar lembrete". Botão principal "Salvar alterações".
- **Tipo travado** na edição: o seletor refeição/água fica desabilitado.
- Salvar em edição: `upsert` com o **mesmo `id`**, preservando `enabled`; depois `scheduler.schedule`. Nunca cria outro lembrete.
- Botão secundário **"Testar alarme"**: para lembrete insistente, inicia o serviço e a tela de alarme (comportamento atual da prévia); para não insistente, posta a notificação suave.
- Se o lembrete não existir mais (apagado), a tela volta.

## 4. Meta de calorias

- Novo `SettingsRepository` (SharedPreferences `settings`, chave `kcal_goal`), `StateFlow<Int>`, padrão **1800**. Valores aceitos 500..6000.
- `NudgeApp.settings`.
- `Nutrition.dayShare(kcal, dayGoal)` passa a receber a meta do repositório.
- String `goal_note` passa a ter a meta como parâmetro: EN `≈ %1$d%% of a %2$s kcal day`, pt-BR `≈ %1$d%% de um dia de %2$s kcal`, com a meta formatada no locale (1,800 / 1.800).

## 5. Tela "Sua dieta"

- Rota `diet`. Acesso: tocar no card de estatística "Refeições" da tela inicial (que ganha um "›").
- Conteúdo:
  - Topo: total de kcal × meta, barra de progresso (limitada a 100% no desenho, texto mostra o valor real). Tocar na meta abre um diálogo para digitar outra.
  - Card de macros: proteína, carbo e gordura em gramas e %, mesmo estilo do rodapé do montador.
  - Lista das refeições: nome, horário, kcal e P/C/G. Tocar abre o montador daquela refeição. Sem ingredientes → "Vazia — toque para montar".
- Entram somente lembretes de **refeição ativos**, na ordem do horário. Para cada um, usa `latestFor(reminderId)`.
- Totais: soma dos ingredientes de todas essas refeições via `Nutrition.totals` sobre a lista concatenada.
- `DietViewModel` expõe um `DietUiState` combinando reminders, meals e settings.

## 6. Toques na tela inicial

| Ação | Card de refeição | Card de água |
|---|---|---|
| Tocar | abre o montador | registra +1 copo, com snackbar "Copo registrado · Desfazer" |
| Segurar | abre a edição | abre a edição |

- Nenhum toque dispara mais o alarme. A prévia sai de `HomeViewModel` e vai para a edição ("Testar alarme").
- `DayStatsRepository.addWater(delta: Int)`: soma no dia atual (respeita a virada de dia de `today()`), nunca abaixo de 0. "Desfazer" chama `addWater(-1)`.

## Strings novas

Em `values/` e `values-pt-rBR/`, com paridade de chaves e de especificadores: `edit_reminder`, `save_changes`, `test_alarm`, `diet_title`, `diet_goal`, `diet_goal_edit`, `diet_meal_empty`, `diet_no_meals`, `water_logged`, `undo`, `open_diet`. Pluralização de porções vem do próprio catálogo (singular/plural), não de `plurals`.

## Testes automatizados

- `MealRepository.latestFor`: refeição de ontem é encontrada hoje; entre datas iguais, maior `id`; lembrete sem refeição → `null`. (A lógica de escolha é extraída para uma função pura testável sobre `List<SavedMeal>`.)
- Porções: conversão porções↔gramas, arredondamento, limites 0..2000, parse de "0,5" e "1.5", singular/plural, formatação sem zero à direita.
- `MealJson`: ingrediente com porção faz round-trip; ingrediente antigo sem porção lê com `null`.
- Inferência: ingrediente antigo cujo nome bate com item do catálogo recebe a porção; nome desconhecido fica sem porção.
- Dieta: soma de várias refeições, refeição desativada fora da conta, refeição sem ingredientes conta zero.
- `Nutrition.dayShare` com meta diferente de 1800.
- Os dois testes de formato gravado passam **sem modificação**.
- Paridade de chaves e de especificadores entre os dois `strings.xml`.

Telas, toques e diálogos são verificados no aparelho.

## Riscos

- **Arredondamento em porções fracionadas**: 1,5 × 13 ml = 19,5 → 20 ml. Diferença desprezível; aceita para manter `qty: Int` e o formato.
- **Nome do alimento como chave de inferência**: renomear um item do catálogo no futuro faz ingredientes antigos perderem a porção inferida (voltam a gramas, sem perda de dado). Aceito.
- **Toque na água agora registra um copo**: um toque acidental conta errado; mitigado pelo "Desfazer".
