# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# BPM Project Guide

## Build and Test Commands
- Build: `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows)
- Run client: `./gradlew runClient`
- Run all tests: `./gradlew test`
- Run single test: `./gradlew test --tests "bpm.common.vm.transpiler.ASTGeneratorTest"`

## Code Style Guidelines
- **Package Structure**: Root `bpm` with subpackages by functionality (`common`, `client`, `server`)
- **Naming**: PascalCase (classes), camelCase (methods/properties), SCREAMING_SNAKE_CASE (constants)
- **Tests**: Descriptive backtick-quoted names (``test generate node with inputs and outputs``)
- **Kotlin Style**: Official Kotlin style with preference for immutability (`val` over `var`)
- **Imports**: No wildcards except collections; group by package with blank lines
- **Design**: Interface-based abstraction, dependency injection via constructors
- **Documentation**: KDoc on public methods/properties
- **Pattern**: Composition over inheritance

## Common Tasks
- Transpiler tests: `./gradlew test --tests "bpm.common.vm.transpiler.*"`
- Update node schemas: Edit files in `src/main/resources/schemas/` directory