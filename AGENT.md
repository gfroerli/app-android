# Gfrörli Android App

The **Gfrör.li** Android app is a water temperature monitoring application that
displays real-time temperature data from sensors deployed across Swiss water
bodies. The app presents sensor data on an interactive map with detailed
information views and historical temperature charts.

The measurements are requested over an API which is described in the README.md
file at <https://github.com/gfroerli/api/>.

Every sensor has a sponsor, that is shown alongside the other data.

Please see the `README.md` file for more information.

## Build & Commands

- Build app: `./gradlew assembleDebug`
- Run linter: `./gradlew lint`
- Run unit tests: `./gradlew test`
- Run integration tests: `./gradlew connectedDebugAndroidTest`

## Architecture

### Core Technologies

- **Language**: Kotlin (with some Java legacy code)
- **UI Framework**: Jetpack Compose + Material Design
- **Architecture**: MVVM with ViewModels
- **Mapps**: MapLibre SDK (migrated from Mapbox)
- **Networking**: Retrofit2 + OkHttp3 + Gson
- **Charts**: MPAndroidChart
- **Build System**: Gradle with Android Gradle Plugin (AGP)

## Code Style

- Prefer Kotlin over Java
- Follow Kotlin and Android conventions
- Use Jetpack Compose for new UI components
- Maintain existing MVVM architecture patterns
- Keep API models in `rest/models/` package
- Avoid deeply nested logic by using early returns for error cases
- Write clean, high-quality code with concise comments and clear variable names

## Testing

- Run unit tests with `./gradlew test`

## Security

- Never commit secrets or API keys to repository
- Use `app/secrets.properties` for sensitive data, this file is ignored by git

## Git Workflow

- Run `./gradlew lint test` before committing

## Decisions

Whenever there is a situation where you need to choose between two approaches,
don't just pick one. Instead, ask.

This includes:

- Choosing between two possible architectural approaches
- Choosing between two libraries to use
...and similar situations.

## Common Tasks for AI Assistants

### 1. Adding New Features

- Follow existing architecture patterns
- Use Jetpack Compose for UI components
- Add appropriate error handling
- Update strings for all supported languages
- Add tests for new functionality

### 2. API Changes

- Update models in `rest/models/`
- Modify `ApiService.kt` for new endpoints
- Handle backward compatibility if needed
- Update error handling as necessary

### 3. UI Modifications

- Use existing theme colors and typography
- Maintain Material Design consistency
- Ensure multi-language support
- Test on different screen densities

### 4. Bug Fixes

- Check CHANGELOG.md for known issues
- Use existing logging patterns
- Maintain backward compatibility
- Add regression tests

### 5. Dependency Updates

- Update `build.gradle` carefully
- Test thoroughly after updates
- Check for breaking changes
- Update documentation if needed
