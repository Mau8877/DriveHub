---
globs: projects/mobile/**/*
---

# Formularios — Mobile

## Validación nativa de Flutter, sin librería externa

Se usa `Form` + `TextFormField` + el parámetro `validator` nativo de
Flutter. **No se agrega ninguna librería externa de formularios**
(`flutter_form_builder`, `reactive_forms`, etc.) — hoy `pubspec.yaml` no
tiene ninguna, y no hace falta: el caso de uso de este proyecto (formularios
CRUD estándar) no justifica esa dependencia extra.

```dart
final _formKey = GlobalKey<FormState>();

TextFormField(
  decoration: const InputDecoration(labelText: 'Correo'),
  validator: (value) {
    if (value == null || value.isEmpty) return 'El correo es obligatorio';
    if (!value.contains('@')) return 'El correo no es válido';
    return null;
  },
)

// Al enviar:
if (_formKey.currentState!.validate()) {
  // enviar
}
```

## Mensajes en español

Igual que en el frontend web (ver
[FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](../frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)):
todos los mensajes de `validator` en español, directo en el string
retornado — no hay capa de i18n en el proyecto.

## Consistencia de reglas de negocio con el frontend web

La implementación técnica es necesariamente distinta (Zod en TypeScript vs.
`validator` de Dart no comparten código entre sí), pero **la regla de
negocio que valida cada campo debe ser la misma** en ambos lados cuando el
mismo campo existe en los dos formularios (ej. "el nombre tiene mínimo 3
caracteres" no puede ser 3 en el schema de Zod y 5 en el `validator` de
Flutter para el mismo campo del mismo recurso). Si una regla de validación
cambia, se actualiza en los dos lugares en el mismo cambio — no se asume que
"ya está en un lado, en el otro se actualiza después".

El backend sigue siendo la fuente de verdad final (la API valida igual,
independientemente de lo que hagan los clientes) — la validación de
formularios en mobile/frontend es para dar feedback inmediato al usuario,
no reemplaza la validación del backend.
