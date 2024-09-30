# i4Platform

## Overview

`i4Platform` is a development platform that includes a variety of utilities, a UI framework, and a programming language.

- **i4Utils**: Universal classes like SyncVar, MutableCharArray and etc.
- **i4Framework**: UI Framework (requires `i4Utils`).
- **i4Framework.swing**: Swing support for `i4Framework`.
- **i4LCommon**: Classes for my programming language. (WIP)
- **i4LParser**: Parser for my programming language (It requires `i4LParser`).

## Usage
### i4Utils
```Java
final SyncVar<String> v = new SyncVar("Hello, world!");
System.out.println(v.get()); // Hello, world!
v.set("test");
System.out.println(v.get()); // test
```

### i4Framework + i4Framework.swing
```Java
final Window window = new Window();
window.setSize(720, 480);

final Button btn = new Button();
btn.setText("Click me!");
btn.setX(8);
btn.setY(8);
btn.setEndX(new PointAttach(-16, window.width));
btn.setHeight(32);
btn.addEventListener(ActionEvent.class, e -> System.out.println("Hello, world!"));
window.add(btn);

final FrameworkWindow frameworkWindow = new SwingFrame(window);

window.center();
window.setVisible(true);
```

## Contributing
I'm currently developing this project solo, but I welcome feedback, suggestions, and pull requests.
If you find any bugs or have ideas for improvements, feel free to create a GitHub issue or submit a pull request.

## Requirements
 - Java 8 or higher