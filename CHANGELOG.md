# Changelog

## Unreleased

## 1.9.0 - 2026-07-18

### Changed

- required IDE version >= `2026.2`
- Gradle-wrapper update
- dependency updates

### Fixed

- plugin incompatibilities with `2026.2` (`JCEF` module)

## 1.8.5 - 2026-04-25

### Changed

- dependency updates
- Gradle-wrapper update

## 1.8.4 - 2026-02-01

### Changed

- dependency updates
- Gradle-wrapper update

## 1.8.3 - 2025-12-22

### Fixed

- path-handling for import of custom filters and tests  
  (didn't work with Windows paths using backslashes)

### Changed

- dependency updates

## 1.8.2 - 2025-11-23

### Changed

- minor improvements in settings dialog
- Gradle-wrapper update
- dependency updates

## 1.8.1 - 2025-10-14

### Changed

- dependency updates

## 1.8.0 - 2025-10-04

### Added

- new feature: configuration of custom Jinja filters/tests (via new "Customizations" tab in tool window)

### Fixed

- minor UI improvements

### Changed

- Gradle-wrapper update
- dependency updates

## 1.7.1 - 2025-08-08

### Changed

- Gradle-wrapper update
- dependency updates

## 1.7.0 - 2025-04-23

### Fixed

- plugin incompatibilities with `2025.1`

### Changed

- required IDE version >= `2025.1`
- Gradle-wrapper update
- dependency updates

## 1.6.1 - 2025-02-16

### Changed

- Gradle-wrapper update
- dependency updates

## 1.6.0 - 2024-12-14

### Added

- integration of Jinja's `FileSystemLoader` in order to make usage of `extends`, `include`, `import` in templates possible

### Changed

- Gradle-wrapper update
- dependency updates

## 1.5.2 - 2024-09-25

### Fixed

- processing of larger templates no longer results in hangup

### Changed

- other minor code improvements
- Gradle-wrapper update
- dependency updates

## 1.5.1 - 2024-08-27

### Fixed

- improved compatibility with older Python versions (< `3.10`)

## 1.5.0 - 2024-08-22

### Changed

- improved handling of text-selection- and clipboard-listener (which now are only active when the toolbar-windows is actually visible) 
- other minor code improvements
- Gradle-wrapper update
- dependency updates

## 1.4.0 - 2024-08-12

### Changed

- required IDE version >= `2024.2`
- minor code improvements
- Gradle-wrapper update

## 1.3.0 - 2024-08-10

### Fixed

- remaining initialization errors of toolbar window

### Changed

- improved toolbar icon
- major "Gradle IntelliJ Plugin" update (`1.17.4` to `2.0.1`)
- Gradle-wrapper update
- dependency updates

## 1.2.1 - 2024-08-10

### Fixed

- issues with IntelliJ `2024.2` (improved toolbar initialization; removed "empty" icon resource)

## 1.2.0 - 2024-07-28

### Added

- ad hoc option for template source selection (file, selected text, clipboard)
- HTML mode for result viewer
- Monospaced font for variables editor and plaintext result viewer

### Changed

- Gradle-wrapper update

## 1.1.0 - 2024-07-22

### Fixed

- internal file path handling (should work on Windows now as well)

## 1.0.0 - 2024-07-14

### Added

- initial release
