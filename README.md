<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="112" height="112" alt="YummyTV logo" />
</p>

<h1 align="center">YummyTV</h1>

<p align="center">
  Независимый форк <a href="https://github.com/Helandy/YummyTV">оригинального YummyTV</a> —
  неофициального клиента <a href="https://yummyani.me/">YummyAnime</a> для Android и Android TV.
  <br />
  Сохраняет основу оригинального приложения и развивает собственные улучшения.
</p>

<p align="center">
  <a href="https://github.com/MraK0ff/YummyTV/releases/latest">
    <img alt="Скачать APK" src="https://img.shields.io/badge/Скачать-APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" />
  </a>
  <a href="https://github.com/MraK0ff/YummyTV/releases">
    <img alt="Релизы" src="https://img.shields.io/github/v/release/MraK0ff/YummyTV?style=for-the-badge" />
  </a>
  <a href="https://github.com/MraK0ff/YummyTV/stargazers">
    <img alt="Звёзды" src="https://img.shields.io/github/stars/MraK0ff/YummyTV?style=for-the-badge" />
  </a>
</p>

<p align="center">
  <img alt="Android 7.0+" src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img alt="Android TV" src="https://img.shields.io/badge/Android%20TV-supported-4285F4?style=flat-square&logo=androidtv&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="Media3" src="https://img.shields.io/badge/Player-Media3%20%2F%20ExoPlayer-orange?style=flat-square" />
</p>

> [!IMPORTANT]
> **Это независимый форк, а не официальный репозиторий YummyTV.**
> Оригинальный проект создаёт [Helandy](https://github.com/Helandy).
> Этот репозиторий поддерживается отдельно; вопросы и сообщения об ошибках, относящиеся к изменениям форка,
> направляйте в [Issues этого репозитория](https://github.com/MraK0ff/YummyTV/issues).

## О проекте

YummyTV — нативное Android-приложение для просмотра аниме через YummyAnime. Оно предлагает отдельные
интерфейсы для смартфонов и Android TV: управление с сенсорного экрана или пульта, каталог и поиск,
страницы тайтлов, библиотеку, подписки, встроенный видеоплеер и загрузку серий для офлайн-просмотра.

Подробное описание базовых возможностей и интерфейса смотрите в
[README оригинального проекта](https://github.com/Helandy/YummyTV#readme).

## Что изменено в этом форке

Форк сохраняет архитектуру и основную функциональность upstream-проекта. Собственные изменения сосредоточены
на надёжности сети, загрузке изображений и поведении плеера.

- **Сетевая подсистема.** Общая конфигурация HTTP-клиентов и таймаутов; резервное разрешение DNS для хостов изображений Yani и TMDB.
- **Изображения и постеры.** Использование AVIF на совместимых устройствах и полноразмерного JPEG в качестве запасного формата; резервный источник изображений при сетевых сбоях.
- **Устойчивость плеера.** При ошибках декодирования приложение предлагает сменить качество и продолжить воспроизведение с сохранением позиции.
- **Диагностика загрузки.** Более наглядное отображение этапов получения потока; подробный журнал доступен при включённой записи логов.

Изменения развиваются независимо и могут отличаться от текущей версии оригинального приложения.

## Скачать

Готовые APK публикуются в разделе **[Releases](https://github.com/MraK0ff/YummyTV/releases)**.

1. Откройте [последний релиз](https://github.com/MraK0ff/YummyTV/releases/latest).
2. Скачайте APK-файл из списка Assets.
3. Установите его на Android-устройство или передайте APK на Android TV.

Если релизов пока нет, готовые сборки могут быть доступны в разделе
[Actions](https://github.com/MraK0ff/YummyTV/actions) как артефакты соответствующих запусков.

## Сборка

Проект использует Kotlin, Gradle и модульную архитектуру Android.

Чтобы собрать debug APK локально:

```bash
./gradlew :app:assembleDebug
```

Готовый APK появится в:

```
app/build/outputs/apk/debug/
```

Для разработки рекомендуется открыть корневую папку проекта в Android Studio и использовать Gradle Wrapper,
поставляемый вместе с репозиторием.

## Разработка и обратная связь

- [Сообщить об ошибке или предложить улучшение](https://github.com/MraK0ff/YummyTV/issues)
- [Посмотреть изменения форка](https://github.com/MraK0ff/YummyTV/commits/main)
- [Открыть оригинальный проект](https://github.com/Helandy/YummyTV)

Форк периодически синхронизируется с upstream — оригинальным репозиторием Helandy/YummyTV.
При синхронизации сохраняются собственные изменения форка; при этом новые возможности оригинала могут
появляться здесь после обновления.

## Благодарности

Главная благодарность [Helandy](https://github.com/Helandy) за создание YummyTV и развитие оригинального проекта,
а также всем авторам библиотек и участникам open-source-проектов, на которых он основан.

## Лицензия

В репозитории размещён файл [LICENSE](LICENSE) с условиями **CC BY-NC 4.0**.
Перед использованием, распространением или созданием производных материалов ознакомьтесь с полным текстом
лицензии и сохраняйте необходимые указания авторства.

## Отказ от ответственности

- YummyTV — неофициальный клиент YummyAnime; проект не связан с владельцами сервиса и не одобряется ими.
- Приложение не предоставляет собственный каталог видео, не хранит и не распространяет видеоконтент.
- Доступность каталога, API и воспроизведения зависит от YummyAnime и сторонних видеобалансеров.
- Названия сервисов, товарные знаки и права на сторонний контент принадлежат их владельцам.
