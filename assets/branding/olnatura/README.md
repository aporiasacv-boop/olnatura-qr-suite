# Branding Olnatura - Olnatura QR Suite

## Logo oficial

Coloca aquí el archivo **`logo-olnatura.png`** oficial para usar en la app Olnatura QR.

### Requisitos recomendados

- **Formato**: PNG con fondo transparente
- **Tamaño mínimo**: 1024×1024 px para adaptive icons
- **Color**: Preferiblemente versión que contraste sobre fondo claro (#F3F6EF) y oscuro

## Cómo actualizar el ícono de la APK

### 1. Generar tamaños mipmap

Usa [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html) o similar:

1. Sube `logo-olnatura.png`
2. Configura foreground y background si usas adaptive icon
3. Descarga el ZIP con los mipmap
4. Extrae en `OlnaturaQR/app/src/main/res/`

### 2. Reemplazar drawables del adaptive icon

Edita los archivos en `OlnaturaQR/app/src/main/res/`:

- **`drawable/ic_launcher_foreground.xml`**: Reemplaza el círculo verde placeholder por un vector del logo, o crea un drawable que referencie tu PNG
- **`drawable/ic_launcher_background.xml`**: Ajusta el color de fondo si es necesario
- **`mipmap-anydpi/ic_launcher.xml`** y **`ic_launcher_round.xml`**: Ya apuntan a estos drawables

### 3. Validar

```bash
cd OlnaturaQR
./gradlew clean assembleDebug
```

Instala la APK y verifica el ícono en el launcher y en la vista de tareas recientes.

---

**Nota**: Actualmente la app usa un placeholder vectorial verde (#6F8B3A) que no es el ícono Android por defecto.
