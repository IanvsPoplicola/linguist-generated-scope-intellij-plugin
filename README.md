# linguist-generated-scope-intellij-plugin

<!-- Plugin description -->

GitHub supports the <a href="https://docs.github.com/en/repositories/working-with-files/managing-files/customizing-how-changed-files-appear-on-github">linguist-generated</a>
attribute in `.gitattributes` files, such that any file matching a pattern tagged with `linguist-generated=true`
does not show their diffs in a pull request.

This plugin takes advantage of that information, and creates a scope that matches any of the `linguist-generated=true`
file patterns **but not** any of the `linguist-generated=false` file patterns in the entire project.
You can then set a custom colour for that scope, so that these generated files are highlighted with that colour in IntelliJ's project files sidebar.

To set the scope's colour, go to _Preferences > Appearance & Behavior > File Colors_.

To view/delete the scope, go to _Preferences > Appearance & Behavior > Scopes_.
<!-- Plugin description end -->

## Installation
Download the [latest release](https://github.com/IanvsPoplicola/linguist-generated-scope-intellij-plugin/releases/latest) and install it manually using
<kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

### Translating Git's pattern to IntelliJ scope's pattern
This documents how the plugin's file pattern translation works, if you're interested.

References:
* Git's file pattern: https://www.git-scm.com/docs/gitignore#_pattern_format
* IntelliJ scope's file pattern: https://www.jetbrains.com/help/idea/scope-language-syntax-reference.html

| Git pattern            | What it means                                          | Closest IntelliJ equivalent             | Differences                      |
|------------------------|--------------------------------------------------------|-----------------------------------------|----------------------------------|
| `!` prefix             | The pattern is inverted                                | N/A (inversion done in code)            | None                             |
| `**/` prefix           | Matches in any directory                               | `*/` prefix                             | Matches ONE OR MORE directories  |
| `/**` suffix           | Matches files and sub-directories in current directory | `//*` suffix                            | None                             |
| `/**/` middle          | Matches ZERO OR MORE directories                       | `/*/` middle                            | Matches ONE OR MORE directories  |
| `*/` prefix            | Matches EXACTLY ONE directory                          | `*/` prefix                             | Matches ZERO OR MORE directories |
| `/*` suffix            | Matches files (not directories) in current directory   | `/*` suffix                             | None                             |
| `/*/` middle           | Matches EXACTLY ONE directory                          | `/*/` middle                            | Matches ONE OR MORE directories  |
| `*` or `**` in a word  | Matches any number of non-`/` characters               | `*`                                     | None                             |
| `?` in a word          | Matches exactly one character                          | None (IntelliJ says: `unexpected '?'`)  | Not available                    |

There's a special case where the Git pattern is `*/**` or `**/*`:
* if this occurs at the start or middle of the pattern, it should be translated to just `*/`, so they both match one or more directories
* if this occurs at the end of the pattern, it should be translated to `//*`, to match everything at least one directory below


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
