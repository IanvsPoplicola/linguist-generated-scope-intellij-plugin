# linguist-generated-scope-intellij-plugin

## Translating Git's pattern to IntelliJ scope's pattern
References:
* Git's file pattern: https://www.git-scm.com/docs/gitignore#_pattern_format
* IntelliJ scope's file pattern: https://www.jetbrains.com/help/idea/scope-language-syntax-reference.html

| Git pattern   | What it means                            | Closest IntelliJ equivalent             | Differences                      |
|---------------|------------------------------------------|-----------------------------------------|----------------------------------|
| `!` prefix    | The pattern is inverted                  | N/A (inversion done in code)            | None                             |
| `**/` prefix  | Matches in any directory                 | `*/` prefix                             | Matches ONE OR MORE directories  |
| `/**` suffix  | Matches everything in current directory  | `//*` suffix                            | None                             |
| `/**/` middle | Matches ZERO OR MORE directories         | `/*/` middle                            | Matches ONE OR MORE directories  |
| `*/` prefix   | Matches EXACTLY ONE directory            | `*/` prefix                             | Matches ZERO OR MORE directories |
| `/*` suffix   | Matches EXACTLY ONE directory            | `/*` suffix                             | None                             |
| `/*/` middle  | Matches EXACTLY ONE directory            | `/*/` middle                            | Matches ONE OR MORE directories  |
| `*` in a word | Matches any number of non-`/` characters | `*`                                     | None                             |
| `?` in a word | Matches exactly one character            | None (IntelliJ says: `unexpected '?'`)  | Not available                    |

There's a special case where the Git pattern is `*/**/` or `/**/*`; in this case it should be translated to just `/*/`, so they both match one or more directories.

![Build](https://github.com/IanvsPoplicola/linguist-generated-scope-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Verify the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "linguist-generated-scope-intellij-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/IanvsPoplicola/linguist-generated-scope-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
