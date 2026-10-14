# Разработка

[English](DEVELOPMENT.md) · **Русский**

## Окружение

| Что | Версия |
|---|---|
| JDK | 17 (64-bit) |
| Gradle | 7.5.1 через wrapper (`gradlew` / `gradlew.bat`) |
| ForgeGradle | 5.1.x |
| Forge | 1.20.1-47.3.19 (для разработки; в рантайме подходит любая 47.x) |
| Маппинги | Parchment 2023.08.20-1.20.1 поверх официальных Mojang |
| IDE | IntelliJ IDEA: *File → Open* → выбрать `build.gradle` → *Open as Project* |

Версии собраны в `gradle.properties`.

## Зависимость от TaCZ

TaCZ не публикуется в maven-репозитории, поэтому его jar кладётся в `libs/` (папка в `.gitignore`, в репозиторий не попадает).

1. Соберите TaCZ из исходников: в его папке выполните `./gradlew jarJar`. Получится `build/libs/tacz-1.20.1-<версия>-all.jar` — reobf-jar со всеми вложенными библиотеками. Подойдёт и официальный релизный jar TaCZ.
2. Скопируйте его в `libs/` этого проекта.
3. Проверьте, что `tacz_version` в `gradle.properties` совпадает с частью имени файла после `tacz-1.20.1-`. Например, файлу `libs/tacz-1.20.1-1.1.8-hotfix2-all.jar` соответствует `tacz_version=1.1.8-hotfix2-all`.

`fg.deobf(...)` переводит jar в маппинги разработки, поэтому TaCZ работает и при компиляции, и в `runClient`. Его миксины применяются благодаря свойствам `mixin.env.remapRefMap` в `build.gradle`.

## Команды

```bash
./gradlew build                      # jar в build/libs/
./gradlew runClient                  # клиент разработки (папка run/)
./gradlew runClient -PquickPlay="New World"   # сразу зайти в мир из run/saves
```

В IntelliJ после импорта выполните задачу `genIntellijRuns` (вкладка Gradle → `forgegradle runs`) и запускайте конфигурацию `runClient`. Для отладки — та же конфигурация через Debug; HotSwap работает для изменений внутри методов.

## Структура

```
src/main/java/com/tacticalhud/
├── TacticalHud.java              @Mod: регистрирует конфиг, на клиенте вызывает ClientSetup
├── client/
│   ├── ClientSetup.java          оверлей, клавиша настроек, экран конфига
│   └── hud/
│       ├── TacticalHudOverlay    IGuiOverlay: снимок → анимация → перспектива → отрисовка
│       ├── HudVisibility         единая проверка «показывать ли панель сейчас»
│       ├── HudElementHider       отменяет HUD TaCZ и выбранные ванильные оверлеи
│       ├── HudSnapshot(Factory)  неизменяемые данные кадра из TaCZ и игрока
│       ├── AmmoCache             ёмкость магазина и запас (логика HUD TaCZ)
│       ├── HudLayout             профиль камеры → числа для экрана, смешивание профилей
│       ├── HudAnimator           всё, что зависит от времени
│       ├── HudPerspective        включение и выключение перспективной проекции
│       ├── HudPainter            раскладка и рисование панели
│       ├── HudIcons              векторные иконки
│       └── HudPalette            цвета
├── compat/cloth/ClothConfigScreen  меню на Cloth Config (грузится, только если мод есть)
└── config/
    ├── HudConfig                 ForgeConfigSpec: общие флаги + два профиля
    ├── HudProfile                параметры одного вида камеры, пределы
    └── HudGuiScale               Авто / x1–x4
src/main/resources/
├── META-INF/mods.toml
├── assets/tacz_tactical_hud/font/hud.json (+ hud/rajdhani_medium.ttf)
├── assets/tacz_tactical_hud/lang/{en_us,ru_ru}.json
└── data/tacz_tactical_hud/tags/items/{grenades,medkits}.json
```

### Поток кадра

```
TacticalHudOverlay.render
 ├─ HudLayout.of(first) / HudLayout.of(third)       конфиг → числа экрана
 ├─ HudVisibility.shouldRender ? HudSnapshotFactory.capture : null
 │     └─ AmmoCache.refresh (≤ 1 раз в тик, синхронизирует питание из инвентаря)
 ├─ HudAnimator.update(snapshot, …)                 время, инерция, переходы
 └─ draw: HudPerspective.begin → HudPainter.paint → HudPerspective.end (в finally)
```

## Правила кода

- Только клиент: всё в `client/` и `compat/` загружается лишь на `Dist.CLIENT`; `TacticalHud` и `config/` не импортируют `net.minecraft.client.*`.
- Cloth Config — необязательная зависимость. Его API используется только в `ClothConfigScreen`, а вызывается только после `ModList.isLoaded("cloth_config")`.
- Без миксинов: всё делается через события и оверлеи Forge.
- Внутренние классы TaCZ (`GunDisplayInstance`, `GunData`, `AttachmentDataUtils` и т. п.) — точка возможной поломки при обновлении TaCZ. При новой версии TaCZ сначала пересоберите аддон против неё.
- В отрисовке не создавайте текстуры и ресурсы и не трогайте игровое состояние. Единственное исключение — синхронизация патронов в `AmmoCache`, повторяющая поведение TaCZ.
- Новые числа раскладки — константами с понятным именем в `HudPainter`.

## Как добавить элемент на панель

1. Добавьте поле в `HudSnapshot` и заполните его в `HudSnapshotFactory.capture`.
2. Если элементу нужно состояние во времени (мигание, сглаживание), добавьте его в `HudAnimator`.
3. Нарисуйте в `HudPainter`: прямоугольники через `fill`/`fineFill` внутри `drawManaged`, текст через `text(...)`, текстуры — после блока `drawManaged`, как в `paintGunSlot`.
4. Если элемент настраивается, добавьте параметр в `HudConfig` или `HudProfile`, ползунок в `ClothConfigScreen` и строки в оба файла `lang`.

## Выпуск версии

1. Поменяйте `mod_version` в `gradle.properties` и допишите `CHANGELOG.md`.
2. `./gradlew clean build`.
3. Проверьте в чистом профиле: Forge + TaCZ + аддон, затем с Cloth Config и без него.
4. Сценарии: первое и третье лицо, F5 туда-обратно, стрельба до нуля, перезарядка, перегрев, оружие с питанием из инвентаря, креатив, смена масштаба GUI, сворачивание окна, вход на выделенный сервер без аддона.
5. Создайте релиз на GitHub и прикрепите к нему `build/libs/tacz_tactical_hud-1.20.1-<версия>.jar` (без `-sources`), затем загрузите его на Modrinth и CurseForge.
