# Godot-Editor-Datentypglyphen

Die in `MathematikKnoten/.../godot/GodotDatentypEtiketten.kt` eingebetteten Pfadgeometrien und ursprünglichen Farben stammen aus den gleichnamigen SVG-Dateien unter `editor/icons/` des Godot-Engine-Repositories. Sie werden im Mathematik Atlas als kompakte Etiketten für mathematisch kompatible Ausgaben verwendet.

Übernommen wurden die Glyphen für:

- Vector2, Vector2i
- Rect2, Rect2i
- Vector3, Vector3i
- Transform2D
- Vector4, Vector4i
- Plane
- Quaternion
- AABB
- Basis
- Transform3D
- Projection
- Color

Orchestrator verwendet für seine Datenpins ebenfalls Godots Editor-Class-Icons (`SceneUtils::get_class_icon(...)`); die Atlas-Implementierung übernimmt deshalb die zugrunde liegenden Godot-Glyphen direkt und koppelt sie an mathematische Strukturen statt an Godot-Variant-Typen.

## Lizenz

Copyright (c) 2014-present Godot Engine contributors (see AUTHORS.md).
Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
