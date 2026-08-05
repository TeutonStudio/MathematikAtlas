# App-Icon

`app-icon.svg` ist die kanonische, bearbeitbare Quelle des Mathematik-Atlas-App-Icons.

Android verwendet daraus die adaptive Launcher-Ressource unter
`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`. Das Manifest verweist bereits
über `@mipmap/ic_launcher` auf diese Ressource. Die Android-Vektorgrafik unter
`app/src/main/res/drawable/app_icon_foreground.xml` ist die für adaptive Icons
vereinfachte Ableitung der SVG-Quelle.
